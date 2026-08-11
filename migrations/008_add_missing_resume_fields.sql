-- +goose Up
-- Добавляем недостающие колонки
ALTER TABLE resumes ADD COLUMN IF NOT EXISTS experience VARCHAR(50);
ALTER TABLE resumes ADD COLUMN IF NOT EXISTS remote BOOLEAN DEFAULT FALSE;

-- Переносим данные из experience_years в experience
UPDATE resumes SET experience = 'Junior' WHERE experience_years = 1;
UPDATE resumes SET experience = 'Middle' WHERE experience_years BETWEEN 2 AND 5;
UPDATE resumes SET experience = 'Senior' WHERE experience_years > 5;

-- Устанавливаем значение по умолчанию для тех, где experience_years = 0
UPDATE resumes SET experience = 'Junior' WHERE experience IS NULL;

-- Делаем experience NOT NULL
ALTER TABLE resumes ALTER COLUMN experience SET NOT NULL;
ALTER TABLE resumes ALTER COLUMN experience SET DEFAULT 'Junior';

-- +goose Down
ALTER TABLE resumes DROP COLUMN IF EXISTS experience;
ALTER TABLE resumes DROP COLUMN IF EXISTS remote;