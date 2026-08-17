-- +goose Up
-- +goose StatementBegin

-- Поле для отслеживания обновлений вакансии
ALTER TABLE vacancies ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Поле для архивации вакансий
ALTER TABLE vacancies ADD COLUMN IF NOT EXISTS is_archived BOOLEAN DEFAULT FALSE;

-- Индекс для быстрого поиска активных вакансий
CREATE INDEX IF NOT EXISTS idx_vacancies_archived ON vacancies(is_archived);

-- Индекс для поиска по компании
CREATE INDEX IF NOT EXISTS idx_vacancies_company ON vacancies(company_id);

-- Триггер для автоматического обновления updated_at
-- +goose StatementEnd

-- +goose StatementBegin
CREATE OR REPLACE FUNCTION update_vacancy_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
-- +goose StatementEnd

-- +goose StatementBegin
DROP TRIGGER IF EXISTS trigger_vacancy_updated_at ON vacancies;
CREATE TRIGGER trigger_vacancy_updated_at
    BEFORE UPDATE ON vacancies
    FOR EACH ROW
    EXECUTE FUNCTION update_vacancy_updated_at();
-- +goose StatementEnd

-- +goose Down
-- +goose StatementBegin
DROP TRIGGER IF EXISTS trigger_vacancy_updated_at ON vacancies;
DROP FUNCTION IF EXISTS update_vacancy_updated_at();
DROP INDEX IF EXISTS idx_vacancies_archived;
DROP INDEX IF EXISTS idx_vacancies_company;
ALTER TABLE vacancies DROP COLUMN IF EXISTS is_archived;
ALTER TABLE vacancies DROP COLUMN IF EXISTS updated_at;
-- +goose StatementEnd