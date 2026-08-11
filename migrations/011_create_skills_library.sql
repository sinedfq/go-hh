-- +goose Up
CREATE TABLE IF NOT EXISTS skills (
  id SERIAL PRIMARY KEY,
  name VARCHAR(100) UNIQUE NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_skills_name ON skills(name);

-- Предзаполнение популярными навыками
INSERT INTO skills (name) VALUES
  ('Go'), ('Python'), ('JavaScript'), ('TypeScript'), ('Java'), ('C#'),
  ('C++'), ('PHP'), ('Ruby'), ('Swift'), ('Kotlin'), ('Rust'),
  ('React'), ('Vue'), ('Angular'), ('Svelte'), ('Node.js'), ('Django'),
  ('Flask'), ('FastAPI'), ('Spring'), ('Express'), ('PostgreSQL'),
  ('MySQL'), ('MongoDB'), ('Redis'), ('SQLite'), ('Docker'),
  ('Kubernetes'), ('Git'), ('Linux'), ('HTML'), ('CSS'), ('SQL'),
  ('REST API'), ('GraphQL'), ('AWS'), ('Azure'), ('GCP'), ('CI/CD'),
  ('Nginx'), ('RabbitMQ'), ('Kafka'), ('gRPC'), ('WebSocket'),
  ('Firebase'), ('Elasticsearch'), ('ClickHouse'), ('Airflow'), ('Spark')
ON CONFLICT (name) DO NOTHING;

-- +goose Down
DROP TABLE IF EXISTS skills;