import { useRef, useState } from 'react'
import axios from 'axios'
import MarkdownContent from '../common/MarkdownContent'
import { userError } from '../../utils/userError'

export default function MarkdownEditor({value,onChange}) {
    const input=useRef(null)
    const [preview,setPreview]=useState(false)
    const [uploading,setUploading]=useState(false)
    const [error,setError]=useState('')
    const upload=async event=>{
        const file=event.target.files?.[0];event.target.value='';if(!file)return
        if(file.size>4*1024*1024){setError('Выбери изображение до 4 МБ.');return}
        if(!['image/png','image/jpeg','image/webp'].includes(file.type)){setError('Поддерживаются JPG, PNG и WebP.');return}
        setUploading(true);setError('')
        try{const form=new FormData();form.append('photo',file);const response=await axios.post('/api/vacancies/images',form);onChange(`${value}\n\n![Фотография компании](${response.data.url})\n`)}catch(failure){setError(userError(failure))}finally{setUploading(false)}
    }
    return <div className="markdown-editor"><div className="markdown-toolbar"><button className="btn btn-secondary" type="button" aria-pressed={!preview} onClick={()=>setPreview(false)}>Текст</button><button className="btn btn-secondary" type="button" aria-pressed={preview} onClick={()=>setPreview(true)}>Предпросмотр</button><button className="btn btn-secondary" type="button" disabled={uploading} onClick={()=>input.current.click()}>{uploading?'Загрузка…':'Добавить картинку'}</button><input ref={input} hidden type="file" accept="image/png,image/jpeg,image/webp" onChange={upload}/></div>{preview?<MarkdownContent>{value||'Здесь появится описание вакансии.'}</MarkdownContent>:<textarea aria-label="Описание вакансии в Markdown" value={value} onChange={event=>onChange(event.target.value)} rows={12} maxLength={50000} disabled={uploading}/>}<small>## Заголовок · **жирный** · *курсив* · - список · [ссылка](https://example.com). Картинки до 4 МБ.</small>{error&&<p role="alert" className="export-error">{error}</p>}</div>
}
