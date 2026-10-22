// Adds fictional demo content without deleting or overwriting user data.
package main

import (
	"context"
	"crypto/rand"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"image"
	"image/color"
	"image/draw"
	"image/png"
	"log"
	"os"
	"path/filepath"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"golang.org/x/crypto/bcrypt"
	"golang.org/x/image/font"
	"golang.org/x/image/font/gofont/gobold"
	"golang.org/x/image/font/opentype"
	"golang.org/x/image/math/fixed"
)

type company struct {
	key, name, city, industry, story string
	lat, lon                         float64
	rgb                              color.RGBA
	roles                            []string
	skills                           []string
}

func main() {
	ctx := context.Background()
	database := os.Getenv("DATABASE_URL")
	if database == "" {
		database = "postgres://postgres:postgres@localhost:5432/gohh?sslmode=disable"
	}
	pool, err := pgxpool.New(ctx, database)
	if err != nil {
		log.Fatal(err)
	}
	defer pool.Close()
	tx, err := pool.Begin(ctx)
	if err != nil {
		log.Fatal(err)
	}
	defer tx.Rollback(ctx)
	if _, err = tx.Exec(ctx, "SELECT pg_advisory_xact_lock(83649721)"); err != nil {
		log.Fatal(err)
	}
	companies := []company{
		{"north", "Северный Код · демо", "Новосибирск", "Разработка ПО", "Создаём инструменты для распределённых команд. Работаем небольшими продуктовыми группами.", 55.03, 82.92, color.RGBA{33, 99, 202, 255}, []string{"Go-разработчик платформы", "Android-разработчик", "Инженер по тестированию", "Стажёр frontend-разработчик"}, []string{"Go", "Kotlin", "React", "PostgreSQL"}},
		{"orbit", "Орбита Данных · демо", "Томск", "Аналитика данных", "Помогаем командам находить смысл в данных: от сбора событий до визуальных отчётов.", 56.48, 84.95, color.RGBA{99, 80, 190, 255}, []string{"Data Engineer", "Python-разработчик", "Аналитик данных", "ML-инженер"}, []string{"Python", "SQL", "Airflow", "ClickHouse"}},
		{"studio", "Точка Интерфейса · демо", "Казань", "Дизайн и цифровые продукты", "Проектируем понятные интерфейсы и проверяем идеи вместе с пользователями.", 55.79, 49.12, color.RGBA{0, 135, 134, 255}, []string{"Продуктовый дизайнер", "Frontend-разработчик React", "UX-исследователь", "Менеджер продукта"}, []string{"Figma", "React", "TypeScript", "UX"}},
		{"cloud", "Облако Рядом · демо", "Москва", "Облачные технологии", "Развиваем инфраструктурные сервисы, автоматизацию и наблюдаемость.", 55.75, 37.62, color.RGBA{41, 118, 162, 255}, []string{"DevOps-инженер", "SRE-инженер", "Backend-разработчик Java", "Инженер технической поддержки"}, []string{"Docker", "Kubernetes", "Linux", "Java"}},
		{"care", "Здоровый Ритм · демо", "Санкт-Петербург", "Цифровая медицина", "Создаём сервис записи на приём и инструменты коммуникации для специалистов.", 59.94, 30.31, color.RGBA{35, 139, 103, 255}, []string{"iOS-разработчик", "Системный аналитик", "Менеджер проектов", "Специалист клиентского сервиса"}, []string{"Swift", "SQL", "REST API", "Agile"}},
		{"route", "Маршрут Лаб · демо", "Екатеринбург", "Логистика", "Работаем над удобными маршрутами, планированием доставок и приложениями для партнёров.", 56.84, 60.61, color.RGBA{190, 103, 39, 255}, []string{"Fullstack-разработчик", "Бизнес-аналитик", "Дизайнер мобильных интерфейсов", "Специалист по подбору персонала"}, []string{"TypeScript", "Node.js", "Figma", "Коммуникация"}},
	}
	insertedCompanies, insertedJobs := 0, 0
	for i, c := range companies {
		logo := fmt.Sprintf("demo_%s_logo.png", c.key)
		cover := fmt.Sprintf("demo_%s_product.png", c.key)
		if err = artwork(logo, string([]rune(c.name)[0]), c.rgb, false); err != nil {
			log.Fatal(err)
		}
		if err = artwork(cover, "КОМАНДА / ПРОДУКТ / РОСТ", c.rgb, true); err != nil {
			log.Fatal(err)
		}
		var id int
		err = tx.QueryRow(ctx, "SELECT id FROM companies WHERE profile->>'demo_key'=$1 LIMIT 1", c.key).Scan(&id)
		if errors.Is(err, pgx.ErrNoRows) {
			profile, _ := json.Marshal(map[string]any{"demo_key": c.key, "team_story": c.story, "benefits": []string{"Обучение и обмен опытом", "Гибкое начало рабочего дня", "Оборудование для работы"}, "hiring_process": "Короткое знакомство → беседа с командой → предложение", "photos": []string{"/uploads/" + cover}, "it_accredited": i < 4})
			err = tx.QueryRow(ctx, "INSERT INTO companies(name,description,industry,size,city,website,logo_url,profile) VALUES($1,$2,$3,$4,$5,'',$6,$7) RETURNING id", c.name, "Демонстрационная компания Trudomix. Вакансии созданы для знакомства с интерфейсом. "+c.story, c.industry, "11-50", c.city, "/uploads/"+logo, profile).Scan(&id)
			if err != nil {
				log.Fatal(err)
			}
			insertedCompanies++
		} else if err != nil {
			log.Fatal(err)
		}
		var author int
		var userCompany *int
		email := "demo+" + c.key + "@trudomix.example"
		err = tx.QueryRow(ctx, "SELECT id,company_id FROM users WHERE email=$1", email).Scan(&author, &userCompany)
		if errors.Is(err, pgx.ErrNoRows) {
			secret := make([]byte, 32)
			if _, err = rand.Read(secret); err != nil {
				log.Fatal(err)
			}
			hash, e := bcrypt.GenerateFromPassword([]byte(hex.EncodeToString(secret)), bcrypt.DefaultCost)
			if e != nil {
				log.Fatal(e)
			}
			if err = tx.QueryRow(ctx, "INSERT INTO users(email,password_hash,role,company_id) VALUES($1,$2,'employer',$3) RETURNING id", email, string(hash), id).Scan(&author); err != nil {
				log.Fatal(err)
			}
		} else if err != nil {
			log.Fatal(err)
		} else if userCompany == nil || *userCompany != id {
			log.Fatalf("demo email belongs to another company: %s", email)
		}
		for j, title := range c.roles {
			grade := []string{"Middle", "Senior", "Junior", "Intern"}[j]
			remote := j%2 == 0
			salary := 80000 + (i * 10000) + (3-j)*30000
			terms, _ := json.Marshal(map[string]any{"salary_from": salary, "salary_to": salary + 60000, "currency": "RUB", "salary_gross": j%2 == 0, "employment": []string{"full_time", "full_time", "part_time", "internship"}[j], "schedule": []string{"flexible", "full_day", "flexible", "full_day"}[j]})
			description := fmt.Sprintf("> Демонстрационная вакансия. Создана для знакомства с Trudomix, реальный набор не ведётся.\n\n## О команде\n%s\n\n## Чем предстоит заниматься\n- Развивать продукт вместе с коллегами\n- Обсуждать решения и делиться обратной связью\n- Улучшать качество и удобство сервиса\n\n## Что мы предлагаем\n**Прозрачные задачи**, поддержку наставника и время на обучение.\n\n![Направления работы команды](/uploads/%s)\n\n### Как познакомиться\n1. Расскажите о своём опыте\n2. Обсудим подходящие задачи\n3. Познакомим с командой", c.story, cover)
			result, e := tx.Exec(ctx, "INSERT INTO vacancies(title,company,company_id,location,experience,remote,skills,description,address,latitude,longitude,views,author_user_id,terms) SELECT $1::text,$2::text,$3::integer,$4::text,$5::text,$6::boolean,$7::text[],$8::text,$4::text,$9::double precision,$10::double precision,0,$11::integer,$12::jsonb WHERE NOT EXISTS(SELECT 1 FROM vacancies WHERE company_id=$3::integer AND title=$1::text)", title, c.name, id, c.city, grade, remote, c.skills, description, c.lat, c.lon, author, terms)
			if e != nil {
				log.Fatal(e)
			}
			insertedJobs += int(result.RowsAffected())
		}
	}
	if err = tx.Commit(ctx); err != nil {
		log.Fatal(err)
	}
	fmt.Printf("Добавлено демо-компаний: %d; демо-вакансий: %d. Существующие данные сохранены.\n", insertedCompanies, insertedJobs)
}

