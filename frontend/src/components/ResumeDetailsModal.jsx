import { useState } from 'react'
import axios from 'axios'
import ModalPortal from './ModalPortal'
import ResumeModal from './ResumeModal'
import './ResumeDetailsModal.css'

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
              <div className="details-header">
                <div className="details-avatar">
                  {(resume.full_name && resume.full_name[0]) || '?'}
                </div>
                <div className="details-info">
                  <h2>{resume.full_name}</h2>
                  <p className="details-position">{resume.desired_position}</p>
                  <div className="details-badges">
                    <span className="badge">{resume.experience}</span>
                    {resume.city && <span className="badge">{resume.city}</span>}
                    {resume.remote && <span className="badge remote">Удалённо</span>}
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