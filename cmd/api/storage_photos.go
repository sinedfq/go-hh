package main

import (
	"context"
	"fmt"
)

func (s *PostgresStorage) UpdateCompanyLogo(ctx context.Context, companyID, userID int, logoURL string) error {
	result, err := s.pool.Exec(ctx, `UPDATE companies SET logo_url=$1 WHERE id=$2 AND EXISTS
        (SELECT 1 FROM users WHERE id=$3 AND company_id=$2 AND role IN ('employer','admin'))`, logoURL, companyID, userID)
	if err != nil {
		return err
	}
	if result.RowsAffected() != 1 {
		return fmt.Errorf("company access denied")
	}
	return nil
}

// ============ ФОТО И СЧЁТЧИКИ ============

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
