package main

import (
	"context"
	"fmt"
	"net/http/httptest"
	"os"
	"strings"
	"testing"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

type capturedNotification struct {
	token, title, body string
	data               map[string]string
}
type capturingSender struct{ deliveries chan capturedNotification }

func (s *capturingSender) Send(_ context.Context, token, title, body string, data map[string]string) error {
	s.deliveries <- capturedNotification{token, title, body, data}
	return nil
}

func TestSavedNotificationIsDeliveredToRegisteredDevice(t *testing.T) {
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
	schema := pgx.Identifier{fmt.Sprintf("notification_delivery_%d", time.Now().UnixNano())}.Sanitize()
	if _, err := admin.Exec(ctx, "CREATE SCHEMA "+schema); err != nil {
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
	_, err = pool.Exec(ctx, `CREATE TABLE notifications(user_id INT CHECK(user_id>0), type TEXT, title TEXT, message TEXT, data JSONB);
CREATE TABLE device_tokens(token TEXT PRIMARY KEY,user_id INT);
INSERT INTO device_tokens VALUES('owner-device',7),('other-user-device',8);`)
	if err != nil {
		t.Fatal(err)
	}
	sender := &capturingSender{deliveries: make(chan capturedNotification, 4)}
	server := NewServer(&PostgresStorage{pool: pool}, nil)
	server.pushSender = sender
	request := CreateNotificationRequest{UserID: 7, Type: "resume_viewed", Title: "Ваше резюме просмотрели", Message: "Команда посмотрела резюме", Data: map[string]interface{}{"resume_id": 12, "viewer_user_id": 8}}
	if err := server.createNotification(ctx, request); err != nil {
		t.Fatal(err)
	}
	select {
	case event := <-sender.deliveries:
		if event.token != "owner-device" || event.title != request.Title || event.body != request.Message || event.data["type"] != "resume_viewed" || event.data["resume_id"] != "12" {
			t.Fatalf("unexpected event: %+v", event)
		}
	case <-time.After(5 * time.Second):
		t.Fatal("saved event was not delivered")
	}
	var count int
	if err := pool.QueryRow(ctx, "SELECT COUNT(*) FROM notifications WHERE user_id=7").Scan(&count); err != nil || count != 1 {
		t.Fatalf("event not persisted: count=%d err=%v", count, err)
	}
	request.UserID = 0
	if err := server.createNotification(ctx, request); err == nil {
		t.Fatal("expected persistence error")
	}
	select {
	case event := <-sender.deliveries:
		t.Fatalf("delivered an unsaved event or to another user: %+v", event)
	case <-time.After(150 * time.Millisecond):
	}
	for _, tc := range []struct {
		token  string
		status int
	}{{"other-user-device", 403}, {"owner-device", 204}} {
		r := httptest.NewRequest("POST", "/api/devices/test", strings.NewReader(`{"token":"`+tc.token+`"}`))
		r = r.WithContext(context.WithValue(r.Context(), userContextKey, &Claims{UserID: 7}))
		w := httptest.NewRecorder()
		server.testDevicePushHandler(w, r)
		if w.Code != tc.status {
			t.Fatalf("device diagnostic returned %d, want %d", w.Code, tc.status)
		}
	}
	select {
	case event := <-sender.deliveries:
		if event.token != "owner-device" || event.data["type"] != "push_test" {
			t.Fatalf("unexpected diagnostic destination: %+v", event)
		}
	default:
		t.Fatal("diagnostic event was not delivered")
	}
	select {
	case <-sender.deliveries:
		t.Fatal("diagnostic sent to another user's device")
	default:
	}
}

func TestApplicationNotificationsContainReadableStatusAndDestination(t *testing.T) {
	app := &Application{CandidateUserID: 7, VacancyID: 12, CompanyName: "Команда", VacancyTitle: "Go-разработчик"}
	for status, title := range map[string]string{"viewed": "Отклик просмотрен", "accepted": "Вас пригласили", "rejected": "Ответ на отклик"} {
		event := applicationNotification(app, 42, status)
		if event.Title != title || event.Type != "application_"+status || event.UserID != 7 || event.Data["application_id"] != 42 || !strings.Contains(event.Message, app.CompanyName) || !strings.Contains(event.Message, app.VacancyTitle) {
			t.Fatalf("status %s: %+v", status, event)
		}
	}
}
