import AppLoader from '../common/AppLoader'
import { groupStories, nextStoryExpiry } from '../../utils/storyGroups'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import axios from 'axios'
import { useAuth } from '../../contexts/AuthContext'
import { userError } from '../../utils/userError'
import MediaGallery from '../company/MediaGallery'
import MediaViewer from '../company/MediaViewer'
import VideoPreview from '../company/VideoPreview'
import './SocialFeed.css'
import CompactRefreshButton from '../common/CompactRefreshButton'

const date=value=>new Date(value).toLocaleString('ru-RU',{day:'numeric',month:'short',hour:'2-digit',minute:'2-digit'})
const storyPlural=new Intl.PluralRules('ru')
const storyCount=count=>`${count} ${{one:'история',few:'истории',many:'историй',other:'истории'}[storyPlural.select(count)]}`
function ReactionIcon({kind,filled=false}){return <svg width="20" height="20" viewBox="0 0 24 24" fill={filled?'currentColor':'none'} stroke="currentColor" strokeWidth="1.8" aria-hidden="true">{kind==='like'?<path d="M20.8 4.6a5.4 5.4 0 0 0-7.6 0L12 5.8l-1.2-1.2a5.4 5.4 0 0 0-7.6 7.6L12 21l8.8-8.8a5.4 5.4 0 0 0 0-7.6Z"/>:<path d="M21 11.5a8.4 8.4 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.4 8.4 0 0 1-3.8-.9L3 21l1.9-5.7a8.4 8.4 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.4 8.4 0 0 1 3.8-.9h.5a8.5 8.5 0 0 1 8 8v.5Z"/>}</svg>}
function Avatar({photo,name}){return photo?<img className="social-avatar" src={photo} alt="" loading="lazy"/>:<span className="social-avatar social-initial" aria-hidden="true">{name?.charAt(0)||'Т'}</span>}

export function SocialComposer({story=false,onPublished}) {
    const {user}=useAuth()
    const [body,setBody]=useState(''),[asCompany,setAsCompany]=useState(false),[file,setFile]=useState(null),[busy,setBusy]=useState(false),[error,setError]=useState(''),[preview,setPreview]=useState('')
    const input=useRef(null)
    const employer=['employer','admin'].includes(user?.role)
    useEffect(()=>{if(!file){setPreview('');return}const url=URL.createObjectURL(file);setPreview(url);return()=>URL.revokeObjectURL(url)},[file])
    if(!user||(story&&!employer))return null
    function pick(event){const media=event.target.files?.[0];event.target.value='';setError('');if(!media)return;const video=['video/mp4','video/webm'].includes(media.type);if(!video&&!['image/jpeg','image/png','image/webp'].includes(media.type)){setError('Выберите JPG, PNG, WebP, MP4 или WebM.');return}if(media.size>(video?50:4)*1024*1024){setError(video?'Видео — до 50 МБ.':'Фото — до 4 МБ.');return}setFile(media)}
    async function publish(event){event.preventDefault();if(busy)return;setBusy(true);setError('');const data=new FormData();data.append('body',body.trim());data.append('as_company',String(story||asCompany));if(file)data.append(file.type.startsWith('video/')?'video':'photo',file);try{await axios.post(story?'/api/stories':'/api/posts',data);setBody('');setFile(null);onPublished?.();window.dispatchEvent(new Event(story?'company-stories-changed':'social-posts-changed'))}catch(e){setError(userError(e))}finally{setBusy(false)}}
    return <form className="social-composer" onSubmit={publish}>
        <div className="social-section-title"><div><span className="eyebrow">{story?'НА 24 ЧАСА':'ПОДЕЛИСЬ С КОМАНДОЙ'}</span><h3>{story?'Новая история компании':'Что нового?'}</h3></div>{!story&&employer&&<label className="social-author-toggle"><input type="checkbox" checked={asCompany} disabled={busy} onChange={e=>setAsCompany(e.target.checked)}/>От компании</label>}</div>
        <label className="social-text-label">{story?'Подпись к истории':'Текст публикации'}<textarea value={body} onChange={e=>setBody(e.target.value)} maxLength={story?500:5000} disabled={busy} placeholder={story?'Покажите день из жизни команды':'Расскажите о работе, опыте или новостях'} rows={story?2:3}/></label>
        <small className="social-muted">{body.length}/{story?500:5000}{story?' · История исчезнет через 24 часа после публикации.':''}</small>
        {file&&<div className="social-attachment">{file.type.startsWith('video/')?<video src={preview} controls playsInline preload="metadata"/>:<img src={preview} alt="Вложение публикации"/>}<button type="button" className="btn btn-secondary" disabled={busy} onClick={()=>setFile(null)}>Убрать вложение</button></div>}
        <div className="social-actions"><input type="file" hidden ref={input} accept="image/jpeg,image/png,image/webp,video/mp4,video/webm" onChange={pick}/><button type="button" className="btn btn-secondary" disabled={busy} onClick={()=>input.current.click()}>{file?'Заменить файл':'Фото или видео'}</button><button className="btn btn-primary" disabled={busy||(!body.trim()&&!file)||(story&&!file)}>{busy?'Публикуем…':story?'Опубликовать историю':'Опубликовать'}</button></div>
        {error&&<p className="social-error" role="alert">{error}</p>}
    </form>
}

