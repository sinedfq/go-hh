import VacancyDetail from '../../components/vacancy/VacancyDetail'
import './RecommendationsPage.css'

function RecommendationsPage({ recommendations, selectedVacancy, onSelectVacancy, loading, onRefresh, onOpenVacancy }) {
  const getScoreColor = (score) => {
    if (score >= 0.7) return '#2e7d32'
    if (score >= 0.4) return '#f57c00'
    return '#c62828'
  }

  const getScoreLabel = (score) => {
    if (score >= 0.8) return 'Отличное совпадение'
    if (score >= 0.6) return 'Хорошее совпадение'
    if (score >= 0.4) return 'Частичное совпадение'
    return 'Слабое совпадение'
  }

  const currentRecommendation = recommendations?.find(
    r => r.vacancy.id === selectedVacancy?.id
  )

  if (loading && recommendations.length === 0) {
    return (
      <div className="recommendations-page">
        <div className="loading">
          <div className="spinner"></div>
          <p>AI анализирует вакансии под ваше резюме...</p>
          <p className="loading-subtitle">Это может занять несколько секунд</p>
        </div>
      </div>
    )
  }

  if (!recommendations || recommendations.length === 0) {
    return (
      <div className="recommendations-page">
        <div className="empty-recommendations">
          <div className="empty-icon">🤖</div>
          <h2>Рекомендации недоступны</h2>
          <p>
            Создайте резюме в профиле, чтобы получить персональные рекомендации от AI
          </p>
          <button className="btn btn-primary" onClick={onRefresh}>
            Обновить
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="recommendations-page">
      <div className="recommendations-layout">
        {/* Список рекомендаций */}
        <div className="recommendations-list">
          <div className="recommendations-header">
            <div>
              <h2>AI-рекомендации</h2>
              <p className="recommendations-count">
                {recommendations.length} вакансий
              </p>
            </div>
            <button
              className="refresh-btn"
              onClick={onRefresh}
              title="Обновить рекомендации"
              disabled={loading}
            >
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                className={loading ? 'spinning' : ''}
              >
                <path d="M21 12a9 9 0 11-6.219-8.56" />
                <polyline points="21 3 21 9 15 9" />
              </svg>
            </button>
          </div>

          <div className="recommendation-items">
            {recommendations.map((rec) => (
              <div
                key={rec.vacancy.id}
                className={`recommendation-item ${selectedVacancy?.id === rec.vacancy.id ? 'active' : ''}`}
                onClick={() => onSelectVacancy(rec.vacancy)}
              >
                <div className="recommendation-score">
                  <div
                    className="score-circle"
                    style={{ '--score-color': getScoreColor(rec.score) }}
                  >
                    {Math.round(rec.score * 100)}%
                  </div>
                </div>

                <div className="recommendation-info">
                  <h3>{rec.vacancy.title}</h3>
                  <p className="recommendation-company">{rec.vacancy.company}</p>
                  <p className="recommendation-label" style={{ color: getScoreColor(rec.score) }}>
                    {getScoreLabel(rec.score)}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Детали вакансии */}
        <div className="recommendation-detail">
          {selectedVacancy ? (
            <>
              <VacancyDetail
                vacancy={selectedVacancy}
                isFavorite={() => false}
                onAddFavorite={() => { }}
                onRemoveFavorite={() => { }}
                onOpenVacancy={onOpenVacancy}
              />

              {currentRecommendation && (
                <div className="ai-reasoning">
                  <div className="ai-reasoning-header">
                    <div className="ai-badge">
                      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M12 2a5 5 0 0 1 5 5v3a5 5 0 0 1-10 0V7a5 5 0 0 1 5-5z" />
                        <path d="M19 10v2a7 7 0 0 1-14 0v-2" />
                        <line x1="12" y1="19" x2="12" y2="23" />
                        <line x1="8" y1="23" x2="16" y2="23" />
                      </svg>
                      <span>AI-анализ</span>
                    </div>
                    <div className="ai-score-badge" style={{ '--score-color': getScoreColor(currentRecommendation.score) }}>
                      Совпадение: {Math.round(currentRecommendation.score * 100)}%
                    </div>
                  </div>

                  <h3>Почему эта вакансия подходит</h3>
                  <p>{currentRecommendation.reasoning}</p>

                  <div className="ai-footer">
                    <span className="ai-model-info">
                      Powered by ML Service
                    </span>
                  </div>
                </div>
              )}
            </>
          ) : (
            <div className="no-selection">
              <div className="no-selection-icon">👈</div>
              <p>Выберите вакансию слева для просмотра деталей</p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

export default RecommendationsPage