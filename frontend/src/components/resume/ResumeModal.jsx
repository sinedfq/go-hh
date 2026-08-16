import { useState, useEffect } from 'react'
import axios from 'axios'
import ModalPortal from '../common/ModalPortal'
import './ResumeViewModal.css'

function ResumeViewModal({ resumeId, onClose }) {
    const [resume, setResume] = useState(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')

    useEffect(() => {
        if (!resumeId) {
            setError('Резюме не указано')
            setLoading(false)
            return
        }

        let isMounted = true

        const loadResume = async () => {
            try {
                setLoading(true)
                setError('')
                const res = await axios.get(`/api/resumes/${resumeId}`)
                if (isMounted) {
                    setResume(res.data)
                }
            } catch (err) {
                if (!isMounted) return
                
                if (err.response?.status === 404) {
                    setError('Резюме было удалено или больше не доступно')
                } else {
                    setError('Не удалось загрузить резюме')
                }
                console.error('Ошибка загрузки резюме:', err)
            } finally {
                if (isMounted) {
                    setLoading(false)
                }
            }
        }

        loadResume()

        return () => {
            isMounted = false
        }
    }, [resumeId])

    return (
        <ModalPortal>
            <div className="resume-view-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
                <button className="modal-close-btn" onClick={onClose} aria-label="Закрыть">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <line x1="18" y1="6" x2="6" y2="18" />
                        <line x1="6" y1="6" x2="18" y2="18" />
                    </svg>
                </button>

                <div className="resume-view-modal">
                    {loading ? (
                        <div className="loading">
                            <div className="spinner"></div>
                            <p>Загрузка резюме...</p>
                        </div>
                    ) : error ? (
                        <div className="resume-view-error">
                            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                                <circle cx="12" cy="12" r="10" />
                                <line x1="12" y1="8" x2="12" y2="12" />
                                <line x1="12" y1="16" x2="12.01" y2="16" />
                            </svg>
                            <h3>Резюме недоступно</h3>
                            <p>{error}</p>
                            <button className="btn btn-primary" onClick={onClose}>Закрыть</button>
                        </div>
                    ) : resume ? (
                        <div className="resume-view-content">
                            {/* Шапка */}
                            <div className="resume-view-header">
                                <div className="resume-view-avatar">
                                    {resume.photo_url ? (
                                        <img src={resume.photo_url} alt="" />
                                    ) : (
                                        <div className="avatar-placeholder">
                                            {(resume.full_name || '?')[0].toUpperCase()}
                                        </div>
                                    )}
                                </div>
                                <div className="resume-view-info">
                                    <h2>{resume.full_name}</h2>
                                    <p className="resume-view-position">{resume.desired_position}</p>
                                    <div className="resume-view-badges">
                                        <span className="badge">{resume.experience}</span>
                                        {resume.city && <span className="badge">{resume.city}</span>}
                                        {resume.remote && <span className="badge remote">Удалённо</span>}
                                        <span className="badge views-badge">
                                            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                                                <circle cx="12" cy="12" r="3" />
                                            </svg>
                                            {resume.views || 0} просмотров
                                        </span>
                                    </div>
                                </div>
                            </div>

                            {/* Навыки */}
                            {resume.skills && resume.skills.length > 0 && (
                                <div className="resume-view-section">
                                    <h3>Навыки</h3>
                                    <div className="resume-view-skills">
                                        {resume.skills.map((skill, i) => (
                                            <span key={i} className="skill-tag">{skill}</span>
                                        ))}
                                    </div>
                                </div>
                            )}

                            {/* Контакты */}
                            {(resume.phone || resume.telegram || resume.github || resume.linkedin) && (
                                <div className="resume-view-section">
                                    <h3>Контакты</h3>
                                    <div className="resume-view-contacts">
                                        {resume.phone && (
                                            <a href={`tel:${resume.phone}`} className="contact-link">
                                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                    <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z" />
                                                </svg>
                                                <span>{resume.phone}</span>
                                            </a>
                                        )}
                                        {resume.telegram && (
                                            <a href={`https://t.me/${resume.telegram.replace('@', '')}`} target="_blank" rel="noreferrer" className="contact-link">
                                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                                    <path d="M21.5 2.5L2.5 9.5c-.7.3-.7 1.3 0 1.5l4.5 1.5 2 5.5c.2.6 1 .7 1.4.2l2.3-2.5 4.2 3.1c.5.4 1.3.1 1.4-.5l3-15c.2-.7-.5-1.3-1.3-1z"/>
                                                    <path d="M8.5 12.5l7-4.5-5.5 5.5"/>
                                                </svg>
                                                <span>{resume.telegram}</span>
                                            </a>
                                        )}
                                        {resume.github && (
                                            <a href={resume.github} target="_blank" rel="noreferrer" className="contact-link">
                                                <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                                                    <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z" />
                                                </svg>
                                                <span>GitHub</span>
                                            </a>
                                        )}
                                        {resume.linkedin && (
                                            <a href={resume.linkedin} target="_blank" rel="noreferrer" className="contact-link">
                                                <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                                                    <path d="M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433c-1.144 0-2.063-.926-2.063-2.065 0-1.138.92-2.063 2.063-2.063 1.14 0 2.064.925 2.064 2.063 0 1.139-.925 2.065-2.064 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z" />
                                                </svg>
                                                <span>LinkedIn</span>
                                            </a>
                                        )}
                                    </div>
                                </div>
                            )}

                            {/* О себе */}
                            {resume.about && (
                                <div className="resume-view-section">
                                    <h3>О себе</h3>
                                    <p className="resume-view-about">{resume.about}</p>
                                </div>
                            )}

                            {/* Опыт работы */}
                            {resume.work_experience && resume.work_experience.length > 0 && (
                                <div className="resume-view-section">
                                    <h3>Опыт работы</h3>
                                    <div className="resume-view-experience">
                                        {resume.work_experience.map(exp => (
                                            <div key={exp.id} className="experience-item">
                                                <div className="experience-header">
                                                    <h4>{exp.position}</h4>
                                                    <p className="experience-company">{exp.company}</p>
                                                </div>
                                                <p className="experience-dates">
                                                    {new Date(exp.start_date).toLocaleDateString('ru-RU', { month: 'short', year: 'numeric' })}
                                                    {' — '}
                                                    {exp.end_date
                                                        ? new Date(exp.end_date).toLocaleDateString('ru-RU', { month: 'short', year: 'numeric' })
                                                        : 'По настоящее время'}
                                                </p>
                                                {exp.description && (
                                                    <p className="experience-description">{exp.description}</p>
                                                )}
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            )}
                        </div>
                    ) : null}
                </div>
            </div>
        </ModalPortal>
    )
}

export default ResumeViewModal