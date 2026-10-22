import { useId, useState } from 'react'
import './FilterPanel.css'
import { employmentLabels,scheduleLabels } from '../../utils/vacancyTerms'

const grades = ['Intern', 'Junior', 'Middle', 'Senior', 'Lead']
const years = ['Без опыта', 'До 1 года', '1-3 года', '3-6 лет', '6+ лет']

export default function FilterPanel({ filters, onFiltersChange, onApply, onReset, cities = [], companies = [], skills = [], experiences = [] }) {
    const id = useId()
    const [skill, setSkill] = useState('')
    const selected = (filters.experience || '').split('|').filter(Boolean)
    const update = (key, value) => onFiltersChange({ ...filters, [key]: value })
    const toggleExperience = value => update('experience', (selected.includes(value) ? selected.filter(item => item !== value) : [...selected, value]).join('|'))
    const addSkill = () => {
        const value = skill.trim()
        if (value && !filters.skills.some(item => item.toLowerCase() === value.toLowerCase())) update('skills', [...filters.skills, value])
        setSkill('')
    }
    const suggestions = (key, label, values, placeholder) => <label className="vf-field">
        <span>{label}</span>
        <input value={filters[key] || ''} onChange={event => update(key, event.target.value)} list={`${id}-${key}`} placeholder={placeholder} autoComplete="off" />
        <datalist id={`${id}-${key}`}>{values.map(value => <option key={value} value={value} />)}</datalist>
    </label>
    const chips = (label, values) => <fieldset className="vf-group"><legend>{label}</legend><div className="vf-chips">{values.map(value => <button type="button" key={value} aria-pressed={selected.includes(value)} onClick={() => toggleExperience(value)}>{value}</button>)}</div></fieldset>
    return <form className="vacancy-filters" onSubmit={event => { event.preventDefault(); onApply() }}>
        <div className="vf-heading"><h2>Твоя следующая работа</h2><p>Настрой поиск под себя</p></div>
        <div className="vf-grid">
            <label className="vf-field"><span>Зарплата от, в месяц</span><input type="number" min="0" value={filters.salaryMin||''} onChange={event=>update('salaryMin',Number(event.target.value))}/></label>
            <label className="vf-field"><span>Зарплата до, в месяц</span><input type="number" min={filters.salaryMin||0} value={filters.salaryMax||''} onChange={event=>update('salaryMax',Number(event.target.value))}/></label>
            <label className="vf-field"><span>Валюта зарплаты</span><select value={filters.currency||'RUB'} onChange={event=>update('currency',event.target.value)}>{['RUB','USD','EUR'].map(currency=><option key={currency}>{currency}</option>)}</select></label>
            <label className="vf-check"><input type="checkbox" checked={!!filters.salaryOnly} onChange={event=>update('salaryOnly',event.target.checked)}/>Только с указанной зарплатой</label>
            <p className="vf-hint vf-wide">Суммы за месяц в выбранной валюте. Налоговый формат указан в вакансии.</p>
            <label className="vf-field"><span>Занятость</span><select value={filters.employment||''} onChange={event=>update('employment',event.target.value)}><option value="">Любая</option>{Object.entries(employmentLabels).map(([key,label])=><option key={key} value={key}>{label}</option>)}</select></label>
            <label className="vf-field"><span>График</span><select value={filters.schedule||''} onChange={event=>update('schedule',event.target.value)}><option value="">Любой</option>{Object.entries(scheduleLabels).map(([key,label])=><option key={key} value={key}>{label}</option>)}</select></label>
            {suggestions('location', 'Город', cities, 'Начни вводить город')}
            {suggestions('company', 'Компания', companies, 'Название компании')}
            <fieldset className="vf-group vf-wide"><legend>Формат работы</legend><div className="vf-chips">{[[null, 'Любой'], [true, 'Удалённо'], [false, 'В офисе']].map(([value, label]) => <button type="button" key={label} aria-pressed={filters.remote === value} onClick={() => update('remote', value)}>{label}</button>)}</div></fieldset>
            {chips('Грейд', grades)}
            {chips('Требуемый опыт в годах', [...new Set([...years, ...experiences.filter(value => !grades.some(grade => grade.toLowerCase() === value.toLowerCase()))])])}
            <p className="vf-hint vf-wide">Можно выбрать несколько значений: подойдёт любое из них. Грейд не заменяет стаж в годах.</p>
            <div className="vf-wide">
                <label className="vf-field" htmlFor={`${id}-skill`}>Навыки</label>
                <div className="vf-add-skill"><input id={`${id}-skill`} value={skill} onChange={event => setSkill(event.target.value)} list={`${id}-skills`} placeholder="Например, Kotlin" onKeyDown={event => { if(event.key === 'Enter'){ event.preventDefault(); addSkill() } }} /><button type="button" onClick={addSkill} disabled={!skill.trim()}>Добавить</button></div>
                <datalist id={`${id}-skills`}>{skills.filter(value => !filters.skills.includes(value)).map(value => <option key={value} value={value} />)}</datalist>
                <div className="vf-chips vf-selected">{filters.skills.map(value => <button type="button" key={value} aria-label={`Убрать навык ${value}`} onClick={() => update('skills', filters.skills.filter(item => item !== value))}>{value} ×</button>)}</div>
                <p className="vf-hint">Подойдёт хотя бы один выбранный навык.</p>
            </div>
            <label className="vf-check"><input type="checkbox" checked={!!filters.hasCoordinates} onChange={event => update('hasCoordinates', event.target.checked)} /><span>Только с адресом на карте</span></label>
            <label className="vf-field"><span>Сортировка</span><select value={filters.sort || 'newest'} onChange={event => update('sort', event.target.value)}><option value="newest">Сначала новые</option><option value="oldest">Сначала старые</option><option value="popular">Популярные</option><option value="title">По названию</option></select></label>
        </div>
        <div className="vf-actions"><button type="button" className="btn btn-secondary" onClick={() => { setSkill(''); onReset() }}>Сбросить</button><button type="submit" className="btn btn-primary">Показать вакансии</button></div>
    </form>
}
