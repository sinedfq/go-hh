import { useEffect, useRef, useState } from 'react'
import axios from 'axios'
import { userError } from '../../utils/userError'
import FormSelect from './FormSelect'
import './ApplicationHistory.css'

const labels={new:'Отклик отправлен',viewed:'Отклик просмотрен',accepted:'Приглашение',rejected:'Отказ'}
const stages=[{value:'viewed',label:'В работе',description:'Резюме изучено, обсуждение продолжается'},{value:'accepted',label:'Приглашение',description:'Кандидат переходит к следующему этапу'},{value:'rejected',label:'Отказ',description:'Подбор по этому отклику завершён'}]
function localDate(value) { if(!value) return ''; const date=new Date(value);return Number.isNaN(date.getTime())?'':new Date(date.getTime()-date.getTimezoneOffset()*60000).toISOString().slice(0,16) }
function dateLabel(value) { if(!value)return 'Дата не указана';const date=new Date(value);return Number.isNaN(date.getTime())?'Дата не указана':date.toLocaleString('ru-RU',{day:'numeric',month:'short',year:'numeric',hour:'2-digit',minute:'2-digit'}) }
export default function ApplicationHistory({application,employer=false,onUpdated}) {
    const [open,setOpen]=useState(false),[events,setEvents]=useState([]),[loading,setLoading]=useState(false),[loaded,setLoaded]=useState(false),[error,setError]=useState(''),[saving,setSaving]=useState(false)
    const [status,setStatus]=useState(application.status==='new'?'viewed':application.status),[feedback,setFeedback]=useState(application.feedback || ''),[interview,setInterview]=useState(localDate(application.interview_at))
    const controller=useRef(null)
    useEffect(()=>()=>controller.current?.abort(),[])
    async function load() {
        controller.current?.abort();const request=new AbortController();controller.current=request
        setLoading(true);setError('')
        try {
            const response=await axios.get(`/api/applications/${application.id}/history`,{signal:request.signal})
            if(request.signal.aborted)return
            const items=Array.isArray(response.data)?response.data:[]
            setEvents(items);setLoaded(true)
            const last=items.at(-1)
            setFeedback(last?.feedback ?? application.feedback ?? '')
            setInterview(localDate(last?last.interview_at:application.interview_at))
            setStatus((last?.status || application.status)==='new'?'viewed':last?.status || application.status)
        } catch(failure) { if(!request.signal.aborted)setError(userError(failure)) }
        finally { if(!request.signal.aborted)setLoading(false) }
    }
    async function save(event) {
        event.preventDefault();setSaving(true);setError('')
        try {
            await axios.patch(`/api/applications/${application.id}/status`,{status,feedback,interview_at:interview?new Date(interview).toISOString():''})
            await load();await onUpdated?.()
            window.dispatchEvent(new CustomEvent('show-toast',{detail:'Этап отклика обновлён'}))
        } catch(failure) { setError(userError(failure)) }
        finally {setSaving(false)}
    }
    return <section className="application-history application-timeline">
        <button type="button" className="application-timeline-toggle" aria-expanded={open} onClick={()=>{if(!open)load();setOpen(!open)}}><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true"><path d="M3 11a9 9 0 1 1 3 8M3 4v7h7M12 7v5l3 2"/></svg>{open?'Скрыть историю':'История отклика'}{employer?' и собеседование':''}<span aria-hidden="true">{open?'−':'+'}</span></button>
        {!open && (application.feedback || application.interview_at) && <div className="application-current-details">{application.feedback&&<p>{application.feedback}</p>}{application.interview_at&&<p className="application-interview">Собеседование: <strong>{dateLabel(application.interview_at)}</strong></p>}</div>}
        {open&&<div className="application-timeline-body">
            {loading?<p role="status">Загружаем историю…</p>:<ol>{events.map(item=><li key={item.id}><span className="application-timeline-dot" aria-hidden="true"/><div><strong>{labels[item.status]||item.status}</strong><time dateTime={item.created_at || undefined}>{dateLabel(item.created_at)}</time>{item.feedback&&<p>{item.feedback}</p>}{item.interview_at&&<p className="application-interview">Собеседование: <strong>{dateLabel(item.interview_at)}</strong></p>}</div></li>)}</ol>}
            {!loading&&loaded&&events.length===0&&<p>Изменений пока нет.</p>}
            {error&&<p role="alert" className="application-timeline-error">{error} <button type="button" className="auth-switch" onClick={load}>Повторить</button></p>}
            {employer&&loaded&&!loading&&<form className="application-stage-form" onSubmit={save}>
                <div className="application-stage-heading"><h3>Следующий шаг</h3><p>Комментарий и время встречи будут видны кандидату на сайте и в приложении.</p></div>
                <div><label htmlFor={`application-${application.id}-stage`}>Этап рассмотрения</label><FormSelect id={`application-${application.id}-stage`} label="Этап рассмотрения" value={status} onChange={setStatus} options={stages}/></div>
                <label>Комментарий кандидату<textarea value={feedback} maxLength={4000} onChange={event=>setFeedback(event.target.value)} rows={3} placeholder="Расскажите о следующем этапе или оставьте обратную связь"/></label>
                <label>Время собеседования<input type="datetime-local" value={interview} onChange={event=>setInterview(event.target.value)}/><small>Время вашего устройства. Кандидат увидит его в своём часовом поясе.</small></label>
                <button className="btn btn-primary" disabled={saving}>{saving?'Сохраняем…':'Сохранить этап и детали'}</button>
            </form>}
        </div>}
    </section>
}
