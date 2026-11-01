import {useEffect,useRef,useState} from 'react'
import axios from 'axios'
import {useAuth} from '../../contexts/AuthContext'
import {browserPhoneKey} from '../../utils/phone'
import CountryPhoneInput from './CountryPhoneInput'
import AppLoader from '../../components/common/AppLoader'
import {otpDigits,OTP_LENGTH} from '../../utils/otp'
import {userError} from '../../utils/userError'

export default function PhoneLogin({onSuccess,onBusy}){
    const {loginWithPhone}=useAuth()
    const [deviceKey]=useState(browserPhoneKey)
    const [phone,setPhone]=useState('+7'),[code,setCode]=useState('')
    const [challenge,setChallenge]=useState(null),[receipt,setReceipt]=useState('')
    const [busy,setBusy]=useState(false),[error,setError]=useState(''),[retryAt,setRetryAt]=useState(0),[now,setNow]=useState(Date.now)
    const codeField=useRef(null)
    const [checking,setChecking]=useState(false)
    useEffect(()=>{onBusy?.(busy);return()=>onBusy?.(false)},[busy,onBusy])
    useEffect(()=>{if(!challenge&&!retryAt)return;const timer=setInterval(()=>setNow(Date.now()),1000);return()=>clearInterval(timer)},[challenge,retryAt])
    const retry=Math.max(0,Math.ceil((retryAt-now)/1000))
    const expired=challenge&&now>=challenge.expiresAt
    useEffect(()=>{if(expired){setChallenge(null);setReceipt('');setCode('');setError('Срок подтверждения истёк. Получи новый код')}},[expired])
    function editPhone(next){
        setPhone(next);setChallenge(null);setReceipt('');setCode('');setError('');setRetryAt(0)
    }
    async function send(){
        if(busy||retry)return
        setBusy(true);setError('');setReceipt('')
        try{const {data}=await axios.post('/api/auth/phone/web/request',{phone,device_key:deviceKey});setChallenge({...data,expiresAt:Date.now()+data.expires_in*1000});setCode('');setRetryAt(Date.now()+data.retry_after*1000);setNow(Date.now());requestAnimationFrame(()=>codeField.current?.focus({preventScroll:true}))}
        catch(e){setError(userError(e));if(e.response?.status===429){setRetryAt(Date.now()+60000);setNow(Date.now())}}finally{setBusy(false)}
    }
    async function submit(event){
        event.preventDefault();if(busy||expired||code.length!==OTP_LENGTH)return
        setBusy(true);setError('')
        try{
            if(!receipt){setChecking(true);const [{data}]=await Promise.all([axios.post('/api/phone-verification/verify',{challenge_id:challenge.challenge_id,code,device_key:deviceKey}),new Promise(resolve=>setTimeout(resolve,650))]);if(!data.verified)throw new Error('Номер не подтверждён');setChecking(false);setReceipt(data.verification_receipt);setChallenge(previous=>({...previous,expiresAt:Date.now()+data.expires_in*1000}));return}
            await loginWithPhone(phone,receipt,deviceKey);onSuccess?.()
        }catch(e){setError(userError(e));if(receipt&&e.response?.status===401){setReceipt('');setChallenge(null);setCode('')}}finally{setChecking(false);setBusy(false)}
    }
    return <form className="auth-form auth-phone-form" onSubmit={submit}>
        <label htmlFor="auth-phone">Номер телефона</label>
        <CountryPhoneInput value={phone} onChange={editPhone} disabled={busy}/>
        <p className="auth-phone-help">Код придёт в Trudomix на телефоне с привязанным номером.</p>
        {!receipt&&<button type="button" className="auth-code-send" disabled={busy||retry>0||phone.length<8} onClick={send}>{busy&&!checking&&!receipt?<AppLoader inline label="Отправляем код"/>:retry?`Повторить через ${retry} с`:challenge?'Отправить новый код':'Получить код'}</button>}
        {challenge&&<><label htmlFor="auth-otp">Код из уведомления</label>
            <div className={`auth-otp ${receipt?'verified':''} ${checking?'checking':''}`}>
                <input ref={codeField} id="auth-otp" value={code} onChange={event=>{setCode(otpDigits(event.target.value));setError('')}} inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{4}" maxLength={OTP_LENGTH} disabled={busy||!!receipt||!!expired} required aria-label="Код из уведомления"/>
                <div className="auth-otp-cells" aria-hidden="true">{Array.from({length:OTP_LENGTH},(_,i)=><span className={i===code.length?'active':''} key={i}><b key={code[i]||'empty'}>{code[i]||''}</b></span>)}</div>
            {checking&&<i className="auth-otp-dot" aria-hidden="true"/>}</div>
            {checking&&<p className="auth-code-checking" role="status">Проверяем код…</p>}
            {receipt&&<p className="auth-code-success" role="status">Код подтверждён. Можно войти</p>}
            {expired&&!receipt&&<p className="auth-error" role="alert">Код истёк. Получи новый код</p>}
            <button type="submit" className="btn btn-primary auth-submit" disabled={busy||code.length!==OTP_LENGTH||!!expired}>{busy?'Подожди…':receipt?'Войти':'Подтвердить код'}</button>
        </>}
        {error&&<p className="auth-error" role="alert">{error}</p>}
    </form>
}
