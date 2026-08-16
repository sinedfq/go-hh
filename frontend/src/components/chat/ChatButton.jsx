import { useState, useEffect } from 'react'
import axios from 'axios'
import './Chat.css'

function ChatButton({ currentUser, onOpenChatApp, onCloseChatApp, isChatOpen }) {
    const [unreadTotal, setUnreadTotal] = useState(0)
    const [isMobile, setIsMobile] = useState(false)

    // ====== ДЕТЕКТ МОБИЛЬНОГО ======
    useEffect(() => {
        const checkMobile = () => setIsMobile(window.innerWidth <= 768)
        checkMobile()
        window.addEventListener('resize', checkMobile)
        return () => window.removeEventListener('resize', checkMobile)
    }, [])

    // ====== ЗАГРУЗКА НЕПРОЧИТАННЫХ ======
    useEffect(() => {
        const loadUnread = async () => {
            try {
                const res = await axios.get('/api/conversations')
                const chats = Array.isArray(res.data) ? res.data : []
                setUnreadTotal(chats.reduce((sum, c) => sum + (c.unread_count || 0), 0))
            } catch (err) {
                console.error('Ошибка загрузки чатов:', err)
            }
        }
        loadUnread()
        const interval = setInterval(loadUnread, 5000)
        return () => clearInterval(interval)
    }, [])

    if (!currentUser) return null

    // На мобильном скрываем FAB когда чат открыт
    if (isMobile && isChatOpen) return null

    // ====== КЛИК: закрываем если открыт, открываем если закрыт ======
    const handleClick = () => {
        if (isChatOpen) {
            onCloseChatApp?.()
        } else {
            onOpenChatApp?.()
        }
    }

    return (
        <button
            className={`chat-fab ${isChatOpen ? 'active' : ''} ${unreadTotal > 0 && !isChatOpen ? 'has-unread' : ''}`}
            onClick={handleClick}
            title={isChatOpen ? 'Закрыть чаты' : 'Открыть чаты'}
        >
            {isChatOpen ? (
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                    <line x1="18" y1="6" x2="6" y2="18" />
                    <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
            ) : (
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                </svg>
            )}
            {unreadTotal > 0 && !isChatOpen && (
                <span className="chat-fab-badge">{unreadTotal > 99 ? '99+' : unreadTotal}</span>
            )}
        </button>
    )
}

export default ChatButton