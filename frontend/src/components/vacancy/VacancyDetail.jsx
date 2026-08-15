import './VacancyDetail.css'

function VacancyDetail({ 
    vacancy, 
    isFavorite, 
    onAddFavorite, 
    onRemoveFavorite, 
    onOpenVacancy, 
    onOpenCompany 
}) {
    if (!vacancy) return null

    const isFav = typeof isFavorite === 'function' 
        ? isFavorite(vacancy.id) 
        : isFavorite

    // Клик по карточке — открывает страницу вакансии
    const handleCardClick = () => {
        if (onOpenVacancy) {
            onOpenVacancy(vacancy.id)
        }
    }

    // Клик по кнопкам действий — не открывает страницу вакансии
    const handleActionsClick = (e) => {
        e.stopPropagation()
    }

    return (
        <div 
            className="vacancy-detail-wrapper clickable" 
            onClick={handleCardClick}
        >
            <div className="detail-header">
                <h2>{vacancy.title}</h2>
                {onOpenCompany ? (
                    <span 
                        className="company" 
                        onClick={(e) => {
                            e.stopPropagation()
                            onOpenCompany(vacancy.company_id)
                        }}
                    >
                        {vacancy.company}
                    </span>
                ) : (
                    <span className="company">{vacancy.company}</span>
                )}
            </div>

            <div className="detail-meta">
                <div className="meta-item">
                    <span className="meta-label">Город</span>
                    <span className="meta-value">{vacancy.location || 'Не указан'}</span>
                </div>
                <div className="meta-item">
                    <span className="meta-label">Опыт</span>
                    <span className="meta-value">{vacancy.experience}</span>
                </div>
                <div className="meta-item">
                    <span className="meta-label">Просмотры</span>
                    <span className="meta-value">{vacancy.views || 0}</span>
                </div>
                {vacancy.remote && (
                    <span className="badge remote">Удалённо</span>
                )}
            </div>

            {vacancy.skills && vacancy.skills.length > 0 && (
                <div className="detail-section">
                    <h3>Навыки</h3>
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

            <div className="detail-actions" onClick={handleActionsClick}>
                <button 
                    className="btn btn-primary"
                    onClick={() => onOpenVacancy(vacancy.id)}
                >
                    Открыть страницу
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ marginLeft: '0.5rem' }}>
                        <polyline points="9 18 15 12 9 6" />
                    </svg>
                </button>
                <button 
                    className={`btn ${isFav ? 'btn-success' : 'btn-secondary'}`}
                    onClick={() => {
                        if (isFav) {
                            onRemoveFavorite(vacancy.id)
                        } else {
                            onAddFavorite(vacancy.id)
                        }
                    }}
                >
                    {isFav ? '✓ В избранном' : '♡ В избранное'}
                </button>
            </div>
        </div>
    )
}

export default VacancyDetail