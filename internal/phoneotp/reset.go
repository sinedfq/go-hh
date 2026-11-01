package phoneotp

import (
	"context"
	"errors"
	"github.com/jackc/pgx/v5/pgxpool"
)

// ResetLimits also invalidates outstanding codes and receipts, never account bindings.
func ResetLimits(ctx context.Context, pool *pgxpool.Pool, phone string, all bool) (int64, error) {
	if all && phone != "" {
		return 0, errors.New("choose a phone or all, not both")
	}
	if !all {
		var err error
		phone, err = NormalizePhone(phone)
		if err != nil {
			return 0, err
		}
	}
	tx, err := pool.Begin(ctx)
	if err != nil {
		return 0, err
	}
	defer tx.Rollback(ctx)
	if all {
		_, err = tx.Exec(ctx, `LOCK TABLE phone_verification_challenges IN ACCESS EXCLUSIVE MODE`)
	} else {
		_, err = tx.Exec(ctx, `SELECT pg_advisory_xact_lock(hashtextextended($1,0))`, "otp-phone:"+phone)
	}
	if err != nil {
		return 0, err
	}
	query := `DELETE FROM phone_verification_challenges WHERE phone=$1`
	args := []any{phone}
	if all {
		query = `DELETE FROM phone_verification_challenges`
		args = nil
	}
	result, err := tx.Exec(ctx, query, args...)
	if err != nil {
		return 0, err
	}
	if err = tx.Commit(ctx); err != nil {
		return 0, err
	}
	return result.RowsAffected(), nil
}
