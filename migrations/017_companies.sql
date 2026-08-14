-- +goose Up

-- 1. Создаём таблицу компаний
CREATE TABLE IF NOT EXISTS companies (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT DEFAULT '',
    industry VARCHAR(100) DEFAULT '',
    size VARCHAR(50) DEFAULT '',
    city VARCHAR(100) DEFAULT '',
    website VARCHAR(255) DEFAULT '',
    logo_url VARCHAR(255) DEFAULT '',
    photo_url VARCHAR(255) DEFAULT '',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Добавляем company_id в vacancies
ALTER TABLE vacancies ADD COLUMN IF NOT EXISTS company_id INTEGER REFERENCES companies(id);
ALTER TABLE vacancies ADD COLUMN IF NOT EXISTS address TEXT DEFAULT '';
ALTER TABLE vacancies ADD COLUMN IF NOT EXISTS latitude DECIMAL(10, 8);
ALTER TABLE vacancies ADD COLUMN IF NOT EXISTS longitude DECIMAL(11, 8);

-- 3. Переносим существующие компании из строкового поля
INSERT INTO companies (name)
SELECT DISTINCT company FROM vacancies
WHERE company IS NOT NULL AND company != ''
ON CONFLICT (name) DO NOTHING;

-- 4. Связываем вакансии с компаниями
UPDATE vacancies v
SET company_id = c.id
FROM companies c
WHERE v.company = c.name;

-- +goose Down
ALTER TABLE vacancies DROP COLUMN IF EXISTS company_id;
ALTER TABLE vacancies DROP COLUMN IF EXISTS address;
ALTER TABLE vacancies DROP COLUMN IF EXISTS latitude;
ALTER TABLE vacancies DROP COLUMN IF EXISTS longitude;
DROP TABLE IF EXISTS companies;