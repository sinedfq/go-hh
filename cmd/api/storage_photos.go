package main

import "context"

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
