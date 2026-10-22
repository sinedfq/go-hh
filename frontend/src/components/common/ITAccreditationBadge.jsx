import { useState, useRef, useEffect, useLayoutEffect } from 'react'
import { createPortal } from 'react-dom'

export default function ITAccreditationBadge({ accredited }) {
    const [open,setOpen]=useState(false)
    const root=useRef(null)
    const note=useRef(null)
    const [position,setPosition]=useState({left:12,top:12,width:260})
    useLayoutEffect(()=>{if(!open)return;const place=()=>{const rect=root.current?.getBoundingClientRect();if(!rect)return;const width=Math.min(280,innerWidth-24);setPosition({width,left:Math.max(12,Math.min(rect.left-width/2,innerWidth-width-12)),top:Math.max(12,rect.bottom+165>innerHeight?rect.top-160:rect.bottom+10)})};place();window.addEventListener('resize',place);window.addEventListener('scroll',place,true);return()=>{window.removeEventListener('resize',place);window.removeEventListener('scroll',place,true)}},[open])
    useEffect(()=>{if(!open)return;const close=event=>{if(!root.current?.contains(event.target)&&!note.current?.contains(event.target))setOpen(false)};const key=event=>{if(event.key==='Escape'){setOpen(false);root.current?.querySelector('[role=button]')?.focus()}};document.addEventListener('pointerdown',close);document.addEventListener('keydown',key);return()=>{document.removeEventListener('pointerdown',close);document.removeEventListener('keydown',key)}},[open])
    if (!accredited) return null
    return <span className="it-accreditation" ref={root} onClick={event=>event.stopPropagation()}><span role="button" tabIndex={0} className="it-accreditation-badge" aria-label="Об IT-аккредитации" aria-expanded={open} onClick={()=>setOpen(value=>!value)} onKeyDown={event=>{if(event.key==='Enter'||event.key===' '){event.preventDefault();event.stopPropagation();setOpen(value=>!value)}}}>IT</span>{open&&createPortal(<span ref={note} className="it-accreditation-note" role="dialog" aria-label="IT-аккредитация" style={position}><strong>IT-аккредитация</strong><span>Компания имеет статус аккредитованной IT-компании.</span><small>Сведения указаны компанией.</small></span>,document.body)}</span>
}
