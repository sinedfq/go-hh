package main

import (
	"context"
	"errors"
	"log"
	"strings"

	"github.com/jackc/pgx/v5"
)

// ============ ВАКАНСИИ ============

func (s *PostgresStorage) GetAllVacancies(ctx context.Context) ([]Vacancy, error) {
	query := `
		SELECT id, title, company, COALESCE(company_id, 0), location, experience, remote, skills, description,
		       COALESCE(address, ''), COALESCE(latitude, 0), COALESCE(longitude, 0)
		FROM vacancies
		ORDER BY created_at DESC
	`

	rows, err := s.pool.Query(ctx, query)
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
			&v.Address, &v.Latitude, &v.Longitude,
		)
		if err != nil {
			return nil, err
		}
		vacancies = append(vacancies, v)
	}

	return vacancies, rows.Err()
}

func (s *PostgresStorage) GetVacancyByID(ctx context.Context, id int) (Vacancy, error) {
	query := `
		SELECT id, title, company, COALESCE(company_id, 0), location, experience, remote, skills, description,
		       COALESCE(address, ''), COALESCE(latitude, 0), COALESCE(longitude, 0)
		FROM vacancies
		WHERE id = $1
	`

	var v Vacancy
	err := s.pool.QueryRow(ctx, query, id).Scan(
		&v.ID, &v.Title, &v.Company, &v.CompanyID, &v.Location,
		&v.Experience, &v.Remote, &v.Skills, &v.Description,
		&v.Address, &v.Latitude, &v.Longitude,
	)

	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return Vacancy{}, errors.New("vacancy not found")
		}
		return Vacancy{}, err
	}

	return v, nil
}

func (s *PostgresStorage) CreateVacancy(ctx context.Context, v Vacancy) (int, error) {
	query := `
		INSERT INTO vacancies (title, company, location, experience, remote, skills, description)
		VALUES ($1, $2, $3, $4, $5, $6, $7)
		RETURNING id
	`

	var id int
	err := s.pool.QueryRow(ctx, query,
		v.Title, v.Company, v.Location, v.Experience, v.Remote,
		v.Skills, v.Description,
	).Scan(&id)

	if err != nil {
		return 0, err
	}

	return id, nil
}

func (s *PostgresStorage) IncrementVacancyViews(ctx context.Context, vacancyID int) error {
	query := `UPDATE vacancies SET views = views + 1 WHERE id = $1`
	_, err := s.pool.Exec(ctx, query, vacancyID)
	return err
}

