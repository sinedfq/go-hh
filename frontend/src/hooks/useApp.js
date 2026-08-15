import { useState, useEffect, useRef } from 'react'
import axios from 'axios'
import { useAuth } from '../contexts/AuthContext'

export function useApp() {
    const { user, loading: authLoading, logout } = useAuth()

    // ====== ВАКАНСИИ И ИЗБРАННОЕ ======
    const [vacancies, setVacancies] = useState([])
    const [favorites, setFavorites] = useState([])
    const [currentIndex, setCurrentIndex] = useState(-1)
    const [loading, setLoading] = useState(true)

    // Выбранная вакансия через ID (сохраняется в sessionStorage)
    const [selectedVacancyId, setSelectedVacancyId] = useState(() => {
        const saved = sessionStorage.getItem('selectedVacancyId')
        return saved ? parseInt(saved) : null
    })

    // Вычисляемая выбранная вакансия
    const selectedVacancy = vacancies.find(v => v.id === selectedVacancyId) || vacancies[0] || null

    // ====== РЕЖИМЫ И МОДАЛКИ ======
    const [mode, setMode] = useState(() => {
        return sessionStorage.getItem('currentMode') || 'browse'
    })
    const [showLoginModal, setShowLoginModal] = useState(false)

    // ====== РЕКОМЕНДАЦИИ ======
    const [recommendations, setRecommendations] = useState([])
    const [selectedRecommendedVacancy, setSelectedRecommendedVacancy] = useState(null)
    const [recommendationsLoaded, setRecommendationsLoaded] = useState(false)
    const [recommendationsLoading, setRecommendationsLoading] = useState(false)

    // ====== РЕЗЮМЕ ======
    const [resume, setResume] = useState(null)
    const [matchScores, setMatchScores] = useState({})

    // ====== СТРАНИЦЫ (вакансия/компания) ======
    const [vacancyPageId, setVacancyPageId] = useState(null)
    const [companyPageId, setCompanyPageId] = useState(null)

    // ====== ПОИСК И ФИЛЬТРЫ ======
    const [searchFilters, setSearchFilters] = useState(() => {
        const params = new URLSearchParams(window.location.search)
        return {
            query: params.get('q') || '',
            location: params.get('location') || '',
            experience: params.get('experience') || '',
            remote: params.get('remote') === 'true' ? true : null,
            skills: params.get('skills') ? params.get('skills').split(',').map(s => s.trim()).filter(Boolean) : []
        }
    })

    const [cities, setCities] = useState([])
    const [allSkills, setAllSkills] = useState([])

    const [searchResults, setSearchResults] = useState([])
    const [searchTotal, setSearchTotal] = useState(0)
    const [searchLoading, setSearchLoading] = useState(false)

    // ====== СКРОЛЛ (через ref для корректного чтения) ======
    const scrollPositionsRef = useRef({})

    useEffect(() => {
        const saved = sessionStorage.getItem('scrollPositions')
        if (saved) {
            try {
                scrollPositionsRef.current = JSON.parse(saved)
            } catch (e) {
                scrollPositionsRef.current = {}
            }
        }
    }, [])

    // ====== ЗАГРУЗКА ДАННЫХ ======

    const loadVacancies = async () => {
        try {
            setLoading(true)
            const res = await axios.get('/api/vacancies')
            const data = Array.isArray(res.data) ? res.data : []
            setVacancies(data)
            setCurrentIndex(data.length - 1)
            setLoading(false)
        } catch (err) {
            console.error('Ошибка загрузки вакансий:', err)
            setLoading(false)
        }
    }

    const loadFilterOptions = async () => {
        try {
            const res = await axios.get('/api/vacancies')
            const allVacancies = Array.isArray(res.data) ? res.data : []
            const uniqueCities = [...new Set(allVacancies.map(v => v.location).filter(Boolean))]
            setCities(uniqueCities.sort())

            const skillsRes = await axios.get('/api/skills')
            const skills = Array.isArray(skillsRes.data) ? skillsRes.data : []
            setAllSkills(skills.map(s => s.name).sort())
        } catch (err) {
            console.error('Ошибка загрузки опций фильтров:', err)
        }
    }

    const loadFavorites = async () => {
        try {
            const res = await axios.get('/api/favorites')
            const data = Array.isArray(res.data) ? res.data : []
            setFavorites(data)
        } catch (err) {
            console.error('Ошибка загрузки избранного:', err)
        }
    }

    const loadRecommendations = async (force = false) => {
        if (recommendationsLoaded && !force) return

        setRecommendationsLoading(true)
        try {
            const url = force ? '/api/recommendations?refresh=true' : '/api/recommendations'
            const res = await axios.get(url)
            const recs = res.data.recommendations || []
            setRecommendations(recs)

            const scores = {}
            recs.forEach(rec => {
                scores[rec.vacancy.id] = rec.score
            })
            setMatchScores(scores)

            setSelectedRecommendedVacancy(recs.length > 0 ? recs[0].vacancy : null)
            setRecommendationsLoaded(true)
        } catch (err) {
            console.error('Ошибка загрузки рекомендаций:', err)
        } finally {
            setRecommendationsLoading(false)
        }
    }

    // ====== ИЗБРАННОЕ ======

    const addToFavorites = async (vacancyId) => {
        if (!user) {
            setShowLoginModal(true)
            return
        }
        try {
            await axios.post('/api/favorites', { vacancy_id: vacancyId })
            await loadFavorites()
        } catch (err) {
            console.error('Ошибка добавления в избранное:', err)
        }
    }

    const removeFromFavorites = async (vacancyId) => {
        if (!user) return
        try {
            await axios.delete(`/api/favorites/${vacancyId}`)
            await loadFavorites()
        } catch (err) {
            console.error('Ошибка удаления из избранного:', err)
        }
    }

    const isFavorite = (vacancyId) => {
        return favorites.some(fav => fav.id === vacancyId)
    }

    // ====== СВАЙПЫ ======

    const handleSwipe = (direction, vacancyId) => {
        if (direction === 'right') {
            addToFavorites(vacancyId)
        }
        setCurrentIndex(prev => {
            const nextIndex = prev - 1
            return nextIndex >= 0 ? nextIndex : prev
        })
    }

    // ====== ВЫБОР ВАКАНСИИ В ПРОСМОТРЕ ======

    const setSelectedVacancy = (vacancy) => {
        if (vacancy && vacancy.id) {
            setSelectedVacancyId(vacancy.id)
            sessionStorage.setItem('selectedVacancyId', vacancy.id.toString())
        } else {
            setSelectedVacancyId(null)
            sessionStorage.removeItem('selectedVacancyId')
        }
    }

    // ====== СКРОЛЛ ======

    const saveScrollPosition = () => {
        const content = document.querySelector('.content')
        if (content) {
            const position = content.scrollTop
            scrollPositionsRef.current = {
                ...scrollPositionsRef.current,
                [mode]: position
            }
            sessionStorage.setItem('scrollPositions', JSON.stringify(scrollPositionsRef.current))
        }
    }

    const restoreScrollPosition = (targetMode) => {
        requestAnimationFrame(() => {
            requestAnimationFrame(() => {
                const content = document.querySelector('.content')
                const position = scrollPositionsRef.current[targetMode]
                if (content && position !== undefined) {
                    content.scrollTop = position
                } else if (content) {
                    content.scrollTop = 0
                }
            })
        })
    }

    // ====== НАВИГАЦИЯ ======

    const openVacancyPage = (id) => {
        setCompanyPageId(null)
        setVacancyPageId(id)

        const params = new URLSearchParams(window.location.search)
        params.set('vacancy', id)
        // Убираем company из URL если есть
        params.delete('company')
        window.history.pushState({ vacancyId: id, companyId: null }, '', `?${params.toString()}`)
    }

    const closeVacancyPage = () => {
        setVacancyPageId(null)
        window.history.back()
    }

    const openCompanyPage = (id) => {
        setVacancyPageId(null)
        setCompanyPageId(id)

        const params = new URLSearchParams(window.location.search)
        params.set('company', id)
        // Сохраняем vacancy в URL если есть
        window.history.pushState({ vacancyId: vacancyPageId, companyId: id }, '', `?${params.toString()}`)
    }

    const closeCompanyPage = () => {
        setCompanyPageId(null)
        window.history.back()
    }

    const clearSearchFromURL = () => {
        const params = new URLSearchParams(window.location.search)
        const searchKeys = ['q', 'location', 'experience', 'remote', 'skills']
        const hasSearchParams = searchKeys.some(key => params.has(key))

        if (hasSearchParams) {
            searchKeys.forEach(key => params.delete(key))
            const newUrl = params.toString()
                ? `?${params.toString()}`
                : window.location.pathname
            window.history.replaceState({}, '', newUrl)
        }
    }

    const handleModeChange = (newMode) => {
        if (vacancyPageId || companyPageId) {
            setVacancyPageId(null)
            setCompanyPageId(null)
            window.history.back()
        }

        // Очищаем URL от фильтров поиска при смене режима
        clearSearchFromURL()

        if ((newMode === 'swipe' || newMode === 'favorites' || newMode === 'recommendations') && !user) {
            setShowLoginModal(true)
            return
        }

        setMode(newMode)
    }

    // ====== ФИЛЬТРЫ ======

    const updateFilters = (filters) => {
        setSearchFilters(filters)
    }

    const applyFilters = async (filters, goToSearch = false) => {
        setSearchFilters(filters)

        const params = new URLSearchParams()
        if (filters.query) params.set('q', filters.query)
        if (filters.location) params.set('location', filters.location)
        if (filters.experience) params.set('experience', filters.experience)
        if (filters.remote === true) params.set('remote', 'true')
        if (filters.skills && filters.skills.length > 0) {
            params.set('skills', filters.skills.join(','))
        }

        const newUrl = params.toString() ? `?${params.toString()}` : window.location.pathname
        window.history.replaceState({}, '', newUrl)

        try {
            setSearchLoading(true)
            const res = await axios.get(`/api/vacancies/search?${params.toString()}`)
            setSearchResults(res.data.vacancies || [])
            setSearchTotal(res.data.total_count || 0)

            if (goToSearch) {
                setMode('search')
            }
        } catch (err) {
            console.error('Ошибка поиска:', err)
        } finally {
            setSearchLoading(false)
        }
    }

    const resetFilters = () => {
        const empty = { query: '', location: '', experience: '', remote: null, skills: [] }
        setSearchFilters(empty)
        window.history.replaceState({}, '', window.location.pathname)
        setSearchResults([])
        setSearchTotal(0)
        loadVacancies()
    }

    // ====== EFFECTS ======

    // Парсим URL при первой загрузке (вакансии/компании/фильтры)
    useEffect(() => {
        const params = new URLSearchParams(window.location.search)
        const vacancyId = params.get('vacancy')
        const companyId = params.get('company')

        const newState = {}
        if (companyId) {
            const id = parseInt(companyId)
            newState.companyId = id
            setCompanyPageId(id)
        }
        if (vacancyId) {
            const id = parseInt(vacancyId)
            newState.vacancyId = id
            setVacancyPageId(id)
        }

        // Если в URL только фильтры поиска (без id) - открываем поиск
        const hasSearchFilters = ['q', 'location', 'experience', 'remote', 'skills']
            .some(key => params.get(key))
        if (hasSearchFilters && !vacancyId && !companyId) {
            setMode('search')
        }

        window.history.replaceState(
            Object.keys(newState).length > 0 ? newState : { base: true },
            '',
            window.location.pathname + window.location.search
        )
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    // Axios interceptor - авто-логаут при 401
    useEffect(() => {
        const interceptor = axios.interceptors.response.use(
            (response) => response,
            (error) => {
                if (error.response?.status === 401) {
                    logout()
                }
                return Promise.reject(error)
            }
        )
        return () => axios.interceptors.response.eject(interceptor)
    }, [logout])

    // Первичная загрузка данных
    useEffect(() => {
        if (authLoading) return

        loadVacancies()
        loadFilterOptions()

        if (user) {
            loadFavorites()
        }

        // Если есть фильтры в URL - применяем их
        const params = new URLSearchParams(window.location.search)
        const hasSearchFilters = ['q', 'location', 'experience', 'remote', 'skills']
            .some(key => params.get(key))
        if (hasSearchFilters) {
            applyFilters(searchFilters)
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [user, authLoading])

    // Закрытие логин-модалки при успешном логине
    useEffect(() => {
        if (user) {
            setShowLoginModal(false)
        }
    }, [user])

    // Загрузка рекомендаций при переходе в соответствующие режимы
    useEffect(() => {
        if ((mode === 'recommendations' || mode === 'swipe') && user && !recommendationsLoaded) {
            loadRecommendations()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [mode, user])

    // Обновление рекомендаций при изменении резюме
    useEffect(() => {
        if (user && recommendationsLoaded && resume) {
            setRecommendationsLoaded(false)
            if (mode === 'recommendations') {
                loadRecommendations(true)
            }
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [resume])

    // Popstate - обработка кнопки "Назад" в браузере
    useEffect(() => {
        const handlePopState = (event) => {
            const state = event.state
            if (!state || state.base) {
                setVacancyPageId(null)
                setCompanyPageId(null)
            } else {
                setVacancyPageId(state.vacancyId || null)
                setCompanyPageId(state.companyId || null)
            }
        }

        window.addEventListener('popstate', handlePopState)
        return () => window.removeEventListener('popstate', handlePopState)
    }, [])

    // Сохраняем режим в sessionStorage при смене
    useEffect(() => {
        sessionStorage.setItem('currentMode', mode)
    }, [mode])

    // ====== СКРОЛЛ: сброс при смене вкладки, восстановление при возврате со страницы ======
    const prevPageRef = useRef({ vacancy: null, company: null })

    useEffect(() => {
        const wasOnPage = !!(prevPageRef.current.vacancy || prevPageRef.current.company)
        const isOnPage = !!(vacancyPageId || companyPageId)

        if (!isOnPage) {
            if (wasOnPage) {
                // Вернулись с вакансии/компании — восстанавливаем позицию
                restoreScrollPosition(mode)
            } else {
                // Переключили вкладку — сбрасываем наверх
                requestAnimationFrame(() => {
                    requestAnimationFrame(() => {
                        const content = document.querySelector('.content')
                        if (content) content.scrollTop = 0
                    })
                })
            }
        }

        prevPageRef.current = { vacancy: vacancyPageId, company: companyPageId }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [mode, vacancyPageId, companyPageId])

    // Синхронизация URL с режимом и фильтрами
    useEffect(() => {
        // Если мы на странице вакансии/компании — не трогаем URL
        if (vacancyPageId || companyPageId) return

        const params = new URLSearchParams(window.location.search)

        if (mode === 'search') {
            // Для режима "search" — восстанавливаем фильтры в URL
            const newParams = new URLSearchParams()
            if (searchFilters.query) newParams.set('q', searchFilters.query)
            if (searchFilters.location) newParams.set('location', searchFilters.location)
            if (searchFilters.experience) newParams.set('experience', searchFilters.experience)
            if (searchFilters.remote === true) newParams.set('remote', 'true')
            if (searchFilters.skills && searchFilters.skills.length > 0) {
                newParams.set('skills', searchFilters.skills.join(','))
            }

            const newUrl = newParams.toString()
                ? `?${newParams.toString()}`
                : window.location.pathname

            // Обновляем только если URL действительно отличается
            const currentUrl = window.location.pathname + window.location.search
            if (newUrl !== currentUrl) {
                window.history.replaceState({}, '', newUrl)
            }
        } else {
            // Для других режимов — удаляем параметры поиска из URL
            const searchKeys = ['q', 'location', 'experience', 'remote', 'skills']
            const hasSearchParams = searchKeys.some(key => params.has(key))

            if (hasSearchParams) {
                searchKeys.forEach(key => params.delete(key))
                const newUrl = params.toString()
                    ? `?${params.toString()}`
                    : window.location.pathname
                window.history.replaceState({}, '', newUrl)
            }
        }
    }, [mode, searchFilters, vacancyPageId, companyPageId])

    // ====== RETURN ======

    return {
        // Auth
        user,
        authLoading,
        logout,
        showLoginModal,
        setShowLoginModal,

        // Вакансии
        vacancies,
        loading,
        selectedVacancy,
        setSelectedVacancy,
        currentIndex,
        loadVacancies,

        // Избранное
        favorites,
        isFavorite,
        addToFavorites,
        removeFromFavorites,
        loadFavorites,

        // Рекомендации
        recommendations,
        selectedRecommendedVacancy,
        setSelectedRecommendedVacancy,
        recommendationsLoading,
        recommendationsLoaded,
        setRecommendationsLoaded,
        loadRecommendations,

        // Резюме
        resume,
        setResume,
        matchScores,

        // Режим
        mode,
        handleModeChange,

        // Навигация
        vacancyPageId,
        companyPageId,
        openVacancyPage,
        closeVacancyPage,
        openCompanyPage,
        closeCompanyPage,

        // Поиск и фильтры
        searchFilters,
        cities,
        allSkills,
        searchResults,
        searchTotal,
        searchLoading,
        updateFilters,
        applyFilters,
        resetFilters,

        // Скролл
        saveScrollPosition,

        // Свайпы
        handleSwipe,
    }
}