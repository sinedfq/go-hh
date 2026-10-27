package phoneotp

import (
	"context"
	"errors"
	"net/http"
	"net/http/httptest"
	"regexp"
	"strings"
	"testing"
	"time"
)

type spyStore struct {
	challenge    Challenge
	delivered    bool
	reservations int
}

func (s *spyStore) Reserve(_ context.Context, c Challenge) error {
	s.challenge = c
	s.reservations++
	return nil
}
func (s *spyStore) Delivered(_ context.Context, _ string) error { s.delivered = true; return nil }
func (s *spyStore) Verify(_ context.Context, _ string, code, receipt string, _ time.Time) error {
	if !s.delivered || !equalHash(s.challenge.CodeHash, code) {
		return ErrInvalid
	}
	s.challenge.ReceiptHash = receipt
	return nil
}
func (s *spyStore) Redeem(_ context.Context, _, hash string, _ time.Time) (string, error) {
	if !equalHash(s.challenge.ReceiptHash, hash) {
		return "", ErrInvalid
	}
	return s.challenge.Phone, nil
}

type captureSender struct {
	code string
	fail bool
}

func (s *captureSender) SendCode(_ context.Context, _, code string, _ time.Duration) error {
	s.code = code
	if s.fail {
		return errors.New("provider-private-detail")
	}
	return nil
}

func TestPhoneNormalization(t *testing.T) {
	for input, want := range map[string]string{"8 (999) 123-45-67": "+79991234567", "+7 999 123 45 67": "+79991234567", "+44 7700 900123": "+447700900123"} {
		got, err := NormalizePhone(input)
		if err != nil || got != want {
			t.Fatalf("%q: %q %v", input, got, err)
		}
	}
	for _, bad := range []string{"", "9991234567", "+00000000", "+7999abc1234", "+7+9991234567", "+1234567890123456"} {
		if _, err := NormalizePhone(bad); err == nil {
			t.Fatalf("accepted %q", bad)
		}
	}
}
func TestProductionNeverExposesCodeAndHashesAreDomainSeparated(t *testing.T) {
	store := &spyStore{}
	sender := &captureSender{}
	service, err := New(store, sender, strings.Repeat("s", 32), nil)
	if err != nil {
		t.Fatal(err)
	}
	result, err := service.Request(context.Background(), "+79991234567", "127.0.0.1")
	if err != nil {
		t.Fatal(err)
	}
	if result.DevelopmentCode != "" || !regexp.MustCompile(`^\d{6}$`).MatchString(sender.code) || store.challenge.CodeHash == sender.code || len(store.challenge.CodeHash) != 64 {
		t.Fatal("code confidentiality or format")
	}
	if digest(service.secret, "code", result.ChallengeID, sender.code) == digest(service.secret, "receipt", result.ChallengeID, sender.code) {
		t.Fatal("hash domains overlap")
	}
	verified, err := service.Verify(context.Background(), result.ChallengeID, sender.code)
	if err != nil {
		t.Fatal(err)
	}
	phone, err := service.Redeem(context.Background(), verified.Receipt)
	if err != nil || phone != "+79991234567" {
		t.Fatal(phone, err)
	}
	if _, err = service.Redeem(context.Background(), strings.Repeat("z", 129)); !errors.Is(err, ErrInvalid) {
		t.Fatal("invalid receipt accepted")
	}
}
func TestDevelopmentAllowsOnlyListedPhonesAndFailedDeliveryCannotVerify(t *testing.T) {
	store := &spyStore{}
	sender := &captureSender{fail: true}
	service, _ := New(store, sender, strings.Repeat("s", 32), []string{"+79990000001"})
	if _, err := service.Request(context.Background(), "+79990000002", "ip"); !errors.Is(err, ErrTestPhone) || store.reservations != 0 {
		t.Fatal("test allowlist bypass")
	}
	if _, err := service.Request(context.Background(), "+79990000001", "ip"); !errors.Is(err, ErrDelivery) || store.delivered {
		t.Fatal("delivery failure accepted")
	}
	if _, err := service.Verify(context.Background(), store.challenge.ID, sender.code); !errors.Is(err, ErrInvalid) {
		t.Fatal("unsent challenge accepted")
	}
	sender.fail = false
	result, err := service.Request(context.Background(), "+79990000001", "ip")
	if err != nil || result.DevelopmentCode == "" {
		t.Fatal("missing test code", err)
	}
}
func TestHTTPGatewayContractAndRedirectProtection(t *testing.T) {
	var sent bool
	gateway := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		sent = true
		if r.Method != "POST" || r.Header.Get("Authorization") != "Bearer test-token" || r.Header.Get("Content-Type") != "application/json" {
			t.Error("gateway contract")
		}
		w.WriteHeader(204)
	}))
	defer gateway.Close()
	sender, err := NewHTTPSender(gateway.URL, "test-token", true)
	if err != nil {
		t.Fatal(err)
	}
	if err = sender.SendCode(context.Background(), "+79990000001", "123456", CodeTTL); err != nil || !sent {
		t.Fatal("gateway request", err)
	}
	for _, endpoint := range []string{"http://sms.example.com/send", "https://user:secret@sms.example.com/send", "https://sms.example.com/send?key=secret"} {
		if _, err = NewHTTPSender(endpoint, "", false); err == nil {
			t.Fatal("unsafe gateway URL accepted")
		}
	}
	redirect := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		http.Redirect(w, r, gateway.URL, http.StatusTemporaryRedirect)
	}))
	defer redirect.Close()
	sent = false
	sender, _ = NewHTTPSender(redirect.URL, "test-token", true)
	if err = sender.SendCode(context.Background(), "+79990000001", "123456", CodeTTL); err == nil || sent {
		t.Fatal("SMS POST followed redirect")
	}
}
