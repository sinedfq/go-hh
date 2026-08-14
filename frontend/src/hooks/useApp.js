import { useState, useEffect } from 'react'
import axios from 'axios'
import { useAuth } from '../contexts/AuthContext'

export function useApp() {
    const { user, loading: authLoading, logout } = useAuth()

    // Вакансии и избранное
    const [vacancies, setVacancies] = useState([])
    const [favorites, setFavorites] = useState([])
    const [currentIndex, setCurrentIndex] = useState(-1)
    const [selectedVacancy, setSelectedVacancy] = useState(null)
    const [loading, setLoading] = useState(true)

    // Режимы и модалки
    const [mode, setMode] = useState('browse')
    const [showLoginModal, setShowLoginModal] = useState(false)

    // Рекомендации
    const [recommendations, setRecommendations] = useState([])
    const [selectedRecommendedVacancy, setSelectedRecommendedVacancy] = useState(null)
    const [recommendationsLoaded, setRecommendationsLoaded] = useState(false)
    const [recommendationsLoading, setRecommendationsLoading] = useState(false)

    // Резюме (для обновления рекомендаций)
    const [resume, setResume] = useState(null)
    const [matchScores, setMatchScores] = useState({})

    // Страницы
    const [vacancyPageId, setVacancyPageId] = useState(null)
    const [companyPageId, setCompanyPageId] = useState(null)

    // ====== DATA LOADING ======

    const loadVacancies = async () => {
        try {
            setLoading(true)
            const res = await axios.get('/api/vacancies')
            let data = res.data
            if (!Array.isArray(data)) {
                data = data.value || []
            }
            setVacancies(data)
            setCurrentIndex(data.length - 1)
            if (data.length > 0 && !selectedVacancy) {
                setSelectedVacancy(data[0])
            }
            setLoading(false)
        } catch (err) {
            console.error('Ошибка загрузки вакансий:', err)
            setLoading(false)
        }
    }

    const loadFavorites = async () => {
        try {
            const res = await axios.get('/api/favorites')
            let data = res.data
            if (!Array.isArray(data)) {
                data = []
            }
            setFavorites(data)
        } catch (err) {
            console.error('Ошибка загрузки избранных:', err)
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

            if (recs.length > 0) {
                setSelectedRecommendedVacancy(recs[0].vacancy)
            } else {
                setSelectedRecommendedVacancy(null)
            }
            setRecommendationsLoaded(true)
        } catch (err) {
            console.error('Ошибка загрузки рекомендаций:', err)
        } finally {
            setRecommendationsLoading(false)
        }
    }

    // ====== FAVORITES ======

    const addToFavorites = async (vacancyId) => {
        if (!user) {
            setShowLoginModal(true)
            return
        }
        try {
            await axios.post('/api/favorites', { vacancy_id: vacancyId })
            loadFavorites()
        } catch (err) {
            console.error('Ошибка добавления в избранное:', err)
        }
    }

    const removeFromFavorites = async (vacancyId) => {
        if (!user) return
        try {
            await axios.delete(`/api/favorites/${vacancyId}`)
            loadFavorites()
        } catch (err) {
            console.error('Ошибка удаления из избранного:', err)
        }
    }

    const isFavorite = (vacancyId) => {
        return favorites.some(fav => fav.id === vacancyId)
    }

    // ====== SWIPE ======

    const handleSwipe = (direction, vacancyId) => {
        if (direction === 'right') {
            addToFavorites(vacancyId)
        }
        setCurrentIndex(prev => {
            const nextIndex = prev - 1
            if (nextIndex >= 0 && vacancies[nextIndex]) {
                setSelectedVacancy(vacancies[nextIndex])
            }
            return nextIndex
        })
    }

    // ====== NAVIGATION ======

    const openVacancyPage = (id) => {
        setCompanyPageId(null)
        setVacancyPageId(id)
        window.history.pushState({ vacancyId: id }, '', `?vacancy=${id}`)
    }

    const closeVacancyPage = () => {
        setVacancyPageId(null)
        window.history.back()
    }

    const openCompanyPage = (id) => {
        setVacancyPageId(null)
        setCompanyPageId(id)
        window.history.pushState({ companyId: id }, '', `?company=${id}`)
    }

    const closeCompanyPage = () => {
        setCompanyPageId(null)
        window.history.back()
    }

    const handleModeChange = (newMode) => {
        if (vacancyPageId || companyPageId) {
            setVacancyPageId(null)
            setCompanyPageId(null)
            window.history.back()
        }

        if ((newMode === 'swipe' || newMode === 'favorites' || newMode === 'recommendations') && !user) {
            setShowLoginModal(true)
            return
        }
        setMode(newMode)
    }

    // ====== EFFECTS ======

    useEffect(() => {
        const params = new URLSearchParams(window.location.search)
        const vacancyId = params.get('vacancy')
        const companyId = params.get('company')

        const newState = {}
        if (companyId) {
            newState.companyId = parseInt(companyId)
            setCompanyPageId(parseInt(companyId))
        }
        if (vacancyId) {
            newState.vacancyId = parseInt(vacancyId)
            setVacancyPageId(parseInt(vacancyId))
        }

        window.history.replaceState(
            Object.keys(newState).length > 0 ? newState : { base: true },
            '',
            window.location.pathname + window.location.search
        )
    }, [])

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

    useEffect(() => {
        if (!authLoading) {
            loadVacancies()
            if (user) {
                loadFavorites()
            }
        }
    }, [user, authLoading])

    useEffect(() => {
        if (user) {
            setShowLoginModal(false)
        }
    }, [user])

    useEffect(() => {
        if ((mode === 'recommendations' || mode === 'swipe') && user && !recommendationsLoaded) {
            loadRecommendations()
        }
    }, [mode, user])

    useEffect(() => {
        if (user && recommendationsLoaded) {
            setRecommendationsLoaded(false)
            if (mode === 'recommendations') {
                loadRecommendations(true)
            }
        }
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

    return {
        user,
        authLoading,
        logout,
        vacancies,
        favorites,
        currentIndex,
        selectedVacancy,
        loading,
        mode,
        showLoginModal,
        setShowLoginModal,
        recommendations,
        selectedRecommendedVacancy,
        setSelectedRecommendedVacancy,
        recommendationsLoading,
        resume,
        setResume,
        matchScores,
        vacancyPageId,
        companyPageId,
        setSelectedVacancy,
        setRecommendationsLoaded,
        loadVacancies,
        loadFavorites,
        loadRecommendations,
        addToFavorites,
        removeFromFavorites,
        isFavorite,
        handleSwipe,
        openVacancyPage,
        closeVacancyPage,
        openCompanyPage,
        closeCompanyPage,
        handleModeChange,
    }
}