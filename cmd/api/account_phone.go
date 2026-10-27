package main

import (
	"encoding/json"
	"errors"
	"github.com/jackc/pgx/v5"
	"github.com/sinedfq/go-hh/internal/phoneotp"
	"net/http"
)

// Only the authenticated owner can attach a verified phone to an existing account.
func (s *Server) accountPhoneHandler(w http.ResponseWriter, r *http.Request) {
	user := getUserFromContext(r.Context())
	if user == nil {
		phoneVerificationError(w, 401, "Войдите в аккаунт")
		return
	}
	w.Header().Set("Cache-Control", "no-store")
	if r.Method == http.MethodGet {
		var phone, mode string
		err := s.pool.QueryRow(r.Context(), `SELECT phone,confirmation_mode FROM user_phone_accounts WHERE user_id=$1`, user.UserID).Scan(&phone, &mode)
		if err != nil && !errors.Is(err, pgx.ErrNoRows) {
			phoneVerificationError(w, 503, "Не удалось загрузить номер")
			return
		}
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]any{"phone": phone, "linked": phone != "", "test_mode": phone != "" && mode != "sms"})
		return
	}
	var req RegisterRequest
	if json.NewDecoder(http.MaxBytesReader(w, r.Body, 8192)).Decode(&req) != nil {
		phoneVerificationError(w, 400, "Укажите телефон и подтверждение")
		return
	}
	tx, err := s.pool.Begin(r.Context())
	if err != nil {
		phoneRegistrationError(w, err)
		return
	}
	defer tx.Rollback(r.Context())
	// Serialize changes to the same account without affecting other users.
	if _, err = tx.Exec(r.Context(), `SELECT id FROM users WHERE id=$1 FOR UPDATE`, user.UserID); err != nil {
		phoneRegistrationError(w, err)
		return
	}
	mode, binding, err := s.consumePhoneReceipt(r.Context(), tx, req.PhoneReceipt, req.Phone, req.DeviceKey)
	if err != nil {
		phoneRegistrationError(w, err)
		return
	}
	if mode == "firebase_account" {
		phoneRegistrationError(w, phoneotp.ErrInvalid)
		return
	}
	phone, err := phoneotp.NormalizePhone(req.Phone)
	if err != nil {
		phoneRegistrationError(w, err)
		return
	}
	_, err = tx.Exec(r.Context(), `INSERT INTO user_phone_accounts(user_id,phone,confirmation_mode,device_hash) VALUES($1,$2,$3,$4)
 ON CONFLICT(user_id) DO UPDATE SET phone=EXCLUDED.phone,confirmation_mode=EXCLUDED.confirmation_mode,device_hash=EXCLUDED.device_hash,confirmed_at=NOW()`, user.UserID, phone, mode, binding)
	if err != nil {
		phoneRegistrationError(w, err)
		return
	}
	if err = tx.Commit(r.Context()); err != nil {
		phoneRegistrationError(w, err)
		return
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]any{"phone": phone, "linked": true, "test_mode": mode != "sms"})
}
