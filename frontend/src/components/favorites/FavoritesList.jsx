function FavoritesList({ favorites, onRemove, onOpenVacancy }) {
    if (favorites.length === 0) {
        return (
            <div className="empty-state">
                <h2>Нет избранных вакансий</h2>
                <p>Свайпай вправо или добавляй вакансии в избранное</p>
            </div>
        )
    }

    return (
        <div className="favorites-grid">
            {favorites.map(fav => (
                <div
                    key={fav.id}
                    className="favorite-card"
                    onClick={() => onOpenVacancy && onOpenVacancy(fav.id)}
                    title="Открыть вакансию"
                >
                    {/* Кнопка удаления — абсолютно позиционирована */}
                    <button
                        className="favorite-remove-btn"
                        onClick={(e) => {
                            e.stopPropagation()
                            onRemove(fav.id)
                        }}
                        title="Удалить из избранного"
                    >
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <line x1="18" y1="6" x2="6" y2="18" />
                            <line x1="6" y1="6" x2="18" y2="18" />
                        </svg>
                    </button>

                    <div className="favorite-card-content">
                        <h3>{fav.title}</h3>
                        <div className="favorite-company">{fav.company}</div>

                        <div className="favorite-meta">
                            <span className="favorite-location">
                                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
                                    <circle cx="12" cy="10" r="3"/>
                                </svg>
                                {fav.location}
                            </span>
                            <span className="favorite-experience">{fav.experience}</span>
                            {fav.remote && <span className="badge remote">Удалённо</span>}
                        </div>

                        {fav.skills && fav.skills.length > 0 && (
                            <div className="favorite-skills">
                                {fav.skills.slice(0, 4).map((skill, i) => (
                                    <span key={i} className="skill-tag">{skill}</span>
                                ))}
                                {fav.skills.length > 4 && (
                                    <span className="skill-tag more">+{fav.skills.length - 4}</span>
                                )}
                            </div>
                        )}

                        {fav.description && (
                            <p className="favorite-description">
                                {fav.description.length > 120
                                    ? fav.description.substring(0, 120) + '...'
                                    : fav.description}
                            </p>
                        )}
                    </div>
                </div>
            ))}
        </div>
    )
}

export default FavoritesList