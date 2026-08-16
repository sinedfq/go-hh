package main

import (
	"encoding/json"
	"net/http"
	"sync"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
)

// ====== СТРУКТУРА СЕРВЕРА ======
type Server struct {
	storage  Storage
	mlClient *MLClient
	pool     *pgxpool.Pool

	// Кэш рекомендаций (реализован в cache.go)
	recCache *RecommendationsCache

	// Индикатор "печатает" в чатах
	typingMu    sync.Mutex
	typingUsers map[int]map[int]time.Time // conversation_id -> user_id -> время последней активности
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
	json.NewEncoder(w).Encode(Health{Status: "ok"})
}
