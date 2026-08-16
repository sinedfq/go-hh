package main

import (
	"context"
	"errors"

	"github.com/jackc/pgx/v5"
)

// ============ ОТКЛИКИ ============

func (s *PostgresStorage) CreateApplication(ctx context.Context, vacancyID, candidateUserID int, req CreateApplicationRequest) (int, error) {
	// Получаем resume_id кандидата (если есть)
	var resumeID *int
	resume, err := s.GetResumeByUserID(ctx, candidateUserID)
	if err == nil && resume.ID > 0 {
		resumeID = &resume.ID
	}

	query := `
        INSERT INTO applications (vacancy_id, candidate_user_id, resume_id, cover_letter, status)
        VALUES ($1, $2, $3, $4, 'new')
        ON CONFLICT (vacancy_id, candidate_user_id) DO NOTHING
        RETURNING id
    `

	var id int
	err = s.pool.QueryRow(ctx, query, vacancyID, candidateUserID, resumeID, req.CoverLetter).Scan(&id)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return 0, errors.New("application already exists")
		}
		return 0, err
	}

	return id, nil
}

func (s *PostgresStorage) GetApplicationByUserAndVacancy(ctx context.Context, userID, vacancyID int) (*Application, error) {
	query := `
        SELECT id, vacancy_id, candidate_user_id, resume_id,
               COALESCE(cover_letter, ''), status, created_at, viewed_at
        FROM applications
        WHERE candidate_user_id = $1 AND vacancy_id = $2
    `

	var a Application
	err := s.pool.QueryRow(ctx, query, userID, vacancyID).Scan(
		&a.ID, &a.VacancyID, &a.CandidateUserID, &a.ResumeID,
		&a.CoverLetter, &a.Status, &a.CreatedAt, &a.ViewedAt,
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return nil, nil
		}
		return nil, err
	}

	return &a, nil
}