export function TimedStories({companyId}) {
    const {user}=useAuth()
    const [items,setItems]=useState([]),[opened,setOpened]=useState(null),[error,setError]=useState(''),[loading,setLoading]=useState(false),[now,setNow]=useState(Date.now())
    const reload=useRef(null)
    useEffect(()=>{
        setItems([]);setError('')
        if(!user)return
        const controller=new AbortController()
        let disposed=false,inFlight=false,queued=false
        async function load(afterMutation=false){
            if(disposed)return
            if(inFlight){if(afterMutation)queued=true;return}
            inFlight=true;setLoading(true)
            try{
                const {data}=await axios.get('/api/stories',{params:companyId?{company_id:companyId}:{},signal:controller.signal})
                if(!disposed){setItems(Array.isArray(data)?data:[]);setNow(Date.now());setError('')}
            }catch(e){if(!disposed&&!controller.signal.aborted)setError(userError(e))}
            finally{inFlight=false;if(!disposed){setLoading(false);if(queued){queued=false;load()}}}
        }
        reload.current=load
        const changed=()=>load(true)
        load()
        const interval=setInterval(()=>{if(!document.hidden)load()},60000)
        window.addEventListener('company-stories-changed',changed)
        window.addEventListener('company-subscriptions-changed',changed)
        return()=>{disposed=true;reload.current=null;controller.abort();clearInterval(interval);window.removeEventListener('company-stories-changed',changed);window.removeEventListener('company-subscriptions-changed',changed)}
    },[user,companyId])
    useEffect(()=>{
        const next=nextStoryExpiry(items,Date.now())
        const timer=next===null?null:setTimeout(()=>setNow(Date.now()),Math.min(2147483647,Math.max(1,next-Date.now()+1)))
        const visible=()=>{if(!document.hidden)setNow(Date.now())}
        document.addEventListener('visibilitychange',visible)
        return()=>{clearTimeout(timer);document.removeEventListener('visibilitychange',visible)}
    },[items,now])
    const groups=useMemo(()=>groupStories(items,now),[items,now])
    const selected=groups.find(group=>group.id===opened)
    const close=useCallback(()=>setOpened(null),[])
    async function remove(item){setOpened(null);try{await axios.delete(`/api/stories/${item.id}`);reload.current?.(true)}catch(e){setError(userError(e))}}
    if(!user)return null
    return <section className="social-stories" aria-label="Истории компаний"><div className="social-section-title"><div><h2>{companyId?'Истории компании':'Истории подписок'}</h2></div><CompactRefreshButton loading={loading} onClick={()=>reload.current?.()} ariaLabel="Обновить истории"/></div>{error&&<p role="alert" className="social-error">{error}</p>}{!groups.length?<p className="social-muted">{companyId?'Активных историй пока нет.':'Подпишитесь на компании: их новые истории появятся здесь.'}</p>:<div className="social-story-strip">{groups.map(group=><button key={group.id} className="social-story-ring" onClick={()=>setOpened(group.id)}><span className="social-story-cover">{group.items.at(-1).media_type==='video'?<VideoPreview src={group.items.at(-1).media_url}/>:<img className="social-story-background" src={group.items.at(-1).media_url} alt=""/>}<span className="social-story-logo"><Avatar photo={group.items[0].company_logo} name={group.items[0].company_name}/></span></span><strong>{group.items[0].company_name}</strong><small>{storyCount(group.items.length)}</small></button>)}</div>}{selected&&<MediaViewer items={selected.items.map(item=>({url:item.media_url,type:item.media_type,caption:item.caption,id:item.id,canDelete:item.can_delete}))} title={selected.items[0].company_name} storyMode onRemove={remove} onClose={close}/>}</section>
}

