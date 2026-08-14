function Sidebar({ mode, onModeChange, user, onLogout, onLogin }) {
    return (
        <aside className="sidebar">
            <div className="sidebar-logo">
                <h1>GoHH!</h1>
            </div>

            <nav className="sidebar-nav">
                <div
                    className={`nav-item ${mode === 'browse' ? 'active' : ''}`}
                    onClick={() => onModeChange('browse')}
                >
                    <div className="nav-bar"></div>
                    <div className="nav-label">
                        <span className="nav-title">Просмотр</span>
                        <span className="nav-subtitle">Все вакансии</span>
                    </div>
                </div>

                <div
                    className={`nav-item ${mode === 'swipe' ? 'active' : ''}`}
                    onClick={() => onModeChange('swipe')}
                >
                    <div className="nav-bar"></div>
                    <div className="nav-label">
                        <span className="nav-title">Свайпы</span>
                        <span className="nav-subtitle">Как в Tinder</span>
                    </div>
                </div>

                <div
                    className={`nav-item ${mode === 'favorites' ? 'active' : ''}`}
                    onClick={() => onModeChange('favorites')}
                >
                    <div className="nav-bar"></div>
                    <div className="nav-label">
                        <span className="nav-title">Избранное</span>
                        <span className="nav-subtitle">Ваши вакансии</span>
                    </div>
                </div>

                <div
                    className={`nav-item ${mode === 'recommendations' ? 'active' : ''}`}
                    onClick={() => onModeChange('recommendations')}
                >
                    <div className="nav-bar"></div>
                    <div className="nav-label">
                        <span className="nav-title">Рекомендации</span>
                        <span className="nav-subtitle">AI-подбор</span>
                    </div>
                </div>

                <div
                    className={`nav-item ${mode === 'companies' ? 'active' : ''}`}
                    onClick={() => onModeChange('companies')}
                >
                    <div className="nav-bar"></div>
                    <div className="nav-label">
                        <span className="nav-title">Компании</span>
                        <span className="nav-subtitle">Каталог</span>
                    </div>
                </div>
            </nav>

            {user ? (
                <div className="sidebar-footer">
                    <div
                        className={`user-profile-btn ${mode === 'profile' ? 'active' : ''}`}
                        onClick={() => onModeChange('profile')}
                    >
                        <div className="user-avatar">
                            {user.photo_url ? (
                                <img
                                    src={user.photo_url}
                                    alt={user.email}
                                    style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '50%' }}
                                />
                            ) : (
                                user.email[0].toUpperCase()
                            )}
                        </div>
                        <div className="user-profile-label">
                            <span className="user-profile-title">Профиль</span>
                            <span className="user-profile-email">{user.email}</span>
                        </div>
                    </div>
                    <button className="logout-btn" onClick={onLogout}>
                        Выйти
                    </button>
                </div>
            ) : (
                <div className="sidebar-footer">
                    <button className="login-btn" onClick={onLogin} title="Войти">
                        <svg
                            className="login-icon"
                            width="20"
                            height="20"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth="2"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                        >
                            <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4" />
                            <polyline points="10 17 15 12 10 7" />
                            <line x1="15" y1="12" x2="3" y2="12" />
                        </svg>
                        <span className="login-text">Войти</span>
                    </button>
                </div>
            )}
        </aside>
    )
}

export default Sidebar