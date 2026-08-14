package main

import (
	"encoding/json"
	"log"
	"net/http"
	"strconv"
)

func (s *Server) matchesHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	idParam := r.PathValue("id")
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

	candidates, err := s.storage.GetCandidateVacancies(ctx, resume.Skills, resume.DesiredPosition, resume.Experience, 30)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "internal error"})
		return
	}

	mlMatches, err := s.mlClient.MatchResumeToVacancies(ctx, resume, candidates)
	if err != nil {
		w.WriteHeader(http.StatusServiceUnavailable)
		json.NewEncoder(w).Encode(map[string]string{
			"error":   "ML service unavailable",
			"details": err.Error(),
		})
		return
	}

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

func (s *Server) recommendationsHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		log.Printf("❌ recommendations: unauthorized")
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	log.Printf("📋 Recommendations request from user %d", claims.UserID)

	resume, err := s.storage.GetResumeByUserID(ctx, claims.UserID)
	if err != nil {
		log.Printf("❌ GetResumeByUserID error: %v", err)
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "resume not found"})
		return
	}

	log.Printf("✅ Resume loaded: %s, skills=%v", resume.FullName, resume.Skills)

	resumeHash := HashResume(resume)
	forceRefresh := r.URL.Query().Get("refresh") == "true"

	if !forceRefresh {
		if cached, ok := s.recCache.Get(claims.UserID, resumeHash); ok {
			w.Header().Set("X-Cache", "HIT")
			log.Printf("✅ Cache HIT for user %d (%d recs)", claims.UserID, len(cached.Recommendations))
			json.NewEncoder(w).Encode(map[string]any{
				"resume":          resume,
				"recommendations": cached.Recommendations,
				"model_used":      cached.ModelUsed,
				"from_cache":      true,
			})
			return
		}
	}

	w.Header().Set("X-Cache", "MISS")
	log.Printf("🔄 Cache MISS for user %d, computing recommendations", claims.UserID)

	const candidateLimit = 15
	candidates, err := s.storage.GetCandidateVacancies(ctx, resume.Skills, resume.DesiredPosition, resume.Experience, candidateLimit)
	if err != nil {
		log.Printf("❌ GetCandidateVacancies error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{
			"error":   "failed to fetch candidates",
			"details": err.Error(),
		})
		return
	}

	log.Printf("✅ Found %d candidates", len(candidates))

	var recommendations []Recommendation
	modelUsed := "none"

	if len(candidates) > 0 {
		mlMatches, err := s.mlClient.MatchResumeToVacancies(ctx, resume, candidates)
		if err != nil {
			log.Printf("❌ ML service error: %v", err)
			w.WriteHeader(http.StatusServiceUnavailable)
			json.NewEncoder(w).Encode(map[string]string{
				"error":   "ML service unavailable",
				"details": err.Error(),
			})
			return
		}

		log.Printf("✅ ML returned %d matches", len(mlMatches))

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

	if recommendations == nil {
		recommendations = []Recommendation{}
	}

	log.Printf("✅ Returning %d recommendations for user %d", len(recommendations), claims.UserID)

	s.recCache.Set(claims.UserID, recommendations, resumeHash, modelUsed)

	json.NewEncoder(w).Encode(map[string]any{
		"resume":          resume,
		"recommendations": recommendations,
		"model_used":      modelUsed,
		"from_cache":      false,
	})
}
