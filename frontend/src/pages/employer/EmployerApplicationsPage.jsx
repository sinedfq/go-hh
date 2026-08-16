import { useState, useEffect } from 'react'
import axios from 'axios'
import './EmployerPages.css'

function EmployerApplicationsPage({ onOpenVacancy, onOpenResume }) {
    const [applications, setApplications] = useState([])
    const [loading, setLoading] = useState(true)
    const [filter, setFilter] = useState('all') // all / new / viewed / accepted / rejected
    const [updating, setUpdating] = useState(null)

    const loadApplications = async () => {
        try {
            setLoading(true)
            const res = await axios.get('/api/employer/applications')
            setApplications(Array.isArray(res.data) ? res.data : [])
        } catch (err) {
            console.error('Ошибка загрузки откликов:', err)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        loadApplications()
    }, [])

    const updateStatus = async (applicationId, status) => {
        setUpdating(applicationId)
        try {
            await axios.patch(`/api/applications/${applicationId}/status`, { status })
            setApplications(prev => prev.map(app =>
                app.id === applicationId ? { ...app, status } : app
            ))
        } catch (err) {
            console.error('Ошибка обновления статуса:', err)
            alert('Не удалось обновить статус')
        } finally {
            setUpdating(null)
        }
    }

    const getStatusInfo = (status) => {
        switch (status) {
            case 'new':
                return { label: 'Новый', className: 'status-new' }
            case 'viewed':
                return { label: 'Просмотрен', className: 'status-viewed' }
            case 'accepted':
                return { label: 'Принят', className: 'status-accepted' }
            case 'rejected':
                return { label: 'Отклонён', className: 'status-rejected' }
            default:
                return { label: status, className: '', icon: '' }
        }
    }

    const filteredApps = applications.filter(app => {
        if (filter === 'all') return true
        return app.status === filter
    })

    const stats = {
        all: applications.length,
        new: applications.filter(a => a.status === 'new').length,
        viewed: applications.filter(a => a.status === 'viewed').length,
        accepted: applications.filter(a => a.status === 'accepted').length,
        rejected: applications.filter(a => a.status === 'rejected').length,
    }

    const formatDate = (dateString) => {
        const date = new Date(dateString)
        return date.toLocaleDateString('ru-RU', {
            day: 'numeric',
            month: 'short',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        })
    }

    if (loading) {
        return (
            <div className="employer-page">
                <div className="loading">
                    <div className="spinner"></div>
                    <p>Загрузка откликов...</p>
                </div>
            </div>
        )
    }

    return (
        <div className="employer-page">
            <div className="employer-header">
                <div>
                    <h1>Отклики на вакансии</h1>
                    <p className="employer-subtitle">
                        Всего откликов: {applications.length}
                        {stats.new > 0 && <span className="new-count"> • {stats.new} новых</span>}
                    </p>
                </div>
            </div>

            {/* ====== ФИЛЬТРЫ ====== */}
            <div className="applications-filters">
                <button
                    className={`filter-btn ${filter === 'all' ? 'active' : ''}`}
                    onClick={() => setFilter('all')}
                >
                    Все <span className="filter-count">{stats.all}</span>
                </button>
                <button
                    className={`filter-btn ${filter === 'new' ? 'active' : ''}`}
                    onClick={() => setFilter('new')}
                >
                    Новые <span className="filter-count">{stats.new}</span>
                </button>
                <button
                    className={`filter-btn ${filter === 'viewed' ? 'active' : ''}`}
                    onClick={() => setFilter('viewed')}
                >
                    Просмотренные <span className="filter-count">{stats.viewed}</span>
                </button>
                <button
                    className={`filter-btn ${filter === 'accepted' ? 'active' : ''}`}
                    onClick={() => setFilter('accepted')}
                >
                    Принятые <span className="filter-count">{stats.accepted}</span>
                </button>
                <button
                    className={`filter-btn ${filter === 'rejected' ? 'active' : ''}`}
                    onClick={() => setFilter('rejected')}
                >
                    Отклонённые <span className="filter-count">{stats.rejected}</span>
                </button>
            </div>

            {/* ====== СПИСОК ОТКЛИКОВ ====== */}
            {filteredApps.length === 0 ? (
                <div className="applications-empty">
                    <div className="applications-empty-icon">📭</div>
                    <h3>
                        {filter === 'all' ? 'Пока нет откликов' : `Нет ${filter === 'new' ? 'новых ' : ''}откликов`}
                    </h3>
                    <p>
                        {filter === 'all'
                            ? 'Когда кандидаты откликнутся на ваши вакансии, они появятся здесь'
                            : 'Попробуйте выбрать другой фильтр'}
                    </p>
                </div>
            ) : (
                <div className="applications-list">
                    {filteredApps.map(app => {
                        const statusInfo = getStatusInfo(app.status)
                        const isUpdating = updating === app.id

                        return (
                            <div key={app.id} className={`application-card ${statusInfo.className}`}>
                                <div className="application-card-header">
                                    <div className="application-candidate">
                                        <div className="candidate-avatar">
                                            {app.candidate_photo ? (
                                                <img src={app.candidate_photo} alt="" />
                                            ) : (
                                                <div className="avatar-placeholder">
                                                    {(app.resume_full_name || app.candidate_email || '?')[0].toUpperCase()}
                                                </div>
                                            )}
                                        </div>
                                        <div className="candidate-info">
                                            <h3 className="candidate-name">
                                                {app.resume_full_name || app.candidate_email}
                                            </h3>
                                            {app.resume_position && (
                                                <p className="candidate-position">{app.resume_position}</p>
                                            )}
                                            <div className="candidate-meta">
                                                {app.resume_experience && (
                                                    <span className="candidate-experience">
                                                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                            <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
                                                            <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
                                                        </svg>
                                                        {app.resume_experience}
                                                    </span>
                                                )}
                                                <p className="candidate-email">{app.candidate_email}</p>
                                            </div>
                                        </div>
                                    </div>

                                    <div className={`application-status-badge ${statusInfo.className}`}>
                                        <span className="status-icon">{statusInfo.icon}</span>
                                        {statusInfo.label}
                                    </div>
                                </div>

                                <div className="application-vacancy-row">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
                                        <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
                                    </svg>
                                    <button
                                        className="application-vacancy-link"
                                        onClick={() => onOpenVacancy && onOpenVacancy(app.vacancy_id)}
                                    >
                                        {app.vacancy_title}
                                    </button>
                                </div>

                                {app.cover_letter && (
                                    <div className="application-cover">
                                        <div className="cover-label">Сопроводительное письмо:</div>
                                        <p className="cover-text">{app.cover_letter}</p>
                                    </div>
                                )}

                                <div className="application-meta">
                                    <span className="application-date">
                                        {formatDate(app.created_at)}
                                    </span>
                                </div>

                                <div className="application-actions">
                                    {app.resume_id && app.resume_id > 0 ? (
                                        <button
                                            className="btn btn-secondary btn-small"
                                            onClick={() => onOpenResume && onOpenResume(app.resume_id)}
                                        >
                                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                                                <polyline points="14 2 14 8 20 8" />
                                            </svg>
                                            Посмотреть резюме
                                        </button>
                                    ) : (
                                        <span className="no-resume-hint">
                                            Резюме не прикреплено
                                        </span>
                                    )}

                                    {app.status !== 'accepted' && (
                                        <button
                                            className="btn btn-success btn-small"
                                            onClick={() => updateStatus(app.id, 'accepted')}
                                            disabled={isUpdating}
                                        >
                                            {isUpdating ? '...' : '✓ Принять'}
                                        </button>
                                    )}

                                    {app.status !== 'rejected' && (
                                        <button
                                            className="btn btn-danger-outline btn-small"
                                            onClick={() => updateStatus(app.id, 'rejected')}
                                            disabled={isUpdating}
                                        >
                                            {isUpdating ? '...' : '✕ Отклонить'}
                                        </button>
                                    )}
                                </div>
                            </div>
                        )
                    })}
                </div>
            )}
        </div>
    )
}

export default EmployerApplicationsPage