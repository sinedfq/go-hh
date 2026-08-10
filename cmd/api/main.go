package main

import (
	"context"
	"log"
	"net/http"
	"os"
)

func main() {
	ctx := context.Background()

	// Подключение к БД
	dbURL := os.Getenv("DATABASE_URL")
	if dbURL == "" {
		dbURL = "postgres://postgres:postgres@localhost:5432/gohh?sslmode=disable"
	}

	storage, err := NewPostgresStorage(ctx, dbURL)
	if err != nil {
		log.Fatalf("failed to connect to database: %v", err)
	}
	defer storage.Close()

	// ML-клиент
	mlURL := os.Getenv("ML_SERVICE_URL")
	if mlURL == "" {
		mlURL = "http://127.0.0.1:8000"
	}
	mlClient := NewMLClient(mlURL)

	server := NewServer(storage, mlClient)

	// Создаём главный mux
	mux := http.NewServeMux()

	// Health
	mux.HandleFunc("/health", server.healthHandler)

	// Вакансии
	mux.HandleFunc("GET /api/vacancies", server.vacancyHandler)
	mux.HandleFunc("POST /api/vacancies", server.createVacancyHandler)

	// Резюме
	mux.HandleFunc("GET /api/resumes", server.resumeHandler)
	mux.HandleFunc("POST /api/resumes", server.createResumeHandler)

	// Матчинг
	mux.HandleFunc("GET /api/resumes/{id}/matches", server.matchesHandler)

	// Избранное
	mux.HandleFunc("POST /api/favorites", server.addFavoriteHandler)
	mux.HandleFunc("DELETE /api/favorites/{vacancyId}", server.removeFavoriteHandler)
	mux.HandleFunc("GET /api/favorites", server.getFavoritesHandler)

	// Статика фронтенда (раздаём собранный React-бандл)
	fs := http.FileServer(http.Dir("frontend/dist"))
	mux.Handle("/", fs)

	// Оборачиваем в CORS middleware
	handler := corsMiddleware(mux)

	log.Println("Server starting on :8080")
	log.Printf("ML service URL: %s", mlURL)

	if err := http.ListenAndServe(":8080", handler); err != nil {
		log.Fatalf("server failed: %v", err)
	}
}
