import { useState, useEffect, useRef } from 'react'
import axios from 'axios'
import ConfirmModal from '../../components/common/ConfirmModal'
import './EmployerPages.css'

function EmployerApplicationsPage({ onOpenVacancy, onOpenResume, onOpenChatWith, showToast }) {
    const [applications, setApplications] = useState([])
    const [loading, setLoading] = useState(true)
    const [filter, setFilter] = useState('all')
    const [updating, setUpdating] = useState(null)
    const [confirmAction, setConfirmAction] = useState(null)
    
    const lastKnownIdsRef = useRef(new Set())
    const isFirstLoadRef = useRef(true)

    const loadApplications = async (silent = false) => {
        try {
            if (!silent) setLoading(true)
            const res = await axios.get('/api/employer/applications')
            const apps = Array.isArray(res.data) ? res.data : []
            
            // На первом загрузке просто запоминаем ID
            if (isFirstLoadRef.current) {
                isFirstLoadRef.current = false
                apps.forEach(a => lastKnownIdsRef.current.add(a.id))
                setApplications(apps)
                return
            }
            
            // Проверяем новые отклики
            const newApps = apps.filter(a => !lastKnownIdsRef.current.has(a.id))
            if (newApps.length > 0 && showToast) {
                newApps.forEach(app => {
                    showToast(`🎉 Новый отклик от ${app.resume_full_name || app.candidate_email} на "${app.vacancy_title}"`)
                })
                // Обновляем известные ID
                newApps.forEach(a => lastKnownIdsRef.current.add(a.id))
            }
            
            setApplications(apps)
        } catch (err) {
            console.error('Ошибка загрузки откликов:', err)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        loadApplications()
    }, [])

    // ====== POLLING КАЖДЫЕ 10 СЕКУНД ======
    useEffect(() => {
        const interval = setInterval(() => {
            loadApplications(true)
        }, 10000)
        return () => clearInterval(interval)
    }, [])

    // ====== ОБНОВЛЕНИЕ СТАТУСА С TOAST ======
    const updateStatus = async (applicationId, status, candidateName) => {
        setUpdating(applicationId)
        try {
            await axios.patch(`/api/applications/${applicationId}/status`, { status })
            setApplications(prev => prev.map(app =>
                app.id === applicationId ? { ...app, status } : app
            ))
            if (showToast) {
                if (status === 'accepted') {
                    showToast(`Кандидат ${candidateName} принят!`)
                } else if (status === 'rejected') {
                    showToast(`Отклик ${candidateName} отклонён`)
                }
            }
        } catch (err) {
            console.error('Ошибка обновления статуса:', err)
            if (showToast) showToast('Не удалось обновить статус')
        } finally {
            setUpdating(null)
            setConfirmAction(null)
        }
    }

    // ====== СОЗДАТЬ ЧАТ ВРУЧНУЮ (если ещё не создан) ======
    const handleOpenChat = async (app) => {
        if (app.conversation_id) {
            // Чат есть — открываем
            if (onOpenChatWith) {
                onOpenChatWith({
                    conversation_id: app.conversation_id,
                    application_id: app.id,
                    vacancy_id: app.vacancy_id,
                    vacancy_title: app.vacancy_title,
                    company_name: app.company_name,
                    candidate_name: app.resume_full_name || app.candidate_email,
                    candidate_id: app.candidate_user_id,
                    candidate_photo: app.candidate_photo,
                })
            }
        } else {
            // Чата нет — создаём
            try {
                const res = await axios.post(`/api/applications/${app.id}/start-chat`)
                const newConvId = res.data.conversation_id
                
                // Обновляем локальное состояние
                setApplications(prev => prev.map(a => 
                    a.id === app.id ? { ...a, conversation_id: newConvId } : a
                ))
                
                // Открываем чат
                if (onOpenChatWith) {
                    onOpenChatWith({
                        conversation_id: newConvId,
                        application_id: app.id,
                        vacancy_id: app.vacancy_id,
                        vacancy_title: app.vacancy_title,
                        company_name: app.company_name,
                        candidate_name: app.resume_full_name || app.candidate_email,
                        candidate_id: app.candidate_user_id,
                        candidate_photo: app.candidate_photo,
                    })
                }
            } catch (err) {
                console.error('Ошибка создания чата:', err)
                if (showToast) showToast('Не удалось создать чат')
            }
        }
    }

    const handleConfirmAction = () => {
        if (!confirmAction) return
        const { app, action } = confirmAction
        const status = action === 'accept' ? 'accepted' : 'rejected'
        const name = app.resume_full_name || app.candidate_email
        updateStatus(app.id, status, name)
    }

    const getStatusInfo = (status) => {
        switch (status) {
            case 'new': return { label: 'Новый', className: 'status-new' }
            case 'viewed': return { label: 'Просмотрен', className: 'status-viewed' }
            case 'accepted': return { label: 'Принят', className: 'status-accepted' }
            case 'rejected': return { label: 'Отклонён', className: 'status-rejected' }
            default: return { label: status, className: '' }
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
            day: 'numeric', month: 'short', year: 'numeric',
            hour: '2-digit', minute: '2-digit'
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
            {confirmAction && (
                <ConfirmModal
                    title={confirmAction.action === 'accept' ? 'Принять кандидата?' : 'Отклонить отклик?'}
                    message={
                        confirmAction.action === 'accept'
                            ? `Вы собираетесь принять кандидата "${confirmAction.app.resume_full_name || confirmAction.app.candidate_email}".`
                            : `Отклонить отклик от "${confirmAction.app.resume_full_name || confirmAction.app.candidate_email}"?`
                    }
                    confirmText={confirmAction.action === 'accept' ? 'Принять' : 'Отклонить'}
                    cancelText="Отмена"
                    danger={confirmAction.action === 'reject'}
                    onConfirm={handleConfirmAction}
                    onCancel={() => setConfirmAction(null)}
                />
            )}

            <div className="employer-header">
                <div>
                    <h1>Отклики на вакансии</h1>
                    <p className="employer-subtitle">
                        Всего откликов: {applications.length}
                        {stats.new > 0 && <span className="new-count"> • {stats.new} новых</span>}
                    </p>
                </div>
            </div>

            <div className="applications-filters">
                <button className={`filter-btn ${filter === 'all' ? 'active' : ''}`} onClick={() => setFilter('all')}>
                    Все <span className="filter-count">{stats.all}</span>
                </button>
                <button className={`filter-btn ${filter === 'new' ? 'active' : ''}`} onClick={() => setFilter('new')}>
                    Новые <span className="filter-count">{stats.new}</span>
                </button>
                <button className={`filter-btn ${filter === 'viewed' ? 'active' : ''}`} onClick={() => setFilter('viewed')}>
                    Просмотренные <span className="filter-count">{stats.viewed}</span>
                </button>
                <button className={`filter-btn ${filter === 'accepted' ? 'active' : ''}`} onClick={() => setFilter('accepted')}>
                    Принятые <span className="filter-count">{stats.accepted}</span>
                </button>
                <button className={`filter-btn ${filter === 'rejected' ? 'active' : ''}`} onClick={() => setFilter('rejected')}>
                    Отклонённые <span className="filter-count">{stats.rejected}</span>
                </button>
            </div>

            {filteredApps.length === 0 ? (
                <div className="applications-empty">
                    <div className="applications-empty-icon">📭</div>
                    <h3>{filter === 'all' ? 'Пока нет откликов' : 'Нет откликов'}</h3>
                    <p>Когда кандидаты откликнутся, они появятся здесь</p>
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
                                        {statusInfo.label}
                                    </div>
                                </div>

                                <div className="application-vacancy-row">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
                                        <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
                                    </svg>
                                    <button className="application-vacancy-link" onClick={() => onOpenVacancy?.(app.vacancy_id)}>
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
                                    <span className="application-date">{formatDate(app.created_at)}</span>
                                </div>

                                <div className="application-actions">
                                    {app.resume_id && app.resume_id > 0 ? (
                                        <button className="btn btn-secondary btn-small" onClick={() => onOpenResume?.(app.resume_id)}>
                                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                                                <polyline points="14 2 14 8 20 8" />
                                            </svg>
                                            Посмотреть резюме
                                        </button>
                                    ) : (
                                        <span className="no-resume-hint">Резюме не прикреплено</span>
                                    )}

                                    {app.status !== 'accepted' && (
                                        <button className="btn btn-success btn-small" onClick={() => setConfirmAction({ app, action: 'accept' })} disabled={isUpdating}>
                                            {isUpdating ? '...' : '✓ Принять'}
                                        </button>
                                    )}

                                    {app.status !== 'rejected' && (
                                        <button className="btn btn-danger-outline btn-small" onClick={() => setConfirmAction({ app, action: 'reject' })} disabled={isUpdating}>
                                            {isUpdating ? '...' : '✕ Отклонить'}
                                        </button>
                                    )}

                                    <button className="btn btn-primary btn-small" onClick={() => handleOpenChat(app)}>
                                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                                        </svg>
                                        Чат
                                    </button>
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