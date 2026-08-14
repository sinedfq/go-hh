import { useEffect, useRef } from 'react'

const YANDEX_MAPS_API_KEY = '186fe7c5-d63b-402e-84d7-3ccae8e1b92b'

// Кэшируем промис загрузки чтобы скрипт подключался один раз
let ymapsPromise = null

function loadYmaps() {
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
                        zoom: 14,
                        controls: ['zoomControl']
                    },
                    {
                        suppressMapOpenBlock: true // не блокировать скролл страницы
                    }
                )

                // Метка вакансии
                const placemark = new ymaps.Placemark(
                    [latitude, longitude],
                    {
                        balloonContentHeader: `<strong>${title}</strong>`,
                        balloonContentBody: address || '',
                        hintContent: title
                    },
                    {
                        preset: 'islands#blueDotIconWithCaption',
                        iconCaption: title
                    }
                )

                mapRef.current.geoObjects.add(placemark)
            })
            .catch((err) => {
                console.error('Ошибка загрузки Яндекс.Карт:', err)
            })

        // Очистка при размонтировании
        return () => {
            destroyed = true
            if (mapRef.current) {
                mapRef.current.destroy()
                mapRef.current = null
            }
        }
    }, [latitude, longitude, title, address])

    return <div ref={containerRef} className="yandex-map" />
}

export default YandexMap