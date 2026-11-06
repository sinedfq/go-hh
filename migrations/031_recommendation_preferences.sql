-- +goose Up
CREATE TABLE recommendation_dismissals (
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    vacancy_id INTEGER NOT NULL REFERENCES vacancies(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, vacancy_id)
);

-- +goose Down
DROP TABLE recommendation_dismissals;
