import { useState, useEffect, useRef } from 'react'
import './SearchBar.css'

function SearchBar({ value, onChange, onSubmit, placeholder = 'Поиск вакансий...' }) {
    const [localValue, setLocalValue] = useState(value || '')
    const changeRef=useRef(onChange)
    useEffect(()=>{changeRef.current=onChange},[onChange])

    useEffect(() => {
        setLocalValue(value || '')
    }, [value])

    useEffect(() => {
        const timer = setTimeout(() => {
            if (localValue !== value) {
                changeRef.current(localValue)
            }
        }, 300) // debounce 300ms

        return () => clearTimeout(timer)
    }, [localValue,value])

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
                aria-label="Поиск вакансий"
                onKeyDown={event=>{if(event.key==='Enter')onSubmit?.(localValue)}}
                className="search-input"
                value={localValue}
                onChange={(e) => setLocalValue(e.target.value)}
                placeholder={placeholder}
            />
            {localValue && (
                <button type="button" aria-label="Очистить поиск" className="search-clear" onClick={handleClear}>
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
