import { useState, useEffect } from 'react'
import axios from 'axios'
import ConfirmModal from '../../components/common/ConfirmModal'
import './CandidatePages.css'

function MyApplicationsPage({ onOpenVacancy, onOpenChatWith, showToast }) {
    const [applications, setApplications] = useState([])
    const [loading, setLoading] = useState(true)
    const [filter, setFilter] = useState('all')
    const [cancelling, setCancelling] = useState(null)
    const [confirmCancel, setConfirmCancel] = useState(null) // ID отклика для подтверждения

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
        setCancelling(applicationId)
        try {
            await axios.delete(`/api/applications/${applicationId}`)
            setApplications(prev => prev.filter(app => app.id !== applicationId))

            // ✅ ДОБАВЛЕНО: Toast-уведомление
            if (showToast) {
                showToast('🗑️ Отклик отменён')
            }
        } catch (err) {
            console.error('Ошибка отмены:', err)
            if (showToast) {
                showToast('Не удалось отменить отклик')
            }
        } finally {
            setCancelling(null)
            setConfirmCancel(null)
        }
    }

    const getStatusInfo = (status) => {
        switch (status) {
            case 'new':
                return { label: 'На рассмотрении', className: 'status-new' }
            case 'viewed':
                return { label: 'Просмотрен', className: 'status-viewed' }
            case 'accepted':
                return { label: 'Принят', className: 'status-accepted' }
            case 'rejected':
                return { label: 'Отклонён', className: 'status-rejected' }
            default:
                return { label: status, className: '' }
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
            {/* Кастомная модалка подтверждения */}
            {confirmCancel && (
                <ConfirmModal
                    title="Отменить отклик?"
                    message="Вы сможете откликнуться на эту вакансию снова позже. Чат с работодателем будет удалён."
                    confirmText="Отменить отклик"
                    cancelText="Оставить"
                    danger={true}
                    onConfirm={() => cancelApplication(confirmCancel)}
                    onCancel={() => setConfirmCancel(null)}
                />
            )}

            <div className="candidate-header">
                <div>
                    <h1>Мои отклики</h1>
                    <p className="candidate-subtitle">
                        Всего откликов: {applications.length}
                        {stats.accepted > 0 && <span className="accepted-count"> • {stats.accepted} принято</span>}
                    </p>
                </div>
            </div>

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
                        {filter === 'all' ? 'Вы ещё не откликались на вакансии' : `Нет откликов`}
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

                        return (
                            <div key={app.id} className={`my-app-card ${statusInfo.className}`}>
                                <div className="my-app-card-top">
                                    <div className="my-app-vacancy-info">
                                        <h3 className="my-app-vacancy-title">{app.vacancy_title}</h3>
                                        <p className="my-app-company-name">{app.company_name}</p>
                                    </div>
                                    <div className={`my-app-status-badge ${statusInfo.className}`}>
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

                                    {(app.status === 'new' || app.status === 'viewed') && (
                                        <button
                                            className="btn btn-danger-outline btn-small cancel-btn"
                                            onClick={() => setConfirmCancel(app.id)}
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

                                    {app.conversation_id && (
                                        <button
                                            className="btn btn-primary btn-small"
                                            onClick={() => onOpenChatWith && onOpenChatWith({
                                                conversation_id: app.conversation_id,
                                                application_id: app.id,
                                                vacancy_id: app.vacancy_id,
                                                vacancy_title: app.vacancy_title,
                                                company_name: app.company_name,
                                                candidate_name: app.resume_full_name,
                                                candidate_id: app.candidate_user_id,
                                                candidate_photo: app.candidate_photo,
                                            })}
                                        >
                                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                                            </svg>
                                            Чат
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