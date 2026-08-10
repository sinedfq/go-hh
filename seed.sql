-- Очистим сломанные данные и обновим заново
UPDATE vacancies SET 
  skills = ARRAY['Go','PostgreSQL','Docker','Kubernetes','gRPC','Microservices'],
  description = 'Senior backend developer for highload microservices. Millions of requests per second, optimization and scaling tasks. Team of 8 developers, code review, pair programming.'
WHERE title = 'Senior Go Dev';

UPDATE vacancies SET 
  skills = ARRAY['Go','PostgreSQL','Docker','Kafka','gRPC','Microservices'],
  description = 'Senior position in Yandex backend team. Working on highload services, participating in architectural decisions, mentoring junior developers. Modern practices: CI/CD, feature flags, observability.'
WHERE title = 'Senior Go Backend Developer';

UPDATE vacancies SET 
  skills = ARRAY['Go','PostgreSQL','Redis','Docker','REST API','Microservices'],
  description = 'Development of services for the largest classifieds marketplace. Remote work, flexible schedule, modern tech stack. Working on recommendation system and search. Friendly team, well-established processes.'
WHERE title = 'Middle Python Developer';

UPDATE vacancies SET 
  skills = ARRAY['Go','SQL','Docker','Linux','REST API'],
  description = 'Great opportunity for junior Go developers. Mentoring from senior developers, internal courses, paid training. Working on banking services with high reliability requirements.'
WHERE title = 'Junior Go Developer';

UPDATE vacancies SET 
  skills = ARRAY['JavaScript','React','TypeScript','Redux','CSS','HTML'],
  description = 'Development of user interfaces for VK social services. Modern stack, high loads, interesting UX tasks. Office near metro, flexible start time.'
WHERE title = 'Frontend React Developer';
