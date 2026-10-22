package main

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strconv"
	"strings"
	"time"
)

// POST /api/vacancies/{id}/apply — откликнуться на вакансию
func (s *Server) applyToVacancyHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	parts := strings.Split(r.URL.Path, "/")
	vacancyID, err := strconv.Atoi(parts[3])
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid vacancy id"})
		return
	}

	vacancy, err := s.storage.GetVacancyByID(ctx, vacancyID)
	if err != nil {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "vacancy not found"})
		return
	}

	// Защита от самоотклика — автор вакансии
	if vacancy.IsArchived {
		w.WriteHeader(http.StatusConflict)
		json.NewEncoder(w).Encode(map[string]string{"error": "vacancy archived"})
		return
	}
	if vacancy.AuthorUserID == claims.UserID {
		w.WriteHeader(http.StatusForbidden)
		json.NewEncoder(w).Encode(map[string]string{"error": "cannot apply to your own vacancy"})
		return
	}

	// Защита от самоотклика — сотрудник той же компании
	user, userErr := s.storage.GetUserByID(ctx, claims.UserID)
	if userErr != nil {
		http.Error(w, "failed to get user", http.StatusInternalServerError)
		return
	}
	if user.CompanyID != nil && vacancy.CompanyID > 0 && *user.CompanyID == vacancy.CompanyID {
		w.WriteHeader(http.StatusForbidden)
		json.NewEncoder(w).Encode(map[string]string{"error": "cannot apply to your company's vacancy"})
		return
	}

	var req CreateApplicationRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid json"})
		return
	}

	appID, err := s.storage.CreateApplication(ctx, vacancyID, claims.UserID, req)
	if err != nil {
		if err.Error() == "application already exists" {
			w.WriteHeader(http.StatusConflict)
			json.NewEncoder(w).Encode(map[string]string{"error": "already applied"})
			return
		}
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to apply"})
		return
	}

	// ====== АВТОМАТИЧЕСКОЕ СОЗДАНИЕ ЧАТА ======
	conversationID, err := s.storage.CreateConversation(ctx, appID)
	if err != nil {
		log.Printf("⚠️ Failed to create conversation for application %d: %v", appID, err)
		// Не прерываем - чат не критичен
	} else {
		log.Printf("✅ Conversation %d created for application %d", conversationID, appID)
	}

	// Уведомление автору вакансии
	if vacancy.AuthorUserID > 0 && vacancy.AuthorUserID != claims.UserID {
		s.createNotification(ctx, CreateNotificationRequest{
			UserID:  vacancy.AuthorUserID,
			Type:    "new_application",
			Title:   "Новый отклик на вакансию",
			Message: fmt.Sprintf("Пользователь откликнулся на вакансию \"%s\"", vacancy.Title),
			Data: map[string]interface{}{
				"vacancy_id":     vacancyID,
				"application_id": appID,
				"vacancy_title":  vacancy.Title,
			},
		})
	}

	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(map[string]interface{}{
		"id":         appID,
		"vacancy_id": vacancyID,
		"status":     "new",
		"message":    "Отклик отправлен",
	})
}

// GET /api/vacancies/{id}/application — проверить откликнулся ли я
func (s *Server) getMyApplicationHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	parts := strings.Split(r.URL.Path, "/")
	vacancyID, err := strconv.Atoi(parts[3])
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid vacancy id"})
		return
	}

	app, err := s.storage.GetApplicationByUserAndVacancy(ctx, claims.UserID, vacancyID)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to check"})
		return
	}

	if app == nil {
		json.NewEncoder(w).Encode(map[string]interface{}{"applied": false})
		return
	}

	json.NewEncoder(w).Encode(map[string]interface{}{
		"applied":     true,
		"application": app,
	})
}

