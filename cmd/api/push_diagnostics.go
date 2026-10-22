package main

import (
	"context"
	"encoding/json"
	"log"
	"net/http"
	"strings"
	"time"
)

// Only send the diagnostic event to a device belonging to the current user.
func (s *Server) testDevicePushHandler(w http.ResponseWriter, r *http.Request) {
	claims := getUserFromContext(r.Context())
	if claims == nil {
		http.Error(w, "unauthorized", http.StatusUnauthorized)
		return
	}
	var request struct {
		Token string `json:"token"`
	}
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 8192)).Decode(&request); err != nil || strings.TrimSpace(request.Token) == "" || len(request.Token) > 4096 {
		http.Error(w, "invalid token", http.StatusBadRequest)
		return
	}
	if s.pushSender == nil {
		http.Error(w, "push is not configured", http.StatusServiceUnavailable)
		return
	}
	var registered bool
	if err := s.pool.QueryRow(r.Context(), `SELECT EXISTS(SELECT 1 FROM device_tokens WHERE token=$1 AND user_id=$2)`, request.Token, claims.UserID).Scan(&registered); err != nil {
		http.Error(w, "cannot check registration", http.StatusInternalServerError)
		return
	}
	if !registered {
		http.Error(w, "device is not registered", http.StatusForbidden)
		return
	}
	ctx, cancel := context.WithTimeout(r.Context(), 12*time.Second)
	defer cancel()
	if err := s.pushSender.Send(ctx, request.Token, "Уведомления Trudomix подключены", "Это сообщение пришло с вашего сервера через Firebase. Сообщения и ответы компаний будут приходить сюда.", map[string]string{"type": "push_test"}); err != nil {
		log.Printf("test push delivery to user %d failed: %v", claims.UserID, err)
		http.Error(w, "push delivery failed", http.StatusBadGateway)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}
