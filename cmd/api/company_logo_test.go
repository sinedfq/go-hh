package main

import (
	"bytes"
	"context"
	"encoding/base64"
	"fmt"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"mime/multipart"
	"net/http/httptest"
	"net/textproto"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"
)

func TestCompanyLogoUpload(t *testing.T) {
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
	schema := pgx.Identifier{fmt.Sprintf("codex_logo_%d", time.Now().UnixNano())}.Sanitize()
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
	_, err = pool.Exec(ctx, `CREATE TABLE users(id INT PRIMARY KEY,email TEXT,password_hash TEXT,photo_url TEXT,role TEXT,company_id INT,created_at TIMESTAMPTZ DEFAULT NOW());
    CREATE TABLE companies(id INT PRIMARY KEY,name TEXT,description TEXT,industry TEXT,size TEXT,city TEXT,website TEXT,logo_url TEXT,photo_url TEXT,created_at TIMESTAMPTZ DEFAULT NOW());
    CREATE TABLE vacancies(id INT PRIMARY KEY,company_id INT,views INT);
    INSERT INTO companies(id,name,logo_url) VALUES(12,'Own',''),(999,'Other','unchanged');
    INSERT INTO users(id,email,password_hash,role,company_id) VALUES(7,'logo@test','hash','employer',12);`)
	if err != nil {
		t.Fatal(err)
	}
	storage := &PostgresStorage{pool: pool}
	if _, err := pool.Exec(ctx, `ALTER TABLE companies ADD COLUMN profile JSONB NOT NULL DEFAULT '{}';`); err != nil {
		t.Fatal(err)
	}
	t.Chdir(t.TempDir())
	companyID := 12
	for _, tc := range []struct {
		name, role               string
		company                  *int
		payload                  []byte
		authenticated, dbFailure bool
		status                   int
	}{
		{"anonymous", "employer", &companyID, nil, false, false, 401},
		{"candidate", "candidate", &companyID, nil, true, false, 403},
		{"without company", "employer", nil, nil, true, false, 404},
		{"spoofed image", "employer", &companyID, []byte("<html>not a photo</html>"), true, false, 400},
		{"database failure cleans file", "employer", &companyID, nil, true, true, 500},
		{"own company", "employer", &companyID, nil, true, false, 200},
	} {
		t.Run(tc.name, func(t *testing.T) {
			if _, err := pool.Exec(ctx, "UPDATE users SET role=$1,company_id=$2 WHERE id=7", tc.role, tc.company); err != nil {
				t.Fatal(err)
			}
			if tc.dbFailure {
				if _, err := pool.Exec(ctx, "ALTER TABLE companies ADD CONSTRAINT reject_logo CHECK (logo_url='unchanged' OR logo_url='')"); err != nil {
					t.Fatal(err)
				}
				defer pool.Exec(ctx, "ALTER TABLE companies DROP CONSTRAINT reject_logo")
			}
			var body bytes.Buffer
			writer := multipart.NewWriter(&body)
			header := textproto.MIMEHeader{}
			header.Set("Content-Disposition", `form-data; name="photo"; filename="logo.png"`)
			header.Set("Content-Type", "image/png")
			part, _ := writer.CreatePart(header)
			if tc.payload != nil {
				_, _ = part.Write(tc.payload)
			} else {
				fixture, _ := base64.StdEncoding.DecodeString("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a3ioAAAAASUVORK5CYII=")
				_, _ = part.Write(fixture)
			}
			_ = writer.WriteField("company_id", "999") // Cannot choose another company through multipart.
			_ = writer.Close()
			request := httptest.NewRequest("POST", "/api/my-company/logo", &body)
			request.Header.Set("Content-Type", writer.FormDataContentType())
			if tc.authenticated {
				request = request.WithContext(context.WithValue(request.Context(), userContextKey, &Claims{UserID: 7, Role: tc.role}))
			}
			server := &Server{storage: storage}
			response := httptest.NewRecorder()
			server.uploadCompanyLogoHandler(response, request)
			if response.Code != tc.status {
				t.Fatalf("status %d: %s", response.Code, response.Body.String())
			}
			if tc.status == 200 {
				company, err := storage.GetCompanyByID(ctx, companyID)
				if err != nil {
					t.Fatal(err)
				}
				if !strings.HasPrefix(company.LogoURL, "/uploads/company_12_") {
					t.Fatal(company.LogoURL)
				}
				if _, err := os.Stat(filepath.Join("uploads", filepath.Base(company.LogoURL))); err != nil {
					t.Fatal(err)
				}
				if err := storage.UpdateCompanyLogo(ctx, 999, 7, "forbidden"); err == nil {
					t.Fatal("cross-company update allowed")
				}
				other, err := storage.GetCompanyByID(ctx, 999)
				if err != nil || other.LogoURL != "unchanged" {
					t.Fatalf("other company changed: %+v %v", other, err)
				}
			} else {
				files, _ := filepath.Glob("uploads/*")
				if len(files) != 0 {
					t.Fatalf("unexpected leftover upload: %v", files)
				}
			}
		})
	}
}
