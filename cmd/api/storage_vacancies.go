package main

import (
	"context"
	"errors"
	"fmt"
	"log"
	"strings"

	"github.com/jackc/pgx/v5"
)

// ============ ВАКАНСИИ ============

func (s *PostgresStorage) GetAllVacancies(ctx context.Context) ([]Vacancy, error) {
	query := `
		SELECT id, title, company, COALESCE(company_id, 0), location, experience, remote, skills, description,
		       COALESCE(address, ''), COALESCE(latitude, 0), COALESCE(longitude, 0),
		       COALESCE(views, 0), COALESCE(author_user_id, 0),
		       COALESCE(is_archived, FALSE), created_at, updated_at,
		       COALESCE((SELECT c.logo_url FROM companies c WHERE c.id=vacancies.company_id), ''), COALESCE((SELECT c.profile->>'it_accredited' FROM companies c WHERE c.id=vacancies.company_id),'false')='true', terms
		FROM vacancies
		WHERE is_archived = FALSE
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
			&v.Views, &v.AuthorUserID, &v.IsArchived,
			&v.CreatedAt, &v.UpdatedAt, &v.CompanyLogo, &v.CompanyITAccredited, &v.VacancyTerms,
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

func (s *PostgresStorage) GetVacancyByID(ctx context.Context, id int) (Vacancy, error) {
	query := `
		SELECT id, title, company, COALESCE(company_id, 0), location, experience, remote, skills, description,
		       COALESCE(address, ''), COALESCE(latitude, 0), COALESCE(longitude, 0), COALESCE(views, 0),
		       COALESCE(author_user_id, 0), COALESCE(is_archived, FALSE), created_at, updated_at,
		       COALESCE((SELECT c.logo_url FROM companies c WHERE c.id=vacancies.company_id), ''), COALESCE((SELECT c.profile->>'it_accredited' FROM companies c WHERE c.id=vacancies.company_id),'false')='true', terms
		FROM vacancies
		WHERE id = $1
	`

	var v Vacancy
	err := s.pool.QueryRow(ctx, query, id).Scan(
		&v.ID, &v.Title, &v.Company, &v.CompanyID, &v.Location,
		&v.Experience, &v.Remote, &v.Skills, &v.Description,
		&v.Address, &v.Latitude, &v.Longitude, &v.Views, &v.AuthorUserID,
		&v.IsArchived, &v.CreatedAt, &v.UpdatedAt, &v.CompanyLogo, &v.CompanyITAccredited, &v.VacancyTerms,
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
		INSERT INTO vacancies (title, company, company_id, location, experience, remote, skills, description, address, latitude, longitude, views, author_user_id, terms)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, 0, $12, $13)
		RETURNING id
	`

	var id int
	err := s.pool.QueryRow(ctx, query,
		v.Title, v.Company, v.CompanyID, v.Location, v.Experience,
		v.Remote, v.Skills, v.Description, v.Address,
		v.Latitude, v.Longitude, v.AuthorUserID, v.VacancyTerms.json(),
	).Scan(&id)
	if err != nil {
		return 0, err
	}

	return id, nil
}

// ============ НОВОЕ: ОБНОВЛЕНИЕ ============
func (s *PostgresStorage) UpdateVacancy(ctx context.Context, vacancyID int, companyID int, req CreateVacancyRequest) error {
	query := `
		UPDATE vacancies
		SET title = $1, location = $2, experience = $3, remote = $4,
		    skills = $5, description = $6, address = $7,
		    latitude = $8, longitude = $9, updated_at = CURRENT_TIMESTAMP, terms=$12
		WHERE id = $10 AND company_id = $11
	`
	result, err := s.pool.Exec(ctx, query,
		req.Title, req.Location, req.Experience, req.Remote,
		req.Skills, req.Description, req.Address,
		req.Latitude, req.Longitude,
		vacancyID, companyID, req.VacancyTerms.json(),
	)
	if err != nil {
		return err
	}
	if result.RowsAffected() == 0 {
		return errors.New("vacancy not found or access denied")
	}
	return nil
}

// ============ НОВОЕ: УДАЛЕНИЕ С КАСКАДОМ ============
func (s *PostgresStorage) DeleteVacancy(ctx context.Context, vacancyID int, companyID int) error {
	// Удаляем сообщения → чаты → отклики → избранное → вакансию
	_, _ = s.pool.Exec(ctx, `
		DELETE FROM messages WHERE conversation_id IN (
			SELECT id FROM conversations WHERE application_id IN (
				SELECT id FROM applications WHERE vacancy_id = $1
			)
		)
	`, vacancyID)

	_, _ = s.pool.Exec(ctx, `
		DELETE FROM conversations WHERE application_id IN (
			SELECT id FROM applications WHERE vacancy_id = $1
		)
	`, vacancyID)

	_, _ = s.pool.Exec(ctx, `DELETE FROM applications WHERE vacancy_id = $1`, vacancyID)
	_, _ = s.pool.Exec(ctx, `DELETE FROM favorites WHERE vacancy_id = $1`, vacancyID)

	result, err := s.pool.Exec(ctx, `
		DELETE FROM vacancies WHERE id = $1 AND company_id = $2
	`, vacancyID, companyID)
	if err != nil {
		return err
	}
	if result.RowsAffected() == 0 {
		return errors.New("vacancy not found or access denied")
	}
	return nil
}

