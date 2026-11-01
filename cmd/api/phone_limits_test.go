package main

import (
	"context"
	"encoding/json"
	"errors"
	"github.com/sinedfq/go-hh/internal/phoneotp"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func TestOTPResetIsScopedAndRequiresAdmin(t *testing.T) {
	pool := phoneOTPTestPool(t)
	ctx := context.Background()
	service, _ := phoneotp.NewAppNotificationService(phoneVerificationStore{pool: pool}, strings.Repeat("s", 32))
	first, err := service.Request(ctx, "+79990000001", "ip1")
	if err != nil {
		t.Fatal(err)
	}
	other, err := service.Request(ctx, "+79990000002", "ip2")
	if err != nil {
		t.Fatal(err)
	}
	s := NewServer(&PostgresStorage{pool: pool}, nil)
	s.phoneVerification = service
	mux := http.NewServeMux()
	s.phoneVerificationRoutes(mux)
	call := func(role string) int {
		r := httptest.NewRequest("POST", "/api/admin/phone-verification/reset", strings.NewReader(`{"phone":"+79990000001"}`))
		if role != "" {
			token, _ := GenerateToken(1, "reset@example.test", role)
			r.Header.Set("Authorization", "Bearer "+token)
		}
		w := httptest.NewRecorder()
		mux.ServeHTTP(w, r)
		return w.Code
	}
	if call("") != 401 || call("candidate") != 403 {
		t.Fatal("reset was not protected")
	}
	if call("admin") != 200 {
		t.Fatal("admin reset failed")
	}
	if _, err = service.Verify(ctx, first.ChallengeID, first.DevelopmentCode); err == nil {
		t.Fatal("old code survived reset")
	}
	if _, err = service.Request(ctx, "+79990000001", "ip1"); err != nil {
		t.Fatal("phone cooldown was not reset")
	}
	if _, err = service.Verify(ctx, other.ChallengeID, other.DevelopmentCode); err != nil {
		t.Fatal("another phone was affected")
	}
	count, err := phoneotp.ResetLimits(ctx, pool, "", true)
	if err != nil || count != 2 {
		t.Fatal("all reset failed", count, err)
	}
	w := httptest.NewRecorder()
	t.Setenv("APP_ENV", "production")
	s.resetPhoneLimits(w, httptest.NewRequest("POST", "/", strings.NewReader(`{"all":true}`)))
	var body map[string]any
	json.Unmarshal(w.Body.Bytes(), &body)
	if w.Code != 403 {
		t.Fatal("production allowed all reset")
	}
}

func TestOTPResponseUsesActualCooldown(t *testing.T) {
	s := &Server{}
	w := httptest.NewRecorder()
	s.phoneVerificationFailure(w, &phoneotp.RateLimitError{RetryAfter: 3500})
	if w.Code != 429 || w.Header().Get("Retry-After") != "3500" {
		t.Fatal("real cooldown was not returned")
	}
}

func TestOTPPostgresReportsTheActualHourlyWindow(t *testing.T) {
	pool := phoneOTPTestPool(t)
	ctx := context.Background()
	service, _ := phoneotp.NewAppNotificationService(phoneVerificationStore{pool: pool}, strings.Repeat("s", 32))
	for i := 0; i < 5; i++ {
		if _, err := service.Request(ctx, "+79990000001", "ip"); err != nil {
			t.Fatal(err)
		}
		if _, err := pool.Exec(ctx, `UPDATE phone_verification_challenges SET created_at=created_at-INTERVAL '61 seconds'`); err != nil {
			t.Fatal(err)
		}
	}
	if _, err := pool.Exec(ctx, `UPDATE phone_verification_challenges SET created_at=NOW()-INTERVAL '2 minutes'`); err != nil {
		t.Fatal(err)
	}
	_, err := service.Request(ctx, "+79990000001", "ip")
	var limit *phoneotp.RateLimitError
	if !errors.As(err, &limit) || limit.RetryAfter < 3400 || limit.RetryAfter > 3500 {
		t.Fatal("hourly limit must not claim a 60 second retry", err)
	}
	if _, err = phoneotp.ResetLimits(ctx, pool, "", true); err != nil {
		t.Fatal(err)
	}
	if _, err = service.Request(ctx, "+79990000001", "ip"); err != nil {
		t.Fatal("reset did not reopen sending", err)
	}
}
