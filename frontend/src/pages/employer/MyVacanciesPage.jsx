import AppLoader from '../../components/common/AppLoader'
import { useState } from 'react'
import ConfirmModal from '../../components/common/ConfirmModal'
import WorkspaceHeader, { WorkspaceIcon, WorkspaceMetric } from '../../components/common/WorkspaceHeader'
import VacancyTerms from '../../components/vacancy/VacancyTerms'
import { userError } from '../../utils/userError'
import './EmployerPages.css'

export default function MyVacanciesPage({ myCompany, myVacancies = [], employerLoading, employerError, onOpenVacancy, onCreateVacancy, onCreateCompany, onEditVacancy, onReload, onDeleteVacancy, onArchiveVacancy, showToast }) {
    const [confirmation, setConfirmation] = useState(null)
    const [busy, setBusy] = useState(null)
    const [actionError, setActionError] = useState('')
    const [tab, setTab] = useState('active')
    const [query, setQuery] = useState('')
    const active = myVacancies.filter(job => !job.is_archived)
    const archived = myVacancies.filter(job => job.is_archived)
    const selected = tab === 'active' ? active : archived
    const visible = selected.filter(job => `${job.title} ${job.location}`.toLowerCase().includes(query.trim().toLowerCase()))
    const views = myVacancies.reduce((sum, job) => sum + (Number(job.views) || 0), 0)

    async function changeVacancy(job, action) {
        if (busy) return
        setBusy(job.id)
        setActionError('')
        try {
            if (action === 'delete') await onDeleteVacancy(job.id)
            else await onArchiveVacancy(job.id, action === 'archive')
            showToast?.(action === 'delete' ? 'Вакансия удалена' : action === 'archive' ? 'Вакансия перемещена в архив' : 'Вакансия снова опубликована')
            setConfirmation(null)
        } catch (err) { setActionError(userError(err)) }
        finally { setBusy(null) }
    }

    return <div className="employer-page workspace-screen vacancies-workspace">
        <WorkspaceHeader eyebrow="ПОДБОР КОМАНДЫ" title="Мои вакансии" description={myCompany ? `${myCompany.name} · Управляйте публикациями и находите новых коллег.` : 'Все публикации вашей компании в одном месте.'}>
            {myCompany && <button className="btn btn-primary" onClick={onCreateVacancy}><WorkspaceIcon name="plus" size={18}/>Новая вакансия</button>}
        </WorkspaceHeader>
        {confirmation && <ConfirmModal title={confirmation.action === 'delete' ? 'Удалить вакансию?' : 'Переместить в архив?'} message={confirmation.action === 'delete' ? `Вакансия «${confirmation.job.title}» будет удалена вместе с откликами и чатами. Отменить это действие нельзя.` : `Вакансия «${confirmation.job.title}» исчезнет из поиска. Её можно будет восстановить из архива.`} confirmText={confirmation.action === 'delete' ? 'Удалить' : 'В архив'} danger={confirmation.action === 'delete'} onConfirm={() => changeVacancy(confirmation.job, confirmation.action)} onCancel={() => setConfirmation(null)}/>}
        {(employerError || actionError) && <div className="employer-error" role="alert">{actionError || employerError}{employerError && onReload && <button className="btn btn-secondary" onClick={onReload}>Повторить загрузку</button>}</div>}
        {employerLoading ? <div className="workspace-state" role="status"><AppLoader inline/><p>Загружаем вакансии…</p></div>
        : !myCompany ? <div className="workspace-state"><span className="workspace-state-icon"><WorkspaceIcon name="briefcase" size={28}/></span><h2>Расскажите о компании</h2><p>Создайте страницу работодателя, чтобы публиковать вакансии и получать отклики.</p><button className="btn btn-primary" onClick={onCreateCompany || onCreateVacancy}>Создать компанию</button></div>
        : <>
            <div className="workspace-metrics"><WorkspaceMetric icon="briefcase" value={active.length} label="Опубликовано" hint="Доступны кандидатам в поиске"/><WorkspaceMetric icon="eye" value={views.toLocaleString('ru-RU')} label="Просмотры вакансий" hint="За всё время"/><WorkspaceMetric icon="archive" value={archived.length} label="В архиве" hint="Можно опубликовать снова"/></div>
            <div className="workspace-toolbar"><div className="workspace-tabs" aria-label="Публикации вакансий"><button aria-pressed={tab === 'active'} onClick={() => setTab('active')}>Активные<span>{active.length}</span></button><button aria-pressed={tab === 'archived'} onClick={() => setTab('archived')}>Архив<span>{archived.length}</span></button></div><label className="workspace-search"><WorkspaceIcon name="search" size={18}/><input aria-label="Найти свою вакансию" placeholder="Название или город" value={query} onChange={event => setQuery(event.target.value)}/></label></div>
            {visible.length === 0 ? <div className="workspace-state"><span className="workspace-state-icon"><WorkspaceIcon name={query ? 'search' : tab === 'active' ? 'briefcase' : 'archive'} size={28}/></span><h2>{query ? 'Ничего не найдено' : tab === 'active' ? 'Здесь появятся ваши вакансии' : 'Архив пока пуст'}</h2><p>{query ? 'Попробуйте другое название или город.' : tab === 'active' ? 'Опишите задачи и условия — и начните знакомиться с кандидатами.' : 'Сохраняйте здесь вакансии, по которым подбор уже завершён.'}</p>{query ? <button className="btn btn-secondary" onClick={() => setQuery('')}>Сбросить поиск</button> : tab === 'active' && <button className="btn btn-primary" onClick={onCreateVacancy}>Создать вакансию</button>}</div>
            : <div className="workspace-job-grid">{visible.map(job => <article key={job.id} className="workspace-job-card">
                <div className="workspace-card-top"><span className={`workspace-status ${job.is_archived ? 'muted' : ''}`}>{job.is_archived ? 'В архиве' : 'Опубликована'}</span><span className="workspace-card-views" title="Просмотры"><WorkspaceIcon name="eye" size={16}/>{job.views || 0}</span></div>
                <button className="workspace-job-title" onClick={() => onOpenVacancy(job.id)}>{job.title}</button><VacancyTerms vacancy={job}/>
                <div className="workspace-job-meta">{[job.location, job.experience, job.remote ? 'Удалённо' : 'В офисе'].filter(Boolean).map((text, index) => <span key={index}>{text}</span>)}</div>
                {!!job.skills?.length && <div className="workspace-job-skills">{job.skills.slice(0,4).map((skill, index) => <span key={index}>{skill}</span>)}{job.skills.length > 4 && <span>+{job.skills.length - 4}</span>}</div>}
                <div className="workspace-card-actions">{job.is_archived ? <button className="btn btn-primary" disabled={busy === job.id} onClick={() => changeVacancy(job, 'restore')}>Опубликовать снова</button> : <><button className="btn btn-primary" onClick={() => onEditVacancy?.(job)}>Редактировать</button><button className="btn btn-secondary" disabled={!!busy} onClick={() => setConfirmation({ job, action:'archive' })}>В архив</button></>}<button className="workspace-delete" disabled={!!busy} onClick={() => setConfirmation({ job, action:'delete' })}>Удалить</button></div>
            </article>)}</div>}
        </>}
    </div>
}
