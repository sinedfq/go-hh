import { useEffect, useRef, useState } from 'react'
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
}
function Icon({ name }) {
    return <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d={paths[name]} /></svg>
}

function Sidebar({ mode, onModeChange, user, onLogout, onLogin, onOpenChat, onCloseChat, isChatOpen }) {
    const [openMenu, setOpenMenu] = useState(null)
    const dialog = useRef(null)
    const employer = user && ['employer', 'admin'].includes(user.role)
    const candidate = !user || ['candidate', 'admin'].includes(user.role)
    const discover = [['browse', 'Карта вакансий', 'map'], ['swipe', 'Свайпы', 'swipe'], ['recommendations', 'Рекомендации', 'sparkles'], ['companies', 'Компании', 'company']]
    const work = [['my-vacancies', 'Мои вакансии', 'work'], ['create-vacancy', 'Создать вакансию', 'plus'], ['stats', 'Аналитика', 'chart'],
        ...(candidate ? [['search', 'Поиск вакансий', 'search'], ...discover] : [['browse', 'Карта вакансий', 'map']])]
    const personal = [['profile', 'Мой профиль', 'profile'], ...(candidate ? [['favorites', 'Избранное', 'favorite'], ['saved-searches', 'Мои поиски', 'saved']] : [])]
    const menuItems = openMenu === 'discover' ? discover : openMenu === 'work' ? work : personal
    const menuTitle = openMenu === 'discover' ? 'Обзор' : openMenu === 'work' ? 'Работа с вакансиями' : 'Личное'
    useEffect(() => {
        if(openMenu && !dialog.current.open)dialog.current.showModal()
        else if(!openMenu && dialog.current.open)dialog.current.close()
    }, [openMenu])
    useEffect(() => { setOpenMenu(null) }, [mode, isChatOpen, user?.id, user?.role])
    const navigate = destination => {
        setOpenMenu(null)
        onCloseChat()
        if (!user && !['home', 'search', 'browse', 'companies'].includes(destination)) onLogin()
        else onModeChange(destination)
    }
    const tab = (destination, label, icon) => <button key={destination} type="button" className="glass-tab"
        aria-current={!isChatOpen && mode === destination ? 'page' : undefined} onClick={() => navigate(destination)}>
        <Icon name={icon} /><span className="glass-tab-label">{label}</span>
    </button>
    const group = (name, label, icon, items) => <button type="button" className="glass-tab dock-menu-trigger"
        aria-haspopup="dialog" aria-expanded={openMenu === name}
        data-active={!isChatOpen && items.some(([destination]) => destination === mode) || undefined}
        onClick={() => setOpenMenu(name)}>
        {name === 'personal' && user?.photo_url ? <img className="dock-avatar" src={user.photo_url} alt="" /> : <Icon name={icon} />}
        <span className="glass-tab-label">{label}</span><svg className="dock-chevron" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true"><path d="m7 14 5-5 5 5" /></svg>
    </button>
    return <>
        <div className="bottom-navigation">
            <nav className="web-dock" aria-label="Основная навигация">
                {tab('home', 'Главная', 'home')}
                {employer ? group('work', 'Вакансии', 'work', work) : tab('search', 'Поиск', 'search')}
                {employer ? tab('my-company', 'Компания', 'company') : group('discover', 'Обзор', 'company', discover)}
                {tab(employer ? 'applications' : 'my-applications', 'Отклики', 'applications')}
                {user ? <ChatButton currentUser={user} onOpenChatApp={onOpenChat} onCloseChatApp={onCloseChat} isChatOpen={isChatOpen} navigation />
                    : <button type="button" className="glass-tab" onClick={onLogin}><Icon name="chat" /><span className="glass-tab-label">Чаты</span></button>}
                {user ? group('personal', 'Личное', 'profile', personal)
                    : <button type="button" className="glass-tab dock-sign-in" onClick={onLogin}><Icon name="profile" /><span className="glass-tab-label">Войти</span></button>}
            </nav>
        </div>
        <dialog ref={dialog} className="dock-panel" aria-labelledby="dock-panel-title" onClose={() => setOpenMenu(null)}
            onClick={event => { if(event.target === event.currentTarget)setOpenMenu(null) }}>
            <header><div><span className="dock-panel-caption">РАЗДЕЛЫ TRUDOMIX</span><h2 id="dock-panel-title">{menuTitle}</h2></div><button type="button" className="dock-panel-close" aria-label="Закрыть разделы" onClick={() => setOpenMenu(null)}>×</button></header>
            <div className="dock-panel-links">{menuItems.map(([destination, label, icon]) => <button type="button" key={destination} aria-current={mode === destination ? 'page' : undefined} onClick={() => navigate(destination)}><Icon name={icon} /><span>{label}</span></button>)}</div>
            {openMenu === 'personal' && <button type="button" className="dock-panel-logout" onClick={() => { setOpenMenu(null); onCloseChat(); onLogout() }}><Icon name="logout" />Выйти из аккаунта</button>}
        </dialog>
    </>
}
export default Sidebar
