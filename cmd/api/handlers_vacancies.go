package main

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strconv"
	"strings"
)

func (s *Server) vacancyHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	idParam := r.URL.Query().Get("id")

	if idParam == "" {
		vacancies, err := s.storage.GetAllVacancies(ctx)
		if err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			json.NewEncoder(w).Encode(map[string]string{"error": "internal error"})
			return
		}
		json.NewEncoder(w).Encode(vacancies)
		return
	}

	id, err := strconv.Atoi(idParam)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid id"})
		return
	}

	vacancy, err := s.storage.GetVacancyByID(ctx, id)
	if err != nil {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "vacancy not found"})
		return
	}

	json.NewEncoder(w).Encode(vacancy)
}

func (s *Server) createVacancyHandler(w http.ResponseWriter, r *http.Request) {
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
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "you must create a company first"})
		return
	}

	company, err := s.storage.GetCompanyByID(ctx, *user.CompanyID)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to get company"})
		return
	}

	var req CreateVacancyRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid json"})
		return
	}

	if err := req.VacancyTerms.validate(); err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}
	if req.Title == "" || req.Location == "" || req.Experience == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "title, location and experience required"})
		return
	}

	vacancy := Vacancy{
		VacancyTerms: req.VacancyTerms,
		Title:        req.Title,
		Company:      company.Name,
		CompanyID:    company.ID,
		Location:     req.Location,
		Experience:   req.Experience,
		Remote:       req.Remote,
		Skills:       req.Skills,
		Description:  req.Description,
		Address:      req.Address,
		Latitude:     req.Latitude,
		Longitude:    req.Longitude,
		AuthorUserID: claims.UserID,
	}

	id, err := s.storage.CreateVacancy(ctx, vacancy)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to create vacancy"})
		return
	}

	vacancy.ID = id
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(vacancy)
}

// POST /api/vacancies/{id}/view — инкремент просмотров вакансии
func (s *Server) viewVacancyHandler(w http.ResponseWriter, r *http.Request) {
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
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid id"})
		return
	}

	vacancy, err := s.storage.GetVacancyByID(ctx, vacancyID)
	if err != nil {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "vacancy not found"})
		return
	}

	// ====== НЕ ИНКРЕМЕНТИРУЕМ СВОЮ ВАКАНСИЮ ======
	if vacancy.AuthorUserID == claims.UserID {
		log.Printf("⏭️ Skip vacancy view: user %d viewing own vacancy %d", claims.UserID, vacancyID)
		json.NewEncoder(w).Encode(map[string]string{"status": "skipped"})
		return
	}

	// Проверяем что кандидат не из той же компании
	user, _ := s.storage.GetUserByID(ctx, claims.UserID)
	if user.CompanyID != nil && vacancy.CompanyID > 0 && *user.CompanyID == vacancy.CompanyID {
		log.Printf("⏭️ Skip vacancy view: user %d from same company as vacancy %d", claims.UserID, vacancyID)
		json.NewEncoder(w).Encode(map[string]string{"status": "skipped"})
		return
	}

	err = s.storage.IncrementVacancyViews(ctx, vacancyID)
	if err != nil {
		log.Printf("⚠️ IncrementVacancyViews error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed"})
		return
	}

	log.Printf("✅ Vacancy %d viewed by user %d (author: %d)", vacancyID, claims.UserID, vacancy.AuthorUserID)
	json.NewEncoder(w).Encode(map[string]string{"status": "viewed"})
}

