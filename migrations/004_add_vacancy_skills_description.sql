-- +goose Up
ALTER TABLE vacancies ADD COLUMN IF NOT EXISTS skills TEXT[] DEFAULT '{}';
ALTER TABLE vacancies ADD COLUMN IF NOT EXISTS description TEXT DEFAULT '';

-- +goose Down
ALTER TABLE vacancies DROP COLUMN IF EXISTS skills;
ALTER TABLE vacancies DROP COLUMN IF EXISTS description;