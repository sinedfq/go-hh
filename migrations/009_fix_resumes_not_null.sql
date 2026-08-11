-- +goose Up
-- Убираем NOT NULL со старых колонок, которые больше не используем
ALTER TABLE resumes ALTER COLUMN title DROP NOT NULL;
ALTER TABLE resumes ALTER COLUMN experience_years DROP NOT NULL;
ALTER TABLE resumes ALTER COLUMN skills DROP NOT NULL;

-- Добавляем default для experience_years
ALTER TABLE resumes ALTER COLUMN experience_years SET DEFAULT 0;

-- Делаем remote NOT NULL с default
ALTER TABLE resumes ALTER COLUMN remote SET DEFAULT FALSE;
UPDATE resumes SET remote = FALSE WHERE remote IS NULL;
ALTER TABLE resumes ALTER COLUMN remote SET NOT NULL;

-- +goose Down
ALTER TABLE resumes ALTER COLUMN title SET NOT NULL;
ALTER TABLE resumes ALTER COLUMN experience_years SET NOT NULL;
ALTER TABLE resumes ALTER COLUMN skills SET NOT NULL;
ALTER TABLE resumes ALTER COLUMN remote DROP NOT NULL;
ALTER TABLE resumes ALTER COLUMN remote DROP DEFAULT;