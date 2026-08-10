function FavoritesList({ favorites, onRemove }) {
  if (favorites.length === 0) {
    return (
      <div className="empty-state">
        <h2>Пока нет избранных вакансий</h2>
        <p>Добавляй вакансии в избранное в режиме просмотра или свайпов</p>
      </div>
    )
  }

  return (
    <div className="favorites-grid">
      {favorites.map(vacancy => (
        <div key={vacancy.id} className="favorite-card">
          <div className="favorite-content">
            <h3>{vacancy.title}</h3>
            <div className="company">{vacancy.company}</div>
            <div className="meta">
              <span>{vacancy.city}</span>
              {vacancy.remote && <span className="badge remote">Удалённо</span>}
            </div>
            {vacancy.skills && vacancy.skills.length > 0 && (
              <div className="skills">
                {vacancy.skills.slice(0, 3).map((skill, i) => (
                  <span key={i} className="skill-tag">{skill}</span>
                ))}
                {vacancy.skills.length > 3 && (
                  <span className="skill-tag">+{vacancy.skills.length - 3}</span>
                )}
              </div>
            )}
          </div>
          <button 
            className="btn btn-danger btn-small"
            onClick={() => onRemove(vacancy.id)}
          >
            Удалить
          </button>
        </div>
      ))}
    </div>
  )
}

export default FavoritesList