package main

import "context"

// ============ НАВЫКИ ============

func (s *PostgresStorage) GetAllSkills(ctx context.Context) ([]Skill, error) {
	query := `SELECT id, name FROM skills ORDER BY name ASC`

	rows, err := s.pool.Query(ctx, query)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var skills []Skill
	for rows.Next() {
		var skill Skill
		if err := rows.Scan(&skill.ID, &skill.Name); err != nil {
			return nil, err
		}
		skills = append(skills, skill)
	}

	if skills == nil {
		skills = []Skill{}
	}

	return skills, rows.Err()
}

func (s *PostgresStorage) CreateSkill(ctx context.Context, name string) (Skill, error) {
	query := `
		INSERT INTO skills (name)
		VALUES ($1)
		ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name
		RETURNING id, name
	`

	var skill Skill
	err := s.pool.QueryRow(ctx, query, name).Scan(&skill.ID, &skill.Name)
	if err != nil {
		return Skill{}, err
	}

	return skill, nil
}

// ============ ДОЛЖНОСТИ ============

func (s *PostgresStorage) GetAllPositions(ctx context.Context) ([]Position, error) {
	query := `SELECT id, name FROM positions ORDER BY name ASC`

	rows, err := s.pool.Query(ctx, query)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var positions []Position
	for rows.Next() {
		var p Position
		if err := rows.Scan(&p.ID, &p.Name); err != nil {
			return nil, err
		}
		positions = append(positions, p)
	}

	if positions == nil {
		positions = []Position{}
	}

	return positions, rows.Err()
}

func (s *PostgresStorage) CreatePosition(ctx context.Context, name string) (Position, error) {
	query := `
		INSERT INTO positions (name)
		VALUES ($1)
		ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name
		RETURNING id, name
	`

	var p Position
	err := s.pool.QueryRow(ctx, query, name).Scan(&p.ID, &p.Name)
	if err != nil {
		return Position{}, err
	}

	return p, nil
}
