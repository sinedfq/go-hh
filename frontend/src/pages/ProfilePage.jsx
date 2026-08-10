import { useAuth } from '../contexts/AuthContext'
import './ProfilePage.css'

function ProfilePage() {
  const { user, logout } = useAuth()

  if (!user) {
    return (
      <div className="profile-page">
        <div className="empty-state">
          <h2>Вы не авторизованы</h2>
          <p>Войдите в аккаунт, чтобы увидеть профиль</p>
        </div>
      </div>
    )
  }

  return (
    <div className="profile-page">
      <div className="profile-card">
        <div className="profile-header">
          <div className="avatar">
            {user.email[0].toUpperCase()}
          </div>
          <div className="profile-info">
            <h2>{user.email}</h2>
            <p className="member-since">
              Участник с {new Date(user.created_at).toLocaleDateString('ru-RU')}
            </p>
          </div>
        </div>

        <div className="profile-stats">
          <div className="stat-item">
            <span className="stat-number">0</span>
            <span className="stat-label">Резюме</span>
          </div>
          <div className="stat-item">
            <span className="stat-number">0</span>
            <span className="stat-label">Избранное</span>
          </div>
          <div className="stat-item">
            <span className="stat-number">0</span>
            <span className="stat-label">Откликов</span>
          </div>
        </div>

        <div className="profile-section">
          <h3>Мои резюме</h3>
          <div className="empty-resume">
            <p>У вас пока нет резюме</p>
            <button className="btn btn-primary">Создать резюме</button>
          </div>
        </div>

        <div className="profile-actions">
          <button className="btn btn-secondary">Настройки</button>
          <button className="btn btn-danger" onClick={logout}>Выйти из аккаунта</button>
        </div>
      </div>
    </div>
  )
}

export default ProfilePage