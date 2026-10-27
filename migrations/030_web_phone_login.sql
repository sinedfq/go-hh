-- +goose Up
ALTER TABLE phone_verification_challenges ADD COLUMN account_user_id INTEGER REFERENCES users(id) ON DELETE CASCADE;

-- +goose Down
ALTER TABLE phone_verification_challenges DROP COLUMN account_user_id;
