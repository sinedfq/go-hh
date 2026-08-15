import { useState } from 'react'

function Sidebar({ mode, onModeChange, user, onLogout, onLogin }) {
    const [showMore, setShowMore] = useState(false)

    const handleMode = (newMode) => {
        onModeChange(newMode)
        setShowMore(false)
    }

    const getAvatarLetter = () => {
        const name = user?.name || user?.email || 'U'
        return name.charAt(0).toUpperCase()
    }

    return (
        <>
            <aside className="sidebar">
                <div className="sidebar-logo">
                    <h1>GoHH</h1>
                </div>

                <nav className="sidebar-nav">
                    <div className={`nav-item ${mode === 'browse' ? 'active' : ''}`} onClick={() => handleMode('browse')}>
                        <div className="nav-bar"></div>
                        <div className="nav-label">
                            <span className="nav-title">Просмотр</span>
                            <span className="nav-subtitle">Карта вакансий</span>
                        </div>
                    </div>

                    <div className={`nav-item ${mode === 'search' ? 'active' : ''}`} onClick={() => handleMode('search')}>
                        <div className="nav-bar"></div>
                        <div className="nav-label">
                            <span className="nav-title">Поиск</span>
                            <span className="nav-subtitle">Фильтры</span>
                        </div>
                    </div>

                    <div className={`nav-item ${mode === 'swipe' ? 'active' : ''}`} onClick={() => handleMode('swipe')}>
                        <div className="nav-bar"></div>
                        <div className="nav-label">
                            <span className="nav-title">Свайпы</span>
                            <span className="nav-subtitle">Быстрый подбор</span>
                        </div>
                    </div>

                    <div className={`nav-item ${mode === 'favorites' ? 'active' : ''}`} onClick={() => handleMode('favorites')}>
                        <div className="nav-bar"></div>
                        <div className="nav-label">
                            <span className="nav-title">Избранное</span>
                            <span className="nav-subtitle">Твой выбор</span>
                        </div>
                    </div>

                    {/* На мобильных спрятаны — доступны через "Ещё" */}
                    <div className={`nav-item more-only ${mode === 'recommendations' ? 'active' : ''}`} onClick={() => handleMode('recommendations')}>
                        <div className="nav-bar"></div>
                        <div className="nav-label">
                            <span className="nav-title">Рекомендации</span>
                            <span className="nav-subtitle">AI-подбор</span>
                        </div>
                    </div>

                    <div className={`nav-item more-only ${mode === 'companies' ? 'active' : ''}`} onClick={() => handleMode('companies')}>
                        <div className="nav-bar"></div>
                        <div className="nav-label">
                            <span className="nav-title">Компании</span>
                            <span className="nav-subtitle">Работодатели</span>
                        </div>
                    </div>

                    {/* Кнопка "Ещё" — только мобильные */}
                    <div
                        className={`nav-item mobile-more-btn ${showMore ? 'active' : ''} ${['recommendations', 'companies', 'profile'].includes(mode) ? 'active' : ''}`}
                        onClick={() => setShowMore(!showMore)}
                    >
                        <div className="nav-bar"></div>
                        <div className="nav-label">
                            <span className="nav-title">Ещё</span>
                            <span className="nav-subtitle">Меню</span>
                        </div>
                    </div>
                </nav>

                <div className="sidebar-footer">
                    {user ? (
                        <>
                            <div className="user-profile-btn" onClick={() => handleMode('profile')}>
                                <div className="user-avatar">{getAvatarLetter()}</div>
                                <div className="user-profile-label">
                                    <span className="user-profile-title">{user.name || 'Профиль'}</span>
                                    <span className="user-profile-email">{user.email}</span>
                                </div>
                            </div>
                            <button className="logout-btn" onClick={onLogout}>Выйти</button>
                        </>
                    ) : (
                        <button className="login-btn" onClick={onLogin}>
                            <svg className="login-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                                <circle cx="12" cy="7" r="4" />
                            </svg>
                            <span className="login-text">Войти</span>
                        </button>
                    )}
                </div>
            </aside>

            {/* ====== МОБИЛЬНОЕ МЕНЮ "ЕЩЁ" ====== */}
            {showMore && (
                <div className="mobile-more-overlay" onClick={() => setShowMore(false)}>
                    <div className="mobile-more-sheet" onClick={(e) => e.stopPropagation()}>
                        <div className="mobile-more-handle"></div>
                        <button className="mobile-more-item" onClick={() => handleMode('recommendations')}>
                            AI-рекомендации
                        </button>
                        <button className="mobile-more-item" onClick={() => handleMode('companies')}>
                            Компании
                        </button>
                        {user ? (
                            <>
                                <button className="mobile-more-item" onClick={() => handleMode('profile')}>
                                    Профиль
                                </button>
                                <button className="mobile-more-item mobile-more-logout" onClick={onLogout}>
                                    Выйти
                                </button>
                            </>
                        ) : (
                            <button className="mobile-more-item" onClick={() => { setShowMore(false); onLogin(); }}>
                                Войти
                            </button>
                        )}
                    </div>
                </div>
            )}
        </>
    )
}

export default Sidebar