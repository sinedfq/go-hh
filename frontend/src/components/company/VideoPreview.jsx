import { useEffect, useState } from 'react'

/** Seek a paused video to decode a real thumbnail without autoplaying it. */
export default function VideoPreview({src,className=''}) {
    const [ready,setReady]=useState(false),[failed,setFailed]=useState(false)
    useEffect(()=>{setReady(false);setFailed(false)},[src])
    return <span className={`video-preview ${className}`} data-ready={ready}>
        {!failed&&<video key={src} src={src} muted playsInline preload="metadata" aria-hidden="true" onLoadedMetadata={event=>{const video=event.currentTarget;video.currentTime=Math.min(.2,Number.isFinite(video.duration)?video.duration/2:.2)}} onLoadedData={()=>setReady(true)} onSeeked={()=>setReady(true)} onError={()=>setFailed(true)}/>}
        {(!ready||failed)&&<span className="video-preview-fallback" aria-hidden="true">▶</span>}
    </span>
}
