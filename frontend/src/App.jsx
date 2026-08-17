import { useApp } from './hooks/useApp'
import { useState, useEffect } from 'react'
import Sidebar from './components/layout/Sidebar'
import SwipeableCard from './components/vacancy/SwipeableCard'
import VacancyList from './components/vacancy/VacancyList'
import VacancyDetail from './components/vacancy/VacancyDetail'
import FavoritesList from './components/favorites/FavoritesList'
import LoginPage from './pages/auth/LoginPage'
import ProfilePage from './pages/profile/ProfilePage'
import RecommendationsPage from './pages/recommendation/RecommendationsPage'
import CompaniesPage from './pages/company/CompaniesPage'
import VacancyPage from './pages/vacancy/VacancyPage'
import CompanyDetails from './components/company/CompanyDetails'
import SearchBar from './components/vacancy/SearchBar'
import FilterPanel from './components/vacancy/FilterPanel'
import SearchResultsPage from './pages/search/SearchResultsPage'
import VacancyMap from './components/vacancy/VacancyMap'
import MyVacanciesPage from './pages/employer/MyVacanciesPage'
import MyCompanyPage from './pages/employer/MyCompanyPage'
import CreateVacancyPage from './pages/employer/CreateVacancyPage'
import NotificationBell from './components/common/NotificationBell'
import EmployerApplicationsPage from './pages/employer/EmployerApplicationsPage'
import ResumeViewModal from './components/resume/ResumeViewModal'
import MyApplicationsPage from './pages/candidate/MyApplicationsPage'
import EmployerStatsPage from './pages/employer/EmployerStatsPage'
import ChatApp from './components/chat/ChatApp'
import ChatButton from './components/chat/ChatButton'
import axios from 'axios'

