package main

import (
	"encoding/json"
	"github.com/sinedfq/go-hh/internal/phoneotp"
	"net/http"
	"os"
)

func (s *Server) resetPhoneLimits(w http.ResponseWriter, r *http.Request) {
	var body struct {
		Phone string `json:"phone"`
		All   bool   `json:"all"`
	}
	if json.NewDecoder(http.MaxBytesReader(w, r.Body, 2048)).Decode(&body) != nil {
		phoneVerificationError(w, 400, "Укажите номер телефона")
		return
	}
	if body.All && body.Phone != "" {
		phoneVerificationError(w, 400, "Выберите номер или общий сброс")
		return
	}
	if body.All && os.Getenv("APP_ENV") != "development" {
		phoneVerificationError(w, 403, "Общий сброс доступен только в development")
		return
	}
	if !body.All {
		if _, err := phoneotp.NormalizePhone(body.Phone); err != nil {
			phoneVerificationError(w, 400, "Укажите номер в международном формате")
			return
		}
	}
	count, err := phoneotp.ResetLimits(r.Context(), s.pool, body.Phone, body.All)
	if err != nil {
		phoneVerificationError(w, 503, "Не удалось сбросить лимиты")
		return
	}
	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("Cache-Control", "no-store")
	json.NewEncoder(w).Encode(map[string]any{"reset": true, "invalidated_challenges": count})
}
