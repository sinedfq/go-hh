package main

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/sinedfq/go-hh/internal/phoneotp"
	"net/http"
	"net/http/httptest"
	"os"
	"os/exec"
	"strings"
	"sync"
	"testing"
	"time"
)

func TestPhoneVerificationConfigurationFailsClosed(t *testing.T) {
	for i, test := range []struct {
		mode, env, secret, phones string
		fail                      bool
	}{{"off", "production", "", "", false}, {"development", "production", strings.Repeat("s", 32), "+79990000001", true}, {"development", "development", "short", "+79990000001", true}, {"development", "development", strings.Repeat("s", 32), "", true}, {"development", "development", strings.Repeat("s", 32), "+79990000001", false}} {
		t.Run(fmt.Sprintf("%s-%s-%d", test.mode, test.env, i), func(t *testing.T) {
			t.Setenv("OTP_MODE", test.mode)
			t.Setenv("APP_ENV", test.env)
			t.Setenv("OTP_SECRET", test.secret)
			t.Setenv("OTP_TEST_PHONES", test.phones)
			s := &Server{}
			err := s.configurePhoneVerification()
			if (err != nil) != test.fail {
				t.Fatal(err)
			}
		})
	}
	s := &Server{}
	w := httptest.NewRecorder()
	s.phoneVerificationRequest(w, httptest.NewRequest("POST", "/", strings.NewReader(`{"phone":"+79990000001"}`)))
	if w.Code != 503 || strings.Contains(w.Body.String(), "development_code") {
		t.Fatal(w.Code)
	}
}
func phoneOTPTestPool(t *testing.T) *pgxpool.Pool {
	t.Helper()
	dsn := os.Getenv("TRUDOMIX_TEST_DATABASE_URL")
	if dsn == "" {
		t.Skip("set TRUDOMIX_TEST_DATABASE_URL for isolated PostgreSQL tests")
	}
	ctx := context.Background()
	admin, err := pgxpool.New(ctx, dsn)
	if err != nil {
		t.Fatal(err)
	}
	schema := pgx.Identifier{fmt.Sprintf("codex_otp_%d", time.Now().UnixNano())}.Sanitize()
	if _, err = admin.Exec(ctx, "CREATE SCHEMA "+schema); err != nil {
		admin.Close()
		t.Fatal(err)
	}
	t.Cleanup(func() {
		if _, err := admin.Exec(ctx, "DROP SCHEMA "+schema+" CASCADE"); err != nil {
			t.Error(err)
		}
		admin.Close()
	})
	config, err := pgxpool.ParseConfig(dsn)
	if err != nil {
		t.Fatal(err)
	}
	config.ConnConfig.RuntimeParams["search_path"] = schema
	pool, err := pgxpool.NewWithConfig(ctx, config)
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(pool.Close)
	migration, err := os.ReadFile("../../migrations/028_phone_verification.sql")
	if err != nil {
		t.Fatal(err)
	}
	if _, err = pool.Exec(ctx, strings.Split(string(migration), "-- +goose Down")[0]); err != nil {
		t.Fatal(err)
	}
	migration, err = os.ReadFile("../../migrations/029_mobile_phone_accounts.sql")
	if err != nil {
		t.Fatal(err)
	}
	if _, err = pool.Exec(ctx, "CREATE TABLE users(id SERIAL PRIMARY KEY,email TEXT UNIQUE NOT NULL,password_hash TEXT NOT NULL,role TEXT NOT NULL,photo_url TEXT DEFAULT '',company_id INTEGER,created_at TIMESTAMPTZ DEFAULT NOW())"); err != nil {
		t.Fatal(err)
	}
	if _, err = pool.Exec(ctx, strings.Split(string(migration), "-- +goose Down")[0]); err != nil {
		t.Fatal(err)
	}
	return pool
}
func TestPhoneVerificationPostgresConcurrentAttemptsAndReceipts(t *testing.T) {
	pool := phoneOTPTestPool(t)
	ctx := context.Background()
	store := phoneVerificationStore{pool: pool}
	service, err := phoneotp.New(store, phoneotp.NewTestSender([]string{"+79990000001"}), strings.Repeat("s", 32), []string{"+79990000001"})
	if err != nil {
		t.Fatal(err)
	}
	request, err := service.Request(ctx, "+79990000001", "ip")
	if err != nil {
		t.Fatal(err)
	}
	if _, err = service.Request(ctx, "+79990000001", "another-ip"); !errors.Is(err, phoneotp.ErrRateLimit) {
		t.Fatal("phone cooldown bypass", err)
	}
	results := make(chan error, 12)
	var group sync.WaitGroup
	wrong := "000000"
	if request.DevelopmentCode == wrong {
		wrong = "111111"
	}
	for i := 0; i < 12; i++ {
		group.Add(1)
		go func() { defer group.Done(); _, err := service.Verify(ctx, request.ChallengeID, wrong); results <- err }()
	}
	group.Wait()
	close(results)
	for err := range results {
		if !errors.Is(err, phoneotp.ErrInvalid) {
			t.Fatal(err)
		}
	}
	var attempts int
	if err = pool.QueryRow(ctx, `SELECT attempts FROM phone_verification_challenges WHERE id=$1`, request.ChallengeID).Scan(&attempts); err != nil || attempts != 5 {
		t.Fatal("attempt limit", attempts, err)
	}
	if _, err = service.Verify(ctx, request.ChallengeID, request.DevelopmentCode); !errors.Is(err, phoneotp.ErrInvalid) {
		t.Fatal("locked code accepted")
	}
	// Use a fresh row after the first challenge has exhausted its attempts.
	if _, err = pool.Exec(ctx, `UPDATE phone_verification_challenges SET created_at=created_at-INTERVAL '61 seconds'`); err != nil {
		t.Fatal(err)
	}
	request, err = service.Request(ctx, "+79990000001", "ip")
	if err != nil {
		t.Fatal(err)
	}
	verified, err := service.Verify(ctx, request.ChallengeID, request.DevelopmentCode)
	if err != nil {
		t.Fatal(err)
	}
	if _, err = service.Verify(ctx, request.ChallengeID, request.DevelopmentCode); !errors.Is(err, phoneotp.ErrInvalid) {
		t.Fatal("code replay accepted")
	}
	results = make(chan error, 12)
	for i := 0; i < 12; i++ {
		group.Add(1)
		go func() { defer group.Done(); _, err := service.Redeem(ctx, verified.Receipt); results <- err }()
	}
	group.Wait()
	close(results)
	success := 0
	for err := range results {
		if err == nil {
			success++
		} else if !errors.Is(err, phoneotp.ErrInvalid) {
			t.Fatal(err)
		}
	}
	if success != 1 {
		t.Fatal("receipt consumed more than once", success)
	}
}
func TestPhoneVerificationAPIAndExpiry(t *testing.T) {
	pool := phoneOTPTestPool(t)
	ctx := context.Background()
	service, _ := phoneotp.New(phoneVerificationStore{pool: pool}, phoneotp.NewTestSender([]string{"+79990000001"}), strings.Repeat("s", 32), []string{"+79990000001"})
	s := &Server{pool: pool, phoneVerification: service, phoneVerificationTest: true}
	w := httptest.NewRecorder()
	s.phoneVerificationRequest(w, httptest.NewRequest(http.MethodPost, "/", strings.NewReader(`{"phone":"+7 (999) 000-00-01"}`)))
	if w.Code != 200 {
		t.Fatal(w.Code, w.Body.String())
	}
	var issued phoneotp.RequestResult
	if err := json.Unmarshal(w.Body.Bytes(), &issued); err != nil {
		t.Fatal(err)
	}
	if issued.DevelopmentCode == "" || issued.RetryAfter != 60 {
		t.Fatal("test API contract")
	}
	if _, err := pool.Exec(ctx, `UPDATE phone_verification_challenges SET expires_at=NOW()-INTERVAL '1 second' WHERE id=$1`, issued.ChallengeID); err != nil {
		t.Fatal(err)
	}
	w = httptest.NewRecorder()
	s.phoneVerificationVerify(w, httptest.NewRequest(http.MethodPost, "/", strings.NewReader(fmt.Sprintf(`{"challenge_id":%q,"code":%q}`, issued.ChallengeID, issued.DevelopmentCode))))
	if w.Code != 400 {
		t.Fatal("expired OTP accepted", w.Code)
	}
}

