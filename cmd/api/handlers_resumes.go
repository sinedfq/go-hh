package main

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strconv"
	"strings"
)

func (s *Server) resumeHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	idParam := r.URL.Query().Get("id")

	if idParam == "" {
		resumes, err := s.storage.GetAllResumes(ctx)
		if err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			json.NewEncoder(w).Encode(map[string]string{"error": "internal error"})
			return
		}
		json.NewEncoder(w).Encode(resumes)
		return
	}

	id, err := strconv.Atoi(idParam)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid id"})
		return
	}

	resume, err := s.storage.GetResumeByID(ctx, id)
	if err != nil {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "resume not found"})
		return
	}

	json.NewEncoder(w).Encode(resume)
}

func (s *Server) createResumeHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	_, err := s.storage.GetResumeByUserID(ctx, claims.UserID)
	if err == nil {
		w.WriteHeader(http.StatusConflict)
		json.NewEncoder(w).Encode(map[string]string{"error": "resume already exists"})
		return
	}

	var req CreateResumeRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid json"})
		return
	}

	if req.FullName == "" || req.DesiredPosition == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "full_name and desired_position are required"})
		return
	}

	if req.Experience == "" {
		req.Experience = "Junior"
	}

	if req.Skills == nil {
		req.Skills = []string{}
	}

	resumeID, err := s.storage.CreateResume(ctx, claims.UserID, req)
	if err != nil {
		log.Printf("CreateResume error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{
			"error":   "failed to create resume",
			"details": err.Error(),
		})
		return
	}

	resume, _ := s.storage.GetResumeByUserID(ctx, claims.UserID)
	resume.ID = resumeID

	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(resume)
}

func (s *Server) getMyResumeHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	resume, err := s.storage.GetResumeByUserID(ctx, claims.UserID)
	if err != nil {
		// Тихо возвращаем 404 без логирования
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "resume not found"})
		return
	}

	json.NewEncoder(w).Encode(resume)
}

func (s *Server) updateMyResumeHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	currentResume, err := s.storage.GetResumeByUserID(ctx, claims.UserID)
	if err != nil {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "resume not found"})
		return
	}

	var req CreateResumeRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid json"})
		return
	}

	if req.FullName == "" || req.DesiredPosition == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "full_name and desired_position are required"})
		return
	}

	if req.Experience == "" {
		req.Experience = "Junior"
	}

	if req.Skills == nil {
		req.Skills = []string{}
	}

	if err := s.storage.UpdateResume(ctx, currentResume.ID, claims.UserID, req); err != nil {
		log.Printf("UpdateResume error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{
			"error":   "failed to update resume",
			"details": err.Error(),
		})
		return
	}

	s.recCache.Invalidate(claims.UserID)

	updatedResume, _ := s.storage.GetResumeByUserID(ctx, claims.UserID)
	json.NewEncoder(w).Encode(updatedResume)
}

func (s *Server) deleteMyResumeHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	resume, err := s.storage.GetResumeByUserID(ctx, claims.UserID)
	if err != nil {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "resume not found"})
		return
	}

	if err := s.storage.DeleteResume(ctx, resume.ID, claims.UserID); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to delete resume"})
		return
	}
	s.recCache.Invalidate(claims.UserID)

	json.NewEncoder(w).Encode(map[string]string{"status": "deleted"})
}

// POST /api/resumes/{id}/view — инкремент просмотров (с проверкой владельца)
func (s *Server) viewResumeHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	parts := strings.Split(r.URL.Path, "/")
	resumeID, err := strconv.Atoi(parts[len(parts)-1])
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid id"})
		return
	}

	// Получаем резюме чтобы проверить владельца
	resume, err := s.storage.GetResumeByID(ctx, resumeID)
	if err != nil {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "resume not found"})
		return
	}

	// ====== ЗАЩИТА: свой резюме не инкрементируем ======
	if resume.UserID == claims.UserID {
		log.Printf("⏭️ Пропускаем инкремент: пользователь %d смотрит своё резюме %d", claims.UserID, resumeID)
		w.WriteHeader(http.StatusOK)
		json.NewEncoder(w).Encode(map[string]string{"status": "skipped (own resume)"})
		return
	}

	// Инкремент только для чужих резюме
	err = s.storage.IncrementResumeViews(ctx, resumeID)
	if err != nil {
		log.Printf("⚠️ IncrementResumeViews error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to increment"})
		return
	}

	log.Printf("✅ Resume %d viewed by user %d (owner: %d)", resumeID, claims.UserID, resume.UserID)
	json.NewEncoder(w).Encode(map[string]string{"status": "viewed"})
}

