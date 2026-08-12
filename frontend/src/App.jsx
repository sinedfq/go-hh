import { useState, useEffect } from 'react'
import axios from 'axios'
import { useAuth } from './contexts/AuthContext'
import SwipeableCard from './components/SwipeableCard'
import VacancyList from './components/VacancyList'
import VacancyDetail from './components/VacancyDetail'
import FavoritesList from './components/FavoritesList'
import LoginPage from './pages/LoginPage'
import ProfilePage from './pages/ProfilePage'
import RecommendationsPage from './pages/RecommendationsPage'
import './App.css'

function App() {
  const { user, loading: authLoading, logout } = useAuth()

  // Вакансии и избранное
  const [vacancies, setVacancies] = useState([])
  const [favorites, setFavorites] = useState([])
  const [currentIndex, setCurrentIndex] = useState(-1)
  const [selectedVacancy, setSelectedVacancy] = useState(null)
  const [loading, setLoading] = useState(true)

  // Режимы и модалки
  const [mode, setMode] = useState('browse')
  const [showLoginModal, setShowLoginModal] = useState(false)

  // Рекомендации
  const [recommendations, setRecommendations] = useState([])
  const [selectedRecommendedVacancy, setSelectedRecommendedVacancy] = useState(null)
  const [recommendationsLoaded, setRecommendationsLoaded] = useState(false)
  const [recommendationsLoading, setRecommendationsLoading] = useState(false)

  // Резюме (для обновления рекомендаций)
  const [resume, setResume] = useState(null)

  const [matchScores, setMatchScores] = useState({})

  // ====== EFFECTS ======

  // Интерцептор для авто-логаута при 401
  useEffect(() => {
    const interceptor = axios.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response?.status === 401) {
          logout()
        }
        return Promise.reject(error)
      }
    )
    return () => axios.interceptors.response.eject(interceptor)
  }, [logout])

  // Загрузка вакансий при старте и избранного при логине
  useEffect(() => {
    if (!authLoading) {
      loadVacancies()
      if (user) {
        loadFavorites()
      }
    }
  }, [user, authLoading])

  // Закрытие логин модалки при успешном логине
  useEffect(() => {
    if (user) {
      setShowLoginModal(false)
    }
  }, [user])

  // Загрузка рекомендаций при переключении в режим
  useEffect(() => {
    if (mode === 'recommendations' && user && !recommendationsLoaded) {
      loadRecommendations()
    }
  }, [mode, user])

  // Обновление рекомендаций при изменении резюме
  useEffect(() => {
    if (user && recommendationsLoaded) {
      setRecommendationsLoaded(false)
      if (mode === 'recommendations') {
        loadRecommendations(true)
      }
    }
  }, [resume])

  useEffect(() => {
    if (mode === 'swipe' && user && !recommendationsLoaded) {
      loadRecommendations()
    }
  }, [mode, user])

  useEffect(() => {
    if ((mode === 'recommendations' || mode === 'swipe') && user && !recommendationsLoaded) {
      loadRecommendations()
    }
  }, [mode, user])

  // ====== DATA LOADING ======

  const loadVacancies = async () => {
    try {
      setLoading(true)
      const res = await axios.get('/api/vacancies')
      let data = res.data
      if (!Array.isArray(data)) {
        data = data.value || []
      }
      setVacancies(data)
      setCurrentIndex(data.length - 1)
      if (data.length > 0 && !selectedVacancy) {
        setSelectedVacancy(data[0])
      }
      setLoading(false)
    } catch (err) {
      console.error('Ошибка загрузки вакансий:', err)
      setLoading(false)
    }
  }

  const loadFavorites = async () => {
    try {
      const res = await axios.get('/api/favorites')
      let data = res.data
      if (!Array.isArray(data)) {
        data = []
      }
      setFavorites(data)
    } catch (err) {
      console.error('Ошибка загрузки избранных:', err)
    }
  }

  const loadRecommendations = async (force = false) => {
    if (recommendationsLoaded && !force) return

    setRecommendationsLoading(true)
    try {
      const url = force ? '/api/recommendations?refresh=true' : '/api/recommendations'
      const res = await axios.get(url)
      const recs = res.data.recommendations || []
      setRecommendations(recs)

      // Создаём мапу vacancy_id -> score
      const scores = {}
      recs.forEach(rec => {
        scores[rec.vacancy.id] = rec.score
      })
      setMatchScores(scores)

      if (recs.length > 0) {
        setSelectedRecommendedVacancy(recs[0].vacancy)
      } else {
        setSelectedRecommendedVacancy(null)
      }
      setRecommendationsLoaded(true)
    } catch (err) {
      console.error('Ошибка загрузки рекомендаций:', err)
    } finally {
      setRecommendationsLoading(false)
    }
  }
  // ====== FAVORITES ======

  const addToFavorites = async (vacancyId) => {
    if (!user) {
      setShowLoginModal(true)
      return
    }
    try {
      await axios.post('/api/favorites', { vacancy_id: vacancyId })
      loadFavorites()
    } catch (err) {
      console.error('Ошибка добавления в избранное:', err)
    }
  }

  const removeFromFavorites = async (vacancyId) => {
    if (!user) return
    try {
      await axios.delete(`/api/favorites/${vacancyId}`)
      loadFavorites()
    } catch (err) {
      console.error('Ошибка удаления из избранного:', err)
    }
  }

  // ====== SWIPE ======

  const handleSwipe = (direction, vacancyId) => {
    if (direction === 'right') {
      addToFavorites(vacancyId)
    }
    setCurrentIndex(prev => {
      const nextIndex = prev - 1
      if (nextIndex >= 0 && vacancies[nextIndex]) {
        setSelectedVacancy(vacancies[nextIndex])
      }
      return nextIndex
    })
  }

  const swipe = (dir) => {
    if (currentIndex >= 0) {
      const vacancy = vacancies[currentIndex]
      handleSwipe(dir, vacancy.id)
    }
  }

  const isFavorite = (vacancyId) => {
    return favorites.some(fav => fav.id === vacancyId)
  }

  // ====== NAVIGATION ======

  const handleModeChange = (newMode) => {
    if ((newMode === 'swipe' || newMode === 'favorites' || newMode === 'recommendations') && !user) {
      setShowLoginModal(true)
      return
    }
    setMode(newMode)
  }

  // ====== RENDER ======

  if (authLoading) {
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
      {/* Модальное окно логина */}
      {showLoginModal && (
        <LoginPage onClose={() => setShowLoginModal(false)} />
      )}

      {/* ====== САЙДБАР ====== */}
      <aside className="sidebar">
        <div className="sidebar-logo">
          <h1>GoHH!</h1>
        </div>

        <nav className="sidebar-nav">
          <div
            className={`nav-item ${mode === 'browse' ? 'active' : ''}`}
            onClick={() => setMode('browse')}
          >
            <div className="nav-bar"></div>
            <div className="nav-label">
              <span className="nav-title">Просмотр</span>
              <span className="nav-subtitle">Все вакансии</span>
            </div>
          </div>

          <div
            className={`nav-item ${mode === 'swipe' ? 'active' : ''}`}
            onClick={() => handleModeChange('swipe')}
          >
            <div className="nav-bar"></div>
            <div className="nav-label">
              <span className="nav-title">Свайпы</span>
              <span className="nav-subtitle">Как в Tinder</span>
            </div>
          </div>

          <div
            className={`nav-item ${mode === 'favorites' ? 'active' : ''}`}
            onClick={() => handleModeChange('favorites')}
          >
            <div className="nav-bar"></div>
            <div className="nav-label">
              <span className="nav-title">Избранное</span>
              <span className="nav-subtitle">{user ? favorites.length : 0} вакансий</span>
            </div>
          </div>

          <div
            className={`nav-item ${mode === 'recommendations' ? 'active' : ''}`}
            onClick={() => handleModeChange('recommendations')}
          >
            <div className="nav-bar"></div>
            <div className="nav-label">
              <span className="nav-title">Рекомендации</span>
              <span className="nav-subtitle">AI-подбор</span>
            </div>
          </div>
        </nav>

        {/* Футер сайдбара */}
        {user && (
          <div className="sidebar-footer">
            <div
              className={`user-profile-btn ${mode === 'profile' ? 'active' : ''}`}
              onClick={() => handleModeChange('profile')}
            >
              <div className="user-avatar">
                {user.email[0].toUpperCase()}
              </div>
              <div className="user-profile-label">
                <span className="user-profile-title">Профиль</span>
                <span className="user-profile-email">{user.email}</span>
              </div>
            </div>
            <button className="logout-btn" onClick={logout}>
              Выйти
            </button>
          </div>
        )}

        {!user && (
          <div className="sidebar-footer">
            <button
              className="login-btn"
              onClick={() => setShowLoginModal(true)}
              title="Войти"
            >
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

      {/* ====== ОСНОВНАЯ ОБЛАСТЬ ====== */}
      <div className="main-area">
        <header className="top-header">
          <h2 className="page-title">
            {mode === 'browse' && 'Все вакансии'}
            {mode === 'swipe' && 'Свайпай вакансии'}
            {mode === 'favorites' && 'Избранные вакансии'}
            {mode === 'profile' && 'Профиль'}
            {mode === 'recommendations' && 'AI-рекомендации'}
          </h2>
        </header>

        <div className="content">
          {/* ====== РЕЖИМ ПРОСМОТРА ====== */}
          {mode === 'browse' && (
            <>
              <VacancyList
                vacancies={vacancies}
                selectedVacancy={selectedVacancy}
                onSelect={setSelectedVacancy}
                isFavorite={isFavorite}
              />
              <main className="vacancy-detail">
                <VacancyDetail
                  vacancy={selectedVacancy}
                  isFavorite={isFavorite}
                  onAddFavorite={addToFavorites}
                  onRemoveFavorite={removeFromFavorites}
                />
              </main>
            </>
          )}

          {/* ====== РЕЖИМ СВАЙПОВ ====== */}
          {mode === 'swipe' && user && (
            <div className="swipe-container">
              <div className="card-container">
                {vacancies.length === 0 ? (
                  <div className="empty-state">
                    <h2>Вакансий пока нет</h2>
                    <p>Создай первую вакансию через API</p>
                  </div>
                ) : currentIndex < 0 ? (
                  <div className="empty-state">
                    <h2>Вакансии закончились</h2>
                    <p>Ты просмотрел все {vacancies.length} вакансий</p>
                    <button className="btn btn-primary" onClick={loadVacancies}>
                      Начать заново
                    </button>
                  </div>
                ) : (
                  vacancies.slice(0, currentIndex + 1).map((vacancy, index) => (
                    <SwipeableCard
                      key={vacancy.id}
                      vacancy={vacancy}
                      onSwipe={handleSwipe}
                      isTop={index === currentIndex}
                      matchScore={matchScores[vacancy.id]}
                    />
                  ))
                )}
              </div>
            </div>
          )}

          {/* ====== РЕЖИМ ИЗБРАННОГО ====== */}
          {mode === 'favorites' && user && (
            <div className="favorites-container">
              <FavoritesList
                favorites={favorites}
                onRemove={removeFromFavorites}
              />
            </div>
          )}

          {/* ====== РЕЖИМ ПРОФИЛЯ ====== */}
          {mode === 'profile' && user && (
            <ProfilePage onResumeUpdate={(r) => {
              setResume(r)
              setRecommendationsLoaded(false)
            }} />
          )}

          {/* ====== РЕЖИМ РЕКОМЕНДАЦИЙ ====== */}
          {mode === 'recommendations' && user && (
            <RecommendationsPage
              recommendations={recommendations}
              selectedVacancy={selectedRecommendedVacancy}
              onSelectVacancy={setSelectedRecommendedVacancy}
              loading={recommendationsLoading}
              onRefresh={() => loadRecommendations(true)}
            />
          )}
        </div>
      </div>
    </div>
  )
}

export default App