import { useState, useEffect, useRef } from 'react'
import { loadYmaps } from '../../components/common/YandexMap'

function VacancyMapPicker({ address, latitude, longitude, onAddressChange, onCoordinatesChange }) {
    const [ymaps, setYmaps] = useState(null)
    const [map, setMap] = useState(null)
    const [searchQuery, setSearchQuery] = useState(address || '')
    const [searching, setSearching] = useState(false)
    const [searchError, setSearchError] = useState('')
    const [suggestions, setSuggestions] = useState([])
    const mapRef = useRef(null)
    const placemarkRef = useRef(null)
    const searchInputRef = useRef(null)

    useEffect(() => {
        let destroyed = false
        loadYmaps()
            .then((ym) => {
                if (!destroyed) setYmaps(ym)
            })
            .catch((err) => {
                console.error('Ошибка загрузки Яндекс.Карт:', err)
                setSearchError('Не удалось загрузить карту')
            })
        return () => { destroyed = true }
    }, [])

    useEffect(() => {
        if (!ymaps || !mapRef.current || map) return

        const initialCenter = latitude && longitude
            ? [parseFloat(latitude), parseFloat(longitude)]
            : [55.0415, 82.7056]
        const initialZoom = latitude && longitude ? 15 : 5

        const newMap = new ymaps.Map(mapRef.current, {
            center: initialCenter,
            zoom: initialZoom,
            controls: ['zoomControl']
        })

        const newPlacemark = new ymaps.Placemark(initialCenter, {
            balloonContent: 'Перетащите для точного указания'
        }, {
            draggable: true,
            preset: 'islands#redDotIcon'
        })

        newPlacemark.events.add('dragend', () => {
            const coords = newPlacemark.geometry.getCoordinates()
            onCoordinatesChange(coords[0].toFixed(6), coords[1].toFixed(6))
        })

        newMap.geoObjects.add(newPlacemark)

        newMap.events.add('click', (e) => {
            const coords = e.get('coords')
            newPlacemark.geometry.setCoordinates(coords)
            onCoordinatesChange(coords[0].toFixed(6), coords[1].toFixed(6))
        })

        setMap(newMap)
        placemarkRef.current = newPlacemark

        return () => {
            if (newMap) newMap.destroy()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [ymaps, mapRef.current])

    useEffect(() => {
        if (placemarkRef.current && latitude && longitude) {
            const lat = parseFloat(latitude)
            const lng = parseFloat(longitude)
            if (!isNaN(lat) && !isNaN(lng)) {
                placemarkRef.current.geometry.setCoordinates([lat, lng])
            }
        }
    }, [latitude, longitude])

    // ====== ГЕОКОДИНГ через прокси бэкенда ======
    const geocodeAddress = async (query) => {
        const res = await fetch(`/api/geocode?address=${encodeURIComponent(query)}`)
        if (!res.ok) {
            const err = await res.json()
            throw new Error(err.error || 'Ошибка геокодера')
        }
        return res.json()
    }

    const geocodeSuggest = async (query) => {
        const res = await fetch(`/api/geocode/suggest?address=${encodeURIComponent(query)}`)
        if (!res.ok) return []
        return res.json()
    }

    // ====== ПОИСК ======
    const handleSearch = async () => {
        if (!searchQuery.trim()) {
            setSearchError('Введите адрес')
            return
        }

        setSearching(true)
        setSearchError('')
        setSuggestions([])

        try {
            const result = await geocodeAddress(searchQuery)

            if (map) {
                map.setCenter([result.latitude, result.longitude], 16, { duration: 500 })
            }

            if (placemarkRef.current) {
                placemarkRef.current.geometry.setCoordinates([result.latitude, result.longitude])
            }

            onAddressChange(result.address)
            onCoordinatesChange(result.latitude.toFixed(6), result.longitude.toFixed(6))
            setSearchQuery(result.address)
        } catch (err) {
            console.error('Ошибка геокодинга:', err)
            setSearchError('Не удалось найти адрес. Попробуйте другой запрос.')
        } finally {
            setSearching(false)
        }
    }

    // ====== АВТОДОПОЛНЕНИЕ ======
    useEffect(() => {
        if (!searchQuery.trim() || searchQuery.length < 3) {
            setSuggestions([])
            return
        }

        const timeout = setTimeout(async () => {
            try {
                const results = await geocodeSuggest(searchQuery)
                setSuggestions(results || [])
            } catch (err) {
                // Тихо
            }
        }, 400)

        return () => clearTimeout(timeout)
    }, [searchQuery])

    const selectSuggestion = async (suggestion) => {
        setSuggestions([])
        setSearchQuery(suggestion.address)

        if (map) {
            map.setCenter([suggestion.latitude, suggestion.longitude], 16, { duration: 500 })
        }

        if (placemarkRef.current) {
            placemarkRef.current.geometry.setCoordinates([suggestion.latitude, suggestion.longitude])
        }

        onAddressChange(suggestion.address)
        onCoordinatesChange(suggestion.latitude.toFixed(6), suggestion.longitude.toFixed(6))
    }

    const handleKeyDown = (e) => {
        if (e.key === 'Enter') {
            e.preventDefault()
            handleSearch()
        }
    }

    const handleBlur = () => {
        setTimeout(() => setSuggestions([]), 200)
    }

    return (
        <div className="vacancy-map-picker">
            <div className="map-search-row" style={{ position: 'relative' }}>
                <input
                    ref={searchInputRef}
                    type="text"
                    className="map-search-input"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    onKeyDown={handleKeyDown}
                    onBlur={handleBlur}
                    placeholder="Например: Новосибирск, Ленина 1"
                />
                <button
                    type="button"
                    className="btn btn-primary map-search-btn"
                    onClick={handleSearch}
                    disabled={searching}
                >
                    {searching ? 'Поиск...' : 'Найти'}
                </button>

                {suggestions.length > 0 && (
                    <div className="map-suggestions">
                        {suggestions.map((s, i) => (
                            <button
                                key={i}
                                type="button"
                                className="map-suggestion-item"
                                onMouseDown={(e) => {
                                    e.preventDefault()
                                    selectSuggestion(s)
                                }}
                            >
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
                                    <circle cx="12" cy="10" r="3" />
                                </svg>
                                {s.address}
                            </button>
                        ))}
                    </div>
                )}
            </div>

            {searchError && (
                <div className="map-search-error">{searchError}</div>
            )}

            <div className="map-hint">
                💡 Начните вводить адрес — появятся подсказки. Кликните на карту или перетащите маркер.
            </div>

            <div ref={mapRef} className="vacancy-map-container" />

            {latitude && longitude && (
                <div className="map-coords">
                    Координаты: {latitude}, {longitude}
                </div>
            )}
        </div>
    )
}

export default VacancyMapPicker