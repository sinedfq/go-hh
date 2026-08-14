package main

import (
	"context"
	"errors"

	"github.com/jackc/pgx/v5"
)

// ============ КОМПАНИИ ============

func (s *PostgresStorage) GetAllCompanies(ctx context.Context) ([]CompanyWithStats, error) {
	query := `
		SELECT c.id, c.name, COALESCE(c.description, ''), COALESCE(c.industry, ''),
		       COALESCE(c.size, ''), COALESCE(c.city, ''), COALESCE(c.website, ''),
		       COALESCE(c.logo_url, ''), COALESCE(c.photo_url, ''), c.created_at,
		       COUNT(v.id) AS vacancies_count,
		       COALESCE(SUM(v.views), 0) AS total_views
		FROM companies c
		LEFT JOIN vacancies v ON v.company_id = c.id
		GROUP BY c.id
		ORDER BY vacancies_count DESC, c.name ASC
	`

	rows, err := s.pool.Query(ctx, query)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var companies []CompanyWithStats
	for rows.Next() {
		var c CompanyWithStats
		err := rows.Scan(
			&c.ID, &c.Name, &c.Description, &c.Industry,
			&c.Size, &c.City, &c.Website, &c.LogoURL, &c.PhotoURL, &c.CreatedAt,
			&c.VacanciesCount, &c.TotalViews,
		)
		if err != nil {
			return nil, err
		}
		companies = append(companies, c)
	}

	if companies == nil {
		companies = []CompanyWithStats{}
	}

	return companies, rows.Err()
}

func (s *PostgresStorage) GetCompanyByID(ctx context.Context, id int) (CompanyWithStats, error) {
	query := `
		SELECT c.id, c.name, COALESCE(c.description, ''), COALESCE(c.industry, ''),
		       COALESCE(c.size, ''), COALESCE(c.city, ''), COALESCE(c.website, ''),
		       COALESCE(c.logo_url, ''), COALESCE(c.photo_url, ''), c.created_at,
		       COUNT(v.id) AS vacancies_count,
		       COALESCE(SUM(v.views), 0) AS total_views
		FROM companies c
		LEFT JOIN vacancies v ON v.company_id = c.id
		WHERE c.id = $1
		GROUP BY c.id
	`

	var c CompanyWithStats
	err := s.pool.QueryRow(ctx, query, id).Scan(
		&c.ID, &c.Name, &c.Description, &c.Industry,
		&c.Size, &c.City, &c.Website, &c.LogoURL, &c.PhotoURL, &c.CreatedAt,
		&c.VacanciesCount, &c.TotalViews,
	)

	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return CompanyWithStats{}, errors.New("company not found")
		}
		return CompanyWithStats{}, err
	}

	return c, nil
}

func (s *PostgresStorage) GetVacanciesByCompanyID(ctx context.Context, companyID int) ([]Vacancy, error) {
	query := `
		SELECT id, title, company, COALESCE(company_id, 0), location, experience, remote, skills, description,
		       COALESCE(address, ''), COALESCE(latitude, 0), COALESCE(longitude, 0), COALESCE(views, 0)
		FROM vacancies
		WHERE company_id = $1
		ORDER BY created_at DESC
	`

	rows, err := s.pool.Query(ctx, query, companyID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var vacancies []Vacancy
	for rows.Next() {
		var v Vacancy
		err := rows.Scan(
			&v.ID, &v.Title, &v.Company, &v.CompanyID, &v.Location,
			&v.Experience, &v.Remote, &v.Skills, &v.Description,
			&v.Address, &v.Latitude, &v.Longitude, &v.Views,
		)
		if err != nil {
			return nil, err
		}
		vacancies = append(vacancies, v)
	}

	if vacancies == nil {
		vacancies = []Vacancy{}
	}

	return vacancies, rows.Err()
}
