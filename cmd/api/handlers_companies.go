package main

import (
	"encoding/json"
	"net/http"
	"strconv"
	"strings"
)

// ====== ПУБЛИЧНЫЕ ХЕНДЛЕРЫ КОМПАНИЙ ======

// GET /api/companies — список всех компаний
func (s *Server) getAllCompaniesHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	companies, err := s.storage.GetAllCompanies(ctx)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to get companies"})
		return
	}

	json.NewEncoder(w).Encode(companies)
}

// GET /api/companies/{id} — получить компанию по ID
func (s *Server) getCompanyHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")

	// Извлекаем ID из пути /api/companies/123
	parts := strings.Split(r.URL.Path, "/")
	idStr := parts[len(parts)-1]
	id, err := strconv.Atoi(idStr)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid id"})
		return
	}

	company, err := s.storage.GetCompanyByID(r.Context(), id)
	if err != nil {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "company not found"})
		return
	}

	json.NewEncoder(w).Encode(company)
}

// GET /api/companies/{id}/vacancies — вакансии конкретной компании
func (s *Server) getCompanyVacanciesHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")

	// Путь: /api/companies/123/vacancies — нужен второй сегмент
	parts := strings.Split(r.URL.Path, "/")
	// parts: ["", "api", "companies", "123", "vacancies"]
	if len(parts) < 5 {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid path"})
		return
	}

	idStr := parts[3]
	id, err := strconv.Atoi(idStr)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid id"})
		return
	}

	vacancies, err := s.storage.GetVacanciesByCompanyID(r.Context(), id)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to get vacancies"})
		return
	}

	json.NewEncoder(w).Encode(vacancies)
}

// POST /api/companies — создание компании работодателем
func (s *Server) createCompanyHandler(w http.ResponseWriter, r *http.Request) {
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

	if user.CompanyID != nil {
		w.WriteHeader(http.StatusConflict)
		json.NewEncoder(w).Encode(map[string]string{"error": "you already have a company"})
		return
	}

	var req CreateCompanyRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid json"})
		return
	}

	if req.Name == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "company name required"})
		return
	}

	companyID, err := s.storage.CreateCompany(ctx, req, claims.UserID)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to create company"})
		return
	}

	company, _ := s.storage.GetCompanyByID(ctx, companyID)

	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(company)
}

// GET /api/my-company — получить свою компанию
func (s *Server) getMyCompanyHandler(w http.ResponseWriter, r *http.Request) {
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
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "company not found"})
		return
	}

	company, err := s.storage.GetCompanyByID(ctx, *user.CompanyID)
	if err != nil {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "company not found"})
		return
	}

	json.NewEncoder(w).Encode(company)
}

// GET /api/my-vacancies — вакансии моей компании
func (s *Server) getMyVacanciesHandler(w http.ResponseWriter, r *http.Request) {
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
		w.WriteHeader(http.StatusOK)
		json.NewEncoder(w).Encode(MyVacanciesResponse{
			Vacancies: []Vacancy{},
		})
		return
	}

	company, err := s.storage.GetCompanyByID(ctx, *user.CompanyID)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to get company"})
		return
	}

	vacancies, err := s.storage.GetMyVacancies(ctx, *user.CompanyID)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to get vacancies"})
		return
	}

	json.NewEncoder(w).Encode(MyVacanciesResponse{
		Company:   company.Company,
		Vacancies: vacancies,
	})
}

// GET /api/vacancies/{id} — получить одну вакансию
func (s *Server) getVacancyByIDHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")

	// Извлекаем ID из пути /api/vacancies/123
	parts := strings.Split(r.URL.Path, "/")
	idStr := parts[len(parts)-1]
	id, err := strconv.Atoi(idStr)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid id"})
		return
	}

	vacancy, err := s.storage.GetVacancyByID(r.Context(), id)
	if err != nil {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "vacancy not found"})
		return
	}

	s.storage.IncrementVacancyViews(r.Context(), id)

	json.NewEncoder(w).Encode(vacancy)
}
