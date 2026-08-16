-- +goose Up
-- +goose StatementBegin

-- Таблица чатов (связь с откликом)
CREATE TABLE IF NOT EXISTS conversations (
    id SERIAL PRIMARY KEY,
    application_id INTEGER NOT NULL UNIQUE REFERENCES applications(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Таблица сообщений
CREATE TABLE IF NOT EXISTS messages (
    id SERIAL PRIMARY KEY,
    conversation_id INTEGER NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Индексы для быстрого поиска
CREATE INDEX IF NOT EXISTS idx_messages_conversation ON messages(conversation_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_messages_sender ON messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_conversations_application ON conversations(application_id);

-- +goose StatementEnd

-- +goose Down
-- +goose StatementBegin

DROP INDEX IF EXISTS idx_conversations_application;
DROP INDEX IF EXISTS idx_messages_sender;
DROP INDEX IF EXISTS idx_messages_conversation;
DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS conversations;

-- +goose StatementEnd