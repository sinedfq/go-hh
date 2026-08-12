-- +goose Up
INSERT INTO vacancies (title, company, location, experience, remote, skills, description) VALUES
-- ====== РЕЛЕВАНТНЫЕ для Go-разработчика ======
('Go Backend Developer', 'Яндекс', 'Москва', 'Middle', true,
 ARRAY['Go', 'PostgreSQL', 'Docker', 'gRPC'],
 'Разработка высоконагруженных микросервисов на Go. Работа с PostgreSQL, Docker, gRPC. Участие в проектировании архитектуры.'),

('Go Developer', 'Ozon', 'Санкт-Петербург', 'Middle', true,
 ARRAY['Go', 'Kubernetes', 'Kafka', 'PostgreSQL'],
 'Разработка сервисов электронной коммерции на Go. Kubernetes, Kafka, микросервисная архитектура.'),

('Backend Developer', 'Сбер', 'Москва', 'Junior', false,
 ARRAY['Go', 'Python', 'PostgreSQL', 'SQL'],
 'Разработка backend-сервисов на Go и Python. Работа с PostgreSQL, проектирование API.'),

('Senior Go Engineer', 'Тинькофф', 'Москва', 'Senior', true,
 ARRAY['Go', 'Kafka', 'Docker', 'Kubernetes', 'PostgreSQL'],
 'Проектирование и разработка распределённых систем на Go. Kafka, Kubernetes, высокие нагрузки.'),

-- ====== ЧАСТИЧНО релевантные (Python) ======
('Python Developer', 'VK', 'Санкт-Петербург', 'Middle', true,
 ARRAY['Python', 'Django', 'PostgreSQL', 'Redis'],
 'Разработка сервисов на Python/Django. PostgreSQL, Redis, высоконагруженные системы.'),

('Data Engineer', 'Avito', 'Москва', 'Middle', true,
 ARRAY['Python', 'Airflow', 'SQL', 'Spark'],
 'Построение data-пайплайнов на Python. Airflow, Spark, обработка больших данных.'),

('ML Engineer', 'МТС', 'Москва', 'Middle', true,
 ARRAY['Python', 'Go', 'Docker', 'TensorFlow'],
 'Разработка ML-инфраструктуры. Python, Go, Docker. Обучение и деплой моделей.'),

-- ====== DevOps (если есть Docker/Kubernetes) ======
('DevOps Engineer', 'Mail.ru', 'Москва', 'Middle', true,
 ARRAY['Docker', 'Kubernetes', 'Linux', 'Go'],
 'Автоматизация инфраструктуры. Docker, Kubernetes, CI/CD. Желателен опыт с Go.'),

-- ====== НЕрелевантные (для проверки фильтра) ======
('Frontend Developer', 'Wildberries', 'Москва', 'Middle', true,
 ARRAY['React', 'JavaScript', 'TypeScript', 'CSS'],
 'Разработка интерфейсов на React. TypeScript, CSS, работа с дизайн-системой.'),

('React Developer', 'Lamoda', 'Москва', 'Junior', true,
 ARRAY['React', 'Redux', 'JavaScript', 'HTML'],
 'Разработка SPA на React. Redux, JavaScript, вёрстка.'),

('Vue Developer', 'DNS', 'Владивосток', 'Middle', false,
 ARRAY['Vue', 'JavaScript', 'HTML', 'CSS'],
 'Разработка интерфейсов на Vue.js. JavaScript, HTML, CSS.'),

('Java Developer', 'Альфа-Банк', 'Москва', 'Middle', false,
 ARRAY['Java', 'Spring', 'MySQL', 'Hibernate'],
 'Разработка банковских сервисов на Java. Spring, MySQL, Hibernate.'),

('C# Developer', 'Касперский', 'Москва', 'Senior', false,
 ARRAY['C#', '.NET', 'SQL Server', 'WPF'],
 'Разработка десктопных приложений на C#. .NET, SQL Server, WPF.'),

('QA Engineer', 'Positive Technologies', 'Москва', 'Junior', true,
 ARRAY['Python', 'Selenium', 'SQL'],
 'Автоматизация тестирования на Python. Selenium, SQL, написание тест-кейсов.'),

('Product Manager', 'X5 Group', 'Москва', 'Middle', false,
 ARRAY['Agile', 'Scrum', 'Analytics'],
 'Управление продуктом. Agile, Scrum, аналитика, работа с командой разработки.')
ON CONFLICT DO NOTHING;

-- +goose Down
DELETE FROM vacancies WHERE company IN (
    'Яндекс', 'Ozon', 'Сбер', 'Тинькофф', 'VK', 'Avito', 'МТС', 'Mail.ru',
    'Wildberries', 'Lamoda', 'DNS', 'Альфа-Банк', 'Касперский',
    'Positive Technologies', 'X5 Group'
);