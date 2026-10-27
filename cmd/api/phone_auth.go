package main

import (
	"context"
	"crypto/hmac"
	"encoding/json"
	"errors"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/sinedfq/go-hh/internal/phoneotp"
	"net/http"
	"strings"
)

func (s *Server) checkPhoneDevice(ctx context.Context, id, key string) error {
	var stored string
	if err := s.pool.QueryRow(ctx, `SELECT device_hash FROM phone_verification_challenges WHERE id=$1`, id).Scan(&stored); err != nil {
		return phoneotp.ErrInvalid
	}
	if stored == "" {
		return nil
	}
	binding, err := s.phoneVerification.DeviceBinding(key)
	if err != nil || !hmac.Equal([]byte(stored), []byte(binding)) {
		return phoneotp.ErrInvalid
	}
	return nil
}

func (s *Server) consumePhoneReceipt(ctx context.Context, tx pgx.Tx, receipt, phone, key string) (string, string, error) {
	if s.phoneVerification == nil {
		return "", "", phoneotp.ErrInvalid
	}
	normalized, err := phoneotp.NormalizePhone(phone)
	if err != nil {
		return "", "", phoneotp.ErrInvalid
	}
	verified, err := s.phoneVerification.RedeemWith(ctx, receipt, phoneVerificationStore{pool: s.pool, tx: tx})
	if err != nil || verified != normalized {
		return "", "", phoneotp.ErrInvalid
	}
	var binding, mode string
	if err = tx.QueryRow(ctx, `SELECT device_hash,delivery_mode FROM phone_verification_challenges WHERE id=$1`, strings.Split(receipt, ".")[0]).Scan(&binding, &mode); err != nil {
		return "", "", err
	}
	if mode != "sms" {
		actual, err := s.phoneVerification.DeviceBinding(key)
		if err != nil || binding == "" || !hmac.Equal([]byte(binding), []byte(actual)) {
			return "", "", phoneotp.ErrInvalid
		}
	}
	return mode, binding, nil
}
func (s *Server) createPhoneUser(ctx context.Context, req RegisterRequest, passwordHash, role string) (int, error) {
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return 0, err
	}
	defer tx.Rollback(ctx)
	mode, binding, err := s.consumePhoneReceipt(ctx, tx, req.PhoneReceipt, req.Phone, req.DeviceKey)
	if err != nil {
		return 0, err
	}
	if mode == "firebase_account" {
		return 0, phoneotp.ErrInvalid
	}
	phone, _ := phoneotp.NormalizePhone(req.Phone)
	var id int
	if err = tx.QueryRow(ctx, `INSERT INTO users(email,password_hash,role) VALUES($1,$2,$3) RETURNING id`, req.Email, passwordHash, role).Scan(&id); err != nil {
		return 0, err
	}
	if _, err = tx.Exec(ctx, `INSERT INTO user_phone_accounts(user_id,phone,confirmation_mode,device_hash) VALUES($1,$2,$3,$4)`, id, phone, mode, binding); err != nil {
		return 0, err
	}
	return id, tx.Commit(ctx)
}
func (s *Server) phoneLoginHandler(w http.ResponseWriter, r *http.Request) {
	if s.phoneVerification == nil {
		phoneVerificationError(w, 503, "Вход по телефону пока недоступен")
		return
	}
	w.Header().Set("Cache-Control", "no-store")
	var req RegisterRequest
	if json.NewDecoder(http.MaxBytesReader(w, r.Body, 8192)).Decode(&req) != nil {
		phoneVerificationError(w, 400, "Укажите номер и код подтверждения")
		return
	}
	tx, err := s.pool.Begin(r.Context())
	if err != nil {
		phoneVerificationError(w, 503, "Не удалось выполнить вход")
		return
	}
	defer tx.Rollback(r.Context())
	mode, binding, err := s.consumePhoneReceipt(r.Context(), tx, req.PhoneReceipt, req.Phone, req.DeviceKey)
	if err != nil {
		phoneVerificationError(w, 401, "Подтвердите номер ещё раз")
		return
	}
	phone, _ := phoneotp.NormalizePhone(req.Phone)
	var id int
	var accountMode, accountDevice string
	err = tx.QueryRow(r.Context(), `SELECT user_id,confirmation_mode,device_hash FROM user_phone_accounts WHERE phone=$1`, phone).Scan(&id, &accountMode, &accountDevice)
	if err != nil {
		phoneVerificationError(w, 401, "Номер не привязан к аккаунту. Войдите по почте или зарегистрируйтесь")
		return
	}
	// A locally displayed test code must never authenticate another installation or a real-SMS account.
	trustedWeb := false
	if mode == "firebase_account" {
		var owner int
		if err = tx.QueryRow(r.Context(), `SELECT account_user_id FROM phone_verification_challenges WHERE id=$1`, strings.Split(req.PhoneReceipt, ".")[0]).Scan(&owner); err != nil || owner != id {
			phoneVerificationError(w, 401, "Привязка номера изменилась. Запросите новый код")
			return
		}
		trustedWeb = true
	}
	if !trustedWeb && ((accountMode == "sms" && mode != "sms") || (accountMode != "sms" && (binding == "" || !hmac.Equal([]byte(binding), []byte(accountDevice))))) {
		phoneVerificationError(w, 401, "Тестовый вход доступен на устройстве регистрации. Используйте почту и пароль")
		return
	}
	user, err := s.storage.GetUserByID(r.Context(), id)
	if err != nil {
		phoneVerificationError(w, 503, "Не удалось загрузить аккаунт")
		return
	}
	token, err := GenerateToken(user.ID, user.Email, user.Role)
	if err != nil {
		phoneVerificationError(w, 503, "Не удалось выполнить вход")
		return
	}
	if err = tx.Commit(r.Context()); err != nil {
		phoneVerificationError(w, 503, "Не удалось выполнить вход")
		return
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(AuthResponse{Token: token, User: user})
}
func phoneRegistrationError(w http.ResponseWriter, err error) {
	if errors.Is(err, phoneotp.ErrInvalid) {
		phoneVerificationError(w, 400, "Подтвердите номер телефона кодом из уведомления")
		return
	}
	var pgErr *pgconn.PgError
	if errors.As(err, &pgErr) && pgErr.Code == "23505" {
		phoneVerificationError(w, 409, "Аккаунт с такой почтой или номером уже существует")
		return
	}
	phoneVerificationError(w, 503, "Не удалось создать аккаунт. Попробуйте позже")
}
