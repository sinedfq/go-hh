-- +goose Up
ALTER TABLE resumes ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Обновляем существующие записи
UPDATE resumes SET updated_at = created_at WHERE updated_at IS NULL;

-- +goose Down
ALTER TABLE resumes DROP COLUMN IF EXISTS updated_at;