func TestPhoneVerificationConcurrentSendAndReceiptExpiry(t *testing.T) {
	pool := phoneOTPTestPool(t)
	ctx := context.Background()
	service, _ := phoneotp.New(phoneVerificationStore{pool: pool}, phoneotp.NewTestSender([]string{"+79990000001"}), strings.Repeat("s", 32), []string{"+79990000001"})
	type result struct {
		issued phoneotp.RequestResult
		err    error
	}
	results := make(chan result, 12)
	var group sync.WaitGroup
	for i := 0; i < 12; i++ {
		group.Add(1)
		go func() {
			defer group.Done()
			issued, err := service.Request(ctx, "+79990000001", "same-ip")
			results <- result{issued, err}
		}()
	}
	group.Wait()
	close(results)
	var issued phoneotp.RequestResult
	success := 0
	for r := range results {
		if r.err == nil {
			issued = r.issued
			success++
		} else if !errors.Is(r.err, phoneotp.ErrRateLimit) {
			t.Fatal(r.err)
		}
	}
	if success != 1 {
		t.Fatal("concurrent send bypassed cooldown", success)
	}
	verified, err := service.Verify(ctx, issued.ChallengeID, issued.DevelopmentCode)
	if err != nil {
		t.Fatal(err)
	}
	if _, err = pool.Exec(ctx, `UPDATE phone_verification_challenges SET expires_at=NOW()-INTERVAL '1 second' WHERE id=$1`, issued.ChallengeID); err != nil {
		t.Fatal(err)
	}
	if _, err = service.Redeem(ctx, verified.Receipt); !errors.Is(err, phoneotp.ErrInvalid) {
		t.Fatal("expired receipt accepted", err)
	}
}

func TestPhoneVerificationSmokeClient(t *testing.T) {
	node, err := exec.LookPath("node")
	if err != nil {
		t.Skip("Node is required for the optional CLI smoke check")
	}
	pool := phoneOTPTestPool(t)
	service, err := phoneotp.New(phoneVerificationStore{pool: pool}, phoneotp.NewTestSender([]string{"+79990000001"}), strings.Repeat("s", 32), []string{"+79990000001"})
	if err != nil {
		t.Fatal(err)
	}
	s := &Server{pool: pool, phoneVerification: service, phoneVerificationTest: true}
	mux := http.NewServeMux()
	s.phoneVerificationRoutes(mux)
	api := httptest.NewServer(mux)
	defer api.Close()
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	output, err := exec.CommandContext(ctx, node, "../../scripts/phone-otp-check.mjs", api.URL, "+79990000001").CombinedOutput()
	if err != nil {
		t.Fatalf("smoke client: %v: %s", err, output)
	}
	if !strings.Contains(string(output), `"phone_verified":true`) || !strings.Contains(string(output), `"code_replay_blocked":true`) {
		t.Fatal("unexpected smoke result")
	}
}
