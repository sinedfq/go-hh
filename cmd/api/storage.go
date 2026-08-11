package main

import (
	"context"
	"errors"
	"fmt"
	"time"

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

	CreateResume(ctx context.Context, userID int, req CreateResumeRequest) (int, error)
	GetResumeByUserID(ctx context.Context, userID int) (Resume, error)
	DeleteResume(ctx context.Context, resumeID, userID int) error

	CreateWorkExperience(ctx context.Context, resumeID int, req CreateWorkExperienceRequest) (int, error)
	GetWorkExperienceByResumeID(ctx context.Context, resumeID int) ([]WorkExperience, error)
	DeleteWorkExperience(ctx context.Context, id int) error

	// Избранное
	AddFavorite(ctx context.Context, userID, vacancyID int) error
	RemoveFavorite(ctx context.Context, userID, vacancyID int) error
	GetFavorites(ctx context.Context, userID int) ([]Vacancy, error)

	CreateUser(ctx context.Context, email, passwordHash string) (int, error)
	GetUserByEmail(ctx context.Context, email string) (User, error)
	GetUserByID(ctx context.Context, id int) (User, error)

	GetAllSkills(ctx context.Context) ([]Skill, error)
	CreateSkill(ctx context.Context, name string) (Skill, error)

	GetAllPositions(ctx context.Context) ([]Position, error)
	CreatePosition(ctx context.Context, name string) (Position, error)
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
		SELECT id, user_id, full_name, desired_position, experience, skills, about, city, remote, created_at
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
			&r.UserID,
			&r.FullName,
			&r.DesiredPosition,
			&r.Experience,
			&r.Skills,
			&r.About,
			&r.City,
			&r.Remote,
			&r.CreatedAt,
		)
		if err != nil {
			return nil, err
		}
		resumes = append(resumes, r)
	}

	if resumes == nil {
		resumes = []Resume{}
	}

	return resumes, rows.Err()
}

func (s *PostgresStorage) GetResumeByID(ctx context.Context, id int) (Resume, error) {
	query := `
		SELECT id, user_id, full_name, desired_position, experience, skills, about, city, remote, created_at
		FROM resumes
		WHERE id = $1
	`

	var r Resume
	err := s.pool.QueryRow(ctx, query, id).Scan(
		&r.ID,
		&r.UserID,
		&r.FullName,
		&r.DesiredPosition,
		&r.Experience,
		&r.Skills,
		&r.About,
		&r.City,
		&r.Remote,
		&r.CreatedAt,
	)

	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return Resume{}, errors.New("resume not found")
		}
		return Resume{}, err
	}

	return r, nil
}

func (s *PostgresStorage) CreateResume(ctx context.Context, userID int, req CreateResumeRequest) (int, error) {
	query := `
		INSERT INTO resumes (user_id, full_name, desired_position, experience, skills, about, city, remote)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
		RETURNING id
	`

	var id int
	err := s.pool.QueryRow(ctx, query,
		userID,
		req.FullName,
		req.DesiredPosition,
		req.Experience,
		req.Skills,
		req.About,
		req.City,
		req.Remote,
	).Scan(&id)

	if err != nil {
		return 0, err
	}

	return id, nil
}

func (s *PostgresStorage) GetResumeByUserID(ctx context.Context, userID int) (Resume, error) {
	query := `
		SELECT id, user_id, full_name, desired_position, experience, skills, about, city, remote, created_at
		FROM resumes
		WHERE user_id = $1
		LIMIT 1
	`

	var r Resume
	err := s.pool.QueryRow(ctx, query, userID).Scan(
		&r.ID,
		&r.UserID,
		&r.FullName,
		&r.DesiredPosition,
		&r.Experience,
		&r.Skills,
		&r.About,
		&r.City,
		&r.Remote,
		&r.CreatedAt,
	)

	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return Resume{}, errors.New("resume not found")
		}
		return Resume{}, err
	}

	// Загружаем опыт работы
	workExp, err := s.GetWorkExperienceByResumeID(ctx, r.ID)
	if err != nil {
		return Resume{}, err
	}
	r.WorkExperience = workExp

	return r, nil
}

func (s *PostgresStorage) DeleteResume(ctx context.Context, resumeID, userID int) error {
	query := `DELETE FROM resumes WHERE id = $1 AND user_id = $2`

	result, err := s.pool.Exec(ctx, query, resumeID, userID)
	if err != nil {
		return err
	}

	if result.RowsAffected() == 0 {
		return errors.New("resume not found")
	}

	return nil
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

// ===== Reg ======

func (s *PostgresStorage) CreateUser(ctx context.Context, email, passwordHash string) (int, error) {
	query := `
		INSERT INTO users (email, password_hash)
		VALUES ($1, $2)
		RETURNING id
	`

	var id int
	err := s.pool.QueryRow(ctx, query, email, passwordHash).Scan(&id)
	if err != nil {
		return 0, err
	}

	return id, nil
}

func (s *PostgresStorage) GetUserByEmail(ctx context.Context, email string) (User, error) {
	query := `
		SELECT id, email, password_hash, created_at
		FROM users
		WHERE email = $1
	`

	var u User
	err := s.pool.QueryRow(ctx, query, email).Scan(
		&u.ID,
		&u.Email,
		&u.PasswordHash,
		&u.CreatedAt,
	)

	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return User{}, errors.New("user not found")
		}
		return User{}, err
	}

	return u, nil
}

