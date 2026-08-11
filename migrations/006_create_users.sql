-- +goose Up
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Добавляем связь резюме с пользователем
ALTER TABLE resumes ADD COLUMN user_id INTEGER REFERENCES users(id) ON DELETE CASCADE;

-- Обновляем favorites чтобы привязать к пользователю
ALTER TABLE favorites DROP CONSTRAINT IF EXISTS favorites_user_id_fkey;

-- +goose Down
DROP TABLE users;
ALTER TABLE resumes DROP COLUMN IF EXISTS user_id;