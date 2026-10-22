package main

import (
	"encoding/json"
	"net/http"
)

// devicePushStatusHandler reports readiness without exposing registration tokens.
func (s *Server) devicePushStatusHandler(w http.ResponseWriter, r *http.Request) {
	claims := getUserFromContext(r.Context())
	if claims == nil {
		http.Error(w, "unauthorized", http.StatusUnauthorized)
		return
	}
	var request struct {
		Token string `json:"token"`
	}
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 8192)).Decode(&request); err != nil || len(request.Token) > 4096 {
		http.Error(w, "invalid request", http.StatusBadRequest)
		return
	}
	registered := false
	if request.Token != "" {
		if err := s.pool.QueryRow(r.Context(), `SELECT EXISTS(SELECT 1 FROM device_tokens WHERE user_id=$1 AND token=$2)`, claims.UserID, request.Token).Scan(&registered); err != nil {
			http.Error(w, "cannot check registration", http.StatusInternalServerError)
			return
		}
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(struct {
		ServerEnabled bool `json:"server_enabled"`
		Registered    bool `json:"registered"`
	}{s.pushSender != nil, registered})
}
