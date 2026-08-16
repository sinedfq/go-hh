import { useState, useRef, useEffect } from 'react'
import VacancyMapPicker from './VacancyMapPicker'
import './EmployerPages.css'

function CreateVacancyPage({ onCreate, onCancel, cities = [], allSkills = [], allPositions = [] }) {
    const [form, setForm] = useState({
        title: '',
        location: '',
        experience: '',
        remote: false,
        skills: [],
        description: '',
        address: '',
        latitude: '',
        longitude: ''
    })
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState('')

    // ====== НАЗВАНИЕ ВАКАНСИИ (автодополнение из /api/positions) ======
    const [titleInput, setTitleInput] = useState('')
    const [showTitleSuggestions, setShowTitleSuggestions] = useState(false)
    const titleInputRef = useRef(null)

    const titleSuggestions = allPositions.filter(pos =>
        pos.toLowerCase().includes(titleInput.toLowerCase()) &&
        pos.toLowerCase() !== form.title.toLowerCase()
    ).slice(0, 8)

    useEffect(() => {
        const handleClickOutside = (e) => {
            if (titleInputRef.current && !titleInputRef.current.contains(e.target)) {
                setShowTitleSuggestions(false)
            }
        }
        document.addEventListener('mousedown', handleClickOutside)
        return () => document.removeEventListener('mousedown', handleClickOutside)
    }, [])

    const selectTitle = (title) => {
        setForm(prev => ({ ...prev, title }))
        setTitleInput(title)
        setShowTitleSuggestions(false)
    }

    // ====== ГОРОД ======
    const [showCityDropdown, setShowCityDropdown] = useState(false)
    const [citySearch, setCitySearch] = useState('')
    const cityDropdownRef = useRef(null)

    useEffect(() => {
        const handleClickOutside = (e) => {
            if (cityDropdownRef.current && !cityDropdownRef.current.contains(e.target)) {
                setShowCityDropdown(false)
            }
        }
        document.addEventListener('mousedown', handleClickOutside)
        return () => document.removeEventListener('mousedown', handleClickOutside)
    }, [])

    const filteredCities = cities.filter(city =>
        city.toLowerCase().includes(citySearch.toLowerCase())
    )

    const selectCity = (city) => {
        setForm(prev => ({ ...prev, location: city }))
        setShowCityDropdown(false)
        setCitySearch(city)
    }

    // ====== ОПЫТ ======
    const [showExpDropdown, setShowExpDropdown] = useState(false)
    const expDropdownRef = useRef(null)

    const experienceOptions = [
        { value: 'Junior', label: 'Junior', desc: 'До 1 года опыта' },
        { value: 'Middle', label: 'Middle', desc: '1-3 года опыта' },
        { value: 'Senior', label: 'Senior', desc: '3-6 лет опыта' },
        { value: 'Lead', label: 'Lead', desc: '6+ лет опыта' }
    ]

    useEffect(() => {
        const handleClickOutside = (e) => {
            if (expDropdownRef.current && !expDropdownRef.current.contains(e.target)) {
                setShowExpDropdown(false)
            }
        }
        document.addEventListener('mousedown', handleClickOutside)
        return () => document.removeEventListener('mousedown', handleClickOutside)
    }, [])

    const selectExperience = (exp) => {
        setForm(prev => ({ ...prev, experience: exp }))
        setShowExpDropdown(false)
    }

    // ====== НАВЫКИ ======
    const [skillInput, setSkillInput] = useState('')
    const [showSkillSuggestions, setShowSkillSuggestions] = useState(false)
    const skillInputRef = useRef(null)

    useEffect(() => {
        const handleClickOutside = (e) => {
            if (skillInputRef.current && !skillInputRef.current.contains(e.target)) {
                setShowSkillSuggestions(false)
            }
        }
        document.addEventListener('mousedown', handleClickOutside)
        return () => document.removeEventListener('mousedown', handleClickOutside)
    }, [])

    const skillSuggestions = allSkills.filter(skill =>
        skill.toLowerCase().includes(skillInput.toLowerCase()) &&
        !form.skills.includes(skill)
    ).slice(0, 5)

    const addSkill = (skill) => {
        if (skill && !form.skills.includes(skill)) {
            setForm(prev => ({ ...prev, skills: [...prev.skills, skill] }))
        }
        setSkillInput('')
        setShowSkillSuggestions(false)
    }

    const removeSkill = (skill) => {
        setForm(prev => ({
            ...prev,
            skills: prev.skills.filter(s => s !== skill)
        }))
    }

    const handleSkillKeyDown = (e) => {
        if (e.key === 'Enter' && skillInput.trim()) {
            e.preventDefault()
            addSkill(skillInput.trim())
        }
    }

    // ====== ОТПРАВКА ======
    const handleChange = (field, value) => {
        setForm(prev => ({ ...prev, [field]: value }))
    }

    const handleSubmit = async (e) => {
        e.preventDefault()
        setError('')

        if (!form.title.trim()) {
            setError('Укажите название вакансии')
            return
        }
        if (!form.location.trim()) {
            setError('Выберите город')
            return
        }
        if (!form.experience) {
            setError('Выберите требуемый опыт')
            return
        }

        setLoading(true)
        try {
            const vacancyData = {
                title: form.title,
                location: form.location,
                experience: form.experience,
                remote: form.remote,
                skills: form.skills,
                description: form.description,
                address: form.address,
                latitude: form.latitude ? parseFloat(form.latitude) : 0,
                longitude: form.longitude ? parseFloat(form.longitude) : 0
            }

            await onCreate(vacancyData)
        } catch (err) {
            const rawMessage = err.response?.data?.error || 'Ошибка при создании вакансии'
            const errorMap = {
                'title, location and experience required': 'Заполните обязательные поля',
                'you must create a company first': 'Сначала создайте компанию',
                'unauthorized': 'Требуется авторизация',
                'forbidden: insufficient role': 'Недостаточно прав'
            }
            setError(errorMap[rawMessage] || rawMessage)
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="employer-page">
            <div className="employer-header">
                <div>
                    <h1>Создать вакансию</h1>
                    <p className="employer-subtitle">Заполните информацию о вакансии</p>
                </div>
                {onCancel && (
                    <button className="btn btn-secondary" onClick={onCancel}>
                        Отмена
                    </button>
                )}
            </div>

            <form className="vacancy-form" onSubmit={handleSubmit}>
                {error && <div className="employer-error">{error}</div>}

                {/* ====== НАЗВАНИЕ ВАКАНСИИ С АВТОДОПОЛНЕНИЕМ ====== */}
                <div className="form-field" ref={titleInputRef}>
                    <label>Название вакансии *</label>
                    <div className="position-input-wrapper">
                        <input
                            type="text"
                            value={titleInput}
                            onChange={(e) => {
                                setTitleInput(e.target.value)
                                setForm(prev => ({ ...prev, title: e.target.value }))
                                setShowTitleSuggestions(e.target.value.length > 0)
                            }}
                            onFocus={() => setShowTitleSuggestions(titleInput.length > 0)}
                            placeholder="Начните вводить должность..."
                            required
                        />

                        {showTitleSuggestions && titleSuggestions.length > 0 && (
                            <div className="position-suggestions">
                                {titleSuggestions.map(pos => (
                                    <button
                                        key={pos}
                                        type="button"
                                        className="position-suggestion-item"
                                        onClick={() => selectTitle(pos)}
                                    >
                                        {pos}
                                    </button>
                                ))}
                            </div>
                        )}
                    </div>
                </div>

                <div className="form-row-2">
                    {/* ====== ГОРОД ====== */}
                    <div className="form-field" ref={cityDropdownRef}>
                        <label>Город *</label>
                        <div className="city-picker-wrapper">
                            <input
                                type="text"
                                className="city-picker-input"
                                value={citySearch}
                                onChange={(e) => {
                                    setCitySearch(e.target.value)
                                    setShowCityDropdown(true)
                                }}
                                onFocus={() => setShowCityDropdown(true)}
                                placeholder="Начните вводить город..."
                                autoComplete="off"
                            />

                            {form.location && form.location !== citySearch && (
                                <span className="city-selected-badge">
                                    ✓ {form.location}
                                </span>
                            )}

                            {showCityDropdown && citySearch.length > 0 && (
                                <div className="city-dropdown">
                                    {filteredCities.length === 0 ? (
                                        <div className="city-dropdown-empty">Город не найден</div>
                                    ) : (
                                        filteredCities.map(city => (
                                            <div
                                                key={city}
                                                className={`city-dropdown-item ${form.location === city ? 'selected' : ''}`}
                                                onClick={() => selectCity(city)}
                                            >
                                                {city}
                                            </div>
                                        ))
                                    )}

                                    {citySearch.trim() && !filteredCities.some(c => c.toLowerCase() === citySearch.toLowerCase()) && (
                                        <div
                                            className="city-dropdown-item city-dropdown-add"
                                            onClick={() => {
                                                const newCity = citySearch.trim()
                                                setForm(prev => ({ ...prev, location: newCity }))
                                                setShowCityDropdown(false)
                                                setCitySearch(newCity)
                                            }}
                                        >
                                            + Добавить "{citySearch.trim()}"
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>
                    </div>

                    {/* ====== ОПЫТ ====== */}
                    <div className="form-field" ref={expDropdownRef}>
                        <label>Требуемый опыт *</label>
                        <div className="custom-dropdown">
                            <button
                                type="button"
                                className={`dropdown-trigger ${showExpDropdown ? 'open' : ''} ${form.experience ? 'has-value' : ''}`}
                                onClick={() => setShowExpDropdown(!showExpDropdown)}
                            >
                                <span className="dropdown-value">
                                    {form.experience || 'Выберите опыт'}
                                </span>
                                <svg className={`dropdown-arrow ${showExpDropdown ? 'rotated' : ''}`} width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <polyline points="6 9 12 15 18 9" />
                                </svg>
                            </button>

                            {showExpDropdown && (
                                <div className="dropdown-menu">
                                    {experienceOptions.map(opt => (
                                        <button
                                            key={opt.value}
                                            type="button"
                                            className={`dropdown-option ${form.experience === opt.value ? 'selected' : ''}`}
                                            onClick={() => selectExperience(opt.value)}
                                        >
                                            <div className="option-content">
                                                <span className="option-label">{opt.label}</span>
                                                <span className="option-description">{opt.desc}</span>
                                            </div>
                                            {form.experience === opt.value && (
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
                </div>

                {/* ====== УДАЛЁНКА ====== */}
                <div className="form-field">
                    <label className="remote-toggle-label">
                        <input
                            type="checkbox"
                            checked={form.remote}
                            onChange={(e) => handleChange('remote', e.target.checked)}
                        />
                        <span>Удалённая работа</span>
                    </label>
                </div>

                {/* ====== НАВЫКИ ====== */}
                <div className="form-field" ref={skillInputRef}>
                    <label>Требуемые навыки</label>
                    <div className="skills-input-wrapper">
                        <div className="skills-input">
                            <input
                                type="text"
                                value={skillInput}
                                onChange={(e) => {
                                    setSkillInput(e.target.value)
                                    setShowSkillSuggestions(e.target.value.length > 0)
                                }}
                                onKeyDown={handleSkillKeyDown}
                                onFocus={() => setShowSkillSuggestions(skillInput.length > 0)}
                                placeholder="Начните вводить навык и нажмите Enter"
                            />
                            <button
                                type="button"
                                className="add-skill-btn"
                                onClick={() => addSkill(skillInput.trim())}
                                disabled={!skillInput.trim()}
                            >
                                +
                            </button>
                        </div>

                        {showSkillSuggestions && skillSuggestions.length > 0 && (
                            <div className="skills-suggestions">
                                {skillSuggestions.map(skill => (
                                    <button
                                        key={skill}
                                        type="button"
                                        className="skill-suggestion-item"
                                        onClick={() => addSkill(skill)}
                                    >
                                        {skill}
                                    </button>
                                ))}
                            </div>
                        )}
                    </div>

                    {form.skills.length > 0 && (
                        <div className="skills-list">
                            {form.skills.map(skill => (
                                <span key={skill} className="skill-tag-form">
                                    {skill}
                                    <button
                                        type="button"
                                        className="remove-skill"
                                        onClick={() => removeSkill(skill)}
                                    >
                                        ×
                                    </button>
                                </span>
                            ))}
                        </div>
                    )}
                </div>

                {/* ====== ОПИСАНИЕ ====== */}
                <div className="form-field">
                    <label>Описание вакансии</label>
                    <textarea
                        value={form.description}
                        onChange={(e) => handleChange('description', e.target.value)}
                        placeholder="Расскажите о обязанностях, требованиях и условиях..."
                        rows={6}
                    />
                </div>

                {/* ====== РАСПОЛОЖЕНИЕ С КАРТОЙ ====== */}
                <div className="form-section-title">
                    Расположение офиса
                </div>

                <div className="form-field">
                    <VacancyMapPicker
                        address={form.address}
                        latitude={form.latitude}
                        longitude={form.longitude}
                        onAddressChange={(addr) => handleChange('address', addr)}
                        onCoordinatesChange={(lat, lng) => {
                            setForm(prev => ({
                                ...prev,
                                latitude: lat.toString(),
                                longitude: lng.toString()
                            }))
                        }}
                    />
                </div>

                <div className="form-actions">
                    <button type="button" className="btn btn-secondary" onClick={onCancel}>
                        Отмена
                    </button>
                    <button type="submit" className="btn btn-primary" disabled={loading}>
                        {loading ? 'Создание...' : 'Создать вакансию'}
                    </button>
                </div>
            </form>
        </div>
    )
}

export default CreateVacancyPage    