-- +goose Up
CREATE TABLE resumes (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    skills TEXT[] NOT NULL DEFAULT '{}',
    experience_years INT NOT NULL DEFAULT 0,
    expected_salary INT,
    about TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- +goose Down
DROP TABLE resumes;