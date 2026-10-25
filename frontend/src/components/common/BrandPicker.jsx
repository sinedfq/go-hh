import { useEffect, useRef, useState } from 'react'
import { BrandLogo, useBrand } from '../../contexts/BrandContext'
import ModalPortal from './ModalPortal'
import FormSelect from './FormSelect'
import './PageLoader.css'

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
    return <><button type="button" ref={trigger} className="brand-picker-button" aria-label="Иконка приложения" onClick={()=>setOpen(true)}><BrandLogo alt=""/></button>{open&&<ModalPortal><div className="brand-picker-overlay" onClick={event=>{if(event.target===event.currentTarget)setOpen(false)}} onKeyDown={keyboard}><section className="brand-picker-dialog" role="dialog" aria-modal="true" aria-label="Иконка приложения"><div className="brand-picker-heading"><h2>Иконка приложения</h2><button type="button" autoFocus onClick={()=>setOpen(false)} aria-label="Закрыть выбор иконки">×</button></div><BrandLogo className="brand-picker-preview" alt="Выбранная иконка"/><FormSelect id="brand-design" label="Рисунок" value={String(selection.design)} onChange={value=>select({...selection,design:Number(value)})} options={Array.from({length:9},(_,index)=>({value:String(index+1),label:`Вариант ${index+1}`}))}/><FormSelect id="brand-color" label="Фон" value={selection.color} onChange={color=>select({...selection,color})} options={Object.entries({auto:'По теме приложения',black:'Тёмный',white:'Белый',blue:'Голубой',violet:'Фиолетовый',mint:'Мятный',coral:'Коралловый',navy:'Ночной',cream:'Кремовый'}).map(([value,label])=>({value,label}))}/><p>Иконка используется в меню, при входе и во время загрузки. Фон «По теме» меняется вместе со светлым и тёмным оформлением.</p></section></div></ModalPortal>}</>
}
