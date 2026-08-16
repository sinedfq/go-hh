import { useState, useEffect, useRef } from 'react'
import axios from 'axios'
import ChatMessage from './ChatMessage'
import './Chat.css'

function ChatModal({ conversation, onClose, currentUser, onOpenVacancy, onCancelApplication }) {
    const [messages, setMessages] = useState([])
    const [loading, setLoading] = useState(true)
    const [inputValue, setInputValue] = useState('')
    const [sending, setSending] = useState(false)
    const [showMenu, setShowMenu] = useState(false)
    const [showParticipants, setShowParticipants] = useState(false)
    const [isExpanded, setIsExpanded] = useState(false) // развернуть на весь экран
    const messagesEndRef = useRef(null)
    const menuRef = useRef(null)
    const inputRef = useRef(null)

    // ====== ЗАЩИТА ОТ undefined ======
    if (!conversation || !conversation.conversation_id) {
        return null
    }

    // ====== ЗАГРУЗКА СООБЩЕНИЙ ======
    useEffect(() => {
        const loadMessages = async () => {
            try {
                setLoading(true)
                const res = await axios.get(`/api/conversations/${conversation.conversation_id}/messages`)
                setMessages(Array.isArray(res.data) ? res.data : [])
            } catch (err) {
                console.error('Ошибка загрузки сообщений:', err)
            } finally {
                setLoading(false)
            }
        }
        loadMessages()
    }, [conversation.conversation_id])

    // ====== POLLING НОВЫХ СООБЩЕНИЙ ======
    useEffect(() => {
        const interval = setInterval(async () => {
            try {
                const res = await axios.get(`/api/conversations/${conversation.conversation_id}/messages`)
                const newMessages = Array.isArray(res.data) ? res.data : []

                setMessages(prev => {
                    if (newMessages.length !== prev.length) return newMessages
                    const hasChanges = newMessages.some((msg, i) =>
                        prev[i]?.is_read !== msg.is_read || prev[i]?.read_at !== msg.read_at
                    )
                    return hasChanges ? newMessages : prev
                })
            } catch (err) {
                console.error('Ошибка обновления:', err)
            }
        }, 3000)
        return () => clearInterval(interval)
    }, [conversation.conversation_id])

    // ====== АВТОСКРОЛЛ ======
    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
    }, [messages])

    // ====== ЗАКРЫТИЕ МЕНЮ ПРИ КЛИКЕ ВНЕ ======
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

    // ====== БЛОКИРОВКА СКРОЛЛА ФОНА ======
    useEffect(() => {
        document.body.style.overflow = 'hidden'
        return () => {
            document.body.style.overflow = ''
        }
    }, [])

    // ====== ЗАКРЫТИЕ ПО ESC ======
    useEffect(() => {
        const handleEsc = (e) => {
            if (e.key === 'Escape' && !showParticipants) {
                onClose()
            }
        }
        window.addEventListener('keydown', handleEsc)
        return () => window.removeEventListener('keydown', handleEsc)
    }, [onClose, showParticipants])

    // ====== ОТПРАВКА СООБЩЕНИЯ ======
    const handleSend = async () => {
        const content = inputValue.trim()
        if (!content || sending) return

        setSending(true)
        try {
            const res = await axios.post(
                `/api/conversations/${conversation.conversation_id}/messages`,
                { content }
            )
            setMessages(prev => [...prev, res.data])
            setInputValue('')
            inputRef.current?.focus()
        } catch (err) {
            console.error('Ошибка отправки:', err)
            alert('Не удалось отправить сообщение')
        } finally {
            setSending(false)
        }
    }

    // ====== ENTER ДЛЯ ОТПРАВКИ ======
    const handleKeyDown = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault()
            handleSend()
        }
    }

    // ====== ЗАКРЫТИЕ ПО КЛИКУ НА OVERLAY ======
    const handleOverlayClick = (e) => {
        if (e.target === e.currentTarget) {
            onClose()
        }
    }

    // ====== ОПРЕДЕЛЕНИЕ ИМЕН В ШАПКЕ ======
    const isEmployer = currentUser.role === 'employer' || currentUser.role === 'admin'
    const headerName = isEmployer
        ? (conversation.candidate_name || 'Кандидат')
        : (conversation.company_name || 'Работодатель')
    const headerSubtitle = isEmployer ? 'Кандидат' : 'Работодатель'

    // ====== ОТМЕНА ОТКЛИКА ======
    const handleCancelApplication = () => {
        if (window.confirm('Отменить отклик? Чат будет удалён.')) {
            if (onCancelApplication) {
                onCancelApplication(conversation.application_id)
                onClose()
            }
        }
    }

    return (
        <>
            <div
                className={`chat-fixed-overlay ${isExpanded ? 'expanded' : ''}`}
                onClick={handleOverlayClick}
            >
                <div className="chat-fixed-modal">
                    {/* ====== ШАПКА ====== */}
                    <div className="chat-header">
                        <div className="chat-header-avatar">
                            {headerName[0]?.toUpperCase()}
                        </div>
                        <div className="chat-header-info">
                            <h3 className="chat-header-title">{headerName}</h3>
                            <p className="chat-header-subtitle">{headerSubtitle}</p>
                        </div>

                        <div className="chat-header-actions">
                            {/* Развернуть/свернуть */}
                            <button
                                className="chat-header-btn desktop-only"
                                onClick={() => setIsExpanded(!isExpanded)}
                                title={isExpanded ? 'Свернуть' : 'Развернуть'}
                            >
                                {isExpanded ? (
                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <polyline points="4 14 10 14 10 20" />
                                        <polyline points="20 10 14 10 14 4" />
                                        <line x1="14" y1="10" x2="21" y2="3" />
                                        <line x1="3" y1="21" x2="10" y2="14" />
                                    </svg>
                                ) : (
                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <polyline points="15 3 21 3 21 9" />
                                        <polyline points="9 21 3 21 3 15" />
                                        <line x1="21" y1="3" x2="14" y2="10" />
                                        <line x1="3" y1="21" x2="10" y2="14" />
                                    </svg>
                                )}
                            </button>

                            {/* Меню три точки */}
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
                                                <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                                                <path d="M16 3.13a4 4 0 0 1 0 7.75" />
                                            </svg>
                                            Участники чата
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

                            {/* Закрыть */}
                            <button className="chat-header-btn" onClick={onClose} title="Закрыть">
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <line x1="18" y1="6" x2="6" y2="18" />
                                    <line x1="6" y1="6" x2="18" y2="18" />
                                </svg>
                            </button>
                        </div>
                    </div>

                    {/* ====== ПАНЕЛЬ ВАКАНСИИ ====== */}
                    <div
                        className="chat-vacancy-bar"
                        onClick={() => {
                            onClose()
                            onOpenVacancy(conversation.vacancy_id)
                        }}
                    >
                        <div className="chat-vacancy-bar-icon">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
                                <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
                            </svg>
                        </div>
                        <div className="chat-vacancy-bar-info">
                            <h4 className="chat-vacancy-bar-title">{conversation.vacancy_title}</h4>
                            <p className="chat-vacancy-bar-hint">Открыть вакансию</p>
                        </div>
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <polyline points="9 18 15 12 9 6" />
                        </svg>
                    </div>

                    {/* ====== СООБЩЕНИЯ ====== */}
                    <div className="chat-messages">
                        {loading ? (
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

                    {/* ====== ПОЛЕ ВВОДА ====== */}
                    <div className="chat-input-area">
                        <textarea
                            ref={inputRef}
                            className="chat-input"
                            value={inputValue}
                            onChange={(e) => setInputValue(e.target.value)}
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
                </div>
            </div>

            {/* ====== МОДАЛКА УЧАСТНИКОВ ====== */}
            {showParticipants && (
                <div className="chat-participants-modal" onClick={() => setShowParticipants(false)}>
                    <div className="chat-participants-content" onClick={(e) => e.stopPropagation()}>
                        <h3>Участники чата</h3>

                        <div className="participant-item">
                            <div className="participant-avatar">
                                {conversation.candidate_name?.[0]?.toUpperCase() || 'К'}
                            </div>
                            <div className="participant-info">
                                <p className="participant-name">{conversation.candidate_name}</p>
                                <p className="participant-role">Кандидат</p>
                            </div>
                        </div>

                        <div className="participant-item">
                            <div className="participant-avatar">
                                {conversation.company_name?.[0]?.toUpperCase() || 'К'}
                            </div>
                            <div className="participant-info">
                                <p className="participant-name">{conversation.company_name}</p>
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

export default ChatModal