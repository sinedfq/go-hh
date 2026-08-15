import { useEffect, useRef, useState } from 'react'
import { loadYmaps } from '../common/YandexMap'
import './VacancyMap.css'

// Координаты городов России
const CITY_COORDS = {
    'Москва': { lat: 55.7558, lng: 37.6173 },
    'Санкт-Петербург': { lat: 59.9343, lng: 30.3351 },
    'Новосибирск': { lat: 55.0084, lng: 82.9357 },
    'Екатеринбург': { lat: 56.8389, lng: 60.6057 },
    'Казань': { lat: 55.7963, lng: 49.1088 },
    'Нижний Новгород': { lat: 56.2965, lng: 43.9361 },
    'Челябинск': { lat: 55.1644, lng: 61.4368 },
    'Самара': { lat: 53.1959, lng: 50.1008 },
    'Омск': { lat: 54.9885, lng: 73.3242 },
    'Ростов-на-Дону': { lat: 47.2357, lng: 39.7015 },
    'Уфа': { lat: 54.7388, lng: 55.9721 },
    'Красноярск': { lat: 56.0153, lng: 92.8932 },
    'Воронеж': { lat: 51.6720, lng: 39.1843 },
    'Пермь': { lat: 58.0105, lng: 56.2502 },
    'Волгоград': { lat: 48.7080, lng: 44.5133 },
}

const DEFAULT_CENTER = [55.7558, 49.1088]
const DEFAULT_ZOOM = 5
const CLUSTER_ZOOM_THRESHOLD = 5
const EXPAND_ZOOM_THRESHOLD = 10
const CIRCLE_RADIUS = 0.008

function pluralize(n) {
    const mod10 = n % 10
    const mod100 = n % 100
    if (mod10 === 1 && mod100 !== 11) return 'вакансия'
    if (mod10 >= 2 && mod10 <= 4 && (mod100 < 10 || mod100 >= 20)) return 'вакансии'
    return 'вакансий'
}

function getZoomLevel(zoom) {
    if (zoom < CLUSTER_ZOOM_THRESHOLD) return 0
    if (zoom < EXPAND_ZOOM_THRESHOLD) return 1
    return 2
}

