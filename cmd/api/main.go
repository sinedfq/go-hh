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

	mlURL := os.Getenv("ML_SERVICE_URL")
	if mlURL == "" {
		mlURL = "http://127.0.0.1:8000"
	}
	mlClient := NewMLClient(mlURL)

	server := NewServer(storage, mlClient)

	mux := http.NewServeMux()

	mux.HandleFunc("/health", server.healthHandler)

	// Аутентификация
	mux.HandleFunc("POST /api/auth/register", server.registerHandler)
	mux.HandleFunc("POST /api/auth/login", server.loginHandler)
	mux.HandleFunc("GET /api/me", server.authMiddleware(server.meHandler))

	// Вакансии (публичные + создание для employer)
	mux.HandleFunc("GET /api/vacancies/search", server.searchVacanciesHandler)
	mux.HandleFunc("GET /api/vacancies/{id}", server.getVacancyByIDHandler)
	mux.HandleFunc("GET /api/vacancies", server.vacancyHandler)
	mux.HandleFunc("POST /api/vacancies", server.authMiddleware(server.RequireRole("employer", "admin")(server.createVacancyHandler)))
	mux.HandleFunc("POST /api/vacancies/{id}/view", server.authMiddleware(server.viewVacancyHandler))

	// Статистика
	mux.HandleFunc("GET /api/employer/stats", server.authMiddleware(server.RequireRole("employer", "admin")(server.getEmployerStatsHandler)))
	mux.HandleFunc("GET /api/my-resume/stats", server.authMiddleware(server.getMyResumeStatsHandler))

	// Резюме
	mux.HandleFunc("GET /api/resumes", server.resumeHandler)
	mux.HandleFunc("GET /api/resumes/{id}", server.authMiddleware(server.getResumeByIDHandler))
	mux.HandleFunc("POST /api/resumes", server.authMiddleware(server.createResumeHandler))
	mux.HandleFunc("GET /api/my-resume", server.authMiddleware(server.getMyResumeHandler))
	mux.HandleFunc("DELETE /api/my-resume", server.authMiddleware(server.deleteMyResumeHandler))
	mux.HandleFunc("PUT /api/my-resume", server.authMiddleware(server.updateMyResumeHandler))

	// Опыт работы
	mux.HandleFunc("POST /api/work-experience", server.authMiddleware(server.addWorkExperienceHandler))
	mux.HandleFunc("DELETE /api/work-experience/{id}", server.authMiddleware(server.deleteWorkExperienceHandler))

	// Матчинг
	mux.HandleFunc("GET /api/resumes/{id}/matches", server.authMiddleware(server.matchesHandler))

	// Избранное
	mux.HandleFunc("POST /api/favorites", server.authMiddleware(server.addFavoriteHandler))
	mux.HandleFunc("DELETE /api/favorites/{vacancyId}", server.authMiddleware(server.removeFavoriteHandler))
	mux.HandleFunc("GET /api/favorites", server.authMiddleware(server.getFavoritesHandler))

	// Библиотека навыков
	mux.HandleFunc("GET /api/skills", server.getSkillsHandler)
	mux.HandleFunc("POST /api/skills", server.authMiddleware(server.createSkillHandler))

	// Библиотека должностей
	mux.HandleFunc("GET /api/positions", server.getPositionsHandler)
	mux.HandleFunc("POST /api/positions", server.authMiddleware(server.createPositionHandler))

	// Рекомендации
	mux.HandleFunc("GET /api/recommendations", server.authMiddleware(server.recommendationsHandler))

	// Фото
	mux.HandleFunc("POST /api/users/me/photo", server.authMiddleware(server.uploadUserPhotoHandler))
	mux.HandleFunc("POST /api/resumes/me/photo", server.authMiddleware(server.uploadResumePhotoHandler))

	// Счётчик просмотров резюме
	mux.HandleFunc("POST /api/resumes/{id}/view", server.viewResumeHandler)

	// ====== КОМПАНИИ (публичные) ======
	mux.HandleFunc("GET /api/companies", server.getAllCompaniesHandler)
	mux.HandleFunc("GET /api/companies/{id}", server.getCompanyHandler)
	mux.HandleFunc("GET /api/companies/{id}/vacancies", server.getCompanyVacanciesHandler)

	// ====== КОМПАНИИ РАБОТОДАТЕЛЯ (employer/admin) ======
	mux.HandleFunc("POST /api/companies", server.authMiddleware(server.RequireRole("employer", "admin")(server.createCompanyHandler)))
	mux.HandleFunc("GET /api/my-company", server.authMiddleware(server.RequireRole("employer", "admin")(server.getMyCompanyHandler)))
	mux.HandleFunc("GET /api/my-vacancies", server.authMiddleware(server.RequireRole("employer", "admin")(server.getMyVacanciesHandler)))

	// Геокодинг (прокси через бэкенд)
	mux.HandleFunc("GET /api/geocode", server.geocodeHandler)
	mux.HandleFunc("GET /api/geocode/suggest", server.geocodeSuggestHandler)

	// ====== ОТКЛИКИ ======
	mux.HandleFunc("POST /api/vacancies/{id}/apply", server.authMiddleware(server.applyToVacancyHandler))
	mux.HandleFunc("GET /api/vacancies/{id}/application", server.authMiddleware(server.getMyApplicationHandler))
	mux.HandleFunc("GET /api/employer/applications", server.authMiddleware(server.RequireRole("employer", "admin")(server.getEmployerApplicationsHandler)))
	mux.HandleFunc("GET /api/my-applications", server.authMiddleware(server.getMyApplicationsHandler))
	mux.HandleFunc("DELETE /api/applications/{id}", server.authMiddleware(server.cancelApplicationHandler))
	mux.HandleFunc("PATCH /api/applications/{id}/status", server.authMiddleware(server.RequireRole("employer", "admin")(server.updateApplicationStatusHandler)))

	// ====== УВЕДОМЛЕНИЯ ======
	mux.HandleFunc("GET /api/notifications", server.authMiddleware(server.getNotificationsHandler))
	mux.HandleFunc("GET /api/notifications/unread-count", server.authMiddleware(server.getUnreadCountHandler))
	mux.HandleFunc("POST /api/notifications/mark-read", server.authMiddleware(server.markNotificationsReadHandler))

	// Статика
	uploadsFS := http.FileServer(http.Dir("uploads"))
	mux.Handle("/uploads/", http.StripPrefix("/uploads/", uploadsFS))

	fs := http.FileServer(http.Dir("frontend/dist"))
	mux.Handle("/", fs)

	handler := corsMiddleware(mux)

	log.Println("Server starting on :8080")
	log.Printf("ML service URL: %s", mlURL)

	if err := http.ListenAndServe(":8080", handler); err != nil {
		log.Fatalf("server failed: %v", err)
	}
}
