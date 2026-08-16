package main

import (
	"encoding/json"
	"net/http"

	"github.com/jackc/pgx/v5/pgxpool"
)

type Server struct {
	storage  Storage
	mlClient *MLClient
	recCache *RecommendationsCache
	pool     *pgxpool.Pool
}

func NewServer(storage *PostgresStorage, mlClient *MLClient) *Server {
	return &Server{
		storage:  storage,
		mlClient: mlClient,
		pool:     storage.pool,
	}
}

func (s *Server) healthHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(Health{Status: "ok"})
}
