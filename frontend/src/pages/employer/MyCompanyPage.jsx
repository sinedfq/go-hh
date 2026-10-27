import AppLoader from '../../components/common/AppLoader'
import { useState } from 'react'
import PhotoUpload from '../../components/common/PhotoUpload'
import ITAccreditationBadge from '../../components/common/ITAccreditationBadge'
import WorkspaceHeader, { WorkspaceIcon, WorkspaceMetric } from '../../components/common/WorkspaceHeader'
import FormSelect from '../../components/common/FormSelect'
import MediaGallery from '../../components/company/MediaGallery'
import { SocialComposer, TimedStories } from '../../components/social/SocialFeed'
import CompanyMediaUpload from '../../components/company/CompanyMediaUpload'
import { userError } from '../../utils/userError'
import './EmployerPages.css'
import './MyCompanyPage.css'

const empty = {name:'',description:'',industry:'',size:'',city:'',website:'',team_story:'',benefits:'',hiring_process:'',it_accredited:false}
const sizes = ['1-10','11-50','51-200','201-500','500+'].map(value=>({value,label:`${value} человек`}))
function Section({step,title,description,children}) { return <section className="editor-section"><div className="editor-section-heading"><span>{step}</span><div><h2>{title}</h2><p>{description}</p></div></div>{children}</section> }
export default function MyCompanyPage({myCompany,onCreateCompany,onUploadLogo,loadingCompany,onUpdateCompany,onUploadPhoto,onRemovePhoto}) {
    const [editing,setEditing]=useState(false)
    const [form,setForm]=useState(empty)
    const [loading,setLoading]=useState(false)
    const [error,setError]=useState('')
    const [removing,setRemoving]=useState(null)
    const company=myCompany?.id ? myCompany : null
    const update=(field,value)=>setForm(previous=>({...previous,[field]:value}))
    function edit() {setForm({...empty,...company,benefits:(company?.benefits||[]).join('\n')});setError('');setEditing(true)}
    async function submit(event) {
        event.preventDefault();setError('')
        if(!form.name.trim()){setError('Укажите название компании');return}
        setLoading(true)
        try {
            const data={...form,name:form.name.trim(),benefits:form.benefits.split('\n').map(value=>value.trim()).filter(Boolean)}
            if(company)await onUpdateCompany(data);else await onCreateCompany(data)
            setEditing(false)
        }catch(failure){setError(userError(failure))}finally{setLoading(false)}
    }
    async function remove(photo) {
        if(removing)return
        setRemoving(photo);setError('')
        try{await onRemovePhoto(photo)}catch(failure){setError(userError(failure))}finally{setRemoving(null)}
    }
    const filled=company?[company.description,company.logo_url,company.team_story,company.benefits?.length,company.hiring_process,company.photos?.length].filter(Boolean).length:0
    if(loadingCompany&&!company)return <div className="workspace-state" role="status"><AppLoader inline/><p>Загружаем компанию…</p></div>
    return <div className="employer-page workspace-screen company-workspace">
        <WorkspaceHeader eyebrow="ЛИЦО ВАШЕЙ КОМАНДЫ" title={editing?'Редактировать компанию':company?'Моя компания':'Создать компанию'} description="Покажите кандидатам, кто вы, как работаете и почему к вам стоит присоединиться."/>
        {error&&<div className="employer-error" role="alert">{error}</div>}
        {company&&!editing ? <>
            <section className="company-info-card company-management-hero">
                <div className="company-avatar-editor"><PhotoUpload currentPhoto={company.logo_url} onUpload={onUploadLogo} label="Изменить аватарку компании" size="large" fallback={company.name}/><div><span className="eyebrow">СТРАНИЦА КОМПАНИИ</span><h2 className="company-name-with-badge">{company.name}<ITAccreditationBadge accredited={company.it_accredited}/></h2><p>{[company.industry,company.city,company.size&&`${company.size} человек`].filter(Boolean).join(' · ')||'Добавьте город и сферу деятельности'}</p><small>Нажмите на логотип, чтобы изменить его · до 4 МБ</small></div></div>
                <button className="btn btn-secondary" onClick={edit}>Редактировать страницу компании<WorkspaceIcon name="arrow" size={16}/></button>
            </section>
            <div className="workspace-metrics"><WorkspaceMetric icon="check" value={`${Math.round(filled/6*100)}%`} label="Оформление страницы" hint="Описание, логотип и история команды"/><WorkspaceMetric icon="eye" value={company.photos?.length||0} label="Фотографии" hint="Покажите офис и людей"/><WorkspaceMetric icon="briefcase" value={company.benefits?.length||0} label="Преимущества" hint="Что вы предлагаете команде"/></div>
            <SocialComposer story/><TimedStories companyId={company.id}/>
            <div className="company-content-grid">
                <section className="company-content-card"><h2>О компании</h2><p className="preserve-text">{company.description||'Расскажите, чем занимается ваша команда и какие задачи решает.'}</p><dl>{[['Сфера',company.industry],['Размер команды',company.size&&`${company.size} человек`],['Город',company.city]].filter(([,value])=>value).map(([label,value])=><div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}</dl>{/^https?:\/\//i.test(company.website||'')&&<a className="company-website-link" href={company.website} target="_blank" rel="noreferrer">Сайт компании<WorkspaceIcon name="arrow" size={16}/></a>}</section>
                <section className="company-content-card"><h2>Наша команда</h2><p className="preserve-text">{company.team_story||'Добавьте историю команды, её ценности и подход к работе.'}</p><h3>Как проходит найм</h3><p className="preserve-text">{company.hiring_process||'Расскажите об этапах знакомства с кандидатами.'}</p></section>
                <section className="company-content-card company-wide-card"><h2>Преимущества работы</h2>{company.benefits?.length?<ul className="company-benefit-list">{company.benefits.map((benefit,index)=><li key={index}><WorkspaceIcon name="check" size={18}/>{benefit}</li>)}</ul>:<p>Укажите условия, обучение и другие преимущества работы у вас.</p>}</section>
                <section className="company-content-card company-wide-card"><div className="company-section-title"><div><h2>Жизнь команды</h2><p>До 6 фото (4 МБ) и 4 видео MP4/WebM (50 МБ)</p></div></div><MediaGallery className="company-gallery" photos={company.photos} videos={company.videos} title={`Жизнь команды ${company.name}`} onRemove={remove} removing={removing}><CompanyMediaUpload onUpload={onUploadPhoto} photos={company.photos?.length||0} videos={company.videos?.length||0}/></MediaGallery></section>
            </div>
        </> : <form className="company-form company-editor-form" onSubmit={submit}>
            <Section step="01" title="Основная информация" description="Название, направление работы и контакты компании.">
                <div className="form-field"><label htmlFor="company-name">Название компании *</label><input id="company-name" value={form.name} onChange={e=>update('name',e.target.value)} placeholder="Название команды или организации" required maxLength={255}/></div>
                <div className="form-field"><label htmlFor="company-description">О компании</label><textarea id="company-description" value={form.description} onChange={e=>update('description',e.target.value)} rows={4} placeholder="Чем занимаетесь и какие продукты создаёте"/></div>
                <div className="form-row-2"><div className="form-field"><label htmlFor="company-industry">Сфера деятельности</label><input id="company-industry" value={form.industry} onChange={e=>update('industry',e.target.value)} placeholder="Например, информационные технологии"/></div><div className="form-field"><label htmlFor="company-size">Размер команды</label><FormSelect id="company-size" label="Размер команды" value={form.size} onChange={value=>update('size',value)} options={[{value:'',label:'Не указан'},...sizes]}/></div></div>
                <div className="form-row-2"><div className="form-field"><label htmlFor="company-city">Город</label><input id="company-city" value={form.city} onChange={e=>update('city',e.target.value)} placeholder="Где находится ваша команда"/></div><div className="form-field"><label htmlFor="company-website">Сайт</label><input id="company-website" type="url" value={form.website} onChange={e=>update('website',e.target.value)} placeholder="https://company.ru"/></div></div>
            </Section>
            <Section step="02" title="Команда и условия" description="Помогите кандидатам представить работу у вас.">
                <div className="form-field"><label htmlFor="company-team">Наша команда</label><textarea id="company-team" maxLength={10000} value={form.team_story} onChange={e=>update('team_story',e.target.value)} rows={4}/></div>
                <div className="form-field"><label htmlFor="company-benefits">Преимущества — по одному на строке</label><textarea id="company-benefits" value={form.benefits} onChange={e=>update('benefits',e.target.value)} rows={4}/></div>
                <div className="form-field"><label htmlFor="company-hiring">Как проходит найм</label><textarea id="company-hiring" maxLength={10000} value={form.hiring_process} onChange={e=>update('hiring_process',e.target.value)} rows={4}/></div>
            </Section>
            <Section step="03" title="Статус компании" description="Информация, которая отображается рядом с названием."><label className="company-accreditation-option"><input type="checkbox" checked={Boolean(form.it_accredited)} onChange={e=>update('it_accredited',e.target.checked)}/><span><strong>IT-аккредитация</strong><small>У компании есть действующая IT-аккредитация. Статус указывается компанией.</small></span><ITAccreditationBadge accredited/></label></Section>
            <div className="form-actions"><button type="submit" className="btn btn-primary" disabled={loading}>{loading?'Сохраняем…':company?'Сохранить изменения':'Создать компанию'}</button>{editing&&company&&<button type="button" className="btn btn-secondary" disabled={loading} onClick={()=>{setEditing(false);setError('')}}>Отмена</button>}</div>
        </form>}
    </div>
}
