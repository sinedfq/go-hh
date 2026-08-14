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

function App() {
    const app = useApp()

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
                        {!app.companyPageId && !app.vacancyPageId && app.mode === 'browse' && 'Все вакансии'}
                        {!app.companyPageId && !app.vacancyPageId && app.mode === 'swipe' && 'Свайпай вакансии'}
                        {!app.companyPageId && !app.vacancyPageId && app.mode === 'favorites' && 'Избранные вакансии'}
                        {!app.companyPageId && !app.vacancyPageId && app.mode === 'profile' && 'Профиль'}
                        {!app.companyPageId && !app.vacancyPageId && app.mode === 'recommendations' && 'AI-рекомендации'}
                        {!app.companyPageId && !app.vacancyPageId && app.mode === 'companies' && 'Компании'}
                    </h2>
                </header>

                <div className="content">
                    {app.companyPageId ? (
                        <CompanyDetails
                            companyId={app.companyPageId}
                            onBack={app.closeCompanyPage}
                            onSelectVacancy={app.openVacancyPage}
                        />
                    ) : app.vacancyPageId ? (
                        <VacancyPage
                            vacancyId={app.vacancyPageId}
                            onClose={app.closeVacancyPage}
                            onOpenCompany={app.openCompanyPage}
                        />
                    ) : (
                        <>
                            {app.mode === 'browse' && (
                                <>
                                    <VacancyList
                                        vacancies={app.vacancies}
                                        selectedVacancy={app.selectedVacancy}
                                        onSelect={app.setSelectedVacancy}
                                        isFavorite={app.isFavorite}
                                        onOpenVacancy={app.openVacancyPage}
                                    />
                                    <main className="vacancy-detail">
                                        <VacancyDetail
                                            vacancy={app.selectedVacancy}
                                            isFavorite={app.isFavorite}
                                            onAddFavorite={app.addToFavorites}
                                            onRemoveFavorite={app.removeFromFavorites}
                                            onOpenVacancy={app.openVacancyPage}
                                            onOpenCompany={app.openCompanyPage}
                                        />
                                    </main>
                                </>
                            )}

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
                                                    onOpenVacancy={app.openVacancyPage}
                                                    onOpenCompany={app.openCompanyPage}
                                                />
                                            ))
                                        )}
                                    </div>
                                </div>
                            )}

                            {app.mode === 'favorites' && app.user && (
                                <div className="favorites-container">
                                    <FavoritesList
                                        favorites={app.favorites}
                                        onRemove={app.removeFromFavorites}
                                        onOpenVacancy={app.openVacancyPage}
                                    />
                                </div>
                            )}

                            {app.mode === 'profile' && app.user && (
                                <ProfilePage onResumeUpdate={(r) => {
                                    app.setResume(r)
                                    app.setRecommendationsLoaded(false)
                                }} />
                            )}

                            {app.mode === 'recommendations' && app.user && (
                                <RecommendationsPage
                                    recommendations={app.recommendations}
                                    selectedVacancy={app.selectedRecommendedVacancy}
                                    onSelectVacancy={app.setSelectedRecommendedVacancy}
                                    loading={app.recommendationsLoading}
                                    onRefresh={() => app.loadRecommendations(true)}
                                    onOpenVacancy={app.openVacancyPage}
                                />
                            )}

                            {app.mode === 'companies' && (
                                <CompaniesPage onSelectVacancy={app.openVacancyPage} />
                            )}
                        </>
                    )}
                </div>
            </div>
        </div>
    )
}

export default App