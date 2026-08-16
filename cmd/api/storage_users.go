package main

import (
	"context"
	"errors"

	"github.com/jackc/pgx/v5"
)

func (s *PostgresStorage) CreateUser(ctx context.Context, email, passwordHash, role string) (int, error) {
	query := `
		INSERT INTO users (email, password_hash, role)
		VALUES ($1, $2, $3)
		RETURNING id
	`

	var id int
	err := s.pool.QueryRow(ctx, query, email, passwordHash, role).Scan(&id)
	if err != nil {
		return 0, err
	}

	return id, nil
}

func (s *PostgresStorage) GetUserByEmail(ctx context.Context, email string) (User, error) {
	query := `
		SELECT id, email, password_hash, COALESCE(photo_url, ''), role, company_id, created_at
		FROM users
		WHERE email = $1
	`

	var u User
	err := s.pool.QueryRow(ctx, query, email).Scan(
		&u.ID, &u.Email, &u.PasswordHash, &u.PhotoURL, &u.Role, &u.CompanyID, &u.CreatedAt,
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
		SELECT id, email, password_hash, COALESCE(photo_url, ''), role, company_id, created_at
		FROM users
		WHERE id = $1
	`

	var u User
	err := s.pool.QueryRow(ctx, query, id).Scan(
		&u.ID, &u.Email, &u.PasswordHash, &u.PhotoURL, &u.Role, &u.CompanyID, &u.CreatedAt,
	)

	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return User{}, errors.New("user not found")
		}
		return User{}, err
	}

	return u, nil
}

func (s *PostgresStorage) UpdateUserPhoto(ctx context.Context, userID int, photoURL string) error {
	query := `UPDATE users SET photo_url = $1 WHERE id = $2`
	_, err := s.pool.Exec(ctx, query, photoURL, userID)
	return err
}