// GET /api/employer/applications — все отклики на вакансии моей компании
func (s *Server) getEmployerApplicationsHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	user, err := s.storage.GetUserByID(ctx, claims.UserID)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to get user"})
		return
	}

	if user.CompanyID == nil {
		json.NewEncoder(w).Encode([]Application{})
		return
	}

	apps, err := s.storage.GetApplicationsForEmployer(ctx, *user.CompanyID)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to get applications"})
		return
	}

	// Помечаем новые как просмотренные
	for _, app := range apps {
		if app.Status == "new" {
			s.storage.MarkApplicationViewed(ctx, app.ID)
		}
	}

	json.NewEncoder(w).Encode(apps)
}

// GET /api/my-applications — мои отклики (для кандидата)
func (s *Server) getMyApplicationsHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	apps, err := s.storage.GetMyApplications(ctx, claims.UserID)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to get applications"})
		return
	}

	json.NewEncoder(w).Encode(apps)
}

// PATCH /api/applications/{id}/status — изменить статус отклика
func (s *Server) updateApplicationStatusHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	parts := strings.Split(r.URL.Path, "/")
	appID, err := strconv.Atoi(parts[3])
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid id"})
		return
	}

	var req ApplicationUpdateRequest
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 16384)).Decode(&req); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid json"})
		return
	}

	if req.Status != "viewed" && req.Status != "accepted" && req.Status != "rejected" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid status"})
		return
	}

	// Получаем отклик с данными вакансии
	app, err := s.storage.GetApplicationByID(ctx, appID)
	if err != nil {
		log.Printf("⚠️ GetApplicationByID error: %v", err)
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "application not found"})
		return
	}

	log.Printf("📧 Изменение статуса отклика:")
	log.Printf("   Application ID: %d", app.ID)
	log.Printf("   Vacancy: %s (company_id: %d)", app.VacancyTitle, app.VacancyCompanyID)
	log.Printf("   Candidate user: %d", app.CandidateUserID)
	log.Printf("   New status: %s", req.Status)

	// Проверяем что работодатель имеет право менять этот отклик
	employer, err := s.storage.GetUserByID(ctx, claims.UserID)
	if err != nil {
		log.Printf("⚠️ GetUserByID error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to get user"})
		return
	}

	if employer.CompanyID == nil || (employer.Role != "employer" && employer.Role != "admin") {
		log.Printf("⚠️ Employer has no company")
		w.WriteHeader(http.StatusForbidden)
		json.NewEncoder(w).Encode(map[string]string{"error": "forbidden"})
		return
	}

	log.Printf("   Employer company: %d, Vacancy company: %d", *employer.CompanyID, app.VacancyCompanyID)

	if app.VacancyCompanyID != *employer.CompanyID {
		log.Printf("⚠️ Company mismatch! Access denied")
		w.WriteHeader(http.StatusForbidden)
		json.NewEncoder(w).Encode(map[string]string{"error": "forbidden"})
		return
	}

	var interview *time.Time
	if req.Feedback != nil && len(*req.Feedback) > 4000 {
		http.Error(w, "feedback too long", 400)
		return
	}
	if req.InterviewAt != nil && *req.InterviewAt != "" {
		parsed, err := time.Parse(time.RFC3339, *req.InterviewAt)
		if err != nil {
			http.Error(w, "invalid interview date", 400)
			return
		}
		interview = &parsed
	}
	// Status, candidate-visible feedback and meeting time change atomically.
	changed, err := s.storage.updateApplicationDetails(ctx, appID, req, interview)
	if err != nil {
		log.Printf("⚠️ UpdateApplicationStatus error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to update"})
		return
	}

	log.Printf("   ✅ Статус обновлён")
	if changed && app.Status != req.Status {
		if err := s.createNotification(ctx, applicationNotification(app, appID, req.Status)); err != nil {
			log.Printf("application notification: %v", err)
		}
	}

	if changed && app.Status == req.Status && (req.Feedback != nil || req.InterviewAt != nil) {
		message := app.CompanyName + ": обновлены комментарий или время собеседования по вакансии «" + app.VacancyTitle + "»"
		if err := s.createNotification(ctx, CreateNotificationRequest{UserID: app.CandidateUserID, Type: "application_updated", Title: "Обновлены детали отклика", Message: message, Data: map[string]interface{}{"vacancy_id": app.VacancyID, "application_id": appID}}); err != nil {
			log.Printf("application details notification: %v", err)
		}
	}
	json.NewEncoder(w).Encode(map[string]string{"status": "updated"})
}

