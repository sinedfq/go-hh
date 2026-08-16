package main

import (
	"encoding/json"
	"log"
	"net/http"
)

// GET /api/employer/stats — статистика компании работодателя
func (s *Server) getEmployerStatsHandler(w http.ResponseWriter, r *http.Request) {
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
		log.Printf("⚠️ getEmployerStats: GetUserByID error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to get user"})
		return
	}

	log.Printf("📊 getEmployerStats: user %d (role=%s, company_id=%v)",
		claims.UserID, claims.Role, user.CompanyID)

	if user.CompanyID == nil {
		log.Printf("⚠️ getEmployerStats: user has no company")
		json.NewEncoder(w).Encode(EmployerStats{})
		return
	}

	stats, err := s.storage.GetEmployerStats(ctx, *user.CompanyID)
	if err != nil {
		log.Printf("⚠️ getEmployerStats: error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to get stats"})
		return
	}

	// ====== ИСПРАВЛЕНО: используем TotalVacancyViews вместо TotalViews ======
	log.Printf("✅ Employer stats loaded for company %d: %d vacancies, %d views, %d apps, %d resume views",
		*user.CompanyID,
		stats.TotalVacancies,
		stats.TotalVacancyViews, // ← БЫЛО stats.TotalViews
		stats.TotalApplications,
		stats.TotalResumeViews)

	json.NewEncoder(w).Encode(stats)
}

// GET /api/my-resume/stats — статистика моего резюме (для кандидата)
func (s *Server) getMyResumeStatsHandler(w http.ResponseWriter, r *http.Request) {
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
		// Резюме нет — возвращаем пустую статистику (не ошибка!)
		json.NewEncoder(w).Encode(ResumeStats{TotalViews: 0})
		return
	}

	stats, err := s.storage.GetResumeStats(ctx, resume.ID)
	if err != nil {
		log.Printf("⚠️ getMyResumeStats: error: %v", err)
		json.NewEncoder(w).Encode(ResumeStats{TotalViews: 0})
		return
	}

	log.Printf("✅ Resume stats for user %d: %d views", claims.UserID, stats.TotalViews)
	json.NewEncoder(w).Encode(stats)
}
