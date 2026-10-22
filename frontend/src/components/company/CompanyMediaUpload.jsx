import { useRef, useState } from 'react'
import { userError } from '../../utils/userError'

export default function CompanyMediaUpload({onUpload,photos=0,videos=0}) {
    const input=useRef(null)
    const [busy,setBusy]=useState(false),[error,setError]=useState('')
    async function upload(event){
        const file=event.target.files?.[0];event.target.value=''
        if(!file||busy)return
        setError('')
        const video=['video/mp4','video/webm'].includes(file.type)
        if(!video&&!['image/jpeg','image/png','image/webp'].includes(file.type)){setError('Выберите JPG, PNG, WebP, MP4 или WebM.');return}
        if(file.size>(video?50:4)*1024*1024){setError(video?'Видео должно быть не больше 50 МБ.':'Фото должно быть не больше 4 МБ.');return}
        if((video&&videos>=4)||(!video&&photos>=6)){setError('Можно добавить до 6 фотографий и 4 видео.');return}
        const data=new FormData();data.append(video?'video':'photo',file)
        setBusy(true)
        try{await onUpload(data);window.dispatchEvent(new CustomEvent('show-toast',{detail:video?'Видео добавлено':'Фото добавлено'}))}catch(failure){setError(userError(failure))}finally{setBusy(false)}
    }
    return <div className="company-media-upload"><input hidden ref={input} type="file" accept="image/jpeg,image/png,image/webp,video/mp4,video/webm" onChange={upload}/><button type="button" className="btn btn-secondary" onClick={()=>input.current?.click()} disabled={busy||photos+videos>=10}>{busy?'Загружаем…':'Добавить фото или видео'}</button>{busy&&<p role="status">Дождитесь завершения загрузки.</p>}{error&&<p role="alert">{error}</p>}</div>
}
