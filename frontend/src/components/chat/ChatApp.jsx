import { useState, useEffect, useRef } from 'react'
import axios from 'axios'
import ChatMessage from './ChatMessage'
import './Chat.css'

function ChatApp({ currentUser, onClose, onOpenVacancy, onCancelApplication, initialChat = null }) {
    const [conversations, setConversations] = useState([])
    const [activeChat, setActiveChat] = useState(null)
    const [messages, setMessages] = useState([])
    const [loadingChats, setLoadingChats] = useState(true)
    const [loadingMessages, setLoadingMessages] = useState(false)
    const [inputValue, setInputValue] = useState('')
    const [sending, setSending] = useState(false)
    const [showMenu, setShowMenu] = useState(false)
    const [showParticipants, setShowParticipants] = useState(false)
    const [otherUserTyping, setOtherUserTyping] = useState(false)  // ← ДОБАВЬ ЭТУ СТРОКУ

    const messagesEndRef = useRef(null)
    const menuRef = useRef(null)
    const inputRef = useRef(null)

    const isEmployer = currentUser?.role === 'employer' || currentUser?.role === 'admin'

    // ====== ЗАГРУЗКА СПИСКА ЧАТОВ ======
    const loadConversations = async () => {
        try {
            const res = await axios.get('/api/conversations')
            setConversations(Array.isArray(res.data) ? res.data : [])
        } catch (err) {
            console.error('Ошибка загрузки чатов:', err)
        } finally {
            setLoadingChats(false)
        }
    }

    useEffect(() => {
        loadConversations()
        const interval = setInterval(loadConversations, 8000)
        return () => clearInterval(interval)
    }, [])

    // ====== ЗАГРУЗКА СООБЩЕНИЙ АКТИВНОГО ЧАТА ======
    useEffect(() => {
        if (!activeChat?.conversation_id) {
            setMessages([])
            return
        }

        const loadMessages = async () => {
            try {
                setLoadingMessages(true)
                const res = await axios.get(`/api/conversations/${activeChat.conversation_id}/messages`)
                setMessages(Array.isArray(res.data) ? res.data : [])
            } catch (err) {
                console.error('Ошибка загрузки сообщений:', err)
            } finally {
                setLoadingMessages(false)
            }
        }
        loadMessages()
    }, [activeChat?.conversation_id])

    // ====== POLLING СООБЩЕНИЙ ======
    useEffect(() => {
        if (!activeChat?.conversation_id) return

        const interval = setInterval(async () => {
            try {
                const res = await axios.get(`/api/conversations/${activeChat.conversation_id}/messages`)
                const newMessages = Array.isArray(res.data) ? res.data : []

                setMessages(prev => {
                    if (newMessages.length !== prev.length) return newMessages
                    const hasChanges = newMessages.some((msg, i) =>
                        prev[i]?.is_read !== msg.is_read || prev[i]?.read_at !== msg.read_at
                    )
                    return hasChanges ? newMessages : prev
                })
            } catch (err) {
                console.error('Ошибка polling:', err)
            }
        }, 3000)
        return () => clearInterval(interval)
    }, [activeChat?.conversation_id])

    // ====== POLLING: кто печатает ======
    useEffect(() => {
        if (!activeChat?.conversation_id) {
            setOtherUserTyping(false)
            return
        }

        const pollTyping = async () => {
            try {
                const res = await axios.get(`/api/conversations/${activeChat.conversation_id}/typing`)
                const typingUsers = res.data.typing_users || []
                setOtherUserTyping(typingUsers.length > 0)
            } catch (err) {
                // Тихо игнорируем
            }
        }

        pollTyping()
        const interval = setInterval(pollTyping, 2000)

        return () => clearInterval(interval)
    }, [activeChat?.conversation_id])

    // ====== АВТОСКРОЛЛ ======
    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
    }, [messages])

    // ====== БЛОКИРОВКА СКРОЛЛА ФОНА ======
    useEffect(() => {
        document.body.style.overflow = 'hidden'
        return () => { document.body.style.overflow = '' }
    }, [])

    // ====== ЗАКРЫТИЕ ПО ESC ======
    useEffect(() => {
        const handleEsc = (e) => {
            if (e.key === 'Escape') {
                if (showParticipants) {
                    setShowParticipants(false)
                } else if (showMenu) {
                    setShowMenu(false)
                } else if (activeChat && window.innerWidth <= 768) {
                    setActiveChat(null) // На мобильном — назад к списку
                } else {
                    onClose()
                }
            }
        }
        window.addEventListener('keydown', handleEsc)
        return () => window.removeEventListener('keydown', handleEsc)
    }, [onClose, showParticipants, showMenu, activeChat])

    // ====== ЗАКРЫТИЕ МЕНЮ ВНЕ ======
    useEffect(() => {
        const handleClickOutside = (e) => {
            if (menuRef.current && !menuRef.current.contains(e.target)) {
                setShowMenu(false)
            }
        }
        if (showMenu) {
            document.addEventListener('mousedown', handleClickOutside)
        }
        return () => document.removeEventListener('mousedown', handleClickOutside)
    }, [showMenu])

    // ====== ОТПРАВКА СООБЩЕНИЯ ======
    const handleSend = async () => {
        const content = inputValue.trim()
        if (!content || sending || !activeChat) return

        setSending(true)
        try {
            const res = await axios.post(
                `/api/conversations/${activeChat.conversation_id}/messages`,
                { content }
            )
            setMessages(prev => [...prev, res.data])
            setInputValue('')
            inputRef.current?.focus()
            // Обновляем список (последнее сообщение)
            loadConversations()
        } catch (err) {
            console.error('Ошибка отправки:', err)
            alert('Не удалось отправить сообщение')
        } finally {
            setSending(false)
        }
    }

    // ====== ОТПРАВКА СИГНАЛА "ПЕЧАТАЕТ" ======
    const sendTypingSignal = async () => {
        if (!activeChat?.conversation_id) return
        try {
            await axios.post(`/api/conversations/${activeChat.conversation_id}/typing`)
        } catch (err) {
            // Тихо игнорируем
        }
    }

    useEffect(() => {
        if (initialChat && !activeChat) {
            setActiveChat(initialChat)
        }
    }, [initialChat])

    // ====== ОБРАБОТКА ВВОДА ======
    const handleInputChange = (e) => {
        setInputValue(e.target.value)

        // Отправляем сигнал когда пользователь печатает
        if (e.target.value.length > 0) {
            sendTypingSignal()
        }
    }

    const handleKeyDown = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault()
            handleSend()
        }
    }

    // ====== ВЫБОР ЧАТА ======
    const handleSelectChat = (chat) => {
        setActiveChat(chat)
        setShowMenu(false)
    }

    // ====== НАЗАД К СПИСКУ (мобильный) ======
    const handleBackToList = () => {
        setActiveChat(null)
    }

    // ====== ОТМЕНА ОТКЛИКА ======
    const handleCancelApplication = () => {
        if (!activeChat) return
        if (window.confirm('Отменить отклик? Чат будет удалён.')) {
            if (onCancelApplication) {
                onCancelApplication(activeChat.application_id)
                onClose()
            }
        }
    }

    // ====== ФОРМАТИРОВАНИЕ ВРЕМЕНИ ======
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

    // ====== ИМЕНА В ШАПКЕ ======
    const headerName = activeChat
        ? (isEmployer ? (activeChat.candidate_name || 'Кандидат') : (activeChat.company_name || 'Работодатель'))
        : ''
    const headerSubtitle = isEmployer ? 'Кандидат' : 'Работодатель'

    return (
        <>
            <div className="chat-app-overlay" onClick={onClose}>
                <div className="chat-app-container" onClick={(e) => e.stopPropagation()}>
                    {/* ====== ЛЕВАЯ ПАНЕЛЬ: СПИСОК ЧАТОВ ====== */}
                    <div className={`chat-app-sidebar ${activeChat ? 'hidden-mobile' : ''}`}>
                        <div className="chat-app-sidebar-header">
                            <h2>Чаты</h2>
                            <button
                                className="chat-app-close-btn"
                                onClick={onClose}
                                title="Закрыть чаты"
                            >
                                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                                    <line x1="18" y1="6" x2="6" y2="18" />
                                    <line x1="6" y1="6" x2="18" y2="18" />
                                </svg>
                            </button>
                        </div>

                        <div className="chat-app-sidebar-list">
                            {loadingChats ? (
                                <div className="chat-app-loading">
                                    <div className="spinner"></div>
                                </div>
                            ) : conversations.length === 0 ? (
                                <div className="chat-app-empty">
                                    <div className="chat-app-empty-icon">💬</div>
                                    <h3>Чатов пока нет</h3>
                                    <p>
                                        {isEmployer
                                            ? 'Когда кандидаты откликнутся — здесь появится переписка'
                                            : 'Откликнитесь на вакансию — и здесь появится переписка'}
                                    </p>
                                </div>
                            ) : (
                                conversations.map(chat => {
                                    const displayName = isEmployer ? chat.candidate_name : chat.company_name
                                    const isActive = activeChat?.conversation_id === chat.conversation_id

                                    return (
                                        <div
                                            key={chat.conversation_id}
                                            className={`chat-app-sidebar-item ${isActive ? 'active' : ''}`}
                                            onClick={() => handleSelectChat(chat)}
                                        >
                                            <div className="chat-app-sidebar-item-avatar">
                                                {isEmployer ? (
                                                    // Работодатель видит фото кандидата
                                                    chat.candidate_photo ? (
                                                        <img src={chat.candidate_photo} alt={displayName} />
                                                    ) : (
                                                        displayName?.[0]?.toUpperCase() || '?'
                                                    )
                                                ) : (
                                                    // Кандидат видит фото компании
                                                    chat.company_photo ? (
                                                        <img src={chat.company_photo} alt={displayName} />
                                                    ) : (
                                                        displayName?.[0]?.toUpperCase() || '?'
                                                    )
                                                )}
                                            </div>
                                            <div className="chat-app-sidebar-item-info">
                                                <div className="chat-app-sidebar-item-top">
                                                    <span className="chat-app-sidebar-item-name">{displayName}</span>
                                                    <span className="chat-app-sidebar-item-time">
                                                        {formatTime(chat.last_message_at)}
                                                    </span>
                                                </div>
                                                <div className="chat-app-sidebar-item-bottom">
                                                    <span className="chat-app-sidebar-item-preview">
                                                        {chat.last_message || chat.vacancy_title || 'Нет сообщений'}
                                                    </span>
                                                    {chat.unread_count > 0 && (
                                                        <span className="chat-app-sidebar-item-unread">
                                                            {chat.unread_count}
                                                        </span>
                                                    )}
                                                </div>
                                            </div>
                                        </div>
                                    )
                                })
                            )}
                        </div>
                    </div>

                    {/* ====== ПРАВАЯ ПАНЕЛЬ: АКТИВНЫЙ ЧАТ ====== */}
                    <div className={`chat-app-main ${!activeChat ? 'hidden-mobile' : ''}`}>
                        {activeChat ? (
                            <>
                                {/* ШАПКА */}
                                <div className="chat-app-main-header">
                                    <button
                                        className="chat-app-back-btn mobile-only"
                                        onClick={handleBackToList}
                                        title="Назад"
                                    >
                                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <polyline points="15 18 9 12 15 6" />
                                        </svg>
                                    </button>

                                    <div className="chat-app-main-header-avatar">
                                        {headerName[0]?.toUpperCase()}
                                    </div>
                                    <div className="chat-app-main-header-info">
                                        <h3>{headerName}</h3>
                                        <p>{headerSubtitle}</p>
                                    </div>

                                    <div className="chat-app-main-header-actions">
                                        <div className="chat-menu-wrapper" ref={menuRef}>
                                            <button
                                                className="chat-header-btn"
                                                onClick={() => setShowMenu(!showMenu)}
                                                title="Меню"
                                            >
                                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                    <circle cx="12" cy="12" r="1" />
                                                    <circle cx="19" cy="12" r="1" />
                                                    <circle cx="5" cy="12" r="1" />
                                                </svg>
                                            </button>

                                            {showMenu && (
                                                <div className="chat-dropdown-menu">
                                                    <button
                                                        className="chat-dropdown-item"
                                                        onClick={() => {
                                                            setShowParticipants(true)
                                                            setShowMenu(false)
                                                        }}
                                                    >
                                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                                                            <circle cx="9" cy="7" r="4" />
                                                        </svg>
                                                        Участники чата
                                                    </button>
                                                    <button
                                                        className="chat-dropdown-item"
                                                        onClick={() => {
                                                            setShowMenu(false)
                                                            onClose()
                                                            onOpenVacancy(activeChat.vacancy_id)
                                                        }}
                                                    >
                                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                            <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
                                                            <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
                                                        </svg>
                                                        Открыть вакансию
                                                    </button>
                                                    <button
                                                        className="chat-dropdown-item danger"
                                                        onClick={() => {
                                                            setShowMenu(false)
                                                            handleCancelApplication()
                                                        }}
                                                    >
                                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                            <polyline points="3 6 5 6 21 6" />
                                                            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                                                        </svg>
                                                        Отменить отклик
                                                    </button>
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                </div>

                                {/* ВАКАНСИЯ */}
                                <div
                                    className="chat-vacancy-bar"
                                    onClick={() => {
                                        onClose()
                                        onOpenVacancy(activeChat.vacancy_id)
                                    }}
                                >
                                    <div className="chat-vacancy-bar-icon">
                                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
                                            <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
                                        </svg>
                                    </div>
                                    <div className="chat-vacancy-bar-info">
                                        <h4 className="chat-vacancy-bar-title">{activeChat.vacancy_title}</h4>
                                        <p className="chat-vacancy-bar-hint">Открыть вакансию</p>
                                    </div>
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <polyline points="9 18 15 12 9 6" />
                                    </svg>
                                </div>

                                {/* СООБЩЕНИЯ */}
                                <div className="chat-messages">
                                    {loadingMessages ? (
                                        <div className="loading">
                                            <div className="spinner"></div>
                                        </div>
                                    ) : messages.length === 0 ? (
                                        <div className="chat-empty">
                                            <div className="chat-empty-icon">💬</div>
                                            <p>Сообщений пока нет</p>
                                            <p className="chat-empty-hint">Начните диалог!</p>
                                        </div>
                                    ) : (
                                        messages.map(msg => (
                                            <ChatMessage
                                                key={msg.id}
                                                message={msg}
                                                currentUser={currentUser}
                                            />
                                        ))
                                    )}
                                    <div ref={messagesEndRef} />
                                </div>

                                {/* ====== ИНДИКАТОР ПЕЧАТИ ====== */}
                                {otherUserTyping && (
                                    <div className="typing-indicator">
                                        <div className="typing-dots">
                                            <div className="typing-dot"></div>
                                            <div className="typing-dot"></div>
                                            <div className="typing-dot"></div>
                                        </div>
                                        <span>{isEmployer ? 'Кандидат печатает' : 'Работодатель печатает'}...</span>
                                    </div>
                                )}

                                {/* ПОЛЕ ВВОДА */}
                                <div className="chat-input-area">
                                    <textarea
                                        ref={inputRef}
                                        className="chat-input"
                                        value={inputValue}
                                        onChange={handleInputChange}
                                        onKeyDown={handleKeyDown}
                                        placeholder="Напишите сообщение..."
                                        rows={1}
                                    />
                                    <button
                                        className="chat-send-btn"
                                        onClick={handleSend}
                                        disabled={!inputValue.trim() || sending}
                                        title="Отправить"
                                    >
                                        {sending ? (
                                            <div className="spinner-small"></div>
                                        ) : (
                                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                <line x1="22" y1="2" x2="11" y2="13" />
                                                <polygon points="22 2 15 22 11 13 2 9 22 2" />
                                            </svg>
                                        )}
                                    </button>
                                </div>
                            </>
                        ) : (
                            <div className="chat-app-main-empty">
                                <div className="chat-app-main-empty-icon">
                                    <svg width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                                        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                                    </svg>
                                </div>
                                <h2>Выберите чат</h2>
                                <p>Выберите переписку из списка слева</p>
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {/* МОДАЛКА УЧАСТНИКОВ */}
            {showParticipants && activeChat && (
                <div className="chat-participants-modal" onClick={() => setShowParticipants(false)}>
                    <div className="chat-participants-content" onClick={(e) => e.stopPropagation()}>
                        <h3>Участники чата</h3>
                        <div className="participant-item">
                            <div className="participant-avatar">
                                {activeChat.candidate_name?.[0]?.toUpperCase() || 'К'}
                            </div>
                            <div className="participant-info">
                                <p className="participant-name">{activeChat.candidate_name}</p>
                                <p className="participant-role">Кандидат</p>
                            </div>
                        </div>
                        <div className="participant-item">
                            <div className="participant-avatar">
                                {activeChat.company_name?.[0]?.toUpperCase() || 'К'}
                            </div>
                            <div className="participant-info">
                                <p className="participant-name">{activeChat.company_name}</p>
                                <p className="participant-role">Работодатель</p>
                            </div>
                        </div>
                        <button
                            className="btn btn-primary chat-participants-close"
                            onClick={() => setShowParticipants(false)}
                        >
                            Закрыть
                        </button>
                    </div>
                </div>
            )}
        </>
    )
}

export default ChatApp