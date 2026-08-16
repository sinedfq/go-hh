import { useState, useEffect } from 'react'
import axios from 'axios'
import { useAuth } from '../../contexts/AuthContext'
import ResumeModal from '../../components/resume/ResumeModal'
import ResumeDetailsModal from '../../components/resume/ResumeDetailsModal'
import PhotoUpload from '../../components/common/PhotoUpload'
import './ProfilePage.css'

function ProfilePage({ onResumeUpdate, onLogout }) {
    const { user, logout, updateUserPhoto } = useAuth()
    const [resume, setResume] = useState(null)
    const [loading, setLoading] = useState(true)
    const [showResumeModal, setShowResumeModal] = useState(false)
    const [showResumeDetails, setShowResumeDetails] = useState(false)
    const [stats, setStats] = useState({ favorites: 0, applications: 0, new_applications: 0 })
    const [resumeStats, setResumeStats] = useState({ total_views: 0 })

    const isEmployer = user && (user.role === 'employer' || user.role === 'admin')

    useEffect(() => {
        if (user) {
            loadData()
        }
    }, [user])

    const loadData = async () => {
        try {
            if (isEmployer) {
                await loadStats()
            } else {
                await Promise.all([loadResume(), loadStats(), loadResumeStats()])
            }
        } catch (err) {
            console.error('Ошибка загрузки данных профиля:', err)
        } finally {
            setLoading(false)
        }
    }

    const loadResume = async () => {
        try {
            const res = await axios.get('/api/my-resume', {
                validateStatus: (status) => status < 500
            })
            if (res.status === 404 || !res.data || !res.data.id) {
                setResume(null)
            } else {
                setResume(res.data)
            }
        } catch (err) {
            console.error('Ошибка сервера:', err)
            setResume(null)
        }
    }

    const loadResumeStats = async () => {
        try {
            const res = await axios.get('/api/my-resume/stats')
            setResumeStats(res.data || { total_views: 0 })
            console.log('📊 Resume stats loaded:', res.data)
        } catch (err) {
            console.error('Ошибка загрузки статистики резюме:', err)
            setResumeStats({ total_views: 0 })
        }
    }

    const handleUserPhotoUpload = async (formData) => {
        try {
            const res = await axios.post('/api/users/me/photo', formData, {
                headers: { 'Content-Type': 'multipart/form-data' }
            })
            updateUserPhoto(res.data.photo_url)
        } catch (err) {
            console.error('Ошибка загрузки фото профиля:', err)
            alert('Не удалось загрузить фото: ' + (err.response?.data?.error || err.message))
            throw err
        }
    }

    const handleResumePhotoUpload = async (formData) => {
        try {
            const res = await axios.post('/api/resumes/me/photo', formData, {
                headers: { 'Content-Type': 'multipart/form-data' }
            })
            setResume(res.data)
            onResumeUpdate?.(res.data)
        } catch (err) {
            console.error('Ошибка загрузки фото резюме:', err)
            alert('Не удалось загрузить фото: ' + (err.response?.data?.error || err.message))
            throw err
        }
    }

    const loadStats = async () => {
        try {
            if (isEmployer) {
                const res = await axios.get('/api/employer/applications')
                const apps = Array.isArray(res.data) ? res.data : []
                const newCount = apps.filter(a => a.status === 'new').length
                setStats({ applications: apps.length, new_applications: newCount })
                console.log('📊 Employer stats:', { applications: apps.length, new_applications: newCount })
            } else {
                const res = await axios.get('/api/favorites')
                const data = Array.isArray(res.data) ? res.data : []
                setStats({ favorites: data.length })
            }
        } catch (err) {
            console.error('Ошибка загрузки статистики:', err)
        }
    }

    const handleResumeCreated = (newResume) => {
        setResume(newResume)
        setShowResumeModal(false)
        onResumeUpdate?.(newResume)
    }

    const handleResumeUpdated = (updatedResume) => {
        setResume(updatedResume)
        onResumeUpdate?.(updatedResume)
    }

    const handleDeleteResume = async () => {
        if (!window.confirm('Удалить резюме?')) return
        try {
            await axios.delete('/api/my-resume')
            setResume(null)
            setShowResumeDetails(false)
            onResumeUpdate?.(null)
        } catch (err) {
            console.error('Ошибка удаления:', err)
        }
    }

    if (!user) {
        return (
            <div className="profile-page">
                <div className="empty-state">
                    <h2>Вы не авторизованы</h2>
                    <p>Войдите в аккаунт, чтобы увидеть профиль</p>
                </div>
            </div>
        )
    }

    if (loading) {
        return (
            <div className="profile-page">
                <div className="loading">
                    <div className="spinner"></div>
                    <p>Загрузка...</p>
                </div>
            </div>
        )
    }

    return (
        <div className="profile-page">
            {showResumeModal && !isEmployer && (
                <ResumeModal
                    onClose={() => setShowResumeModal(false)}
                    onSuccess={handleResumeCreated}
                />
            )}

            {showResumeDetails && resume && resume.id && (
                <ResumeDetailsModal
                    resume={resume}
                    onClose={() => setShowResumeDetails(false)}
                    onUpdate={handleResumeUpdated}
                    onDelete={handleDeleteResume}
                />
            )}

            <div className="profile-card">
                {/* Шапка профиля */}
                <div className="profile-header animate-in" style={{ animationDelay: '0ms' }}>
                    <PhotoUpload
                        currentPhoto={user.photo_url}
                        onUpload={handleUserPhotoUpload}
                        label="Загрузить фото профиля"
                        size="medium"
                        fallback={user.email}
                    />
                    <div className="profile-info">
                        <h2>{user.email}</h2>
                        <p className="member-since">
                            Участник с {new Date(user.created_at).toLocaleDateString('ru-RU')}
                        </p>
                        <div className="profile-role-badge">
                            {user.role === 'employer' ? '👔 Работодатель' : user.role === 'admin' ? '⭐ Администратор' : '👤 Кандидат'}
                        </div>
                    </div>
                </div>

                {/* Двухколоночный layout */}
                <div className="profile-body">
                    {/* Левая колонка — статистика */}
                    <div className="profile-left">
                        <div className="profile-stats">
                            {isEmployer ? (
                                <>
                                    <div className="stat-item">
                                        <span className="stat-number">{stats.applications || 0}</span>
                                        <span className="stat-label">Откликов получено</span>
                                    </div>
                                    <div className="stat-item">
                                        <span className="stat-number">{stats.new_applications || 0}</span>
                                        <span className="stat-label">Новых откликов</span>
                                    </div>
                                </>
                            ) : (
                                <>
                                    <div className="stat-item">
                                        <span className="stat-number">{stats.favorites || 0}</span>
                                        <span className="stat-label">Избранное</span>
                                    </div>
                                    <div className="stat-item">
                                        <span className="stat-number">{resumeStats.total_views || 0}</span>
                                        <span className="stat-label">Просмотров резюме</span>
                                    </div>
                                </>
                            )}
                        </div>
                    </div>

                    {/* Правая колонка */}
                    <div className="profile-right">
                        {/* СЕКЦИЯ РЕЗЮМЕ — ТОЛЬКО ДЛЯ КАНДИДАТОВ */}
                        {!isEmployer && (
                            <div className="profile-section">
                                <div className="section-header">
                                    <h3>Моё резюме</h3>
                                </div>

                                {resume ? (
                                    <div
                                        className="resume-card clickable"
                                        onClick={() => setShowResumeDetails(true)}
                                    >
                                        <div className="resume-card-top">
                                            <div onClick={(e) => e.stopPropagation()}>
                                                <PhotoUpload
                                                    currentPhoto={resume.photo_url}
                                                    onUpload={handleResumePhotoUpload}
                                                    label="Фото резюме"
                                                    size="small"
                                                    fallback={resume.full_name}
                                                />
                                            </div>
                                            <div className="resume-header">
                                                <div>
                                                    <h4>{resume.full_name}</h4>
                                                    <p className="resume-position">{resume.desired_position}</p>
                                                </div>
                                                <div className="resume-badges">
                                                    <span className="badge">{resume.experience}</span>
                                                    {resume.remote && <span className="badge remote">Удалённо</span>}
                                                </div>
                                            </div>
                                        </div>

                                        {resume.city && (
                                            <div className="resume-meta">
                                                {resume.city}
                                            </div>
                                        )}

                                        {resume.skills && resume.skills.length > 0 && (
                                            <div className="resume-skills">
                                                {resume.skills.slice(0, 6).map((skill, i) => (
                                                    <span key={i} className="skill-tag">{skill}</span>
                                                ))}
                                                {resume.skills.length > 6 && (
                                                    <span className="skill-tag more">+{resume.skills.length - 6}</span>
                                                )}
                                            </div>
                                        )}

                                        <div className="resume-cta">
                                            <span>Нажмите для просмотра деталей</span>
                                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                <polyline points="9 18 15 12 9 6" />
                                            </svg>
                                        </div>
                                    </div>
                                ) : (
                                    <div className="empty-resume">
                                        <p>У вас пока нет резюме</p>
                                        <button
                                            className="btn btn-primary"
                                            onClick={() => setShowResumeModal(true)}
                                        >
                                            Создать резюме
                                        </button>
                                    </div>
                                )}
                            </div>
                        )}

                        {/* СЕКЦИЯ РАБОТОДАТЕЛЯ */}
                        {isEmployer && (
                            <div className="profile-section">
                                <div className="section-header">
                                    <h3>Панель работодателя</h3>
                                </div>
                                <div className="employer-info-card">
                                    <p className="employer-hint">
                                        Управляйте вакансиями и откликами из бокового меню:
                                    </p>
                                    <ul className="employer-links">
                                        <li>
                                            <span className="employer-link-icon">📋</span>
                                            <strong>Мои вакансии</strong> — список всех ваших вакансий
                                        </li>
                                        <li>
                                            <span className="employer-link-icon">➕</span>
                                            <strong>Создать</strong> — новая вакансия
                                        </li>
                                        <li>
                                            <span className="employer-link-icon">🏢</span>
                                            <strong>Моя компания</strong> — информация о компании
                                        </li>
                                        <li>
                                            <span className="employer-link-icon">✈️</span>
                                            <strong>Отклики</strong> — кандидаты откликнувшиеся на ваши вакансии
                                        </li>
                                    </ul>
                                </div>
                            </div>
                        )}
                    </div>
                </div>

                {/* Кнопка выхода */}
                <div className="profile-actions">
                    <button className="btn btn-danger" onClick={onLogout}>
                        Выйти из аккаунта
                    </button>
                </div>
            </div>
        </div>
    )
}

export default ProfilePage