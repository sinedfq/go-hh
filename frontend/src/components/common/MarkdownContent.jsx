import { useCallback, useMemo, useState } from 'react'
import MediaViewer from '../company/MediaViewer'
import Markdown from 'react-markdown'
import remarkGfm from 'remark-gfm'

export default function MarkdownContent({ children }) {
    const [opened,setOpened]=useState(null)
    const close=useCallback(()=>setOpened(null),[])
    const components=useMemo(()=>({
        img:({node:_node,...props})=><button type="button" className="markdown-media-open" aria-label="Открыть изображение" onClick={()=>setOpened(props)}><img {...props} loading="lazy"/></button>,
        a:({node:_node,...props})=><a {...props} target="_blank" rel="noopener noreferrer"/>
    }),[])
    return <><div className="markdown-content"><Markdown remarkPlugins={[remarkGfm]} skipHtml components={components}>{children||''}</Markdown></div>{opened&&<MediaViewer items={[{url:opened.src,type:"image"}]} title={opened.alt||"Изображение вакансии"} onClose={close}/>}</>
}
