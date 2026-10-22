import WorkspaceHeader, { WorkspaceIcon, WorkspaceMetric } from '../../components/common/WorkspaceHeader'
import PhotoUpload from '../../components/common/PhotoUpload'
import './EmployerProfile.css'

export default function EmployerProfile({ user, stats, loading, dataError, onRetry, onUpload, onNavigate, onLogout }) {
    const actions=[['my-vacancies','briefcase','Мои вакансии','Публикации, просмотры и архив'],['applications','chat','Отклики','Кандидаты и запланированные встречи'],['my-company','eye','Моя компания','Логотип, команда и условия работы'],['create-vacancy','plus','Новая вакансия','Расскажите о работе в команде']]
    return <div className="employer-page workspace-screen employer-profile-workspace">
        <WorkspaceHeader eyebrow="ЛИЧНЫЙ КАБИНЕТ" title="Профиль работодателя" description="Ваш аккаунт и рабочее пространство команды."/>
        {dataError&&<div className="employer-error" role="status">Часть данных не загрузилась. <button className="btn btn-secondary" onClick={onRetry}>Попробовать снова</button></div>}
        <section className="employer-account-card profile-header">
            <PhotoUpload currentPhoto={user.photo_url} onUpload={onUpload} label="Загрузить фото профиля" size="large" fallback={user.email}/>
            <div className="employer-account-info"><span className="eyebrow">{user.role==='admin'?'АДМИНИСТРАТОР':'РАБОТОДАТЕЛЬ'}</span><h2>{stats.company?.name || 'Ваш рабочий аккаунт'}</h2><p>{user.email}</p><span>В Trudomix с {new Date(user.created_at).toLocaleDateString('ru-RU')}</span></div>
            <button className="btn btn-secondary" onClick={()=>onNavigate?.('my-company')}>{stats.company?.id?'Страница компании':'Добавить компанию'}<WorkspaceIcon name="arrow" size={16}/></button>
        </section>
        <div className="workspace-metrics" aria-busy={loading}><WorkspaceMetric icon="briefcase" value={stats.active_vacancies??'—'} label="Активные вакансии" hint="Доступны кандидатам"/><WorkspaceMetric icon="chat" value={stats.applications??'—'} label="Всего откликов" hint={`${stats.new_applications??'—'} новых кандидатов`}/><WorkspaceMetric icon="eye" value={stats.views??'—'} label="Просмотры вакансий" hint="Интерес к вашей команде"/></div>
        <section className="employer-profile-shortcuts" aria-label="Управление подбором">{actions.map(([mode,icon,title,description])=><button key={mode} onClick={()=>onNavigate?.(mode)}><span className="workspace-metric-icon"><WorkspaceIcon name={icon}/></span><span><strong>{title}</strong><small>{description}</small></span><WorkspaceIcon name="arrow" size={18}/></button>)}</section>
        <footer className="employer-account-footer"><div><strong>Личный профиль</strong><p>Фото аккаунта отображается собеседникам в чатах. Логотип компании меняется на её странице.</p></div><button className="btn btn-secondary" onClick={onLogout}>Выйти из аккаунта</button></footer>
    </div>
}
