import { useState, useRef } from 'react'
import { userError } from '../../utils/userError'
import './PhotoUpload.css'

export default function PhotoUpload({ currentPhoto, onUpload, label='Загрузить фото', size='medium', fallback }) {
    const [loading,setLoading]=useState(false)
    const [failedPhoto,setFailedPhoto]=useState(null)
    const [error,setError]=useState('')
    const input=useRef(null)
    async function select(event) {
        const file=event.target.files?.[0]
        event.target.value=''
        if(!file||loading)return
        setError('')
        if(file.size>4*1024*1024){setError('Выбери фото размером до 4 МБ.');return}
        if(!['image/jpeg','image/png','image/webp'].includes(file.type)){setError('Поддерживаются JPG, PNG и WebP.');return}
        setLoading(true)
        try {
            const form=new FormData();form.append('photo',file)
            await onUpload(form)
            window.dispatchEvent(new CustomEvent('show-toast',{detail:'Фото обновлено'}))
        } catch(failure){setError(userError(failure))}
        finally{setLoading(false)}
    }
    return <div className="photo-upload-control">
        <button type="button" className={`photo-upload photo-upload-${size}`} onClick={()=>input.current?.click()} disabled={loading} aria-label={label} aria-busy={loading} title={label}>
            {currentPhoto&&failedPhoto!==currentPhoto ? <img src={currentPhoto} alt="" className="photo-preview" onError={()=>setFailedPhoto(currentPhoto)} /> : <span className="photo-fallback">{(fallback||'?').charAt(0).toUpperCase()}</span>}
            <span className="photo-hover-overlay" aria-hidden="true">＋</span>
            {loading&&<span className="photo-loading-overlay">…</span>}
        </button>
        <input type="file" ref={input} onChange={select} accept="image/jpeg,image/png,image/webp" hidden disabled={loading} aria-label={label} />
        {error&&<p className="photo-upload-error" role="alert">{error}</p>}
    </div>
}
