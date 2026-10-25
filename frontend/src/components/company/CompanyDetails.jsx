import { useEffect, useState } from 'react'
import axios from 'axios'
import MediaGallery from './MediaGallery'
import CompanyFollow from '../social/CompanyFollow'
import SocialFeed from '../social/SocialFeed'
import ITAccreditationBadge from '../common/ITAccreditationBadge'
import VacancyTerms from '../vacancy/VacancyTerms'
import { WorkspaceMetric } from '../common/WorkspaceHeader'
import { userError } from '../../utils/userError'
import './CompanyDetails.css'

function websiteUrl(value) {
    if (!value) return null
    try {
        const url = new URL(/^https?:\/\//i.test(value) ? value : `https://${value}`)
        return ['http:', 'https:'].includes(url.protocol) ? url.href : null
    } catch { return null }
}

export default function CompanyDetails({ companyId, onBack, onSelectVacancy }) {
    const [company, setCompany] = useState(null)
    const [vacancies, setVacancies] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')
    const [retry, setRetry] = useState(0)
    const [logoFailed, setLogoFailed] = useState(false)

    useEffect(() => {
        const controller = new AbortController()
        setLoading(true)
        setCompany(null)
        setError('')
        setLogoFailed(false)
        Promise.all([
            axios.get(`/api/companies/${companyId}`, { signal: controller.signal }),
            axios.get(`/api/companies/${companyId}/vacancies`, { signal: controller.signal })
        ]).then(([details, jobs]) => {
            setCompany(details.data)
            setVacancies(Array.isArray(jobs.data) ? jobs.data : [])
        }).catch(err => {
            if (!controller.signal.aborted) setError(userError(err))
        }).finally(() => {
            if (!controller.signal.aborted) setLoading(false)
        })
        return () => controller.abort()
    }, [companyId, retry])

    const website = websiteUrl(company?.website)
    return <article className="company-profile" aria-busy={loading}>
        <button type="button" className="company-profile-back" onClick={onBack}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true"><path d="m15 18-6-6 6-6"/></svg>
            К компаниям
        </button>
        {loading ? <div className="company-profile-panel company-profile-state" role="status"><div className="spinner"/><p>Загружаем компанию…</p></div>
        : error || !company ? <div className="company-profile-panel company-profile-state" role="alert"><h2>Не удалось открыть компанию</h2><p>{error || 'Компания не найдена'}</p><button className="btn btn-primary" onClick={() => setRetry(value => value + 1)}>Попробовать ещё раз</button></div>
        : <>
            <header className="company-profile-panel company-profile-hero">
                <div className="company-profile-identity">
                    <div className="company-profile-logo">{company.logo_url && !logoFailed ? <img src={company.logo_url} alt="" onError={() => setLogoFailed(true)}/> : <span>{company.name?.charAt(0).toUpperCase() || 'К'}</span>}</div>
                    <div className="company-profile-heading"><span className="eyebrow">РАБОТОДАТЕЛЬ</span><h1>{company.name}<ITAccreditationBadge accredited={company.it_accredited}/></h1>
                        <div className="company-profile-tags">{[company.industry, company.size, company.city].filter(Boolean).map((text, index) => <span key={index}>{text}</span>)}</div>
                        {website && <a className="company-profile-website" href={website} target="_blank" rel="noopener noreferrer">Сайт компании <span aria-hidden="true">↗</span></a>}
                    </div>
                </div>
                <CompanyFollow companyId={companyId}/><div className="company-profile-stats"><WorkspaceMetric icon="briefcase" value={vacancies.length} label="Открытые вакансии" hint="Присоединяйтесь к команде"/><WorkspaceMetric icon="eye" value={Number(company.total_views || 0).toLocaleString('ru-RU')} label="Просмотры вакансий" hint="Интерес к предложениям компании"/></div>
                {vacancies.length > 0 && <a className="btn btn-primary company-profile-jobs-link" href="#company-open-jobs">Смотреть вакансии <span aria-hidden="true">↓</span></a>}
            </header>
            <SocialFeed companyId={companyId} composer={false}/>
            <div className="company-profile-layout">
                <div className="company-profile-story">
                    <section className="company-profile-panel"><h2>О компании</h2><p className="company-profile-copy">{company.description || 'Работодатель пока не добавил описание компании.'}</p></section>
                    {company.team_story && <section className="company-profile-panel"><h2>Наша команда</h2><p className="company-profile-copy">{company.team_story}</p></section>}
                    {(company.photos?.length > 0||company.videos?.length>0) && <section className="company-profile-panel"><h2>Жизнь команды</h2><MediaGallery className="company-profile-gallery" photos={company.photos} videos={company.videos} title={`Жизнь команды ${company.name}`}/></section>}
                </div>
                <aside className="company-profile-sidebar">
                    {company.benefits?.length > 0 && <section className="company-profile-panel"><h2>Что мы предлагаем</h2><ul className="company-profile-benefits">{company.benefits.map((benefit, index) => <li key={index}>{benefit}</li>)}</ul></section>}
                    {company.hiring_process && <section className="company-profile-panel"><h2>Как проходит найм</h2><p className="company-profile-copy">{company.hiring_process}</p></section>}
                </aside>
            </div>
            <section id="company-open-jobs" className="company-profile-jobs"><div className="company-profile-section-heading"><div><span className="eyebrow">ПРИСОЕДИНЯЙТЕСЬ К КОМАНДЕ</span><h2>Вакансии компании</h2></div><span className="company-profile-count">{vacancies.length}</span></div>
                {vacancies.length === 0 ? <div className="company-profile-panel company-profile-state"><h3>Открытых вакансий пока нет</h3><p>Вернитесь позже — здесь появятся новые предложения.</p></div>
                : <div className="company-profile-job-grid">{vacancies.map(vacancy => <button type="button" key={vacancy.id} className="company-profile-job" onClick={() => onSelectVacancy?.(vacancy.id)}>
                    <h3>{vacancy.title}</h3><VacancyTerms vacancy={vacancy}/>
                    <div className="company-profile-job-meta">{[vacancy.location, vacancy.experience, vacancy.remote ? 'Удалённо' : 'В офисе'].filter(Boolean).map((label, index) => <span key={index}>{label}</span>)}</div>
                    {vacancy.skills?.length > 0 && <div className="company-profile-tags">{vacancy.skills.slice(0, 4).map((skill, index) => <span key={index}>{skill}</span>)}{vacancy.skills.length > 4 && <span>+{vacancy.skills.length - 4}</span>}</div>}
                    <span className="company-profile-job-link">Подробнее о вакансии <span aria-hidden="true">↗</span></span>
                </button>)}</div>}
            </section>
        </>}
    </article>
}
