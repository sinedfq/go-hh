import { useState, useEffect, useRef } from 'react'
import axios from 'axios'
import { useAuth } from '../../contexts/AuthContext'
import ConfirmModal from '../../components/common/ConfirmModal'
import './VacancyPage.css'
import YandexMap from '../../components/common/YandexMap'
import ApplicationModal from '../../components/common/ApplicationModal'

function VacancyPage({ vacancyId, onClose, onOpenCompany, showToast }) {
    const { user } = useAuth()
    const [vacancy, setVacancy] = useState(null)
    const [company, setCompany] = useState(null)
    const [loading, setLoading] = useState(true)
    const [isFavorite, setIsFavorite] = useState(false)
    const [showApplyModal, setShowApplyModal] = useState(false)
    const [hasApplied, setHasApplied] = useState(false)
    const [checkingApplication, setCheckingApplication] = useState(true)
    const [applicationId, setApplicationId] = useState(null)
    const [confirmCancel, setConfirmCancel] = useState(false)
    const viewLoggedRef = useRef(false)

    useEffect(() => {
        loadVacancy()
    }, [vacancyId])

    // ====== ПРОВЕРКА ОТКЛИКА И ИЗБРАННОГО ======
    useEffect(() => {
        if (!user || !vacancy?.id) {
            setCheckingApplication(false)
            return
        }

        const checkApplication = async () => {
            try {
                const res = await axios.get(`/api/vacancies/${vacancy.id}/application`)
                if (res.data.applied) {
                    setHasApplied(true)
                    setApplicationId(res.data.application?.id || null)
                } else {
                    setHasApplied(false)
                    setApplicationId(null)
                }
            } catch (err) {
                console.error('Ошибка проверки отклика:', err)
            } finally {
                setCheckingApplication(false)
            }
        }

        const checkFavorite = async () => {
            try {
                const res = await axios.get('/api/favorites')
                const favorites = Array.isArray(res.data) ? res.data : []
                setIsFavorite(favorites.some(fav => fav.id === vacancy.id))
            } catch (err) {
                console.error('Ошибка проверки избранного:', err)
            }
        }

        checkApplication()
        checkFavorite()
    }, [user, vacancy?.id])

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

    const handleToggleFavorite = async () => {
        if (!user) return
        try {
            if (isFavorite) {
                await axios.delete(`/api/favorites/${vacancy.id}`)
                setIsFavorite(false)
                if (showToast) showToast('Удалено из избранного')
            } else {
                await axios.post('/api/favorites', { vacancy_id: vacancy.id })
                setIsFavorite(true)
                if (showToast) showToast('✨ Добавлено в избранное')
            }
        } catch (err) {
            console.error('Ошибка избранного:', err)
        }
    }

    // ====== УСПЕШНЫЙ ОТКЛИК — СОХРАНЯЕМ ID ======
    const handleApplySuccess = (data) => {
        setHasApplied(true)
        setShowApplyModal(false)
        
        // ✅ СОХРАНЯЕМ ID отклика из ответа бэкенда
        if (data?.id) {
            setApplicationId(data.id)
        }
        
        // Уведомление об отклике
        if (showToast) {
            showToast(`✨ Отклик отправлен! Компания "${vacancy?.company}" скоро свяжется с вами`)
        }
    }

    // ====== ОТКРЫТИЕ МОДАЛКИ ПОДТВЕРЖДЕНИЯ ======
    const handleCancelApplication = () => {
        setConfirmCancel(true)
    }

    // ====== РЕАЛЬНАЯ ОТМЕНА ОТКЛИКА ======
    const doCancelApplication = async () => {
        // Если ID нет — попробуем получить его через API
        let idToCancel = applicationId
        
        if (!idToCancel) {
            try {
                const res = await axios.get(`/api/vacancies/${vacancy.id}/application`)
                if (res.data.applied && res.data.application?.id) {
                    idToCancel = res.data.application.id
                }
            } catch (err) {
                console.error('Не удалось получить ID отклика:', err)
            }
        }

        if (!idToCancel) {
            console.error('Нет ID отклика')
            if (showToast) showToast('Ошибка: не удалось найти отклик')
            setConfirmCancel(false)
            return
        }

        try {
            await axios.delete(`/api/applications/${idToCancel}`)
            setHasApplied(false)
            setApplicationId(null)
            setConfirmCancel(false)
            
            // ✅ УВЕДОМЛЕНИЕ ОБ ОТМЕНЕ
            if (showToast) {
                showToast('🗑️ Отклик отменён')
            }
        } catch (err) {
            console.error('Ошибка отмены:', err)
            setConfirmCancel(false)
            if (showToast) {
                showToast('Не удалось отменить отклик')
            }
        }
    }

    // ====== ИНКРЕМЕНТ ПРОСМОТРОВ ВАКАНСИИ ======
    useEffect(() => {
        if (!vacancy?.id || viewLoggedRef.current) return

        viewLoggedRef.current = true

        axios.post(`/api/vacancies/${vacancy.id}/view`)
            .then(() => {
                console.log(`✅ Vacancy ${vacancy.id} view incremented`)
            })
            .catch(err => {
                viewLoggedRef.current = false
                console.warn('⚠️ Ошибка инкремента просмотров:', err.message)
            })
    }, [vacancy?.id])

    useEffect(() => {
        viewLoggedRef.current = false
    }, [vacancyId])

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

    const hasCoordinates = vacancy.latitude && vacancy.longitude &&
        vacancy.latitude !== 0 && vacancy.longitude !== 0

    const isOwnVacancy = user && vacancy.author_user_id === user.id
    const isSameCompany = user && user.company_id && vacancy.company_id && user.company_id === vacancy.company_id

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
                            <span className="stat-label">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                                    <circle cx="12" cy="12" r="3" />
                                </svg>
                                Просмотры
                            </span>
                            <span className="stat-value">{vacancy.views || 0}</span>
                        </div>

                        {vacancy.author_user_id === user?.id && (
                            <div className="stat-row">
                                <span className="stat-label">
                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" />
                                    </svg>
                                    Откликов
                                </span>
                                <span className="stat-value">—</span>
                            </div>
                        )}
                    </div>

                    {/* ====== КНОПКИ ДЕЙСТВИЙ ====== */}
                    <div className="vacancy-actions-card">
                        {user ? (
                            checkingApplication ? (
                                <button className="btn btn-secondary btn-block" disabled>
                                    Проверка...
                                </button>
                            ) : hasApplied ? (
                                <>
                                    <div className="applied-badge-card">
                                        <div className="applied-badge-icon">
                                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                <polyline points="20 6 9 17 4 12" />
                                            </svg>
                                        </div>
                                        <div className="applied-badge-text">
                                            <h4>Отклик отправлен</h4>
                                            <p>Компания скоро свяжется с вами</p>
                                        </div>
                                    </div>
                                    <button
                                        className="btn btn-danger-outline btn-block"
                                        onClick={handleCancelApplication}
                                    >
                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <line x1="18" y1="6" x2="6" y2="18" />
                                            <line x1="6" y1="6" x2="18" y2="18" />
                                        </svg>
                                        Отменить отклик
                                    </button>
                                </>
                            ) : isOwnVacancy ? (
                                <div className="own-vacancy-badge">
                                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                                    </svg>
                                    Это ваша вакансия
                                </div>
                            ) : isSameCompany ? (
                                <div className="own-company-badge">
                                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
                                        <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
                                    </svg>
                                    Вакансия вашей компании
                                </div>
                            ) : (
                                <button
                                    className="btn btn-primary btn-block apply-btn"
                                    onClick={() => setShowApplyModal(true)}
                                >
                                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" />
                                    </svg>
                                    Откликнуться
                                </button>
                            )
                        ) : (
                            <button className="btn btn-secondary btn-block" disabled>
                                Войдите чтобы откликнуться
                            </button>
                        )}

                        {user && (
                            <button
                                className={`btn btn-block ${isFavorite ? 'btn-favorite-active' : 'btn-secondary'}`}
                                onClick={handleToggleFavorite}
                            >
                                <svg width="18" height="18" viewBox="0 0 24 24" fill={isFavorite ? "currentColor" : "none"} stroke="currentColor" strokeWidth="2">
                                    <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
                                </svg>
                                {isFavorite ? 'В избранном' : 'В избранное'}
                            </button>
                        )}
                    </div>
                </div>
            </div>

            {/* ====== МОДАЛКА ОТКЛИКА ====== */}
            {showApplyModal && (
                <ApplicationModal
                    vacancy={vacancy}
                    onClose={() => setShowApplyModal(false)}
                    onSuccess={handleApplySuccess}
                />
            )}

            {/* ====== КАСТОМНАЯ МОДАЛКА ОТМЕНЫ ОТКЛИКА ====== */}
            {confirmCancel && (
                <ConfirmModal
                    title="Отменить отклик?"
                    message="Вы сможете откликнуться на эту вакансию снова позже. Чат с работодателем будет удалён."
                    confirmText="Отменить отклик"
                    cancelText="Оставить"
                    danger={true}
                    onConfirm={doCancelApplication}
                    onCancel={() => setConfirmCancel(false)}
                />
            )}
        </div>
    )
}

export default VacancyPage