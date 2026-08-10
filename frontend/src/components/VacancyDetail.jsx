function VacancyDetail({ vacancy, isFavorite, onAddFavorite, onRemoveFavorite }) {
  if (!vacancy) {
    return (
      <div className="empty-state">
        <h2>Выберите вакансию</h2>
        <p>Выберите вакансию из списка слева</p>
      </div>
    )
  }

  return (
    <div className="detail-card">
      <div className="detail-header">
        <h2>{vacancy.title}</h2>
        <div className="company">{vacancy.company}</div>
      </div>

      <div className="detail-meta">
        <div className="meta-item">
          <span className="meta-label">Локация</span>
          <span className="meta-value">
            {vacancy.city}
            {vacancy.remote && <span className="badge remote">Удалённо</span>}
          </span>
        </div>
        <div className="meta-item">
          <span className="meta-label">Опыт</span>
          <span className="meta-value">{vacancy.experience}</span>
        </div>
      </div>

      {vacancy.skills && vacancy.skills.length > 0 && (
        <div className="detail-section">
          <h3>Требуемые навыки</h3>
          <div className="skills">
            {vacancy.skills.map((skill, i) => (
              <span key={i} className="skill-tag">{skill}</span>
            ))}
          </div>
        </div>
      )}

      {vacancy.description && (
        <div className="detail-section">
          <h3>Описание</h3>
          <p className="description">{vacancy.description}</p>
        </div>
      )}

      <div className="detail-actions">
        {!isFavorite(vacancy.id) ? (
          <button 
            className="btn btn-primary"
            onClick={() => onAddFavorite(vacancy.id)}
          >
            Добавить в избранное
          </button>
        ) : (
          <button 
            className="btn btn-secondary"
            onClick={() => onRemoveFavorite(vacancy.id)}
          >
            Убрать из избранного
          </button>
        )}
      </div>
    </div>
  )
}

export default VacancyDetail