// GET /api/notifications — мои уведомления
func (s *Server) getNotificationsHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	notifications, err := s.storage.GetNotifications(ctx, claims.UserID, 50)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to get notifications"})
		return
	}

	json.NewEncoder(w).Encode(notifications)
}

// GET /api/notifications/unread-count — количество непрочитанных
func (s *Server) getUnreadCountHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	count, err := s.storage.GetUnreadCount(ctx, claims.UserID)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed"})
		return
	}

	json.NewEncoder(w).Encode(map[string]int{"count": count})
}

// POST /api/notifications/mark-read — пометить все как прочитанные
func (s *Server) markNotificationsReadHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	if err := s.storage.MarkNotificationsRead(ctx, claims.UserID); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed"})
		return
	}

	json.NewEncoder(w).Encode(map[string]string{"status": "ok"})
}

// DELETE /api/applications/{id} — отменить свой отклик (для кандидата)
func (s *Server) cancelApplicationHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	parts := strings.Split(r.URL.Path, "/")
	appID, err := strconv.Atoi(parts[3])
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid id"})
		return
	}

	// Проверяем что отклик принадлежит текущему пользователю
	var candidateUserID int
	err = s.pool.QueryRow(ctx, `SELECT candidate_user_id FROM applications WHERE id = $1`, appID).Scan(&candidateUserID)
	if err != nil {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "application not found"})
		return
	}

	if candidateUserID != claims.UserID {
		w.WriteHeader(http.StatusForbidden)
		json.NewEncoder(w).Encode(map[string]string{"error": "forbidden"})
		return
	}

	// Удаляем отклик
	_, err = s.pool.Exec(ctx, `DELETE FROM applications WHERE id = $1`, appID)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to cancel"})
		return
	}

	json.NewEncoder(w).Encode(map[string]string{"status": "cancelled"})
}

// POST /api/applications/{id}/start-chat — создать чат для существующего отклика
func (s *Server) startChatHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	// Получаем ID отклика из URL
	appID, err := strconv.Atoi(r.PathValue("id"))
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid application id"})
		return
	}

	// Получаем отклик
	application, err := s.storage.GetApplicationByID(ctx, appID)
	if err != nil {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "application not found"})
		return
	}

	// Проверяем что пользователь имеет право создавать чат для этого отклика
	// Это может быть либо кандидат, либо работодатель компании
	user, userErr := s.storage.GetUserByID(ctx, claims.UserID)
	if userErr != nil {
		http.Error(w, "failed to get user", http.StatusInternalServerError)
		return
	}
	isCandidate := application.CandidateUserID == claims.UserID
	isEmployer := (user.Role == "employer" || user.Role == "admin") && user.CompanyID != nil && application.VacancyCompanyID == *user.CompanyID

	if !isCandidate && !isEmployer {
		w.WriteHeader(http.StatusForbidden)
		json.NewEncoder(w).Encode(map[string]string{"error": "forbidden"})
		return
	}

	// Проверяем, существует ли уже чат
	if application.ConversationID != nil && *application.ConversationID > 0 {
		// Чат уже существует
		w.WriteHeader(http.StatusOK)
		json.NewEncoder(w).Encode(map[string]interface{}{
			"conversation_id": *application.ConversationID,
			"message":         "Chat already exists",
		})
		return
	}

	// Создаем новый чат
	conversationID, err := s.storage.CreateConversation(ctx, appID)
	if err != nil {
		log.Printf("⚠️ Failed to create conversation for application %d: %v", appID, err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to create chat"})
		return
	}

	log.Printf("✅ Conversation %d created for application %d by user %d", conversationID, appID, claims.UserID)

	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(map[string]interface{}{
		"conversation_id": conversationID,
		"message":         "Chat created successfully",
	})
}
