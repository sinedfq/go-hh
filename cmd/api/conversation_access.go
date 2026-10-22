package main

import (
	"context"
	"encoding/json"
	"net/http"
	"strconv"
)

// The same membership rule protects history, read receipts, sending and typing.
func requireConversationAccess(check func(context.Context, int, int) (bool, error), next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		claims := getUserFromContext(r.Context())
		if claims == nil {
			http.Error(w, "unauthorized", http.StatusUnauthorized)
			return
		}
		id, err := strconv.Atoi(r.PathValue("id"))
		if err != nil || id <= 0 {
			http.Error(w, "invalid conversation id", http.StatusBadRequest)
			return
		}
		allowed, err := check(r.Context(), id, claims.UserID)
		if err != nil {
			http.Error(w, "failed to check conversation access", http.StatusInternalServerError)
			return
		}
		if !allowed {
			w.Header().Set("Content-Type", "application/json")
			w.WriteHeader(http.StatusForbidden)
			json.NewEncoder(w).Encode(map[string]string{"error": "forbidden"})
			return
		}
		next(w, r)
	}
}

func (s *Server) conversationMiddleware(next http.HandlerFunc) http.HandlerFunc {
	return requireConversationAccess(s.storage.CanAccessConversation, next)
}

func (s *PostgresStorage) CanAccessConversation(ctx context.Context, conversationID, userID int) (bool, error) {
	var allowed bool
	err := s.pool.QueryRow(ctx, `SELECT EXISTS (
  SELECT 1 FROM conversations c
  JOIN applications a ON a.id = c.application_id
  JOIN vacancies v ON v.id = a.vacancy_id
  WHERE c.id = $1 AND (a.candidate_user_id = $2 OR v.company_id IN (
   SELECT company_id FROM users WHERE id = $2 AND role IN ('employer','admin') AND company_id IS NOT NULL
  ))
 )`, conversationID, userID).Scan(&allowed)
	return allowed, err
}
