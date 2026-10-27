-- +goose Up
ALTER TABLE phone_verification_challenges ADD COLUMN device_hash TEXT NOT NULL DEFAULT '';
ALTER TABLE phone_verification_challenges ADD COLUMN delivery_mode TEXT NOT NULL DEFAULT 'sms';
CREATE TABLE user_phone_accounts (
    user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    phone TEXT NOT NULL UNIQUE,
    confirmation_mode TEXT NOT NULL,
    device_hash TEXT NOT NULL DEFAULT '',
    confirmed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- +goose Down
DROP TABLE user_phone_accounts;
ALTER TABLE phone_verification_challenges DROP COLUMN device_hash;
ALTER TABLE phone_verification_challenges DROP COLUMN delivery_mode;
