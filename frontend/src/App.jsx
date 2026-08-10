import { useState, useEffect } from 'react'
import axios from 'axios'
import SwipeableCard from './components/SwipeableCard'
import VacancyList from './components/VacancyList'
import VacancyDetail from './components/VacancyDetail'
import FavoritesList from './components/FavoritesList'
import './App.css'

function App() {
  const [vacancies, setVacancies] = useState([])
  const [favorites, setFavorites] = useState([])
  const [currentIndex, setCurrentIndex] = useState(-1)
  const [selectedVacancy, setSelectedVacancy] = useState(null)
  const [loading, setLoading] = useState(true)
  const [mode, setMode] = useState('browse')

  useEffect(() => {
    loadVacancies()
    loadFavorites()
  }, [])

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

  const addToFavorites = async (vacancyId) => {
    try {
      await axios.post('/api/favorites', { vacancy_id: vacancyId })
      loadFavorites()
    } catch (err) {
      console.error('Ошибка добавления в избранное:', err)
    }
  }

  const removeFromFavorites = async (vacancyId) => {
    try {
      await axios.delete(`/api/favorites/${vacancyId}`)
      loadFavorites()
    } catch (err) {
      console.error('Ошибка удаления из избранного:', err)
    }
  }

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

  if (loading) {
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
      {/* Левый сайдбар с навигацией */}
      <aside className="sidebar">
        <div className="sidebar-logo">
          <h1>Go</h1>
          <h1>Work!</h1>
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
            onClick={() => setMode('swipe')}
          >
            <div className="nav-bar"></div>
            <div className="nav-label">
              <span className="nav-title">Свайпы</span>
              <span className="nav-subtitle">Как в Tinder</span>
            </div>
          </div>

          <div
            className={`nav-item ${mode === 'favorites' ? 'active' : ''}`}
            onClick={() => setMode('favorites')}
          >
            <div className="nav-bar"></div>
            <div className="nav-label">
              <span className="nav-title">Избранное</span>
              <span className="nav-subtitle">{favorites.length} вакансий</span>
            </div>
          </div>
        </nav>
      </aside>

      {/* Основная область */}
      <div className="main-area">
        {/* Верхняя шапка — только название */}
        <header className="top-header">
          <h2 className="page-title">
            {mode === 'browse' && 'Все вакансии'}
            {mode === 'swipe' && 'Свайпай вакансии'}
            {mode === 'favorites' && 'Избранные вакансии'}
          </h2>
        </header>

        <div className="content">
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

          {mode === 'swipe' && (
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
                    />
                  ))
                )}
              </div>
            </div>
          )}

          {mode === 'favorites' && (
            <div className="favorites-container">
              <FavoritesList
                favorites={favorites}
                onRemove={removeFromFavorites}
              />
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

export default App