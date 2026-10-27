-- +goose Up
CREATE TABLE phone_verification_challenges (
    id TEXT PRIMARY KEY,
    phone TEXT NOT NULL,
    code_hash TEXT NOT NULL,
    ip_hash TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    delivered BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMPTZ,
    receipt_hash TEXT,
    consumed_at TIMESTAMPTZ
);
CREATE INDEX phone_verification_phone_time ON phone_verification_challenges(phone, created_at DESC);
CREATE INDEX phone_verification_ip_time ON phone_verification_challenges(ip_hash, created_at DESC);

-- +goose Down
DROP TABLE phone_verification_challenges;
