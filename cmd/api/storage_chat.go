package main

import (
	"context"
	"errors"
	"log"

	"github.com/jackc/pgx/v5"
)

// Создать чат при отклике
func (s *PostgresStorage) CreateConversation(ctx context.Context, applicationID int) (int, error) {
	var id int
	err := s.pool.QueryRow(ctx, `
		INSERT INTO conversations (application_id) VALUES ($1)
		ON CONFLICT (application_id) DO UPDATE SET application_id = EXCLUDED.application_id
		RETURNING id
	`, applicationID).Scan(&id)

	if err != nil {
		log.Printf("⚠️ CreateConversation error: %v", err)
	}
	return id, err
}

// Получить чат по application_id
func (s *PostgresStorage) GetConversationByApplicationID(ctx context.Context, applicationID int) (*Conversation, error) {
	var c Conversation
	err := s.pool.QueryRow(ctx, `
		SELECT id, application_id, created_at
		FROM conversations WHERE application_id = $1
	`, applicationID).Scan(&c.ID, &c.ApplicationID, &c.CreatedAt)

	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &c, nil
}

// Отправить сообщение
func (s *PostgresStorage) CreateMessage(ctx context.Context, conversationID, senderID int, content string) (int, error) {
	var id int
	err := s.pool.QueryRow(ctx, `
		INSERT INTO messages (conversation_id, sender_id, content)
		VALUES ($1, $2, $3)
		RETURNING id
	`, conversationID, senderID, content).Scan(&id)

	if err != nil {
		log.Printf("⚠️ CreateMessage error: %v", err)
	}
	return id, err
}

// Получить все сообщения чата с данными отправителей
func (s *PostgresStorage) GetMessages(ctx context.Context, conversationID int) ([]Message, error) {
	query := `
		SELECT 
			m.id, m.conversation_id, m.sender_id, m.content, 
			m.is_read, m.read_at, m.created_at,
			u.email as sender_name,
			COALESCE(u.photo_url, '') as sender_photo,
			u.role as sender_role
		FROM messages m
		JOIN users u ON u.id = m.sender_id
		WHERE m.conversation_id = $1
		ORDER BY m.created_at ASC
	`

	rows, err := s.pool.Query(ctx, query, conversationID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var messages []Message
	for rows.Next() {
		var msg Message
		err := rows.Scan(
			&msg.ID, &msg.ConversationID, &msg.SenderID, &msg.Content,
			&msg.IsRead, &msg.ReadAt, &msg.CreatedAt,
			&msg.SenderName, &msg.SenderPhoto, &msg.SenderRole,
		)
		if err != nil {
			return nil, err
		}
		messages = append(messages, msg)
	}

	if messages == nil {
		messages = []Message{}
	}

	return messages, rows.Err()
}

// Пометить сообщения как прочитанные
func (s *PostgresStorage) MarkMessagesRead(ctx context.Context, conversationID, userID int) error {
	result, err := s.pool.Exec(ctx, `
		UPDATE messages 
		SET is_read = TRUE, read_at = NOW()
		WHERE conversation_id = $1 
		  AND sender_id != $2 
		  AND is_read = FALSE
	`, conversationID, userID)

	if err != nil {
		return err
	}

	if result.RowsAffected() > 0 {
		log.Printf("✅ Marked %d messages as read in conversation %d", result.RowsAffected(), conversationID)
	}

	return nil
}

// Получить все чаты пользователя (кандидат или работодатель)
func (s *PostgresStorage) GetUserConversations(ctx context.Context, userID int) ([]ChatInfo, error) {
	query := `
		SELECT 
			c.id as conversation_id,
			c.application_id,
			COALESCE(comp.name, '') as company_name,
			v.title as vacancy_title,
			v.id as vacancy_id,
			u.email as candidate_name,
			u.id as candidate_id,
			u.email as candidate_email,
			COALESCE(u.photo_url, '') as candidate_photo,
			COALESCE((SELECT content FROM messages WHERE conversation_id = c.id ORDER BY created_at DESC LIMIT 1), '') as last_message,
			(SELECT created_at FROM messages WHERE conversation_id = c.id ORDER BY created_at DESC LIMIT 1) as last_message_at,
			(SELECT COUNT(*) FROM messages WHERE conversation_id = c.id AND sender_id != $1 AND is_read = FALSE) as unread_count
		FROM conversations c
		JOIN applications a ON a.id = c.application_id
		JOIN vacancies v ON v.id = a.vacancy_id
		LEFT JOIN companies comp ON comp.id = v.company_id
		JOIN users u ON u.id = a.candidate_user_id
		WHERE a.candidate_user_id = $1 OR v.author_user_id = $1
		ORDER BY last_message_at DESC NULLS LAST, c.created_at DESC
	`

	rows, err := s.pool.Query(ctx, query, userID)
	if err != nil {
		log.Printf("⚠️ GetUserConversations error: %v", err)
		return nil, err
	}
	defer rows.Close()

	var chats []ChatInfo
	for rows.Next() {
		var chat ChatInfo
		err := rows.Scan(
			&chat.ConversationID,
			&chat.ApplicationID,
			&chat.CompanyName,
			&chat.VacancyTitle,
			&chat.VacancyID,
			&chat.CandidateName,
			&chat.CandidateID,
			&chat.CandidateEmail,
			&chat.CandidatePhoto,
			&chat.LastMessage,
			&chat.LastMessageAt,
			&chat.UnreadCount,
		)
		if err != nil {
			log.Printf("⚠️ GetUserConversations scan error: %v", err)
			return nil, err
		}
		chats = append(chats, chat)
	}

	if chats == nil {
		chats = []ChatInfo{}
	}

	return chats, rows.Err()
}

// Получить количество непрочитанных сообщений пользователя
func (s *PostgresStorage) GetUnreadMessagesCount(ctx context.Context, userID int) (int, error) {
	var count int
	err := s.pool.QueryRow(ctx, `
		SELECT COUNT(*)
		FROM messages m
		JOIN conversations c ON c.id = m.conversation_id
		JOIN applications a ON a.id = c.application_id
		JOIN vacancies v ON v.id = a.vacancy_id
		WHERE (a.candidate_user_id = $1 OR v.author_user_id = $1)
		  AND m.sender_id != $1
		  AND m.is_read = FALSE
	`, userID).Scan(&count)

	if err != nil {
		return 0, err
	}
	return count, nil
}
