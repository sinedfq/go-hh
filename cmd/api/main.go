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

	// ====== API МАРШРУТЫ (ДО СТАТИКИ!) ======

	// Аутентификация
	mux.HandleFunc("POST /api/auth/register", server.registerHandler)
	mux.HandleFunc("POST /api/auth/login", server.loginHandler)
	mux.HandleFunc("GET /api/me", server.authMiddleware(server.meHandler))

	// Вакансии (публичные)
	mux.HandleFunc("GET /api/vacancies/search", server.searchVacanciesHandler) // ← ВАЖНО: ДО /api/vacancies
	mux.HandleFunc("GET /api/vacancies", server.vacancyHandler)
	mux.HandleFunc("POST /api/vacancies", server.createVacancyHandler)
	mux.HandleFunc("POST /api/vacancies/{id}/view", server.viewVacancyHandler)

	// Резюме
	mux.HandleFunc("GET /api/resumes", server.resumeHandler)
	mux.HandleFunc("POST /api/resumes", server.authMiddleware(server.createResumeHandler))
	mux.HandleFunc("GET /api/my-resume", server.authMiddleware(server.getMyResumeHandler))
	mux.HandleFunc("DELETE /api/my-resume", server.authMiddleware(server.deleteMyResumeHandler))
	mux.HandleFunc("PUT /api/my-resume", server.authMiddleware(server.updateMyResumeHandler))

	// Опыт работы
	mux.HandleFunc("POST /api/work-experience", server.authMiddleware(server.addWorkExperienceHandler))
	mux.HandleFunc("DELETE /api/work-experience/{id}", server.authMiddleware(server.deleteWorkExperienceHandler))

	// Матчинг (требует авторизации)
	mux.HandleFunc("GET /api/resumes/{id}/matches", server.authMiddleware(server.matchesHandler))

	// Избранное (требует авторизации)
	mux.HandleFunc("POST /api/favorites", server.authMiddleware(server.addFavoriteHandler))
	mux.HandleFunc("DELETE /api/favorites/{vacancyId}", server.authMiddleware(server.removeFavoriteHandler))
	mux.HandleFunc("GET /api/favorites", server.authMiddleware(server.getFavoritesHandler))

	// Библиотека навыков
	mux.HandleFunc("GET /api/skills", server.getSkillsHandler)
	mux.HandleFunc("POST /api/skills", server.authMiddleware(server.createSkillHandler))

	// Библиотека должностей
	mux.HandleFunc("GET /api/positions", server.getPositionsHandler)
	mux.HandleFunc("POST /api/positions", server.authMiddleware(server.createPositionHandler))

	// Рекомендации (требует авторизации и резюме)
	mux.HandleFunc("GET /api/recommendations", server.authMiddleware(server.recommendationsHandler))

	// Фото (требует авторизации)
	mux.HandleFunc("POST /api/users/me/photo", server.authMiddleware(server.uploadUserPhotoHandler))
	mux.HandleFunc("POST /api/resumes/me/photo", server.authMiddleware(server.uploadResumePhotoHandler))

	// Счётчик просмотров резюме
	mux.HandleFunc("POST /api/resumes/{id}/view", server.viewResumeHandler)

	// Компании (публичные) — ПЕРЕНЕСЕНО ВЫШЕ!
	mux.HandleFunc("GET /api/companies", server.getAllCompaniesHandler)
	mux.HandleFunc("GET /api/companies/{id}", server.getCompanyHandler)
	mux.HandleFunc("GET /api/companies/{id}/vacancies", server.getCompanyVacanciesHandler)

	// ====== СТАТИКА (В САМОМ КОНЦЕ) ======

	// Раздача загруженных фото (публичная)
	uploadsFS := http.FileServer(http.Dir("uploads"))
	mux.Handle("/uploads/", http.StripPrefix("/uploads/", uploadsFS))

	// Статика фронтенда
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
