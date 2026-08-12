package main

import (
	"context"
	"errors"
	"fmt"
	"strings"
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

	UpdateResume(ctx context.Context, resumeID int, userID int, req CreateResumeRequest) error

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

	GetCandidateVacancies(ctx context.Context, skills []string, limit int) ([]Vacancy, error)

	UpdateUserPhoto(ctx context.Context, userID int, photoURL string) error
	UpdateResumePhoto(ctx context.Context, resumeID, userID int, photoURL string) error
	IncrementResumeViews(ctx context.Context, resumeID int) error
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
		SELECT id, user_id, full_name, desired_position, experience, skills, about, city, remote,
		       COALESCE(phone, ''), COALESCE(telegram, ''), COALESCE(github, ''), COALESCE(linkedin, ''),
		       COALESCE(photo_url, ''), COALESCE(views, 0), created_at
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
			&r.Phone,
			&r.Telegram,
			&r.GitHub,
			&r.LinkedIn,
			&r.PhotoURL,
			&r.Views,
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
		SELECT id, user_id, full_name, desired_position, experience, skills, about, city, remote,
		       COALESCE(phone, ''), COALESCE(telegram, ''), COALESCE(github, ''), COALESCE(linkedin, ''),
		       COALESCE(photo_url, ''), COALESCE(views, 0), created_at
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
		&r.Phone,
		&r.Telegram,
		&r.GitHub,
		&r.LinkedIn,
		&r.PhotoURL,
		&r.Views,
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

func (s *PostgresStorage) CreateResume(ctx context.Context, userID int, req CreateResumeRequest) (int, error) {
	query := `
		INSERT INTO resumes (user_id, full_name, desired_position, experience, skills, about, city, remote, phone, telegram, github, linkedin)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
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
		req.Phone,
		req.Telegram,
		req.GitHub,
		req.LinkedIn,
	).Scan(&id)

	if err != nil {
		return 0, err
	}

	return id, nil
}

func (s *PostgresStorage) GetResumeByUserID(ctx context.Context, userID int) (Resume, error) {
	query := `
		SELECT id, user_id, full_name, desired_position, experience, skills, about, city, remote,
		       COALESCE(phone, ''), COALESCE(telegram, ''), COALESCE(github, ''), COALESCE(linkedin, ''),
		       COALESCE(photo_url, ''), COALESCE(views, 0), created_at
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
		&r.Phone,
		&r.Telegram,
		&r.GitHub,
		&r.LinkedIn,
		&r.PhotoURL,
		&r.Views,
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
		SELECT id, email, password_hash, COALESCE(photo_url, ''), created_at
		FROM users
		WHERE email = $1
	`

	var u User
	err := s.pool.QueryRow(ctx, query, email).Scan(
		&u.ID,
		&u.Email,
		&u.PasswordHash,
		&u.PhotoURL,
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
		SELECT id, email, password_hash, COALESCE(photo_url, ''), created_at
		FROM users
		WHERE id = $1
	`

	var u User
	err := s.pool.QueryRow(ctx, query, id).Scan(
		&u.ID,
		&u.Email,
		&u.PasswordHash,
		&u.PhotoURL,
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
	// Парсим start_date — поддерживаем оба формата
	var startDate time.Time
	var err error

	// Сначала пробуем простой формат
	startDate, err = time.Parse("2006-01-02", req.StartDate)
	if err != nil {
		// Если не получилось, пробуем ISO формат
		startDate, err = time.Parse(time.RFC3339, req.StartDate)
		if err != nil {
			// Если и это не сработало, пробуем дату без времени
			if len(req.StartDate) >= 10 {
				startDate, err = time.Parse("2006-01-02", req.StartDate[:10])
			}
			if err != nil {
				return 0, fmt.Errorf("invalid start_date format: %s, error: %w", req.StartDate, err)
			}
		}
	}

	// Парсим end_date (может быть null)
	var endDate *time.Time
	if req.EndDate != nil && *req.EndDate != "" {
		var parsed time.Time

		// Сначала пробуем простой формат
		parsed, err = time.Parse("2006-01-02", *req.EndDate)
		if err != nil {
			// Если не получилось, пробуем ISO формат
			parsed, err = time.Parse(time.RFC3339, *req.EndDate)
			if err != nil {
				// Если и это не сработало, пробуем дату без времени
				if len(*req.EndDate) >= 10 {
					parsed, err = time.Parse("2006-01-02", (*req.EndDate)[:10])
				}
				if err != nil {
					return 0, fmt.Errorf("invalid end_date format: %s, error: %w", *req.EndDate, err)
				}
			}
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

func (s *PostgresStorage) GetCandidateVacancies(ctx context.Context, skills []string, limit int) ([]Vacancy, error) {
	// Если навыков в резюме нет — кандидатов нет
	if len(skills) == 0 {
		return []Vacancy{}, nil
	}

	lowerSkills := make([]string, len(skills))
	for i, sk := range skills {
		lowerSkills[i] = strings.ToLower(strings.TrimSpace(sk))
	}

	query := `
		WITH scored AS (
			SELECT v.id, v.title, v.company, v.location, v.experience, v.remote,
			       v.skills, v.description, v.created_at,
			       COALESCE((
			           SELECT COUNT(*)
			           FROM unnest(v.skills) AS skill
			           WHERE LOWER(skill) = ANY($1)
			       ), 0) AS skill_overlap
			FROM vacancies v
		)
		SELECT id, title, company, location, experience, remote, skills, description, skill_overlap
		FROM scored
		WHERE skill_overlap > 0
		ORDER BY skill_overlap DESC, created_at DESC
		LIMIT $2
	`

	rows, err := s.pool.Query(ctx, query, lowerSkills, limit)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var vacancies []Vacancy
	for rows.Next() {
		var v Vacancy
		var overlap int
		err := rows.Scan(
			&v.ID, &v.Title, &v.Company, &v.Location,
			&v.Experience, &v.Remote, &v.Skills, &v.Description,
			&overlap,
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

func (s *PostgresStorage) UpdateResume(ctx context.Context, resumeID int, userID int, req CreateResumeRequest) error {
	query := `
		UPDATE resumes 
		SET full_name = $1, desired_position = $2, experience = $3, 
		    skills = $4, about = $5, city = $6, remote = $7,
		    phone = $8, telegram = $9, github = $10, linkedin = $11,
		    updated_at = CURRENT_TIMESTAMP
		WHERE id = $12 AND user_id = $13
	`

	_, err := s.pool.Exec(ctx, query,
		req.FullName,
		req.DesiredPosition,
		req.Experience,
		req.Skills,
		req.About,
		req.City,
		req.Remote,
		req.Phone,
		req.Telegram,
		req.GitHub,
		req.LinkedIn,
		resumeID,
		userID,
	)

	return err
}

func (s *PostgresStorage) UpdateUserPhoto(ctx context.Context, userID int, photoURL string) error {
	query := `UPDATE users SET photo_url = $1 WHERE id = $2`
	_, err := s.pool.Exec(ctx, query, photoURL, userID)
	return err
}

func (s *PostgresStorage) UpdateResumePhoto(ctx context.Context, resumeID, userID int, photoURL string) error {
	query := `UPDATE resumes SET photo_url = $1 WHERE id = $2 AND user_id = $3`
	_, err := s.pool.Exec(ctx, query, photoURL, resumeID, userID)
	return err
}

func (s *PostgresStorage) IncrementResumeViews(ctx context.Context, resumeID int) error {
	query := `UPDATE resumes SET views = views + 1 WHERE id = $1`
	_, err := s.pool.Exec(ctx, query, resumeID)
	return err
}
