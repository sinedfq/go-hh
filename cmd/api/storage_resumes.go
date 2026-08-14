package main

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5"
)

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
			&r.ID, &r.UserID, &r.FullName, &r.DesiredPosition, &r.Experience,
			&r.Skills, &r.About, &r.City, &r.Remote,
			&r.Phone, &r.Telegram, &r.GitHub, &r.LinkedIn,
			&r.PhotoURL, &r.Views, &r.CreatedAt,
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
		&r.ID, &r.UserID, &r.FullName, &r.DesiredPosition, &r.Experience,
		&r.Skills, &r.About, &r.City, &r.Remote,
		&r.Phone, &r.Telegram, &r.GitHub, &r.LinkedIn,
		&r.PhotoURL, &r.Views, &r.CreatedAt,
	)

	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return Resume{}, errors.New("resume not found")
		}
		return Resume{}, err
	}

	workExp, err := s.GetWorkExperienceByResumeID(ctx, r.ID)
	if err != nil {
		return Resume{}, err
	}
	r.WorkExperience = workExp

	return r, nil
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
		&r.ID, &r.UserID, &r.FullName, &r.DesiredPosition, &r.Experience,
		&r.Skills, &r.About, &r.City, &r.Remote,
		&r.Phone, &r.Telegram, &r.GitHub, &r.LinkedIn,
		&r.PhotoURL, &r.Views, &r.CreatedAt,
	)

	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return Resume{}, errors.New("resume not found")
		}
		return Resume{}, err
	}

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
		userID, req.FullName, req.DesiredPosition, req.Experience,
		req.Skills, req.About, req.City, req.Remote,
		req.Phone, req.Telegram, req.GitHub, req.LinkedIn,
	).Scan(&id)

	if err != nil {
		return 0, err
	}

	return id, nil
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
		req.FullName, req.DesiredPosition, req.Experience,
		req.Skills, req.About, req.City, req.Remote,
		req.Phone, req.Telegram, req.GitHub, req.LinkedIn,
		resumeID, userID,
	)

	return err
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

// ============ ОПЫТ РАБОТЫ ============

func (s *PostgresStorage) CreateWorkExperience(ctx context.Context, resumeID int, req CreateWorkExperienceRequest) (int, error) {
	var startDate time.Time
	var err error

	startDate, err = time.Parse("2006-01-02", req.StartDate)
	if err != nil {
		startDate, err = time.Parse(time.RFC3339, req.StartDate)
		if err != nil {
			if len(req.StartDate) >= 10 {
				startDate, err = time.Parse("2006-01-02", req.StartDate[:10])
			}
			if err != nil {
				return 0, fmt.Errorf("invalid start_date format: %s, error: %w", req.StartDate, err)
			}
		}
	}

	var endDate *time.Time
	if req.EndDate != nil && *req.EndDate != "" {
		var parsed time.Time

		parsed, err = time.Parse("2006-01-02", *req.EndDate)
		if err != nil {
			parsed, err = time.Parse(time.RFC3339, *req.EndDate)
			if err != nil {
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
		resumeID, req.Company, req.Position,
		startDate, endDate, req.Description,
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
			&exp.ID, &exp.ResumeID, &exp.Company, &exp.Position,
			&exp.StartDate, &exp.EndDate, &exp.Description, &exp.CreatedAt,
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
