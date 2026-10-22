import { salaryLabel,employmentLabels,scheduleLabels } from '../../utils/vacancyTerms'
import FormSelect from '../common/FormSelect'
export default function VacancyTerms({vacancy}) {
    return <div className="vacancy-terms"><strong>{salaryLabel(vacancy)}</strong><div>{employmentLabels[vacancy.employment]&&<span>{employmentLabels[vacancy.employment]}</span>}{scheduleLabels[vacancy.schedule]&&<span>{scheduleLabels[vacancy.schedule]}</span>}</div></div>
}
export function VacancyTermsFields({value,onChange,step}) {
    const update=(key,newValue)=>onChange({...value,[key]:newValue})
    return <fieldset className="terms-fields"><legend>{step && <span className="editor-legend-step">{step}</span>}Зарплата и условия</legend><p>Оплата за месяц. Если зарплата не раскрывается, оставь оба поля пустыми.</p><div className="form-row-2">
        <label>Зарплата от<input type="number" min="0" max="1000000000" value={value.salary_from||''} onChange={event=>update('salary_from',event.target.value)} /></label>
        <label>Зарплата до<input type="number" min={value.salary_from||0} max="1000000000" value={value.salary_to||''} onChange={event=>update('salary_to',event.target.value)} /></label>
        <div><label htmlFor="vacancy-currency">Валюта</label><FormSelect id="vacancy-currency" label="Валюта" value={value.currency||'RUB'} onChange={currency=>update('currency',currency)} options={['RUB','USD','EUR']}/></div>
        <div><label htmlFor="vacancy-taxes">Налоги</label><FormSelect id="vacancy-taxes" label="Налоги" value={value.salary_gross?'gross':'net'} onChange={tax=>update('salary_gross',tax==='gross')} options={[{value:'net',label:'На руки'},{value:'gross',label:'До вычета налогов'}]}/></div>
        <div><label htmlFor="vacancy-employment">Занятость</label><FormSelect id="vacancy-employment" label="Занятость" value={value.employment||''} onChange={employment=>update('employment',employment)} options={[{value:'',label:'Не указана'},...Object.entries(employmentLabels).map(([value,label])=>({value,label}))]}/></div>
        <div><label htmlFor="vacancy-schedule">График</label><FormSelect id="vacancy-schedule" label="График" value={value.schedule||''} onChange={schedule=>update('schedule',schedule)} options={[{value:'',label:'Не указан'},...Object.entries(scheduleLabels).map(([value,label])=>({value,label}))]}/></div>
    </div></fieldset>
}
