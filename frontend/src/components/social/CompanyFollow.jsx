import { useEffect, useState } from 'react'
import axios from 'axios'
import { useAuth } from '../../contexts/AuthContext'
import { userError } from '../../utils/userError'
import './SocialFeed.css'

export default function CompanyFollow({companyId}) {
    const {user}=useAuth()
    const [value,setValue]=useState(null),[busy,setBusy]=useState(false),[error,setError]=useState(''),[retry,setRetry]=useState(0)
    useEffect(()=>{setValue(null);setError('');if(!user)return;const controller=new AbortController();axios.get(`/api/companies/${companyId}/subscription`,{signal:controller.signal}).then(({data})=>setValue(data)).catch(e=>{if(!controller.signal.aborted)setError(userError(e))});return()=>controller.abort()},[companyId,user,retry])
    async function toggle(){setBusy(true);setError('');try{const {data}=await axios({method:value.following?'delete':'put',url:`/api/companies/${companyId}/subscription`});setValue(data);window.dispatchEvent(new Event('company-subscriptions-changed'))}catch(e){setError(userError(e))}finally{setBusy(false)}}
    return <div className="company-follow">{user?<><button type="button" className={`btn ${value?.following?'btn-secondary':'btn-primary'}`} disabled={busy||!value} aria-pressed={value?.following||false} onClick={toggle}>{busy?'Сохраняем…':value?.following?'Вы подписаны':'Подписаться'}</button>{value&&<small>{value.followers} подписчиков · новости и истории на главной</small>}</>:<p>Войдите, чтобы подписаться на компанию и видеть её истории.</p>}{error&&<p role="alert">{error} {!value&&<button type="button" className="auth-switch" onClick={()=>setRetry(x=>x+1)}>Повторить</button>}</p>}</div>
}
