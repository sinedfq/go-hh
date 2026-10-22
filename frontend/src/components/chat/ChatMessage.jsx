import { useState } from 'react'

function ChatMessage({ message, currentUser }) {
    const isMine = message.sender_id === currentUser?.id
    const [failedPhoto,setFailedPhoto]=useState(null)
    const photo=message.sender_photo||(isMine?currentUser?.photo_url:'')
    const name=message.sender_name||(isMine?currentUser?.email:'Собеседник')

    const formatTime = (dateString) => {
        const date = new Date(dateString)
        return date.toLocaleTimeString('ru-RU', {
            hour: '2-digit',
            minute: '2-digit'
        })
    }

    const formatReadTime = (dateString) => {
        if (!dateString) return 'Прочитано'
        const date = new Date(dateString)
        return `Прочитано ${date.toLocaleTimeString('ru-RU', {
            hour: '2-digit',
            minute: '2-digit'
        })}`
    }

    const getInitial = (name) => {
        return (name || '?')[0].toUpperCase()
    }

    return (
        <div className={`chat-message ${isMine ? 'mine' : ''}`}>
                <div className="chat-message-avatar" aria-label={`Аватар: ${name}`}>
                    {photo && failedPhoto!==photo ? (
                        <img src={photo} alt="" onError={()=>setFailedPhoto(photo)} />
                    ) : (
                        getInitial(name)
                    )}
                </div>

            <div className="chat-message-content">
                <div className="chat-message-bubble">
                    <div className="chat-sender-name">{isMine?'Вы':name}</div>
                    <div className="chat-message-text">{message.content}</div>
                </div>

                <div className="chat-message-meta">
                    <span className="chat-message-time">{formatTime(message.created_at)}</span>

                    {isMine && (
                        <span className={`message-status ${message.is_read ? 'read' : 'sent'}`}>
                            <span className="checkmarks">
                                {message.is_read ? '✓✓' : '✓'}
                            </span>
                            {message.is_read && message.read_at && (
                                <span className="message-status-tooltip">
                                    {formatReadTime(message.read_at)}
                                </span>
                            )}
                        </span>
                    )}
                </div>
            </div>
        </div>
    )
}

export default ChatMessage
