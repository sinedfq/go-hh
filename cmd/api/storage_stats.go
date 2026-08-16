package main

import (
	"context"
)

type EmployerStats struct {
	TotalVacancies       int     `json:"total_vacancies"`
	TotalVacancyViews    int     `json:"total_vacancy_views"` // ← ИЗМЕНЕНО с total_views
	ViewsThisWeek        int     `json:"views_this_week"`
	ViewsThisMonth       int     `json:"views_this_month"`
	TotalApplications    int     `json:"total_applications"`
	NewApplications      int     `json:"new_applications"`
	ViewedApplications   int     `json:"viewed_applications"`
	AcceptedApplications int     `json:"accepted_applications"`
	RejectedApplications int     `json:"rejected_applications"`
	TotalResumeViews     int     `json:"total_resume_views"` // ← ДОБАВЛЕНО
	ConversionRate       float64 `json:"conversion_rate"`    // ← ДОБАВЛЕНО
}

type ResumeStats struct {
	TotalViews     int `json:"total_views"`
	ViewsThisWeek  int `json:"views_this_week"`
	ViewsThisMonth int `json:"views_this_month"`
}

func (s *PostgresStorage) GetEmployerStats(ctx context.Context, companyID int) (*EmployerStats, error) {
	stats := &EmployerStats{}

	// Количество вакансий
	s.pool.QueryRow(ctx, `SELECT COUNT(*) FROM vacancies WHERE company_id = $1`, companyID).Scan(&stats.TotalVacancies)

	// Суммарные просмотры вакансий
	s.pool.QueryRow(ctx, `SELECT COALESCE(SUM(views), 0) FROM vacancies WHERE company_id = $1`, companyID).Scan(&stats.TotalVacancyViews)

	// Примерные значения
	stats.ViewsThisWeek = stats.TotalVacancyViews
	stats.ViewsThisMonth = stats.TotalVacancyViews

	// Отклики — всего
	s.pool.QueryRow(ctx, `
		SELECT COUNT(*) FROM applications a
		JOIN vacancies v ON v.id = a.vacancy_id
		WHERE v.company_id = $1
	`, companyID).Scan(&stats.TotalApplications)

	// Новые
	s.pool.QueryRow(ctx, `
		SELECT COUNT(*) FROM applications a
		JOIN vacancies v ON v.id = a.vacancy_id
		WHERE v.company_id = $1 AND a.status = 'new'
	`, companyID).Scan(&stats.NewApplications)

	// Просмотренные
	s.pool.QueryRow(ctx, `
		SELECT COUNT(*) FROM applications a
		JOIN vacancies v ON v.id = a.vacancy_id
		WHERE v.company_id = $1 AND a.status = 'viewed'
	`, companyID).Scan(&stats.ViewedApplications)

	// Принятые
	s.pool.QueryRow(ctx, `
		SELECT COUNT(*) FROM applications a
		JOIN vacancies v ON v.id = a.vacancy_id
		WHERE v.company_id = $1 AND a.status = 'accepted'
	`, companyID).Scan(&stats.AcceptedApplications)

	// Отклонённые
	s.pool.QueryRow(ctx, `
		SELECT COUNT(*) FROM applications a
		JOIN vacancies v ON v.id = a.vacancy_id
		WHERE v.company_id = $1 AND a.status = 'rejected'
	`, companyID).Scan(&stats.RejectedApplications)

	// Просмотры резюме кандидатов откликнувшихся на вакансии компании
	s.pool.QueryRow(ctx, `
		SELECT COALESCE(SUM(r.views), 0)
		FROM applications a
		JOIN vacancies v ON v.id = a.vacancy_id
		LEFT JOIN resumes r ON r.id = a.resume_id
		WHERE v.company_id = $1 AND a.resume_id IS NOT NULL
	`, companyID).Scan(&stats.TotalResumeViews)

	// Конверсия
	if stats.TotalVacancyViews > 0 {
		stats.ConversionRate = float64(stats.TotalApplications) / float64(stats.TotalVacancyViews) * 100
	}

	return stats, nil
}

// GetResumeStats — статистика резюме
func (s *PostgresStorage) GetResumeStats(ctx context.Context, resumeID int) (*ResumeStats, error) {
	stats := &ResumeStats{}

	err := s.pool.QueryRow(ctx, `SELECT COALESCE(views, 0) FROM resumes WHERE id = $1`, resumeID).Scan(&stats.TotalViews)
	if err != nil {
		return stats, err
	}

	stats.ViewsThisWeek = stats.TotalViews
	stats.ViewsThisMonth = stats.TotalViews

	return stats, nil
}
