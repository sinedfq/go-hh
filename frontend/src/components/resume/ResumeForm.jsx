import { useState, useRef, useEffect } from 'react'
import { formatPhone, normalizePhone, normalizeUrl, normalizeTelegram } from '../../utils/format'
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
    phone: formatPhone(initialData?.phone || ''), // форматируем для отображения
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

  // ====== Загрузка библиотек ======
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

  const handlePhoneChange = (e) => {
    const value = e.target.value
    setFormData(prev => ({ ...prev, phone: formatPhone(value) }))
  }

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

  // ====== Навыки ======
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

  // ====== Должности ======
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
    // Если клик был внутри списка подсказок — НЕ закрываем dropdown
    // и НЕ создаём должность (это сделает selectPosition)
    const relatedTarget = e.relatedTarget
    if (relatedTarget && positionSuggestionsRef.current &&
      positionSuggestionsRef.current.contains(relatedTarget)) {
      return
    }

    setTimeout(async () => {
      // Если dropdown всё ещё открыт — значит это был клик по подсказке
      if (showPositionSuggestions) {
        return
      }

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

  // ====== Опыт работы ======
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

  // ====== Отправка формы ======
  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      // Нормализуем контакты перед отправкой
      const payload = {
        ...formData,
        phone: normalizePhone(formData.phone),
        telegram: normalizeTelegram(formData.telegram),
        github: normalizeUrl(formData.github),
        linkedin: normalizeUrl(formData.linkedin)
      }

      let updatedResume

      if (isEditMode) {
        const res = await axios.put('/api/my-resume', payload)
        updatedResume = res.data

        // ... код обновления опыта работы
      } else {
        const resumeRes = await axios.post('/api/resumes', payload)
        updatedResume = resumeRes.data

        // ... код создания опыта работы
      }

      onSuccess(updatedResume)
    } catch (err) {
      const message = err.response?.data?.error || 'Ошибка сохранения резюме'
      setError(message)
    } finally {
      setLoading(false)
    }
  }

  const selectedExperience = EXPERIENCE_LEVELS.find(
    level => level.value === formData.experience
  )

  // Остальной JSX без изменений
  return (
    <div className="resume-form-wrapper">
      <form onSubmit={handleSubmit} className="resume-form">
        <h3>{isEditMode ? 'Редактирование резюме' : 'Создание резюме'}</h3>

        {/* ====== Основная информация ====== */}
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
                        e.preventDefault()  // Предотвращает blur input'а
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
                  strokeLinecap="round"
                  strokeLinejoin="round"
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
                      {formData.experience === level.value && (
                        <svg className="option-check" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                          <polyline points="20 6 9 17 4 12" />
                        </svg>
                      )}
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

        {/* ====== Навыки ====== */}
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

        {/* ====== О себе ====== */}
        <div className="form-group">
          <label>О себе</label>
          <textarea
            name="about"
            value={formData.about}
            onChange={handleChange}
            placeholder="Расскажите о своём опыте, проектах, достижениях..."
            rows={3}
          />
        </div>

        {/* ====== Контакты ====== */}
        <div className="form-group contacts-section">
          <label>Контакты для связи</label>
          <div className="contacts-grid">
            <div className="contact-input-wrapper">
              <svg className="contact-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z" />
              </svg>
              <input
                type="tel"
                name="phone"
                value={formData.phone}
                onChange={handlePhoneChange}
                placeholder="+7 (999) 999-99-99"
                maxLength="18"
              />
            </div>

            <div className="contact-input-wrapper">
              <svg className="contact-icon" width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                <path d="M11.944 0A12 12 0 0 0 0 12a12 12 0 0 0 12 12 12 12 0 0 0 12-12A12 12 0 0 0 12 0a12 12 0 0 0-.056 0zm4.962 7.224c.1-.002.321.023.465.14a.506.506 0 0 1 .171.325c.016.093.036.306.02.472-.18 1.898-.962 6.502-1.36 8.627-.168.9-.499 1.201-.82 1.23-.696.065-1.225-.46-1.9-.902-1.056-.693-1.653-1.124-2.678-1.8-1.185-.78-.417-1.21.258-1.91.177-.184 3.247-2.977 3.307-3.23.007-.032.014-.15-.056-.212s-.174-.041-.249-.024c-.106.024-1.793 1.14-5.061 3.345-.48.33-.913.49-1.302.48-.428-.008-1.252-.241-1.865-.44-.752-.245-1.349-.374-1.297-.789.027-.216.325-.437.893-.663 3.498-1.524 5.83-2.529 6.998-3.014 3.332-1.386 4.025-1.627 4.476-1.635z" />
              </svg>
              <input
                type="text"
                name="telegram"
                value={formData.telegram}
                onChange={handleChange}
                placeholder="@username"
              />
            </div>

            <div className="contact-input-wrapper">
              <svg className="contact-icon" width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z" />
              </svg>
              <input
                type="url"
                name="github"
                value={formData.github}
                onChange={handleChange}
                placeholder="github.com/username"
              />
            </div>

            <div className="contact-input-wrapper">
              <svg className="contact-icon" width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                <path d="M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433c-1.144 0-2.063-.926-2.063-2.065 0-1.138.92-2.063 2.063-2.063 1.14 0 2.064.925 2.064 2.063 0 1.139-.925 2.065-2.064 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z" />
              </svg>
              <input
                type="url"
                name="linkedin"
                value={formData.linkedin}
                onChange={handleChange}
                placeholder="linkedin.com/in/username"
              />
            </div>
          </div>
        </div>

        {/* ====== Удалённая работа ====== */}
        <div className="form-group">
          <div className="remote-toggle">
            <div className="remote-toggle-info">
              <span className="remote-toggle-title">Удалённая работа</span>
              <span className="remote-toggle-subtitle">Готов работать удалённо</span>
            </div>
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

        {/* ====== Опыт работы ====== */}
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
                    title="Удалить"
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
                  <div className="date-input-wrapper">
                    <label>С</label>
                    <input
                      type="date"
                      value={exp.start_date ? exp.start_date.split('T')[0] : ''}
                      onChange={(e) => updateWorkExperience(exp.id, 'start_date', e.target.value)}
                    />
                  </div>
                  <div className="date-input-wrapper">
                    <label>По</label>
                    <input
                      type="date"
                      value={exp.end_date ? exp.end_date.split('T')[0] : ''}
                      onChange={(e) => updateWorkExperience(exp.id, 'end_date', e.target.value)}
                    />
                  </div>
                </div>

                <textarea
                  placeholder="Описание обязанностей и достижений..."
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