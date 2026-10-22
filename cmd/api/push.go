package main

import (
	"context"
	"encoding/json"
	"fmt"
	"github.com/sinedfq/go-hh/internal/push"
	"log"
	"net/http"
	"strconv"
	"strings"
	"time"
)

// Persist the event before delivering it. A delivery failure must not lose the inbox event.
func (s *Server) createNotification(ctx context.Context, req CreateNotificationRequest) error {
	if err := s.storage.CreateNotification(ctx, req); err != nil {
		return err
	}
	data := map[string]string{"type": req.Type}
	for key, value := range req.Data {
		if value != nil {
			data[key] = fmt.Sprint(value)
		}
	}
	go s.sendPush(req.UserID, req.Title, req.Message, data)
	return nil
}

func applicationNotification(app *Application, appID int, status string) CreateNotificationRequest {
	title := "Отклик просмотрен"
	message := app.CompanyName + " просмотрела ваш отклик на вакансию «" + app.VacancyTitle + "»"
	switch status {
	case "accepted":
		title = "Вас пригласили"
		message = app.CompanyName + " приглашает вас по вакансии «" + app.VacancyTitle + "»"
	case "rejected":
		title = "Ответ на отклик"
		message = app.CompanyName + ": по вакансии «" + app.VacancyTitle + "» пока отказ. Продолжайте поиск — впереди новые возможности."
	}
	return CreateNotificationRequest{UserID: app.CandidateUserID, Type: "application_" + status, Title: title, Message: message,
		Data: map[string]interface{}{"application_id": appID, "vacancy_id": app.VacancyID, "vacancy_title": app.VacancyTitle, "company_name": app.CompanyName}}
}

func (s *Server) registerDeviceHandler(w http.ResponseWriter, r *http.Request) {
	claims := getUserFromContext(r.Context())
	if claims == nil {
		http.Error(w, "unauthorized", 401)
		return
	}
	var req struct {
		Token string `json:"token"`
	}
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 8192)).Decode(&req); err != nil || strings.TrimSpace(req.Token) == "" || len(req.Token) > 4096 {
		http.Error(w, "invalid token", 400)
		return
	}
	var err error
	if r.Method == http.MethodDelete {
		_, err = s.pool.Exec(r.Context(), `DELETE FROM device_tokens WHERE token=$1 AND user_id=$2`, req.Token, claims.UserID)
	} else {
		_, err = s.pool.Exec(r.Context(), `INSERT INTO device_tokens(token,user_id) VALUES($1,$2) ON CONFLICT(token) DO UPDATE SET user_id=EXCLUDED.user_id,updated_at=NOW()`, req.Token, claims.UserID)
	}
	if err != nil {
		log.Printf("device registration: %v", err)
		http.Error(w, "device registration failed", 500)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (s *Server) sendPush(userID int, title, body string, data map[string]string) {
	if s.pushSender == nil {
		return
	}
	ctx, cancel := context.WithTimeout(context.Background(), 15*time.Second)
	defer cancel()
	rows, err := s.pool.Query(ctx, `SELECT token FROM device_tokens WHERE user_id=$1`, userID)
	if err != nil {
		log.Printf("push tokens: %v", err)
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
	if err == nil {
		err = rows.Err()
	}
	rows.Close()
	if err != nil {
		log.Printf("push token scan: %v", err)
		return
	}
	for _, token := range tokens {
		if err := s.pushSender.Send(ctx, token, title, body, data); err != nil {
			if push.InvalidToken(err) {
				s.pool.Exec(ctx, `DELETE FROM device_tokens WHERE token=$1 AND user_id=$2`, token, userID)
			} else {
				log.Printf("push delivery to user %d failed: %v", userID, err)
			}
		}
	}
}

func (s *Server) pushNewMessage(conversationID, senderID int, content string) {
	if s.pushSender == nil {
		return
	}
	ctx, cancel := context.WithTimeout(context.Background(), 20*time.Second)
	defer cancel()
	rows, err := s.pool.Query(ctx, `SELECT DISTINCT u.id FROM conversations c
 JOIN applications a ON a.id=c.application_id JOIN vacancies v ON v.id=a.vacancy_id
 JOIN users u ON (u.id=a.candidate_user_id OR (u.company_id=v.company_id AND u.role IN ('employer','admin')))
 WHERE c.id=$1 AND u.id<>$2`, conversationID, senderID)
	if err != nil {
		log.Printf("push recipients: %v", err)
		return
	}
	var ids []int
	for rows.Next() {
		var id int
		if err = rows.Scan(&id); err != nil {
			break
		}
		ids = append(ids, id)
	}
	if err == nil {
		err = rows.Err()
	}
	rows.Close()
	if err != nil {
		return
	}
	senderName := "Собеседник"
	if sender, err := s.storage.GetUserByID(ctx, senderID); err == nil {
		if sender.CompanyID != nil {
			if company, err := s.storage.GetCompanyByID(ctx, *sender.CompanyID); err == nil {
				senderName = company.Name
			}
		} else if resume, err := s.storage.GetResumeByUserID(ctx, senderID); err == nil && resume.FullName != "" {
			senderName = resume.FullName
		}
	}
	preview := []rune(strings.Join(strings.Fields(content), " "))
	if len(preview) > 180 {
		preview = append(preview[:180], '…')
	}
	for _, id := range ids {
		s.sendPush(id, senderName, string(preview), map[string]string{"conversation_id": strconv.Itoa(conversationID), "sender_name": senderName, "type": "message"})
	}
}
