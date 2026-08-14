package main

import (
	"context"
	"fmt"
	"log"

	"github.com/jackc/pgx/v5/pgxpool"
)

func main() {
	ctx := context.Background()
	pool, err := pgxpool.New(ctx, "postgres://postgres:postgres@localhost:5432/gohh?sslmode=disable")
	if err != nil {
		log.Fatal(err)
	}
	defer pool.Close()

	// ====== Шаг 1: Удалить ВСЕ вакансии ======
	_, err = pool.Exec(ctx, `DELETE FROM vacancies`)
	if err != nil {
		log.Fatalf("Ошибка удаления вакансий: %v", err)
	}
	fmt.Println("✅ Все вакансии удалены")

	// ====== Шаг 2: Создать недостающие компании (если их нет) ======
	companiesToEnsure := []string{
		"Яндекс", "Сбер", "Тинькофф", "Авито", "VK",
		"Ozon", "Альфа-Банк", "МТС", "Wildberries", "Касперский", "X5 Group",
	}
	for _, c := range companiesToEnsure {
		_, _ = pool.Exec(ctx, `INSERT INTO companies (name) VALUES ($1) ON CONFLICT (name) DO NOTHING`, c)
	}
	fmt.Println("✅ Компании проверены")

	// ====== Шаг 3: Создать новые вакансии ======
	// Часть с точными координатами, часть только с городом (для заглушки на карте)
	sql := `
		INSERT INTO vacancies (title, company, company_id, location, experience, remote, skills, description, address, latitude, longitude, views, author_user_id)
		VALUES 
			-- ===== С ТОЧНЫМИ КООРДИНАТАМИ (карта работает) =====

			-- 1. Яндекс, Москва (офис на Льва Толстого)
			('Senior Go Developer', 'Яндекс', 
			 (SELECT id FROM companies WHERE name = 'Яндекс'), 
			 'Москва', 'Senior', false, 
			 ARRAY['Go','PostgreSQL','Docker','Kubernetes','gRPC','Microservices'],
			 'Senior backend developer для высоконагруженных микросервисов. Миллионы запросов в секунду, оптимизация и масштабирование. Команда из 8 разработчиков, code review, pair programming.',
			 'Москва, ул. Льва Толстого, 16', 55.7352, 37.5878, 142, 1),

			-- 2. Сбер, Москва (Сити)
			('Middle Go Developer', 'Сбер', 
			 (SELECT id FROM companies WHERE name = 'Сбер'),
			 'Москва', 'Middle', false,
			 ARRAY['Go','PostgreSQL','Redis','Docker','Kubernetes','gRPC'],
			 'Разработка микросервисов для финансовых продуктов. Команда из 12 человек, agile-процессы, регулярные ретро и планирования.',
			 'Москва, Пресненская наб., 8с1', 55.7497, 37.5374, 89, 1),

			-- 3. Тинькофф, Санкт-Петербург
			('Senior Backend Engineer', 'Тинькофф', 
			 (SELECT id FROM companies WHERE name = 'Тинькофф'),
			 'Санкт-Петербург', 'Senior', true,
			 ARRAY['Go','PostgreSQL','Kafka','Redis','Docker','Kubernetes'],
			 'Разработка высоконагруженных банковских сервисов. Миллионы транзакций в день, строгие SLA, мониторинг 24/7.',
			 'Санкт-Петербург, Пулковское шоссе, 56', 59.7972, 30.3300, 213, 1),

			-- 4. Ozon, Санкт-Петербург
			('Go Developer', 'Ozon', 
			 (SELECT id FROM companies WHERE name = 'Ozon'),
			 'Санкт-Петербург', 'Middle', false,
			 ARRAY['Go','PostgreSQL','Kafka','Redis','Docker','gRPC'],
			 'Разработка сервисов e-commerce платформы. Большие объёмы данных, интеграция с множеством систем.',
			 'Санкт-Петербург, ул. Оптиков, 4к2', 59.9951, 30.2663, 76, 1),

			-- 5. VK, Москва
			('Backend Developer', 'VK', 
			 (SELECT id FROM companies WHERE name = 'VK'),
			 'Москва', 'Middle', false,
			 ARRAY['Go','PostgreSQL','Redis','Docker','Kafka','Prometheus'],
			 'Разработка backend для социальных сервисов VK. Работа с огромными объёмами данных, оптимизация производительности.',
			 'Москва, Ленинградский проспект, 39с4', 55.8074, 37.5113, 164, 1),

			-- 6. Альфа-Банк, Москва
			('Senior Go Developer', 'Альфа-Банк', 
			 (SELECT id FROM companies WHERE name = 'Альфа-Банк'),
			 'Москва', 'Senior', false,
			 ARRAY['Go','PostgreSQL','Kafka','Redis','Kubernetes','CI/CD'],
			 'Разработка финтех-микросервисов. Работа с платежами, интеграция с ЦБ РФ, строгий code review.',
			 'Москва, ул. Каланчёвская, 27', 55.7756, 37.6572, 95, 1),

			-- 7. Касперский, Новосибирск (филиал)
			('Middle Go Developer', 'Касперский', 
			 (SELECT id FROM companies WHERE name = 'Касперский'),
			 'Новосибирск', 'Middle', false,
			 ARRAY['Go','PostgreSQL','Docker','Linux','gRPC'],
			 'Разработка сервисов кибербезопасности. Работа с низкоуровневым кодом, анализ угроз в реальном времени.',
			 'Новосибирск, Академгородок, ул. Инженерная, 1', 54.8477, 83.0939, 38, 1),

			-- ===== ТОЛЬКО ГОРОД (координаты NULL — на карте заглушка) =====

			-- 8. Авито, Москва
			('Backend Developer', 'Авито', 
			 (SELECT id FROM companies WHERE name = 'Авито'),
			 'Москва', 'Middle', true,
			 ARRAY['Go','PostgreSQL','Redis','Docker','Kafka'],
			 'Разработка высоконагруженных сервисов для платформы Авито. Работа с микросервисной архитектурой, оптимизация запросов.',
			 '', 0, 0, 128, 1),

			-- 9. МТС, Екатеринбург
			('Go Developer', 'МТС', 
			 (SELECT id FROM companies WHERE name = 'МТС'),
			 'Екатеринбург', 'Junior', false,
			 ARRAY['Go','PostgreSQL','Docker','Linux'],
			 'Разработка телеком-сервисов. Отличная возможность для джуна: опытные менторы, план развития, обучение за счёт компании.',
			 '', 0, 0, 54, 1),

			-- 10. Wildberries, Казань
			('Junior Go Developer', 'Wildberries', 
			 (SELECT id FROM companies WHERE name = 'Wildberries'),
			 'Казань', 'Junior', true,
			 ARRAY['Go','PostgreSQL','Docker','Git'],
			 'Разработка логистических сервисов для крупнейшего маркетплейса. Обучаем с нуля, даём ментора, гибкий график.',
			 '', 0, 0, 67, 1),

			-- 11. X5 Group, Москва (удалённо)
			('Middle Backend Developer', 'X5 Group', 
			 (SELECT id FROM companies WHERE name = 'X5 Group'),
			 'Москва', 'Middle', true,
			 ARRAY['Go','PostgreSQL','Kafka','Redis','Docker'],
			 'Разработка сервисов для розничной торговли. Работа с большими объёмами данных, интеграция с ERP-системами.',
			 '', 0, 0, 43, 1),

			-- 12. Сбер, Казань (Junior)
			('Junior Go Developer', 'Сбер', 
			 (SELECT id FROM companies WHERE name = 'Сбер'),
			 'Казань', 'Junior', false,
			 ARRAY['Go','PostgreSQL','Docker','Git','SQL'],
			 'Первая работа для начинающего Go-разработчика. Обучение, менторство, возможность роста до Middle за 1-2 года.',
			 '', 0, 0, 31, 1)
	`

	result, err := pool.Exec(ctx, sql)
	if err != nil {
		log.Fatalf("Ошибка создания вакансий: %v", err)
	}

	fmt.Printf("✅ Создано вакансий: %d\n", result.RowsAffected())

	// ====== Шаг 4: Показать что получилось ======
	rows, err := pool.Query(ctx, `
		SELECT v.id, v.title, v.company, 
		       CASE WHEN v.latitude != 0 THEN '✅ координаты' ELSE '❌ только город' END AS has_coords
		FROM vacancies v
		ORDER BY v.id
	`)
	if err != nil {
		log.Fatal(err)
	}
	defer rows.Close()

	fmt.Println("\n📋 Список вакансий:")
	for rows.Next() {
		var id int
		var title, company, coords string
		if err := rows.Scan(&id, &title, &company, &coords); err != nil {
			log.Fatal(err)
		}
		fmt.Printf("  [%2d] %-35s %-15s %s\n", id, title, company, coords)
	}
}
