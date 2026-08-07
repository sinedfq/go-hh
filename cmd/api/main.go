package main

import (
	"context"
	"log"
	"net/http"
	"os"
)

func main() {
	ctx := context.Background()

	dbURL := os.Getenv("DATABASE_URL")
	if dbURL == "" {
		dbURL = "postgres://postgres:postgres@localhost:5432/gohh?sslmode=disable"
	}

	storage, err := NewPostgresStorage(ctx, dbURL)
	if err != nil {
		log.Fatalf("failed to connect to database: %v", err)
	}
	defer storage.Close()

	server := NewServer(storage)

	// Health
	http.HandleFunc("/health", server.healthHandler)

	// Вакансии
	http.HandleFunc("GET /api/vacancies", server.vacancyHandler)
	http.HandleFunc("POST /api/vacancies", server.createVacancyHandler)

	// Резюме
	http.HandleFunc("GET /api/resumes", server.resumeHandler)
	http.HandleFunc("POST /api/resumes", server.createResumeHandler)

	log.Println("Server starting on :8080")
	if err := http.ListenAndServe(":8080", nil); err != nil {
		log.Fatalf("server failed: %v", err)
	}
}
