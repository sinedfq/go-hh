-- +goose Up
ALTER TABLE vacancies ADD COLUMN IF NOT EXISTS author_user_id INTEGER REFERENCES users(id);
CREATE INDEX IF NOT EXISTS idx_vacancies_author ON vacancies(author_user_id);
UPDATE vacancies SET author_user_id = 1 WHERE author_user_id IS NULL AND EXISTS (SELECT 1 FROM users WHERE id = 1);

-- +goose Down
DROP INDEX IF EXISTS idx_vacancies_author;
ALTER TABLE vacancies DROP COLUMN IF EXISTS author_user_id;