func (s *PostgresStorage) SearchVacancies(ctx context.Context, query string, limit int) ([]Vacancy, error) {
	sql := `
		SELECT id, title, company, COALESCE(company_id, 0), location, experience, remote, skills, description,
		       COALESCE(address, ''), COALESCE(latitude, 0), COALESCE(longitude, 0), COALESCE(views, 0),
		       COALESCE(author_user_id, 0)
		FROM vacancies
		WHERE LOWER(title) LIKE LOWER($1)
		ORDER BY created_at DESC
		LIMIT $2
	`

	rows, err := s.pool.Query(ctx, sql, "%"+query+"%", limit)
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
			&v.Address, &v.Latitude, &v.Longitude, &v.Views, &v.AuthorUserID,
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

// ============ КАНДИДАТЫ ДЛЯ РЕКОМЕНДАЦИЙ ============

func (s *PostgresStorage) GetCandidateVacancies(ctx context.Context, skills []string, desiredPosition string, experience string, limit int) ([]Vacancy, error) {
	if len(skills) == 0 {
		return []Vacancy{}, nil
	}

	lowerSkills := make([]string, len(skills))
	for i, sk := range skills {
		lowerSkills[i] = strings.ToLower(strings.TrimSpace(sk))
	}

	roleGroup := getRoleGroup(desiredPosition)
	experienceLevel := getExperienceLevel(experience)

	query := `
		WITH scored AS (
			SELECT v.id, v.title, v.company, 
			       COALESCE(v.company_id, 0) AS company_id,
			       v.location, v.experience, v.remote,
			       v.skills, v.description, 
			       COALESCE(v.address, '') AS address, 
			       COALESCE(v.latitude, 0) AS latitude, 
			       COALESCE(v.longitude, 0) AS longitude, 
			       COALESCE(v.views, 0) AS views,
			       v.created_at,
			       COALESCE((
			           SELECT COUNT(*)
			           FROM unnest(v.skills) AS skill
			           WHERE LOWER(skill) = ANY($1)
			       ), 0) AS skill_overlap,
			       CASE 
			           WHEN $2 = '' THEN TRUE
			           WHEN LOWER(v.title) LIKE ANY($3) THEN TRUE
			           ELSE FALSE
			       END AS role_compatible,
			       CASE
			           WHEN $4 = 'junior' AND LOWER(v.experience) IN ('junior', 'middle') THEN TRUE
			           WHEN $4 = 'middle' AND LOWER(v.experience) IN ('junior', 'middle', 'senior') THEN TRUE
			           WHEN $4 = 'senior' THEN TRUE
			           ELSE FALSE
			       END AS experience_compatible
			FROM vacancies v
		)
		SELECT id, title, company, company_id, location, experience, remote, skills, description,
		       address, latitude, longitude, views, skill_overlap
		FROM scored
		WHERE skill_overlap > 0 
		  AND (role_compatible OR $2 = '')
		  AND (experience_compatible OR $4 = '')
		ORDER BY skill_overlap DESC, created_at DESC
		LIMIT $5
	`

	var roleKeywords []string
	if roleGroup == "backend" {
		roleKeywords = []string{"%backend%", "%go%", "%golang%", "%python%", "%java%", "%server%"}
	} else if roleGroup == "frontend" {
		roleKeywords = []string{"%frontend%", "%react%", "%vue%", "%angular%", "%javascript%"}
	} else if roleGroup == "fullstack" {
		roleKeywords = []string{"%fullstack%", "%full-stack%", "%frontend%", "%backend%"}
	} else if roleGroup == "devops" {
		roleKeywords = []string{"%devops%", "%sre%", "%infrastructure%", "%platform%"}
	} else if roleGroup == "data" {
		roleKeywords = []string{"%data%", "%ml%", "%machine learning%", "%bi%"}
	} else {
		roleKeywords = []string{}
	}

	rows, err := s.pool.Query(ctx, query, lowerSkills, roleGroup, roleKeywords, experienceLevel, limit)
	if err != nil {
		log.Printf("GetCandidateVacancies query error: %v", err)
		return nil, err
	}
	defer rows.Close()

	var vacancies []Vacancy
	for rows.Next() {
		var v Vacancy
		var overlap int
		err := rows.Scan(
			&v.ID, &v.Title, &v.Company, &v.CompanyID, &v.Location,
			&v.Experience, &v.Remote, &v.Skills, &v.Description,
			&v.Address, &v.Latitude, &v.Longitude, &v.Views, &overlap,
		)
		if err != nil {
			log.Printf("GetCandidateVacancies scan error: %v", err)
			return nil, err
		}
		vacancies = append(vacancies, v)
	}

	if vacancies == nil {
		vacancies = []Vacancy{}
	}

	return vacancies, rows.Err()
}

// ============ ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ============

func getRoleGroup(position string) string {
	pos := strings.ToLower(position)

	backendKeywords := []string{"backend", "go", "golang", "python", "java", "c#", "c++", "php", "ruby", "rust"}
	for _, kw := range backendKeywords {
		if strings.Contains(pos, kw) {
			return "backend"
		}
	}

	frontendKeywords := []string{"frontend", "front-end", "react", "vue", "angular", "javascript"}
	for _, kw := range frontendKeywords {
		if strings.Contains(pos, kw) {
			return "frontend"
		}
	}

	if strings.Contains(pos, "fullstack") || strings.Contains(pos, "full-stack") {
		return "fullstack"
	}

	devopsKeywords := []string{"devops", "sre", "infrastructure", "platform"}
	for _, kw := range devopsKeywords {
		if strings.Contains(pos, kw) {
			return "devops"
		}
	}

	dataKeywords := []string{"data", "ml", "machine learning", "bi analyst"}
	for _, kw := range dataKeywords {
		if strings.Contains(pos, kw) {
			return "data"
		}
	}

	return ""
}

func getExperienceLevel(experience string) string {
	exp := strings.ToLower(experience)
	if strings.Contains(exp, "junior") {
		return "junior"
	}
	if strings.Contains(exp, "middle") {
		return "middle"
	}
	if strings.Contains(exp, "senior") || strings.Contains(exp, "lead") {
		return "senior"
	}
	return ""
}
