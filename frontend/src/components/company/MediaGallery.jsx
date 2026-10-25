import { useMemo, useState } from 'react'
import MediaViewer from './MediaViewer'
import VideoPreview from './VideoPreview'

export default function MediaGallery({photos=[],videos=[],title='Жизнь команды',onRemove,removing,children,className=''}) {
    const items=useMemo(()=>[...(photos||[]).map(url=>({url,type:'image'})),...(videos||[]).map(url=>({url,type:'video'}))],[photos,videos])
    const [opened,setOpened]=useState(null)
    return <><div className={`media-gallery ${className}`}>{items.map((item,index)=><figure key={item.url}><button type="button" className="media-tile" onClick={()=>setOpened(index)} aria-label={`Открыть ${item.type==='video'?'видео':'фото'} ${index+1}`}>
        {item.type==='video'?<><VideoPreview src={item.url}/><span className="media-tile-play" aria-hidden="true">▶</span><span className="media-tile-label">Видео</span></>:<img src={item.url} alt={`${title}, фото ${index+1}`} loading="lazy"/>}<span className="media-tile-expand" aria-hidden="true">⛶</span>
    </button>{onRemove&&<button type="button" className="btn btn-secondary media-tile-remove" disabled={!!removing} onClick={()=>onRemove(item.url)}>{removing===item.url?'Удаляем…':'Убрать'}</button>}</figure>)}{children}</div>{opened!==null&&items.length>0&&<MediaViewer key={items[opened]?.url||opened} items={items} initial={Math.min(opened,items.length-1)} title={title} onClose={()=>setOpened(null)}/>}</>
}
