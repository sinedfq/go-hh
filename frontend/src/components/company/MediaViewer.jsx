import { useEffect, useRef, useState } from 'react'
import ModalPortal from '../common/ModalPortal'
import './MediaViewer.css'

const clock = value => { const seconds=Number.isFinite(value)?Math.max(0,Math.floor(value)):0;return `${Math.floor(seconds/60)}:${String(seconds%60).padStart(2,'0')}` }

export function VideoPlayer({src,onFullscreen,storyMode=false,paused=false,onProgress,onFinished}) {
    const video=useRef(null)
    const [playing,setPlaying]=useState(false),[time,setTime]=useState(0),[duration,setDuration]=useState(0),[muted,setMuted]=useState(storyMode),[loading,setLoading]=useState(true),[error,setError]=useState(false),[speed,setSpeed]=useState('1')
    useEffect(()=>{if(!storyMode)return;if(paused)video.current?.pause();else video.current?.play().catch(()=>setLoading(false))},[src,paused,storyMode])
    async function toggle(){if(!video.current)return;if(playing)video.current.pause();else try{await video.current.play()}catch{setError(true);setLoading(false)}}
    function seek(value){if(video.current&&Number.isFinite(video.current.duration))video.current.currentTime=Math.max(0,Math.min(video.current.duration,Number(value)))}
    return <div className="styled-video-player" onKeyDown={event=>{if(['INPUT','SELECT','BUTTON'].includes(event.target.tagName))return;if(event.code==='Space'||event.key==='k'){event.preventDefault();toggle()}else if(event.key==='ArrowRight'){event.preventDefault();seek(time+5)}else if(event.key==='ArrowLeft'){event.preventDefault();seek(time-5)}}} tabIndex={0} aria-label="Видеоплеер">
        <div className="media-video-stage"><video ref={video} src={src} playsInline preload="metadata" muted={muted} onClick={toggle} onLoadedMetadata={event=>setDuration(event.currentTarget.duration)} onCanPlay={()=>setLoading(false)} onWaiting={()=>setLoading(true)} onPlaying={()=>{setPlaying(true);setLoading(false)}} onPause={()=>{setPlaying(false);setLoading(false)}} onEnded={()=>{setPlaying(false);onFinished?.()}} onTimeUpdate={event=>{const element=event.currentTarget;setTime(element.currentTime);if(Number.isFinite(element.duration)&&element.duration>0)onProgress?.(element.currentTime/element.duration)}} onError={()=>{setError(true);setLoading(false)}}/>
            {error?<div className="media-video-error" role="alert"><p>Не удалось воспроизвести видео.</p><button type="button" onClick={()=>{setError(false);setLoading(true);video.current.load()}}>Повторить загрузку</button></div>:loading?<span className="media-video-loading" role="status">Загружаем видео…</span>:!playing&&<button type="button" className="media-video-big-play" aria-label="Воспроизвести видео" onClick={toggle}>▶</button>}
        </div>
        <div className="media-video-controls"><input type="range" className="media-video-seek" aria-label="Перемотка видео" min="0" max={duration||0} step="0.1" value={Math.min(time,duration||0)} onChange={event=>seek(event.target.value)} disabled={!duration||error}/><div className="media-video-control-row"><button type="button" onClick={toggle} aria-label={playing?'Пауза':'Воспроизвести'} disabled={error}>{playing?'Ⅱ':'▶'}</button><span className="media-video-time">{clock(time)} <span>/ {clock(duration)}</span></span><button type="button" onClick={()=>setMuted(!muted)} aria-label={muted?'Включить звук':'Выключить звук'} aria-pressed={muted}>{muted?'Без звука':'Звук'}</button><label className="media-video-speed"><span className="visually-hidden">Скорость воспроизведения</span><select aria-label="Скорость воспроизведения" value={speed} onChange={event=>{setSpeed(event.target.value);video.current.playbackRate=Number(event.target.value)}}>{['0.75','1','1.25','1.5','2'].map(value=><option key={value} value={value}>{value}×</option>)}</select></label><button type="button" onClick={onFullscreen} aria-label="На весь экран">⛶</button></div></div>
    </div>
}

