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

func TestWebPhoneLoginUsesOnlyAccountDevicesAndPinsAccountOwner(t *testing.T) {
	pool := phoneOTPTestPool(t)
	ctx := context.Background()
	service, _ := phoneotp.NewAppNotificationService(phoneVerificationStore{pool: pool}, strings.Repeat("s", 32))
	s := NewServer(&PostgresStorage{pool: pool}, nil)
	s.phoneVerification = service
	s.phoneVerificationFirebase = true
	sender := &capturingSender{deliveries: make(chan capturedNotification, 8)}
	s.pushSender = sender
	var owner, other int
	pool.QueryRow(ctx, `INSERT INTO users(email,password_hash,role) VALUES('web@example.test','hash','candidate') RETURNING id`).Scan(&owner)
	pool.QueryRow(ctx, `INSERT INTO users(email,password_hash,role) VALUES('other@example.test','hash','candidate') RETURNING id`).Scan(&other)
	accountBinding, _ := service.DeviceBinding(strings.Repeat("a", 64))
	browserKey := strings.Repeat("b", 64)
	if _, err := pool.Exec(ctx, `INSERT INTO user_phone_accounts(user_id,phone,confirmation_mode,device_hash)VALUES($1,'+79990000001','app_notification',$2)`, owner, accountBinding); err != nil {
		t.Fatal(err)
	}
	pool.Exec(ctx, `INSERT INTO device_tokens(token,user_id)VALUES('owner-device',$1),('unrelated-device',$2)`, owner, other)
	issue := func() string {
		t.Helper()
		pool.Exec(ctx, `UPDATE phone_verification_challenges SET created_at=created_at-INTERVAL '61 seconds'`)
		body, _ := json.Marshal(map[string]string{"phone": "+79990000001", "device_key": browserKey, "push_token": "attacker-device"})
		w := httptest.NewRecorder()
		s.webPhoneRequest(w, httptest.NewRequest("POST", "/", strings.NewReader(string(body))))
		if w.Code != 200 {
			t.Fatal("web request", w.Code)
		}
		var challenge phoneotp.RequestResult
		json.Unmarshal(w.Body.Bytes(), &challenge)
		event := <-sender.deliveries
		if event.token != "owner-device" || event.data["type"] != "web_auth_code" || challenge.DevelopmentCode != "" {
			t.Fatal("wrong recipient or code leak")
		}
		body, _ = json.Marshal(map[string]string{"challenge_id": challenge.ChallengeID, "code": event.data["code"], "device_key": browserKey})
		w = httptest.NewRecorder()
		s.phoneVerificationVerify(w, httptest.NewRequest("POST", "/", strings.NewReader(string(body))))
		if w.Code != 200 {
			t.Fatal("verify", w.Code)
		}
		var proof phoneotp.Verification
		json.Unmarshal(w.Body.Bytes(), &proof)
		return proof.Receipt
	}
	login := func(proof string) int {
		b, _ := json.Marshal(RegisterRequest{Phone: "+79990000001", PhoneReceipt: proof, DeviceKey: browserKey})
		w := httptest.NewRecorder()
		s.phoneLoginHandler(w, httptest.NewRequest("POST", "/", strings.NewReader(string(b))))
		return w.Code
	}
	receipt := issue()
	if _, err := s.createPhoneUser(ctx, RegisterRequest{Email: "new@example.test", Phone: "+79990000001", PhoneReceipt: receipt, DeviceKey: browserKey}, "hash", "candidate"); !errors.Is(err, phoneotp.ErrInvalid) {
		t.Fatal("browser proof accepted for signup")
	}
	if login(receipt) != 200 {
		t.Fatal("trusted browser login rejected")
	}
	if login(receipt) != 401 {
		t.Fatal("proof reused")
	}
	receipt = issue()
	if _, err := pool.Exec(ctx, `UPDATE user_phone_accounts SET user_id=$1 WHERE user_id=$2`, other, owner); err != nil {
		t.Fatal(err)
	}
	if login(receipt) != 401 {
		t.Fatal("old code opened the new owner of a reassigned phone")
	}
	select {
	case <-sender.deliveries:
		t.Fatal("code sent to unrelated device")
	default:
	}
}
