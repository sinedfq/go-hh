import { useEffect, useRef, useState } from 'react'
import { BrandLogo, useBrand } from '../../contexts/BrandContext'
import ModalPortal from './ModalPortal'
import FormSelect from './FormSelect'
import './BrandPicker.css'

export default function BrandPicker(){
    const [open,setOpen]=useState(false)
    const trigger=useRef(null)
    useEffect(()=>{
        if(!open)return
        const element=trigger.current,overflow=document.body.style.overflow
        document.body.style.overflow='hidden'
        return()=>{document.body.style.overflow=overflow;element?.focus()}
    },[open])
    function keyboard(event){
        if(event.defaultPrevented)return
        if(event.key==='Escape'){setOpen(false);return}
        if(event.key==='Tab'){
            const elements=[...event.currentTarget.querySelectorAll('button:not(:disabled),input:not(:disabled),select:not(:disabled)')]
            if(event.shiftKey&&document.activeElement===elements[0]){event.preventDefault();elements.at(-1)?.focus()}
            else if(!event.shiftKey&&document.activeElement===elements.at(-1)){event.preventDefault();elements[0]?.focus()}
        }
    }
    const {selection,select}=useBrand()
    return <><button type="button" ref={trigger} className="brand-picker-button" aria-label="Иконка приложения" onClick={()=>setOpen(true)}><BrandLogo alt=""/></button>{open&&<ModalPortal><div className="brand-picker-overlay" onClick={event=>{if(event.target===event.currentTarget)setOpen(false)}} onKeyDown={keyboard}><section className="brand-picker-dialog" role="dialog" aria-modal="true" aria-label="Иконка приложения"><div className="brand-picker-heading"><h2>Иконка приложения</h2><button type="button" autoFocus onClick={()=>setOpen(false)} aria-label="Закрыть выбор иконки">×</button></div><BrandLogo className="brand-picker-preview" alt="Выбранная иконка"/><FormSelect id="brand-color" label="Фон" value={selection.color} onChange={color=>select({...selection,color})} options={Object.entries({auto:'\u041f\u043e \u0442\u0435\u043c\u0435 \u043f\u0440\u0438\u043b\u043e\u0436\u0435\u043d\u0438\u044f',black:'\u041d\u043e\u0447\u043d\u0430\u044f \u0432\u043e\u043b\u043d\u0430',white:'\u0411\u0435\u043b\u0430\u044f \u0432\u043e\u043b\u043d\u0430'}).map(([value,label])=>({value,label}))}/><p>Иконка используется в меню, при входе и во время загрузки. Фон «По теме» меняется вместе со светлым и тёмным оформлением.</p></section></div></ModalPortal>}</>
}
