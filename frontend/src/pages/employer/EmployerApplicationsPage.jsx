import AppLoader from '../../components/common/AppLoader'
import { useCallback, useEffect, useRef, useState } from 'react'
import axios from 'axios'
import ApplicationHistory from '../../components/common/ApplicationHistory'
import ConfirmModal from '../../components/common/ConfirmModal'
import WorkspaceHeader, { WorkspaceIcon, WorkspaceMetric } from '../../components/common/WorkspaceHeader'
import { userError } from '../../utils/userError'
import './EmployerPages.css'

const statuses = { new:'Новый отклик', viewed:'В работе', accepted:'Приглашение', rejected:'Отказ' }
export default function EmployerApplicationsPage({ onOpenVacancy, onOpenResume, onOpenChatWith, showToast }) {
    const [applications,setApplications] = useState([])
    const [loading,setLoading] = useState(true)
    const [filter,setFilter] = useState('all')
    const [query,setQuery] = useState('')
    const [updating,setUpdating] = useState(null)
    const [error,setError] = useState('')
    const [confirmation,setConfirmation] = useState(null)
    const knownIds = useRef(null)
    const request = useRef(null)
    const toast = useRef(showToast)
    useEffect(()=>{toast.current=showToast},[showToast])
    const load = useCallback(async (silent = false) => {
        request.current?.abort()
        const controller = new AbortController();request.current=controller
        if (!silent) setLoading(true)
        try {
            const response = await axios.get('/api/employer/applications',{signal:controller.signal})
            if (controller.signal.aborted) return
            const items = Array.isArray(response.data) ? response.data : []
            if (knownIds.current) {
                const fresh = items.filter(item=>!knownIds.current.has(item.id))
                if (fresh.length) toast.current?.(fresh.length===1 ? `Новый отклик от ${fresh[0].resume_full_name || fresh[0].candidate_email}` : `Новых откликов: ${fresh.length}`)
            }
            knownIds.current = new Set(items.map(item=>item.id))
            setApplications(items);setError('')
        } catch (failure) { if (!controller.signal.aborted) setError(userError(failure)) }
        finally { if (!controller.signal.aborted) setLoading(false) }
    },[])
    useEffect(()=>{
        load()
        const refresh=()=>{if(!document.hidden)load(true)}
        const interval=setInterval(refresh,10000)
        document.addEventListener('visibilitychange',refresh)
        return ()=>{clearInterval(interval);document.removeEventListener('visibilitychange',refresh);request.current?.abort()}
    },[load])
    async function changeStatus(app,status) {
        if(updating) return
        setUpdating(app.id);setError('')
        try {
            await axios.patch(`/api/applications/${app.id}/status`,{status})
            await load(true)
            showToast?.(status==='accepted' ? 'Приглашение отправлено кандидату' : 'Ответ отправлен кандидату')
        } catch(failure) { setError(userError(failure)) }
        finally {setUpdating(null);setConfirmation(null)}
    }
    async function openChat(app) {
        if(updating) return
        setUpdating(app.id)
        try {
            const id = app.conversation_id || (await axios.post(`/api/applications/${app.id}/start-chat`)).data.conversation_id
            onOpenChatWith?.({conversation_id:id,application_id:app.id,vacancy_id:app.vacancy_id,vacancy_title:app.vacancy_title,company_name:app.company_name,candidate_name:app.resume_full_name || app.candidate_email,candidate_id:app.candidate_user_id,candidate_photo:app.candidate_photo})
        } catch(failure) { setError(userError(failure)) }
        finally {setUpdating(null)}
    }
    const stats = Object.fromEntries(['new','viewed','accepted','rejected'].map(status=>[status,applications.filter(app=>app.status===status).length]))
    const filtered = applications.filter(app=>(filter==='all'||app.status===filter)&&`${app.resume_full_name} ${app.candidate_email} ${app.vacancy_title}`.toLocaleLowerCase('ru').includes(query.trim().toLocaleLowerCase('ru')))
    return <div className="employer-page workspace-screen employer-applications-workspace">
        <WorkspaceHeader eyebrow="ЗНАКОМСТВО С КАНДИДАТАМИ" title="Отклики" description="Все ответы на вакансии команды. Изучайте резюме, приглашайте кандидатов и планируйте встречи."/>
        {confirmation && <ConfirmModal title={confirmation.status==='accepted'?'Пригласить кандидата?':'Отправить отказ?'} message={`${confirmation.app.resume_full_name || confirmation.app.candidate_email} получит ответ по вакансии «${confirmation.app.vacancy_title}». Комментарий и время встречи можно добавить в истории отклика.`} confirmText={confirmation.status==='accepted'?'Пригласить':'Отправить отказ'} danger={confirmation.status==='rejected'} onConfirm={()=>changeStatus(confirmation.app,confirmation.status)} onCancel={()=>setConfirmation(null)}/>}
        {error && <div className="employer-error" role="alert">{error}<button className="btn btn-secondary" onClick={()=>load()}>Повторить загрузку</button></div>}
        {loading ? <div className="workspace-state" role="status"><AppLoader inline/><p>Загружаем отклики…</p></div> : <>
            <div className="workspace-metrics"><WorkspaceMetric icon="briefcase" value={applications.length} label="Всего откликов" hint="На вакансии вашей компании"/><WorkspaceMetric icon="eye" value={stats.new} label="Новые кандидаты" hint="Ожидают первого ответа"/><WorkspaceMetric icon="check" value={stats.accepted} label="Приглашения" hint="Следующий шаг — знакомство"/></div>
            <div className="workspace-toolbar"><label className="workspace-search"><WorkspaceIcon name="search" size={18}/><input aria-label="Поиск кандидатов и вакансий" placeholder="Кандидат или вакансия" value={query} onChange={event=>setQuery(event.target.value)}/></label></div>
            <div className="workspace-tabs applications-tabs" aria-label="Этапы подбора">{[['all','Все',applications.length],['new','Новые',stats.new],['viewed','В работе',stats.viewed],['accepted','Приглашения',stats.accepted],['rejected','Отказы',stats.rejected]].map(([value,label,count])=><button key={value} aria-pressed={filter===value} onClick={()=>setFilter(value)}>{label}<span>{count}</span></button>)}</div>
            {filtered.length===0 ? <div className="workspace-state"><span className="workspace-state-icon"><WorkspaceIcon name="search" size={28}/></span><h2>{applications.length?'Подходящих откликов пока нет':'Здесь появятся будущие коллеги'}</h2><p>{applications.length?'Измените поиск или выберите другой этап.':'Когда кандидаты откликнутся на ваши вакансии, вы сможете познакомиться с ними здесь.'}</p>{(query||filter!=='all')&&<button className="btn btn-secondary" onClick={()=>{setQuery('');setFilter('all')}}>Показать все отклики</button>}</div>
            : <div className="recruitment-list">{filtered.map(app=><article key={app.id} className="recruitment-card">
                <div className="recruitment-card-header"><div className="recruitment-candidate"><div className="recruitment-avatar">{app.candidate_photo?<img src={app.candidate_photo} alt=""/>:(app.resume_full_name || app.candidate_email || '?')[0].toUpperCase()}</div><div><h2>{app.resume_full_name || app.candidate_email}</h2>{app.resume_position && <p>{app.resume_position}</p>}<span>{[app.resume_experience,app.candidate_email].filter(Boolean).join(' · ')}</span></div></div><span className={`workspace-status ${app.status==='accepted'?'success':app.status==='rejected'?'declined':app.status==='new'?'muted':''}`}>{statuses[app.status] || app.status}</span></div>
                <button className="recruitment-vacancy" onClick={()=>onOpenVacancy?.(app.vacancy_id)}><WorkspaceIcon name="briefcase" size={17}/>{app.vacancy_title}<WorkspaceIcon name="arrow" size={16}/></button>
                {app.cover_letter && <details className="recruitment-cover"><summary>Сопроводительное письмо</summary><p>{app.cover_letter}</p></details>}
                <ApplicationHistory application={app} employer onUpdated={()=>load(true)}/>
                <div className="recruitment-card-footer"><span>Отклик {app.created_at?new Date(app.created_at).toLocaleDateString('ru-RU'):'получен'}</span><div className="workspace-card-actions">{app.resume_id>0&&<button className="btn btn-secondary" onClick={()=>onOpenResume?.(app.resume_id)}>Резюме</button>}<button className="btn btn-primary" disabled={!!updating} onClick={()=>openChat(app)}>Открыть чат</button>{app.status!=='accepted'&&<button className="btn btn-secondary" disabled={!!updating} onClick={()=>setConfirmation({app,status:'accepted'})}>Пригласить</button>}{app.status!=='rejected'&&<button className="workspace-delete" disabled={!!updating} onClick={()=>setConfirmation({app,status:'rejected'})}>Отказать</button>}</div></div>
            </article>)}</div>}
        </>}
    </div>
}
