function VacancyDetail({ vacancy, isFavorite, onAddFavorite, onRemoveFavorite, onOpenVacancy, onOpenCompany }) {
  if (!vacancy) {
    return (
      <div className="empty-state">
        <h2>Выберите вакансию</h2>
        <p>Выберите вакансию из списка слева</p>
      </div>
    )
  }

  return (
    <div
      className="detail-card clickable"
      onClick={() => onOpenVacancy && onOpenVacancy(vacancy.id)}
      title="Открыть страницу вакансии"
    >
      <div className="detail-header">
        <h2>{vacancy.title}</h2>
        <div
          className="company"
          onClick={(e) => {
            e.stopPropagation()
            if (vacancy.company_id && onOpenCompany) {
              onOpenCompany(vacancy.company_id)
            }
          }}
          style={{
            cursor: vacancy.company_id ? 'pointer' : 'default',
            color: vacancy.company_id ? '#0066cc' : 'inherit'
          }}
          title={vacancy.company_id ? 'Открыть страницу компании' : ''}
        >
          {vacancy.company}
        </div>
      </div>

      <div className="detail-meta">
        <div className="meta-item">
          <span className="meta-label">Локация</span>
          <span className="meta-value">
            {vacancy.location}
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
            onClick={(e) => {
              e.stopPropagation()
              onAddFavorite(vacancy.id)
            }}
          >
            Добавить в избранное
          </button>
        ) : (
          <button
            className="btn btn-secondary"
            onClick={(e) => {
              e.stopPropagation()
              onRemoveFavorite(vacancy.id)
            }}
          >
            Убрать из избранного
          </button>
        )}
      </div>
    </div>
  )
}

export default VacancyDetail