package main

import (
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strconv"
	"time"

	"golang.org/x/crypto/bcrypt"
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

	var v Vacancy
	if err := json.NewDecoder(r.Body).Decode(&v); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid json"})
		return
	}

	id, err := s.storage.CreateVacancy(ctx, v)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to create"})
		return
	}

	v.ID = id
	s.recCache.InvalidateAll()
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(v)
}

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
	log.Printf("CREATE RESUME HANDLER CALLED")
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	// Проверяем, нет ли уже резюме у пользователя
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

	// Валидация
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
		log.Printf("CreateResume error: %v", err) // Логируем ошибку
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
		log.Printf("getMyResume error for user %d: %v", claims.UserID, err) // ← ДОБАВЛЕНО
		if err.Error() == "resume not found" {
			w.WriteHeader(http.StatusNotFound)
			json.NewEncoder(w).Encode(map[string]string{"error": "resume not found"})
			return
		}
		s.recCache.Invalidate(claims.UserID)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{
			"error":   "failed to get resume",
			"details": err.Error(),
		})
		return
	}

	json.NewEncoder(w).Encode(resume)
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

func (s *Server) matchesHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	// Получаем ID резюме из пути
	idParam := r.PathValue("id")
	id, err := strconv.Atoi(idParam)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid id"})
		return
	}

	// Получаем резюме из БД
	resume, err := s.storage.GetResumeByID(ctx, id)
	if err != nil {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "resume not found"})
		return
	}

	// Получаем все вакансии
	candidates, err := s.storage.GetCandidateVacancies(ctx, resume.Skills, 30)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "internal error"})
		return
	}

	// Вызываем ML-сервис
	mlMatches, err := s.mlClient.MatchResumeToVacancies(ctx, resume, candidates)
	if err != nil {
		w.WriteHeader(http.StatusServiceUnavailable)
		json.NewEncoder(w).Encode(map[string]string{
			"error":   "ML service unavailable",
			"details": err.Error(),
		})
		return
	}

	// Формируем ответ с полными данными о вакансиях
	type FullMatch struct {
		Vacancy   Vacancy `json:"vacancy"`
		Score     float64 `json:"score"`
		Reasoning string  `json:"reasoning"`
	}

	var fullMatches []FullMatch
	for _, mlMatch := range mlMatches {
		for _, v := range candidates {
			if v.ID == mlMatch.VacancyID {
				fullMatches = append(fullMatches, FullMatch{
					Vacancy:   v,
					Score:     mlMatch.Score,
					Reasoning: mlMatch.Reasoning,
				})
				break
			}
		}
	}

	json.NewEncoder(w).Encode(map[string]any{
		"resume_id": resume.ID,
		"matches":   fullMatches,
	})
}

func (s *Server) addFavoriteHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	var req struct {
		VacancyID int `json:"vacancy_id"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid json"})
		return
	}

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}
	userID := claims.UserID

	if err := s.storage.AddFavorite(ctx, userID, req.VacancyID); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to add favorite"})
		return
	}

	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(map[string]string{"status": "added"})
}

func (s *Server) removeFavoriteHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	vacancyID, err := strconv.Atoi(r.PathValue("vacancyId"))
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid vacancy id"})
		return
	}

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}
	userID := claims.UserID

	if err := s.storage.RemoveFavorite(ctx, userID, vacancyID); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to remove favorite"})
		return
	}

	json.NewEncoder(w).Encode(map[string]string{"status": "removed"})
}

func (s *Server) getFavoritesHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}
	userID := claims.UserID

	vacancies, err := s.storage.GetFavorites(ctx, userID)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to get favorites"})
		return
	}

	if vacancies == nil {
		vacancies = []Vacancy{}
	}

	json.NewEncoder(w).Encode(vacancies)
}

func (s *Server) registerHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	var req RegisterRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid json"})
		return
	}

	// Валидация
	if req.Email == "" || req.Password == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "email and password required"})
		return
	}

	if len(req.Password) < 6 {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "password must be at least 6 characters"})
		return
	}

	// Проверяем, не зарегистрирован ли уже
	_, err := s.storage.GetUserByEmail(ctx, req.Email)
	if err == nil {
		w.WriteHeader(http.StatusConflict)
		json.NewEncoder(w).Encode(map[string]string{"error": "user already exists"})
		return
	}

	// Хешируем пароль
	passwordHash, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to hash password"})
		return
	}

	// Создаём пользователя
	userID, err := s.storage.CreateUser(ctx, req.Email, string(passwordHash))
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to create user"})
		return
	}

	// Генерируем токен
	token, err := GenerateToken(userID, req.Email)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to generate token"})
		return
	}

	user, _ := s.storage.GetUserByID(ctx, userID)

	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(AuthResponse{
		Token: token,
		User:  user,
	})
}

func (s *Server) loginHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	var req LoginRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid json"})
		return
	}

	user, err := s.storage.GetUserByEmail(ctx, req.Email)
	if err != nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid credentials"})
		return
	}

	if err := bcrypt.CompareHashAndPassword([]byte(user.PasswordHash), []byte(req.Password)); err != nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid credentials"})
		return
	}

	token, err := GenerateToken(user.ID, user.Email)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to generate token"})
		return
	}

	json.NewEncoder(w).Encode(AuthResponse{
		Token: token,
		User:  user,
	})
}

func (s *Server) meHandler(w http.ResponseWriter, r *http.Request) {
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
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "user not found"})
		return
	}

	json.NewEncoder(w).Encode(user)
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

	// Логируем что получили
	log.Printf("Adding work experience: company=%s, position=%s, start_date=%s",
		req.Company, req.Position, req.StartDate)

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

func (s *Server) getSkillsHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	skills, err := s.storage.GetAllSkills(ctx)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to get skills"})
		return
	}

	json.NewEncoder(w).Encode(skills)
}

func (s *Server) createSkillHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	var req struct {
		Name string `json:"name"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid json"})
		return
	}

	if req.Name == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "name is required"})
		return
	}

	skill, err := s.storage.CreateSkill(ctx, req.Name)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to create skill"})
		return
	}

	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(skill)
}

