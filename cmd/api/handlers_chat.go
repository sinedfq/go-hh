package main

import (
	"encoding/json"
	"log"
	"net/http"
	"strconv"
	"strings"
	"time"
)

// GET /api/conversations — получить все чаты пользователя
func (s *Server) getUserConversationsHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	chats, err := s.storage.GetUserConversations(ctx, claims.UserID)
	if err != nil {
		log.Printf("⚠️ getUserConversations error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to get conversations"})
		return
	}

	json.NewEncoder(w).Encode(chats)
}

// GET /api/conversations/unread-count — количество непрочитанных
func (s *Server) getUnreadMessagesCountHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	count, err := s.storage.GetUnreadMessagesCount(ctx, claims.UserID)
	if err != nil {
		log.Printf("⚠️ getUnreadMessagesCount error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed"})
		return
	}

	json.NewEncoder(w).Encode(map[string]int{"count": count})
}

// GET /api/conversations/{id}/messages — получить сообщения чата
func (s *Server) getChatMessagesHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	parts := strings.Split(r.URL.Path, "/")
	conversationID, err := strconv.Atoi(parts[3])
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid id"})
		return
	}

	messages, err := s.storage.GetMessages(ctx, conversationID)
	if err != nil {
		log.Printf("⚠️ getChatMessages error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to get messages"})
		return
	}

	// Помечаем сообщения как прочитанные при открытии чата
	s.storage.MarkMessagesRead(ctx, conversationID, claims.UserID)

	json.NewEncoder(w).Encode(messages)
}

// POST /api/conversations/{id}/messages — отправить сообщение
func (s *Server) sendMessageHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	parts := strings.Split(r.URL.Path, "/")
	conversationID, err := strconv.Atoi(parts[3])
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid id"})
		return
	}

	var req CreateMessageRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid json"})
		return
	}

	if strings.TrimSpace(req.Content) == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "message cannot be empty"})
		return
	}

	messageID, err := s.storage.CreateMessage(ctx, conversationID, claims.UserID, req.Content)
	if err != nil {
		log.Printf("⚠️ sendMessage error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to send message"})
		return
	}

	log.Printf("✅ Message %d sent to conversation %d by user %d", messageID, conversationID, claims.UserID)

	// Возвращаем созданное сообщение с данными отправителя
	messages, err := s.storage.GetMessages(ctx, conversationID)
	if err == nil {
		for _, msg := range messages {
			if msg.ID == messageID {
				w.WriteHeader(http.StatusCreated)
				json.NewEncoder(w).Encode(msg)
				return
			}
		}
	}

	// Fallback если не нашли сообщение в списке
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(Message{
		ID:             messageID,
		ConversationID: conversationID,
		SenderID:       claims.UserID,
		Content:        req.Content,
		IsRead:         false,
		CreatedAt:      time.Now(),
	})
}

// POST /api/conversations/{id}/typing — отметить что пользователь печатает
func (s *Server) markTypingHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	parts := strings.Split(r.URL.Path, "/")
	conversationID, err := strconv.Atoi(parts[3])
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid id"})
		return
	}

	// Сохраняем что пользователь печатает
	s.typingMu.Lock()
	if s.typingUsers[conversationID] == nil {
		s.typingUsers[conversationID] = make(map[int]time.Time)
	}
	s.typingUsers[conversationID][claims.UserID] = time.Now()
	s.typingMu.Unlock()

	json.NewEncoder(w).Encode(map[string]bool{"ok": true})
}

// GET /api/conversations/{id}/typing — получить кто печатает
func (s *Server) getTypingUsersHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	parts := strings.Split(r.URL.Path, "/")
	conversationID, err := strconv.Atoi(parts[3])
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid id"})
		return
	}

	// Чистим старые записи (старше 5 секунд)
	s.typingMu.Lock()
	now := time.Now()
	typingList := []int{}

	if users, exists := s.typingUsers[conversationID]; exists {
		for userID, lastSeen := range users {
			// Удаляем старые записи
			if now.Sub(lastSeen) > 5*time.Second {
				delete(users, userID)
				continue
			}
			// Не включаем текущего пользователя
			if userID != claims.UserID {
				typingList = append(typingList, userID)
			}
		}

		// Удаляем пустые conversation entries
		if len(users) == 0 {
			delete(s.typingUsers, conversationID)
		}
	}
	s.typingMu.Unlock()

	json.NewEncoder(w).Encode(map[string][]int{"typing_users": typingList})
}
