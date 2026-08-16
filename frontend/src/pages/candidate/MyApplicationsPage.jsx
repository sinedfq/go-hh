import { useState, useEffect } from 'react'
import axios from 'axios'
import './CandidatePages.css'

function MyApplicationsPage({ onOpenVacancy }) {
    const [applications, setApplications] = useState([])
    const [loading, setLoading] = useState(true)
    const [filter, setFilter] = useState('all')
    const [cancelling, setCancelling] = useState(null)

    const loadApplications = async () => {
        try {
            setLoading(true)
            const res = await axios.get('/api/my-applications')
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

    const cancelApplication = async (applicationId) => {
        if (!window.confirm('Отменить отклик? Вы сможете откликнуться снова позже.')) return

        setCancelling(applicationId)
        try {
            await axios.delete(`/api/applications/${applicationId}`)
            setApplications(prev => prev.filter(app => app.id !== applicationId))
        } catch (err) {
            console.error('Ошибка отмены:', err)
            alert('Не удалось отменить отклик')
        } finally {
            setCancelling(null)
        }
    }

    const getStatusInfo = (status) => {
        switch (status) {
            case 'new':
                return { label: 'На рассмотрении', className: 'status-new', color: '#0066cc' }
            case 'viewed':
                return { label: 'Просмотрен', className: 'status-viewed', color: '#d97706' }
            case 'accepted':
                return { label: 'Принят', className: 'status-accepted', color: '#2ea043' }
            case 'rejected':
                return { label: 'Отклонён', className: 'status-rejected', color: '#dc3545' }
            default:
                return { label: status, className: '', icon: '', color: '#666' }
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
            year: 'numeric'
        })
    }

    if (loading) {
        return (
            <div className="candidate-page">
                <div className="loading">
                    <div className="spinner"></div>
                    <p>Загрузка откликов...</p>
                </div>
            </div>
        )
    }

    return (
        <div className="candidate-page">
            <div className="candidate-header">
                <div>
                    <h1>Мои отклики</h1>
                    <p className="candidate-subtitle">
                        Всего откликов: {applications.length}
                        {stats.accepted > 0 && <span className="accepted-count"> • {stats.accepted} принято</span>}
                    </p>
                </div>
            </div>

            {/* Фильтры */}
            <div className="my-apps-filters">
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
                    На рассмотрении <span className="filter-count">{stats.new}</span>
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

            {filteredApps.length === 0 ? (
                <div className="my-apps-empty">
                    <div className="my-apps-empty-icon">📝</div>
                    <h3>
                        {filter === 'all' ? 'Вы ещё не откликались на вакансии' : `Нет ${filter === 'new' ? 'откликов на рассмотрении' : 'откликов'}`}
                    </h3>
                    <p>
                        {filter === 'all'
                            ? 'Найдите интересную вакансию и нажмите "Откликнуться"'
                            : 'Попробуйте выбрать другой фильтр'}
                    </p>
                </div>
            ) : (
                <div className="my-apps-list">
                    {filteredApps.map(app => {
                        const statusInfo = getStatusInfo(app.status)
                        const isCancelling = cancelling === app.id
                        const canCancel = app.status === 'new' || app.status === 'viewed'

                        return (
                            <div key={app.id} className={`my-app-card ${statusInfo.className}`}>
                                <div className="my-app-card-top">
                                    <div className="my-app-vacancy-info">
                                        <h3 className="my-app-vacancy-title">{app.vacancy_title}</h3>
                                        <p className="my-app-company-name">{app.company_name}</p>
                                    </div>
                                    <div className={`my-app-status-badge ${statusInfo.className}`}>
                                        <span className="status-icon">{statusInfo.icon}</span>
                                        {statusInfo.label}
                                    </div>
                                </div>

                                <div className="my-app-meta">
                                    <span className="my-app-date">
                                        Отклик отправлен {formatDate(app.created_at)}
                                    </span>
                                </div>

                                <div className="my-app-actions">
                                    <button
                                        className="btn btn-secondary btn-small"
                                        onClick={() => onOpenVacancy && onOpenVacancy(app.vacancy_id)}
                                    >
                                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                                            <circle cx="12" cy="12" r="3" />
                                        </svg>
                                        Открыть вакансию
                                    </button>

                                    {/* ====== КНОПКА ОТМЕНЫ ОТКЛИКА ====== */}
                                    {(app.status === 'new' || app.status === 'viewed') && (
                                        <button
                                            className="btn btn-danger-outline btn-small cancel-btn"
                                            onClick={() => cancelApplication(app.id)}
                                            disabled={isCancelling}
                                        >
                                            {isCancelling ? (
                                                <>
                                                    <div className="spinner-small"></div>
                                                    Отмена...
                                                </>
                                            ) : (
                                                <>
                                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                        <line x1="18" y1="6" x2="6" y2="18" />
                                                        <line x1="6" y1="6" x2="18" y2="18" />
                                                    </svg>
                                                    Отменить отклик
                                                </>
                                            )}
                                        </button>
                                    )}

                                    {app.status === 'accepted' && (
                                        <div className="btn btn-success btn-small" style={{ cursor: 'default' }}>
                                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                <polyline points="20 6 9 17 4 12" />
                                            </svg>
                                            Отклик принят 🎉
                                        </div>
                                    )}

                                    {app.status === 'rejected' && (
                                        <div className="btn btn-danger btn-small" style={{ cursor: 'default', opacity: 0.8 }}>
                                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                <line x1="18" y1="6" x2="6" y2="18" />
                                                <line x1="6" y1="6" x2="18" y2="18" />
                                            </svg>
                                            Отклик отклонён
                                        </div>
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

export default MyApplicationsPage