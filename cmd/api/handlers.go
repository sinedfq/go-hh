package main

import (
	"context"
	"encoding/json"
	"net/http"
	"sync"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/sinedfq/go-hh/internal/phoneotp"
)

// ====== СТРУКТУРА СЕРВЕРА ======
type notificationSender interface {
	Send(context.Context, string, string, string, map[string]string) error
}

type Server struct {
	phoneVerification         *phoneotp.Service
	phoneVerificationTest     bool
	phoneVerificationFirebase bool
	pushSender                notificationSender
	storage                   *PostgresStorage // ← КОНКРЕТНЫЙ ТИП, без интерфейса
	mlClient                  *MLClient
	pool                      *pgxpool.Pool
	recCache                  *RecommendationsCache
	typingMu                  sync.Mutex
	typingUsers               map[int]map[int]time.Time
}

func NewServer(storage *PostgresStorage, mlClient *MLClient) *Server {
	return &Server{
		storage:     storage,
		mlClient:    mlClient,
		pool:        storage.pool,
		recCache:    NewRecommendationsCache(), // ← используем функцию из cache.go
		typingUsers: make(map[int]map[int]time.Time),
	}
}

func (s *Server) healthHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]any{"status": "ok", "push_enabled": s.pushSender != nil})
}
