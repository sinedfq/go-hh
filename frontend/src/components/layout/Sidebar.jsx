import { useEffect, useRef, useState } from 'react'
import ChatButton from '../chat/ChatButton'

const paths = {
    home: 'm3 10 9-7 9 7v11h-6v-8H9v8H3Z',
    swipe: 'M8 5H5v14h11v-3M8 2h11v14H8ZM11 8h5m-2-2 2 2-2 2',
    search: 'M21 21l-5-5M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0',
    work: 'M3 7h18v14H3ZM8 7V3h8v4M3 12h18M10 12v3h4v-3',
    company: 'M3 21V7h11v14M14 11h7v10M7 11h3m-3 4h3m-3 4h3M2 21h20',
    applications: 'M8 4H4v18h16V4h-4M8 2h8v5H8Zm0 10h8m-8 5h8',
    more: 'M5 12h.01M12 12h.01M19 12h.01',
}
function Icon({ name }) {
    return <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d={paths[name]} /></svg>
}
function Sidebar({ mode, onModeChange, user, onLogout, onLogin, onOpenChat, onCloseChat, isChatOpen }) {
    const [showMore, setShowMore] = useState(false)
    const dialog = useRef(null)
    const employer = user && ['employer', 'admin'].includes(user.role)
    const candidate = !user || ['candidate', 'admin'].includes(user.role)
    const primary = employer
        ? [['home', 'Главная', 'home'], ['my-company', 'Компания', 'company'], ['my-vacancies', 'Вакансии', 'work'], ['applications', 'Отклики', 'applications']]
        : [['home', 'Главная', 'home'], ['swipe', 'Свайпы', 'swipe'], ['search', 'Поиск', 'search'], ['my-applications', 'Отклики', 'applications']]
    const extra = [
        ['browse', 'Карта вакансий'],
        ...(candidate ? [['favorites', 'Избранное'], ['saved-searches', 'Мои поиски'], ['recommendations', 'Рекомендации'], ['companies', 'Компании']] : []),
        ...(employer ? [['create-vacancy', 'Создать вакансию'], ['stats', 'Аналитика']] : []),
        ...(employer && candidate ? [['search', 'Поиск'], ['swipe', 'Свайпы'], ['my-applications', 'Мои отклики']] : []),
    ]
    useEffect(() => {
        if (showMore && !dialog.current.open) dialog.current.showModal()
        else if (!showMore && dialog.current.open) dialog.current.close()
    }, [showMore])
    const navigate = (destination) => {
        setShowMore(false)
        onCloseChat()
        if (!user && !['home', 'search', 'browse', 'companies'].includes(destination)) onLogin()
        else onModeChange(destination)
    }
    return <>
        <div className="bottom-navigation">
            <nav className="glass-nav" aria-label="Основная навигация">
                {primary.map(([destination, label, icon], index) => <button
                    key={destination} type="button" className={`glass-tab ${index === 2 ? 'glass-tab-primary' : ''}`}
                    aria-current={!isChatOpen && mode === destination ? 'page' : undefined}
                    aria-label={label} title={label} onClick={() => navigate(destination)}>
                    <span className="glass-tab-icon"><Icon name={icon} /></span>
                    {index !== 2 && <span className="glass-tab-label">{label}</span>}
                </button>)}
                {user ? <ChatButton currentUser={user} onOpenChatApp={onOpenChat} onCloseChatApp={onCloseChat} isChatOpen={isChatOpen} navigation />
                    : <button type="button" className="glass-tab" onClick={onLogin}><svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2Z" /></svg><span className="glass-tab-label">Чаты</span></button>}
            </nav>
            <button type="button" className="glass-more" aria-label="Ещё разделы" aria-haspopup="dialog" aria-expanded={showMore} onClick={() => setShowMore(true)}><Icon name="more" /></button>
        </div>
        <dialog ref={dialog} className="navigation-dialog" aria-labelledby="navigation-title" onClose={() => setShowMore(false)} onClick={event => { if (event.target === event.currentTarget) setShowMore(false) }}>
            <header><h2 id="navigation-title">Разделы</h2><button type="button" className="navigation-close" aria-label="Закрыть меню" onClick={() => setShowMore(false)}>×</button></header>
            <div className="navigation-links">
                {extra.map(([destination, label]) => <button type="button" key={destination} aria-current={mode === destination ? 'page' : undefined} onClick={() => navigate(destination)}>{label}</button>)}
                {user ? <>
                    <button type="button" onClick={() => navigate('profile')}>Профиль · {user.name || user.email}</button>
                    <button type="button" className="navigation-logout" onClick={() => { setShowMore(false); onCloseChat(); onLogout() }}>Выйти</button>
                </> : <button type="button" onClick={() => { setShowMore(false); onLogin() }}>Войти в аккаунт</button>}
            </div>
        </dialog>
    </>
}
export default Sidebar
