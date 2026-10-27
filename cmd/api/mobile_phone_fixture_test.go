package main

import (
	"context"
	"github.com/sinedfq/go-hh/internal/phoneotp"
	"net"
	"net/http"
	"os"
	"strings"
	"sync"
	"testing"
	"time"
)

// Opt-in live fixture for Android UI tests. Its temporary schema is removed on shutdown.
func TestMobilePhoneFixture(t *testing.T) {
	addr := os.Getenv("TRUDOMIX_MOBILE_PHONE_TEST_ADDR")
	if addr == "" {
		t.Skip("set address to run the Android phone fixture")
	}
	pool := phoneOTPTestPool(t)
	t.Setenv("JWT_SECRET", strings.Repeat("fixture-jwt-secret-", 3))
	service, err := phoneotp.NewAppNotificationService(phoneVerificationStore{pool: pool}, strings.Repeat("mobile-fixture-secret-", 3))
	if err != nil {
		t.Fatal(err)
	}
	s := NewServer(&PostgresStorage{pool: pool}, nil)
	s.phoneVerification = service
	s.phoneVerificationTest = true
	mux := http.NewServeMux()
	s.phoneVerificationRoutes(mux)
	mux.HandleFunc("GET /health", s.healthHandler)
	mux.HandleFunc("GET /api/me", s.authMiddleware(s.meHandler))
	done := make(chan struct{})
	var once sync.Once
	mux.HandleFunc("POST /test/stop", func(w http.ResponseWriter, r *http.Request) { w.WriteHeader(204); once.Do(func() { close(done) }) })
	mux.HandleFunc("POST /test/resend-ready", func(w http.ResponseWriter, r *http.Request) {
		if _, err := pool.Exec(r.Context(), `UPDATE phone_verification_challenges SET created_at=created_at-INTERVAL '61 seconds'`); err != nil {
			w.WriteHeader(500)
			return
		}
		w.WriteHeader(204)
	})
	listener, err := net.Listen("tcp", addr)
	if err != nil {
		t.Fatal(err)
	}
	server := &http.Server{Handler: mux, ReadHeaderTimeout: 5 * time.Second}
	go server.Serve(listener)
	t.Cleanup(func() {
		ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
		defer cancel()
		server.Shutdown(ctx)
	})
	t.Log("MOBILE_PHONE_FIXTURE_READY")
	select {
	case <-done:
	case <-time.After(8 * time.Minute):
		t.Fatal("Android fixture timed out")
	}
}
