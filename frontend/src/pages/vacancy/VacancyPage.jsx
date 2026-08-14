import { useState, useEffect } from 'react'
import axios from 'axios'
import './VacancyPage.css'
import YandexMap from '../../components/common/YandexMap'

function VacancyPage({ vacancyId, onClose, onOpenCompany }) {
    const [vacancy, setVacancy] = useState(null)
    const [company, setCompany] = useState(null)
    const [loading, setLoading] = useState(true)
    const [isFavorite, setIsFavorite] = useState(false)

    useEffect(() => {
        loadVacancy()
    }, [vacancyId])

    const loadVacancy = async () => {
        try {
            setLoading(true)
            const res = await axios.get(`/api/vacancies?id=${vacancyId}`)
            setVacancy(res.data)

            if (res.data.company_id) {
                try {
                    const companyRes = await axios.get(`/api/companies/${res.data.company_id}`)
                    setCompany(companyRes.data)
                } catch (e) {
                    console.error('Не удалось загрузить компанию:', e)
                }
            }
        } catch (err) {
            console.error('Ошибка загрузки вакансии:', err)
        } finally {
            setLoading(false)
        }
    }

    if (loading) {
        return (
            <div className="vacancy-page">
                <div className="loading">
                    <div className="spinner"></div>
                    <p>Загрузка вакансии...</p>
                </div>
            </div>
        )
    }

    if (!vacancy) {
        return (
            <div className="vacancy-page">
                <div className="empty-state">
                    <h2>Вакансия не найдена</h2>
                    <button className="btn btn-primary" onClick={onClose}>Назад</button>
                </div>
            </div>
        )
    }

    // ⬇️ ЭТО КЛЮЧЕВАЯ СТРОКА — должна быть перед основным return
    const hasCoordinates = vacancy.latitude && vacancy.longitude &&
                           vacancy.latitude !== 0 && vacancy.longitude !== 0

    return (
        <div className="vacancy-page">
            <button className="back-btn" onClick={onClose}>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <polyline points="15 18 9 12 15 6" />
                </svg>
                Назад
            </button>

            <div className="vacancy-page-layout">
                {/* ====== ЛЕВАЯ КОЛОНКА ====== */}
                <div className="vacancy-main">
                    <div className="vacancy-header-card">
                        <div className="vacancy-title-row">
                            <h1>{vacancy.title}</h1>
                            {vacancy.remote && <span className="badge remote-badge">Удалённо</span>}
                        </div>

                        <div className="vacancy-company-row">
                            {company ? (
                                <button
                                    className="company-link"
                                    onClick={() => onOpenCompany && onOpenCompany(company.id)}
                                >
                                    {company.name}
                                </button>
                            ) : (
                                <span className="company-name">{vacancy.company}</span>
                            )}
                        </div>

                        <div className="vacancy-meta-badges">
                            <span className="meta-badge">
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
                                    <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
                                </svg>
                                {vacancy.experience}
                            </span>
                            <span className="meta-badge">
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
                                    <circle cx="12" cy="10" r="3" />
                                </svg>
                                {vacancy.location}
                            </span>
                        </div>
                    </div>

                    {vacancy.description && (
                        <div className="vacancy-section-card">
                            <h3>Описание</h3>
                            <p className="vacancy-description">{vacancy.description}</p>
                        </div>
                    )}

                    {vacancy.skills && vacancy.skills.length > 0 && (
                        <div className="vacancy-section-card">
                            <h3>Требуемые навыки</h3>
                            <div className="vacancy-skills">
                                {vacancy.skills.map((skill, i) => (
                                    <span key={i} className="skill-tag">{skill}</span>
                                ))}
                            </div>
                        </div>
                    )}
                </div>

                {/* ====== ПРАВАЯ КОЛОНКА ====== */}
                <div className="vacancy-sidebar">
                    <div className="vacancy-location-card">
                        <h3>Расположение</h3>

                        {hasCoordinates ? (
                            <>
                                <YandexMap
                                    latitude={vacancy.latitude}
                                    longitude={vacancy.longitude}
                                    title={vacancy.title}
                                    address={vacancy.address || vacancy.location}
                                />
                                <p className="vacancy-address">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
                                        <circle cx="12" cy="10" r="3" />
                                    </svg>
                                    {vacancy.address || vacancy.location}
                                </p>
                            </>
                        ) : (
                            <div className="no-map">
                                <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                                    <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
                                    <circle cx="12" cy="10" r="3" />
                                </svg>
                                <p className="location-text">
                                    {vacancy.address || vacancy.location}
                                </p>
                                <p className="location-hint">Точное расположение не указано</p>
                            </div>
                        )}
                    </div>

                    <div className="vacancy-stats-card">
                        <div className="stat-row">
                            <span className="stat-label">Просмотры</span>
                            <span className="stat-value">{vacancy.views || 0}</span>
                        </div>
                    </div>

                    <div className="vacancy-actions-card">
                        <button className="btn btn-primary btn-block">
                            Откликнуться
                        </button>
                        <button className="btn btn-secondary btn-block">
                            В избранное
                        </button>
                    </div>
                </div>
            </div>
        </div>
    )
}

export default VacancyPage
