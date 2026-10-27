package main

import (
	"encoding/json"
	"github.com/sinedfq/go-hh/internal/phoneotp"
	"net"
	"net/http"
	"strconv"
	"time"
)

// Website codes are sent only to devices already enrolled by the phone's account.
// A caller-supplied Firebase token must never authorize browser login.
func (s *Server) webPhoneRequest(w http.ResponseWriter, r *http.Request) {
	if !s.phoneVerificationFirebase || s.phoneVerification == nil || s.pushSender == nil {
		phoneVerificationError(w, 503, "Вход по телефону временно недоступен. Используйте почту")
		return
	}
	var body struct {
		Phone     string `json:"phone"`
		DeviceKey string `json:"device_key"`
	}
	if json.NewDecoder(http.MaxBytesReader(w, r.Body, 2048)).Decode(&body) != nil {
		phoneVerificationError(w, 400, "Укажите номер телефона")
		return
	}
	phone, err := phoneotp.NormalizePhone(body.Phone)
	if err != nil {
		phoneVerificationError(w, 400, "Проверьте номер телефона")
		return
	}
	binding, err := s.phoneVerification.DeviceBinding(body.DeviceKey)
	if err != nil {
		phoneVerificationError(w, 400, "Не удалось подготовить вход в браузере")
		return
	}
	var owner int
	if err = s.pool.QueryRow(r.Context(), `SELECT user_id FROM user_phone_accounts WHERE phone=$1`, phone).Scan(&owner); err != nil {
		phoneVerificationError(w, 400, "Привяжите номер в приложении Trudomix и войдите в аккаунт на телефоне")
		return
	}
	rows, err := s.pool.Query(r.Context(), `SELECT token FROM device_tokens WHERE user_id=$1 ORDER BY updated_at DESC LIMIT 3`, owner)
	if err != nil {
		phoneVerificationError(w, 503, "Не удалось подключиться к уведомлениям")
		return
	}
	var tokens []string
	for rows.Next() {
		var token string
		if err = rows.Scan(&token); err != nil {
			break
		}
		tokens = append(tokens, token)
	}
	rows.Close()
	if err != nil || rows.Err() != nil || len(tokens) == 0 {
		phoneVerificationError(w, 400, "Откройте Trudomix на телефоне, войдите в аккаунт и разрешите уведомления")
		return
	}
	ip, _, err := net.SplitHostPort(r.RemoteAddr)
	if err != nil {
		ip = r.RemoteAddr
	}
	result, err := s.phoneVerification.RequestBound(r.Context(), phone, ip, binding)
	if err != nil {
		s.phoneVerificationFailure(w, err)
		return
	}
	if _, err = s.pool.Exec(r.Context(), `UPDATE phone_verification_challenges SET delivery_mode='firebase_account',account_user_id=$2 WHERE id=$1`, result.ChallengeID, owner); err != nil {
		phoneVerificationError(w, 503, "Не удалось подготовить код")
		return
	}
	delivered := false
	data := map[string]string{"type": "web_auth_code", "code": result.DevelopmentCode, "phone": phone, "expires_at": strconv.FormatInt(time.Now().Add(time.Duration(result.ExpiresIn)*time.Second).Unix(), 10)}
	for _, token := range tokens {
		if s.pushSender.Send(r.Context(), token, "Код входа на сайт Trudomix", "Код для входа в браузере", data) == nil {
			delivered = true
		}
	}
	result.DevelopmentCode = ""
	if !delivered {
		s.pool.Exec(r.Context(), `DELETE FROM phone_verification_challenges WHERE id=$1 AND verified_at IS NULL`, result.ChallengeID)
		phoneVerificationError(w, 503, "Не удалось доставить код в приложение. Проверьте уведомления на телефоне")
		return
	}
	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("Cache-Control", "no-store")
	json.NewEncoder(w).Encode(result)
}