func (s *PostgresStorage) GetUserByID(ctx context.Context, id int) (User, error) {
	query := `
		SELECT id, email, password_hash, created_at
		FROM users
		WHERE id = $1
	`

	var u User
	err := s.pool.QueryRow(ctx, query, id).Scan(
		&u.ID,
		&u.Email,
		&u.PasswordHash,
		&u.CreatedAt,
	)

	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return User{}, errors.New("user not found")
		}
		return User{}, err
	}

	return u, nil
}

func (s *PostgresStorage) CreateWorkExperience(ctx context.Context, resumeID int, req CreateWorkExperienceRequest) (int, error) {
	// Парсим start_date
	startDate, err := time.Parse("2006-01-02", req.StartDate)
	if err != nil {
		return 0, fmt.Errorf("invalid start_date format: %w", err)
	}

	// Парсим end_date (может быть null)
	var endDate *time.Time
	if req.EndDate != nil && *req.EndDate != "" {
		parsed, err := time.Parse("2006-01-02", *req.EndDate)
		if err != nil {
			return 0, fmt.Errorf("invalid end_date format: %w", err)
		}
		endDate = &parsed
	}

	query := `
		INSERT INTO work_experience (resume_id, company, position, start_date, end_date, description)
		VALUES ($1, $2, $3, $4, $5, $6)
		RETURNING id
	`

	var id int
	err = s.pool.QueryRow(ctx, query,
		resumeID,
		req.Company,
		req.Position,
		startDate,
		endDate,
		req.Description,
	).Scan(&id)

	if err != nil {
		return 0, err
	}

	return id, nil
}

func (s *PostgresStorage) GetWorkExperienceByResumeID(ctx context.Context, resumeID int) ([]WorkExperience, error) {
	query := `
		SELECT id, resume_id, company, position, start_date, end_date, description, created_at
		FROM work_experience
		WHERE resume_id = $1
		ORDER BY start_date DESC
	`

	rows, err := s.pool.Query(ctx, query, resumeID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var experiences []WorkExperience
	for rows.Next() {
		var exp WorkExperience
		err := rows.Scan(
			&exp.ID,
			&exp.ResumeID,
			&exp.Company,
			&exp.Position,
			&exp.StartDate,
			&exp.EndDate,
			&exp.Description,
			&exp.CreatedAt,
		)
		if err != nil {
			return nil, err
		}
		experiences = append(experiences, exp)
	}

	if experiences == nil {
		experiences = []WorkExperience{}
	}

	return experiences, rows.Err()
}

func (s *PostgresStorage) DeleteWorkExperience(ctx context.Context, id int) error {
	query := `DELETE FROM work_experience WHERE id = $1`
	result, err := s.pool.Exec(ctx, query, id)
	if err != nil {
		return err
	}
	if result.RowsAffected() == 0 {
		return errors.New("work experience not found")
	}
	return nil
}

func (s *PostgresStorage) GetAllSkills(ctx context.Context) ([]Skill, error) {
	query := `SELECT id, name FROM skills ORDER BY name ASC`

	rows, err := s.pool.Query(ctx, query)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var skills []Skill
	for rows.Next() {
		var skill Skill
		if err := rows.Scan(&skill.ID, &skill.Name); err != nil {
			return nil, err
		}
		skills = append(skills, skill)
	}

	if skills == nil {
		skills = []Skill{}
	}

	return skills, rows.Err()
}

func (s *PostgresStorage) CreateSkill(ctx context.Context, name string) (Skill, error) {
	query := `
		INSERT INTO skills (name)
		VALUES ($1)
		ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name
		RETURNING id, name
	`

	var skill Skill
	err := s.pool.QueryRow(ctx, query, name).Scan(&skill.ID, &skill.Name)
	if err != nil {
		return Skill{}, err
	}

	return skill, nil
}

func (s *PostgresStorage) GetAllPositions(ctx context.Context) ([]Position, error) {
	query := `SELECT id, name FROM positions ORDER BY name ASC`

	rows, err := s.pool.Query(ctx, query)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var positions []Position
	for rows.Next() {
		var p Position
		if err := rows.Scan(&p.ID, &p.Name); err != nil {
			return nil, err
		}
		positions = append(positions, p)
	}

	if positions == nil {
		positions = []Position{}
	}

	return positions, rows.Err()
}

func (s *PostgresStorage) CreatePosition(ctx context.Context, name string) (Position, error) {
	query := `
		INSERT INTO positions (name)
		VALUES ($1)
		ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name
		RETURNING id, name
	`

	var p Position
	err := s.pool.QueryRow(ctx, query, name).Scan(&p.ID, &p.Name)
	if err != nil {
		return Position{}, err
	}

	return p, nil
}
