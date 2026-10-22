export function salaryLabel(job) {
    const from=Number(job.salary_from)||0,to=Number(job.salary_to)||0
    if(!from&&!to)return 'Зарплата не указана'
    const number=value=>new Intl.NumberFormat('ru-RU').format(value)
    return `${from&&to?`${number(from)} – ${number(to)}`:from?`от ${number(from)}`:`до ${number(to)}`} ${{RUB:'₽',USD:'$',EUR:'€'}[job.currency]||'₽'} / мес. · ${job.salary_gross?'до вычета налогов':'на руки'}`
}
export const employmentLabels={full_time:'Полная занятость',part_time:'Частичная занятость',project:'Проектная работа',internship:'Стажировка'}
export const scheduleLabels={full_day:'Полный день',flexible:'Гибкий график',shift:'Сменный график'}
