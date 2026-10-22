package main

import (
	"context"
	"fmt"
	"net/http"
	"net/http/httptest"
	"os"
	"strings"
	"testing"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

// A private, disposable schema keeps the developer's existing users and chats intact.
func TestChatAuthorizationWithPostgres(t *testing.T) {
	url := os.Getenv("TRUDOMIX_TEST_DATABASE_URL")
	if url == "" {
		t.Skip("set TRUDOMIX_TEST_DATABASE_URL to run PostgreSQL integration tests")
	}
	ctx := context.Background()
	admin, err := pgxpool.New(ctx, url)
	if err != nil {
		t.Fatal(err)
	}
	defer admin.Close()
	schema := fmt.Sprintf("codex_chat_%d", time.Now().UnixNano())
	quoted := pgx.Identifier{schema}.Sanitize()
	if _, err = admin.Exec(ctx, "CREATE SCHEMA "+quoted); err != nil {
		t.Fatal(err)
	}
	defer func() {
		if _, err := admin.Exec(ctx, "DROP SCHEMA "+quoted+" CASCADE"); err != nil {
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
	_, err = pool.Exec(ctx, `
 CREATE TABLE users(id INT PRIMARY KEY,email TEXT,role TEXT,company_id INT,photo_url TEXT);
 CREATE TABLE vacancies(id INT PRIMARY KEY,company_id INT);
 CREATE TABLE applications(id INT PRIMARY KEY,vacancy_id INT,candidate_user_id INT);
 CREATE TABLE conversations(id INT PRIMARY KEY,application_id INT);
 CREATE TABLE device_tokens(token TEXT PRIMARY KEY,user_id INT);
 CREATE TABLE messages(id SERIAL PRIMARY KEY,conversation_id INT,sender_id INT,content TEXT,is_read BOOLEAN DEFAULT FALSE,read_at TIMESTAMPTZ,created_at TIMESTAMPTZ DEFAULT NOW());
 INSERT INTO users VALUES(1,'candidate@example.test','candidate',NULL,''),(2,'employer@example.test','employer',10,''),(3,'outsider@example.test','employer',20,''),(4,'colleague@example.test','employer',10,''),(5,'unrelated@example.test','candidate',10,'');
 INSERT INTO vacancies VALUES(7,10);INSERT INTO applications VALUES(8,7,1);INSERT INTO conversations VALUES(9,8);
 INSERT INTO messages(conversation_id,sender_id,content) VALUES(9,2,'Hello');`)
	if err != nil {
		t.Fatal(err)
	}
	s := NewServer(&PostgresStorage{pool: pool}, nil)
	mux := http.NewServeMux()
	mux.HandleFunc("GET /api/conversations/{id}/messages", s.authMiddleware(s.conversationMiddleware(s.getChatMessagesHandler)))
	mux.HandleFunc("POST /api/conversations/{id}/messages", s.authMiddleware(s.conversationMiddleware(s.sendMessageHandler)))
	mux.HandleFunc("GET /api/conversations/{id}/typing", s.authMiddleware(s.conversationMiddleware(s.getTypingUsersHandler)))
	mux.HandleFunc("POST /api/conversations/{id}/typing", s.authMiddleware(s.conversationMiddleware(s.markTypingHandler)))
	mux.HandleFunc("POST /api/devices/status", s.authMiddleware(s.devicePushStatusHandler))
	request := func(user int, method, path, body string) *httptest.ResponseRecorder {
		t.Helper()
		r := httptest.NewRequest(method, path, strings.NewReader(body))
		token, err := GenerateToken(user, "test@example.test", "candidate")
		if err != nil {
			t.Fatal(err)
		}
		r.Header.Set("Authorization", "Bearer "+token)
		w := httptest.NewRecorder()
		mux.ServeHTTP(w, r)
		return w
	}
	for _, user := range []int{3, 5} {
		for _, method := range []string{"GET", "POST"} {
			for _, kind := range []string{"messages", "typing"} {
				w := request(user, method, "/api/conversations/9/"+kind, `{"content":"forbidden"}`)
				if w.Code != 403 {
					t.Fatalf("outsider %d %s %s: %d %s", user, method, kind, w.Code, w.Body.String())
				}
			}
		}
	}
	var read bool
	if err := pool.QueryRow(ctx, `SELECT is_read FROM messages WHERE id=1`).Scan(&read); err != nil {
		t.Fatal(err)
	}
	if read {
		t.Fatal("outsider marked message read")
	}
	for _, user := range []int{1, 2, 4} {
		w := request(user, "GET", "/api/conversations/9/messages", "")
		if w.Code != 200 {
			t.Fatalf("participant %d history: %d", user, w.Code)
		}
	}
	w := request(1, "POST", "/api/conversations/9/messages", `{"content":"Reply"}`)
	if w.Code != 201 {
		t.Fatalf("send: %d %s", w.Code, w.Body.String())
	}
	w = request(1, "POST", "/api/conversations/9/typing", "")
	if w.Code != 200 {
		t.Fatal(w.Code)
	}
	w = request(2, "GET", "/api/conversations/9/typing", "")
	if !strings.Contains(w.Body.String(), `"typing_users":[1]`) {
		t.Fatal(w.Body.String())
	}
	w = request(1, "GET", "/api/conversations/999/messages", "")
	if w.Code != 403 {
		t.Fatal(w.Code)
	}
	if _, err := pool.Exec(ctx, `INSERT INTO device_tokens(token,user_id) VALUES('integration-device',1)`); err != nil {
		t.Fatal(err)
	}
	for _, user := range []int{1, 3} {
		w := request(user, "POST", "/api/devices/status", `{"token":"integration-device"}`)
		want := `"registered":false`
		if user == 1 {
			want = `"registered":true`
		}
		if w.Code != 200 || !strings.Contains(w.Body.String(), want) || !strings.Contains(w.Body.String(), `"server_enabled":false`) {
			t.Fatalf("device readiness for user %d: %d %s", user, w.Code, w.Body.String())
		}
	}
}
