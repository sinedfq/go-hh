package main

import (
	"bytes"
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"mime/multipart"
	"net/http"
	"net/http/httptest"
	"net/textproto"
	"os"
	"strings"
	"testing"
	"time"
)

func TestProductGrowthFlows(t *testing.T) {
	database := os.Getenv("TRUDOMIX_TEST_DATABASE_URL")
	if database == "" {
		t.Skip("set TRUDOMIX_TEST_DATABASE_URL")
	}
	ctx := context.Background()
	admin, err := pgxpool.New(ctx, database)
	if err != nil {
		t.Fatal(err)
	}
	defer admin.Close()
	schema := pgx.Identifier{fmt.Sprintf("codex_product_%d", time.Now().UnixNano())}.Sanitize()
	if _, err = admin.Exec(ctx, "CREATE SCHEMA "+schema); err != nil {
		t.Fatal(err)
	}
	defer func() {
		if _, err := admin.Exec(ctx, "DROP SCHEMA "+schema+" CASCADE"); err != nil {
			t.Error(err)
		}
	}()
	config, err := pgxpool.ParseConfig(database)
	if err != nil {
		t.Fatal(err)
	}
	config.ConnConfig.RuntimeParams["search_path"] = schema
	pool, err := pgxpool.NewWithConfig(ctx, config)
	if err != nil {
		t.Fatal(err)
	}
	defer pool.Close()
	_, err = pool.Exec(ctx, `CREATE TABLE users(id SERIAL PRIMARY KEY,email TEXT,password_hash TEXT,photo_url TEXT,role TEXT,company_id INT,created_at TIMESTAMPTZ DEFAULT NOW());
 CREATE TABLE companies(id SERIAL PRIMARY KEY,name TEXT,description TEXT,industry TEXT,size TEXT,city TEXT,website TEXT,logo_url TEXT,photo_url TEXT,created_at TIMESTAMPTZ DEFAULT NOW());
 CREATE TABLE vacancies(id SERIAL PRIMARY KEY,title TEXT,company TEXT,company_id INT,location TEXT,experience TEXT,remote BOOLEAN,skills TEXT[],description TEXT,address TEXT,latitude DOUBLE PRECISION,longitude DOUBLE PRECISION,views INT DEFAULT 0,author_user_id INT,is_archived BOOLEAN DEFAULT FALSE,created_at TIMESTAMPTZ DEFAULT NOW(),updated_at TIMESTAMPTZ DEFAULT NOW());
 CREATE TABLE applications(id SERIAL PRIMARY KEY,vacancy_id INT,candidate_user_id INT,resume_id INT,cover_letter TEXT,status TEXT DEFAULT 'new',created_at TIMESTAMPTZ DEFAULT NOW(),viewed_at TIMESTAMPTZ);
 CREATE TABLE notifications(id SERIAL PRIMARY KEY,user_id INT,type TEXT,title TEXT,message TEXT,data JSONB,is_read BOOLEAN DEFAULT FALSE,created_at TIMESTAMPTZ DEFAULT NOW());
 INSERT INTO companies(id,name) VALUES(10,'Team'),(20,'Other');
 INSERT INTO users(id,email,password_hash,role,company_id) VALUES(1,'candidate@test','hash','candidate',NULL),(2,'employer@test','hash','employer',10),(3,'foreign@test','hash','employer',20),(4,'outsider@test','hash','candidate',NULL);
 INSERT INTO applications(id,status,created_at,viewed_at) VALUES(100,'accepted',NOW()-INTERVAL '2 days',NOW()-INTERVAL '1 day');`)
	if err != nil {
		t.Fatal(err)
	}
	migration, err := os.ReadFile("../../migrations/025_product_growth.sql")
	if err != nil {
		t.Fatal(err)
	}
	if _, err = pool.Exec(ctx, strings.Split(string(migration), "-- +goose Down")[0]); err != nil {
		t.Fatal(err)
	}
	t.Chdir(t.TempDir())
	server := NewServer(&PostgresStorage{pool: pool}, nil)
	mux := http.NewServeMux()
	employer := func(fn http.HandlerFunc) http.HandlerFunc {
		return server.authMiddleware(server.RequireRole("employer", "admin")(fn))
	}
	candidate := func(fn http.HandlerFunc) http.HandlerFunc {
		return server.authMiddleware(server.RequireRole("candidate", "admin")(fn))
	}
	mux.HandleFunc("POST /api/vacancies", employer(server.createVacancyHandler))
	mux.HandleFunc("GET /api/vacancies/search", server.searchVacanciesHandler)
	mux.HandleFunc("PUT /api/my-company", employer(server.updateCompanyProfileHandler))
	mux.HandleFunc("POST /api/my-company/photos", employer(server.companyGalleryHandler))
	mux.HandleFunc("DELETE /api/my-company/photos", employer(server.companyGalleryHandler))
	mux.HandleFunc("POST /api/saved-searches", candidate(server.savedSearchesHandler))
	mux.HandleFunc("GET /api/saved-searches", candidate(server.savedSearchesHandler))
	mux.HandleFunc("DELETE /api/saved-searches/{id}", candidate(server.savedSearchesHandler))
	mux.HandleFunc("GET /api/applications/{id}/history", server.authMiddleware(server.applicationHistoryHandler))
	mux.HandleFunc("PATCH /api/applications/{id}/status", employer(server.updateApplicationStatusHandler))
	mux.HandleFunc("GET /api/my-resume/pdf", server.authMiddleware(server.downloadMyResumeHandler))
	mux.HandleFunc("POST /api/vacancies/images", employer(server.uploadVacancyImageHandler))
	mux.HandleFunc("DELETE /api/work-experience/{id}", server.authMiddleware(server.deleteWorkExperienceHandler))
	mux.HandleFunc("POST /api/work-experience", server.authMiddleware(server.addWorkExperienceHandler))
	request := func(user int, method, path string, body []byte, kind string) *httptest.ResponseRecorder {
		t.Helper()
		r := httptest.NewRequest(method, path, bytes.NewReader(body))
		if kind == "" {
			kind = "application/json"
		}
		r.Header.Set("Content-Type", kind)
		if user > 0 {
			role := "candidate"
			if user == 2 || user == 3 {
				role = "employer"
			}
			token, e := GenerateToken(user, "test@example.invalid", role)
			if e != nil {
				t.Fatal(e)
			}
			r.Header.Set("Authorization", "Bearer "+token)
		}
		w := httptest.NewRecorder()
		mux.ServeHTTP(w, r)
		return w
	}
	check := func(w *httptest.ResponseRecorder, status int) {
		t.Helper()
		if w.Code != status {
			t.Fatalf("HTTP %d want %d: %s", w.Code, status, w.Body.String())
		}
	}
	createJob := func(currency string) int {
		t.Helper()
		body, _ := json.Marshal(map[string]interface{}{"title": "Go developer", "location": "Новосибирск", "experience": "Middle", "skills": []string{"Go"}, "salary_from": 100000, "salary_to": 150000, "currency": currency, "employment": "full_time", "schedule": "flexible"})
		w := request(2, "POST", "/api/vacancies", body, "")
		check(w, 201)
		var v Vacancy
		if err := json.Unmarshal(w.Body.Bytes(), &v); err != nil {
			t.Fatal(err)
		}
		return v.ID
	}
	first := createJob("RUB")
	t.Run("salary filters and validation", func(t *testing.T) {
		for _, tc := range []struct {
			query string
			total int
		}{{"salary_min=120000&salary_max=160000&currency=RUB&employment=full_time&schedule=flexible", 1}, {"salary_min=200000", 0}, {"salary_max=50000", 0}, {"salary_only=true&currency=USD", 0}} {
			w := request(0, "GET", "/api/vacancies/search?"+tc.query, nil, "")
			check(w, 200)
			var result struct {
				Vacancies []Vacancy `json:"vacancies"`
				Total     int       `json:"total_count"`
			}
			if err := json.Unmarshal(w.Body.Bytes(), &result); err != nil {
				t.Fatal(err)
			}
			if result.Total != tc.total {
				t.Fatalf("%s returned %d", tc.query, result.Total)
			}
		}
		check(request(0, "GET", "/api/vacancies/search?salary_min=200000&salary_max=100000", nil, ""), 400)
		check(request(2, "POST", "/api/vacancies", []byte(`{"title":"Bad","location":"City","experience":"Middle","salary_from":200,"salary_to":100}`), ""), 400)
		v, err := server.storage.GetVacancyByID(ctx, first)
		if err != nil || v.SalaryTo != 150000 {
			t.Fatalf("terms not persisted: %+v %v", v, err)
		}
	})
	t.Run("private resume PDF export", func(t *testing.T) {
		_, err := pool.Exec(ctx, `CREATE TABLE resumes(id INT PRIMARY KEY,user_id INT,full_name TEXT,desired_position TEXT,experience TEXT,skills TEXT[],about TEXT,city TEXT,remote BOOLEAN,phone TEXT,telegram TEXT,github TEXT,linkedin TEXT,photo_url TEXT,views INT,created_at TIMESTAMPTZ DEFAULT NOW());
		CREATE TABLE work_experience(id INT PRIMARY KEY,resume_id INT,company TEXT,position TEXT,start_date TIMESTAMPTZ,end_date TIMESTAMPTZ,description TEXT,created_at TIMESTAMPTZ DEFAULT NOW());
		INSERT INTO resumes(id,user_id,full_name,desired_position,experience,skills,about,city,remote) VALUES(77,1,'Александр Иванов','Разработчик','Middle',ARRAY['Go'],'О себе','Новосибирск',TRUE);
		INSERT INTO work_experience(id,resume_id,company,position,start_date,description) VALUES(1,77,'Команда','Инженер',NOW(),'Сервисы');`)
		if err != nil {
			t.Fatal(err)
		}
		check(request(0, "GET", "/api/my-resume/pdf", nil, ""), 401)
		check(request(4, "GET", "/api/my-resume/pdf?user_id=1", nil, ""), 404)
		w := request(1, "GET", "/api/my-resume/pdf", nil, "")
		check(w, 200)
		if w.Header().Get("Content-Type") != "application/pdf" || w.Header().Get("Cache-Control") != "private, no-store" || !strings.Contains(w.Header().Get("Content-Disposition"), "trudomix-resume-77.pdf") || !bytes.HasPrefix(w.Body.Bytes(), []byte("%PDF-")) {
			t.Fatalf("invalid PDF download: %v", w.Header())
		}
	})
	t.Run("work history ownership and dates", func(t *testing.T) {
		check(request(4, "DELETE", "/api/work-experience/1", nil, ""), 404)
		check(request(1, "POST", "/api/work-experience", []byte(`{"company":"Team","position":"Engineer","start_date":"2035-01-01","end_date":"2026-01-01"}`), ""), 400)
		check(request(1, "DELETE", "/api/work-experience/1", nil, ""), 200)
	})
	t.Run("vacancy Markdown image upload", func(t *testing.T) {
		var body bytes.Buffer
		writer := multipart.NewWriter(&body)
		part, e := writer.CreateFormFile("photo", "office.png")
		if e != nil {
			t.Fatal(e)
		}
		data, _ := base64.StdEncoding.DecodeString("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a3ioAAAAASUVORK5CYII=")
		part.Write(data)
		writer.Close()
		check(request(0, "POST", "/api/vacancies/images", body.Bytes(), writer.FormDataContentType()), 401)
		check(request(1, "POST", "/api/vacancies/images", body.Bytes(), writer.FormDataContentType()), 403)
		w := request(2, "POST", "/api/vacancies/images", body.Bytes(), writer.FormDataContentType())
		check(w, 201)
		var result map[string]string
		json.Unmarshal(w.Body.Bytes(), &result)
		if !strings.HasPrefix(result["url"], "/uploads/vacancy_company_10_") {
			t.Fatal(result)
		}
	})
	t.Run("company profile and gallery", func(t *testing.T) {
		body := []byte(`{"name":"New team","it_accredited":true,"team_story":"Our team","benefits":["Обучение"],"hiring_process":"Знакомство и интервью","website":"https://example.test"}`)
		check(request(1, "PUT", "/api/my-company", body, ""), 403)
		check(request(2, "PUT", "/api/my-company", body, ""), 200)
		v, err := server.storage.GetVacancyByID(ctx, first)
		if err != nil || v.Company != "New team" || !v.CompanyITAccredited {
			t.Fatalf("company name not propagated: %+v %v", v, err)
		}
		var uploaded string
		for i := 0; i < 7; i++ {
			var body bytes.Buffer
			writer := multipart.NewWriter(&body)
			header := textproto.MIMEHeader{}
			header.Set("Content-Disposition", `form-data; name="photo"; filename="team.png"`)
			header.Set("Content-Type", "image/png")
			part, _ := writer.CreatePart(header)
			png, _ := base64.StdEncoding.DecodeString("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a3ioAAAAASUVORK5CYII=")
			_, _ = part.Write(png)
			_ = writer.Close()
			w := request(2, "POST", "/api/my-company/photos", body.Bytes(), writer.FormDataContentType())
			if i == 6 {
				check(w, 409)
			} else {
				check(w, 200)
				var c CompanyWithStats
				_ = json.Unmarshal(w.Body.Bytes(), &c)
				if len(c.Photos) != i+1 {
					t.Fatal(c.Photos)
				}
				uploaded = c.Photos[0]
			}
		}
		encoded, _ := json.Marshal(map[string]string{"url": uploaded})
		check(request(3, "DELETE", "/api/my-company/photos", encoded, ""), 200)
		c, err := server.storage.GetCompanyByID(ctx, 10)
		if err != nil || len(c.Photos) != 6 {
			t.Fatalf("foreign deletion changed gallery: %+v %v", c, err)
		}
		check(request(2, "DELETE", "/api/my-company/photos", encoded, ""), 200)
		c, err = server.storage.GetCompanyByID(ctx, 10)
		if err != nil || len(c.Photos) != 5 {
			t.Fatalf("photo not removed: %+v %v", c, err)
		}
	})
	var second int
	t.Run("saved search future matches and deduplication", func(t *testing.T) {
		w := request(1, "POST", "/api/saved-searches", []byte(`{"name":"Go jobs","query":"q=Go&salary_min=120000&currency=RUB"}`), "")
		check(w, 201)
		var saved SavedSearch
		_ = json.Unmarshal(w.Body.Bytes(), &saved)
		check(request(4, "DELETE", fmt.Sprintf("/api/saved-searches/%d", saved.ID), nil, ""), 404)
		if err := server.checkSavedSearches(ctx); err != nil {
			t.Fatal(err)
		}
		var count int
		if err := pool.QueryRow(ctx, "SELECT COUNT(*) FROM notifications WHERE type='saved_search_match'").Scan(&count); err != nil || count != 0 {
			t.Fatalf("old vacancies notified: %d %v", count, err)
		}
		second = createJob("RUB")
		_ = createJob("USD")
		for i := 0; i < 2; i++ {
			if err := server.checkSavedSearches(ctx); err != nil {
				t.Fatal(err)
			}
		}
		if err := pool.QueryRow(ctx, "SELECT COUNT(*) FROM notifications WHERE type='saved_search_match' AND user_id=1").Scan(&count); err != nil || count != 1 {
			t.Fatalf("missing/duplicate/wrong-currency notifications: %d %v", count, err)
		}
		w = request(4, "GET", "/api/saved-searches", nil, "")
		check(w, 200)
		if strings.TrimSpace(w.Body.String()) != "[]" {
			t.Fatal("searches leaked", w.Body.String())
		}
	})
	t.Run("application history access and scheduling", func(t *testing.T) {
		if second == 0 {
			t.Fatal("no job")
		}
		var id int
		if err := pool.QueryRow(ctx, "INSERT INTO applications(vacancy_id,candidate_user_id) VALUES($1,1) RETURNING id", second).Scan(&id); err != nil {
			t.Fatal(err)
		}
		path := fmt.Sprintf("/api/applications/%d/history", id)
		for _, tc := range []struct{ user, status int }{{0, 401}, {1, 200}, {2, 200}, {3, 403}, {4, 403}} {
			check(request(tc.user, "GET", path, nil, ""), tc.status)
		}
		body := []byte(`{"status":"accepted","feedback":"Приглашаем познакомиться","interview_at":"2026-11-01T10:00:00+07:00"}`)
		statusPath := fmt.Sprintf("/api/applications/%d/status", id)
		check(request(1, "PATCH", statusPath, body, ""), 403)
		if _, err := pool.Exec(ctx, "UPDATE users SET role='candidate' WHERE id=2"); err != nil {
			t.Fatal(err)
		}
		check(request(2, "PATCH", statusPath, body, ""), 403)
		if _, err := pool.Exec(ctx, "UPDATE users SET role='employer' WHERE id=2"); err != nil {
			t.Fatal(err)
		}
		check(request(3, "PATCH", statusPath, body, ""), 403)
		check(request(2, "PATCH", statusPath, body, ""), 200)
		check(request(2, "PATCH", statusPath, body, ""), 200)
		w := request(1, "GET", path, nil, "")
		var events []ApplicationEvent
		if err := json.Unmarshal(w.Body.Bytes(), &events); err != nil {
			t.Fatal(err)
		}
		if len(events) != 2 || events[1].Feedback != "Приглашаем познакомиться" || events[1].InterviewAt == nil {
			t.Fatalf("wrong history: %+v", events)
		}
		check(request(2, "PATCH", statusPath, []byte(`{"status":"accepted","interview_at":"not a date"}`), ""), 400)
		var unknown int
		if err := pool.QueryRow(ctx, "SELECT COUNT(*) FROM application_events WHERE application_id=100 AND created_at IS NULL").Scan(&unknown); err != nil || unknown != 1 {
			t.Fatalf("invented legacy timestamp: %d %v", unknown, err)
		}
	})
}
