import { useState } from 'react'
import { useAuth } from '../../contexts/AuthContext'
import './LoginPage.css'

function LoginPage({ onClose }) {
  const [step, setStep] = useState('role')      // 'role' или 'form'
  const [isLogin, setIsLogin] = useState(true)
  const [role, setRole] = useState(null)        // 'candidate' или 'employer'
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { login, register, skipAuth } = useAuth()

  const handleRoleSelect = (selectedRole) => {
    setRole(selectedRole)
    setStep('form')
    setError('')
  }

  const handleBack = () => {
    setStep('role')
    setError('')
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      if (isLogin) {
        await login(email, password)
      } else {
        await register(email, password, role)
      }
    } catch (err) {
      const rawMessage = err.response?.data?.error || 'Произошла ошибка'

      // Перевод стандартных ошибок бэкенда
      const errorMap = {
        'invalid credentials': 'Неверный email или пароль',
        'invalid json': 'Некорректные данные',
        'email and password required': 'Заполните email и пароль',
        'password must be at least 6 characters': 'Пароль должен быть минимум 6 символов',
        'user already exists': 'Пользователь с таким email уже существует',
        'role must be \'candidate\' or \'employer\'': 'Некорректная роль',
        'failed to create user': 'Ошибка при создании пользователя',
        'failed to generate token': 'Ошибка авторизации'
      }

      setError(errorMap[rawMessage] || rawMessage)
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
        <button className="login-close" onClick={onClose}>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      )}

      <div className="login-wide">
        {/* ====== ЛЕВАЯ КОЛОНКА: LIQUID GLASS ====== */}
        <div className="login-brand">
          {/* Декоративные blob-ы под стеклом */}
          <div className="glass-blob glass-blob-1"></div>
          <div className="glass-blob glass-blob-2"></div>
          <div className="glass-blob glass-blob-3"></div>

          <div className="brand-content">
            <div className="brand-logo">
              <div className="brand-logo-icon">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                  <circle cx="12" cy="7" r="4" />
                </svg>
              </div>
              <span>GoHH</span>
            </div>

            <h1 className="brand-title">
              Найдите работу<br />мечты с AI
            </h1>
            <p className="brand-subtitle">
              Умный матчинг вакансий, свайпы как в Tinder и персональные рекомендации
            </p>

            <div className="brand-features">
              <div className="brand-feature">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="11" cy="11" r="8" />
                  <line x1="21" y1="21" x2="16.65" y2="16.65" />
                </svg>
                <span>Умный подбор вакансий</span>
              </div>
              <div className="brand-feature">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.29 1.51 4.04 3 5.5l7 7Z" />
                </svg>
                <span>Свайпы как в Tinder</span>
              </div>
              <div className="brand-feature">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M12 2L2 7l10 5 10-5-10-5z" />
                  <path d="M2 17l10 5 10-5" />
                  <path d="M2 12l10 5 10-5" />
                </svg>
                <span>AI-анализ совместимости</span>
              </div>
            </div>
          </div>
        </div>

        {/* ====== ПРАВАЯ КОЛОНКА: ШАГИ ====== */}
        <div className="login-form-side">
          <div className="form-container">
            {/* ====== ШАГ 1: ВЫБОР РОЛИ ====== */}
            {step === 'role' && (
              <div className="step-role">
                <div className="form-header">
                  <h2>Кто вы?</h2>
                  <p>Выберите, как вы будете использовать GoHH</p>
                </div>

                <div className="role-cards">
                  <button
                    className="role-card"
                    onClick={() => handleRoleSelect('candidate')}
                  >
                    <div className="role-card-icon">
                      <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                        <circle cx="12" cy="7" r="4" />
                      </svg>
                    </div>
                    <div className="role-card-content">
                      <h3>Ищу работу</h3>
                      <p>Свайпы, отклики, AI-рекомендации вакансий под ваше резюме</p>
                    </div>
                    <svg className="role-card-arrow" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <polyline points="9 18 15 12 9 6" />
                    </svg>
                  </button>

                  <button
                    className="role-card"
                    onClick={() => handleRoleSelect('employer')}
                  >
                    <div className="role-card-icon">
                      <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                        <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
                        <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
                      </svg>
                    </div>
                    <div className="role-card-content">
                      <h3>Нанимаю</h3>
                      <p>Публикация вакансий, просмотр откликов, управление командой HR</p>
                    </div>
                    <svg className="role-card-arrow" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <polyline points="9 18 15 12 9 6" />
                    </svg>
                  </button>
                </div>

                <div className="form-extra">
                  <div className="login-divider">
                    <span>или</span>
                  </div>
                  <button className="login-skip" onClick={handleSkip}>
                    Посмотреть вакансии без регистрации
                  </button>
                </div>
              </div>
            )}

            {/* ====== ШАГ 2: ФОРМА ====== */}
            {step === 'form' && (
              <div className="step-form">
                <button className="back-btn" onClick={handleBack}>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <polyline points="15 18 9 12 15 6" />
                  </svg>
                  Назад
                </button>

                <div className="form-header">
                  <div className="role-chip-display">
                    <span className="role-chip-icon">
                      {role === 'candidate' ? (
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                          <circle cx="12" cy="7" r="4" />
                        </svg>
                      ) : (
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                          <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
                          <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
                        </svg>
                      )}
                    </span>
                    {role === 'candidate' ? 'Соискатель' : 'Работодатель'}
                  </div>
                  <h2>{isLogin ? 'С возвращением' : 'Создать аккаунт'}</h2>
                  <p>{isLogin ? 'Войдите чтобы продолжить' : 'Заполните данные для регистрации'}</p>
                </div>

                <div className="login-tabs">
                  <button
                    className={`login-tab ${isLogin ? 'active' : ''}`}
                    onClick={() => { setIsLogin(true); setError('') }}
                  >
                    Вход
                  </button>
                  <button
                    className={`login-tab ${!isLogin ? 'active' : ''}`}
                    onClick={() => { setIsLogin(false); setError('') }}
                  >
                    Регистрация
                  </button>
                </div>

                <form onSubmit={handleSubmit} className="login-form">
                  <div className="form-field">
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

                  <div className="form-field">
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
                    <div className="login-error">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <circle cx="12" cy="12" r="10" />
                        <line x1="12" y1="8" x2="12" y2="12" />
                        <line x1="12" y1="16" x2="12.01" y2="16" />
                      </svg>
                      <span>{error}</span>
                    </div>
                  )}

                  <button type="submit" className="login-submit" disabled={loading}>
                    {loading ? (
                      <span className="loading-spinner"></span>
                    ) : (
                      isLogin ? 'Войти' : 'Зарегистрироваться'
                    )}
                  </button>
                </form>

                <div className="login-footer">
                  {isLogin ? 'Нет аккаунта?' : 'Уже есть аккаунт?'}{' '}
                  <button
                    type="button"
                    onClick={() => { setIsLogin(!isLogin); setError('') }}
                  >
                    {isLogin ? 'Зарегистрироваться' : 'Войти'}
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

export default LoginPage