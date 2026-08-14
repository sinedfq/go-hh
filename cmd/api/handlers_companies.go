package main

import (
	"encoding/json"
	"log"
	"net/http"
	"strconv"
)

func (s *Server) getAllCompaniesHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	companies, err := s.storage.GetAllCompanies(ctx)
	if err != nil {
		log.Printf("GetAllCompanies error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to fetch companies"})
		return
	}

	json.NewEncoder(w).Encode(companies)
}

func (s *Server) getCompanyHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	id, err := strconv.Atoi(r.PathValue("id"))
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid company id"})
		return
	}

	company, err := s.storage.GetCompanyByID(ctx, id)
	if err != nil {
		if err.Error() == "company not found" {
			w.WriteHeader(http.StatusNotFound)
			json.NewEncoder(w).Encode(map[string]string{"error": "company not found"})
			return
		}
		log.Printf("GetCompanyByID error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to fetch company"})
		return
	}

	json.NewEncoder(w).Encode(company)
}

func (s *Server) getCompanyVacanciesHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	id, err := strconv.Atoi(r.PathValue("id"))
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid company id"})
		return
	}

	if _, err := s.storage.GetCompanyByID(ctx, id); err != nil {
		if err.Error() == "company not found" {
			w.WriteHeader(http.StatusNotFound)
			json.NewEncoder(w).Encode(map[string]string{"error": "company not found"})
			return
		}
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to fetch company"})
		return
	}

	vacancies, err := s.storage.GetVacanciesByCompanyID(ctx, id)
	if err != nil {
		log.Printf("GetVacanciesByCompanyID error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to fetch vacancies"})
		return
	}

	json.NewEncoder(w).Encode(vacancies)
}