func artwork(name, label string, tint color.RGBA, wide bool) error {
	w, h := 240, 240
	if wide {
		w, h = 1000, 420
	}
	canvas := image.NewRGBA(image.Rect(0, 0, w, h))
	draw.Draw(canvas, canvas.Bounds(), image.NewUniform(tint), image.Point{}, draw.Src)
	parsed, err := opentype.Parse(gobold.TTF)
	if err != nil {
		return err
	}
	size := float64(110)
	if wide {
		size = 36
	}
	face, err := opentype.NewFace(parsed, &opentype.FaceOptions{Size: size, DPI: 72, Hinting: font.HintingFull})
	if err != nil {
		return err
	}
	defer face.Close()
	if wide {
		for i := 0; i < 3; i++ {
			r := image.Rect(60+i*310, 80, 320+i*310, 210)
			draw.Draw(canvas, r, image.NewUniform(color.RGBA{255, 255, 255, 35}), image.Point{}, draw.Over)
		}
	}
	d := font.Drawer{Dst: canvas, Src: image.White, Face: face}
	width := d.MeasureString(label).Ceil()
	d.Dot = fixed.P((w-width)/2, h/2+int(size/3))
	d.DrawString(label)
	if err = os.MkdirAll("uploads", 0755); err != nil {
		return err
	}
	f, err := os.Create(filepath.Join("uploads", name))
	if err != nil {
		return err
	}
	defer f.Close()
	return png.Encode(f, canvas)
}
