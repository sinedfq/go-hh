import {useEffect,useRef,useState} from 'react'
import axios from 'axios'
import {useAuth} from '../../contexts/AuthContext'
import {normalizePhone,formatPhone,browserPhoneKey} from '../../utils/phone'
import {userError} from '../../utils/userError'

export default function PhoneLogin({onSuccess,onBusy}){
    const {loginWithPhone}=useAuth()
    const [deviceKey]=useState(browserPhoneKey)
    const [phone,setPhone]=useState('+7'),[code,setCode]=useState('')
    const [challenge,setChallenge]=useState(null),[receipt,setReceipt]=useState('')
    const [busy,setBusy]=useState(false),[error,setError]=useState(''),[retryAt,setRetryAt]=useState(0),[now,setNow]=useState(Date.now)
    const codeField=useRef(null)
    useEffect(()=>{onBusy?.(busy);return()=>onBusy?.(false)},[busy,onBusy])
    useEffect(()=>{if(!challenge&&!retryAt)return;const timer=setInterval(()=>setNow(Date.now()),1000);return()=>clearInterval(timer)},[challenge,retryAt])
    const retry=Math.max(0,Math.ceil((retryAt-now)/1000))
    const expired=challenge&&now>=challenge.expiresAt
    useEffect(()=>{if(expired){setChallenge(null);setReceipt('');setCode('');setError('Срок подтверждения истёк. Получи новый код')}},[expired])
    function editPhone(event){
        const input=event.target,raw=input.value,caret=input.selectionStart??raw.length
        let next=normalizePhone(raw),ordinal=raw.slice(0,caret).replace(/\D/g,'').length
        if(event.nativeEvent.inputType==='deleteContentBackward'&&next===phone&&ordinal>0){const digits=phone.slice(1);next='+'+digits.slice(0,ordinal-1)+digits.slice(ordinal);ordinal--}
        setPhone(next);setChallenge(null);setReceipt('');setCode('');setError('');setRetryAt(0)
        requestAnimationFrame(()=>{const value=formatPhone(next);let count=0,position=0;for(;position<value.length;position++){if(/\d/.test(value[position]))count++;if(count>=ordinal){position++;break}};input.setSelectionRange(position,position)})
    }
    async function send(){
        if(busy||retry)return
        setBusy(true);setError('');setReceipt('')
        try{const {data}=await axios.post('/api/auth/phone/web/request',{phone,device_key:deviceKey});setChallenge({...data,expiresAt:Date.now()+data.expires_in*1000});setCode('');setRetryAt(Date.now()+data.retry_after*1000);setNow(Date.now());requestAnimationFrame(()=>codeField.current?.focus({preventScroll:true}))}
        catch(e){setError(userError(e));if(e.response?.status===429){setRetryAt(Date.now()+60000);setNow(Date.now())}}finally{setBusy(false)}
    }
    async function submit(event){
        event.preventDefault();if(busy||expired||code.length!==6)return
        setBusy(true);setError('')
        try{
            if(!receipt){const {data}=await axios.post('/api/phone-verification/verify',{challenge_id:challenge.challenge_id,code,device_key:deviceKey});setReceipt(data.verification_receipt);setChallenge(previous=>({...previous,expiresAt:Date.now()+data.expires_in*1000}));return}
            await loginWithPhone(phone,receipt,deviceKey);onSuccess?.()
        }catch(e){setError(userError(e));if(receipt&&e.response?.status===401){setReceipt('');setChallenge(null);setCode('')}}finally{setBusy(false)}
    }
    return <form className="auth-form auth-phone-form" onSubmit={submit}>
        <label htmlFor="auth-phone">Номер телефона</label>
        <input id="auth-phone" type="tel" autoComplete="tel" placeholder="+7 (999) 123-45-67" value={formatPhone(phone)} onChange={editPhone} disabled={busy} required/>
        <p className="auth-phone-help">Код придёт через Firebase в приложение Trudomix на телефоне, где ты уже вошёл. Номер должен быть привязан в настройках приложения.</p>
        {!receipt&&<button type="button" className="auth-code-send" disabled={busy||retry>0||phone.length<8} onClick={send}>{retry?`Повторить через ${retry} с`:challenge?'Отправить новый код':'Получить код'}</button>}
        {challenge&&<><label htmlFor="auth-otp">Код из уведомления</label>
            <div className={`auth-otp ${receipt?'verified':''}`}>
                <input ref={codeField} id="auth-otp" value={code} onChange={event=>{setCode(event.target.value.replace(/\D/g,'').slice(0,6));setError('')}} inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{6}" maxLength={6} disabled={busy||!!receipt||!!expired} required aria-label="Код из уведомления"/>
                <div className="auth-otp-cells" aria-hidden="true">{Array.from({length:6},(_,i)=><span className={i===code.length?'active':''} key={i}>{code[i]||''}</span>)}</div>
            </div>
            {receipt&&<p className="auth-code-success" role="status">Код подтверждён. Можно войти</p>}
            {expired&&!receipt&&<p className="auth-error" role="alert">Код истёк. Получи новый код</p>}
            <button type="submit" className="btn btn-primary auth-submit" disabled={busy||code.length!==6||!!expired}>{busy?'Подожди…':receipt?'Войти':'Подтвердить код'}</button>
        </>}
        {error&&<p className="auth-error" role="alert">{error}</p>}
    </form>
}
