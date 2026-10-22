package main

import (
	"context"
	"fmt"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"os"
	"testing"
	"time"
)

func TestExpandedVacancySearch(t *testing.T) {
	url := os.Getenv("TRUDOMIX_TEST_DATABASE_URL")
	if url == "" {
		t.Skip("set TRUDOMIX_TEST_DATABASE_URL")
	}
	ctx := context.Background()
	admin, err := pgxpool.New(ctx, url)
	if err != nil {
		t.Fatal(err)
	}
	defer admin.Close()
	schema := pgx.Identifier{fmt.Sprintf("codex_search_%d", time.Now().UnixNano())}.Sanitize()
	if _, err = admin.Exec(ctx, "CREATE SCHEMA "+schema); err != nil {
		t.Fatal(err)
	}
	defer func() {
		if _, err := admin.Exec(ctx, "DROP SCHEMA "+schema+" CASCADE"); err != nil {
			t.Error(err)
		}
	}()
	config, err := pgxpool.ParseConfig(url)
	if err != nil {
		t.Fatal(err)
	}
	config.ConnConfig.RuntimeParams["search_path"] = schema
	pool, err := pgxpool.NewWithConfig(ctx, config)
	if err != nil {
		t.Fatal(err)
	}
	defer pool.Close()
	if _, err := pool.Exec(ctx, `CREATE TABLE companies(id INT PRIMARY KEY,logo_url TEXT,profile JSONB NOT NULL DEFAULT '{}'); INSERT INTO companies(id,logo_url) VALUES(1,'/uploads/team.png'),(2,NULL);
 CREATE TABLE favorites(user_id INT,vacancy_id INT,created_at TIMESTAMPTZ DEFAULT NOW()); INSERT INTO favorites(user_id,vacancy_id) VALUES(42,1);`); err != nil {
		t.Fatal(err)
	}
	_, err = pool.Exec(ctx, `CREATE TABLE vacancies(id INT PRIMARY KEY,title TEXT,company TEXT,company_id INT,location TEXT,experience TEXT,remote BOOLEAN,skills TEXT[],description TEXT,address TEXT,latitude DOUBLE PRECISION,longitude DOUBLE PRECISION,views INT,author_user_id INT,is_archived BOOLEAN,created_at TIMESTAMPTZ,updated_at TIMESTAMPTZ);
 INSERT INTO vacancies VALUES
 (1,'Android','Team',1,'Новосибирск','Junior',true,ARRAY['Kotlin'],'Mobile','Street',55.03,82.92,10,1,false,NOW()-INTERVAL '1 day',NOW()),
 (2,'Backend','Team',1,'Новосибирск','1-3 года',false,ARRAY['Go'],'API','Street',55.04,82.93,50,1,false,NOW(),NOW()),
 (3,'Designer','Other',2,'Москва','Senior',true,ARRAY['Figma'],'Design','',NULL,NULL,100,1,false,NOW(),NOW()),
 (4,'Archived','Team',1,'Новосибирск','Junior',true,ARRAY['Kotlin'],'Old','Street',55.03,82.92,1000,1,true,NOW(),NOW()),
 (5,'Invalid coordinates','Team',1,'Москва','Lead',false,ARRAY['Go'],'Invalid','',0,0,0,1,false,NOW(),NOW());`)
	if err != nil {
		t.Fatal(err)
	}
	storage := &PostgresStorage{pool: pool}
	if _, err := pool.Exec(ctx, `ALTER TABLE vacancies ADD COLUMN terms JSONB NOT NULL DEFAULT '{}';`); err != nil {
		t.Fatal(err)
	}
	t.Run("logos in every vacancy response", func(t *testing.T) {
		check := func(items []Vacancy, err error) {
			t.Helper()
			if err != nil {
				t.Fatal(err)
			}
			if len(items) == 0 {
				t.Fatal("no vacancies")
			}
			for _, v := range items {
				expected := ""
				if v.CompanyID == 1 {
					expected = "/uploads/team.png"
				}
				if v.CompanyLogo != expected {
					t.Fatalf("vacancy %d logo=%q want %q", v.ID, v.CompanyLogo, expected)
				}
			}
		}
		check(storage.GetAllVacancies(ctx))
		v, err := storage.GetVacancyByID(ctx, 1)
		check([]Vacancy{v}, err)
		check(storage.GetFavorites(ctx, 42))
		check(storage.GetVacanciesByCompanyID(ctx, 1))
		check(storage.GetMyVacancies(ctx, 1))
		check(storage.GetCandidateVacancies(ctx, []string{"Kotlin"}, "", "", 10))
		if _, err := pool.Exec(ctx, "UPDATE companies SET logo_url='/uploads/new.png' WHERE id=1"); err != nil {
			t.Fatal(err)
		}
		v, err = storage.GetVacancyByID(ctx, 1)
		if err != nil || v.CompanyLogo != "/uploads/new.png" {
			t.Fatalf("replacement not reflected: %+v %v", v, err)
		}
		if _, err := pool.Exec(ctx, "UPDATE companies SET logo_url='/uploads/team.png' WHERE id=1"); err != nil {
			t.Fatal(err)
		}
	})
	remote := true
	for _, tc := range []struct {
		name    string
		filters SearchFilters
		ids     []int
		total   int
	}{
		{"grade or years", SearchFilters{Experience: "junior|1-3 года", Sort: "title"}, []int{1, 2}, 2},
		{"combined filters", SearchFilters{Location: "новосибирск", Company: "team", Remote: &remote, Skills: []string{"kotlin"}, HasCoordinates: true}, []int{1}, 1},
		{"popular and pagination", SearchFilters{HasCoordinates: true, Sort: "popular", Limit: 1, Offset: 1}, []int{1}, 2},
		{"unknown company", SearchFilters{Company: "'; DROP TABLE vacancies; --"}, []int{}, 0},
	} {
		t.Run(tc.name, func(t *testing.T) {
			items, total, err := storage.SearchVacancies(ctx, tc.filters)
			if err != nil {
				t.Fatal(err)
			}
			if total != tc.total || len(items) != len(tc.ids) {
				t.Fatalf("total=%d items=%v", total, items)
			}
			for i, item := range items {
				if item.CompanyID == 1 && item.CompanyLogo != "/uploads/team.png" {
					t.Fatalf("search omitted company logo: %+v", item)
				}
				if item.ID != tc.ids[i] {
					t.Fatalf("id=%d want %d", item.ID, tc.ids[i])
				}
			}
		})
	}
}
