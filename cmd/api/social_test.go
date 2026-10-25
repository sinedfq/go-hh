package main

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"mime/multipart"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

func TestSocialSubscriptionsFeedAndExpiry(t *testing.T) {
	database := os.Getenv("TRUDOMIX_TEST_DATABASE_URL")
	if database == "" {
		t.Skip("set TRUDOMIX_TEST_DATABASE_URL")
	}
	ctx := context.Background()
	admin, e := pgxpool.New(ctx, database)
	if e != nil {
		t.Fatal(e)
	}
	defer admin.Close()
	schema := pgx.Identifier{fmt.Sprintf("codex_social_%d", time.Now().UnixNano())}.Sanitize()
	if _, e = admin.Exec(ctx, "CREATE SCHEMA "+schema); e != nil {
		t.Fatal(e)
	}
	defer admin.Exec(ctx, "DROP SCHEMA "+schema+" CASCADE")
	config, e := pgxpool.ParseConfig(database)
	if e != nil {
		t.Fatal(e)
	}
	config.ConnConfig.RuntimeParams["search_path"] = schema
	pool, e := pgxpool.NewWithConfig(ctx, config)
	if e != nil {
		t.Fatal(e)
	}
	defer pool.Close()
	_, e = pool.Exec(ctx, `CREATE TABLE users(id INT PRIMARY KEY,email TEXT,password_hash TEXT,photo_url TEXT,role TEXT,company_id INT,created_at TIMESTAMPTZ DEFAULT NOW());CREATE TABLE companies(id INT PRIMARY KEY,name TEXT,logo_url TEXT);CREATE TABLE resumes(user_id INT,full_name TEXT);INSERT INTO companies VALUES(10,'Team',''),(20,'Other','');INSERT INTO users(id,email,password_hash,role,company_id) VALUES(1,'candidate@test','hash','candidate',NULL),(2,'owner@test','hash','employer',10),(3,'other@test','hash','employer',20),(4,'reader@test','hash','candidate',NULL);`)
	if e != nil {
		t.Fatal(e)
	}
	migration, e := os.ReadFile("../../migrations/027_social_feed.sql")
	if e != nil {
		t.Fatal(e)
	}
	video, e := os.ReadFile("../../android/app/src/androidTest/assets/company-story.mp4")
	if e != nil {
		t.Fatal(e)
	}
	if _, e = pool.Exec(ctx, strings.Split(string(migration), "-- +goose Down")[0]); e != nil {
		t.Fatal(e)
	}
	t.Chdir(t.TempDir())
	server := NewServer(&PostgresStorage{pool: pool}, nil)
	mux := http.NewServeMux()
	server.registerSocialRoutes(mux)
	mux.Handle("/uploads/", protectStoryUploads(http.StripPrefix("/uploads/", http.FileServer(http.Dir("uploads")))))
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
			token, err := GenerateToken(user, "test@test", role)
			if err != nil {
				t.Fatal(err)
			}
			r.Header.Set("Authorization", "Bearer "+token)
		}
		w := httptest.NewRecorder()
		mux.ServeHTTP(w, r)
		return w
	}
	expect := func(w *httptest.ResponseRecorder, status int) {
		t.Helper()
		if w.Code != status {
			t.Fatalf("want %d, got %d: %s", status, w.Code, w.Body.String())
		}
	}
	idOf := func(w *httptest.ResponseRecorder) int64 {
		t.Helper()
		var v struct{ ID int64 }
		if e := json.Unmarshal(w.Body.Bytes(), &v); e != nil {
			t.Fatal(e)
		}
		return v.ID
	}
	feed := func(user int, scope string) []SocialPost {
		t.Helper()
		w := request(user, "GET", "/api/feed?scope="+scope, nil, "")
		expect(w, 200)
		var data struct{ Posts []SocialPost }
		if e := json.Unmarshal(w.Body.Bytes(), &data); e != nil {
			t.Fatal(e)
		}
		return data.Posts
	}
	expect(request(0, "GET", "/api/feed", nil, ""), 401)
	expect(request(1, "POST", "/api/posts", []byte(`{"body":"forged","as_company":true}`), ""), 403)
	expect(request(1, "POST", "/api/posts", []byte(`{"body":"  "}`), ""), 400)
	companyPost := request(2, "POST", "/api/posts", []byte(`{"body":"Company news","as_company":true}`), "")
	expect(companyPost, 201)
	postID := idOf(companyPost)
	path := fmt.Sprintf("/api/posts/%d", postID)
	expect(request(1, "POST", "/api/posts", []byte(`{"body":"Personal news"}`), ""), 201)
	if len(feed(1, "following")) != 1 || len(feed(1, "all")) != 2 {
		t.Fatal("feed filtering before subscription")
	}
	for range 2 {
		expect(request(1, "PUT", "/api/companies/10/subscription", nil, ""), 200)
	}
	if len(feed(1, "following")) != 2 || len(feed(4, "following")) != 0 {
		t.Fatal("subscriptions leak between users")
	}
	for range 2 {
		expect(request(1, "PUT", path+"/like", nil, ""), 200)
	}
	posts := feed(1, "all")
	for _, post := range posts {
		if post.ID == postID && (!post.Liked || post.Likes != 1 || post.CanDelete) {
			t.Fatal("like uniqueness or ownership")
		}
	}
	expect(request(1, "DELETE", path, nil, ""), 404)
	expect(request(3, "DELETE", path, nil, ""), 404)
	comment := request(1, "POST", path+"/comments", []byte(`{"body":"Great team"}`), "")
	expect(comment, 201)
	commentPath := fmt.Sprintf("/api/comments/%d", idOf(comment))
	expect(request(4, "DELETE", commentPath, nil, ""), 404)
	expect(request(1, "DELETE", commentPath, nil, ""), 204)
	expect(request(1, "POST", path+"/comments", []byte(`{"body":" "}`), ""), 400)
	expect(request(1, "POST", path+"/comments", []byte(`{"body":"Still here"}`), ""), 201)
	expect(request(1, "GET", path+"/comments", nil, ""), 200)
	upload := func(user int, endpoint string) *httptest.ResponseRecorder {
		var body bytes.Buffer
		form := multipart.NewWriter(&body)
		_ = form.WriteField("body", "Our day")
		part, err := form.CreateFormFile("video", "story.mp4")
		if err != nil {
			t.Fatal(err)
		}
		_, _ = part.Write(video)
		_ = form.Close()
		return request(user, "POST", endpoint, body.Bytes(), form.FormDataContentType())
	}
	expect(upload(1, "/api/stories"), 403)
	story := upload(2, "/api/stories")
	expect(story, 201)
	storyID := idOf(story)
	mediaPath := fmt.Sprintf("/api/stories/%d/media", storyID)
	stories := func(user int) []TimedStory {
		t.Helper()
		w := request(user, "GET", "/api/stories", nil, "")
		expect(w, 200)
		var list []TimedStory
		if e := json.Unmarshal(w.Body.Bytes(), &list); e != nil {
			t.Fatal(e)
		}
		return list
	}
	active := stories(1)
	if len(active) != 1 || active[0].ExpiresAt.Sub(active[0].CreatedAt) != 24*time.Hour {
		t.Fatal("story lifetime")
	}
	if len(stories(4)) != 0 {
		t.Fatal("unfollowed stories in home feed")
	}
	expect(request(0, "GET", mediaPath, nil, ""), 200)
	var stored string
	if e = pool.QueryRow(ctx, `SELECT media_url FROM company_stories WHERE id=$1`, storyID).Scan(&stored); e != nil {
		t.Fatal(e)
	}
	expect(request(0, "GET", "/uploads/.stories/"+filepath.Base(stored), nil, ""), 404)
	expect(request(0, "GET", "/uploads/%2Estories%2F"+filepath.Base(stored), nil, ""), 404)
	expect(request(3, "DELETE", fmt.Sprintf("/api/stories/%d", storyID), nil, ""), 404)
	expect(request(1, "DELETE", "/api/companies/10/subscription", nil, ""), 200)
	if len(stories(1)) != 0 || len(feed(1, "following")) != 1 {
		t.Fatal("unfollow not reflected")
	}
	if _, e = pool.Exec(ctx, `UPDATE company_stories SET created_at=NOW()-INTERVAL '25 hours',expires_at=NOW()-INTERVAL '1 hour' WHERE id=$1`, storyID); e != nil {
		t.Fatal(e)
	}
	if len(stories(2)) != 0 {
		t.Fatal("expired story listed")
	}
	expect(request(0, "GET", mediaPath, nil, ""), 404)
	server.cleanupStories(ctx)
	if _, e = os.Stat(filepath.Join("uploads", ".stories", filepath.Base(stored))); !os.IsNotExist(e) {
		t.Fatal("expired media not cleaned")
	}
	expect(request(2, "DELETE", path, nil, ""), 204)
	var children int
	if e = pool.QueryRow(ctx, `SELECT (SELECT COUNT(*) FROM social_likes)+(SELECT COUNT(*) FROM social_comments)`).Scan(&children); e != nil || children != 0 {
		t.Fatal("post children not removed", e)
	}
	for range 22 {
		expect(request(1, "POST", "/api/posts", []byte(`{"body":"Page"}`), ""), 201)
	}
	first := feed(1, "all")
	if len(first) != 20 {
		t.Fatal("page size")
	}
	w := request(1, "GET", fmt.Sprintf("/api/feed?before=%d", first[19].ID), nil, "")
	expect(w, 200)
	var page struct{ Posts []SocialPost }
	_ = json.Unmarshal(w.Body.Bytes(), &page)
	if len(page.Posts) != 3 || page.Posts[0].ID >= first[19].ID {
		t.Fatal("keyset pagination")
	}
}
