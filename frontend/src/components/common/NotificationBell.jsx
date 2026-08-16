import { useState, useEffect, useRef } from 'react'
import axios from 'axios'
import './NotificationBell.css'

function NotificationBell() {
    const [notifications, setNotifications] = useState([])
    const [unreadCount, setUnreadCount] = useState(0)
    const [isOpen, setIsOpen] = useState(false)
    const [loading, setLoading] = useState(false)
    const bellRef = useRef(null)

    // Загрузка счётчика
    const loadUnreadCount = async () => {
        try {
            const res = await axios.get('/api/notifications/unread-count')
            setUnreadCount(res.data.count || 0)
        } catch (err) {
            console.error('Ошибка загрузки уведомлений:', err)
        }
    }

    // Загрузка списка уведомлений
    const loadNotifications = async () => {
        setLoading(true)
        try {
            const res = await axios.get('/api/notifications')
            setNotifications(Array.isArray(res.data) ? res.data : [])
        } catch (err) {
            console.error('Ошибка загрузки списка:', err)
        } finally {
            setLoading(false)
        }
    }

    // Polling каждые 30 секунд
    useEffect(() => {
        loadUnreadCount()
        const interval = setInterval(loadUnreadCount, 30000)
        return () => clearInterval(interval)
    }, [])

    // Закрытие при клике снаружи
    useEffect(() => {
        const handleClickOutside = (e) => {
            if (bellRef.current && !bellRef.current.contains(e.target)) {
                setIsOpen(false)
            }
        }
        document.addEventListener('mousedown', handleClickOutside)
        return () => document.removeEventListener('mousedown', handleClickOutside)
    }, [])

    const handleBellClick = () => {
        if (!isOpen) {
            loadNotifications()
            // Помечаем как прочитанные при открытии
            axios.post('/api/notifications/mark-read').catch(() => { })
        }
        setIsOpen(!isOpen)
        // Сбрасываем счётчик локально
        setTimeout(() => setUnreadCount(0), 300)
    }

    const formatTime = (dateString) => {
        const date = new Date(dateString)
        const now = new Date()
        const diff = Math.floor((now - date) / 1000)

        if (diff < 60) return 'только что'
        if (diff < 3600) return `${Math.floor(diff / 60)} мин назад`
        if (diff < 86400) return `${Math.floor(diff / 3600)} ч назад`
        return date.toLocaleDateString('ru-RU', { day: 'numeric', month: 'short' })
    }

    const getNotificationIcon = (type) => {
        switch (type) {
            case 'new_application':
                return (
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" />
                    </svg>
                )
            case 'resume_viewed':
                return (
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                        <circle cx="12" cy="12" r="3" />
                    </svg>
                )
            case 'application_viewed':
                return (
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                        <circle cx="12" cy="12" r="3" />
                    </svg>
                )
            case 'application_accepted':
                return (
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <polyline points="20 6 9 17 4 12" />
                    </svg>
                )
            case 'application_rejected':
                return (
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <line x1="18" y1="6" x2="6" y2="18" />
                        <line x1="6" y1="6" x2="18" y2="18" />
                    </svg>
                )
            default:
                return (
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
                        <path d="M13.73 21a2 2 0 0 1-3.46 0" />
                    </svg>
                )
        }
    }

    return (
        <div className="notification-bell-wrapper" ref={bellRef}>
            <button
                className={`notification-bell ${unreadCount > 0 ? 'has-unread' : ''}`}
                onClick={handleBellClick}
                aria-label="Уведомления"
            >
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
                    <path d="M13.73 21a2 2 0 0 1-3.46 0" />
                </svg>
                {unreadCount > 0 && (
                    <span className="notification-badge">
                        {unreadCount > 99 ? '99+' : unreadCount}
                    </span>
                )}
            </button>

            {isOpen && (
                <div className="notification-dropdown">
                    <div className="notification-dropdown-header">
                        <h3>Уведомления</h3>
                    </div>

                    {loading ? (
                        <div className="notification-loading">
                            <div className="spinner-small"></div>
                        </div>
                    ) : notifications.length === 0 ? (
                        <div className="notification-empty">
                            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
                                <path d="M13.73 21a2 2 0 0 1-3.46 0" />
                            </svg>
                            <p>Нет уведомлений</p>
                        </div>
                    ) : (
                        <div className="notification-list">
                            {notifications.map(notif => (
                                <div
                                    key={notif.id}
                                    className={`notification-item ${!notif.is_read ? 'unread' : ''}`}
                                >
                                    <div className={`notification-icon notification-icon-${notif.type}`}>
                                        {getNotificationIcon(notif.type)}
                                    </div>
                                    <div className="notification-content">
                                        <div className="notification-title">{notif.title}</div>
                                        <div className="notification-message">{notif.message}</div>
                                        <div className="notification-time">{formatTime(notif.created_at)}</div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            )}
        </div>
    )
}

export default NotificationBell