package main

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"os"
	"sync/atomic"
	"testing"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

func TestRecommendationsEndpointWithUnavailableML(t *testing.T) {
	database := os.Getenv("TRUDOMIX_TEST_DATABASE_URL")
	if database == "" {
		t.Skip("set TRUDOMIX_TEST_DATABASE_URL for isolated PostgreSQL integration")
	}
	ctx := context.Background()
	admin, err := pgxpool.New(ctx, database)
	if err != nil {
		t.Fatal(err)
	}
	defer admin.Close()
	schema := pgx.Identifier{fmt.Sprintf("codex_recommend_%d", time.Now().UnixNano())}.Sanitize()
	if _, err := admin.Exec(ctx, "CREATE SCHEMA "+schema); err != nil {
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
	_, err = pool.Exec(ctx, `
CREATE TABLE resumes(id INT,user_id INT,full_name TEXT,desired_position TEXT,experience TEXT,skills TEXT[],about TEXT,city TEXT,remote BOOLEAN,phone TEXT,telegram TEXT,github TEXT,linkedin TEXT,photo_url TEXT,views INT,created_at TIMESTAMPTZ);
CREATE TABLE work_experience(id INT,resume_id INT,company TEXT,position TEXT,start_date DATE,end_date DATE,description TEXT,created_at TIMESTAMPTZ);
CREATE TABLE companies(id INT,logo_url TEXT,profile JSONB);
CREATE TABLE vacancies(id INT,title TEXT,company TEXT,company_id INT,location TEXT,experience TEXT,remote BOOLEAN,skills TEXT[],description TEXT,address TEXT,latitude DOUBLE PRECISION,longitude DOUBLE PRECISION,views INT,created_at TIMESTAMPTZ,is_archived BOOLEAN,terms JSONB);
CREATE TABLE recommendation_dismissals(user_id INT, vacancy_id INT, created_at TIMESTAMPTZ DEFAULT NOW(), PRIMARY KEY(user_id,vacancy_id));
INSERT INTO resumes VALUES(9,42,'Test Candidate','Go Developer','Middle',ARRAY['Go','PostgreSQL'],'','Test City',true,'','','','','',0,NOW());
INSERT INTO companies VALUES(1,'','{}');
INSERT INTO vacancies VALUES(7,'Go Developer','Test Company',1,'Test City','Middle',true,ARRAY['Go','PostgreSQL'],'Build services','',0,0,0,NOW(),false,'{}');`)
	if err != nil {
		t.Fatal(err)
	}
	var unavailable atomic.Bool
	unavailable.Store(true)
	ml := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if unavailable.Load() {
			http.Error(w, "temporarily unavailable", 503)
			return
		}
		_, _ = w.Write([]byte(`{"matches":[{"vacancy_id":7,"score":0.91,"reasoning":"ML restored"}]}`))
	}))
	defer ml.Close()
	s := NewServer(&PostgresStorage{pool: pool}, NewMLClient(ml.URL))
	api := httptest.NewServer(s.authMiddleware(s.recommendationsHandler))
	defer api.Close()
	token, err := GenerateToken(42, "candidate@example.test", "candidate")
	if err != nil {
		t.Fatal(err)
	}
	get := func(query string, wantModel string, wantCache bool, wantCount int) {
		t.Helper()
		req, _ := http.NewRequest(http.MethodGet, api.URL+query, nil)
		req.Header.Set("Authorization", "Bearer "+token)
		response, err := http.DefaultClient.Do(req)
		if err != nil {
			t.Fatal(err)
		}
		defer response.Body.Close()
		var result struct {
			Recommendations []Recommendation `json:"recommendations"`
			Model           string           `json:"model_used"`
			Cached          bool             `json:"from_cache"`
			HiddenCount     int              `json:"hidden_count"`
		}
		if err := json.NewDecoder(response.Body).Decode(&result); err != nil {
			t.Fatal(err)
		}
		if response.StatusCode != 200 || result.Model != wantModel || result.Cached != wantCache || len(result.Recommendations) != wantCount {
			t.Fatalf("recommendations did not remain available: HTTP %d %#v", response.StatusCode, result)
		}
		if wantCount > 0 && (result.Recommendations[0].Vacancy.ID != 7 || len(result.Recommendations[0].Insights.MatchedSkills) != 2) {
			t.Fatalf("missing explanation: %+v", result)
		}
		if wantCount == 0 && result.HiddenCount != 1 {
			t.Fatalf("hidden preference lost: %+v", result)
		}
	}
	get("/", basicRecommendationModel, false, 1)
	get("/", basicRecommendationModel, true, 1)
	unavailable.Store(false)
	get("/?refresh=true", "ml-service", false, 1)
	preference := func(user int, method, id string, want int) {
		t.Helper()
		req := httptest.NewRequest(method, "/", nil)
		req.SetPathValue("id", id)
		if user > 0 {
			value, err := GenerateToken(user, "preference@example.test", "candidate")
			if err != nil {
				t.Fatal(err)
			}
			req.Header.Set("Authorization", "Bearer "+value)
		}
		response := httptest.NewRecorder()
		s.authMiddleware(s.recommendationPreferenceHandler).ServeHTTP(response, req)
		if response.Code != want {
			t.Fatalf("preference user=%d method=%s id=%s: %d %s", user, method, id, response.Code, response.Body.String())
		}
	}
	preference(0, "PUT", "7", 401)
	preference(42, "PUT", "-1", 400)
	preference(42, "PUT", "999", 404)
	preference(42, "PUT", "7", 200)
	preference(42, "PUT", "7", 200)
	get("/", "none", false, 0)
	preference(43, "DELETE", "7", 200)
	get("/", "none", true, 0)
	preference(42, "DELETE", "7", 200)
	get("/", "ml-service", false, 1)
	preference(42, "PUT", "7", 200)
	preference(42, "DELETE", "", 200)
	get("/", "ml-service", false, 1)
	if _, err := pool.Exec(ctx, `INSERT INTO vacancies SELECT 8,title,company,company_id,location,experience,remote,skills,description,address,latitude,longitude,views,created_at-INTERVAL '1 day',is_archived,terms FROM vacancies WHERE id=7`); err != nil {
		t.Fatal(err)
	}
	page, err := s.storage.GetCandidateVacancies(ctx, []string{"Go", "PostgreSQL"}, "Go Developer", "Middle", 1, []int{7})
	if err != nil || len(page) != 1 || page[0].ID != 8 {
		t.Fatalf("hidden filtering must precede LIMIT: %+v %v", page, err)
	}
}
