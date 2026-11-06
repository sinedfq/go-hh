package main

import (
	"context"
	"encoding/json"
	"net/http"
	"strconv"
)

func (s *PostgresStorage) recommendationDismissals(ctx context.Context, userID int) (map[int]bool, error) {
	rows, err := s.pool.Query(ctx, `SELECT vacancy_id FROM recommendation_dismissals WHERE user_id=$1`, userID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	result := make(map[int]bool)
	for rows.Next() {
		var id int
		if err := rows.Scan(&id); err != nil {
			return nil, err
		}
		result[id] = true
	}
	return result, rows.Err()
}

func (s *Server) recommendationPreferenceHandler(w http.ResponseWriter, r *http.Request) {
	claims := getUserFromContext(r.Context())
	if claims == nil {
		http.Error(w, "unauthorized", http.StatusUnauthorized)
		return
	}
	id := 0
	if r.PathValue("id") != "" {
		var err error
		id, err = strconv.Atoi(r.PathValue("id"))
		if err != nil || id <= 0 {
			http.Error(w, "invalid vacancy id", http.StatusBadRequest)
			return
		}
	}
	if r.Method == http.MethodPut {
		if id == 0 {
			http.Error(w, "invalid vacancy id", http.StatusBadRequest)
			return
		}
		var exists bool
		if err := s.storage.pool.QueryRow(r.Context(), `SELECT EXISTS(SELECT 1 FROM vacancies WHERE id=$1)`, id).Scan(&exists); err != nil {
			http.Error(w, "failed to save preference", 500)
			return
		}
		if !exists {
			http.Error(w, "vacancy not found", 404)
			return
		}
		if _, err := s.storage.pool.Exec(r.Context(), `INSERT INTO recommendation_dismissals(user_id,vacancy_id) VALUES($1,$2) ON CONFLICT DO NOTHING`, claims.UserID, id); err != nil {
			http.Error(w, "failed to save preference", 500)
			return
		}
	} else {
		if _, err := s.storage.pool.Exec(r.Context(), `DELETE FROM recommendation_dismissals WHERE user_id=$1 AND ($2::integer=0 OR vacancy_id=$2)`, claims.UserID, id); err != nil {
			http.Error(w, "failed to restore preferences", 500)
			return
		}
	}
	s.recCache.Invalidate(claims.UserID)
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]bool{"ok": true})
}