func (s *Server) searchVacanciesHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	filters := SearchFilters{
		Company:        strings.TrimSpace(r.URL.Query().Get("company")),
		Sort:           r.URL.Query().Get("sort"),
		HasCoordinates: r.URL.Query().Get("has_coordinates") == "true",
		Query:          strings.TrimSpace(r.URL.Query().Get("q")),
		Location:       strings.TrimSpace(r.URL.Query().Get("location")),
		Experience:     strings.TrimSpace(r.URL.Query().Get("experience")),
		Limit:          50,
		Offset:         0,
	}

	// Парсим remote
	if err := parseExtendedFilters(r, &filters); err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}
	if remoteStr := r.URL.Query().Get("remote"); remoteStr != "" {
		remoteBool := remoteStr == "true"
		filters.Remote = &remoteBool
	}

	// Парсим skills (через запятую)
	if skillsStr := r.URL.Query().Get("skills"); skillsStr != "" {
		filters.Skills = strings.Split(skillsStr, ",")
		for i := range filters.Skills {
			filters.Skills[i] = strings.TrimSpace(filters.Skills[i])
		}
	}

	// Парсим limit/offset
	if l := r.URL.Query().Get("limit"); l != "" {
		if parsed, err := strconv.Atoi(l); err == nil && parsed > 0 && parsed <= 100 {
			filters.Limit = parsed
		}
	}
	if o := r.URL.Query().Get("offset"); o != "" {
		if parsed, err := strconv.Atoi(o); err == nil && parsed >= 0 {
			filters.Offset = parsed
		}
	}

	vacancies, totalCount, err := s.storage.SearchVacancies(ctx, filters)
	if err != nil {
		log.Printf("SearchVacancies error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "search failed"})
		return
	}

	json.NewEncoder(w).Encode(map[string]any{
		"vacancies":   vacancies,
		"total_count": totalCount,
		"limit":       filters.Limit,
		"offset":      filters.Offset,
	})
}

// PUT /api/vacancies/{id} — обновить вакансию
func (s *Server) updateVacancyHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()
	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	vacancyID, err := strconv.Atoi(r.PathValue("id"))
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid id"})
		return
	}

	if updatedVacancy, err := s.storage.GetVacancyByID(ctx, vacancyID); err == nil {
		candidateIDs, _ := s.storage.GetApplicationUserIDs(ctx, vacancyID)
		for _, candID := range candidateIDs {
			if candID == claims.UserID {
				continue // не уведомляем самого себя
			}
			s.storage.CreateNotification(ctx, CreateNotificationRequest{
				UserID:  candID,
				Type:    "vacancy_updated",
				Title:   "Вакансия обновлена 📝",
				Message: fmt.Sprintf("Компания \"%s\" обновила вакансию \"%s\", на которую вы откликнулись", updatedVacancy.Company, updatedVacancy.Title),
				Data: map[string]interface{}{
					"vacancy_id":    vacancyID,
					"vacancy_title": updatedVacancy.Title,
					"company_name":  updatedVacancy.Company,
				},
			})
		}
		if len(candidateIDs) > 0 {
			log.Printf("📬 Notified %d candidates about vacancy %d update", len(candidateIDs), vacancyID)
		}
	}

	user, err := s.storage.GetUserByID(ctx, claims.UserID)
	if err != nil || user.CompanyID == nil {
		w.WriteHeader(http.StatusForbidden)
		json.NewEncoder(w).Encode(map[string]string{"error": "no company"})
		return
	}

	var req CreateVacancyRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid json"})
		return
	}

	if req.Title == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "title is required"})
		return
	}

	if err := req.VacancyTerms.validate(); err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}
	if err := s.storage.UpdateVacancy(ctx, vacancyID, *user.CompanyID, req); err != nil {
		if err.Error() == "vacancy not found or access denied" {
			w.WriteHeader(http.StatusForbidden)
			json.NewEncoder(w).Encode(map[string]string{"error": "vacancy not found or access denied"})
			return
		}
		log.Printf("⚠️ UpdateVacancy error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to update"})
		return
	}

	log.Printf("✅ Vacancy %d updated by user %d", vacancyID, claims.UserID)

	vacancy, err := s.storage.GetVacancyByID(ctx, vacancyID)
	if err != nil {
		json.NewEncoder(w).Encode(map[string]string{"status": "updated"})
		return
	}

	json.NewEncoder(w).Encode(vacancy)
}

