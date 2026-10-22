import { useEffect, useId, useLayoutEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import FormSelect from './FormSelect'
import './DatePicker.css'

const pad = value => String(value).padStart(2, '0')
const keyOf = date => `${date.getFullYear()}-${pad(date.getMonth()+1)}-${pad(date.getDate())}`
const parse = value => {
    if(!/^\d{4}-\d{2}-\d{2}$/.test(value || ''))return null
    const date=new Date(`${value}T12:00:00`)
    return !Number.isNaN(date.getTime())&&keyOf(date)===value?date:null
}
const months = Array.from({length:12}, (_,month) => new Date(2026,month,1).toLocaleString('ru-RU',{month:'long'}))

export default function DatePicker({ id, name, value = '', onChange, label = 'Дата', includeTime = false, required = false, min, max, placeholder = 'Выберите дату' }) {
    const generated = useId(), controlId = id || `date-${generated}`
    const trigger = useRef(null), popup = useRef(null)
    const [open,setOpen] = useState(false)
    const [draft,setDraft] = useState('')
    const [time,setTime] = useState('10:00')
    const [month,setMonth] = useState(new Date())
    const [position,setPosition] = useState({top:0,left:0,maxHeight:500})
    const selected = parse(value.slice(0,10))
    const today = keyOf(new Date())
    const available = day => (!min || day >= min.slice(0,10)) && (!max || day <= max.slice(0,10))
    const years = Array.from({length:Math.max(101,new Date().getFullYear()+10-1900+1)},(_,index)=>String(1900+index))
    const currentYear = String(month.getFullYear())
    if (!years.includes(currentYear)) years.push(currentYear)
    const first = new Date(month.getFullYear(),month.getMonth(),1)
    const offset = (first.getDay()+6)%7
    const days = Array.from({length:42},(_,index)=>new Date(month.getFullYear(),month.getMonth(),index-offset+1))

    function close() { setOpen(false); trigger.current?.focus() }
    function show() {
        setDraft(selected ? keyOf(selected) : '')
        setTime(value.slice(11,16) || '10:00')
        setMonth(selected || new Date())
        setOpen(true)
    }
    function choose(day) {
        setDraft(day)
        if (!includeTime) { onChange(day); close() }
    }
    useLayoutEffect(()=>{
        if(!open)return
        function place() {
            const bounds=trigger.current.getBoundingClientRect()
            const height=popup.current?.offsetHeight || 420
            const below=innerHeight-bounds.bottom-12
            const top=below>=Math.min(height,350) ? bounds.bottom+6 : Math.max(8,bounds.top-height-6)
            setPosition({top,left:Math.max(8,Math.min(bounds.left,innerWidth-348)),maxHeight:innerHeight-top-12})
        }
        place()
        window.addEventListener('resize',place);window.addEventListener('scroll',place,true)
        return ()=>{window.removeEventListener('resize',place);window.removeEventListener('scroll',place,true)}
    },[open])
    useEffect(()=>{
        if(!open)return
        function outside(event) { if(!popup.current?.contains(event.target)&&!trigger.current?.contains(event.target))setOpen(false) }
        function key(event) { if(event.key==='Escape'&&!event.defaultPrevented){event.preventDefault();close()} }
        document.addEventListener('pointerdown',outside);document.addEventListener('keydown',key)
        const timer=setTimeout(()=>popup.current?.querySelector('[data-current=true]:not(:disabled),.date-picker-day:not(:disabled)')?.focus(),0)
        return ()=>{clearTimeout(timer);document.removeEventListener('pointerdown',outside);document.removeEventListener('keydown',key)}
    },[open])
    function move(event,date) {
        const delta={ArrowLeft:-1,ArrowRight:1,ArrowUp:-7,ArrowDown:7,Home:-((date.getDay()+6)%7),End:6-((date.getDay()+6)%7)}[event.key]
        if(delta===undefined && !['PageUp','PageDown'].includes(event.key))return
        event.preventDefault()
        const next=new Date(date)
        if(delta!==undefined)next.setDate(next.getDate()+delta)
        else next.setMonth(next.getMonth()+(event.key==='PageUp'?-1:1),1)
        if(!available(keyOf(next)))return
        setMonth(next)
        requestAnimationFrame(()=>popup.current?.querySelector(`[data-day="${keyOf(next)}"]`)?.focus())
    }
    const display=selected ? selected.toLocaleDateString('ru-RU',{day:'numeric',month:'long',year:'numeric'})+(includeTime?`, ${value.slice(11,16)}`:'') : placeholder
    return <div className="date-picker">
        <button type="button" id={controlId} ref={trigger} className={`date-picker-trigger ${selected?'':'is-empty'}`} aria-label={label} aria-haspopup="dialog" aria-expanded={open} aria-controls={open?`${controlId}-dialog`:undefined} aria-required={required} onClick={()=>open?close():show()}><svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="3"/><path d="M7 3v4M17 3v4M3 11h18M8 15h2M14 15h2"/></svg><span>{display}</span></button>
        <input className="date-picker-validation" type="date" name={name} value={value.slice(0,10)} onChange={()=>{}} required={required} min={min?.slice(0,10)} max={max?.slice(0,10)} tabIndex={-1} aria-hidden="true" onInvalid={event=>{event.preventDefault();show()}}/>
        {open && createPortal(<div className="date-picker-popup" ref={popup} id={`${controlId}-dialog`} role="dialog" aria-label={label} style={position}>
            <div className="date-picker-heading"><strong>{label}</strong><button type="button" aria-label="Закрыть календарь" onClick={close}>×</button></div>
            <div className="date-picker-navigation"><button type="button" aria-label="Предыдущий месяц" onClick={()=>setMonth(new Date(month.getFullYear(),month.getMonth()-1,1))}>‹</button><FormSelect label="Месяц" value={String(month.getMonth())} options={months.map((label,index)=>({value:String(index),label}))} onChange={value=>setMonth(new Date(month.getFullYear(),Number(value),1))}/><FormSelect label="Год" value={currentYear} options={years} onChange={year=>setMonth(new Date(Number(year),month.getMonth(),1))}/><button type="button" aria-label="Следующий месяц" onClick={()=>setMonth(new Date(month.getFullYear(),month.getMonth()+1,1))}>›</button></div>
            <div className="date-picker-weekdays" aria-hidden="true">{['Пн','Вт','Ср','Чт','Пт','Сб','Вс'].map(day=><span key={day}>{day}</span>)}</div>
            <div className="date-picker-grid" role="group" aria-label="Дни месяца">{days.map(date=>{const day=keyOf(date);return <button type="button" key={day} className={`date-picker-day ${date.getMonth()!==month.getMonth()?'is-outside':''} ${day===draft?'is-selected':''}`} data-day={day} data-current={day===(draft||today)} aria-label={date.toLocaleDateString('ru-RU',{day:'numeric',month:'long',year:'numeric'})} aria-pressed={day===draft} aria-current={day===today?'date':undefined} disabled={!available(day)} onKeyDown={event=>move(event,date)} onClick={()=>choose(day)}>{date.getDate()}</button>})}</div>
            {includeTime && <div className="date-picker-time"><span>Время</span><FormSelect label="Часы" value={time.slice(0,2)} options={Array.from({length:24},(_,n)=>pad(n))} onChange={hour=>setTime(`${hour}:${time.slice(3,5)}`)}/><span>:</span><FormSelect label="Минуты" value={time.slice(3,5)} options={Array.from({length:60},(_,n)=>pad(n))} onChange={minute=>setTime(`${time.slice(0,2)}:${minute}`)}/></div>}
            <div className="date-picker-footer"><button type="button" onClick={()=>{setMonth(new Date());choose(today)}} disabled={!available(today)}>Сегодня</button>{!required&&<button type="button" onClick={()=>{onChange('');close()}}>Очистить</button>}{includeTime&&<button type="button" className="date-picker-apply" disabled={!draft} onClick={()=>{onChange(`${draft}T${time}`);close()}}>Выбрать</button>}</div>
        </div>,document.body)}
    </div>
}
