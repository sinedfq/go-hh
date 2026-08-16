package main

import (
	"encoding/json"
	"net/http"
)

// RequireRole — middleware для ограничения доступа по ролям
func (s *Server) RequireRole(allowedRoles ...string) func(http.HandlerFunc) http.HandlerFunc {
	roleSet := make(map[string]bool)
	for _, r := range allowedRoles {
		roleSet[r] = true
	}

	return func(next http.HandlerFunc) http.HandlerFunc {
		return func(w http.ResponseWriter, r *http.Request) {
			w.Header().Set("Content-Type", "application/json")

			claims := getUserFromContext(r.Context())
			if claims == nil {
				w.WriteHeader(http.StatusUnauthorized)
				json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
				return
			}

			if !roleSet[claims.Role] {
				w.WriteHeader(http.StatusForbidden)
				json.NewEncoder(w).Encode(map[string]string{"error": "forbidden: insufficient role"})
				return
			}

			next(w, r)
		}
	}
}
