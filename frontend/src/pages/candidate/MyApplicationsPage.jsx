import AppLoader from '../../components/common/AppLoader'
import { useState, useEffect, useCallback } from 'react'
import axios from 'axios'
import ApplicationHistory from '../../components/common/ApplicationHistory'
import ConfirmModal from '../../components/common/ConfirmModal'
import WorkspaceHeader, { WorkspaceIcon, WorkspaceMetric } from '../../components/common/WorkspaceHeader'
import { userError } from '../../utils/userError'
import './CandidatePages.css'

function MyApplicationsPage({ onOpenVacancy, onOpenChatWith, showToast }) {
    const [applications, setApplications] = useState([])
    const [loading, setLoading] = useState(true)
    const [filter, setFilter] = useState('all')
    const [query, setQuery] = useState('')
    const [error, setError] = useState('')
    const [cancelling, setCancelling] = useState(null)
    const [confirmCancel, setConfirmCancel] = useState(null) // ID отклика для подтверждения

    const loadApplications = useCallback(async () => {
        try {
            setLoading(true)
            setError('')
            const res = await axios.get('/api/my-applications')
            setApplications(Array.isArray(res.data) ? res.data : [])
        } catch (err) {
            setError(userError(err))
        } finally {
            setLoading(false)
        }
    }, [])

    useEffect(() => {
        loadApplications()
    }, [loadApplications])

    const cancelApplication = async (applicationId) => {
        setCancelling(applicationId)
        try {
            await axios.delete(`/api/applications/${applicationId}`)
            setApplications(prev => prev.filter(app => app.id !== applicationId))

            // ✅ ДОБАВЛЕНО: Toast-уведомление
            if (showToast) {
                showToast('Отклик отменён')
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
                return { label: 'Приглашение', className: 'status-accepted' }
            case 'rejected':
                return { label: 'Отказ', className: 'status-rejected' }
            default:
                return { label: status, className: '' }
        }
    }

    const filteredApps = applications.filter(app => {
        return (filter === 'all' || app.status === filter) && `${app.vacancy_title} ${app.company_name}`.toLowerCase().includes(query.trim().toLowerCase())
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
                    <AppLoader inline/>
                    <p>Загрузка откликов...</p>
                </div>
            </div>
        )
    }

    return (
        <div className="candidate-page workspace-screen applications-workspace">
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

            <WorkspaceHeader eyebrow="ВАШ СЛЕДУЮЩИЙ ШАГ" title="Мои отклики" description="Следите за ответами компаний, общайтесь с командами и готовьтесь к знакомству."/>
            {error && <div className="employer-error" role="alert">{error}<button className="btn btn-secondary" onClick={loadApplications}>Повторить загрузку</button></div>}
            <div className="workspace-metrics"><WorkspaceMetric icon="briefcase" value={stats.all} label="Всего откликов" hint="Ваши шаги к новой работе"/><WorkspaceMetric icon="eye" value={stats.new + stats.viewed} label="В процессе" hint="Ожидают ответа компании"/><WorkspaceMetric icon="check" value={stats.accepted} label="Приглашения" hint="Компании готовы познакомиться"/></div>

            <div className="workspace-toolbar"><label className="workspace-search"><WorkspaceIcon name="search" size={18}/><input aria-label="Поиск по откликам" placeholder="Должность или компания" value={query} onChange={event => setQuery(event.target.value)}/></label></div>
            <div className="workspace-tabs applications-tabs" aria-label="Статус откликов">
                <button
                    aria-pressed={filter === 'all'}
                    onClick={() => setFilter('all')}
                >
                    Все <span className="filter-count">{stats.all}</span>
                </button>
                <button
                    aria-pressed={filter === 'new'}
                    onClick={() => setFilter('new')}
                >
                    На рассмотрении <span className="filter-count">{stats.new}</span>
                </button>
                <button
                    aria-pressed={filter === 'viewed'}
                    onClick={() => setFilter('viewed')}
                >
                    Просмотренные <span className="filter-count">{stats.viewed}</span>
                </button>
                <button
                    aria-pressed={filter === 'accepted'}
                    onClick={() => setFilter('accepted')}
                >
                    Приглашения <span className="filter-count">{stats.accepted}</span>
                </button>
                <button
                    aria-pressed={filter === 'rejected'}
                    onClick={() => setFilter('rejected')}
                >
                    Отказы <span className="filter-count">{stats.rejected}</span>
                </button>
            </div>

            {filteredApps.length === 0 ? (
                <div className="workspace-state">
                    <span className="workspace-state-icon"><WorkspaceIcon name="briefcase" size={28}/></span>
                    <h3>
                        {query ? 'Ничего не найдено' : filter === 'all' ? 'Здесь начнётся ваша история' : 'Пока нет откликов с таким статусом'}
                    </h3>
                    <p>
                        {filter === 'all' && !query
                            ? 'Найдите интересную вакансию и нажмите "Откликнуться"'
                            : 'Попробуйте выбрать другой фильтр'}
                    </p>
                    {(query || filter !== 'all') && <button className="btn btn-secondary" onClick={() => { setQuery(''); setFilter('all') }}>Показать все отклики</button>}
                </div>
            ) : (
                <div className="my-apps-list">
                    {filteredApps.map(app => {
                        const statusInfo = getStatusInfo(app.status)
                        const isCancelling = cancelling === app.id

                        return (
                            <div key={app.id} className={`my-app-card ${app.vacancy_archived ? 'vacancy-archived' : ''}`}>
                                {app.vacancy_archived && (
                                    <div className="archived-badge-top">
                                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <polyline points="21 8 21 21 3 21 3 8" />
                                            <rect x="1" y="3" width="22" height="5" />
                                        </svg>
                                        Вакансия в архиве
                                    </div>
                                )}
                                <div className="my-app-card-top">
                                    <div className="my-app-vacancy-info">
                                        <h3 className="my-app-vacancy-title">{app.vacancy_title}</h3>
                                        <p className="my-app-company-name">{app.company_name}</p>
                                    </div>
                                    <div className={`workspace-status ${app.status === 'accepted' ? 'success' : app.status === 'rejected' ? 'declined' : app.status === 'new' ? 'muted' : ''}`}>
                                        {statusInfo.label}
                                    </div>
                                </div>

                                <div className="my-app-meta">
                                    <span className="my-app-date">
                                        Отклик отправлен {formatDate(app.created_at)}
                                    </span>
                                </div>

                                <ApplicationHistory application={app}/>
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
                                                    <AppLoader inline/>
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

                                </div>
                            </div>
                        )
                    })}
                </div>
            )
            }
        </div>
    )
}

export default MyApplicationsPage
