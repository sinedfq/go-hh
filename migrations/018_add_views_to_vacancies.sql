-- +goose Up
ALTER TABLE vacancies ADD COLUMN IF NOT EXISTS views INTEGER DEFAULT 0;

-- +goose Down
ALTER TABLE vacancies DROP COLUMN IF EXISTS views;