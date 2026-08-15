import { useEffect, useRef } from 'react'
import './YandexMap.css'

const YANDEX_MAPS_API_KEY = '186fe7c5-d63b-402e-84d7-3ccae8e1b92b'

let ymapsPromise = null

// Экспортируем функцию чтобы другие компоненты могли её использовать
export function loadYmaps() {
    if (window.ymaps) {
        return Promise.resolve(window.ymaps)
    }
    if (ymapsPromise) {
        return ymapsPromise
    }
    ymapsPromise = new Promise((resolve, reject) => {
        const script = document.createElement('script')
        script.src = `https://api-maps.yandex.ru/2.1/?apikey=${YANDEX_MAPS_API_KEY}&lang=ru_RU`
        script.async = true
        script.onload = () => {
            window.ymaps.ready(() => resolve(window.ymaps))
        }
        script.onerror = () => reject(new Error('Не удалось загрузить Яндекс.Карты'))
        document.head.appendChild(script)
    })
    return ymapsPromise
}

function YandexMap({ latitude, longitude, title, address }) {
    const containerRef = useRef(null)
    const mapRef = useRef(null)

    useEffect(() => {
        let destroyed = false

        loadYmaps()
            .then((ymaps) => {
                if (destroyed || !containerRef.current) return

                // Создаём карту
                mapRef.current = new ymaps.Map(
                    containerRef.current,
                    {
                        center: [latitude, longitude],
                        zoom: 15,
                        controls: ['zoomControl']
                    },
                    {
                        suppressMapOpenBlock: true
                    }
                )

                // Кастомный layout маркера (в стиле VacancyMap)
                const markerLayout = ymaps.templateLayoutFactory.createClass(
                    '<div class="detail-map-marker">' +
                    '<div class="detail-map-marker-box">' +
                    '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">' +
                    '<path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>' +
                    '<circle cx="12" cy="10" r="3"/>' +
                    '</svg>' +
                    '</div>' +
                    '</div>'
                )

                // Маркер без балуна и подписи
                const placemark = new ymaps.Placemark(
                    [latitude, longitude],
                    {},
                    {
                        iconLayout: markerLayout,
                        iconShape: {
                            type: 'Rectangle',
                            coordinates: [[0, 0], [44, 44]]
                        },
                        iconOffset: [-22, -44], // маркер указывает острием вниз
                        draggable: false,
                    }
                )

                mapRef.current.geoObjects.add(placemark)
            })
            .catch((err) => {
                console.error('Ошибка загрузки Яндекс.Карт:', err)
            })

        return () => {
            destroyed = true
            if (mapRef.current) {
                mapRef.current.destroy()
                mapRef.current = null
            }
        }
    }, [latitude, longitude])

    return <div ref={containerRef} className="yandex-map" />
}

export default YandexMap