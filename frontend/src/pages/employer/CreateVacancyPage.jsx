import { useState } from 'react'
import VacancyMapPicker from './VacancyMapPicker'
import MarkdownEditor from '../../components/vacancy/MarkdownEditor'
import VacancyTerms, { VacancyTermsFields } from '../../components/vacancy/VacancyTerms'
import WorkspaceHeader from '../../components/common/WorkspaceHeader'
import FormSelect from '../../components/common/FormSelect'
import { userError } from '../../utils/userError'
import './EmployerPages.css'

const grades = [
    { value:'Junior', label:'Junior', description:'До 1 года опыта' },
    { value:'Middle', label:'Middle', description:'1–3 года опыта' },
    { value:'Senior', label:'Senior', description:'3–6 лет опыта' },
    { value:'Lead', label:'Lead', description:'Более 6 лет опыта' }
]
function EditorSection({ number, title, description, children }) {
    return <section className="editor-section"><div className="editor-section-heading"><span>{number}</span><div><h2>{title}</h2><p>{description}</p></div></div>{children}</section>
}
export default function CreateVacancyPage({ onCreate, onUpdate, onCancel, cities = [], allSkills = [], allPositions = [], editVacancy = null }) {
    const [form,setForm] = useState({ title:editVacancy?.title || '', location:editVacancy?.location || '', experience:editVacancy?.experience || '', remote:editVacancy?.remote || false, skills:editVacancy?.skills || [], description:editVacancy?.description || '', address:editVacancy?.address || '', latitude:editVacancy?.latitude?.toString() || '', longitude:editVacancy?.longitude?.toString() || '' })
    const [terms,setTerms] = useState({ salary_from:editVacancy?.salary_from || '', salary_to:editVacancy?.salary_to || '', currency:editVacancy?.currency || 'RUB', salary_gross:editVacancy?.salary_gross || false, employment:editVacancy?.employment || '', schedule:editVacancy?.schedule || '' })
    const [loading,setLoading] = useState(false)
    const [error,setError] = useState('')
    const update = (key,value) => setForm(previous => ({ ...previous,[key]:value }))
    function addSkill(skill) { if (skill.trim() && !form.skills.includes(skill.trim())) update('skills',[...form.skills,skill.trim()]) }
    async function submit(event) {
        event.preventDefault();setError('')
        if (!form.title.trim()) { setError('Укажите название вакансии');return }
        if (!form.location.trim()) { setError('Укажите город');return }
        if (!form.experience) { setError('Выберите требуемый опыт');return }
        setLoading(true)
        try {
            const data = { ...form, ...terms, title:form.title.trim(), location:form.location.trim(), salary_from:Number(terms.salary_from) || 0, salary_to:Number(terms.salary_to) || 0, latitude:Number(form.latitude) || 0, longitude:Number(form.longitude) || 0 }
            if (editVacancy) await onUpdate(editVacancy.id,data)
            else await onCreate(data)
        } catch (failure) { setError(userError(failure)) }
        finally { setLoading(false) }
    }
    return <div className="employer-page workspace-screen vacancy-editor-screen">
        <WorkspaceHeader eyebrow="КОМАНДА НАЧИНАЕТСЯ ЗДЕСЬ" title={editVacancy ? 'Редактировать вакансию' : 'Новая вакансия'} description="Расскажите о работе, условиях и команде — так подходящим кандидатам будет проще вас найти.">
            {onCancel && <button className="btn btn-secondary" onClick={onCancel}>К вакансиям</button>}
        </WorkspaceHeader>
        <div className="vacancy-editor-layout"><form className="vacancy-form" onSubmit={submit}>
            {error && <div className="employer-error" role="alert">{error}</div>}
            <EditorSection number="01" title="Кого ищете" description="Должность, город и уровень специалиста. Обязательные поля отмечены звёздочкой.">
                <div className="form-field position-input-wrapper"><label htmlFor="vacancy-title">Название вакансии *</label><FormSelect id="vacancy-title" label="Название вакансии" value={form.title} onChange={value=>update('title',value)} options={allPositions} searchable allowCustom freeText required placeholder="Выберите или введите должность"/></div>
                <div className="form-row-2"><div className="form-field"><label htmlFor="vacancy-city">Город *</label><FormSelect id="vacancy-city" inputClassName="city-picker-input" label="Город вакансии" value={form.location} onChange={value=>update('location',value)} options={cities} searchable allowCustom freeText required placeholder="Выберите или введите город"/></div>
                <div className="form-field"><label htmlFor="vacancy-grade">Требуемый опыт *</label><FormSelect id="vacancy-grade" label="Требуемый опыт" value={form.experience} onChange={value=>update('experience',value)} options={grades} placeholder="Выберите уровень"/></div></div>
                <div className="form-field"><label className="remote-toggle-label"><input type="checkbox" checked={form.remote} onChange={event=>update('remote',event.target.checked)}/><span>Удалённая работа</span></label></div>
            </EditorSection>
            <VacancyTermsFields value={terms} onChange={setTerms} step="02"/>
            <EditorSection number="03" title="Задачи и ожидания" description="Добавьте навыки и опишите работу. В описании доступны Markdown и изображения.">
                <div className="form-field"><label htmlFor="vacancy-skill">Требуемые навыки</label><FormSelect id="vacancy-skill" label="Добавить навык" value="" onChange={addSkill} options={allSkills.filter(skill=>!form.skills.includes(skill))} searchable allowCustom placeholder="Выберите или добавьте навык"/>
                    {form.skills.length > 0 && <div className="skills-list">{form.skills.map(skill=><span key={skill} className="skill-tag-form">{skill}<button type="button" className="remove-skill" aria-label={`Удалить навык ${skill}`} onClick={()=>update('skills',form.skills.filter(item=>item!==skill))}>×</button></span>)}</div>}
                </div>
                <div className="form-field"><label>Описание вакансии</label><MarkdownEditor value={form.description} onChange={value=>update('description',value)}/></div>
            </EditorSection>
            <EditorSection number="04" title="Где работает команда" description={form.remote ? 'Адрес необязателен для удалённой работы.' : 'Укажите офис на карте, чтобы кандидатам было проще оценить дорогу.'}>
                <div className="form-field"><VacancyMapPicker address={form.address} latitude={form.latitude} longitude={form.longitude} onAddressChange={value=>update('address',value)} onCoordinatesChange={(latitude,longitude)=>setForm(previous=>({...previous,latitude:latitude.toString(),longitude:longitude.toString()}))}/></div>
            </EditorSection>
            <div className="form-actions">{onCancel && <button type="button" className="btn btn-secondary" onClick={onCancel}>Отмена</button>}<button type="submit" className="btn btn-primary" disabled={loading}>{loading ? 'Сохраняем…' : editVacancy ? 'Сохранить изменения' : 'Создать вакансию'}</button></div>
        </form><aside className="vacancy-editor-preview"><div className="vacancy-editor-preview-card"><span className="eyebrow">ПРЕДПРОСМОТР КАРТОЧКИ</span><h2>{form.title || 'Название вашей вакансии'}</h2><VacancyTerms vacancy={terms}/><div className="workspace-job-meta"><span>{form.location || 'Город'}</span><span>{form.experience || 'Уровень'}</span><span>{form.remote ? 'Удалённо' : 'В офисе'}</span></div>{form.skills.length>0 && <div className="workspace-job-skills">{form.skills.slice(0,5).map(skill=><span key={skill}>{skill}</span>)}</div>}</div><p className="vacancy-editor-preview-note">Перед публикацией проверьте зарплату, город и контакты компании. Описание и фотографии будут доступны на странице вакансии.</p></aside></div>
    </div>
}