function App() {
    const app = useApp()


    const [theme, setTheme] = useState(() => localStorage.getItem('theme') || 'light')
    const [viewResumeId, setViewResumeId] = useState(null)

    // Применяем тему
    useEffect(() => {
        document.documentElement.setAttribute('data-theme', theme)
        localStorage.setItem('theme', theme)
    }, [theme])

    useEffect(() => {
        const handleToast = (e) => {
            app.showToast(e.detail)
        }
        window.addEventListener('show-toast', handleToast)
        return () => window.removeEventListener('show-toast', handleToast)
    }, [])

    // ====== ВЫХОД: сброс вкладки + уведомление ======
    const handleLogout = async () => {
        await app.logout()
        app.handleModeChange('browse')   // уходим с профиля на Просмотр
        app.showToast('Вы вышли из аккаунта')  // ← ИЗМЕНЕНО
    }

    // ====== ОБЁРТКА ДЛЯ ОТКРЫТИЯ ВАКАНСИИ (сохраняет скролл) ======
    const handleOpenVacancy = (id) => {
        app.saveScrollPosition()
        app.openVacancyPage(id)
    }

    if (app.authLoading) {
        return (
            <div className="app">
                <div className="loading">
                    <div className="spinner"></div>
                    <p>Загрузка...</p>
                </div>
            </div>
        )
    }

    return (
        <div className="app">
            {app.showLoginModal && (
                <LoginPage onClose={() => app.setShowLoginModal(false)} />
            )}

            <Sidebar
                mode={app.mode}
                onModeChange={app.handleModeChange}
                user={app.user}
                onLogout={handleLogout}
                onLogin={() => app.setShowLoginModal(true)}
            />

            <div className="main-area">
                <header className="top-header">
                    <h2 className="page-title">
                        {app.mode === 'browse' && 'Просмотр'}
                        {app.mode === 'search' && 'Поиск'}
                        {app.mode === 'swipe' && 'Свайпы'}
                        {app.mode === 'favorites' && 'Избранное'}
                        {app.mode === 'recommendations' && 'Рекомендации'}
                        {app.mode === 'companies' && 'Компании'}
                        {app.mode === 'profile' && 'Профиль'}
                        {app.mode === 'my-vacancies' && 'Мои вакансии'}
                        {app.mode === 'create-vacancy' && 'Создать вакансию'}
                        {app.mode === 'my-company' && 'Моя компания'}
                        {app.mode === 'applications' && 'Отклики'}
                        {app.mode === 'my-applications' && 'Мои отклики'}
                        {app.mode === 'stats' && 'Аналитика'}
                    </h2>

                    <div className="header-actions">
                        <button
                            className="theme-toggle"
                            onClick={() => setTheme(theme === 'light' ? 'dark' : 'light')}
                            title={theme === 'light' ? 'Тёмная тема' : 'Светлая тема'}
                        >
                            {theme === 'light' ? (
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
                                </svg>
                            ) : (
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <circle cx="12" cy="12" r="5" />
                                    <line x1="12" y1="1" x2="12" y2="3" />
                                    <line x1="12" y1="21" x2="12" y2="23" />
                                    <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" />
                                    <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" />
                                    <line x1="1" y1="12" x2="3" y2="12" />
                                    <line x1="21" y1="12" x2="23" y2="12" />
                                    <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" />
                                    <line x1="18.36" y1="5.64" x2="19.78" y2="4.22" />
                                </svg>
                            )}
                        </button>
                        {app.user && <NotificationBell />}
                        {/* Профиль в шапке — виден только на мобильных */}
                        <div className="header-profile">
                            {app.user ? (
                                <button
                                    className={`header-profile-btn ${app.mode === 'profile' ? 'active' : ''}`}
                                    onClick={() => app.handleModeChange('profile')}
                                >
                                    <div className="header-avatar">
                                        {(app.user.name || app.user.email || 'U').charAt(0).toUpperCase()}
                                    </div>
                                </button>
                            ) : (
                                <button className="header-login-btn" onClick={() => app.setShowLoginModal(true)}>
                                    Войти
                                </button>
                            )}
                        </div>
                    </div>
                </header>

                <div className="content">
                    {app.companyPageId ? (
                        <CompanyDetails
                            companyId={app.companyPageId}
                            onBack={app.closeCompanyPage}
                            onSelectVacancy={handleOpenVacancy}
                        />
                    ) : app.vacancyPageId ? (
                        <VacancyPage
                            vacancyId={app.vacancyPageId}
                            onClose={app.closeVacancyPage}
                            onOpenCompany={app.openCompanyPage}
                            showToast={app.showToast}
                        />
                    ) : (
                        <>
                            {/* ====== ПРОСМОТР (двухколоночный) ====== */}
                            {app.mode === 'browse' && (
                                <div className="browse-split-view">
                                    <aside className="browse-sidebar">
                                        <div className="browse-sidebar-header">
                                            <h2>Вакансии</h2>
                                            <span className="browse-count">{app.vacancies.length}</span>
                                        </div>

                                        <div className="browse-sidebar-search">
                                            <SearchBar
                                                value={app.searchFilters.query}
                                                onChange={(query) => app.applyFilters({ ...app.searchFilters, query }, true)}
                                                placeholder="Фильтр..."
                                            />
                                        </div>

                                        <div className="browse-sidebar-list">
                                            {app.vacancies.map((vacancy, index) => (
                                                <div
                                                    key={vacancy.id}
                                                    className={`browse-list-item animate-slide-in ${app.selectedVacancy?.id === vacancy.id ? 'active' : ''}`}
                                                    style={{ animationDelay: `${index * 30}ms` }}
                                                    onClick={() => app.setSelectedVacancy(vacancy)}
                                                >
                                                    <div className="browse-list-item-title">{vacancy.title}</div>
                                                    <div className="browse-list-item-company">{vacancy.company}</div>
                                                    <div className="browse-list-item-meta">
                                                        <span>{vacancy.location || 'Не указан'}</span>
                                                        <span className="browse-list-item-dot">•</span>
                                                        <span>{vacancy.experience}</span>
                                                        {app.isFavorite(vacancy.id) && (
                                                            <span className="browse-list-item-fav">★</span>
                                                        )}
                                                    </div>
                                                </div>
                                            ))}
                                        </div>

                                        <button className="browse-all-filters-btn" onClick={() => app.handleModeChange('search')}>
                                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                <line x1="4" y1="21" x2="4" y2="14" />
                                                <line x1="4" y1="10" x2="4" y2="3" />
                                                <line x1="12" y1="21" x2="12" y2="12" />
                                                <line x1="12" y1="8" x2="12" y2="3" />
                                                <line x1="20" y1="21" x2="20" y2="16" />
                                                <line x1="20" y1="12" x2="20" y2="3" />
                                            </svg>
                                            Расширенный поиск
                                        </button>
                                    </aside>


                                    <main className="browse-detail-panel">
                                        <VacancyMap
                                            vacancies={app.vacancies}
                                            selectedVacancy={app.selectedVacancy}
                                            onOpenVacancy={handleOpenVacancy}
                                            onSelectVacancy={app.setSelectedVacancy}
                                        />
                                    </main>

                                </div>
                            )}

                            {/* ====== ПОИСК ====== */}
                            {app.mode === 'search' && (
                                <SearchResultsPage
                                    results={app.searchResults}
                                    totalCount={app.searchTotal}
                                    loading={app.searchLoading}
                                    filters={app.searchFilters}
                                    onFiltersChange={app.updateFilters}
                                    onApply={(f) => app.applyFilters(f)}
                                    onReset={app.resetFilters}
                                    onOpenVacancy={handleOpenVacancy}
                                    cities={app.cities}
                                    showToast={app.showToast}
                                />
                            )}

                            {/* ====== СВАЙПЫ ====== */}
                            {app.mode === 'swipe' && app.user && (
                                <div className="swipe-container">
                                    <div className="card-container">
                                        {app.vacancies.length === 0 ? (
                                            <div className="empty-state">
                                                <h2>Вакансий пока нет</h2>
                                                <p>Создай первую вакансию через API</p>
                                            </div>
                                        ) : app.currentIndex < 0 ? (
                                            <div className="empty-state">
                                                <h2>Вакансии закончились</h2>
                                                <p>Ты просмотрел все {app.vacancies.length} вакансий</p>
                                                <button className="btn btn-primary" onClick={app.loadVacancies}>
                                                    Начать заново
                                                </button>
                                            </div>
                                        ) : (
                                            app.vacancies.slice(0, app.currentIndex + 1).map((vacancy, index) => (
                                                <SwipeableCard
                                                    key={vacancy.id}
                                                    vacancy={vacancy}
                                                    onSwipe={app.handleSwipe}
                                                    isTop={index === app.currentIndex}
                                                    matchScore={app.matchScores[vacancy.id]}
                                                    onOpenVacancy={handleOpenVacancy}
                                                    onOpenCompany={app.openCompanyPage}
                                                />
                                            ))
                                        )}
                                    </div>
                                </div>
                            )}

                            {/* ====== ИЗБРАННОЕ ====== */}
                            {app.mode === 'favorites' && app.user && (
                                <div className="favorites-container">
                                    <FavoritesList
                                        favorites={app.favorites}
                                        onRemove={app.removeFromFavorites}
                                        onOpenVacancy={handleOpenVacancy}
                                    />
                                </div>
                            )}

                            {app.mode === 'my-applications' && app.user && (app.user.role === 'candidate' || app.user.role === 'admin') && (
                                <MyApplicationsPage
                                    onOpenVacancy={(id) => app.openVacancyPage(id)}
                                    onOpenChatWith={app.openChatWith}
                                    showToast={app.showToast}
                                />
                            )}

                            {/* ====== ПРОФИЛЬ ====== */}
                            {app.mode === 'profile' && app.user && (
                                <ProfilePage
                                    onResumeUpdate={(r) => {
                                        app.setResume(r)
                                        app.setRecommendationsLoaded(false)
                                    }}
                                    onLogout={handleLogout}
                                    showToast={app.showToast}   // ← ДОБАВЬ
                                />
                            )}

                            {/* ====== РЕКОМЕНДАЦИИ ====== */}
                            {app.mode === 'recommendations' && app.user && (
                                <RecommendationsPage
                                    recommendations={app.recommendations}
                                    selectedVacancy={app.selectedRecommendedVacancy}
                                    onSelectVacancy={app.setSelectedRecommendedVacancy}
                                    loading={app.recommendationsLoading}
                                    onRefresh={() => app.loadRecommendations(true)}
                                    onOpenVacancy={handleOpenVacancy}
                                />
                            )}

                            {/* ====== КОМПАНИИ ====== */}
                            {app.mode === 'companies' && (
                                <CompaniesPage onSelectVacancy={handleOpenVacancy} />
                            )}

                            {app.mode === 'my-vacancies' && app.user && (app.user.role === 'employer' || app.user.role === 'admin') && (
                                <MyVacanciesPage
                                    myCompany={app.myCompany}
                                    myVacancies={app.myVacancies}
                                    employerLoading={app.employerLoading}
                                    employerError={app.employerError}
                                    onOpenVacancy={app.openVacancyPage}
                                    onCreateVacancy={() => app.handleModeChange('create-vacancy')}
                                    onReload={app.loadMyVacancies}
                                />
                            )}

                            {app.mode === 'my-company' && app.user && (app.user.role === 'employer' || app.user.role === 'admin') && (
                                <MyCompanyPage
                                    myCompany={app.myCompany}
                                    onCreateCompany={async (data) => {
                                        await app.createCompany(data)
                                        app.handleModeChange('my-vacancies')
                                    }}
                                />
                            )}

                            {app.mode === 'stats' && app.user && (app.user.role === 'employer' || app.user.role === 'admin') && (
                                <EmployerStatsPage />
                            )}

                            {app.mode === 'create-vacancy' && app.user && (app.user.role === 'employer' || app.user.role === 'admin') && (
                                <CreateVacancyPage
                                    cities={app.cities}
                                    allSkills={app.allSkills}
                                    allPositions={app.allPositions}
                                    onCreate={async (data) => {
                                        await app.createVacancy(data)
                                        app.handleModeChange('my-vacancies')
                                    }}
                                    onCancel={() => app.handleModeChange('my-vacancies')}
                                />
                            )}



                            {/* ====== УВЕДОМЛЕНИЕ ====== */}


                            {app.mode === 'applications' && app.user && (app.user.role === 'employer' || app.user.role === 'admin') && (
                                <EmployerApplicationsPage
                                    onOpenVacancy={(id) => app.openVacancyPage(id)}
                                    onOpenResume={(resumeId) => setViewResumeId(resumeId)}
                                    onOpenChatWith={app.openChatWith}   // ← ИЗМЕНЕНО: теперь передаём openChatWith
                                    showToast={app.showToast}            // ← ДОБАВЛЕНО
                                />
                            )}


                        </>
                    )}

                    {viewResumeId && (
                        <ResumeViewModal
                            resumeId={viewResumeId}
                            onClose={() => setViewResumeId(null)}
                        />
                    )}
                </div>

                {app.toast && (
                    <div className="toast">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                            <polyline points="20 6 9 17 4 12" />
                        </svg>
                        {app.toast}
                    </div>
                )}
            </div>

        

            {/* ====== ПЛАВАЮЩАЯ КНОПКА ЧАТА ====== */}
            {app.user && (
                <div className="chat-fab-container">
                    <ChatButton
                        currentUser={app.user}
                        onOpenChatApp={app.openChatApp}
                        onCloseChatApp={app.closeChatApp}   // ← ДОБАВЬ ЭТУ СТРОКУ
                        isChatOpen={app.showChatApp}
                    />
                </div>
            )}

            {/* ====== ПРИЛОЖЕНИЕ ЧАТА (двухпанельное) ====== */}
            {app.showChatApp && app.user && (
                <ChatApp
                    currentUser={app.user}
                    onClose={app.closeChatApp}
                    initialChat={app.initialChat}
                    onOpenVacancy={(id) => {
                        app.closeChatApp()
                        app.openVacancyPage(id)
                    }}
                    onCancelApplication={async (applicationId) => {
                        try {
                            await axios.delete(`/api/applications/${applicationId}`)
                            app.closeChatApp()
                            app.showToast('🗑️ Отклик отменён')  // ← Toast вместо alert
                        } catch (err) {
                            console.error('Ошибка:', err)
                            app.showToast('Не удалось отменить отклик')  // ← Toast вместо alert
                        }
                    }}
                />
            )}
        </div>
    )
}

export default App