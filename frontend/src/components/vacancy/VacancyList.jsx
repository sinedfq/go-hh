import './VacancyList.css'

function VacancyList({ vacancies, selectedVacancy, onSelect, isFavorite, onOpenVacancy }) {
    return (
        <aside className="vacancy-list">
            <h2>Все вакансии</h2>
            <div className="vacancy-items">
                {vacancies.map((vacancy, index) => (
                    <div
                        key={vacancy.id}
                        className={`vacancy-item animate-in ${selectedVacancy?.id === vacancy.id ? 'active' : ''}`}
                        style={{ animationDelay: `${index * 40}ms` }}
                        onClick={() => onSelect(vacancy)}
                    >
                        <div className="vacancy-item-top">
                            <div className="vacancy-item-title">{vacancy.title}</div>
                            {isFavorite?.(vacancy.id) && <span className="favorite-badge">★</span>}
                        </div>
                        <div className="vacancy-item-company">{vacancy.company}</div>
                        <div className="vacancy-item-meta">
                            <span className="meta-location">
                                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
                                    <circle cx="12" cy="10" r="3" />
                                </svg>
                                {vacancy.location || 'Не указан'}
                            </span>
                            <span className="meta-views">
                                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                                    <circle cx="12" cy="12" r="3" />
                                </svg>
                                {vacancy.views || 0}
                            </span>
                        </div>
                        {onOpenVacancy && (
                            <button
                                className="vacancy-item-apply-btn"
                                onClick={(e) => {
                                    e.stopPropagation()
                                    onOpenVacancy(vacancy.id)
                                }}
                            >
                                Открыть
                            </button>
                        )}
                    </div>
                ))}
            </div>
        </aside>
    )
}

export default VacancyList