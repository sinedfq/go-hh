import { useEffect, useId, useLayoutEffect, useMemo, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import './FormSelect.css'

export default function FormSelect({ id, value = '', onChange, options = [], placeholder = 'Выберите значение', label, searchable = false, allowCustom = false, freeText = false, required = false, inputClassName = '', disabled = false, floating = false }) {
    const generatedId = useId()
    const controlId = id || `select-${generatedId}`
    const listId = `${controlId}-options`
    const root = useRef(null)
    const menu = useRef(null)
    const [position,setPosition] = useState({})
    const [open, setOpen] = useState(false)
    const [query, setQuery] = useState('')
    const [active, setActive] = useState(0)
    const normalized = useMemo(() => options.map(option => typeof option === 'string' ? { value: option, label: option } : option), [options])
    const selected = normalized.find(option => option.value === value)
    const visible = normalized.filter(option => !searchable || !query || `${option.label} ${option.description || ''}`.toLocaleLowerCase('ru').includes(query.toLocaleLowerCase('ru'))).slice(0,50)
    const custom = query.trim()
    const items = allowCustom && custom && !normalized.some(option => String(option.value).toLocaleLowerCase('ru') === custom.toLocaleLowerCase('ru'))
        ? [...visible, { value: custom, label: `Использовать «${custom}»`, custom: true }] : visible

    useEffect(() => {
        function outside(event) { if (!root.current?.contains(event.target)&&!menu.current?.contains(event.target)) setOpen(false) }
        function anotherOpened(event) { if (event.detail !== controlId) setOpen(false) }
        document.addEventListener('pointerdown', outside)
        document.addEventListener('form-select-open', anotherOpened)
        return () => { document.removeEventListener('pointerdown', outside); document.removeEventListener('form-select-open', anotherOpened) }
    }, [controlId])
    useEffect(() => { if (open) document.dispatchEvent(new CustomEvent('form-select-open',{detail:controlId})) }, [open,controlId])
    const selection = useRef({items,value})
    selection.current={items,value}
    useEffect(() => { const current=selection.current;setActive(Math.max(0,current.items.findIndex(item=>item.value===current.value))) }, [query, open])
    useEffect(() => {
        if(!open)return
        const option=document.getElementById(`${listId}-${active}`),list=document.getElementById(listId)
        if(option&&list){if(option.offsetTop<list.scrollTop)list.scrollTop=option.offsetTop;else if(option.offsetTop+option.offsetHeight>list.scrollTop+list.clientHeight)list.scrollTop=option.offsetTop+option.offsetHeight-list.clientHeight}
    }, [active,open,listId])
    useLayoutEffect(()=>{
        if(!open||!floating)return
        function place(){const r=root.current.getBoundingClientRect(),below=innerHeight-r.bottom-12;const height=Math.max(60,Math.min(200,below>=140?below:r.top-12));setPosition({left:Math.max(8,Math.min(r.left,innerWidth-r.width-8)),width:r.width,top:below>=140?r.bottom+6:Math.max(8,r.top-height-6),maxHeight:height})}
        place();window.addEventListener('resize',place);window.addEventListener('scroll',place,true)
        return ()=>{window.removeEventListener('resize',place);window.removeEventListener('scroll',place,true)}
    },[open,floating])

    function choose(option) {
        onChange(option.value)
        setOpen(false)
        setQuery('')
    }
    function toggle() { setQuery(searchable && freeText ? (selected?.label || value) : ''); setOpen(!open) }
    function keyboard(event) {
        if (event.key === 'Escape') { setOpen(false); event.preventDefault(); return }
        if (event.key === 'Tab') { setOpen(false); return }
        if (['ArrowDown','ArrowUp'].includes(event.key)) {
            event.preventDefault()
            if (!open) { setQuery(''); setOpen(true); return }
            setActive(index => Math.max(0,Math.min(items.length-1,index + (event.key === 'ArrowDown' ? 1 : -1))))
        } else if ((event.key === 'Enter' || (!searchable && event.key === ' '))) {
            event.preventDefault()
            if (!open) toggle()
            else if (items[active]) choose(items[active])
        }
    }
    const attributes = { id: controlId, role:'combobox', 'aria-label':label, 'aria-expanded':open, 'aria-controls':listId, 'aria-haspopup':'listbox', 'aria-activedescendant':open && items[active] ? `${listId}-${active}` : undefined, disabled, onKeyDown:keyboard }
    const list = <div className="form-select-menu" ref={menu} id={listId} role="listbox" aria-label={label || placeholder}>
        {items.length === 0 && <p className="form-select-empty">Ничего не найдено</p>}
        {items.map((option,index) => <button type="button" role="option" id={`${listId}-${index}`} key={`${option.custom ? 'custom-' : ''}${option.value}`} aria-selected={option.value === value} className={`form-select-option ${index === active ? 'is-highlighted' : ''}`} onMouseDown={event => event.preventDefault()} onPointerMove={() => setActive(index)} onClick={() => choose(option)}><span><strong>{option.label}</strong>{option.description && <small>{option.description}</small>}</span>{option.value === value && <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true"><path d="m5 12 4 4 10-10"/></svg>}</button>)}
    </div>
    return <div className={`form-select ${open ? 'is-open' : ''}`} ref={root}>
        {searchable ? <div className="form-select-trigger">
            <input {...attributes} className={`form-select-input ${inputClassName}`} autoComplete="off" required={required} value={open ? query : selected?.label || value} placeholder={placeholder} onFocus={() => { setQuery(freeText ? (selected?.label || value) : ''); setOpen(true) }} onChange={event => { setQuery(event.target.value); setOpen(true); if (freeText) onChange(event.target.value) }}/>
            <button type="button" className="form-select-arrow" aria-label={`${open ? 'Закрыть' : 'Открыть'} список: ${label || placeholder}`} disabled={disabled} onClick={toggle}><Chevron/></button>
        </div> : <button {...attributes} type="button" className="form-select-trigger" onClick={toggle}><span className={selected ? '' : 'form-select-placeholder'}>{selected?.label || value || placeholder}</span><Chevron/></button>}
        {open && (floating?createPortal(<div className="form-select form-select-floating" data-select-owner={root.current?.closest('[role=dialog]')?.id} style={position}>{list}</div>,document.body):list)}
    </div>
}

function Chevron() { return <svg className="form-select-chevron" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true"><path d="m6 9 6 6 6-6"/></svg> }
