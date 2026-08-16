package main

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strconv"
	"strings"
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
	if vacancy.AuthorUserID == claims.UserID {
		w.WriteHeader(http.StatusForbidden)
		json.NewEncoder(w).Encode(map[string]string{"error": "cannot apply to your own vacancy"})
		return
	}

	// Защита от самоотклика — сотрудник той же компании
	user, _ := s.storage.GetUserByID(ctx, claims.UserID)
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

	// Уведомление автору вакансии
	if vacancy.AuthorUserID > 0 && vacancy.AuthorUserID != claims.UserID {
		s.storage.CreateNotification(ctx, CreateNotificationRequest{
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

	var req struct {
		Status string `json:"status"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
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

	if employer.CompanyID == nil {
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

	// Обновляем статус
	if err := s.storage.UpdateApplicationStatus(ctx, appID, req.Status); err != nil {
		log.Printf("⚠️ UpdateApplicationStatus error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to update"})
		return
	}

	log.Printf("   ✅ Статус обновлён")

	// ====== УВЕДОМЛЕНИЕ КАНДИДАТУ ======
	if req.Status == "accepted" {
		log.Printf("   📬 Отправляем уведомление о принятии кандидату %d", app.CandidateUserID)
		err := s.storage.CreateNotification(ctx, CreateNotificationRequest{
			UserID:  app.CandidateUserID,
			Type:    "application_accepted",
			Title:   "Ваш отклик принят! 🎉",
			Message: fmt.Sprintf("Компания \"%s\" приняла ваш отклик на вакансию \"%s\"", app.CompanyName, app.VacancyTitle),
			Data: map[string]interface{}{
				"vacancy_id":    app.VacancyID,
				"vacancy_title": app.VacancyTitle,
				"company_name":  app.CompanyName,
			},
		})
		if err != nil {
			log.Printf("   ❌ Ошибка создания уведомления: %v", err)
		} else {
			log.Printf("   ✅ Уведомление создано")
		}
	} else if req.Status == "rejected" {
		log.Printf("   📬 Отправляем уведомление об отклонении кандидату %d", app.CandidateUserID)
		err := s.storage.CreateNotification(ctx, CreateNotificationRequest{
			UserID:  app.CandidateUserID,
			Type:    "application_rejected",
			Title:   "Отклик отклонён",
			Message: fmt.Sprintf("К сожалению, компания \"%s\" отклонила ваш отклик на вакансию \"%s\"", app.CompanyName, app.VacancyTitle),
			Data: map[string]interface{}{
				"vacancy_id":    app.VacancyID,
				"vacancy_title": app.VacancyTitle,
				"company_name":  app.CompanyName,
			},
		})
		if err != nil {
			log.Printf("   ❌ Ошибка создания уведомления: %v", err)
		} else {
			log.Printf("   ✅ Уведомление создано")
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
