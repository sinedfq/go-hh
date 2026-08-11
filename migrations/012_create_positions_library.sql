-- +goose Up
CREATE TABLE IF NOT EXISTS positions (
  id SERIAL PRIMARY KEY,
  name VARCHAR(255) UNIQUE NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_positions_name ON positions(name);

-- Предзаполнение популярными должностями
INSERT INTO positions (name) VALUES
  ('Backend Developer'), ('Frontend Developer'), ('Fullstack Developer'),
  ('Go Developer'), ('Python Developer'), ('Java Developer'),
  ('JavaScript Developer'), ('TypeScript Developer'), ('C# Developer'),
  ('C++ Developer'), ('PHP Developer'), ('Ruby Developer'),
  ('iOS Developer'), ('Android Developer'), ('Mobile Developer'),
  ('DevOps Engineer'), ('SRE Engineer'), ('Cloud Engineer'),
  ('Data Engineer'), ('Data Scientist'), ('ML Engineer'),
  ('AI Engineer'), ('QA Engineer'), ('Test Automation Engineer'),
  ('System Administrator'), ('Database Administrator'),
  ('Security Engineer'), ('Network Engineer'),
  ('Product Manager'), ('Project Manager'), ('Team Lead'),
  ('Tech Lead'), ('Engineering Manager'), ('CTO'),
  ('UX Designer'), ('UI Designer'), ('Product Designer'),
  ('Business Analyst'), ('System Analyst'), ('Data Analyst'),
  ('Technical Writer'), ('Scrum Master'), ('Agile Coach'),
  ('Solution Architect'), ('Software Architect'), ('Enterprise Architect'),
  ('Support Engineer'), ('Technical Support'), ('Customer Success'),
  ('Sales Engineer'), ('Pre-sale Engineer'), ('Consultant')
ON CONFLICT (name) DO NOTHING;

-- +goose Down
DROP TABLE IF EXISTS positions;