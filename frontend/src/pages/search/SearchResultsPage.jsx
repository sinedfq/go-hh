import { useState, useEffect } from 'react'
import SearchBar from '../../components/vacancy/SearchBar'
import FilterPanel from '../../components/vacancy/FilterPanel'
import ApplicationModal from '../../components/common/ApplicationModal'
import axios from 'axios'
import './SearchResultsPage.css'

function SearchResultsPage({
    results,
    totalCount,
    loading,
    filters,
    onFiltersChange,
    onApply,
    onReset,
    onOpenVacancy,
    cities,
    showToast
}) {
    const [showFilters, setShowFilters] = useState(false)
    const [applyVacancy, setApplyVacancy] = useState(null)
    const [appliedVacancies, setAppliedVacancies] = useState(new Set())  // ✅ НОВОЕ

    // ====== ПРОВЕРКА СТАТУСА ОТКЛИКОВ ======
    useEffect(() => {
        if (!results || results.length === 0) return
        
        const checkAppliedStatuses = async () => {
            try {
                // Загружаем все отклики пользователя
                const res = await axios.get('/api/my-applications')
                const apps = Array.isArray(res.data) ? res.data : []
                const appliedIds = new Set(apps.map(app => app.vacancy_id))
                setAppliedVacancies(appliedIds)
            } catch (err) {
                console.error('Ошибка проверки откликов:', err)
            }
        }
        
        checkAppliedStatuses()
    }, [results])

    useEffect(() => {
        if (results.length === 0 && !loading) {
            onApply(filters)
        }
    }, [])

    const truncate = (text, limit = 150) => {
        if (!text) return 'Без описания'
        return text.length > limit ? text.slice(0, limit) + '...' : text
    }

    const handleApplySuccess = (data) => {
        // ✅ ДОБАВЛЕНО: обновляем список откликнутых вакансий
        if (applyVacancy) {
            setAppliedVacancies(prev => new Set([...prev, applyVacancy.id]))
        }
        setApplyVacancy(null)
        if (showToast) {
            showToast(`✨ Отклик отправлен! Компания скоро свяжется с вами`)
        }
    }

    return (
        <div className="search-results-container animate-fade-in">
            {/* Панель поиска и фильтров */}
            <div className="search-header">
                <div className="search-bar-wrapper">
                    <SearchBar
                        value={filters.query || ''}
                        onChange={(query) => onFiltersChange({ ...filters, query })}
                        placeholder="Поиск по названию, компании или навыкам..."
                    />
                    <button
                        className={`filter-toggle-button ${showFilters ? 'active' : ''}`}
                        onClick={() => setShowFilters(!showFilters)}
                    >
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <line x1="4" y1="21" x2="4" y2="14" />
                            <line x1="4" y1="10" x2="4" y2="3" />
                            <line x1="12" y1="21" x2="12" y2="12" />
                            <line x1="12" y1="8" x2="12" y2="3" />
                            <line x1="20" y1="21" x2="20" y2="16" />
                            <line x1="20" y1="12" x2="20" y2="3" />
                            <line x1="1" y1="14" x2="7" y2="14" />
                            <line x1="9" y1="8" x2="15" y2="8" />
                            <line x1="17" y1="16" x2="23" y2="16" />
                        </svg>
                        Фильтры
                    </button>
                </div>

                {showFilters && (
                    <div className="filter-panel-wrapper animate-slide-down">
                        <FilterPanel
                            filters={filters}
                            onFiltersChange={onFiltersChange}
                            onApply={() => onApply(filters)}
                            onReset={onReset}
                            cities={cities}
                        />
                    </div>
                )}
            </div>

            {/* Счётчик результатов */}
            <div className="results-count animate-fade-in-delayed">
                {loading ? (
                    <span>Ищем вакансии...</span>
                ) : (
                    <>
                        <span className="count-number">{totalCount}</span>
                        <span className="count-text">
                            {totalCount === 1 ? 'вакансия найдена' :
                                totalCount > 1 && totalCount < 5 ? 'вакансии найдены' :
                                    'вакансий найдено'}
                        </span>
                    </>
                )}
            </div>

            {/* Результаты поиска */}
            {loading ? (
                <div className="loading">
                    <div className="spinner"></div>
                </div>
            ) : results.length === 0 ? (
                <div className="empty-state">
                    <h2>Ничего не найдено</h2>
                    <p>Попробуйте изменить параметры поиска</p>
                    <button className="btn btn-secondary" onClick={onReset}>
                        Сбросить фильтры
                    </button>
                </div>
            ) : (
                <div className="search-results-grid">
                    {results.map((vacancy, index) => {
                        const hasApplied = appliedVacancies.has(vacancy.id)  // ✅ ПРОВЕРКА
                        
                        return (
                            <div
                                key={vacancy.id}
                                className="search-result-card animate-search-result"
                                style={{ animationDelay: `${index * 100}ms` }}
                                onClick={() => onOpenVacancy(vacancy.id)}
                            >
                                <h3 className="result-card-title">{vacancy.title}</h3>
                                <div className="result-card-company">{vacancy.company}</div>

                                <div className="result-card-meta">
                                    <span className="meta-item">
                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
                                            <circle cx="12" cy="10" r="3" />
                                        </svg>
                                        {vacancy.location || 'Не указан'}
                                    </span>
                                    <span className="meta-item">
                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
                                            <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
                                        </svg>
                                        {vacancy.experience}
                                    </span>
                                </div>

                                <div className="result-card-skills">
                                    {vacancy.skills.slice(0, 4).map((skill, i) => (
                                        <span key={i} className="skill-tag">{skill}</span>
                                    ))}
                                    {vacancy.skills.length > 4 && (
                                        <span className="skill-tag skill-more">+{vacancy.skills.length - 4}</span>
                                    )}
                                </div>

                                {vacancy.description && (
                                    <p className="result-card-description">
                                        {truncate(vacancy.description)}
                                    </p>
                                )}

                                <div className="result-card-footer">
                                    <span className="views-count" title="Просмотров">
                                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                                            <circle cx="12" cy="12" r="3" />
                                        </svg>
                                        <span className="views-count-number">{vacancy.views || 0}</span>
                                    </span>

                                    {hasApplied ? (
                                        <div className="applied-badge-small">
                                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                                                <polyline points="20 6 9 17 4 12" />
                                            </svg>
                                            Отклик отправлен
                                        </div>
                                    ) : (
                                        <button
                                            className="btn btn-primary btn-small"
                                            onClick={(e) => {
                                                e.stopPropagation()
                                                setApplyVacancy(vacancy)
                                            }}
                                        >
                                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" />
                                            </svg>
                                            Откликнуться
                                        </button>
                                    )}
                                </div>
                            </div>
                        )
                    })}
                </div>
            )}
            {applyVacancy && (
                <ApplicationModal
                    vacancy={applyVacancy}
                    onClose={() => setApplyVacancy(null)}
                    onSuccess={handleApplySuccess}
                />
            )}
        </div>
    )
}

export default SearchResultsPage