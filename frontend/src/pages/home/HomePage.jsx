import { BrandLogo } from '../../contexts/BrandContext'
import ITAccreditationBadge from '../../components/common/ITAccreditationBadge'
import { useEffect, useState } from 'react'
import axios from 'axios'
import { userError } from '../../utils/userError'
import './HomePage.css'
import SocialFeed from '../../components/social/SocialFeed'
import VacancyTerms from '../../components/vacancy/VacancyTerms'

export default function HomePage({ user, vacancies, onNavigate, onVacancy, onChat, onLogin }) {
    const [data,setData]=useState({})
    const [errors,setErrors]=useState({})
    const [retry,setRetry]=useState(0)
    const employer=user?.role==='employer'||user?.role==='admin'
    useEffect(()=>{
        const controller=new AbortController()
        setData({});setErrors({})
        if(!user)return()=>controller.abort()
        const endpoints=employer?{company:'/api/my-vacancies',applications:'/api/employer/applications',chats:'/api/conversations'}:{resume:'/api/my-resume',views:'/api/my-resume/stats',applications:'/api/my-applications',chats:'/api/conversations',recommendations:'/api/recommendations'}
        Object.entries(endpoints).forEach(([key,url])=>axios.get(url,{signal:controller.signal}).then(response=>setData(previous=>({...previous,[key]:response.data}))).catch(error=>{
            if(controller.signal.aborted)return
            if(error.response?.status===404&&['resume','views','recommendations'].includes(key))setData(previous=>({...previous,[key]:null}))
            else setErrors(previous=>({...previous,[key]:userError(error)}))
        }))
        return()=>controller.abort()
    },[user,employer,retry])
    const apps=Array.isArray(data.applications)?data.applications:[]
    const unread=Array.isArray(data.chats)?data.chats.reduce((sum,chat)=>sum+(chat.unread_count||0),0):null
    const resume=data.resume
    const completion=resume?Math.round([resume.full_name,resume.desired_position,resume.city,resume.experience,resume.skills?.length,resume.about||resume.about_me,resume.phone||resume.telegram,resume.photo_url].filter(Boolean).length/8*100):0
    const recommended=data.recommendations?.recommendations?.map(item=>item.vacancy).filter(Boolean)||[]
    const jobs=employer?(data.company?.vacancies||[]):recommended.length?recommended:vacancies.slice(0,4)
    return <div className="home-page">
        <section className="home-hero">
            <div><span className="eyebrow">ТВОЙ СЛЕДУЮЩИЙ ШАГ</span><h1>{employer?'Собери свою команду':user?`С возвращением${resume?.full_name?', '+resume.full_name.split(' ')[0]:''}`:'Работа, с которой хочется начать день'}</h1><p>{employer?'Вакансии, кандидаты и общение — всё под рукой.':'Подходящие возможности, история откликов и новые знакомства — в одном месте.'}</p>
                <div className="home-actions"><button className="btn btn-primary" onClick={()=>onNavigate(employer?'create-vacancy':'search')}>{employer?'Создать вакансию':'Найти работу'} →</button><button className="btn btn-secondary" onClick={()=>user?onNavigate(employer?'my-company':'profile'):onLogin()}>{employer?'Моя компания':user?'Мой профиль':'Создать профиль'}</button></div>
            </div><div className="home-brand" aria-hidden="true"><BrandLogo alt=""/><span>Люди. Работа. Возможности.</span></div>
        </section>
        {Object.keys(errors).length>0&&<div className="profile-status" role="status">Часть данных не загрузилась. <button className="auth-switch" onClick={()=>setRetry(value=>value+1)}>Повторить</button></div>}
        {user&&<div className="home-stats">
            <button onClick={()=>onNavigate(employer?'my-vacancies':'my-applications')}><strong>{employer?(data.company?.vacancies?.length??'—'):(data.applications?apps.length:'—')}</strong><span>{employer?'Мои вакансии':'Мои отклики'}</span></button>
            <button onClick={()=>onNavigate(employer?'applications':'profile')}><strong>{employer?(data.applications?apps.filter(app=>app.status==='new').length:'—'):(data.views?.total_views??'—')}</strong><span>{employer?'Новые кандидаты':'Просмотры резюме'}</span></button>
            <button onClick={onChat}><strong>{unread??'—'}</strong><span>Непрочитанные сообщения</span></button>
        </div>}
        <SocialFeed/>
        {user&&!employer&&data.resume!==undefined&&!errors.resume&&<section className="home-resume"><div><span className="eyebrow">МОЁ РЕЗЮМЕ</span><h2>{resume?resume.desired_position||'Расскажи, какую работу ищешь':'Пусть работодатели узнают о тебе'}</h2><p>{completion===100?'Все основные разделы заполнены. Обновляй резюме по мере появления нового опыта.':`Заполнено ${completion}%. Добавь опыт, навыки и контакты, чтобы твой профиль был полезнее работодателям.`}</p></div><button className="btn btn-secondary" onClick={()=>onNavigate('profile')}>{resume?'Дополнить резюме':'Создать резюме'}</button><progress max="100" value={completion} aria-label="Заполненность резюме"/></section>}
        <div className="home-section-title"><div><span className="eyebrow">{employer?'ТВОИ ПУБЛИКАЦИИ':recommended.length?'ПОД ТВОЙ ПРОФИЛЬ':'ВОЗМОЖНОСТИ'}</span><h2>{employer?'Активные вакансии':recommended.length?'Может подойти тебе':'Посмотри свежие вакансии'}</h2></div><button className="auth-switch" onClick={()=>onNavigate(employer?'my-vacancies':recommended.length?'recommendations':'search')}>Смотреть все →</button></div>
        <div className="home-jobs">{jobs.slice(0,4).map(job=><button className="home-job" key={job.id} onClick={()=>onVacancy(job.id)}>{job.company_logo&&<img src={job.company_logo} alt=""/>}<h3>{job.title}</h3><VacancyTerms vacancy={job}/><p className="company-name-with-badge">{job.company} <ITAccreditationBadge accredited={job.company_it_accredited}/></p><span>{job.location} · {job.experience}</span><div>{job.skills?.slice(0,3).map(skill=><small key={skill}>{skill}</small>)}</div></button>)}</div>
        {!jobs.length&&<div className="profile-status">{employer?'Создай первую вакансию, чтобы начать получать отклики.':'Список вакансий пока пуст. Попробуй вернуться немного позже.'}</div>}
        {user&&apps.length>0&&<section className="home-recent"><div className="home-section-title"><h2>{employer?'Последние отклики':'Последние шаги'}</h2><button className="auth-switch" onClick={()=>onNavigate(employer?'applications':'my-applications')}>Все отклики →</button></div>{apps.slice(0,3).map(app=><button key={app.id} onClick={()=>onNavigate(employer?'applications':'my-applications')}><span><strong>{app.vacancy_title}</strong><small>{employer?app.resume_full_name||app.candidate_email:app.company_name}</small></span><span>{({new:'На рассмотрении',viewed:'Просмотрено',accepted:'Приглашение',rejected:'Отказ'})[app.status]||app.status}</span></button>)}</section>}
    </div>
}
