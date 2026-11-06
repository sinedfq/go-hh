import { useEffect, useRef, useState } from 'react'
import { BrandLogo } from '../../contexts/BrandContext'
import ChatButton from '../chat/ChatButton'

const paths = {
    home: 'm3 10 9-7 9 7v11h-6v-8H9v8H3Z',
    swipe: 'M8 5H5v14h11v-3M8 2h11v14H8ZM11 8h5m-2-2 2 2-2 2',
    search: 'M21 21l-5-5M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0',
    map: 'm3 5 6-2 6 2 6-2v16l-6 2-6-2-6 2Zm6-2v16M15 5v16',
    work: 'M3 7h18v14H3ZM8 7V3h8v4M3 12h18M10 12v3h4v-3',
    company: 'M3 21V7h11v14M14 11h7v10M7 11h3m-3 4h3m-3 4h3M2 21h20',
    applications: 'M8 4H4v18h16V4h-4M8 2h8v5H8Zm0 10h8m-8 5h8',
    favorite: 'm12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2-5.6-3-5.6 3 1.1-6.2L3 9.6l6.2-.9Z',
    saved: 'M5 3h14v19l-7-4-7 4Zm4 5h6m-6 4h4',
    sparkles: 'm12 3 2.5 6.5L21 12l-6.5 2.5L12 21l-2.5-6.5L3 12l6.5-2.5Z',
    plus: 'M12 5v14M5 12h14',
    chart: 'M4 3v18h17M8 17v-5m5 5V7m5 10V4',
    profile: 'M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0',
    logout: 'M9 4H4v16h5M10 12h11m-4-4 4 4-4 4',
    chat: 'M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2Z',
    left: 'm14 6-6 6 6 6',
    right: 'm10 6 6 6-6 6',
}
function Icon({ name }) {
    return <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d={paths[name]} /></svg>
}

function Sidebar({ mode, onModeChange, user, onLogout, onLogin, onOpenChat, onCloseChat, isChatOpen }) {
    const rail = useRef(null)
    const links = useRef(null)
    const [edges, setEdges] = useState({ start: false, end: false })
    const employer = user && ['employer', 'admin'].includes(user.role)
    const candidate = !user || ['candidate', 'admin'].includes(user.role)
    const groups = [
        { label: 'Главная', items: [['home', 'Главная', 'home']] },
        ...(candidate ? [{ label: 'Поиск работы', items: [['search', 'Поиск', 'search'], ['browse', 'Карта', 'map'], ['swipe', 'Свайпы', 'swipe']] },
            { label: 'Моя карьера', items: [['favorites', 'Избранное', 'favorite'], ['saved-searches', 'Мои поиски', 'saved'], ['recommendations', 'Рекомендации', 'sparkles'], ['companies', 'Компании', 'company'], ['my-applications', 'Мои отклики', 'applications']] }]
            : [{ label: 'Карта вакансий', items: [['browse', 'Карта', 'map']] }]),
        ...(employer ? [{ label: 'Работодателю', items: [['my-company', 'Компания', 'company'], ['my-vacancies', 'Вакансии', 'work'], ['create-vacancy', 'Создать', 'plus'], ['applications', 'Отклики', 'applications'], ['stats', 'Аналитика', 'chart']] }] : []),
    ]
    useEffect(() => {
        const element = rail.current
        const update = () => {
            const overflow = links.current.scrollWidth > element.parentElement.clientWidth - 4
            const start = overflow && element.scrollLeft > 2
            const end = overflow && element.scrollWidth - element.clientWidth - element.scrollLeft > 2
            setEdges(previous => previous.start === start && previous.end === end ? previous : { start, end })
        }
        const observer = new ResizeObserver(update)
        observer.observe(element)
        observer.observe(links.current)
        element.addEventListener('scroll', update, { passive: true })
        update()
        return () => { observer.disconnect(); element.removeEventListener('scroll', update) }
    }, [user?.role, user?.id])
    useEffect(() => {
        links.current?.querySelector('[aria-current="page"], [aria-pressed="true"]')?.scrollIntoView({ block: 'nearest', inline: 'nearest', behavior: 'auto' })
    }, [mode, isChatOpen, user?.role])
    const navigate = destination => {
        onCloseChat()
        if (!user && !['home', 'search', 'browse', 'companies'].includes(destination)) onLogin()
        else onModeChange(destination)
    }
    const scroll = direction => rail.current.scrollBy({ left: direction * rail.current.clientWidth * .7,
        behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth' })
    const overflow = edges.start || edges.end
    return <div className="bottom-navigation">
        <nav className="web-dock" aria-label="Основная навигация">
            <button type="button" className="dock-brand" aria-label="Trudomix — главная" title="Главная Trudomix" onClick={() => navigate('home')}>
                <BrandLogo width="32" height="32" alt="" /><span>Trudomix</span>
            </button>
            <div className={`dock-scroll-region ${overflow ? 'has-overflow' : ''}`}>
                {overflow && <button type="button" className="dock-scroll-arrow dock-scroll-previous" disabled={!edges.start} aria-label="Предыдущие разделы" onClick={() => scroll(-1)}><Icon name="left" /></button>}
                <div className="dock-scroll" ref={rail}>
                    <div className="dock-links" ref={links}>
                        {groups.map(group => <div className="dock-group" role="group" aria-label={group.label} key={group.label}>
                            {group.items.map(([destination, label, icon]) => <button key={destination} type="button"
                                className={`glass-tab ${destination === (employer ? 'create-vacancy' : 'search') ? 'dock-featured' : ''}`}
                                aria-current={!isChatOpen && mode === destination ? 'page' : undefined}
                                title={destination === 'recommendations' ? 'Рекомендации для тебя' : label} onClick={() => navigate(destination)}>
                                <span className="glass-tab-icon"><Icon name={icon} /></span><span className="glass-tab-label">{label}</span>
                            </button>)}
                        </div>)}
                        <div className="dock-group dock-account" role="group" aria-label="Общение и аккаунт">
                            {user ? <>
                                <ChatButton currentUser={user} onOpenChatApp={onOpenChat} onCloseChatApp={onCloseChat} isChatOpen={isChatOpen} navigation />
                                <button type="button" className="glass-tab" aria-current={!isChatOpen && mode === 'profile' ? 'page' : undefined} onClick={() => navigate('profile')}>
                                    <span className="glass-tab-icon">{user.photo_url ? <img className="dock-avatar" src={user.photo_url} alt="" /> : <Icon name="profile" />}</span><span className="glass-tab-label">Профиль</span>
                                </button>
                                <button type="button" className="glass-tab dock-logout" onClick={() => { onCloseChat(); onLogout() }}><span className="glass-tab-icon"><Icon name="logout" /></span><span className="glass-tab-label">Выйти</span></button>
                            </> : <>
                                <button type="button" className="glass-tab" onClick={onLogin}><span className="glass-tab-icon"><Icon name="chat" /></span><span className="glass-tab-label">Чаты</span></button>
                                <button type="button" className="glass-tab dock-sign-in" onClick={onLogin}><span className="glass-tab-icon"><Icon name="profile" /></span><span className="glass-tab-label">Войти</span></button>
                            </>}
                        </div>
                    </div>
                </div>
                {overflow && <button type="button" className="dock-scroll-arrow dock-scroll-next" disabled={!edges.end} aria-label="Следующие разделы" onClick={() => scroll(1)}><Icon name="right" /></button>}
            </div>
        </nav>
    </div>
}
export default Sidebar
