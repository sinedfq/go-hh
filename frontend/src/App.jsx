import { useApp } from './hooks/useApp'
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


function App() {
    const app = useApp()

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
                onLogout={app.logout}
                onLogin={() => app.setShowLoginModal(true)}
            />

            <div className="main-area">
                <header className="top-header">
                    <h2 className="page-title">
                        {app.companyPageId && 'Компания'}
                        {!app.companyPageId && app.vacancyPageId && 'Вакансия'}
                        {!app.companyPageId && !app.vacancyPageId && app.mode === 'browse' && 'Карта'}
                        {!app.companyPageId && !app.vacancyPageId && app.mode === 'search' && 'Поиск вакансий'}
                        {!app.companyPageId && !app.vacancyPageId && app.mode === 'swipe' && 'Свайпай вакансии'}
                        {!app.companyPageId && !app.vacancyPageId && app.mode === 'favorites' && 'Избранные вакансии'}
                        {!app.companyPageId && !app.vacancyPageId && app.mode === 'profile' && 'Профиль'}
                        {!app.companyPageId && !app.vacancyPageId && app.mode === 'recommendations' && 'AI-рекомендации'}
                        {!app.companyPageId && !app.vacancyPageId && app.mode === 'companies' && 'Компании'}
                    </h2>

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

                            {/* ====== ПРОФИЛЬ ====== */}
                            {app.mode === 'profile' && app.user && (
                                <ProfilePage onResumeUpdate={(r) => {
                                    app.setResume(r)
                                    app.setRecommendationsLoaded(false)
                                }} />
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
                        </>
                    )}
                </div>
            </div>
        </div>
    )
}

export default App