func (s *Server) getPositionsHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	positions, err := s.storage.GetAllPositions(ctx)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to get positions"})
		return
	}

	json.NewEncoder(w).Encode(positions)
}

func (s *Server) createPositionHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	var req struct {
		Name string `json:"name"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid json"})
		return
	}

	if req.Name == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "name is required"})
		return
	}

	position, err := s.storage.CreatePosition(ctx, req.Name)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to create position"})
		return
	}

	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(position)
}

func (s *Server) recommendationsHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	// Получаем резюме пользователя
	resume, err := s.storage.GetResumeByUserID(ctx, claims.UserID)
	if err != nil {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "resume not found"})
		return
	}

	resumeHash := HashResume(resume)
	forceRefresh := r.URL.Query().Get("refresh") == "true"

	// ====== ПРОВЕРЯЕМ КЭШ ======
	if !forceRefresh {
		if cached, ok := s.recCache.Get(claims.UserID, resumeHash); ok {
			w.Header().Set("X-Cache", "HIT")
			log.Printf("Recommendations cache HIT for user %d", claims.UserID)
			json.NewEncoder(w).Encode(map[string]any{
				"resume":          resume,
				"recommendations": cached.Recommendations,
				"model_used":      cached.ModelUsed,
				"from_cache":      true,
			})
			return
		}
	}

	// ====== КЭША НЕТ — СЧИТАЕМ ЗАНОВО ======
	w.Header().Set("X-Cache", "MISS")
	log.Printf("Recommendations cache MISS for user %d, calling ML service", claims.UserID)

	// Этап 1: отбор кандидатов
	const candidateLimit = 30
	candidates, err := s.storage.GetCandidateVacancies(ctx, resume.Skills, candidateLimit)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to fetch candidates"})
		return
	}

	var recommendations []Recommendation
	modelUsed := "none"

	if len(candidates) > 0 {
		// Этап 2: AI-ранжирование
		mlMatches, err := s.mlClient.MatchResumeToVacancies(ctx, resume, candidates)
		if err != nil {
			w.WriteHeader(http.StatusServiceUnavailable)
			json.NewEncoder(w).Encode(map[string]string{
				"error":   "ML service unavailable",
				"details": err.Error(),
			})
			return
		}

		for _, mlMatch := range mlMatches {
			for _, v := range candidates {
				if v.ID == mlMatch.VacancyID {
					recommendations = append(recommendations, Recommendation{
						Vacancy:   v,
						Score:     mlMatch.Score,
						Reasoning: mlMatch.Reasoning,
					})
					break
				}
			}
		}
		modelUsed = "ml-service"
	}

	// Гарантируем не-nil массив для JSON
	if recommendations == nil {
		recommendations = []Recommendation{}
	}

	// ====== СОХРАНЯЕМ В КЭШ ======
	s.recCache.Set(claims.UserID, recommendations, resumeHash, modelUsed)

	json.NewEncoder(w).Encode(map[string]any{
		"resume":          resume,
		"recommendations": recommendations,
		"model_used":      modelUsed,
		"from_cache":      false,
	})
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

	// Получаем текущее резюме
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

	// Валидация
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
		log.Printf("UpdateResume error: %v", err) // ← ДОБАВЛЕНО
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{
			"error":   "failed to update resume",
			"details": err.Error(),
		})
		return
	}

	// Инвалидируем кэш рекомендаций
	s.recCache.Invalidate(claims.UserID)

	// Возвращаем обновлённое резюме
	updatedResume, _ := s.storage.GetResumeByUserID(ctx, claims.UserID)
	json.NewEncoder(w).Encode(updatedResume)
}

func (s *Server) uploadUserPhotoHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		log.Printf("❌ uploadUserPhoto: unauthorized")
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	log.Printf("📤 uploadUserPhoto: user_id=%d", claims.UserID)

	photoURL, err := s.saveUploadedPhoto(r, fmt.Sprintf("user_%d", claims.UserID))
	if err != nil {
		log.Printf("❌ saveUploadedPhoto error: %v", err)
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": err.Error()})
		return
	}

	log.Printf("✅ Photo saved: %s", photoURL)

	if err := s.storage.UpdateUserPhoto(ctx, claims.UserID, photoURL); err != nil {
		log.Printf("❌ UpdateUserPhoto error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to update user"})
		return
	}

	user, _ := s.storage.GetUserByID(ctx, claims.UserID)
	log.Printf("✅ User updated, returning: %+v", user.PhotoURL)
	json.NewEncoder(w).Encode(user)
}

func (s *Server) uploadResumePhotoHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		log.Printf("❌ uploadResumePhoto: unauthorized")
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	log.Printf("📤 uploadResumePhoto: user_id=%d", claims.UserID)

	resume, err := s.storage.GetResumeByUserID(ctx, claims.UserID)
	if err != nil {
		log.Printf("❌ GetResumeByUserID error: %v", err)
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "resume not found"})
		return
	}

	photoURL, err := s.saveUploadedPhoto(r, fmt.Sprintf("resume_%d", resume.ID))
	if err != nil {
		log.Printf("❌ saveUploadedPhoto error: %v", err)
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": err.Error()})
		return
	}

	log.Printf("✅ Photo saved: %s", photoURL)

	if err := s.storage.UpdateResumePhoto(ctx, resume.ID, claims.UserID, photoURL); err != nil {
		log.Printf("❌ UpdateResumePhoto error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to update resume"})
		return
	}

	updatedResume, _ := s.storage.GetResumeByUserID(ctx, claims.UserID)
	log.Printf("✅ Resume updated, returning: %+v", updatedResume.PhotoURL)
	json.NewEncoder(w).Encode(updatedResume)
}

func (s *Server) saveUploadedPhoto(r *http.Request, prefix string) (string, error) {
	// Лимит 5MB
	r.Body = http.MaxBytesReader(nil, r.Body, 5<<20)

	if err := r.ParseMultipartForm(5 << 20); err != nil {
		log.Printf("❌ ParseMultipartForm error: %v", err)
		return "", fmt.Errorf("file too large or invalid format")
	}

	file, header, err := r.FormFile("photo")
	if err != nil {
		log.Printf("❌ FormFile error: %v", err)
		return "", fmt.Errorf("photo field required")
	}
	defer file.Close()

	log.Printf("📎 Received file: %s, size=%d, content-type=%s",
		header.Filename, header.Size, header.Header.Get("Content-Type"))

	// Проверка типа файла
	contentType := header.Header.Get("Content-Type")
	allowedTypes := map[string]string{
		"image/jpeg": ".jpg",
		"image/png":  ".png",
		"image/webp": ".webp",
	}
	ext, ok := allowedTypes[contentType]
	if !ok {
		log.Printf("❌ Unsupported content type: %s", contentType)
		return "", fmt.Errorf("only jpg, png, webp allowed (got: %s)", contentType)
	}

	// Уникальное имя файла
	filename := fmt.Sprintf("%s_%d%s", prefix, time.Now().UnixNano(), ext)

	// Создаём папку uploads
	if err := os.MkdirAll("uploads", 0755); err != nil {
		log.Printf("❌ MkdirAll error: %v", err)
		return "", fmt.Errorf("failed to create uploads dir")
	}

	// Сохраняем файл
	filePath := filepath.Join("uploads", filename)
	dst, err := os.Create(filePath)
	if err != nil {
		log.Printf("❌ Create file error: %v", err)
		return "", fmt.Errorf("failed to create file")
	}
	defer dst.Close()

	written, err := io.Copy(dst, file)
	if err != nil {
		log.Printf("❌ Copy error: %v", err)
		return "", fmt.Errorf("failed to write file")
	}

	log.Printf("✅ File saved to %s (%d bytes)", filePath, written)

	return "/uploads/" + filename, nil
}

func (s *Server) viewResumeHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	resumeID, err := strconv.Atoi(r.PathValue("id"))
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid id"})
		return
	}

	// Проверяем что пользователь не владелец резюме
	claims := getUserFromContext(ctx)
	if claims != nil {
		resume, err := s.storage.GetResumeByID(ctx, resumeID)
		if err == nil && resume.UserID == claims.UserID {
			// Владелец смотрит своё резюме — не считаем
			json.NewEncoder(w).Encode(map[string]string{"status": "skipped", "reason": "self-view"})
			return
		}
	}

	if err := s.storage.IncrementResumeViews(ctx, resumeID); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to increment views"})
		return
	}

	json.NewEncoder(w).Encode(map[string]string{"status": "ok"})
}
