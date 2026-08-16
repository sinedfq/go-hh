package main

import (
	"encoding/json"
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

	if req.Title == "" || req.Location == "" || req.Experience == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "title, location and experience required"})
		return
	}

	vacancy := Vacancy{
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
		Query:      strings.TrimSpace(r.URL.Query().Get("q")),
		Location:   strings.TrimSpace(r.URL.Query().Get("location")),
		Experience: strings.TrimSpace(r.URL.Query().Get("experience")),
		Limit:      50,
		Offset:     0,
	}

	// Парсим remote
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
