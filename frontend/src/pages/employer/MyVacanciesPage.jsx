import { useState } from 'react'
import ConfirmModal from '../../components/common/ConfirmModal'
import './EmployerPages.css'

function MyVacanciesPage({
    myCompany, myVacancies, employerLoading, employerError,
    onOpenVacancy, onCreateVacancy, onEditVacancy, onReload,
    onDeleteVacancy, onArchiveVacancy, showToast
}) {
    const [confirmDelete, setConfirmDelete] = useState(null)
    const [confirmArchive, setConfirmArchive] = useState(null)
    const [deleting, setDeleting] = useState(null)
    const [archiving, setArchiving] = useState(null)
    const [showArchived, setShowArchived] = useState(false)

    const handleDelete = async () => {
        if (!confirmDelete) return
        setDeleting(confirmDelete.id)
        try {
            await onDeleteVacancy(confirmDelete.id)
            if (showToast) showToast(`🗑️ Вакансия "${confirmDelete.title}" удалена`)
            setConfirmDelete(null)
        } catch (err) {
            console.error('Ошибка удаления:', err)
            if (showToast) showToast('Не удалось удалить вакансию')
        } finally {
            setDeleting(null)
        }
    }

    const handleArchive = async () => {
        if (!confirmArchive) return
        setArchiving(confirmArchive.id)
        try {
            await onArchiveVacancy(confirmArchive.id, true)
            if (showToast) showToast(`📦 Вакансия перемещена в архив`)
            setConfirmArchive(null)
        } catch (err) {
            console.error('Ошибка архивации:', err)
            if (showToast) showToast('Не удалось архивировать')
        } finally {
            setArchiving(null)
        }
    }

    const handleRestore = async (vacancy) => {
        try {
            await onArchiveVacancy(vacancy.id, false)
            if (showToast) showToast(`✨ Вакансия "${vacancy.title}" восстановлена`)
        } catch (err) {
            console.error('Ошибка восстановления:', err)
            if (showToast) showToast('Не удалось восстановить')
        }
    }

    if (employerLoading) {
        return (
            <div className="employer-page">
                <div className="loading"><div className="spinner"></div><p>Загрузка...</p></div>
            </div>
        )
    }

    if (!myCompany) {
        return (
            <div className="employer-page">
                <div className="employer-empty">
                    <h2>Сначала создайте компанию</h2>
                    <button className="btn btn-primary" onClick={onCreateVacancy}>Создать компанию</button>
                </div>
            </div>
        )
    }

    const activeVacancies = myVacancies.filter(v => !v.is_archived)
    const archivedVacancies = myVacancies.filter(v => v.is_archived)

    return (
        <div className="employer-page">
            {/* ====== МОДАЛКА УДАЛЕНИЯ ====== */}
            {confirmDelete && (
                <ConfirmModal
                    title="Удалить вакансию?"
                    message={`Вакансия "${confirmDelete.title}" будет удалена навсегда вместе со всеми откликами и чатами. Это действие нельзя отменить.`}
                    confirmText={deleting ? 'Удаление...' : 'Удалить'}
                    cancelText="Отмена"
                    danger={true}
                    onConfirm={handleDelete}
                    onCancel={() => setConfirmDelete(null)}
                />
            )}

            {/* ====== МОДАЛКА АРХИВАЦИИ ====== */}
            {confirmArchive && (
                <ConfirmModal
                    title="Архивировать вакансию?"
                    message={`Вакансия "${confirmArchive.title}" будет скрыта из поиска и списков кандидатов. Вы сможете восстановить её в любой момент.`}
                    confirmText={archiving ? 'Архивация...' : 'Архивировать'}
                    cancelText="Отмена"
                    onConfirm={handleArchive}
                    onCancel={() => setConfirmArchive(null)}
                />
            )}

            <div className="employer-header">
                <div>
                    <h1>Мои вакансии</h1>
                    <p className="employer-subtitle">
                        {myCompany.name} • {activeVacancies.length} активных
                        {archivedVacancies.length > 0 && ` • ${archivedVacancies.length} в архиве`}
                    </p>
                </div>
                <button className="btn btn-primary" onClick={onCreateVacancy}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <line x1="12" y1="5" x2="12" y2="19" />
                        <line x1="5" y1="12" x2="19" y2="12" />
                    </svg>
                    Создать вакансию
                </button>
            </div>

            {employerError && <div className="employer-error">{employerError}</div>}

            {activeVacancies.length === 0 && archivedVacancies.length === 0 ? (
                <div className="employer-empty-small">
                    <p>У вас пока нет вакансий</p>
                    <button className="btn btn-primary" onClick={onCreateVacancy}>
                        Создать первую вакансию
                    </button>
                </div>
            ) : (
                <>
                    {/* ====== АКТИВНЫЕ ВАКАНСИИ ====== */}
                    {activeVacancies.length > 0 && (
                        <div className="my-vacancies-grid">
                            {activeVacancies.map(v => (
                                <div key={v.id} className="my-vacancy-card">
                                    <div className="my-vacancy-header" onClick={() => onOpenVacancy(v.id)}>
                                        <h3>{v.title}</h3>
                                        <span className="vacancy-views">
                                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                                                <circle cx="12" cy="12" r="3" />
                                            </svg>
                                            {v.views || 0}
                                        </span>
                                    </div>
                                    <div className="my-vacancy-meta" onClick={() => onOpenVacancy(v.id)}>
                                        <span>{v.location}</span>
                                        <span>•</span>
                                        <span>{v.experience}</span>
                                        {v.remote && (<><span>•</span><span className="remote-badge-mini">Удалённо</span></>)}
                                    </div>
                                    {v.skills && v.skills.length > 0 && (
                                        <div className="my-vacancy-skills" onClick={() => onOpenVacancy(v.id)}>
                                            {v.skills.slice(0, 4).map((skill, i) => (
                                                <span key={i} className="skill-tag-mini">{skill}</span>
                                            ))}
                                            {v.skills.length > 4 && (
                                                <span className="skill-tag-mini more">+{v.skills.length - 4}</span>
                                            )}
                                        </div>
                                    )}

                                    {/* ====== КНОПКИ ДЕЙСТВИЙ ====== */}
                                    <div className="my-vacancy-actions">
                                        <button
                                            className="btn btn-secondary btn-small"
                                            onClick={() => onOpenVacancy(v.id)}
                                            title="Открыть"
                                        >
                                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                                                <circle cx="12" cy="12" r="3" />
                                            </svg>
                                        </button>
                                        <button
                                            className="btn btn-primary btn-small"
                                            onClick={() => onEditVacancy && onEditVacancy(v)}
                                            title="Редактировать"
                                        >
                                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                                                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                                            </svg>
                                            Редактировать
                                        </button>
                                        <button
                                            className="btn btn-secondary btn-small"
                                            onClick={() => setConfirmArchive(v)}
                                            disabled={archiving === v.id}
                                            title="В архив"
                                        >
                                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                <polyline points="21 8 21 21 3 21 3 8" />
                                                <rect x="1" y="3" width="22" height="5" />
                                            </svg>
                                            В архив
                                        </button>
                                        <button
                                            className="btn btn-danger-outline btn-small"
                                            onClick={() => setConfirmDelete(v)}
                                            disabled={deleting === v.id}
                                            title="Удалить"
                                        >
                                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                <polyline points="3 6 5 6 21 6" />
                                                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                                            </svg>
                                            Удалить
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}

                    {/* ====== АРХИВНЫЕ ВАКАНСИИ ====== */}
                    {archivedVacancies.length > 0 && (
                        <div className="archived-section">
                            <button
                                className="archived-toggle"
                                onClick={() => setShowArchived(!showArchived)}
                            >
                                <svg
                                    width="16" height="16"
                                    viewBox="0 0 24 24"
                                    fill="none"
                                    stroke="currentColor"
                                    strokeWidth="2"
                                    className={`toggle-arrow ${showArchived ? 'open' : ''}`}
                                >
                                    <polyline points="9 18 15 12 9 6" />
                                </svg>
                                Архив ({archivedVacancies.length})
                            </button>

                            {showArchived && (
                                <div className="my-vacancies-grid archived-grid">
                                    {archivedVacancies.map(v => (
                                        <div key={v.id} className="my-vacancy-card archived">
                                            <div className="archived-badge">📦 В архиве</div>
                                            <div className="my-vacancy-header">
                                                <h3>{v.title}</h3>
                                            </div>
                                            <div className="my-vacancy-meta">
                                                <span>{v.location}</span>
                                                <span>•</span>
                                                <span>{v.experience}</span>
                                            </div>
                                            <div className="my-vacancy-actions">
                                                <button
                                                    className="btn btn-secondary btn-small"
                                                    onClick={() => handleRestore(v)}
                                                >
                                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                        <polyline points="1 4 1 10 7 10" />
                                                        <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10" />
                                                    </svg>
                                                    Восстановить
                                                </button>
                                                <button
                                                    className="btn btn-danger-outline btn-small"
                                                    onClick={() => setConfirmDelete(v)}
                                                    disabled={deleting === v.id}
                                                >
                                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                        <polyline points="3 6 5 6 21 6" />
                                                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                                                    </svg>
                                                    Удалить
                                                </button>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    )}
                </>
            )}
        </div>
    )
}

export default MyVacanciesPage