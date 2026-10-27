package main

import (
	"context"
	"encoding/json"
	"errors"
	"github.com/sinedfq/go-hh/internal/phoneotp"
	"net/http/httptest"
	"strings"
	"testing"
)

type failedCodeSender struct{}

func (failedCodeSender) Send(context.Context, string, string, string, map[string]string) error {
	return errors.New("delivery unavailable")
}

func TestFirebaseCodeIsDeliveredWithoutLeakingIntoHTTP(t *testing.T) {
	pool := phoneOTPTestPool(t)
	service, _ := phoneotp.NewAppNotificationService(phoneVerificationStore{pool: pool}, strings.Repeat("s", 32))
	sender := &capturingSender{deliveries: make(chan capturedNotification, 1)}
	s := NewServer(&PostgresStorage{pool: pool}, nil)
	s.phoneVerification = service
	s.phoneVerificationTest = true
	s.phoneVerificationFirebase = true
	s.pushSender = sender
	body := `{"phone":"+79990000001","device_key":"` + strings.Repeat("a", 64) + `","push_token":"fixture-firebase-device-token","request_id":"12345678-1234-1234-1234-123456789abc"}`
	w := httptest.NewRecorder()
	s.phoneVerificationRequest(w, httptest.NewRequest("POST", "/", strings.NewReader(body)))
	if w.Code != 200 {
		t.Fatal("request", w.Code)
	}
	var response phoneotp.RequestResult
	json.Unmarshal(w.Body.Bytes(), &response)
	event := <-sender.deliveries
	if event.data["type"] != "auth_code" || event.token != "fixture-firebase-device-token" || len(event.data["code"]) != 6 || event.data["expires_at"] == "" {
		t.Fatal("incorrect Firebase payload")
	}
	if response.DevelopmentCode != "" || strings.Contains(w.Body.String(), event.data["code"]) {
		t.Fatal("code leaked into HTTP response")
	}
	// Bind a verified code to an existing signed-in user.
	verify := httptest.NewRecorder()
	b, _ := json.Marshal(map[string]string{"challenge_id": response.ChallengeID, "code": event.data["code"], "device_key": strings.Repeat("a", 64)})
	s.phoneVerificationVerify(verify, httptest.NewRequest("POST", "/", strings.NewReader(string(b))))
	if verify.Code != 200 {
		t.Fatal("verify", verify.Code)
	}
	var proof phoneotp.Verification
	json.Unmarshal(verify.Body.Bytes(), &proof)
	var userID int
	if err := pool.QueryRow(context.Background(), `INSERT INTO users(email,password_hash,role)VALUES('link@example.test','hash','candidate') RETURNING id`).Scan(&userID); err != nil {
		t.Fatal(err)
	}
	link := func(receipt string) int {
		b, _ := json.Marshal(RegisterRequest{Phone: "+79990000001", PhoneReceipt: receipt, DeviceKey: strings.Repeat("a", 64)})
		r := httptest.NewRequest("PUT", "/", strings.NewReader(string(b)))
		r = r.WithContext(context.WithValue(r.Context(), userContextKey, &Claims{UserID: userID}))
		w := httptest.NewRecorder()
		s.accountPhoneHandler(w, r)
		return w.Code
	}
	if link(proof.Receipt) != 200 {
		t.Fatal("phone not linked")
	}
	if link(proof.Receipt) != 400 {
		t.Fatal("proof reused")
	}
	var phone string
	if err := pool.QueryRow(context.Background(), `SELECT phone FROM user_phone_accounts WHERE user_id=$1`, userID).Scan(&phone); err != nil || phone != "+79990000001" {
		t.Fatal("binding missing")
	}
	// A failed Firebase send must not leave a valid, delivered challenge or a cooldown.
	s.pushSender = failedCodeSender{}
	body = strings.Replace(body, "+79990000001", "+79990000002", 1)
	w = httptest.NewRecorder()
	s.phoneVerificationRequest(w, httptest.NewRequest("POST", "/", strings.NewReader(body)))
	if w.Code != 503 {
		t.Fatal("delivery error", w.Code)
	}
	var count int
	pool.QueryRow(context.Background(), `SELECT COUNT(*) FROM phone_verification_challenges WHERE phone='+79990000002'`).Scan(&count)
	if count != 0 {
		t.Fatal("failed code retained")
	}
}
