package main

import (
	"context"
	"log"
	"net/http"
	"os"

	"github.com/sinedfq/go-hh/internal/push"
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
	if projectID := os.Getenv("FIREBASE_PROJECT_ID"); projectID != "" {
		server.pushSender, err = push.New(ctx, projectID)
		if err != nil {
			log.Fatalf("push initialization: %v", err)
		}
	} else {
		log.Println("FCM disabled: FIREBASE_PROJECT_ID is not set")
	}

	mux := http.NewServeMux()
	mux.HandleFunc("GET /api/saved-searches", server.authMiddleware(server.RequireRole("candidate", "admin")(server.savedSearchesHandler)))
	mux.HandleFunc("POST /api/saved-searches", server.authMiddleware(server.RequireRole("candidate", "admin")(server.savedSearchesHandler)))
	mux.HandleFunc("DELETE /api/saved-searches/{id}", server.authMiddleware(server.RequireRole("candidate", "admin")(server.savedSearchesHandler)))
	mux.HandleFunc("POST /api/devices", server.authMiddleware(server.registerDeviceHandler))
	mux.HandleFunc("POST /api/devices/status", server.authMiddleware(server.devicePushStatusHandler))
	mux.HandleFunc("DELETE /api/devices", server.authMiddleware(server.registerDeviceHandler))

	mux.HandleFunc("/health", server.healthHandler)

	// Аутентификация
	mux.HandleFunc("POST /api/auth/register", server.registerHandler)
	mux.HandleFunc("POST /api/auth/login", server.loginHandler)
	mux.HandleFunc("GET /api/me", server.authMiddleware(server.meHandler))

	// Вакансии (публичные + создание для employer)
	mux.HandleFunc("GET /api/vacancies/search", server.searchVacanciesHandler)
	mux.HandleFunc("POST /api/vacancies/images", server.authMiddleware(server.RequireRole("employer", "admin")(server.uploadVacancyImageHandler)))
	mux.HandleFunc("GET /api/vacancies/{id}", server.getVacancyByIDHandler)
	mux.HandleFunc("GET /api/vacancies", server.vacancyHandler)
	mux.HandleFunc("POST /api/vacancies", server.authMiddleware(server.RequireRole("employer", "admin")(server.createVacancyHandler)))
	mux.HandleFunc("PUT /api/vacancies/{id}", server.authMiddleware(server.RequireRole("employer", "admin")(server.updateVacancyHandler)))            // ← НОВОЕ
	mux.HandleFunc("DELETE /api/vacancies/{id}", server.authMiddleware(server.RequireRole("employer", "admin")(server.deleteVacancyHandler)))         // ← НОВОЕ
	mux.HandleFunc("PATCH /api/vacancies/{id}/archive", server.authMiddleware(server.RequireRole("employer", "admin")(server.archiveVacancyHandler))) // ← НОВОЕ
	mux.HandleFunc("POST /api/vacancies/{id}/view", server.authMiddleware(server.viewVacancyHandler))

	// Статистика
	mux.HandleFunc("GET /api/employer/stats", server.authMiddleware(server.RequireRole("employer", "admin")(server.getEmployerStatsHandler)))
	mux.HandleFunc("GET /api/my-resume/stats", server.authMiddleware(server.getMyResumeStatsHandler))

	// Резюме
	mux.HandleFunc("GET /api/resumes", server.resumeHandler)
	mux.HandleFunc("GET /api/resumes/{id}", server.authMiddleware(server.getResumeByIDHandler))
	mux.HandleFunc("POST /api/resumes", server.authMiddleware(server.createResumeHandler))
	mux.HandleFunc("GET /api/my-resume", server.authMiddleware(server.getMyResumeHandler))
	mux.HandleFunc("GET /api/my-resume/pdf", server.authMiddleware(server.downloadMyResumeHandler))
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
	mux.HandleFunc("POST /api/my-company/logo", server.authMiddleware(server.RequireRole("employer", "admin")(server.uploadCompanyLogoHandler)))
	mux.HandleFunc("PUT /api/my-company", server.authMiddleware(server.RequireRole("employer", "admin")(server.updateCompanyProfileHandler)))
	mux.HandleFunc("POST /api/my-company/photos", server.authMiddleware(server.RequireRole("employer", "admin")(server.companyGalleryHandler)))
	mux.HandleFunc("DELETE /api/my-company/photos", server.authMiddleware(server.RequireRole("employer", "admin")(server.companyGalleryHandler)))
	mux.HandleFunc("GET /api/my-vacancies", server.authMiddleware(server.RequireRole("employer", "admin")(server.getMyVacanciesHandler)))

	// Геокодинг (прокси через бэкенд)
	mux.HandleFunc("GET /api/geocode", server.geocodeHandler)
	mux.HandleFunc("GET /api/geocode/suggest", server.geocodeSuggestHandler)

	// ====== ОТКЛИКИ ======
	mux.HandleFunc("POST /api/vacancies/{id}/apply", server.authMiddleware(server.applyToVacancyHandler))
	mux.HandleFunc("GET /api/vacancies/{id}/application", server.authMiddleware(server.getMyApplicationHandler))
	mux.HandleFunc("GET /api/employer/applications", server.authMiddleware(server.RequireRole("employer", "admin")(server.getEmployerApplicationsHandler)))
	mux.HandleFunc("GET /api/my-applications", server.authMiddleware(server.getMyApplicationsHandler))
	mux.HandleFunc("GET /api/applications/{id}/history", server.authMiddleware(server.applicationHistoryHandler))
	mux.HandleFunc("DELETE /api/applications/{id}", server.authMiddleware(server.cancelApplicationHandler))
	mux.HandleFunc("PATCH /api/applications/{id}/status", server.authMiddleware(server.RequireRole("employer", "admin")(server.updateApplicationStatusHandler)))
	mux.HandleFunc("POST /api/applications/{id}/start-chat", server.authMiddleware(server.startChatHandler)) // ← ДОБАВЬ ЭТУ СТРОКУ
	// ====== УВЕДОМЛЕНИЯ ======
	mux.HandleFunc("GET /api/notifications", server.authMiddleware(server.getNotificationsHandler))
	mux.HandleFunc("GET /api/notifications/unread-count", server.authMiddleware(server.getUnreadCountHandler))
	mux.HandleFunc("POST /api/notifications/mark-read", server.authMiddleware(server.markNotificationsReadHandler))

	// ====== ЧАТ ======
	mux.HandleFunc("GET /api/conversations", server.authMiddleware(server.getUserConversationsHandler))
	mux.HandleFunc("GET /api/conversations/unread-count", server.authMiddleware(server.getUnreadMessagesCountHandler))
	mux.HandleFunc("GET /api/conversations/{id}/messages", server.authMiddleware(server.conversationMiddleware(server.getChatMessagesHandler)))
	mux.HandleFunc("POST /api/conversations/{id}/messages", server.authMiddleware(server.conversationMiddleware(server.sendMessageHandler)))
	mux.HandleFunc("POST /api/conversations/{id}/typing", server.authMiddleware(server.conversationMiddleware(server.markTypingHandler)))    // ← НОВОЕ
	mux.HandleFunc("GET /api/conversations/{id}/typing", server.authMiddleware(server.conversationMiddleware(server.getTypingUsersHandler))) // ← НОВОЕ

	// Статика
	uploadsFS := http.FileServer(http.Dir("uploads"))
	mux.Handle("/uploads/", http.StripPrefix("/uploads/", uploadsFS))

	fs := http.FileServer(http.Dir("frontend/dist"))
	mux.Handle("/", fs)

	handler := corsMiddleware(mux)
	go server.runSavedSearches(ctx)

	address := os.Getenv("HTTP_ADDR")
	if address == "" {
		address = ":8080"
	}
	log.Printf("Server starting on %s", address)
	log.Printf("ML service URL: %s", mlURL)

	if err := http.ListenAndServe(address, handler); err != nil {
		log.Fatalf("server failed: %v", err)
	}
}
