package main

import (
	"encoding/json"
	"log"
	"net/http"
	"strconv"
	"strings"
)

func (s *Server) vacancyHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	idParam := r.URL.Query().Get("id")

	if idParam == "" {
		vacancies, err := s.storage.GetAllVacancies(ctx)
		if err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			json.NewEncoder(w).Encode(map[string]string{"error": "internal error"})
			return
		}
		json.NewEncoder(w).Encode(vacancies)
		return
	}

	id, err := strconv.Atoi(idParam)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid id"})
		return
	}

	vacancy, err := s.storage.GetVacancyByID(ctx, id)
	if err != nil {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "vacancy not found"})
		return
	}

	json.NewEncoder(w).Encode(vacancy)
}

func (s *Server) createVacancyHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	var v Vacancy
	if err := json.NewDecoder(r.Body).Decode(&v); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid json"})
		return
	}

	id, err := s.storage.CreateVacancy(ctx, v)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to create"})
		return
	}

	v.ID = id
	s.recCache.InvalidateAll()
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(v)
}

func (s *Server) viewVacancyHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	vacancyID, err := strconv.Atoi(r.PathValue("id"))
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid id"})
		return
	}

	claims := getUserFromContext(ctx)
	if claims != nil {
		vacancy, err := s.storage.GetVacancyByID(ctx, vacancyID)
		if err == nil && vacancy.AuthorUserID == claims.UserID {
			json.NewEncoder(w).Encode(map[string]string{"status": "skipped", "reason": "self-view"})
			return
		}
	}

	if err := s.storage.IncrementVacancyViews(ctx, vacancyID); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to increment views"})
		return
	}

	json.NewEncoder(w).Encode(map[string]string{"status": "ok"})
}

func (s *Server) searchVacanciesHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	filters := SearchFilters{
		Query:      strings.TrimSpace(r.URL.Query().Get("q")),
		Location:   strings.TrimSpace(r.URL.Query().Get("location")),
		Experience: strings.TrimSpace(r.URL.Query().Get("experience")),
		Limit:      50,
		Offset:     0,
	}

	// Парсим remote
	if remoteStr := r.URL.Query().Get("remote"); remoteStr != "" {
		remoteBool := remoteStr == "true"
		filters.Remote = &remoteBool
	}

	// Парсим skills (через запятую)
	if skillsStr := r.URL.Query().Get("skills"); skillsStr != "" {
		filters.Skills = strings.Split(skillsStr, ",")
		for i := range filters.Skills {
			filters.Skills[i] = strings.TrimSpace(filters.Skills[i])
		}
	}

	// Парсим limit/offset
	if l := r.URL.Query().Get("limit"); l != "" {
		if parsed, err := strconv.Atoi(l); err == nil && parsed > 0 && parsed <= 100 {
			filters.Limit = parsed
		}
	}
	if o := r.URL.Query().Get("offset"); o != "" {
		if parsed, err := strconv.Atoi(o); err == nil && parsed >= 0 {
			filters.Offset = parsed
		}
	}

	vacancies, totalCount, err := s.storage.SearchVacancies(ctx, filters)
	if err != nil {
		log.Printf("SearchVacancies error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "search failed"})
		return
	}

	json.NewEncoder(w).Encode(map[string]any{
		"vacancies":   vacancies,
		"total_count": totalCount,
		"limit":       filters.Limit,
		"offset":      filters.Offset,
	})
}
