import { useState, useEffect, useRef } from 'react'
import axios from 'axios'
import { useAuth } from '../contexts/AuthContext'
import { userError } from '../utils/userError'
import { emptySearchFilters, readSearchFilters, searchParams, searchKeys } from '../utils/searchFilters'

export function useApp() {
    const { user, loading: authLoading, logout } = useAuth()

    // ====== ВАКАНСИИ И ИЗБРАННОЕ ======
    const [vacancies, setVacancies] = useState([])
    const [favorites, setFavorites] = useState([])
    const [currentIndex, setCurrentIndex] = useState(-1)
    const [loading, setLoading] = useState(true)

    // ====== РЕДАКТИРОВАНИЕ ВАКАНСИИ ======
    const [editingVacancy, setEditingVacancy] = useState(null)

    const [allPositions, setAllPositions] = useState([])

    const [toast, setToast] = useState(null)
    const toastTimer = useRef(null)
    const searchRequest = useRef(null)

    useEffect(() => () => {
        clearTimeout(toastTimer.current)
        searchRequest.current?.abort()
    }, [])

    const showToast = (message, duration = 3000) => {
        clearTimeout(toastTimer.current)
        setToast(message)
        toastTimer.current = setTimeout(() => setToast(null), duration)
    }

    const [selectedVacancyId, setSelectedVacancyId] = useState(() => {
        const saved = sessionStorage.getItem('selectedVacancyId')
        return saved ? parseInt(saved) : null
    })

    const selectedVacancy = vacancies.find(v => v.id === selectedVacancyId) || vacancies[0] || null

    // ====== РЕЖИМЫ И МОДАЛКИ ======
    const [mode, setMode] = useState(() => {
        return sessionStorage.getItem('currentMode') || 'home'
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
        return readSearchFilters(window.location.search)
    })

    const [cities, setCities] = useState([])
    const [allSkills, setAllSkills] = useState([])

    const [searchResults, setSearchResults] = useState([])
    const [searchTotal, setSearchTotal] = useState(0)
    const [searchLoading, setSearchLoading] = useState(false)

    // ====== ЧАТ ======
    const [showChatApp, setShowChatApp] = useState(false)

    const openChatApp = () => setShowChatApp(true)


    // ====== РАБОТОДАТЕЛЬ (НОВОЕ) ======
    const [myCompany, setMyCompany] = useState(null)
    const [myVacancies, setMyVacancies] = useState([])
    const [employerLoading, setEmployerLoading] = useState(false)
    const [employerError, setEmployerError] = useState(null)

    // ====== СКРОЛЛ ======
    const scrollPositionsRef = useRef({})

    // Добавь к другим useState
    const [activeChat, setActiveChat] = useState(null)

    const [initialChat, setInitialChat] = useState(null)


    // Добавь функции
    const openChatWith = (chatInfo) => {
        setInitialChat(chatInfo)
        setShowChatApp(true)
    }

    // Обнови closeChatApp чтобы сбрасывать initialChat
    const closeChatApp = () => {
        setShowChatApp(false)
        setInitialChat(null)
    }

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
            const positionsRes = await axios.get('/api/positions')
            const positions = Array.isArray(positionsRes.data) ? positionsRes.data : []
            setAllPositions(positions.map(p => p.name).sort())
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

    // ====== РАБОТОДАТЕЛЬ: загрузка данных ======

    const isEmployer = user && (user.role === 'employer' || user.role === 'admin')

    const loadMyCompany = async () => {
        if (!isEmployer) return
        try {
            const res = await axios.get('/api/my-company')
            setMyCompany(res.data)
            setEmployerError(null)
        } catch (err) {
            if (err.response?.status === 404) {
                setMyCompany(null)
                setEmployerError(null)
            } else {
                console.error('Ошибка загрузки компании:', err)
                setEmployerError('Не удалось загрузить данные компании')
            }
        }
    }

    const loadMyVacancies = async () => {
        if (!isEmployer) return
        setEmployerLoading(true)
        try {
            const res = await axios.get('/api/my-vacancies')
            setMyVacancies(res.data.vacancies || [])
            if (res.data.company) {
                setMyCompany(res.data.company)
            }
            setEmployerError(null)
        } catch (err) {
            console.error('Ошибка загрузки своих вакансий:', err)
            setEmployerError('Не удалось загрузить вакансии')
        } finally {
            setEmployerLoading(false)
        }
    }

    const createCompany = async (companyData) => {
        const res = await axios.post('/api/companies', companyData)
        setMyCompany(res.data)
        return res.data
    }

    const uploadCompanyLogo = async (form) => {
        const res = await axios.post('/api/my-company/logo', form)
        setMyCompany(res.data)
        return res.data
    }

    const updateCompany = async (data) => {const response=await axios.put('/api/my-company',data);setMyCompany(response.data);return response.data}
    const uploadCompanyPhoto = async (form) => {const response=await axios.post('/api/my-company/photos',form);setMyCompany(response.data);return response.data}
    const removeCompanyPhoto = async (url) => {const response=await axios.delete('/api/my-company/photos',{data:{url}});setMyCompany(response.data);return response.data}

    const createVacancy = async (vacancyData) => {
        const res = await axios.post('/api/vacancies', vacancyData)
        setMyVacancies(prev => [res.data, ...prev])
        return res.data
    }

    // ====== НОВОЕ: ОБНОВЛЕНИЕ ВАКАНСИИ ======
    const updateVacancy = async (vacancyId, vacancyData) => {
        const res = await axios.put(`/api/vacancies/${vacancyId}`, vacancyData)
        setMyVacancies(prev => prev.map(v => v.id === vacancyId ? { ...v, ...res.data } : v))
        return res.data
    }

    // ====== НОВОЕ: УДАЛЕНИЕ ВАКАНСИИ ======
    const deleteVacancy = async (vacancyId) => {
        await axios.delete(`/api/vacancies/${vacancyId}`)
        setMyVacancies(prev => prev.filter(v => v.id !== vacancyId))
    }

    // ====== НОВОЕ: АРХИВАЦИЯ ВАКАНСИИ ======
    const archiveVacancy = async (vacancyId, archive = true) => {
        await axios.patch(`/api/vacancies/${vacancyId}/archive`, { archive })
        setMyVacancies(prev => prev.map(v => v.id === vacancyId ? { ...v, is_archived: archive } : v))
    }

    const startEditVacancy = (vacancy) => {
        setEditingVacancy(vacancy)
        setMode('edit-vacancy')
    }

    const cancelEditVacancy = () => {
        setEditingVacancy(null)
        setMode('my-vacancies')
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
        window.history.pushState({ vacancyId: vacancyPageId, companyId: id }, '', `?${params.toString()}`)
    }

    const closeCompanyPage = () => {
        setCompanyPageId(null)
        window.history.back()
    }

    const clearSearchFromURL = () => {
        const params = new URLSearchParams(window.location.search)
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
        if (showChatApp) {
            setShowChatApp(false)
        }

        if (vacancyPageId || companyPageId) {
            setVacancyPageId(null)
            setCompanyPageId(null)
            window.history.back()
        }

        clearSearchFromURL()

        // Для кандидатских режимов нужен логин
        if ((newMode === 'saved-searches' || newMode === 'swipe' || newMode === 'favorites' || newMode === 'recommendations' || newMode === 'my-applications') && !user) {
            setShowLoginModal(true)
            return
        }

        // Для режимов работодателя нужен employer/admin
        if ((newMode === 'my-vacancies' || newMode === 'create-vacancy' || newMode === 'edit-vacancy' || newMode === 'my-company' || newMode === 'applications' || newMode === 'stats') && !isEmployer) {
            if (!user) {
                setShowLoginModal(true)
            }
            return
        }

        setMode(newMode)
    }

    // ====== ФИЛЬТРЫ ======

    const updateFilters = (filters) => {
        setSearchFilters(filters)
    }

    const applyFilters = async (filters, goToSearch = false) => {
        searchRequest.current?.abort()
        const request = new AbortController()
        searchRequest.current = request
        setSearchFilters(filters)

        const params = searchParams(filters)

        const newUrl = params.toString() ? `?${params.toString()}` : window.location.pathname
        window.history.replaceState({}, '', newUrl)

        try {
            setSearchLoading(true)
            const res = await axios.get(`/api/vacancies/search?${searchParams(filters, true).toString()}`, { signal: request.signal })
            if (request.signal.aborted) return
            setSearchResults(res.data.vacancies || [])
            setSearchTotal(res.data.total_count || 0)

            if (goToSearch) {
                setMode('search')
            }
        } catch (err) {
            if (!request.signal.aborted) showToast(userError(err))
        } finally {
            if (searchRequest.current === request) setSearchLoading(false)
        }
    }

    const resetFilters = () => {
        const empty = emptySearchFilters()
        applyFilters(empty)
    }

    // ====== EFFECTS ======

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

        const hasSearchFilters = searchKeys
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

    useEffect(() => {
        const interceptor = axios.interceptors.response.use(
            (response) => response,
            (error) => {
                // Игнорируем 401 для "некритичных" запросов (просмотры)
                const url = error.config?.url || ''
                const isViewRequest = url.includes('/view')

                if (error.response?.status === 401 && !isViewRequest) {
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

            // ====== НОВОЕ: если работодатель — загружаем его данные ======
            if (isEmployer) {
                loadMyVacancies()
            }
        }

        const params = new URLSearchParams(window.location.search)
        const hasSearchFilters = searchKeys
            .some(key => params.get(key))
        if (hasSearchFilters) {
            applyFilters(searchFilters)
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [user, authLoading])

    // Если работодатель — при смене режима my-vacancies обновляем данные
    useEffect(() => {
        if (mode === 'my-vacancies' && isEmployer && myVacancies.length === 0 && !employerLoading) {
            loadMyVacancies()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [mode])

    useEffect(() => {
        if (user) {
            setShowLoginModal(false)
        }
    }, [user])

    useEffect(() => {
        if ((mode === 'recommendations' || mode === 'swipe') && user && !recommendationsLoaded) {
            loadRecommendations()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [mode, user])

    useEffect(() => {
        if (user && recommendationsLoaded && resume) {
            setRecommendationsLoaded(false)
            if (mode === 'recommendations') {
                loadRecommendations(true)
            }
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [resume])

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

    useEffect(() => {
        sessionStorage.setItem('currentMode', mode)
    }, [mode])

    useEffect(()=>{
        if(authLoading)return
        if(!user&&!['home','browse','search','companies'].includes(mode))setMode('home')
        if(user?.role==='employer'&&mode==='saved-searches')setMode('home')
    },[authLoading,user,mode])

    const prevPageRef = useRef({ vacancy: null, company: null })

    useEffect(() => {
        const wasOnPage = !!(prevPageRef.current.vacancy || prevPageRef.current.company)
        const isOnPage = !!(vacancyPageId || companyPageId)

        if (!isOnPage) {
            if (wasOnPage) {
                restoreScrollPosition(mode)
            } else {
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

    useEffect(() => {
        if (vacancyPageId || companyPageId) return

        const params = new URLSearchParams(window.location.search)

        if (mode === 'search') {
            const newParams = searchParams(searchFilters)

            const newUrl = newParams.toString()
                ? `?${newParams.toString()}`
                : window.location.pathname

            const currentUrl = window.location.pathname + window.location.search
            if (newUrl !== currentUrl) {
                window.history.replaceState({}, '', newUrl)
            }
        } else {
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
        isEmployer,

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

        // Работодатель (НОВОЕ)
        myCompany,
        myVacancies,
        employerLoading,
        employerError,
        loadMyCompany,
        loadMyVacancies,
        createCompany,
        uploadCompanyLogo,
        updateCompany,uploadCompanyPhoto,removeCompanyPhoto,
        createVacancy,
        allPositions,

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

        // Чат
        showChatApp,
        openChatApp,
        closeChatApp,

        initialChat,
        openChatWith,

        toast,
        showToast,

        updateVacancy,
        deleteVacancy,
        archiveVacancy,
        editingVacancy,
        startEditVacancy,
        cancelEditVacancy,
    }
}
