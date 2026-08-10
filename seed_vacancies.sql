UPDATE vacancies SET 
  skills = ARRAY['Go','PostgreSQL','Docker','Kubernetes','gRPC','Микросервисы'],
  description = 'Ищем опытного бэкенд-разработчика для работы над высоконагруженными микросервисами поисковой платформы. У нас миллионы запросов в секунду, интересные задачи по оптимизации и масштабированию. Работа в команде из 8 разработчиков, code review, парное программирование.'
WHERE title = 'Senior Go Dev';

UPDATE vacancies SET 
  skills = ARRAY['Go','PostgreSQL','Redis','Docker','REST API','Микросервисы'],
  description = 'Разработка сервисов для крупнейшего маркетплейса объявлений. Удалённая работа, гибкий график, современный стек. Занимаемся системой рекомендаций и поиском по объявлениям. Команда дружная, процессы налажены.'
WHERE title = 'Middle Python Developer';

UPDATE vacancies SET 
  skills = ARRAY['Go','SQL','Docker','Linux','REST API'],
  description = 'Отличная возможность для начинающих Go-разработчиков. Менторство от senior-разработчиков, внутренние курсы, оплачиваемое обучение. Работа над банковскими сервисами с высокими требованиями к надёжности.'
WHERE title = 'Junior Go Developer';

UPDATE vacancies SET 
  skills = ARRAY['JavaScript','React','TypeScript','Redux','CSS','HTML'],
  description = 'Разработка пользовательских интерфейсов для социальных сервисов ВКонтакте. Современный стек, большие нагрузки, интересные UX-задачи. Офис у метро, гибкое начало рабочего дня.'
WHERE title = 'Frontend React Developer';

UPDATE vacancies SET 
  skills = ARRAY['Go','PostgreSQL','Docker','Kafka','gRPC','Микросервисы'],
  description = 'Senior позиция в команде бэкенда Яндекса. Работа над высоконагруженными сервисами, участие в архитектурных решениях, менторство junior-разработчиков. Современные практики: CI/CD, feature flags, observability.'
WHERE title = 'Senior Go Backend Developer';