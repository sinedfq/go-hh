package main

import (
	"encoding/json"
	"net/http"
)

type Server struct {
	storage  Storage
	mlClient *MLClient
	recCache *RecommendationsCache
}

func NewServer(storage Storage, mlClient *MLClient) *Server {
	return &Server{
		storage:  storage,
		mlClient: mlClient,
		recCache: NewRecommendationsCache(),
	}
}

func (s *Server) healthHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(Health{Status: "ok"})
}
