import { createContext, useContext, useState, useEffect } from 'react'
import axios from 'axios'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null)
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        const token = localStorage.getItem('token')
        const savedUser = localStorage.getItem('user')

        if (token && savedUser) {
            try {
                const parsedUser = JSON.parse(savedUser)
                setUser(parsedUser)
                axios.defaults.headers.common['Authorization'] = `Bearer ${token}`
            } catch (err) {
                localStorage.removeItem('token')
                localStorage.removeItem('user')
            }
        }

        setLoading(false)
    }, [])

    const login = async (email, password) => {
        const res = await axios.post('/api/auth/login', { email, password })
        const { token, user: userData } = res.data

        localStorage.setItem('token', token)
        localStorage.setItem('user', JSON.stringify(userData))
        axios.defaults.headers.common['Authorization'] = `Bearer ${token}`

        setUser(userData)
        return userData
    }

    // ====== ИСПРАВЛЕНО: role теперь в параметрах ======
    const register = async (email, password, role = 'candidate') => {
        const res = await axios.post('/api/auth/register', { email, password, role })
        const { token, user: userData } = res.data

        localStorage.setItem('token', token)
        localStorage.setItem('user', JSON.stringify(userData))
        axios.defaults.headers.common['Authorization'] = `Bearer ${token}`

        setUser(userData)
        return userData
    }

    const logout = () => {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        delete axios.defaults.headers.common['Authorization']
        setUser(null)
    }

    const updateUserPhoto = (photoUrl) => {
        setUser(prev => {
            if (!prev) return prev
            const updated = { ...prev, photo_url: photoUrl }
            localStorage.setItem('user', JSON.stringify(updated))
            return updated
        })
    }

    // ====== НОВОЕ: обновление роли (для переключения) ======
    const updateUserRole = (role) => {
        setUser(prev => {
            if (!prev) return prev
            const updated = { ...prev, role }
            localStorage.setItem('user', JSON.stringify(updated))
            return updated
        })
    }

    return (
        <AuthContext.Provider value={{
            user,
            loading,
            login,
            register,
            logout,
            updateUserPhoto,
            updateUserRole
        }}>
            {children}
        </AuthContext.Provider>
    )
}

export function useAuth() {
    const context = useContext(AuthContext)
    if (!context) {
        throw new Error('useAuth must be used within AuthProvider')
    }
    return context
}