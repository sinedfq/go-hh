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

func (s *PostgresStorage) GetUserConversations(ctx context.Context, userID int) ([]ChatInfo, error) {
	query := `
        SELECT
            c.id AS conversation_id,
            c.application_id,
            a.vacancy_id,
            v.title AS vacancy_title,
            v.company AS company_name,
            COALESCE(v.is_archived, FALSE) AS vacancy_archived,
            COALESCE(a.cover_letter, '') AS cover_letter,
            CASE
                WHEN a.candidate_user_id = $1 THEN 'candidate'
                ELSE 'employer'
            END AS user_role,
            a.candidate_user_id,
            COALESCE(u_candidate.email, '') AS candidate_email,
            COALESCE(u_candidate.photo_url, '') AS candidate_photo,
            COALESCE(r.full_name, u_candidate.email) AS candidate_name,
            COALESCE((
                SELECT content FROM messages
                WHERE conversation_id = c.id
                ORDER BY created_at DESC LIMIT 1
            ), '') AS last_message,
            (
                SELECT created_at FROM messages
                WHERE conversation_id = c.id
                ORDER BY created_at DESC LIMIT 1
            ) AS last_message_at,
            COALESCE((
                SELECT COUNT(*) FROM messages
                WHERE conversation_id = c.id AND sender_id != $1 AND is_read = false
            ), 0) AS unread_count
        FROM conversations c
        JOIN applications a ON a.id = c.application_id
        JOIN vacancies v ON v.id = a.vacancy_id
        JOIN users u_candidate ON u_candidate.id = a.candidate_user_id
        LEFT JOIN resumes r ON r.id = a.resume_id
        WHERE a.candidate_user_id = $1
           OR v.company_id IN (SELECT company_id FROM users WHERE id = $1 AND role IN ('employer','admin') AND company_id IS NOT NULL)
        ORDER BY last_message_at DESC NULLS LAST, c.created_at DESC
    `
	rows, err := s.pool.Query(ctx, query, userID)
	if err != nil {
		log.Printf("⚠️ GetUserConversations query error: %v", err)
		return nil, err
	}
	defer rows.Close()

	var chats []ChatInfo
	for rows.Next() {
		var chat ChatInfo
		var userRole string
		var isArchived bool
		err := rows.Scan(
			&chat.ConversationID, &chat.ApplicationID, &chat.VacancyID,
			&chat.VacancyTitle, &chat.CompanyName, &isArchived, &chat.CoverLetter,
			&userRole, &chat.CandidateID, &chat.CandidateEmail,
			&chat.CandidatePhoto, &chat.CandidateName,
			&chat.LastMessage, &chat.LastMessageAt, &chat.UnreadCount,
		)
		if err != nil {
			log.Printf("⚠️ GetUserConversations scan error: %v", err)
			return nil, err
		}
		chat.VacancyArchived = isArchived
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
		WHERE (a.candidate_user_id = $1 OR v.company_id IN (SELECT company_id FROM users WHERE id = $1 AND role IN ('employer','admin') AND company_id IS NOT NULL))
		  AND m.sender_id != $1
		  AND m.is_read = FALSE
	`, userID).Scan(&count)

	if err != nil {
		return 0, err
	}
	return count, nil
}