// ============ НОВОЕ: АРХИВАЦИЯ ============
func (s *PostgresStorage) ArchiveVacancy(ctx context.Context, vacancyID int, companyID int, archive bool) error {
	result, err := s.pool.Exec(ctx, `
		UPDATE vacancies SET is_archived = $1, updated_at = CURRENT_TIMESTAMP
		WHERE id = $2 AND company_id = $3
	`, archive, vacancyID, companyID)
	if err != nil {
		return err
	}
	if result.RowsAffected() == 0 {
		return errors.New("vacancy not found or access denied")
	}
	return nil
}

func (s *PostgresStorage) IncrementVacancyViews(ctx context.Context, vacancyID int) error {
	query := `UPDATE vacancies SET views = views + 1 WHERE id = $1`
	_, err := s.pool.Exec(ctx, query, vacancyID)
	return err
}

func (s *PostgresStorage) SearchVacancies(ctx context.Context, filters SearchFilters) ([]Vacancy, int, error) {
	var conditions []string
	var args []interface{}
	argIndex := 1

	// ВАЖНО: исключаем архивные
	conditions = append(conditions, `(is_archived = FALSE OR is_archived IS NULL)`)

	baseQuery := `
		SELECT id, title, company, COALESCE(company_id, 0), location, experience, remote, skills, description,
		       COALESCE(address, ''), COALESCE(latitude, 0), COALESCE(longitude, 0), COALESCE(views, 0),
		       COALESCE(author_user_id, 0), COALESCE(is_archived, FALSE), created_at, updated_at,
		       COALESCE((SELECT c.logo_url FROM companies c WHERE c.id=vacancies.company_id), ''), COALESCE((SELECT c.profile->>'it_accredited' FROM companies c WHERE c.id=vacancies.company_id),'false')='true', terms
		FROM vacancies
	`

	if filters.Query != "" {
		q := "%" + strings.ToLower(filters.Query) + "%"
		conditions = append(conditions, fmt.Sprintf(
			`(LOWER(title) LIKE $%d OR LOWER(company) LIKE $%d OR LOWER(description) LIKE $%d)`,
			argIndex, argIndex, argIndex,
		))
		args = append(args, q)
		argIndex++
	}

	if filters.Location != "" {
		conditions = append(conditions, fmt.Sprintf(`LOWER(location) = LOWER($%d)`, argIndex))
		args = append(args, filters.Location)
		argIndex++
	}

	if filters.Experience != "" {
		values := strings.Split(strings.ToLower(filters.Experience), "|")
		for i := range values {
			values[i] = strings.TrimSpace(values[i])
		}
		conditions = append(conditions, fmt.Sprintf(`LOWER(TRIM(experience)) = ANY($%d)`, argIndex))
		args = append(args, values)
		argIndex++
	}
	if filters.Company != "" {
		conditions = append(conditions, fmt.Sprintf(`LOWER(company) = LOWER($%d)`, argIndex))
		args = append(args, filters.Company)
		argIndex++
	}
	if filters.HasCoordinates {
		conditions = append(conditions, `latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180 AND NOT (latitude = 0 AND longitude = 0)`)
	}

	if filters.Remote != nil {
		conditions = append(conditions, fmt.Sprintf(`remote = $%d`, argIndex))
		args = append(args, *filters.Remote)
		argIndex++
	}

	if len(filters.Skills) > 0 {
		lowerSkills := make([]string, len(filters.Skills))
		for i, sk := range filters.Skills {
			lowerSkills[i] = strings.ToLower(sk)
		}
		conditions = append(conditions, fmt.Sprintf(
			`EXISTS (SELECT 1 FROM unnest(skills) AS skill WHERE LOWER(skill) = ANY($%d))`,
			argIndex,
		))
		args = append(args, lowerSkills)
		argIndex++
	}

	whereClause := " WHERE " + strings.Join(conditions, " AND ")
	add := func(condition string, value interface{}) {
		conditions = append(conditions, fmt.Sprintf(condition, argIndex))
		args = append(args, value)
		argIndex++
	}
	if filters.SalaryOnly || filters.SalaryMin > 0 || filters.SalaryMax > 0 {
		conditions = append(conditions, `(COALESCE((terms->>'salary_from')::BIGINT,0)>0 OR COALESCE((terms->>'salary_to')::BIGINT,0)>0)`)
	}
	if filters.SalaryMin > 0 {
		add(`COALESCE(NULLIF((terms->>'salary_to')::BIGINT,0),NULLIF((terms->>'salary_from')::BIGINT,0),0)>=$%d`, filters.SalaryMin)
	}
	if filters.SalaryMax > 0 {
		add(`COALESCE(NULLIF((terms->>'salary_from')::BIGINT,0),NULLIF((terms->>'salary_to')::BIGINT,0),0)<=$%d`, filters.SalaryMax)
	}
	if filters.Currency != "" || filters.SalaryMin > 0 || filters.SalaryMax > 0 {
		currency := filters.Currency
		if currency == "" {
			currency = "RUB"
		}
		add(`COALESCE(NULLIF(terms->>'currency',''),'RUB')=$%d`, currency)
	}
	if filters.Employment != "" {
		add(`terms->>'employment'=$%d`, filters.Employment)
	}
	if filters.Schedule != "" {
		add(`terms->>'schedule'=$%d`, filters.Schedule)
	}
	if filters.NewAfter != nil {
		add(`created_at>$%d`, *filters.NewAfter)
	}
	if filters.ExcludeSearchID > 0 {
		add(`NOT EXISTS(SELECT 1 FROM saved_search_matches m WHERE m.vacancy_id=vacancies.id AND m.search_id=$%d)`, filters.ExcludeSearchID)
	}
	whereClause = " WHERE " + strings.Join(conditions, " AND ")

	countQuery := `SELECT COUNT(*) FROM vacancies` + whereClause
	var totalCount int
	countArgs := make([]interface{}, len(args))
	copy(countArgs, args)
	if err := s.pool.QueryRow(ctx, countQuery, countArgs...).Scan(&totalCount); err != nil {
		return nil, 0, err
	}

	limit := filters.Limit
	if limit <= 0 {
		limit = 50
	}
	offset := filters.Offset

	order := `created_at DESC, id DESC`
	switch filters.Sort {
	case "oldest":
		order = `created_at ASC, id ASC`
	case "popular":
		order = `COALESCE(views, 0) DESC, id DESC`
	case "title":
		order = `LOWER(title) ASC, id DESC`
	}
	finalQuery := baseQuery + whereClause +
		` ORDER BY ` + order +
		fmt.Sprintf(` LIMIT $%d OFFSET $%d`, argIndex, argIndex+1)
	args = append(args, limit, offset)

	rows, err := s.pool.Query(ctx, finalQuery, args...)
	if err != nil {
		log.Printf("SearchVacancies error: %v, query: %s", err, finalQuery)
		return nil, 0, err
	}
	defer rows.Close()

	var vacancies []Vacancy
	for rows.Next() {
		var v Vacancy
		err := rows.Scan(
			&v.ID, &v.Title, &v.Company, &v.CompanyID, &v.Location,
			&v.Experience, &v.Remote, &v.Skills, &v.Description,
			&v.Address, &v.Latitude, &v.Longitude, &v.Views, &v.AuthorUserID,
			&v.IsArchived, &v.CreatedAt, &v.UpdatedAt, &v.CompanyLogo, &v.CompanyITAccredited, &v.VacancyTerms,
		)
		if err != nil {
			return nil, 0, err
		}
		vacancies = append(vacancies, v)
	}

	if vacancies == nil {
		vacancies = []Vacancy{}
	}

	return vacancies, totalCount, rows.Err()
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
			       COALESCE((SELECT c.logo_url FROM companies c WHERE c.id=v.company_id), '') AS company_logo, COALESCE((SELECT c.profile->>'it_accredited' FROM companies c WHERE c.id=v.company_id),'false')='true' AS company_it_accredited, v.terms,
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
			WHERE v.is_archived = FALSE OR v.is_archived IS NULL
		)
		SELECT id, title, company, company_id, location, experience, remote, skills, description,
		       address, latitude, longitude, views, skill_overlap, company_logo, company_it_accredited, terms
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
			&v.Address, &v.Latitude, &v.Longitude, &v.Views, &overlap, &v.CompanyLogo, &v.CompanyITAccredited, &v.VacancyTerms,
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

// GetApplicationUserIDs — получить ID всех кандидатов, откликнувшихся на вакансию
func (s *PostgresStorage) GetApplicationUserIDs(ctx context.Context, vacancyID int) ([]int, error) {
	query := `SELECT candidate_user_id FROM applications WHERE vacancy_id = $1`
	rows, err := s.pool.Query(ctx, query, vacancyID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var userIDs []int
	for rows.Next() {
		var id int
		if err := rows.Scan(&id); err != nil {
			return nil, err
		}
		userIDs = append(userIDs, id)
	}
	return userIDs, rows.Err()
}
