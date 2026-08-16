import { useState, useEffect } from 'react'
import axios from 'axios'
import './Chat.css'

function ChatList({ currentUser, onOpenChat, onOpenVacancy }) {
    const [conversations, setConversations] = useState([])
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        const loadConversations = async () => {
            try {
                setLoading(true)
                const res = await axios.get('/api/conversations')
                setConversations(Array.isArray(res.data) ? res.data : [])
            } catch (err) {
                console.error('Ошибка загрузки чатов:', err)
            } finally {
                setLoading(false)
            }
        }

        loadConversations()
        const interval = setInterval(loadConversations, 10000)
        return () => clearInterval(interval)
    }, [])

    const formatTime = (dateString) => {
        if (!dateString) return ''
        const date = new Date(dateString)
        const now = new Date()

        if (date.toDateString() === now.toDateString()) {
            return date.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })
        }

        const yesterday = new Date(now)
        yesterday.setDate(yesterday.getDate() - 1)
        if (date.toDateString() === yesterday.toDateString()) {
            return 'Вчера'
        }

        return date.toLocaleDateString('ru-RU', { day: 'numeric', month: 'short' })
    }

    if (loading) {
        return (
            <div className="chat-list-container">
                <div className="loading">
                    <div className="spinner"></div>
                    <p>Загрузка чатов...</p>
                </div>
            </div>
        )
    }

    return (
        <div className="chat-list-container">
            <div className="chat-list-header">
                <h1>Чаты</h1>
                <p className="chat-list-subtitle">
                    {currentUser.role === 'employer' || currentUser.role === 'admin'
                        ? 'Переписка с кандидатами'
                        : 'Переписка с работодателями'}
                </p>
            </div>

            {conversations.length === 0 ? (
                <div className="chat-list-empty">
                    <div className="chat-list-empty-icon">💬</div>
                    <h3>Чатов пока нет</h3>
                    <p>
                        {currentUser.role === 'employer' || currentUser.role === 'admin'
                            ? 'Когда кандидаты откликнутся на ваши вакансии, здесь появится переписка'
                            : 'Когда вы откликнетесь на вакансию, здесь появится переписка'}
                    </p>
                </div>
            ) : (
                <div className="chat-list">
                    {conversations.map(chat => {
                        const isEmployer = currentUser.role === 'employer' || currentUser.role === 'admin'
                        const displayName = isEmployer
                            ? chat.candidate_name
                            : chat.company_name

                        return (
                            <div
                                key={chat.conversation_id}
                                className="chat-list-item"
                                onClick={() => onOpenChat(chat)}
                            >
                                <div className="chat-list-avatar">
                                    {displayName?.[0]?.toUpperCase() || '?'}
                                </div>

                                <div className="chat-list-info">
                                    <h3 className="chat-list-title">{displayName}</h3>
                                    <p className="chat-list-preview">
                                        <span className="chat-list-vacancy">{chat.vacancy_title}</span>
                                        <span className="chat-list-sep">•</span>
                                        <span className="chat-list-message">
                                            {chat.last_message || 'Нет сообщений'}
                                        </span>
                                    </p>
                                </div>

                                <div className="chat-list-meta">
                                    <span className="chat-list-time">
                                        {formatTime(chat.last_message_at)}
                                    </span>
                                    {chat.unread_count > 0 && (
                                        <span className="chat-list-unread">{chat.unread_count}</span>
                                    )}
                                </div>
                            </div>
                        )
                    })}
                </div>
            )}
        </div>
    )
}

export default ChatList