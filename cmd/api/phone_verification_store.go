package main

import (
	"context"
	"crypto/hmac"
	"errors"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/sinedfq/go-hh/internal/phoneotp"
	"time"
)

type phoneVerificationStore struct{ pool *pgxpool.Pool }

func (s phoneVerificationStore) Reserve(ctx context.Context, c phoneotp.Challenge) error {
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return err
	}
	defer tx.Rollback(ctx)
	// Stable lock order prevents simultaneous requests from bypassing either limit.
	for _, key := range []string{"otp-ip:" + c.IPHash, "otp-phone:" + c.Phone} {
		if _, err = tx.Exec(ctx, "SELECT pg_advisory_xact_lock(hashtextextended($1,0))", key); err != nil {
			return err
		}
	}
	var phoneCount, ipCount int
	var latest *time.Time
	err = tx.QueryRow(ctx, `SELECT count(*),max(created_at) FROM phone_verification_challenges WHERE phone=$1 AND created_at>$2`, c.Phone, c.CreatedAt.Add(-time.Hour)).Scan(&phoneCount, &latest)
	if err != nil {
		return err
	}
	err = tx.QueryRow(ctx, `SELECT count(*) FROM phone_verification_challenges WHERE ip_hash=$1 AND created_at>$2`, c.IPHash, c.CreatedAt.Add(-time.Hour)).Scan(&ipCount)
	if err != nil {
		return err
	}
	if phoneCount >= 5 || ipCount >= 30 || (latest != nil && c.CreatedAt.Sub(*latest) < phoneotp.ResendAfter) {
		return phoneotp.ErrRateLimit
	}
	if _, err = tx.Exec(ctx, `UPDATE phone_verification_challenges SET consumed_at=$2 WHERE phone=$1 AND verified_at IS NULL AND consumed_at IS NULL`, c.Phone, c.CreatedAt); err != nil {
		return err
	}
	if _, err = tx.Exec(ctx, `INSERT INTO phone_verification_challenges(id,phone,code_hash,ip_hash,created_at,expires_at) VALUES($1,$2,$3,$4,$5,$6)`, c.ID, c.Phone, c.CodeHash, c.IPHash, c.CreatedAt, c.ExpiresAt); err != nil {
		return err
	}
	// Bounded opportunistic cleanup; no OTP challenge is retained past its useful rate-limit window.
	if _, err = tx.Exec(ctx, `DELETE FROM phone_verification_challenges WHERE id IN (SELECT id FROM phone_verification_challenges WHERE created_at<$1 AND expires_at<$2 ORDER BY created_at LIMIT 1000 FOR UPDATE SKIP LOCKED)`, c.CreatedAt.Add(-24*time.Hour), c.CreatedAt); err != nil {
		return err
	}
	return tx.Commit(ctx)
}
func (s phoneVerificationStore) Delivered(ctx context.Context, id string) error {
	_, err := s.pool.Exec(ctx, `UPDATE phone_verification_challenges SET delivered=TRUE WHERE id=$1`, id)
	return err
}
func (s phoneVerificationStore) Verify(ctx context.Context, id, codeHash, receiptHash string, now time.Time) error {
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return err
	}
	defer tx.Rollback(ctx)
	var stored string
	var expiry time.Time
	var attempts int
	var delivered bool
	var verified, consumed *time.Time
	err = tx.QueryRow(ctx, `SELECT code_hash,expires_at,attempts,delivered,verified_at,consumed_at FROM phone_verification_challenges WHERE id=$1 FOR UPDATE`, id).Scan(&stored, &expiry, &attempts, &delivered, &verified, &consumed)
	if errors.Is(err, pgx.ErrNoRows) {
		return phoneotp.ErrInvalid
	}
	if err != nil {
		return err
	}
	if !delivered || verified != nil || consumed != nil || !now.Before(expiry) || attempts >= phoneotp.MaxAttempts {
		return phoneotp.ErrInvalid
	}
	if !hmac.Equal([]byte(stored), []byte(codeHash)) {
		if _, err = tx.Exec(ctx, `UPDATE phone_verification_challenges SET attempts=attempts+1 WHERE id=$1`, id); err != nil {
			return err
		}
		if err = tx.Commit(ctx); err != nil {
			return err
		}
		return phoneotp.ErrInvalid
	}
	if _, err = tx.Exec(ctx, `UPDATE phone_verification_challenges SET verified_at=$2,receipt_hash=$3,expires_at=$4 WHERE id=$1`, id, now, receiptHash, now.Add(phoneotp.ReceiptTTL)); err != nil {
		return err
	}
	return tx.Commit(ctx)
}
func (s phoneVerificationStore) Redeem(ctx context.Context, id, hash string, now time.Time) (string, error) {
	var phone string
	err := s.pool.QueryRow(ctx, `UPDATE phone_verification_challenges SET consumed_at=$3 WHERE id=$1 AND receipt_hash=$2 AND verified_at IS NOT NULL AND consumed_at IS NULL AND expires_at>$3 RETURNING phone`, id, hash, now).Scan(&phone)
	if errors.Is(err, pgx.ErrNoRows) {
		return "", phoneotp.ErrInvalid
	}
	return phone, err
}
