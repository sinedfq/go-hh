import { useState } from 'react'
import { useAuth } from '../../contexts/AuthContext'
import './LoginPage.css'

function LoginPage({ onClose }) {
  const [isLogin, setIsLogin] = useState(true)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { login, register, skipAuth } = useAuth()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      if (isLogin) {
        await login(email, password)
      } else {
        await register(email, password)
      }
    } catch (err) {
      const message = err.response?.data?.error || 'Произошла ошибка'
      setError(message)
    } finally {
      setLoading(false)
    }
  }

  const handleSkip = () => {
    skipAuth()
    if (onClose) onClose()
  }

  return (
    <div className="login-overlay">
      {onClose && (
        <button className="close-modal-btn" onClick={onClose}>
          ×
        </button>
      )}

      <div className={`login-container ${isLogin ? 'login-mode' : 'register-mode'}`}>
        {/* Панель брендинга с жидким стеклом */}
        <div className="login-brand">
          <div className="brand-content">
            <h1 className="brand-title">GoHH!</h1>
            <p className="brand-tagline">Платформа поиска работы с AI-матчингом</p>
            <div className="brand-features">
              <div className="feature">
                <div className="feature-icon"></div>
                <span>Умный подбор вакансий</span>
              </div>
              <div className="feature">
                <div className="feature-icon"></div>
                <span>Свайпы как в Tinder</span>
              </div>
              <div className="feature">
                <div className="feature-icon"></div>
                <span>AI-анализ совместимости</span>
              </div>
            </div>
          </div>
        </div>

        {/* Панель формы */}
        <div className="login-form-container">
          <div className="form-wrapper">
            <div className="form-header">
              <h2>{isLogin ? 'Добро пожаловать' : 'Создать аккаунт'}</h2>
              <p>{isLogin ? 'Войдите в свой аккаунт' : 'Начните поиск работы мечты'}</p>
            </div>

            <div className="tab-switcher">
              <button
                className={`tab ${isLogin ? 'active' : ''}`}
                onClick={() => {
                  setIsLogin(true)
                  setError('')
                }}
              >
                Вход
              </button>
              <button
                className={`tab ${!isLogin ? 'active' : ''}`}
                onClick={() => {
                  setIsLogin(false)
                  setError('')
                }}
              >
                Регистрация
              </button>
            </div>

            <form onSubmit={handleSubmit} className="login-form">
              <div className="input-group">
                <label>Email</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="your@email.com"
                  required
                  autoFocus
                />
              </div>

              <div className="input-group">
                <label>Пароль</label>
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Минимум 6 символов"
                  required
                  minLength={6}
                />
              </div>

              {error && (
                <div className="error-message">
                  <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                    <circle cx="8" cy="8" r="8" fill="#dc3545" opacity="0.1"/>
                    <path d="M8 4v4M8 10v1" stroke="#dc3545" strokeWidth="1.5" strokeLinecap="round"/>
                  </svg>
                  <span>{error}</span>
                </div>
              )}

              <button 
                type="submit" 
                className="btn-submit"
                disabled={loading}
              >
                {loading ? (
                  <span className="btn-loading">
                    <span className="spinner-small"></span>
                    Загрузка...
                  </span>
                ) : (
                  isLogin ? 'Войти' : 'Зарегистрироваться'
                )}
              </button>
            </form>

            <div className="divider">
              <span>или</span>
            </div>

            <button className="btn-skip" onClick={handleSkip}>
              Продолжить без регистрации
            </button>

            <div className="form-footer">
              <p>
                {isLogin ? 'Нет аккаунта?' : 'Уже есть аккаунт?'}{' '}
                <button 
                  type="button" 
                  className="link-btn" 
                  onClick={() => {
                    setIsLogin(!isLogin)
                    setError('')
                  }}
                >
                  {isLogin ? 'Зарегистрироваться' : 'Войти'}
                </button>
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default LoginPage