function Comments({post,onCount}){
    const [items,setItems]=useState([]),[next,setNext]=useState(0),[body,setBody]=useState(''),[busy,setBusy]=useState(false),[error,setError]=useState(''),[loading,setLoading]=useState(true)
    const load=useCallback(async(before=0)=>{setLoading(true);setError('');try{const {data}=await axios.get(`/api/posts/${post.id}/comments`,{params:before?{before}:{}});setItems(old=>before?[...old,...data.comments]:data.comments);setNext(data.next_cursor)}catch(e){setError(userError(e))}finally{setLoading(false)}},[post.id])
    useEffect(()=>{load()},[load])
    async function submit(event){event.preventDefault();setBusy(true);setError('');try{await axios.post(`/api/posts/${post.id}/comments`,{body:body.trim()});setBody('');onCount(1);await load()}catch(e){setError(userError(e))}finally{setBusy(false)}}
    async function remove(id){setBusy(true);setError('');try{await axios.delete(`/api/comments/${id}`);setItems(old=>old.filter(item=>item.id!==id));onCount(-1)}catch(e){setError(userError(e))}finally{setBusy(false)}}
    return <div className="social-comments">{next>0&&<button type="button" className="auth-switch" disabled={loading} onClick={()=>load(next)}>Предыдущие комментарии</button>}{[...items].reverse().map(item=><article className="social-comment" key={item.id}><Avatar photo={item.author_photo} name={item.author_name}/><div><strong>{item.author_name}</strong><small>{date(item.created_at)}</small><p>{item.body}</p>{item.can_delete&&<button type="button" className="auth-switch" disabled={busy} onClick={()=>remove(item.id)}>Удалить</button>}</div></article>)}{loading&&<AppLoader label="Загружаем комментарии…"/>}{error&&<p role="alert" className="social-error">{error} <button className="auth-switch" onClick={()=>load()}>Повторить</button></p>}<form onSubmit={submit}><label>Ваш комментарий<textarea aria-label="Ваш комментарий" rows={2} maxLength={2000} value={body} disabled={busy} onChange={e=>setBody(e.target.value)}/></label><button className="btn btn-primary" disabled={busy||!body.trim()}>{busy?'Отправляем…':'Отправить'}</button></form></div>
}
function PostCard({post,onUpdate,onDelete}){
    const [comments,setComments]=useState(false),[busy,setBusy]=useState(false),[error,setError]=useState(''),[confirm,setConfirm]=useState(false)
    async function like(){setBusy(true);setError('');try{const {data}=await axios({method:post.liked?'delete':'put',url:`/api/posts/${post.id}/like`});onUpdate(data)}catch(e){setError(userError(e))}finally{setBusy(false)}}
    async function remove(){setBusy(true);setError('');try{await axios.delete(`/api/posts/${post.id}`);onDelete()}catch(e){setError(userError(e));setBusy(false)}}
    return <article className="social-post"><header className="social-post-header"><Avatar photo={post.author_photo} name={post.author_name}/><div className="social-post-identity"><strong>{post.author_name}</strong><small>{post.company_id?'Компания':'Личный профиль'} · {date(post.created_at)}</small></div>{post.can_delete&&<button className="social-delete-button" disabled={busy} onClick={()=>setConfirm(!confirm)}>Удалить</button>}</header>{post.body&&<p className="social-post-body">{post.body}</p>}{post.media_url&&<MediaGallery photos={post.media_type==='image'?[post.media_url]:[]} videos={post.media_type==='video'?[post.media_url]:[]} title={`Публикация ${post.author_name}`}/>}<div className="social-post-reactions"><button className={post.liked?'is-liked':''} disabled={busy} aria-pressed={post.liked} aria-label={`Нравится: ${post.likes}`} onClick={like}><ReactionIcon kind="like" filled={post.liked}/><span className="social-reaction-count">{post.likes}</span></button><button aria-expanded={comments} onClick={()=>setComments(!comments)}><ReactionIcon kind="comment"/><span>Комментарии</span><span className="social-reaction-count">{post.comments}</span></button></div>{confirm&&<div className="social-delete-confirm"><span>Удалить пост и его комментарии?</span><button className="btn btn-secondary" disabled={busy} onClick={()=>setConfirm(false)}>Отмена</button><button className="btn btn-primary" disabled={busy} onClick={remove}>Удалить пост</button></div>}{error&&<p role="alert" className="social-error">{error}</p>}{comments&&<Comments post={post} onCount={delta=>onUpdate({comments:Math.max(0,post.comments+delta)})}/>}</article>
}
export default function SocialFeed({companyId,composer=true,stories=true}){
    const {user}=useAuth()
    const [posts,setPosts]=useState([]),[scope,setScope]=useState('all'),[next,setNext]=useState(0),[loading,setLoading]=useState(false),[error,setError]=useState(''),[revision,setRevision]=useState(0)
    const request=useRef(0)
    const loadedContext=useRef('')
    const invalidate=useCallback(()=>{request.current++},[])
    const load=useCallback(async(before=0)=>{const current=++request.current;setLoading(true);setError('');try{const {data}=await axios.get('/api/feed',{params:{scope,company_id:companyId||undefined,before:before||undefined}});if(current===request.current){setPosts(old=>before?[...old,...data.posts]:data.posts);setNext(data.next_cursor)}}catch(e){if(current===request.current)setError(userError(e))}finally{if(current===request.current)setLoading(false)}},[companyId,scope])
    useEffect(()=>{const context=`${user?.id}:${companyId}:${scope}`;if(loadedContext.current!==context){setPosts([]);setNext(0);loadedContext.current=context}if(user)load();return invalidate},[user,companyId,scope,load,revision,invalidate])
    if(!user)return null
    return <section className="social-feed" aria-label="Лента публикаций">{stories&&<TimedStories companyId={companyId}/>}<div className="social-section-title"><div><span className="eyebrow">ЛЮДИ И КОМАНДЫ</span><h2>{companyId?'Публикации компании':'Лента'}</h2></div><CompactRefreshButton loading={loading} onClick={()=>setRevision(x=>x+1)} ariaLabel="Обновить ленту"/></div>{composer&&<SocialComposer onPublished={()=>setRevision(x=>x+1)}/>}<div className="social-feed-tabs">{!companyId&&[['all','Все публикации'],['following','Мои подписки']].map(([value,label])=><button key={value} aria-pressed={scope===value} className={scope===value?'active':''} onClick={()=>setScope(value)}>{label}</button>)}</div>{posts.map(post=><PostCard key={post.id} post={post} onUpdate={patch=>setPosts(old=>old.map(item=>item.id===post.id?{...item,...patch}:item))} onDelete={()=>setPosts(old=>old.filter(item=>item.id!==post.id))}/>)}{!loading&&!error&&!posts.length&&<div className="social-empty"><h3>Здесь начнётся общение</h3><p>{scope==='following'?'Подпишитесь на компании, чтобы видеть их новости.':'Поделитесь первым постом о своей работе или команде.'}</p></div>}{error&&<p role="alert" className="social-error">{error} <button className="auth-switch" onClick={()=>load()}>Повторить</button></p>}{loading&&<AppLoader label="Загружаем ленту…"/>}{next>0&&<button className="btn btn-secondary" disabled={loading} onClick={()=>load(next)}>Показать ещё</button>}</section>
}