func (s *Server) addWorkExperienceHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	resume, err := s.storage.GetResumeByUserID(ctx, claims.UserID)
	if err != nil {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "resume not found"})
		return
	}

	var req CreateWorkExperienceRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		log.Printf("Decode error in addWorkExperience: %v", err)
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid json"})
		return
	}

	if req.Company == "" || req.Position == "" || req.StartDate == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "company, position and start_date are required"})
		return
	}

	_, err = s.storage.CreateWorkExperience(ctx, resume.ID, req)
	if err != nil {
		log.Printf("CreateWorkExperience error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{
			"error":   "failed to create work experience",
			"details": err.Error(),
		})
		return
	}

	updatedResume, _ := s.storage.GetResumeByUserID(ctx, claims.UserID)
	s.recCache.Invalidate(claims.UserID)
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(updatedResume)
}

func (s *Server) deleteWorkExperienceHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	expID, err := strconv.Atoi(r.PathValue("id"))
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid id"})
		return
	}

	if err := s.storage.DeleteWorkExperience(ctx, expID); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to delete"})
		return
	}

	updatedResume, _ := s.storage.GetResumeByUserID(ctx, claims.UserID)
	s.recCache.Invalidate(claims.UserID)
	json.NewEncoder(w).Encode(updatedResume)
}

// GET /api/resumes/{id} — получить резюме по ID
func (s *Server) getResumeByIDHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		log.Printf("❌ getResumeByID: unauthorized")
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	parts := strings.Split(r.URL.Path, "/")
	resumeID, err := strconv.Atoi(parts[len(parts)-1])
	if err != nil {
		log.Printf("❌ getResumeByID: invalid id in path '%s'", r.URL.Path)
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid id"})
		return
	}

	log.Printf("📄 Запрос резюме ID=%d от пользователя %d (role: %s)", resumeID, claims.UserID, claims.Role)

	resume, err := s.storage.GetResumeByID(ctx, resumeID)
	if err != nil {
		log.Printf("❌ GetResumeByID вернул ошибку: %v", err)
		if err.Error() == "resume not found" {
			w.WriteHeader(http.StatusNotFound)
			json.NewEncoder(w).Encode(map[string]string{"error": "resume not found"})
		} else {
			w.WriteHeader(http.StatusInternalServerError)
			json.NewEncoder(w).Encode(map[string]string{"error": err.Error()})
		}
		return
	}

	log.Printf("✅ Резюме %d найдено: %s (user_id=%d, views=%d)", resumeID, resume.FullName, resume.UserID, resume.Views)

	// ====== ИНКРЕМЕНТ ПРОСМОТРОВ + УВЕДОМЛЕНИЕ (только если это не своё резюме) ======
	if resume.UserID != claims.UserID {
		// Инкремент просмотров — ВСЕГДА (каждый просмотр считается)
		err := s.storage.IncrementResumeViews(ctx, resumeID)
		if err != nil {
			log.Printf("⚠️ IncrementResumeViews error: %v", err)
		} else {
			log.Printf("✅ Resume %d: views incremented to %d", resumeID, resume.Views+1)
			resume.Views++
		}

		// ====== УВЕДОМЛЕНИЕ (только если HR не смотрел это резюме за последние 24 часа) ======
		hasRecent, err := s.storage.HasRecentResumeViewNotification(ctx, resume.UserID, claims.UserID, 24)
		if err != nil {
			log.Printf("⚠️ HasRecentResumeViewNotification error: %v", err)
		}

		if hasRecent {
			log.Printf("⏭️ Пропускаем уведомление: HR %d уже смотрел резюме %d за последние 24 часа", claims.UserID, resumeID)
		} else {
			viewer, err := s.storage.GetUserByID(ctx, claims.UserID)
			if err == nil {
				companyName := "Неизвестная компания"
				if viewer.CompanyID != nil {
					company, err := s.storage.GetCompanyByID(ctx, *viewer.CompanyID)
					if err == nil {
						companyName = company.Name
					}
				}

				notifErr := s.storage.CreateNotification(ctx, CreateNotificationRequest{
					UserID:  resume.UserID,
					Type:    "resume_viewed",
					Title:   "Ваше резюме просмотрено 👀",
					Message: fmt.Sprintf("Компания \"%s\" просмотрела ваше резюме", companyName),
					Data: map[string]interface{}{
						"resume_id":      resumeID,
						"company_name":   companyName,
						"viewer_email":   viewer.Email,
						"viewer_user_id": claims.UserID, // ← ВАЖНО: для проверки дубликатов
					},
				})
				if notifErr != nil {
					log.Printf("⚠️ Failed to create notification: %v", notifErr)
				} else {
					log.Printf("📬 Notification sent to candidate %d", resume.UserID)
				}
			}
		}
	}

	json.NewEncoder(w).Encode(resume)
}