function VacancyMap({ vacancies, selectedVacancy, onOpenVacancy, onSelectVacancy }) {
    const containerRef = useRef(null)
    const mapRef = useRef(null)
    const markersRef = useRef([])
    const lastCenteredVacancyId = useRef(null)
    const [mapReady, setMapReady] = useState(false)
    const [currentZoom, setCurrentZoom] = useState(DEFAULT_ZOOM)
    const [selectedPanel, setSelectedPanel] = useState(null)
    const [mapError, setMapError] = useState(false)

    const zoomLevel = getZoomLevel(currentZoom)
    const activeVacancyId = selectedVacancy?.id || null

    // Группируем ВСЕ вакансии по городам
    const grouped = vacancies.reduce((acc, v) => {
        const city = v.location || 'Не указан'
        if (!acc[city]) acc[city] = []
        acc[city].push(v)
        return acc
    }, {})

    // Центрирование при выборе вакансии (только один раз на вакансию)
    useEffect(() => {
        const map = mapRef.current
        if (!map || !mapReady || !selectedVacancy) return

        if (lastCenteredVacancyId.current === selectedVacancy.id) return
        lastCenteredVacancyId.current = selectedVacancy.id

        let position = null
        if (selectedVacancy.latitude && selectedVacancy.longitude &&
            selectedVacancy.latitude !== 0 && selectedVacancy.longitude !== 0) {
            position = [selectedVacancy.latitude, selectedVacancy.longitude]
        } else {
            const city = selectedVacancy.location
            const cityCoords = CITY_COORDS[city]
            if (cityCoords) position = [cityCoords.lat, cityCoords.lng]
        }

        if (position) {
            map.setCenter(position, EXPAND_ZOOM_THRESHOLD + 2, { duration: 500 })
        }
    }, [selectedVacancy?.id, mapReady])

    // Инициализация карты
    useEffect(() => {
        let cancelled = false

        loadYmaps()
            .then((ymaps) => {
                if (cancelled || !containerRef.current || mapRef.current) return

                mapRef.current = new ymaps.Map(containerRef.current, {
                    center: DEFAULT_CENTER,
                    zoom: DEFAULT_ZOOM,
                    controls: ['zoomControl'], // убрали fullscreenControl
                }, {
                    suppressMapOpenBlock: true,
                })

                setCurrentZoom(mapRef.current.getZoom())

                mapRef.current.events.add('boundschange', () => {
                    setCurrentZoom(mapRef.current.getZoom())
                })

                setMapReady(true)
            })
            .catch((err) => {
                console.error('Ошибка загрузки карты:', err)
                setMapError(true)
            })

        return () => {
            cancelled = true
            if (mapRef.current) {
                mapRef.current.destroy()
                mapRef.current = null
            }
        }
    }, [])

    // Центрирование кластера
    const getClusterCenter = () => {
        const allCoords = []
        vacancies.forEach(v => {
            if (v.latitude && v.longitude && v.latitude !== 0 && v.longitude !== 0) {
                allCoords.push([v.latitude, v.longitude])
            } else {
                const coords = CITY_COORDS[v.location]
                if (coords) allCoords.push([coords.lat, coords.lng])
            }
        })
        if (allCoords.length === 0) return DEFAULT_CENTER
        const lat = allCoords.reduce((s, c) => s + c[0], 0) / allCoords.length
        const lng = allCoords.reduce((s, c) => s + c[1], 0) / allCoords.length
        return [lat, lng]
    }

    // Создание HTML layout для маркера вакансии
    const createVacancyLayout = (ymaps, isActive, isHovered) => {
        let className = 'vacancy-marker'
        if (isActive) className += ' vacancy-marker-active'
        if (isHovered) className += ' vacancy-marker-hovered'

        return ymaps.templateLayoutFactory.createClass(
            '<div class="' + className + '">' +
            '<div class="vacancy-marker-box">' + (isActive ? '★' : '1') + '</div>' +
            '</div>'
        )
    }

    // Создание HTML layout для маркера города
    const createCityLayout = (ymaps, isActive, isHovered) => {
        let className = 'city-marker'
        if (isActive) className += ' city-marker-active'
        if (isHovered) className += ' city-marker-hovered'

        return ymaps.templateLayoutFactory.createClass(
            '<div class="' + className + '">' +
            '<div class="city-marker-box">{{ properties.count }}</div>' +
            '</div>'
        )
    }

    // Создание HTML layout для кластера
    const createClusterLayout = (ymaps, isHovered) => {
        let className = 'map-cluster'
        if (isHovered) className += ' map-cluster-hovered'

        return ymaps.templateLayoutFactory.createClass(
            '<div class="' + className + '">' +
            '<div class="map-cluster-box">{{ properties.count }}</div>' +
            '</div>'
        )
    }

    // Пересоздаём маркер при hover
    const updateMarkerLayout = (placemark, type, isActive, isHovered) => {
        const ymaps = window.ymaps
        if (!ymaps) return

        let newLayout
        if (type === 'cluster') {
            newLayout = createClusterLayout(ymaps, isHovered)
        } else if (type === 'city') {
            newLayout = createCityLayout(ymaps, isActive, isHovered)
        } else {
            newLayout = createVacancyLayout(ymaps, isActive, isHovered)
        }
        placemark.options.set('iconLayout', newLayout)
    }

    // Рисуем маркеры
    useEffect(() => {
        const map = mapRef.current
        const ymaps = window.ymaps
        if (!map || !mapReady || !ymaps) return

        markersRef.current.forEach(m => map.geoObjects.remove(m))
        markersRef.current = []

        const totalCount = vacancies.length
        if (totalCount === 0) return

        // ====== УРОВЕНЬ 0: КЛАСТЕР ======
        if (zoomLevel === 0) {
            const clusterCenter = getClusterCenter()

            const clusterPlacemark = new ymaps.Placemark(
                clusterCenter,
                {
                    hintContent: totalCount + ' ' + pluralize(totalCount),
                    count: totalCount,
                },
                {
                    iconLayout: createClusterLayout(ymaps, false),
                    iconShape: { type: 'Rectangle', coordinates: [[0, 0], [64, 64]] },
                    iconOffset: [-32, -32],
                }
            )

            clusterPlacemark.events.add('mouseenter', function () {
                updateMarkerLayout(clusterPlacemark, 'cluster', false, true)
                clusterPlacemark.options.set('zIndex', 500)
            })
            clusterPlacemark.events.add('mouseleave', function () {
                updateMarkerLayout(clusterPlacemark, 'cluster', false, false)
                clusterPlacemark.options.set('zIndex', 1)
            })

            clusterPlacemark.events.add('click', function () {
                map.setCenter(clusterCenter, CLUSTER_ZOOM_THRESHOLD + 1, { duration: 500 })
            })

            map.geoObjects.add(clusterPlacemark)
            markersRef.current.push(clusterPlacemark)
            return
        }

        // ====== УРОВЕНЬ 1: ГОРОДА ======
        if (zoomLevel === 1) {
            Object.entries(grouped).forEach(([city, cityVacancies]) => {
                const coords = CITY_COORDS[city]
                if (!coords) return

                const count = cityVacancies.length
                const hasActive = cityVacancies.some(v => v.id === activeVacancyId)

                const placemark = new ymaps.Placemark(
                    [coords.lat, coords.lng],
                    {
                        hintContent: city + ': ' + count + ' ' + pluralize(count),
                        count: count,
                    },
                    {
                        iconLayout: createCityLayout(ymaps, hasActive, false),
                        iconShape: { type: 'Rectangle', coordinates: [[0, 0], [48, 48]] },
                        iconOffset: [-24, -24],
                        zIndex: hasActive ? 100 : 1,
                    }
                )

                placemark.events.add('mouseenter', function () {
                    updateMarkerLayout(placemark, 'city', hasActive, true)
                    placemark.options.set('zIndex', 500)
                })
                placemark.events.add('mouseleave', function () {
                    updateMarkerLayout(placemark, 'city', hasActive, false)
                    placemark.options.set('zIndex', hasActive ? 100 : 1)
                })

                placemark.events.add('click', function () {
                    setSelectedPanel({ title: city, vacancies: cityVacancies })
                    map.setCenter([coords.lat, coords.lng], EXPAND_ZOOM_THRESHOLD, { duration: 400 })
                })

                map.geoObjects.add(placemark)
                markersRef.current.push(placemark)
            })
            return
        }

        // ====== УРОВЕНЬ 2: ОТДЕЛЬНЫЕ ВАКАНСИИ ======
        Object.entries(grouped).forEach(([city, cityVacancies]) => {
            const cityCoords = CITY_COORDS[city]

            const withCoords = cityVacancies.filter(v =>
                v.latitude && v.longitude && v.latitude !== 0 && v.longitude !== 0
            )
            const withoutCoords = cityVacancies.filter(v =>
                !(v.latitude && v.longitude && v.latitude !== 0 && v.longitude !== 0)
            )

            // Вакансии с координатами
            withCoords.forEach(v => {
                const isActive = v.id === activeVacancyId
                const coords = [v.latitude, v.longitude]

                const placemark = new ymaps.Placemark(
                    coords,
                    {
                        hintContent: v.title + ' — ' + v.company,
                    },
                    {
                        iconLayout: createVacancyLayout(ymaps, isActive, false),
                        iconShape: { type: 'Rectangle', coordinates: [[0, 0], [40, 40]] },
                        iconOffset: [-20, -20],
                        zIndex: isActive ? 100 : 1,
                    }
                )

                placemark.events.add('mouseenter', function () {
                    updateMarkerLayout(placemark, 'vacancy', isActive, true)
                    placemark.options.set('zIndex', 500)
                })
                placemark.events.add('mouseleave', function () {
                    updateMarkerLayout(placemark, 'vacancy', isActive, false)
                    placemark.options.set('zIndex', isActive ? 100 : 1)
                })

                placemark.events.add('click', function () {
                    if (onSelectVacancy) onSelectVacancy(v)
                    setSelectedPanel({ title: v.location || 'Вакансия', vacancies: [v] })
                })

                map.geoObjects.add(placemark)
                markersRef.current.push(placemark)
            })

            // Вакансии без координат — по кругу
            if (withoutCoords.length > 0 && cityCoords) {
                const angleStep = (2 * Math.PI) / withoutCoords.length

                withoutCoords.forEach((v, i) => {
                    const angle = i * angleStep
                    const lat = cityCoords.lat + CIRCLE_RADIUS * Math.sin(angle)
                    const lng = cityCoords.lng + CIRCLE_RADIUS * Math.cos(angle)
                    const isActive = v.id === activeVacancyId

                    const placemark = new ymaps.Placemark(
                        [lat, lng],
                        {
                            hintContent: v.title + ' — ' + v.company,
                        },
                        {
                            iconLayout: createVacancyLayout(ymaps, isActive, false),
                            iconShape: { type: 'Rectangle', coordinates: [[0, 0], [40, 40]] },
                            iconOffset: [-20, -20],
                            zIndex: isActive ? 100 : 1,
                        }
                    )

                    placemark.events.add('mouseenter', function () {
                        updateMarkerLayout(placemark, 'vacancy', isActive, true)
                        placemark.options.set('zIndex', 500)
                    })
                    placemark.events.add('mouseleave', function () {
                        updateMarkerLayout(placemark, 'vacancy', isActive, false)
                        placemark.options.set('zIndex', isActive ? 100 : 1)
                    })

                    placemark.events.add('click', function () {
                        if (onSelectVacancy) onSelectVacancy(v)
                        setSelectedPanel({ title: v.location || 'Вакансия', vacancies: [v] })
                    })

                    map.geoObjects.add(placemark)
                    markersRef.current.push(placemark)
                })
            }
        })
    }, [vacancies, mapReady, zoomLevel, grouped, activeVacancyId, onSelectVacancy])

    if (mapError) {
        return (
            <div className="vacancy-map">
                <div className="map-error">
                    <p>Не удалось загрузить карту</p>
                </div>
            </div>
        )
    }

    return (
        <div className="vacancy-map">
            <div ref={containerRef} className="map-container" />

            {selectedPanel && (
                <div className="map-city-panel animate-slide-up">
                    <div className="map-city-panel-header">
                        <h3>{selectedPanel.title}</h3>
                        <span className="map-city-count">
                            {selectedPanel.vacancies.length} {pluralize(selectedPanel.vacancies.length)}
                        </span>
                        <button className="map-city-close" onClick={() => setSelectedPanel(null)}>
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <line x1="18" y1="6" x2="6" y2="18" />
                                <line x1="6" y1="6" x2="18" y2="18" />
                            </svg>
                        </button>
                    </div>
                    <div className="map-city-list">
                        {selectedPanel.vacancies.map(v => (
                            <div
                                key={v.id}
                                className="map-city-item"
                                onClick={() => onOpenVacancy(v.id)}
                            >
                                <div className="map-city-item-title">{v.title}</div>
                                <div className="map-city-item-company">{v.company}</div>
                                <div className="map-city-item-meta">
                                    <span>{v.experience}</span>
                                    {v.remote && <span className="map-city-item-remote">Удалёнка</span>}
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}
        </div>
    )
}

export default VacancyMap