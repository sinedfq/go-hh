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
        if (date.toDateString() === yesterday.toDateString()) return 'Вчера'
        return date.toLocaleDateString('ru-RU', { day: 'numeric', month: 'short' })
    }

    const isEmployer = currentUser?.role === 'employer' || currentUser?.role === 'admin'

    if (loading) {
        return (
            <div className="chat-list-container">
                <div className="loading"><div className="spinner"></div><p>Загрузка чатов...</p></div>
            </div>
        )
    }

    return (
        <div className="chat-list-container">
            <div className="chat-list-header">
                <h1>Чаты</h1>
                <p className="chat-list-subtitle">
                    {isEmployer ? 'Переписка с кандидатами' : 'Переписка с работодателями'}
                </p>
            </div>

            {conversations.length === 0 ? (
                <div className="chat-list-empty">
                    <div className="chat-list-empty-icon">💬</div>
                    <h3>Чатов пока нет</h3>
                    <p>{isEmployer
                        ? 'Когда кандидаты откликнутся на ваши вакансии, здесь появится переписка'
                        : 'Когда вы откликнетесь на вакансию, здесь появится переписка'}</p>
                </div>
            ) : (
                <div className="chat-list">
                    {conversations.map(chat => {
                        const isArchived = chat.vacancy_archived === true

                        return (
                            <div
                                key={chat.conversation_id}
                                className={`chat-list-item ${isArchived ? 'archived' : ''}`}
                                onClick={() => onOpenChat(chat)}
                            >
                                <div className="chat-list-avatar">
                                    {isEmployer
                                        ? (chat.candidate_photo
                                            ? <img src={chat.candidate_photo} alt="" />
                                            : (chat.candidate_name?.[0]?.toUpperCase() || '?'))
                                        : (chat.company_name?.[0]?.toUpperCase() || '?')}
                                </div>

                                <div className="chat-list-info">
                                    {/* Строка 1: Название вакансии + время */}
                                    <div className="chat-list-row-1">
                                        <h3 className="chat-list-vacancy-title">{chat.vacancy_title}</h3>
                                        <span className="chat-list-time">{formatTime(chat.last_message_at)}</span>
                                    </div>

                                    {/* Строка 2: Компания */}
                                    <div className="chat-list-row-2">
                                        <span className="chat-list-company">
                                            {isEmployer ? chat.candidate_name : chat.company_name}
                                        </span>
                                    </div>

                                    {/* Строка 3: Сообщение ИЛИ "В архиве" */}
                                    <div className="chat-list-row-3">
                                        {isArchived ? (
                                            <span className="chat-list-archived-hint">
                                                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                    <polyline points="21 8 21 21 3 21 3 8" />
                                                    <rect x="1" y="3" width="22" height="5" />
                                                </svg>
                                                Вакансия в архиве
                                            </span>
                                        ) : (
                                            <span className="chat-list-message">
                                                {chat.last_message || 'Нет сообщений'}
                                            </span>
                                        )}
                                    </div>
                                </div>

                                {chat.unread_count > 0 && (
                                    <span className="chat-list-unread">{chat.unread_count}</span>
                                )}
                            </div>
                        )
                    })}
                </div>
            )}
        </div>
    )
}

export default ChatList