export default function MediaViewer({items,initial=0,title='Медиа компании',storyMode=false,onRemove,onClose}) {
    const [index,setIndex]=useState(Math.min(initial,items.length-1)),[failed,setFailed]=useState(false),[fullscreenError,setFullscreenError]=useState('')
    const viewer=useRef(null),close=useRef(null),touch=useRef(null)
    const [confirmDelete,setConfirmDelete]=useState(false)
    const [progress,setProgress]=useState(0),[paused,setPaused]=useState(false),[visible,setVisible]=useState(!document.hidden)
    const elapsed=useRef(0)
    const item=items[Math.min(index,items.length-1)]
    useEffect(()=>{setIndex(value=>Math.min(value,items.length-1))},[items.length])
    useEffect(()=>{setFailed(false)},[index,item.url])
    useEffect(()=>{elapsed.current=0;setProgress(0);setPaused(false)},[item.url])
    useEffect(()=>{const visibility=()=>setVisible(!document.hidden);document.addEventListener('visibilitychange',visibility);return()=>document.removeEventListener('visibilitychange',visibility)},[])
    function nextStory(){if(index+1<items.length)setIndex(value=>value+1);else onClose()}
    useEffect(()=>{
        if(!storyMode||item.type==='video'||paused||!visible||confirmDelete)return
        let frame,last
        const tick=now=>{if(last!==undefined)elapsed.current+=Math.min(now-last,150);last=now;setProgress(Math.min(1,elapsed.current/5000));if(elapsed.current>=5000){if(index+1<items.length)setIndex(index+1);else onClose()}else frame=requestAnimationFrame(tick)}
        frame=requestAnimationFrame(tick)
        return()=>cancelAnimationFrame(frame)
    },[storyMode,item.url,item.type,index,items.length,paused,visible,confirmDelete,onClose])
    useEffect(()=>{
        const element=viewer.current,previous=document.activeElement,overflow=document.body.style.overflow
        document.body.style.overflow='hidden';close.current?.focus()
        function key(event){
            if(event.key==='Escape'){event.preventDefault();onClose();return}
            if(event.defaultPrevented)return
            if(event.key==='Tab'){const nodes=[...viewer.current.querySelectorAll('button:not(:disabled),input:not(:disabled),select,[tabindex="0"]')];if(event.shiftKey&&document.activeElement===nodes[0]){event.preventDefault();nodes.at(-1)?.focus()}else if(!event.shiftKey&&document.activeElement===nodes.at(-1)){event.preventDefault();nodes[0]?.focus()}return}
            if(event.target.closest('.styled-video-player'))return
            if(event.key==='ArrowRight'){event.preventDefault();setIndex(value=>(value+1)%items.length)}
            if(event.key==='ArrowLeft'){event.preventDefault();setIndex(value=>(value-1+items.length)%items.length)}
        }
        document.addEventListener('keydown',key)
        return ()=>{document.removeEventListener('keydown',key);document.body.style.overflow=overflow;if(document.fullscreenElement===element)document.exitFullscreen?.().catch(()=>{});previous?.focus()}
    },[items.length,onClose])
    async function fullscreen(){try{if(document.fullscreenElement)await document.exitFullscreen();else if(viewer.current.requestFullscreen)await viewer.current.requestFullscreen();else setFullscreenError('Полноэкранный режим недоступен в этом браузере.')}catch{setFullscreenError('Не удалось включить полноэкранный режим.')}}
    return <ModalPortal><div className="media-viewer" ref={viewer} role="dialog" aria-modal="true" aria-label={title} onClick={event=>{if(event.target===viewer.current)onClose()}}>
        {storyMode&&<div className="story-progress-track" aria-label="Прогресс историй">{items.map((entry,position)=><div key={entry.url} role="progressbar" aria-label={`История ${position+1}`} aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round((position<index?1:position===index?progress:0)*100)}><span style={{transform:`scaleX(${position<index?1:position===index?progress:0})`}}/></div>)}<button type="button" onClick={()=>setPaused(value=>!value)} aria-label={paused?'Продолжить историю':'Пауза истории'}>{paused?'▶':'Ⅱ'}</button></div>}
        <header className="media-viewer-header"><div><strong>{title}</strong><span>{index+1} / {items.length} · {item.type==='video'?'Видео':'Фото'}</span>{item.caption&&<p className="media-story-caption">{item.caption}</p>}</div><button type="button" onClick={fullscreen} aria-label="Развернуть просмотр">⛶</button><button type="button" ref={close} onClick={onClose} aria-label="Закрыть просмотр">×</button></header>
        <div className="media-viewer-stage" onTouchStart={event=>{touch.current=event.changedTouches[0].clientX}} onTouchEnd={event=>{if(item.type==='video'||event.target.closest('button'))return;const delta=event.changedTouches[0].clientX-touch.current;if(Math.abs(delta)>50)setIndex(value=>(value+(delta<0?1:-1)+items.length)%items.length)}}>
            {item.type==='video'?<VideoPlayer key={item.url} src={item.url} onFullscreen={fullscreen} storyMode={storyMode} paused={paused||!visible||confirmDelete} onProgress={setProgress} onFinished={storyMode?nextStory:undefined}/>:failed?<div className="media-video-error" role="alert"><p>Не удалось загрузить фото.</p><button type="button" onClick={()=>setFailed(false)}>Повторить</button></div>:<img key={item.url} src={item.url} alt={`${title}, фото ${index+1}`} onError={()=>setFailed(true)}/>}
        </div>
        {onRemove&&item.canDelete&&<div className="media-story-delete">{confirmDelete?<><span>Удалить историю?</span><button type="button" onClick={()=>{setConfirmDelete(false);onRemove(item)}}>Удалить</button><button type="button" onClick={()=>setConfirmDelete(false)}>Отмена</button></>:<button type="button" onClick={()=>setConfirmDelete(true)}>Удалить историю</button>}</div>}
        <footer className="media-viewer-footer"><button type="button" onClick={()=>{setConfirmDelete(false);setIndex(value=>(value-1+items.length)%items.length)}} disabled={items.length<2} aria-label="Предыдущее медиа">← Назад</button><p>{fullscreenError||'Esc — закрыть · ← → — перелистнуть'}</p><button type="button" onClick={()=>{setConfirmDelete(false);setIndex(value=>(value+1)%items.length)}} disabled={items.length<2} aria-label="Следующее медиа">Далее →</button></footer>
    </div></ModalPortal>
}
