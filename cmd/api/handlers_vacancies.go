package main

import (
	"encoding/json"
	"log"
	"net/http"
	"strconv"
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

func (s *Server) viewVacancyHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	vacancyID, err := strconv.Atoi(r.PathValue("id"))
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid id"})
		return
	}

	claims := getUserFromContext(ctx)
	if claims != nil {
		vacancy, err := s.storage.GetVacancyByID(ctx, vacancyID)
		if err == nil && vacancy.AuthorUserID == claims.UserID {
			json.NewEncoder(w).Encode(map[string]string{"status": "skipped", "reason": "self-view"})
			return
		}
	}

	if err := s.storage.IncrementVacancyViews(ctx, vacancyID); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to increment views"})
		return
	}

	json.NewEncoder(w).Encode(map[string]string{"status": "ok"})
}

func (s *Server) searchVacanciesHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	query := r.URL.Query().Get("q")
	if query == "" {
		json.NewEncoder(w).Encode([]Vacancy{})
		return
	}

	limit := 10
	if l := r.URL.Query().Get("limit"); l != "" {
		if parsed, err := strconv.Atoi(l); err == nil && parsed > 0 {
			limit = parsed
		}
	}

	vacancies, err := s.storage.SearchVacancies(ctx, query, limit)
	if err != nil {
		log.Printf("SearchVacancies error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "search failed"})
		return
	}

	json.NewEncoder(w).Encode(vacancies)
}
