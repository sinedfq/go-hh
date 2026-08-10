function VacancyList({ vacancies, selectedVacancy, onSelect, isFavorite }) {
  return (
    <aside className="vacancy-list">
      <h2>Все вакансии</h2>
      <div className="vacancy-items">
        {vacancies.map(vacancy => (
          <div 
            key={vacancy.id}
            className={`vacancy-item ${selectedVacancy?.id === vacancy.id ? 'active' : ''}`}
            onClick={() => onSelect(vacancy)}
          >
            <div className="vacancy-item-title">{vacancy.title}</div>
            <div className="vacancy-item-company">{vacancy.company}</div>
            <div className="vacancy-item-meta">
              <span>{vacancy.city}</span>
              {isFavorite(vacancy.id) && <span className="favorite-badge">Избранное</span>}
            </div>
          </div>
        ))}
      </div>
    </aside>
  )
}

export default VacancyList