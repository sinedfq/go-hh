import { useState, useEffect, useRef } from 'react'
import './FilterPanel.css'

function FilterPanel({ filters, onFiltersChange, onApply, onReset, cities = [] }) {
    const [mounted, setMounted] = useState(false)
    const [showCityDropdown, setShowCityDropdown] = useState(false)
    const dropdownRef = useRef(null)

    useEffect(() => {
        const t = setTimeout(() => setMounted(true), 10)
        return () => clearTimeout(t)
    }, [])

    // Закрытие по клику вне
    useEffect(() => {
        const handleClickOutside = (e) => {
            if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
                setShowCityDropdown(false)
            }
        }
        document.addEventListener('mousedown', handleClickOutside)
        return () => document.removeEventListener('mousedown', handleClickOutside)
    }, [])

    const toggleValue = (key, value) => {
        const current = filters[key]
        onFiltersChange({ ...filters, [key]: current === value ? '' : value })
    }

    const toggleRemote = () => {
        onFiltersChange({ ...filters, remote: filters.remote ? null : true })
    }

    const selectCity = (city) => {
        onFiltersChange({ ...filters, location: city })
        setShowCityDropdown(false)
    }

    const activeCount = [
        filters.location,
        filters.experience,
        filters.remote === true
    ].filter(Boolean).length

    return (
        <div className={`filter-panel ${mounted ? 'visible' : ''}`}>
            {/* ====== ГОРОД — кастомный dropdown ====== */}
            <div className="filter-section">
                <span className="filter-label">Город</span>
                <div className="custom-select" ref={dropdownRef}>
                    <button
                        type="button"
                        className={`custom-select-btn ${showCityDropdown ? 'open' : ''} ${filters.location ? 'has-value' : ''}`}
                        onClick={() => setShowCityDropdown(!showCityDropdown)}
                    >
                        <span className="custom-select-value">
                            {filters.location || 'Любой город'}
                        </span>
                        <svg className="custom-select-arrow" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <polyline points="6 9 12 15 18 9" />
                        </svg>
                    </button>

                    {showCityDropdown && (
                        <div className="custom-select-dropdown">
                            <div
                                className={`custom-select-option ${!filters.location ? 'selected' : ''}`}
                                onClick={() => selectCity('')}
                            >
                                Любой город
                            </div>
                            {cities.map(city => (
                                <div
                                    key={city}
                                    className={`custom-select-option ${filters.location === city ? 'selected' : ''}`}
                                    onClick={() => selectCity(city)}
                                >
                                    {city}
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>

            {/* ====== ОПЫТ ====== */}
            <div className="filter-section">
                <span className="filter-label">Опыт работы</span>
                <div className="chip-row">
                    {['Junior', 'Middle', 'Senior'].map(exp => (
                        <button
                            key={exp}
                            type="button"
                            className={`chip ${filters.experience === exp ? 'active' : ''}`}
                            onClick={() => toggleValue('experience', exp)}
                        >
                            {exp}
                        </button>
                    ))}
                </div>
            </div>

            {/* ====== УДАЛЁНКА ====== */}
            <div className="filter-section">
                <span className="filter-label">Формат работы</span>
                <label className="toggle-switch">
                    <input
                        type="checkbox"
                        checked={filters.remote === true}
                        onChange={toggleRemote}
                    />
                    <span className="toggle-slider"></span>
                    <span className="toggle-text">Только удалёнка</span>
                </label>
            </div>

            {/* ====== КНОПКИ ====== */}
            <div className="filter-actions">
                <button className="btn btn-secondary btn-small" onClick={onReset}>
                    {activeCount > 0 && <span className="reset-badge">{activeCount}</span>}
                    Сбросить
                </button>
                <button className="btn btn-primary btn-small" onClick={onApply}>
                    Применить
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                        <polyline points="9 18 15 12 9 6" />
                    </svg>
                </button>
            </div>
        </div>
    )
}

export default FilterPanel