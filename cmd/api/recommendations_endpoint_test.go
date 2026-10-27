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
	get := func(query string, wantModel string, wantCache bool) {
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
		}
		if err := json.NewDecoder(response.Body).Decode(&result); err != nil {
			t.Fatal(err)
		}
		if response.StatusCode != 200 || result.Model != wantModel || result.Cached != wantCache || len(result.Recommendations) != 1 || result.Recommendations[0].Vacancy.ID != 7 {
			t.Fatalf("recommendations did not remain available: HTTP %d %#v", response.StatusCode, result)
		}
	}
	get("/", basicRecommendationModel, false)
	get("/", basicRecommendationModel, true)
	unavailable.Store(false)
	get("/?refresh=true", "ml-service", false)
}
