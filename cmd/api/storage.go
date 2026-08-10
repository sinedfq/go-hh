package main

import (
	"context"
	"errors"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

type Storage interface {
	// Вакансии
	GetAllVacancies(ctx context.Context) ([]Vacancy, error)
	GetVacancyByID(ctx context.Context, id int) (Vacancy, error)
	CreateVacancy(ctx context.Context, v Vacancy) (int, error)

	// Резюме
	GetAllResumes(ctx context.Context) ([]Resume, error)
	GetResumeByID(ctx context.Context, id int) (Resume, error)
	CreateResume(ctx context.Context, r Resume) (int, error)

	// Избранное
	AddFavorite(ctx context.Context, userID, vacancyID int) error
	RemoveFavorite(ctx context.Context, userID, vacancyID int) error
	GetFavorites(ctx context.Context, userID int) ([]Vacancy, error)
}

type PostgresStorage struct {
	pool *pgxpool.Pool
}

func NewPostgresStorage(ctx context.Context, connString string) (*PostgresStorage, error) {
	pool, err := pgxpool.New(ctx, connString)
	if err != nil {
		return nil, err
	}

	if err := pool.Ping(ctx); err != nil {
		return nil, err
	}

	return &PostgresStorage{pool: pool}, nil
}

func (s *PostgresStorage) Close() {
	s.pool.Close()
}

// ============ ВАКАНСИИ ============

func (s *PostgresStorage) GetAllVacancies(ctx context.Context) ([]Vacancy, error) {
	query := `
		SELECT id, title, company, location, experience, remote, skills, description
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
			&v.ID,
			&v.Title,
			&v.Company,
			&v.Location,
			&v.Experience,
			&v.Remote,
			&v.Skills,
			&v.Description,
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
		SELECT id, title, company, location, experience, remote, skills, description
		FROM vacancies
		WHERE id = $1
	`

	var v Vacancy
	err := s.pool.QueryRow(ctx, query, id).Scan(
		&v.ID,
		&v.Title,
		&v.Company,
		&v.Location,
		&v.Experience,
		&v.Remote,
		&v.Skills,
		&v.Description,
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
	err := s.pool.QueryRow(
		ctx,
		query,
		v.Title,
		v.Company,
		v.Location,
		v.Experience,
		v.Remote,
		v.Skills,
		v.Description,
	).Scan(&id)

	if err != nil {
		return 0, err
	}

	return id, nil
}

// ============ РЕЗЮМЕ ============

func (s *PostgresStorage) GetAllResumes(ctx context.Context) ([]Resume, error) {
	query := `
		SELECT id, title, skills, experience_years, expected_salary, about
		FROM resumes
		ORDER BY created_at DESC
	`

	rows, err := s.pool.Query(ctx, query)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var resumes []Resume
	for rows.Next() {
		var r Resume
		err := rows.Scan(
			&r.ID,
			&r.Title,
			&r.Skills,
			&r.ExperienceYears,
			&r.ExpectedSalary,
			&r.About,
		)
		if err != nil {
			return nil, err
		}
		resumes = append(resumes, r)
	}

	return resumes, rows.Err()
}

func (s *PostgresStorage) GetResumeByID(ctx context.Context, id int) (Resume, error) {
	query := `
		SELECT id, title, skills, experience_years, expected_salary, about
		FROM resumes
		WHERE id = $1
	`

	var r Resume
	err := s.pool.QueryRow(ctx, query, id).Scan(
		&r.ID,
		&r.Title,
		&r.Skills,
		&r.ExperienceYears,
		&r.ExpectedSalary,
		&r.About,
	)

	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return Resume{}, errors.New("resume not found")
		}
		return Resume{}, err
	}

	return r, nil
}

func (s *PostgresStorage) CreateResume(ctx context.Context, r Resume) (int, error) {
	query := `
		INSERT INTO resumes (title, skills, experience_years, expected_salary, about)
		VALUES ($1, $2, $3, $4, $5)
		RETURNING id
	`

	var id int
	err := s.pool.QueryRow(
		ctx,
		query,
		r.Title,
		r.Skills,
		r.ExperienceYears,
		r.ExpectedSalary,
		r.About,
	).Scan(&id)

	if err != nil {
		return 0, err
	}

	return id, nil
}

// ============ ИЗБРАННОЕ ============

func (s *PostgresStorage) AddFavorite(ctx context.Context, userID, vacancyID int) error {
	query := `
		INSERT INTO favorites (user_id, vacancy_id)
		VALUES ($1, $2)
		ON CONFLICT (user_id, vacancy_id) DO NOTHING
	`
	_, err := s.pool.Exec(ctx, query, userID, vacancyID)
	return err
}

func (s *PostgresStorage) RemoveFavorite(ctx context.Context, userID, vacancyID int) error {
	query := `
		DELETE FROM favorites
		WHERE user_id = $1 AND vacancy_id = $2
	`
	_, err := s.pool.Exec(ctx, query, userID, vacancyID)
	return err
}

func (s *PostgresStorage) GetFavorites(ctx context.Context, userID int) ([]Vacancy, error) {
	query := `
		SELECT v.id, v.title, v.company, v.location, v.experience, v.remote, v.skills, v.description
		FROM favorites f
		JOIN vacancies v ON f.vacancy_id = v.id
		WHERE f.user_id = $1
		ORDER BY f.created_at DESC
	`

	rows, err := s.pool.Query(ctx, query, userID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var vacancies []Vacancy
	for rows.Next() {
		var v Vacancy
		err := rows.Scan(
			&v.ID,
			&v.Title,
			&v.Company,
			&v.Location,
			&v.Experience,
			&v.Remote,
			&v.Skills,
			&v.Description,
		)
		if err != nil {
			return nil, err
		}
		vacancies = append(vacancies, v)
	}

	return vacancies, rows.Err()
}
