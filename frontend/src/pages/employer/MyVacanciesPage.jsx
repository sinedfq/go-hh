import { useEffect } from 'react'
import axios from 'axios'
import './EmployerPages.css'

function MyVacanciesPage({ myCompany, myVacancies, employerLoading, employerError, onOpenVacancy, onCreateVacancy, onReload }) {

    useEffect(() => {
        if (!myCompany) {
            onReload()
        }
    }, [myCompany, onReload])

    if (employerLoading) {
        return (
            <div className="employer-page">
                <div className="loading">
                    <div className="spinner"></div>
                    <p>Загрузка...</p>
                </div>
            </div>
        )
    }

    if (!myCompany) {
        return (
            <div className="employer-page">
                <div className="employer-empty">
                    <div className="employer-empty-icon">
                        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
                            <polyline points="9 22 9 12 15 12 15 22" />
                        </svg>
                    </div>
                    <h2>Сначала создайте компанию</h2>
                    <p>Чтобы публиковать вакансии, нужно создать профиль вашей компании</p>
                    <button className="btn btn-primary" onClick={onCreateVacancy}>
                        Создать компанию
                    </button>
                </div>
            </div>
        )
    }

    return (
        <div className="employer-page">
            <div className="employer-header">
                <div>
                    <h1>Мои вакансии</h1>
                    <p className="employer-subtitle">{myCompany.name} • {myVacancies.length} {myVacancies.length === 1 ? 'вакансия' : myVacancies.length < 5 ? 'вакансии' : 'вакансий'}</p>
                </div>
                <button className="btn btn-primary" onClick={onCreateVacancy}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <line x1="12" y1="5" x2="12" y2="19" />
                        <line x1="5" y1="12" x2="19" y2="12" />
                    </svg>
                    Создать вакансию
                </button>
            </div>

            {employerError && (
                <div className="employer-error">
                    {employerError}
                </div>
            )}

            {myVacancies.length === 0 ? (
                <div className="employer-empty-small">
                    <p>У вас пока нет вакансий</p>
                    <button className="btn btn-primary" onClick={onCreateVacancy}>
                        Создать первую вакансию
                    </button>
                </div>
            ) : (
                <div className="my-vacancies-grid">
                    {myVacancies.map(v => (
                        <div key={v.id} className="my-vacancy-card" onClick={() => onOpenVacancy(v.id)}>
                            <div className="my-vacancy-header">
                                <h3>{v.title}</h3>
                                <span className="vacancy-views">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                                        <circle cx="12" cy="12" r="3" />
                                    </svg>
                                    {v.views || 0}
                                </span>
                            </div>
                            <div className="my-vacancy-meta">
                                <span>{v.location}</span>
                                <span>•</span>
                                <span>{v.experience}</span>
                                {v.remote && (
                                    <>
                                        <span>•</span>
                                        <span className="remote-badge-mini">Удалённо</span>
                                    </>
                                )}
                            </div>
                            {v.skills && v.skills.length > 0 && (
                                <div className="my-vacancy-skills">
                                    {v.skills.slice(0, 4).map((skill, i) => (
                                        <span key={i} className="skill-tag-mini">{skill}</span>
                                    ))}
                                    {v.skills.length > 4 && (
                                        <span className="skill-tag-mini more">+{v.skills.length - 4}</span>
                                    )}
                                </div>
                            )}
                        </div>
                    ))}
                </div>
            )}
        </div>
    )
}

export default MyVacanciesPage