import { useState, useEffect } from 'react'
import './SearchBar.css'

function SearchBar({ value, onChange, placeholder = 'Поиск вакансий...' }) {
    const [localValue, setLocalValue] = useState(value || '')

    useEffect(() => {
        setLocalValue(value || '')
    }, [value])

    useEffect(() => {
        const timer = setTimeout(() => {
            if (localValue !== value) {
                onChange(localValue)
            }
        }, 300) // debounce 300ms

        return () => clearTimeout(timer)
    }, [localValue])

    const handleClear = () => {
        setLocalValue('')
        onChange('')
    }

    return (
        <div className="search-bar">
            <svg className="search-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <input
                type="text"
                className="search-input"
                value={localValue}
                onChange={(e) => setLocalValue(e.target.value)}
                placeholder={placeholder}
            />
            {localValue && (
                <button className="search-clear" onClick={handleClear}>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <line x1="18" y1="6" x2="6" y2="18" />
                        <line x1="6" y1="6" x2="18" y2="18" />
                    </svg>
                </button>
            )}
        </div>
    )
}

export default SearchBar