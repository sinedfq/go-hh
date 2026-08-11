import { useState, useRef, useEffect } from 'react'
import axios from 'axios'
import './ResumeForm.css'

const EXPERIENCE_LEVELS = [
  { value: 'Junior', label: 'Junior', description: '0-2 года опыта' },
  { value: 'Middle', label: 'Middle', description: '2-5 лет опыта' },
  { value: 'Senior', label: 'Senior', description: '5+ лет опыта' }
]

function ResumeForm({ onSuccess, onCancel }) {
  const [formData, setFormData] = useState({
    full_name: '',
    desired_position: '',
    experience: 'Junior',
    skills: [],
    about: '',
    city: '',
    remote: false
  })
  const [skillInput, setSkillInput] = useState('')
  const [workExperience, setWorkExperience] = useState([])
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
    const handleClickOutside = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setDropdownOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  // Загрузка библиотеки навыков
  useEffect(() => {
    const loadSkills = async () => {
      try {
        const res = await axios.get('/api/skills')
        setSkillsLibrary(res.data || [])
      } catch (err) {
        console.error('Ошибка загрузки навыков:', err)
      }
    }
    loadSkills()
  }, [])

  // Закрытие предложений при клике вне
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (suggestionsRef.current && !suggestionsRef.current.contains(e.target)) {
        setShowSuggestions(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  // Загрузка библиотеки должностей
  useEffect(() => {
    const loadPositions = async () => {
      try {
        const res = await axios.get('/api/positions')
        setPositionsLibrary(res.data || [])
      } catch (err) {
        console.error('Ошибка загрузки должностей:', err)
      }
    }
    loadPositions()
  }, [])

  // Закрытие предложений должностей при клике вне
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (positionSuggestionsRef.current && !positionSuggestionsRef.current.contains(e.target)) {
        setShowPositionSuggestions(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  // Фильтрация должностей при вводе
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

  // Выбор должности из предложений
  const selectPosition = (positionName) => {
    setFormData(prev => ({ ...prev, desired_position: positionName }))
    setShowPositionSuggestions(false)
  }

  // Добавление новой должности в библиотеку при потере фокуса
  const handlePositionBlur = async () => {
    // Небольшая задержка чтобы клик по предложению успел сработать
    setTimeout(async () => {
      setShowPositionSuggestions(false)
      const position = formData.desired_position.trim()
      if (position && !positionsLibrary.some(p => p.name.toLowerCase() === position.toLowerCase())) {
        try {
          await axios.post('/api/positions', { name: position })
          setPositionsLibrary(prev => [...prev, { id: Date.now(), name: position }])
        } catch (err) {
          console.error('Ошибка добавления должности в библиотеку:', err)
        }
      }
    }, 200)
  }

  // Фильтрация предложений при вводе
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

  // Выбор навыка из предложений
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

  // Добавление нового навыка (в форму и в библиотеку)
  const addNewSkill = async () => {
    const skill = skillInput.trim()
    if (!skill) return

    if (!formData.skills.includes(skill)) {
      setFormData(prev => ({
        ...prev,
        skills: [...prev.skills, skill]
      }))
    }

    // Добавляем в библиотеку на сервере
    try {
      await axios.post('/api/skills', { name: skill })
      // Обновляем локальную библиотеку если навыка ещё нет
      setSkillsLibrary(prev => {
        if (!prev.some(s => s.name.toLowerCase() === skill.toLowerCase())) {
          return [...prev, { id: Date.now(), name: skill }]
        }
        return prev
      })
    } catch (err) {
      console.error('Ошибка добавления навыка в библиотеку:', err)
    }

    setSkillInput('')
    setShowSuggestions(false)
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
      // Если есть точное совпадение в библиотеке — выбираем его
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

  // ====== Опыт работы ======
  const addWorkExperience = () => {
    setWorkExperience(prev => [...prev, {
      id: Date.now(), // временный ID для UI
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
      // 1. Создаём резюме
      const resumeRes = await axios.post('/api/resumes', formData)
      let updatedResume = resumeRes.data

      // 2. Добавляем опыт работы (если есть)
      for (const exp of workExperience) {
        // Пропускаем пустые записи
        if (!exp.company || !exp.position || !exp.start_date) continue

        try {
          const expRes = await axios.post('/api/work-experience', {
            company: exp.company,
            position: exp.position,
            start_date: exp.start_date,
            end_date: exp.end_date || null,
            description: exp.description
          })
          updatedResume = expRes.data // сервер возвращает обновлённое резюме
        } catch (err) {
          console.error('Ошибка добавления опыта:', err)
        }
      }

      onSuccess(updatedResume)
    } catch (err) {
      const message = err.response?.data?.error || 'Ошибка создания резюме'
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
        <h3>Создание резюме</h3>

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
                      onClick={() => selectPosition(pos.name)}
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

        {/* ====== Навыки с автодополнением ====== */}
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
                    onClick={() => selectSkill(skill.name)}
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
                      value={exp.start_date}
                      onChange={(e) => updateWorkExperience(exp.id, 'start_date', e.target.value)}
                    />
                  </div>
                  <div className="date-input-wrapper">
                    <label>По</label>
                    <input
                      type="date"
                      value={exp.end_date}
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
            {loading ? 'Создание...' : 'Создать резюме'}
          </button>
        </div>
      </form>
    </div>
  )
}

export default ResumeForm