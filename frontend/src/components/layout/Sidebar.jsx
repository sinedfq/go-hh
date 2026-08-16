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

    const isEmployer = user && (user.role === 'employer' || user.role === 'admin')

    return (
        <>
            <aside className="sidebar">
                <div className="sidebar-logo">
                    <h1>GoHH</h1>
                </div>

                <nav className="sidebar-nav">
                    {/* ====== ОБЩЕЕ ДЛЯ ВСЕХ РОЛЕЙ ====== */}
                    <div className={`nav-item ${mode === 'browse' ? 'active' : ''}`} onClick={() => handleMode('browse')}>
                        <div className="nav-bar"></div>
                        <span className="nav-icon">
                            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6" />
                                <line x1="8" y1="2" x2="8" y2="18" />
                                <line x1="16" y1="6" x2="16" y2="22" />
                            </svg>
                        </span>
                        <div className="nav-label">
                            <span className="nav-title">Просмотр</span>
                            <span className="nav-subtitle">Карта вакансий</span>
                        </div>
                    </div>

                    {/* ====== КАНДИДАТ (десктоп-пункты) ====== */}
                    {(!user || user.role === 'candidate' || user.role === 'admin') && (
                        <>
                            <div className={`nav-item ${mode === 'search' ? 'active' : ''}`} onClick={() => handleMode('search')}>
                                <div className="nav-bar"></div>
                                <span className="nav-icon">
                                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                        <circle cx="11" cy="11" r="8" />
                                        <line x1="21" y1="21" x2="16.65" y2="16.65" />
                                    </svg>
                                </span>
                                <div className="nav-label">
                                    <span className="nav-title">Поиск</span>
                                    <span className="nav-subtitle">Фильтры</span>
                                </div>
                            </div>

                            <div className={`nav-item ${mode === 'swipe' ? 'active' : ''}`} onClick={() => handleMode('swipe')}>
                                <div className="nav-bar"></div>
                                <span className="nav-icon">
                                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                        <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.29 1.51 4.04 3 5.5l7 7Z" />
                                    </svg>
                                </span>
                                <div className="nav-label">
                                    <span className="nav-title">Свайпы</span>
                                    <span className="nav-subtitle">Быстрый подбор</span>
                                </div>
                            </div>

                            <div className={`nav-item ${mode === 'favorites' ? 'active' : ''}`} onClick={() => handleMode('favorites')}>
                                <div className="nav-bar"></div>
                                <span className="nav-icon">
                                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                        <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
                                    </svg>
                                </span>
                                <div className="nav-label">
                                    <span className="nav-title">Избранное</span>
                                    <span className="nav-subtitle">Твой выбор</span>
                                </div>
                            </div>

                            <div className={`nav-item ${mode === 'my-applications' ? 'active' : ''}`} onClick={() => handleMode('my-applications')}>
                                <div className="nav-bar"></div>
                                <span className="nav-icon">
                                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" />
                                    </svg>
                                </span>
                                <div className="nav-label">
                                    <span className="nav-title">Мои отклики</span>
                                    <span className="nav-subtitle">Куда откликнулся</span>
                                </div>
                            </div>

                            {/* Только десктоп */}
                            <div className={`nav-item desktop-only ${mode === 'recommendations' ? 'active' : ''}`} onClick={() => handleMode('recommendations')}>
                                <div className="nav-bar"></div>
                                <span className="nav-icon">
                                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                        <path d="M12 3l1.9 5.8a2 2 0 0 0 1.3 1.3L21 12l-5.8 1.9a2 2 0 0 0-1.3 1.3L12 21l-1.9-5.8a2 2 0 0 0-1.3-1.3L3 12l5.8-1.9a2 2 0 0 0 1.3-1.3L12 3z" />
                                    </svg>
                                </span>
                                <div className="nav-label">
                                    <span className="nav-title">Рекомендации</span>
                                    <span className="nav-subtitle">AI-подбор</span>
                                </div>
                            </div>

                            <div className={`nav-item desktop-only ${mode === 'companies' ? 'active' : ''}`} onClick={() => handleMode('companies')}>
                                <div className="nav-bar"></div>
                                <span className="nav-icon">
                                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                        <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
                                        <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
                                    </svg>
                                </span>
                                <div className="nav-label">
                                    <span className="nav-title">Компании</span>
                                    <span className="nav-subtitle">Работодатели</span>
                                </div>
                            </div>
                        </>
                    )}

                    {/* ====== РАБОТОДАТЕЛЬ (десктоп-пункты) ====== */}
                    {isEmployer && (
                        <>
                            <div className={`nav-item ${mode === 'my-vacancies' ? 'active' : ''}`} onClick={() => handleMode('my-vacancies')}>
                                <div className="nav-bar"></div>
                                <span className="nav-icon">
                                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                                        <polyline points="14 2 14 8 20 8" />
                                        <line x1="16" y1="13" x2="8" y2="13" />
                                        <line x1="16" y1="17" x2="8" y2="17" />
                                    </svg>
                                </span>
                                <div className="nav-label">
                                    <span className="nav-title">Мои вакансии</span>
                                    <span className="nav-subtitle">Управление</span>
                                </div>
                            </div>

                            <div className={`nav-item ${mode === 'create-vacancy' ? 'active' : ''}`} onClick={() => handleMode('create-vacancy')}>
                                <div className="nav-bar"></div>
                                <span className="nav-icon">
                                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                        <circle cx="12" cy="12" r="10" />
                                        <line x1="12" y1="8" x2="12" y2="16" />
                                        <line x1="8" y1="12" x2="16" y2="12" />
                                    </svg>
                                </span>
                                <div className="nav-label">
                                    <span className="nav-title">Создать</span>
                                    <span className="nav-subtitle">Новую вакансию</span>
                                </div>
                            </div>

                            {/* Только десктоп — на мобильном в "Ещё" */}
                            <div className={`nav-item desktop-only ${mode === 'my-company' ? 'active' : ''}`} onClick={() => handleMode('my-company')}>
                                <div className="nav-bar"></div>
                                <span className="nav-icon">
                                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                        <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
                                        <polyline points="9 22 9 12 15 12 15 22" />
                                    </svg>
                                </span>
                                <div className="nav-label">
                                    <span className="nav-title">Моя компания</span>
                                    <span className="nav-subtitle">Настройки</span>
                                </div>
                            </div>

                            <div className={`nav-item desktop-only ${mode === 'applications' ? 'active' : ''}`} onClick={() => handleMode('applications')}>
                                <div className="nav-bar"></div>
                                <span className="nav-icon">
                                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                        <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" />
                                    </svg>
                                </span>
                                <div className="nav-label">
                                    <span className="nav-title">Отклики</span>
                                    <span className="nav-subtitle">Кандидаты</span>
                                </div>
                            </div>

                            <div className={`nav-item desktop-only ${mode === 'stats' ? 'active' : ''}`} onClick={() => handleMode('stats')}>
                                <div className="nav-bar"></div>
                                <span className="nav-icon">
                                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                        <line x1="18" y1="20" x2="18" y2="10" />
                                        <line x1="12" y1="20" x2="12" y2="4" />
                                        <line x1="6" y1="20" x2="6" y2="14" />
                                    </svg>
                                </span>
                                <div className="nav-label">
                                    <span className="nav-title">Аналитика</span>
                                    <span className="nav-subtitle">Статистика</span>
                                </div>
                            </div>
                        </>
                    )}

                    {/* ====== КНОПКА ЕЩЁ (мобильная) ====== */}
                    <div
                        className={`nav-item mobile-more-btn ${showMore ? 'active' : ''} ${['recommendations', 'companies', 'profile', 'my-company', 'applications', 'stats'].includes(mode) ? 'active' : ''}`}
                        onClick={() => setShowMore(!showMore)}
                    >
                        <div className="nav-bar"></div>
                        <span className="nav-icon">
                            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <circle cx="12" cy="12" r="1" />
                                <circle cx="19" cy="12" r="1" />
                                <circle cx="5" cy="12" r="1" />
                            </svg>
                        </span>
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

                        {/* Кандидатские пункты */}
                        {(!user || user.role === 'candidate' || user.role === 'admin') && (
                            <>
                                <button
                                    className={`mobile-more-item ${mode === 'recommendations' ? 'active' : ''}`}
                                    onClick={() => handleMode('recommendations')}
                                >
                                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <path d="M12 3l1.9 5.8a2 2 0 0 0 1.3 1.3L21 12l-5.8 1.9a2 2 0 0 0-1.3 1.3L12 21l-1.9-5.8a2 2 0 0 0-1.3-1.3L3 12l5.8-1.9a2 2 0 0 0 1.3-1.3L12 3z" />
                                    </svg>
                                    AI-рекомендации
                                </button>
                                <button
                                    className={`mobile-more-item ${mode === 'companies' ? 'active' : ''}`}
                                    onClick={() => handleMode('companies')}
                                >
                                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
                                        <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
                                    </svg>
                                    Компании
                                </button>
                            </>
                        )}

                        {/* Работодательские пункты */}
                        {isEmployer && (
                            <>
                                <div className="mobile-more-divider">
                                    <span>Работодателю</span>
                                </div>
                                <button
                                    className={`mobile-more-item ${mode === 'my-company' ? 'active' : ''}`}
                                    onClick={() => handleMode('my-company')}
                                >
                                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
                                        <polyline points="9 22 9 12 15 12 15 22" />
                                    </svg>
                                    Моя компания
                                </button>
                                <button
                                    className={`mobile-more-item ${mode === 'applications' ? 'active' : ''}`}
                                    onClick={() => handleMode('applications')}
                                >
                                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" />
                                    </svg>
                                    Отклики кандидатов
                                </button>
                                <button
                                    className={`mobile-more-item ${mode === 'stats' ? 'active' : ''}`}
                                    onClick={() => handleMode('stats')}
                                >
                                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <line x1="18" y1="20" x2="18" y2="10" />
                                        <line x1="12" y1="20" x2="12" y2="4" />
                                        <line x1="6" y1="20" x2="6" y2="14" />
                                    </svg>
                                    Аналитика
                                </button>
                            </>
                        )}

                        {/* Общие пункты */}
                        <div className="mobile-more-divider">
                            <span>Аккаунт</span>
                        </div>
                        {user ? (
                            <>
                                <button
                                    className={`mobile-more-item ${mode === 'profile' ? 'active' : ''}`}
                                    onClick={() => handleMode('profile')}
                                >
                                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                                        <circle cx="12" cy="7" r="4" />
                                    </svg>
                                    Профиль
                                </button>
                                <button
                                    className="mobile-more-item mobile-more-logout"
                                    onClick={() => { setShowMore(false); onLogout(); }}
                                >
                                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                                        <polyline points="16 17 21 12 16 7" />
                                        <line x1="21" y1="12" x2="9" y2="12" />
                                    </svg>
                                    Выйти
                                </button>
                            </>
                        ) : (
                            <button
                                className="mobile-more-item"
                                onClick={() => { setShowMore(false); onLogin(); }}
                            >
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4" />
                                    <polyline points="10 17 15 12 10 7" />
                                    <line x1="15" y1="12" x2="3" y2="12" />
                                </svg>
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