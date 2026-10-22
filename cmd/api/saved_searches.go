package main

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"time"
	"unicode/utf8"
)

type SavedSearch struct {
	ID        int           `json:"id"`
	UserID    int           `json:"-"`
	Name      string        `json:"name"`
	Filters   SearchFilters `json:"filters"`
	CreatedAt time.Time     `json:"created_at"`
}

func (s *PostgresStorage) savedSearches(ctx context.Context, userID int) ([]SavedSearch, error) {
	query := `SELECT id,user_id,name,filters,created_at FROM saved_searches`
	args := []interface{}{}
	if userID > 0 {
		query += ` WHERE user_id=$1`
		args = append(args, userID)
	}
	query += ` ORDER BY id DESC`
	rows, err := s.pool.Query(ctx, query, args...)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	items := []SavedSearch{}
	for rows.Next() {
		var item SavedSearch
		if err := rows.Scan(&item.ID, &item.UserID, &item.Name, &item.Filters, &item.CreatedAt); err != nil {
			return nil, err
		}
		items = append(items, item)
	}
	return items, rows.Err()
}
func (s *Server) savedSearchesHandler(w http.ResponseWriter, r *http.Request) {
	claims := getUserFromContext(r.Context())
	if claims == nil {
		http.Error(w, "unauthorized", 401)
		return
	}
	w.Header().Set("Content-Type", "application/json")
	if r.Method == http.MethodGet {
		items, err := s.storage.savedSearches(r.Context(), claims.UserID)
		if err != nil {
			http.Error(w, "failed to load searches", 500)
			return
		}
		_ = json.NewEncoder(w).Encode(items)
		return
	}
	if r.Method == http.MethodDelete {
		id, err := strconv.Atoi(r.PathValue("id"))
		if err != nil {
			http.Error(w, "invalid id", 400)
			return
		}
		result, err := s.pool.Exec(r.Context(), `DELETE FROM saved_searches WHERE id=$1 AND user_id=$2`, id, claims.UserID)
		if err != nil {
			http.Error(w, "failed to remove search", 500)
			return
		}
		if result.RowsAffected() != 1 {
			http.Error(w, "search not found", 404)
			return
		}
		w.WriteHeader(204)
		return
	}
	var req struct {
		Name  string `json:"name"`
		Query string `json:"query"`
	}
	if json.NewDecoder(http.MaxBytesReader(w, r.Body, 16384)).Decode(&req) != nil {
		http.Error(w, "invalid json", 400)
		return
	}
	req.Name = strings.TrimSpace(req.Name)
	if req.Name == "" || utf8.RuneCountInString(req.Name) > 160 || len(req.Query) > 8000 {
		http.Error(w, "invalid search", 400)
		return
	}
	params, err := url.ParseQuery(req.Query)
	if err != nil {
		http.Error(w, "invalid filters", 400)
		return
	}
	filter := SearchFilters{Query: params.Get("q"), Location: params.Get("location"), Company: params.Get("company"), Experience: params.Get("experience"), Sort: params.Get("sort"), HasCoordinates: params.Get("has_coordinates") == "true"}
	if params.Get("remote") != "" {
		remote := params.Get("remote") == "true"
		filter.Remote = &remote
	}
	if params.Get("skills") != "" {
		filter.Skills = strings.Split(params.Get("skills"), ",")
	}
	fake := &http.Request{URL: &url.URL{RawQuery: params.Encode()}}
	if err := parseExtendedFilters(fake, &filter); err != nil {
		http.Error(w, err.Error(), 400)
		return
	}
	tx, err := s.pool.Begin(r.Context())
	if err != nil {
		http.Error(w, "failed to save search", 500)
		return
	}
	defer tx.Rollback(r.Context())
	if _, err = tx.Exec(r.Context(), `SELECT id FROM users WHERE id=$1 FOR UPDATE`, claims.UserID); err != nil {
		http.Error(w, "failed to save search", 500)
		return
	}
	var count int
	if err = tx.QueryRow(r.Context(), `SELECT COUNT(*) FROM saved_searches WHERE user_id=$1`, claims.UserID).Scan(&count); err != nil {
		http.Error(w, "failed to save search", 500)
		return
	}
	if count >= 20 {
		http.Error(w, "saved searches limit reached", 409)
		return
	}
	item := SavedSearch{Name: req.Name, Filters: filter}
	encoded, _ := json.Marshal(filter)
	err = tx.QueryRow(r.Context(), `INSERT INTO saved_searches(user_id,name,filters) VALUES($1,$2,$3) RETURNING id,created_at`, claims.UserID, req.Name, encoded).Scan(&item.ID, &item.CreatedAt)
	if err != nil {
		http.Error(w, "failed to save search", 500)
		return
	}
	if err = tx.Commit(r.Context()); err != nil {
		http.Error(w, "failed to save search", 500)
		return
	}
	w.WriteHeader(201)
	_ = json.NewEncoder(w).Encode(item)
}
func (s *Server) checkSavedSearches(ctx context.Context) error {
	searches, err := s.storage.savedSearches(ctx, 0)
	if err != nil {
		return err
	}
	for _, search := range searches {
		filter := search.Filters
		filter.NewAfter = &search.CreatedAt
		filter.ExcludeSearchID = search.ID
		filter.Sort = "oldest"
		filter.Limit = 100
		jobs, _, err := s.storage.SearchVacancies(ctx, filter)
		if err != nil {
			log.Printf("saved search %d: %v", search.ID, err)
			continue
		}
		for _, job := range jobs {
			if err := s.recordSavedSearchMatch(ctx, search, job); err != nil {
				log.Printf("saved search match: %v", err)
			}
		}
	}
	return nil
}
func (s *Server) recordSavedSearchMatch(ctx context.Context, search SavedSearch, job Vacancy) error {
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return err
	}
	defer tx.Rollback(ctx)
	result, err := tx.Exec(ctx, `INSERT INTO saved_search_matches(search_id,vacancy_id) VALUES($1,$2) ON CONFLICT DO NOTHING`, search.ID, job.ID)
	if err != nil {
		return err
	}
	if result.RowsAffected() == 0 {
		return nil
	}
	data, _ := json.Marshal(map[string]int{"vacancy_id": job.ID, "saved_search_id": search.ID})
	_, err = tx.Exec(ctx, `INSERT INTO notifications(user_id,type,title,message,data) VALUES($1,'saved_search_match',$2,$3,$4)`, search.UserID, "Новая вакансия по твоему поиску", fmt.Sprintf("%s: %s · %s", search.Name, job.Title, job.Company), data)
	if err != nil {
		return err
	}
	return tx.Commit(ctx)
}
func (s *Server) runSavedSearches(ctx context.Context) {
	ticker := time.NewTicker(30 * time.Second)
	defer ticker.Stop()
	for {
		scanCtx, cancel := context.WithTimeout(ctx, 25*time.Second)
		err := s.checkSavedSearches(scanCtx)
		cancel()
		if err != nil {
			log.Printf("saved searches: %v", err)
		}
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
		}
	}
}