// DELETE /api/vacancies/{id} — удалить вакансию
func (s *Server) deleteVacancyHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()
	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	vacancyID, err := strconv.Atoi(r.PathValue("id"))
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid id"})
		return
	}

	user, err := s.storage.GetUserByID(ctx, claims.UserID)
	if err != nil || user.CompanyID == nil {
		w.WriteHeader(http.StatusForbidden)
		json.NewEncoder(w).Encode(map[string]string{"error": "no company"})
		return
	}
	vacancyInfo, err := s.storage.GetVacancyByID(ctx, vacancyID)
	if err == nil {
		candidateIDs, _ := s.storage.GetApplicationUserIDs(ctx, vacancyID)
		for _, candID := range candidateIDs {
			if candID == claims.UserID {
				continue
			}
			s.storage.CreateNotification(ctx, CreateNotificationRequest{
				UserID:  candID,
				Type:    "vacancy_deleted",
				Title:   "Вакансия удалена 🗑️",
				Message: fmt.Sprintf("Вакансия \"%s\" компании \"%s\" была удалена. Ваш отклик аннулирован.", vacancyInfo.Title, vacancyInfo.Company),
				Data: map[string]interface{}{
					"vacancy_title": vacancyInfo.Title,
					"company_name":  vacancyInfo.Company,
				},
			})
		}
		if len(candidateIDs) > 0 {
			log.Printf("📬 Notified %d candidates about vacancy %d deletion", len(candidateIDs), vacancyID)
		}
	}

	if err := s.storage.DeleteVacancy(ctx, vacancyID, *user.CompanyID); err != nil {
		if err.Error() == "vacancy not found or access denied" {
			w.WriteHeader(http.StatusForbidden)
			json.NewEncoder(w).Encode(map[string]string{"error": "forbidden"})
			return
		}
		log.Printf("⚠️ DeleteVacancy error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to delete"})
		return
	}

	log.Printf("🗑️ Vacancy %d deleted by user %d", vacancyID, claims.UserID)
	json.NewEncoder(w).Encode(map[string]string{"status": "deleted"})
}

// PATCH /api/vacancies/{id}/archive — архивировать/разархивировать
func (s *Server) archiveVacancyHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()
	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	vacancyID, err := strconv.Atoi(r.PathValue("id"))
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid id"})
		return
	}

	var req struct {
		Archive bool `json:"archive"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid json"})
		return
	}

	user, err := s.storage.GetUserByID(ctx, claims.UserID)
	if err != nil || user.CompanyID == nil {
		w.WriteHeader(http.StatusForbidden)
		json.NewEncoder(w).Encode(map[string]string{"error": "no company"})
		return
	}

	if err := s.storage.ArchiveVacancy(ctx, vacancyID, *user.CompanyID, req.Archive); err != nil {
		if err.Error() == "vacancy not found or access denied" {
			w.WriteHeader(http.StatusForbidden)
			json.NewEncoder(w).Encode(map[string]string{"error": "forbidden"})
			return
		}
		log.Printf("⚠️ ArchiveVacancy error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed"})
		return
	}

	action := "archived"
	if !req.Archive {
		action = "restored"
	}
	log.Printf("📦 Vacancy %d %s by user %d", vacancyID, action, claims.UserID)

	// ====== НОВОЕ: Уведомление при архивации ======
	if req.Archive { // только при архивации, не при восстановлении
		if v, err := s.storage.GetVacancyByID(ctx, vacancyID); err == nil {
			candidateIDs, _ := s.storage.GetApplicationUserIDs(ctx, vacancyID)
			for _, candID := range candidateIDs {
				if candID == claims.UserID {
					continue
				}
				s.storage.CreateNotification(ctx, CreateNotificationRequest{
					UserID:  candID,
					Type:    "vacancy_archived",
					Title:   "Вакансия в архиве 📦",
					Message: fmt.Sprintf("Вакансия \"%s\" компании \"%s\" больше не активна. Ваш отклик сохранён.", v.Title, v.Company),
					Data: map[string]interface{}{
						"vacancy_id":    vacancyID,
						"vacancy_title": v.Title,
						"company_name":  v.Company,
					},
				})
			}
			if len(candidateIDs) > 0 {
				log.Printf("📬 Notified %d candidates about vacancy %d archive", len(candidateIDs), vacancyID)
			}
		}
	}

	json.NewEncoder(w).Encode(map[string]string{"status": action})
}
