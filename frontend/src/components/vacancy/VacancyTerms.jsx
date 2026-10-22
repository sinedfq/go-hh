import { salaryLabel,employmentLabels,scheduleLabels } from '../../utils/vacancyTerms'
export default function VacancyTerms({vacancy}) {
    return <div className="vacancy-terms"><strong>{salaryLabel(vacancy)}</strong><div>{employmentLabels[vacancy.employment]&&<span>{employmentLabels[vacancy.employment]}</span>}{scheduleLabels[vacancy.schedule]&&<span>{scheduleLabels[vacancy.schedule]}</span>}</div></div>
}
export function VacancyTermsFields({value,onChange,step}) {
    const update=(key,newValue)=>onChange({...value,[key]:newValue})
    return <fieldset className="terms-fields"><legend>{step && <span className="editor-legend-step">{step}</span>}Зарплата и условия</legend><p>Оплата за месяц. Если зарплата не раскрывается, оставь оба поля пустыми.</p><div className="form-row-2">
        <label>Зарплата от<input type="number" min="0" max="1000000000" value={value.salary_from||''} onChange={event=>update('salary_from',event.target.value)} /></label>
        <label>Зарплата до<input type="number" min={value.salary_from||0} max="1000000000" value={value.salary_to||''} onChange={event=>update('salary_to',event.target.value)} /></label>
        <label>Валюта<select value={value.currency||'RUB'} onChange={event=>update('currency',event.target.value)}>{['RUB','USD','EUR'].map(currency=><option key={currency}>{currency}</option>)}</select></label>
        <label>Налоги<select value={value.salary_gross?'gross':'net'} onChange={event=>update('salary_gross',event.target.value==='gross')}><option value="net">На руки</option><option value="gross">До вычета налогов</option></select></label>
        <label>Занятость<select value={value.employment||''} onChange={event=>update('employment',event.target.value)}><option value="">Не указана</option>{Object.entries(employmentLabels).map(([key,label])=><option key={key} value={key}>{label}</option>)}</select></label>
        <label>График<select value={value.schedule||''} onChange={event=>update('schedule',event.target.value)}><option value="">Не указан</option>{Object.entries(scheduleLabels).map(([key,label])=><option key={key} value={key}>{label}</option>)}</select></label>
    </div></fieldset>
}