func (s *PostgresStorage) GetApplicationsByVacancyID(ctx context.Context, vacancyID int) ([]Application, error) {
	query := `
        SELECT 
            a.id, a.vacancy_id, a.candidate_user_id, a.resume_id,
            COALESCE(a.cover_letter, ''), a.status, a.created_at, a.viewed_at,
            u.email AS candidate_email,
            COALESCE(u.photo_url, '') AS candidate_photo,
            COALESCE(r.full_name, '') AS resume_full_name,
            COALESCE(r.desired_position, '') AS resume_position
        FROM applications a
        JOIN users u ON u.id = a.candidate_user_id
        LEFT JOIN resumes r ON r.id = a.resume_id
        WHERE a.vacancy_id = $1
        ORDER BY a.created_at DESC
    `

	rows, err := s.pool.Query(ctx, query, vacancyID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var apps []Application
	for rows.Next() {
		var a Application
		err := rows.Scan(
			&a.ID, &a.VacancyID, &a.CandidateUserID, &a.ResumeID,
			&a.CoverLetter, &a.Status, &a.CreatedAt, &a.ViewedAt,
			&a.CandidateEmail, &a.CandidatePhoto,
			&a.ResumeFullName, &a.ResumePosition, &a.ResumeExperience,
			&a.VacancyTitle, &a.CompanyName,
		)
		if err != nil {
			return nil, err
		}
		apps = append(apps, a)
	}

	if apps == nil {
		apps = []Application{}
	}

	return apps, rows.Err()
}

func (s *PostgresStorage) GetApplicationsForEmployer(ctx context.Context, companyID int) ([]Application, error) {
	query := `
        SELECT 
            a.id, a.vacancy_id, a.candidate_user_id, a.resume_id,
            COALESCE(a.cover_letter, ''), a.status, a.created_at, a.viewed_at,
            u.email AS candidate_email,
            COALESCE(u.photo_url, '') AS candidate_photo,
            COALESCE(r.full_name, '') AS resume_full_name,
            COALESCE(r.desired_position, '') AS resume_position,
            COALESCE(r.experience, '') AS resume_experience,
            v.title AS vacancy_title,
            v.company AS company_name
        FROM applications a
        JOIN users u ON u.id = a.candidate_user_id
        JOIN vacancies v ON v.id = a.vacancy_id
        LEFT JOIN resumes r ON r.id = a.resume_id
        WHERE v.company_id = $1
        ORDER BY a.created_at DESC
    `

	rows, err := s.pool.Query(ctx, query, companyID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var apps []Application
	for rows.Next() {
		var a Application
		err := rows.Scan(
			&a.ID, &a.VacancyID, &a.CandidateUserID, &a.ResumeID,
			&a.CoverLetter, &a.Status, &a.CreatedAt, &a.ViewedAt,
			&a.CandidateEmail, &a.CandidatePhoto,
			&a.ResumeFullName, &a.ResumePosition, &a.ResumeExperience,
			&a.VacancyTitle, &a.CompanyName,
		)
		if err != nil {
			return nil, err
		}
		apps = append(apps, a)
	}

	if apps == nil {
		apps = []Application{}
	}

	return apps, rows.Err()
}

func (s *PostgresStorage) GetMyApplications(ctx context.Context, userID int) ([]Application, error) {
	query := `
		SELECT 
			a.id, a.vacancy_id, a.candidate_user_id, a.resume_id, 
			a.cover_letter, a.status, a.created_at, a.viewed_at,
			v.title as vacancy_title,
			COALESCE(comp.name, '') as company_name,
			v.company_id as vacancy_company_id,
			COALESCE(c.id, 0) as conversation_id
		FROM applications a
		JOIN vacancies v ON v.id = a.vacancy_id
		LEFT JOIN companies comp ON comp.id = v.company_id
		LEFT JOIN conversations c ON c.application_id = a.id
		WHERE a.candidate_user_id = $1
		ORDER BY a.created_at DESC
	`

	rows, err := s.pool.Query(ctx, query, userID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var apps []Application
	for rows.Next() {
		var app Application
		var convID int
		err := rows.Scan(
			&app.ID, &app.VacancyID, &app.CandidateUserID, &app.ResumeID,
			&app.CoverLetter, &app.Status, &app.CreatedAt, &app.ViewedAt,
			&app.VacancyTitle, &app.CompanyName, &app.VacancyCompanyID,
			&convID,
		)
		if err != nil {
			return nil, err
		}
		if convID > 0 {
			app.ConversationID = &convID
		}
		apps = append(apps, app)
	}

	if apps == nil {
		apps = []Application{}
	}

	return apps, rows.Err()
}

func (s *PostgresStorage) UpdateApplicationStatus(ctx context.Context, applicationID int, status string) error {
	query := `UPDATE applications SET status = $1 WHERE id = $2`
	_, err := s.pool.Exec(ctx, query, status, applicationID)
	return err
}

func (s *PostgresStorage) MarkApplicationViewed(ctx context.Context, applicationID int) error {
	query := `UPDATE applications SET status = 'viewed', viewed_at = NOW() WHERE id = $1 AND status = 'new'`
	_, err := s.pool.Exec(ctx, query, applicationID)
	return err
}

// ============ УВЕДОМЛЕНИЯ ============

func (s *PostgresStorage) CreateNotification(ctx context.Context, req CreateNotificationRequest) error {
	query := `
        INSERT INTO notifications (user_id, type, title, message, data)
        VALUES ($1, $2, $3, $4, $5)
    `
	_, err := s.pool.Exec(ctx, query, req.UserID, req.Type, req.Title, req.Message, req.Data)
	return err
}

func (s *PostgresStorage) GetNotifications(ctx context.Context, userID int, limit int) ([]Notification, error) {
	query := `
        SELECT id, user_id, type, title, COALESCE(message, ''), data, is_read, created_at
        FROM notifications
        WHERE user_id = $1
        ORDER BY created_at DESC
        LIMIT $2
    `

	rows, err := s.pool.Query(ctx, query, userID, limit)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var notifications []Notification
	for rows.Next() {
		var n Notification
		err := rows.Scan(
			&n.ID, &n.UserID, &n.Type, &n.Title, &n.Message, &n.Data, &n.IsRead, &n.CreatedAt,
		)
		if err != nil {
			return nil, err
		}
		notifications = append(notifications, n)
	}

	if notifications == nil {
		notifications = []Notification{}
	}

	return notifications, rows.Err()
}

func (s *PostgresStorage) MarkNotificationsRead(ctx context.Context, userID int) error {
	query := `UPDATE notifications SET is_read = true WHERE user_id = $1 AND is_read = false`
	_, err := s.pool.Exec(ctx, query, userID)
	return err
}

func (s *PostgresStorage) GetUnreadCount(ctx context.Context, userID int) (int, error) {
	var count int
	err := s.pool.QueryRow(ctx,
		`SELECT COUNT(*) FROM notifications WHERE user_id = $1 AND is_read = false`,
		userID,
	).Scan(&count)
	return count, err
}

// GetApplicationByID — получить отклик с данными вакансии
func (s *PostgresStorage) GetApplicationByID(ctx context.Context, appID int) (*Application, error) {
	query := `
        SELECT 
            a.id, a.vacancy_id, a.candidate_user_id, a.resume_id,
            COALESCE(a.cover_letter, ''), a.status, a.created_at, a.viewed_at,
            v.title AS vacancy_title,
            v.company AS company_name,
            COALESCE(v.company_id, 0) AS vacancy_company_id
        FROM applications a
        JOIN vacancies v ON v.id = a.vacancy_id
        WHERE a.id = $1
    `

	var a Application
	err := s.pool.QueryRow(ctx, query, appID).Scan(
		&a.ID, &a.VacancyID, &a.CandidateUserID, &a.ResumeID,
		&a.CoverLetter, &a.Status, &a.CreatedAt, &a.ViewedAt,
		&a.VacancyTitle, &a.CompanyName, &a.VacancyCompanyID,
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return nil, errors.New("application not found")
		}
		return nil, err
	}

	return &a, nil
}

// HasRecentResumeViewNotification — проверяет было ли уведомление о просмотре резюме
// от viewer_user_id к resume_owner_user_id за последние 24 часа
func (s *PostgresStorage) HasRecentResumeViewNotification(ctx context.Context, resumeOwnerUserID, viewerUserID int, hours int) (bool, error) {
	var count int
	query := `
        SELECT COUNT(*) FROM notifications 
        WHERE user_id = $1 
          AND type = 'resume_viewed'
          AND (data->>'viewer_user_id')::int = $2
          AND created_at > NOW() - INTERVAL '1 hour' * $3
    `
	err := s.pool.QueryRow(ctx, query, resumeOwnerUserID, viewerUserID, hours).Scan(&count)
	if err != nil {
		return false, err
	}
	return count > 0, nil
}
