package main

import (
	"context"
	"encoding/json"
	"github.com/sinedfq/go-hh/internal/phoneotp"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func TestNotificationPhoneRegistrationAndLoginAreDeviceBound(t *testing.T) {
	pool := phoneOTPTestPool(t)
	ctx := context.Background()
	service, err := phoneotp.NewAppNotificationService(phoneVerificationStore{pool: pool}, strings.Repeat("s", 32))
	if err != nil {
		t.Fatal(err)
	}
	s := NewServer(&PostgresStorage{pool: pool}, nil)
	s.phoneVerification = service
	s.phoneVerificationTest = true
	keyA, keyB := strings.Repeat("a", 64), strings.Repeat("b", 64)
	phone := "+79990000001"
	issue := func(key string) string {
		t.Helper()
		pool.Exec(ctx, `UPDATE phone_verification_challenges SET created_at=created_at-INTERVAL '61 seconds'`)
		w := httptest.NewRecorder()
		body, _ := json.Marshal(map[string]string{"phone": phone, "device_key": key})
		s.phoneVerificationRequest(w, httptest.NewRequest("POST", "/", strings.NewReader(string(body))))
		if w.Code != 200 {
			t.Fatal("request failed", w.Code)
		}
		var challenge phoneotp.RequestResult
		json.Unmarshal(w.Body.Bytes(), &challenge)
		verifyBody, _ := json.Marshal(map[string]string{"challenge_id": challenge.ChallengeID, "code": challenge.DevelopmentCode, "device_key": keyB})
		if key == keyA {
			denied := httptest.NewRecorder()
			s.phoneVerificationVerify(denied, httptest.NewRequest("POST", "/", strings.NewReader(string(verifyBody))))
			if denied.Code != 400 {
				t.Fatal("other installation verified the code")
			}
		}
		verifyBody, _ = json.Marshal(map[string]string{"challenge_id": challenge.ChallengeID, "code": challenge.DevelopmentCode, "device_key": key})
		w = httptest.NewRecorder()
		s.phoneVerificationVerify(w, httptest.NewRequest("POST", "/", strings.NewReader(string(verifyBody))))
		if w.Code != 200 {
			t.Fatal("verify failed", w.Code)
		}
		var proof phoneotp.Verification
		json.Unmarshal(w.Body.Bytes(), &proof)
		return proof.Receipt
	}
	proof := issue(keyA)
	register := func(number, receipt, email string) *httptest.ResponseRecorder {
		body, _ := json.Marshal(RegisterRequest{Email: email, Password: "test-password-123", Role: "candidate", Phone: number, PhoneReceipt: receipt, DeviceKey: keyA})
		w := httptest.NewRecorder()
		s.registerHandler(w, httptest.NewRequest("POST", "/", strings.NewReader(string(body))))
		return w
	}
	if w := register("+79990000002", proof, "phone-test@example.test"); w.Code != 400 {
		t.Fatal("receipt accepted for another phone", w.Code)
	}
	w := register(phone, proof, "phone-test@example.test")
	if w.Code != http.StatusCreated {
		t.Fatal("register failed", w.Code)
	}
	var registered AuthResponse
	if err = json.Unmarshal(w.Body.Bytes(), &registered); err != nil {
		t.Fatal(err)
	}
	var storedPhone, mode string
	if err = pool.QueryRow(ctx, `SELECT phone,confirmation_mode FROM user_phone_accounts WHERE user_id=$1`, registered.User.ID).Scan(&storedPhone, &mode); err != nil || storedPhone != phone || mode != "app_notification" {
		t.Fatal("phone was not persisted", err)
	}
	login := func(key, receipt string) *httptest.ResponseRecorder {
		body, _ := json.Marshal(RegisterRequest{Phone: phone, PhoneReceipt: receipt, DeviceKey: key})
		w := httptest.NewRecorder()
		s.phoneLoginHandler(w, httptest.NewRequest("POST", "/", strings.NewReader(string(body))))
		return w
	}
	if w = login(keyA, proof); w.Code != 401 {
		t.Fatal("registration receipt reused for login")
	}
	if w = login(keyB, issue(keyB)); w.Code != 401 || strings.Contains(w.Body.String(), `"token"`) {
		t.Fatal("test notification allowed account takeover")
	}
	loginProof := issue(keyA)
	w = login(keyA, loginProof)
	if w.Code != 200 {
		t.Fatal("same device login failed", w.Code)
	}
	var signedIn AuthResponse
	json.Unmarshal(w.Body.Bytes(), &signedIn)
	claims, err := ValidateToken(signedIn.Token)
	if err != nil || claims.UserID != registered.User.ID {
		t.Fatal("incorrect login session", err)
	}
	if w = login(keyA, loginProof); w.Code != 401 {
		t.Fatal("login receipt replayed")
	}
	// A duplicate phone must roll back both the user insert and receipt consumption.
	duplicateProof := issue(keyA)
	w = register(phone, duplicateProof, "duplicate-phone@example.test")
	if w.Code != 409 {
		t.Fatal("duplicate phone accepted", w.Code)
	}
	var count int
	pool.QueryRow(ctx, `SELECT count(*) FROM users WHERE email='duplicate-phone@example.test'`).Scan(&count)
	if count != 0 {
		t.Fatal("failed registration left an account")
	}
	if w = login(keyA, duplicateProof); w.Code != 200 {
		t.Fatal("failed registration consumed the receipt", w.Code)
	}
}

func TestAppNotificationModeCannotAuthenticateSMSAccount(t *testing.T) {
	pool := phoneOTPTestPool(t)
	ctx := context.Background()
	service, _ := phoneotp.NewAppNotificationService(phoneVerificationStore{pool: pool}, strings.Repeat("s", 32))
	s := NewServer(&PostgresStorage{pool: pool}, nil)
	s.phoneVerification = service
	var id int
	pool.QueryRow(ctx, `INSERT INTO users(email,password_hash,role) VALUES('sms-account@example.test','unused','candidate') RETURNING id`).Scan(&id)
	pool.Exec(ctx, `INSERT INTO user_phone_accounts(user_id,phone,confirmation_mode) VALUES($1,'+79990000002','sms')`, id)
	key := strings.Repeat("b", 64)
	binding, _ := service.DeviceBinding(key)
	challenge, err := service.RequestBound(ctx, "+79990000002", "ip", binding)
	if err != nil {
		t.Fatal(err)
	}
	proof, err := service.Verify(ctx, challenge.ChallengeID, challenge.DevelopmentCode)
	if err != nil {
		t.Fatal(err)
	}
	body, _ := json.Marshal(RegisterRequest{Phone: "+79990000002", PhoneReceipt: proof.Receipt, DeviceKey: key})
	w := httptest.NewRecorder()
	s.phoneLoginHandler(w, httptest.NewRequest("POST", "/", strings.NewReader(string(body))))
	if w.Code != 401 {
		t.Fatal("notification code bypassed real SMS verification")
	}
}
