package main

import "context"

// ============ ИЗБРАННОЕ ============

func (s *PostgresStorage) AddFavorite(ctx context.Context, userID, vacancyID int) error {
	query := `
		INSERT INTO favorites (user_id, vacancy_id)
		VALUES ($1, $2)
		ON CONFLICT (user_id, vacancy_id) DO NOTHING
	`
	_, err := s.pool.Exec(ctx, query, userID, vacancyID)
	return err
}

func (s *PostgresStorage) RemoveFavorite(ctx context.Context, userID, vacancyID int) error {
	query := `DELETE FROM favorites WHERE user_id = $1 AND vacancy_id = $2`
	_, err := s.pool.Exec(ctx, query, userID, vacancyID)
	return err
}

func (s *PostgresStorage) GetFavorites(ctx context.Context, userID int) ([]Vacancy, error) {
	query := `
		SELECT v.id, v.title, v.company, COALESCE(v.company_id, 0), v.location, v.experience, v.remote, v.skills, v.description,
		       COALESCE(v.address, ''), COALESCE(v.latitude, 0), COALESCE(v.longitude, 0)
		FROM favorites f
		JOIN vacancies v ON f.vacancy_id = v.id
		WHERE f.user_id = $1
		ORDER BY f.created_at DESC
	`

	rows, err := s.pool.Query(ctx, query, userID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var vacancies []Vacancy
	for rows.Next() {
		var v Vacancy
		err := rows.Scan(
			&v.ID, &v.Title, &v.Company, &v.CompanyID, &v.Location,
			&v.Experience, &v.Remote, &v.Skills, &v.Description,
			&v.Address, &v.Latitude, &v.Longitude,
		)
		if err != nil {
			return nil, err
		}
		vacancies = append(vacancies, v)
	}

	return vacancies, rows.Err()
}
