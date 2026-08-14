import { useState, useEffect } from 'react'
import axios from 'axios'

function CompanyDetails({ companyId, onBack, onSelectVacancy }) {
    const [company, setCompany] = useState(null)
    const [vacancies, setVacancies] = useState([])
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        loadCompanyData()
    }, [companyId])

    const loadCompanyData = async () => {
        try {
            setLoading(true)
            const [companyRes, vacanciesRes] = await Promise.all([
                axios.get(`/api/companies/${companyId}`),
                axios.get(`/api/companies/${companyId}/vacancies`)
            ])
            setCompany(companyRes.data)
            setVacancies(vacanciesRes.data || [])
        } catch (err) {
            console.error('Ошибка загрузки компании:', err)
        } finally {
            setLoading(false)
        }
    }

    if (loading) {
        return (
            <div className="company-details">
                <div className="loading">
                    <div className="spinner"></div>
                    <p>Загрузка компании...</p>
                </div>
            </div>
        )
    }

    if (!company) {
        return (
            <div className="company-details">
                <div className="empty-state">
                    <h2>Компания не найдена</h2>
                    <button className="btn btn-primary" onClick={onBack}>Назад к списку</button>
                </div>
            </div>
        )
    }

    return (
        <div className="company-details">
            <button className="back-btn" onClick={onBack}>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <polyline points="15 18 9 12 15 6" />
                </svg>
                Назад к компаниям
            </button>

            {/* Шапка компании */}
            <div className="company-header-card">
                <div className="company-logo-large">
                    {company.logo_url ? (
                        <img src={company.logo_url} alt={company.name} />
                    ) : (
                        <span>{company.name.charAt(0).toUpperCase()}</span>
                    )}
                </div>
                <div className="company-header-info">
                    <h1>{company.name}</h1>
                    <div className="company-meta-badges">
                        {company.industry && <span className="meta-badge">{company.industry}</span>}
                        {company.size && <span className="meta-badge">{company.size}</span>}
                        {company.city && <span className="meta-badge">{company.city}</span>}
                    </div>
                    {company.website && (
                        <a
                            href={company.website.startsWith('http') ? company.website : `https://${company.website}`}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="company-website"
                        >
                            {company.website.replace(/^https?:\/\//, '')}
                        </a>
                    )}
                </div>
                <div className="company-header-stats">
                    <div className="header-stat">
                        <span className="stat-value">{company.vacancies_count}</span>
                        <span className="stat-label">вакансий</span>
                    </div>
                    <div className="header-stat">
                        <span className="stat-value">{company.total_views}</span>
                        <span className="stat-label">просмотров</span>
                    </div>
                </div>
            </div>

            {/* Описание компании */}
            {company.description && (
                <div className="company-description-card">
                    <h3>О компании</h3>
                    <p>{company.description}</p>
                </div>
            )}

            {/* Вакансии компании */}
            <div className="company-vacancies-section">
                <h3>
                    Вакансии компании
                    <span className="vacancies-count-badge">{vacancies.length}</span>
                </h3>

                {vacancies.length === 0 ? (
                    <div className="empty-vacancies">
                        <p>У компании пока нет активных вакансий</p>
                    </div>
                ) : (
                    <div className="company-vacancies-grid">
                        {vacancies.map(vacancy => (
                            <div
                                key={vacancy.id}
                                className="company-vacancy-card clickable"
                                onClick={() => onSelectVacancy && onSelectVacancy(vacancy.id)}
                            >
                                <div className="vacancy-card-top">
                                    <h4>{vacancy.title}</h4>
                                    <div className="vacancy-badges">
                                        <span className="badge">{vacancy.experience}</span>
                                        {vacancy.remote && <span className="badge remote">Удалённо</span>}
                                    </div>
                                </div>

                                <div className="vacancy-card-location">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
                                        <circle cx="12" cy="10" r="3"/>
                                    </svg>
                                    {vacancy.location}
                                </div>

                                {vacancy.skills && vacancy.skills.length > 0 && (
                                    <div className="vacancy-card-skills">
                                        {vacancy.skills.slice(0, 5).map((skill, i) => (
                                            <span key={i} className="skill-tag">{skill}</span>
                                        ))}
                                        {vacancy.skills.length > 5 && (
                                            <span className="skill-tag more">+{vacancy.skills.length - 5}</span>
                                        )}
                                    </div>
                                )}

                                <div className="vacancy-card-footer">
                                    <span className="vacancy-views">
                                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                                            <circle cx="12" cy="12" r="3"/>
                                        </svg>
                                        {vacancy.views || 0}
                                    </span>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    )
}

export default CompanyDetails