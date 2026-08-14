import { useState, useEffect } from 'react'
import axios from 'axios'
import PhotoUpload from '../common/PhotoUpload'
import ModalPortal from '../common/ModalPortal'
import ResumeModal from './ResumeModal'
import './ResumeDetailsModal.css'
import { displayUrl } from '../../utils/format'

function ResumeDetailsModal({ resume, onClose, onUpdate, onDelete }) {
  const [showWorkExpForm, setShowWorkExpForm] = useState(false)
  const [showEditModal, setShowEditModal] = useState(false)
  const [workExpData, setWorkExpData] = useState({
    company: '',
    position: '',
    start_date: '',
    end_date: '',
    description: ''
  })
  const [loading, setLoading] = useState(false)

  const handleWorkExpChange = (e) => {
    const { name, value } = e.target
    setWorkExpData(prev => ({ ...prev, [name]: value }))
  }

  useEffect(() => {
    if (resume && resume.id) {
      axios.post(`/api/resumes/${resume.id}/view`, {}, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      }).catch(err => console.error('Ошибка инкремента просмотров:', err))
    }
  }, [resume?.id])

  const handleAddWorkExp = async (e) => {
    e.preventDefault()
    setLoading(true)

    try {
      const payload = {
        ...workExpData,
        end_date: workExpData.end_date || null
      }
      const res = await axios.post('/api/work-experience', payload)
      onUpdate(res.data)
      setWorkExpData({
        company: '',
        position: '',
        start_date: '',
        end_date: '',
        description: ''
      })
      setShowWorkExpForm(false)
    } catch (err) {
      console.error('Ошибка добавления опыта:', err)
      alert('Не удалось добавить опыт работы')
    } finally {
      setLoading(false)
    }
  }

  const handleDeleteWorkExp = async (expId) => {
    if (!window.confirm('Удалить эту запись?')) return

    try {
      const res = await axios.delete(`/api/work-experience/${expId}`)
      onUpdate(res.data)
    } catch (err) {
      console.error('Ошибка удаления:', err)
    }
  }

  const handleEditSuccess = (updatedResume) => {
    onUpdate(updatedResume)
    setShowEditModal(false)
  }

  return (
    <>
      {showEditModal && (
        <ResumeModal
          onClose={() => setShowEditModal(false)}
          onSuccess={handleEditSuccess}
          resume={resume}
        />
      )}

      <ModalPortal>
        <div className="details-modal-overlay" onClick={(e) => {
          if (e.target === e.currentTarget) onClose()
        }}>
          <button className="modal-close-btn" onClick={onClose} aria-label="Закрыть">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
          <div className="details-modal">
            <div className="details-modal-content">
              {/* Шапка */}
              {/* Шапка */}
              <div className="details-header">
                <PhotoUpload
                  currentPhoto={resume.photo_url}
                  onUpload={() => Promise.resolve()} // В модалке не позволяем менять фото
                  label="Фото профиля"
                  size="medium"
                  fallback={resume.full_name}
                />

                <div className="details-info">
                  <h2>{resume.full_name}</h2>
                  <p className="details-position">{resume.desired_position}</p>
                  <div className="details-badges">
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
                <div className="details-section">
                  <h3 className="section-title">Навыки</h3>
                  <div className="details-skills">
                    {resume.skills.map((skill, i) => (
                      <span key={i} className="skill-tag">{skill}</span>
                    ))}
                  </div>
                </div>
              )}

              {/* Контакты */}
              {(resume.phone || resume.telegram || resume.github || resume.linkedin) && (
                <div className="details-section">
                  <h3 className="section-title">Контакты</h3>
                  <div className="details-contacts">
                    {resume.phone && (
                      <a href={`tel:${resume.phone}`} className="contact-link" target="_blank" rel="noopener noreferrer">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                          <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z" />
                        </svg>
                        <span>{resume.phone}</span>
                      </a>
                    )}
                    {resume.telegram && (
                      <a href={`https://t.me/${resume.telegram.replace('@', '')}`} className="contact-link" target="_blank" rel="noopener noreferrer">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                          <path d="M11.944 0A12 12 0 0 0 0 12a12 12 0 0 0 12 12 12 12 0 0 0 12-12A12 12 0 0 0 12 0a12 12 0 0 0-.056 0zm4.962 7.224c.1-.002.321.023.465.14a.506.506 0 0 1 .171.325c.016.093.036.306.02.472-.18 1.898-.962 6.502-1.36 8.627-.168.9-.499 1.201-.82 1.23-.696.065-1.225-.46-1.9-.902-1.056-.693-1.653-1.124-2.678-1.8-1.185-.78-.417-1.21.258-1.91.177-.184 3.247-2.977 3.307-3.23.007-.032.014-.15-.056-.212s-.174-.041-.249-.024c-.106.024-1.793 1.14-5.061 3.345-.48.33-.913.49-1.302.48-.428-.008-1.252-.241-1.865-.44-.752-.245-1.349-.374-1.297-.789.027-.216.325-.437.893-.663 3.498-1.524 5.83-2.529 6.998-3.014 3.332-1.386 4.025-1.627 4.476-1.635z" />
                        </svg>
                        <span>{resume.telegram}</span>
                      </a>
                    )}
                    {resume.github && (
                      <a href={resume.github} className="contact-link" target="_blank" rel="noopener noreferrer">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                          <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z" />
                        </svg>
                        <span>{resume.github}</span>
                      </a>
                    )}
                    {resume.linkedin && (
                      <a href={resume.linkedin} className="contact-link" target="_blank" rel="noopener noreferrer">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                          <path d="M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433c-1.144 0-2.063-.926-2.063-2.065 0-1.138.92-2.063 2.063-2.063 1.14 0 2.064.925 2.064 2.063 0 1.139-.925 2.065-2.064 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z" />
                        </svg>
                        <span>{resume.linkedin}</span>
                      </a>
                    )}
                  </div>
                </div>
              )}

              {/* О себе */}
              {resume.about && (
                <div className="details-section">
                  <h3 className="section-title">О себе</h3>
                  <p className="details-about">{resume.about}</p>
                </div>
              )}

              {/* Опыт работы */}
              <div className="details-section">
                <div className="section-header-with-btn">
                  <h3 className="section-title">Опыт работы</h3>
                  <button
                    className="btn btn-small btn-primary"
                    onClick={() => setShowWorkExpForm(!showWorkExpForm)}
                  >
                    {showWorkExpForm ? 'Отмена' : '+ Добавить'}
                  </button>
                </div>

                {showWorkExpForm && (
                  <form className="work-exp-form" onSubmit={handleAddWorkExp}>
                    <div className="work-exp-form-row">
                      <input
                        type="text"
                        name="company"
                        value={workExpData.company}
                        onChange={handleWorkExpChange}
                        placeholder="Компания *"
                        required
                      />
                      <input
                        type="text"
                        name="position"
                        value={workExpData.position}
                        onChange={handleWorkExpChange}
                        placeholder="Должность *"
                        required
                      />
                    </div>
                    <div className="work-exp-form-row">
                      <input
                        type="date"
                        name="start_date"
                        value={workExpData.start_date}
                        onChange={handleWorkExpChange}
                        required
                      />
                      <input
                        type="date"
                        name="end_date"
                        value={workExpData.end_date}
                        onChange={handleWorkExpChange}
                        placeholder="По настоящее время"
                      />
                    </div>
                    <textarea
                      name="description"
                      value={workExpData.description}
                      onChange={handleWorkExpChange}
                      placeholder="Описание обязанностей и достижений..."
                      rows={3}
                    />
                    <button type="submit" className="btn btn-primary" disabled={loading}>
                      {loading ? 'Сохранение...' : 'Сохранить'}
                    </button>
                  </form>
                )}

                {resume.work_experience && resume.work_experience.length > 0 ? (
                  <div className="work-exp-list">
                    {resume.work_experience.map(exp => (
                      <div key={exp.id} className="work-exp-item">
                        <div className="work-exp-header">
                          <div>
                            <h4>{exp.position}</h4>
                            <p className="work-exp-company">{exp.company}</p>
                          </div>
                          <button
                            className="delete-exp-btn"
                            onClick={() => handleDeleteWorkExp(exp.id)}
                            title="Удалить"
                          >
                            ×
                          </button>
                        </div>
                        <p className="work-exp-dates">
                          {new Date(exp.start_date).toLocaleDateString('ru-RU', { month: 'short', year: 'numeric' })}
                          {' — '}
                          {exp.end_date
                            ? new Date(exp.end_date).toLocaleDateString('ru-RU', { month: 'short', year: 'numeric' })
                            : 'По настоящее время'
                          }
                        </p>
                        {exp.description && (
                          <p className="work-exp-description">{exp.description}</p>
                        )}
                      </div>
                    ))}
                  </div>
                ) : (
                  !showWorkExpForm && (
                    <p className="empty-text">Опыт работы не указан</p>
                  )
                )}
              </div>

              {/* Действия */}
              <div className="details-actions">
                <button
                  className="btn btn-primary"
                  onClick={() => setShowEditModal(true)}
                >
                  Редактировать
                </button>
                <button className="btn btn-danger" onClick={onDelete}>
                  Удалить резюме
                </button>
              </div>
            </div>
          </div>
        </div>
      </ModalPortal>
    </>
  )
}

export default ResumeDetailsModal