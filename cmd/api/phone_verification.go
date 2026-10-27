package main

import (
	"encoding/json"
	"errors"
	"github.com/sinedfq/go-hh/internal/phoneotp"
	"net"
	"net/http"
	"os"
	"strings"
)

func (s *Server) phoneVerificationRoutes(mux *http.ServeMux) {
	mux.HandleFunc("GET /api/phone-verification/config", s.phoneVerificationConfig)
	mux.HandleFunc("POST /api/phone-verification/request", s.phoneVerificationRequest)
	mux.HandleFunc("POST /api/phone-verification/verify", s.phoneVerificationVerify)
}

func (s *Server) configurePhoneVerification() error {
	mode := os.Getenv("OTP_MODE")
	if mode == "" || mode == "off" {
		return nil
	}
	secret := os.Getenv("OTP_SECRET")
	if len(secret) < 32 || secret == string(getJWTSecret()) {
		return errors.New("OTP_SECRET must contain at least 32 bytes and differ from JWT_SECRET")
	}
	var sender phoneotp.Sender
	var phones []string
	switch mode {
	case "development":
		if os.Getenv("APP_ENV") != "development" {
			return errors.New("development OTP requires APP_ENV=development")
		}
		for _, raw := range strings.Split(os.Getenv("OTP_TEST_PHONES"), ",") {
			phone, err := phoneotp.NormalizePhone(raw)
			if err != nil {
				return errors.New("OTP_TEST_PHONES must explicitly list valid test numbers")
			}
			phones = append(phones, phone)
		}
		if len(phones) == 0 {
			return errors.New("OTP_TEST_PHONES is required")
		}
		sender = phoneotp.NewTestSender(phones)
	case "http":
		var err error
		sender, err = phoneotp.NewHTTPSender(os.Getenv("SMS_GATEWAY_URL"), os.Getenv("SMS_GATEWAY_TOKEN"), os.Getenv("APP_ENV") == "development")
		if err != nil {
			return err
		}
	default:
		return errors.New("OTP_MODE must be off, development or http")
	}
	service, err := phoneotp.New(phoneVerificationStore{s.pool}, sender, secret, phones)
	if err != nil {
		return err
	}
	s.phoneVerification = service
	s.phoneVerificationTest = mode == "development"
	return nil
}
func (s *Server) phoneVerificationConfig(w http.ResponseWriter, _ *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("Cache-Control", "no-store")
	json.NewEncoder(w).Encode(map[string]any{"enabled": s.phoneVerification != nil, "test_mode": s.phoneVerificationTest})
}
func (s *Server) phoneVerificationRequest(w http.ResponseWriter, r *http.Request) {
	if s.phoneVerification == nil {
		phoneVerificationError(w, 503, "Проверка номера пока не подключена")
		return
	}
	var body struct {
		Phone string `json:"phone"`
	}
	if json.NewDecoder(http.MaxBytesReader(w, r.Body, 4096)).Decode(&body) != nil {
		phoneVerificationError(w, 400, "Укажите номер телефона")
		return
	}
	if _, err := phoneotp.NormalizePhone(body.Phone); err != nil {
		phoneVerificationError(w, 400, "Укажите номер с кодом страны, например +7 999 123-45-67")
		return
	}
	ip, _, err := net.SplitHostPort(r.RemoteAddr)
	if err != nil {
		ip = r.RemoteAddr
	}
	result, err := s.phoneVerification.Request(r.Context(), body.Phone, ip)
	if err != nil {
		s.phoneVerificationFailure(w, err)
		return
	}
	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("Cache-Control", "no-store")
	json.NewEncoder(w).Encode(result)
}
func (s *Server) phoneVerificationVerify(w http.ResponseWriter, r *http.Request) {
	if s.phoneVerification == nil {
		phoneVerificationError(w, 503, "Проверка номера пока не подключена")
		return
	}
	var body struct {
		ChallengeID string `json:"challenge_id"`
		Code        string `json:"code"`
	}
	if json.NewDecoder(http.MaxBytesReader(w, r.Body, 4096)).Decode(&body) != nil {
		phoneVerificationError(w, 400, "Укажите код подтверждения")
		return
	}
	result, err := s.phoneVerification.Verify(r.Context(), body.ChallengeID, body.Code)
	if err != nil {
		s.phoneVerificationFailure(w, err)
		return
	}
	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("Cache-Control", "no-store")
	json.NewEncoder(w).Encode(result)
}
func (s *Server) phoneVerificationFailure(w http.ResponseWriter, err error) {
	switch {
	case errors.Is(err, phoneotp.ErrInvalid):
		phoneVerificationError(w, 400, "Код неверный, истёк или уже использован. Запросите новый код")
	case errors.Is(err, phoneotp.ErrRateLimit):
		w.Header().Set("Retry-After", "60")
		phoneVerificationError(w, 429, "Слишком много запросов. Подождите перед повторной отправкой")
	case errors.Is(err, phoneotp.ErrTestPhone):
		phoneVerificationError(w, 400, "Этот номер не включён в список тестовых номеров")
	default:
		phoneVerificationError(w, 503, "Не удалось отправить или проверить код. Повторите позже")
	}
}
func phoneVerificationError(w http.ResponseWriter, status int, message string) {
	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("Cache-Control", "no-store")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(map[string]string{"error": message})
}
