import { useState, useRef, useEffect } from 'react'
import axios from 'axios'
import './ResumeForm.css'

const EXPERIENCE_LEVELS = [
  { value: 'Junior', label: 'Junior', description: '0-2 года опыта' },
  { value: 'Middle', label: 'Middle', description: '2-5 лет опыта' },
  { value: 'Senior', label: 'Senior', description: '5+ лет опыта' }
]

function ResumeForm({ onSuccess, onCancel, initialData, isEditMode }) {
  const [formData, setFormData] = useState({
    full_name: initialData?.full_name || '',
    desired_position: initialData?.desired_position || '',
    experience: initialData?.experience || 'Junior',
    skills: initialData?.skills || [],
    about: initialData?.about || '',
    city: initialData?.city || '',
    remote: initialData?.remote || false,
    phone: initialData?.phone || '',
    telegram: initialData?.telegram || '',
    github: initialData?.github || '',
    linkedin: initialData?.linkedin || ''
  })

  const [skillInput, setSkillInput] = useState('')
  const [workExperience, setWorkExperience] = useState(initialData?.work_experience || [])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [dropdownOpen, setDropdownOpen] = useState(false)
  const dropdownRef = useRef(null)

  const [skillsLibrary, setSkillsLibrary] = useState([])
  const [skillSuggestions, setSkillSuggestions] = useState([])
  const [showSuggestions, setShowSuggestions] = useState(false)
  const suggestionsRef = useRef(null)

  const [positionsLibrary, setPositionsLibrary] = useState([])
  const [positionSuggestions, setPositionSuggestions] = useState([])
  const [showPositionSuggestions, setShowPositionSuggestions] = useState(false)
  const positionSuggestionsRef = useRef(null)

  useEffect(() => {
    const loadLibraries = async () => {
      try {
        const [skillsRes, positionsRes] = await Promise.all([
          axios.get('/api/skills'),
          axios.get('/api/positions')
        ])
        setSkillsLibrary(skillsRes.data || [])
        setPositionsLibrary(positionsRes.data || [])
      } catch (err) {
        console.error('Ошибка загрузки библиотек:', err)
      }
    }
    loadLibraries()
  }, [])

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setDropdownOpen(false)
      }
      if (suggestionsRef.current && !suggestionsRef.current.contains(e.target)) {
        setShowSuggestions(false)
      }
      if (positionSuggestionsRef.current && !positionSuggestionsRef.current.contains(e.target)) {
        setShowPositionSuggestions(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }))
  }

  const handleExperienceChange = (level) => {
    setFormData(prev => ({ ...prev, experience: level }))
    setDropdownOpen(false)
  }

  const addSkill = () => {
    const skill = skillInput.trim()
    if (skill && !formData.skills.includes(skill)) {
      setFormData(prev => ({
        ...prev,
        skills: [...prev.skills, skill]
      }))
      setSkillInput('')
    }
  }

  const removeSkill = (skillToRemove) => {
    setFormData(prev => ({
      ...prev,
      skills: prev.skills.filter(s => s !== skillToRemove)
    }))
  }

  const handleSkillKeyDown = (e) => {
    if (e.key === 'Enter') {
      e.preventDefault()
      const exactMatch = skillsLibrary.find(
        s => s.name.toLowerCase() === skillInput.trim().toLowerCase()
      )
      if (exactMatch) {
        selectSkill(exactMatch.name)
      } else {
        addNewSkill()
      }
    }
  }

  const handleSkillInputChange = (e) => {
    const value = e.target.value
    setSkillInput(value)

    if (value.trim()) {
      const filtered = skillsLibrary
        .filter(skill =>
          skill.name.toLowerCase().includes(value.toLowerCase()) &&
          !formData.skills.includes(skill.name)
        )
        .slice(0, 8)
      setSkillSuggestions(filtered)
      setShowSuggestions(filtered.length > 0)
    } else {
      setSkillSuggestions([])
      setShowSuggestions(false)
    }
  }

  const selectSkill = (skillName) => {
    if (!formData.skills.includes(skillName)) {
      setFormData(prev => ({
        ...prev,
        skills: [...prev.skills, skillName]
      }))
    }
    setSkillInput('')
    setShowSuggestions(false)
  }

  const addNewSkill = async () => {
    const skill = skillInput.trim()
    if (!skill) return

    if (!formData.skills.includes(skill)) {
      setFormData(prev => ({
        ...prev,
        skills: [...prev.skills, skill]
      }))
    }

    try {
      await axios.post('/api/skills', { name: skill })
      setSkillsLibrary(prev => {
        if (!prev.some(s => s.name.toLowerCase() === skill.toLowerCase())) {
          return [...prev, { id: Date.now(), name: skill }]
        }
        return prev
      })
    } catch (err) {
      console.error('Ошибка добавления навыка:', err)
    }

    setSkillInput('')
    setShowSuggestions(false)
  }

  const handlePositionInputChange = (e) => {
    const value = e.target.value
    setFormData(prev => ({ ...prev, desired_position: value }))

    if (value.trim()) {
      const filtered = positionsLibrary
        .filter(pos => pos.name.toLowerCase().includes(value.toLowerCase()))
        .slice(0, 8)
      setPositionSuggestions(filtered)
      setShowPositionSuggestions(filtered.length > 0)
    } else {
      setPositionSuggestions([])
      setShowPositionSuggestions(false)
    }
  }

  const selectPosition = (positionName) => {
    setFormData(prev => ({ ...prev, desired_position: positionName }))
    setShowPositionSuggestions(false)
  }

  const handlePositionBlur = async (e) => {
    const relatedTarget = e.relatedTarget
    if (relatedTarget && positionSuggestionsRef.current &&
      positionSuggestionsRef.current.contains(relatedTarget)) {
      return
    }

    setTimeout(async () => {
      if (showPositionSuggestions) return

      const position = formData.desired_position.trim()
      if (position && !positionsLibrary.some(p => p.name.toLowerCase() === position.toLowerCase())) {
        try {
          await axios.post('/api/positions', { name: position })
          setPositionsLibrary(prev => [...prev, { id: Date.now(), name: position }])
        } catch (err) {
          console.error('Ошибка добавления должности:', err)
        }
      }
      setShowPositionSuggestions(false)
    }, 200)
  }

  const addWorkExperience = () => {
    setWorkExperience(prev => [...prev, {
      id: Date.now(),
      company: '',
      position: '',
      start_date: '',
      end_date: '',
      description: ''
    }])
  }

  const updateWorkExperience = (id, field, value) => {
    setWorkExperience(prev => prev.map(exp =>
      exp.id === id ? { ...exp, [field]: value } : exp
    ))
  }

  const removeWorkExperience = (id) => {
    setWorkExperience(prev => prev.filter(exp => exp.id !== id))
  }

  // ====== ОТПРАВКА ФОРМЫ (ИСПРАВЛЕНО) ======
  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      const payload = {
        ...formData,
        phone: formData.phone || '',
        telegram: formData.telegram || '',
        github: formData.github || '',
        linkedin: formData.linkedin || ''
      }

      let updatedResume

      if (isEditMode) {
        // Обновляем основные данные резюме
        const res = await axios.put('/api/my-resume', payload)
        updatedResume = res.data

        // ====== ОБРАБОТКА ОПЫТА РАБОТЫ ======
        const existingExp = initialData?.work_experience || []

        // 1. Удаляем те записи которых нет в новом списке (у которых id — число из БД)
        for (const exp of existingExp) {
          const stillExists = workExperience.some(e => e.id === exp.id)
          if (!stillExists) {
            try {
              await axios.delete(`/api/work-experience/${exp.id}`)
            } catch (err) {
              console.error('Ошибка удаления опыта:', err)
            }
          }
        }

        // 2. Добавляем новые записи (у которых id — timestamp)
        for (const exp of workExperience) {
          if (typeof exp.id === 'number' && exp.id > 1000000000000) {
            // Новый опыт (Date.now() как id)
            try {
              await axios.post('/api/work-experience', {
                company: exp.company,
                position: exp.position,
                start_date: exp.start_date,
                end_date: exp.end_date || null,
                description: exp.description
              })
            } catch (err) {
              console.error('Ошибка добавления опыта:', err)
            }
          }
        }

        // Перезагружаем резюме чтобы получить обновлённый опыт работы
        const reloadRes = await axios.get('/api/my-resume')
        updatedResume = reloadRes.data
      } else {
        // Создание нового резюме
        const resumeRes = await axios.post('/api/resumes', payload)
        updatedResume = resumeRes.data

        // Создаём опыт работы для нового резюме
        for (const exp of workExperience) {
          if (exp.company && exp.position && exp.start_date) {
            try {
              await axios.post('/api/work-experience', {
                company: exp.company,
                position: exp.position,
                start_date: exp.start_date,
                end_date: exp.end_date || null,
                description: exp.description
              })
            } catch (err) {
              console.error('Ошибка добавления опыта:', err)
            }
          }
        }

        // Перезагружаем резюме
        const reloadRes = await axios.get('/api/my-resume')
        updatedResume = reloadRes.data
      }

      onSuccess(updatedResume)
    } catch (err) {
      console.error('Ошибка сохранения резюме:', err)
      const message = err.response?.data?.error || err.message || 'Ошибка сохранения резюме'
      setError(message)
    } finally {
      setLoading(false)
    }
  }

  const selectedExperience = EXPERIENCE_LEVELS.find(
    level => level.value === formData.experience
  )

  return (
    <div className="resume-form-wrapper">
      <form onSubmit={handleSubmit} className="resume-form">
        <h3>{isEditMode ? 'Редактирование резюме' : 'Создание резюме'}</h3>

        <div className="form-row">
          <div className="form-group">
            <label>Полное имя *</label>
            <input
              type="text"
              name="full_name"
              value={formData.full_name}
              onChange={handleChange}
              placeholder="Иван Иванов"
              required
            />
          </div>

          <div className="form-group">
            <label>Желаемая должность *</label>
            <div className="position-input-wrapper" ref={positionSuggestionsRef}>
              <input
                type="text"
                name="desired_position"
                value={formData.desired_position}
                onChange={handlePositionInputChange}
                onBlur={handlePositionBlur}
                placeholder="Начните вводить должность..."
                required
                autoComplete="off"
              />

              {showPositionSuggestions && positionSuggestions.length > 0 && (
                <div className="position-suggestions">
                  {positionSuggestions.map(pos => (
                    <button
                      key={pos.id}
                      type="button"
                      className="position-suggestion-item"
                      onMouseDown={(e) => {
                        e.preventDefault()
                        selectPosition(pos.name)
                      }}
                    >
                      {pos.name}
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>

        <div className="form-row">
          <div className="form-group">
            <label>Уровень опыта</label>
            <div className="custom-dropdown" ref={dropdownRef}>
              <button
                type="button"
                className={`dropdown-trigger ${dropdownOpen ? 'open' : ''}`}
                onClick={() => setDropdownOpen(!dropdownOpen)}
              >
                <span className="dropdown-value">
                  {selectedExperience?.label || 'Выберите уровень'}
                </span>
                <svg
                  className={`dropdown-arrow ${dropdownOpen ? 'rotated' : ''}`}
                  width="16"
                  height="16"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                >
                  <polyline points="6 9 12 15 18 9" />
                </svg>
              </button>

              {dropdownOpen && (
                <div className="dropdown-menu">
                  {EXPERIENCE_LEVELS.map(level => (
                    <button
                      key={level.value}
                      type="button"
                      className={`dropdown-option ${formData.experience === level.value ? 'selected' : ''}`}
                      onClick={() => handleExperienceChange(level.value)}
                    >
                      <div className="option-content">
                        <span className="option-label">{level.label}</span>
                        <span className="option-description">{level.description}</span>
                      </div>
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>

          <div className="form-group">
            <label>Город</label>
            <input
              type="text"
              name="city"
              value={formData.city}
              onChange={handleChange}
              placeholder="Москва"
            />
          </div>
        </div>

        <div className="form-group">
          <label>Навыки</label>
          <div className="skills-input-wrapper" ref={suggestionsRef}>
            <div className="skills-input">
              <input
                type="text"
                value={skillInput}
                onChange={handleSkillInputChange}
                onKeyDown={handleSkillKeyDown}
                onFocus={() => skillInput && setShowSuggestions(true)}
                placeholder="Начните вводить навык..."
                autoComplete="off"
              />
              <button type="button" onClick={addNewSkill} className="add-skill-btn">+</button>
            </div>

            {showSuggestions && skillSuggestions.length > 0 && (
              <div className="skills-suggestions">
                {skillSuggestions.map(skill => (
                  <button
                    key={skill.id}
                    type="button"
                    className="skill-suggestion-item"
                    onMouseDown={(e) => {
                      e.preventDefault()
                      selectSkill(skill.name)
                    }}
                  >
                    {skill.name}
                  </button>
                ))}
              </div>
            )}
          </div>

          {formData.skills.length > 0 && (
            <div className="skills-list">
              {formData.skills.map(skill => (
                <span key={skill} className="skill-tag-form">
                  {skill}
                  <button type="button" onClick={() => removeSkill(skill)} className="remove-skill">×</button>
                </span>
              ))}
            </div>
          )}
        </div>

        <div className="form-group">
          <label>О себе</label>
          <textarea
            name="about"
            value={formData.about}
            onChange={handleChange}
            placeholder="Расскажите о своём опыте..."
            rows={3}
          />
        </div>

        <div className="form-group contacts-section">
          <label>Контакты для связи</label>
          <div className="contacts-grid">
            <input
              type="tel"
              name="phone"
              value={formData.phone}
              onChange={handleChange}
              placeholder="+7 (999) 999-99-99"
            />
            <input
              type="text"
              name="telegram"
              value={formData.telegram}
              onChange={handleChange}
              placeholder="@username"
            />
            <input
              type="url"
              name="github"
              value={formData.github}
              onChange={handleChange}
              placeholder="github.com/username"
            />
            <input
              type="url"
              name="linkedin"
              value={formData.linkedin}
              onChange={handleChange}
              placeholder="linkedin.com/in/username"
            />
          </div>
        </div>

        <div className="form-group">
          <div className="remote-toggle">
            <div className="remote-toggle-info">
              <span className="remote-toggle-title">Удалённая работа</span>
              <span className="remote-toggle-subtitle">
                {formData.remote ? 'Готов работать удалённо' : 'Только офис'}
              </span>
            </div>
            <div className="toggle-with-label">
              <span className={`toggle-status ${formData.remote ? 'active' : ''}`}>
                {formData.remote ? 'Вкл' : 'Выкл'}
              </span>
              <label className="toggle-switch">
                <input
                  type="checkbox"
                  name="remote"
                  checked={formData.remote}
                  onChange={handleChange}
                />
                <span className="toggle-slider"></span>
              </label>
            </div>
          </div>
        </div>

        <div className="form-group work-experience-section">
          <div className="section-header-form">
            <label>Опыт работы</label>
            <button type="button" className="add-experience-btn" onClick={addWorkExperience}>
              + Добавить
            </button>
          </div>

          {workExperience.length === 0 && (
            <p className="empty-work-exp">Нажмите "Добавить", чтобы указать опыт работы</p>
          )}

          <div className="work-experience-list">
            {workExperience.map((exp, index) => (
              <div key={exp.id} className="work-experience-item">
                <div className="work-exp-item-header">
                  <span className="work-exp-index">#{index + 1}</span>
                  <button
                    type="button"
                    className="remove-exp-btn"
                    onClick={() => removeWorkExperience(exp.id)}
                  >
                    ×
                  </button>
                </div>

                <div className="work-exp-row">
                  <input
                    type="text"
                    placeholder="Компания *"
                    value={exp.company}
                    onChange={(e) => updateWorkExperience(exp.id, 'company', e.target.value)}
                  />
                  <input
                    type="text"
                    placeholder="Должность *"
                    value={exp.position}
                    onChange={(e) => updateWorkExperience(exp.id, 'position', e.target.value)}
                  />
                </div>

                <div className="work-exp-row">
                  <input
                    type="date"
                    value={exp.start_date ? exp.start_date.split('T')[0] : ''}
                    onChange={(e) => updateWorkExperience(exp.id, 'start_date', e.target.value)}
                  />
                  <input
                    type="date"
                    value={exp.end_date ? exp.end_date.split('T')[0] : ''}
                    onChange={(e) => updateWorkExperience(exp.id, 'end_date', e.target.value)}
                  />
                </div>

                <textarea
                  placeholder="Описание..."
                  rows={2}
                  value={exp.description}
                  onChange={(e) => updateWorkExperience(exp.id, 'description', e.target.value)}
                />
              </div>
            ))}
          </div>
        </div>

        {error && <div className="form-error">{error}</div>}

        <div className="form-actions">
          <button type="button" className="btn btn-secondary" onClick={onCancel}>
            Отмена
          </button>
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading
              ? (isEditMode ? 'Сохранение...' : 'Создание...')
              : (isEditMode ? 'Сохранить изменения' : 'Создать резюме')
            }
          </button>
        </div>
      </form>
    </div>
  )
}

export default ResumeForm