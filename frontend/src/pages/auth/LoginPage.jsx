import { BrandLogo } from '../../contexts/BrandContext'
import { useEffect, useRef, useState } from 'react'
import { useAuth } from '../../contexts/AuthContext'
import { userError } from '../../utils/userError'
import ModalPortal from '../../components/common/ModalPortal'
import PhoneLogin from './PhoneLogin'
import './LoginPage.css'

export default function LoginPage({ onClose }) {
    const [isLogin, setIsLogin] = useState(true)
    const [role, setRole] = useState('candidate')
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [visible, setVisible] = useState(false)
    const [error, setError] = useState('')
    const [busy, setBusy] = useState(false)
    const [method,setMethod]=useState('email')
    const { login, register } = useAuth()
    const dialog = useRef(null)
    const emailField = useRef(null)
    useEffect(() => {
        const previous = document.activeElement
        const overflow=document.body.style.overflow
        document.body.style.overflow='hidden'
        emailField.current?.focus({ preventScroll: true })
        return () => {document.body.style.overflow=overflow;previous?.focus?.({preventScroll:true})}
    }, [])
    function onKeyDown(event) {
        if (event.key === 'Escape' && !busy) onClose?.()
        if (event.key !== 'Tab') return
        const nodes = [...dialog.current.querySelectorAll('button:not(:disabled), input:not(:disabled), [href]')]
        const first = nodes[0], last = nodes[nodes.length - 1]
        if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus() }
        else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus() }
    }
    async function submit(event) {
        event.preventDefault()
        if (busy) return
        setBusy(true); setError('')
        try {
            if (isLogin) await login(email.trim(), password)
            else await register(email.trim(), password, role)
            onClose?.()
        } catch (failure) { setError(userError(failure, { login: isLogin })) }
        finally { setBusy(false) }
    }
    function switchMode() { setIsLogin(value => !value); setError(''); setPassword('') }
    return <ModalPortal>
        <div className="auth-overlay">
            <section className="auth-dialog" role="dialog" aria-modal="true" aria-labelledby="auth-title" onKeyDown={onKeyDown} ref={dialog}>
                <aside className="auth-story">
                    <BrandLogo alt="Trudomix" className="auth-logo" />
                    <span className="eyebrow">TRUDOMIX / КАРЬЕРА</span>
                    <h1>Твоя следующая<br />команда — здесь.</h1>
                    <p>Находи работу по своим правилам. Знакомься с людьми, с которыми хочется делать больше.</p>
                    <div className="auth-story-tags"><span>Вакансии</span><span>Отклики</span><span>Общение</span></div>
                </aside>
                <div className="auth-form-panel">
                    {onClose && <button type="button" className="auth-close" aria-label="Закрыть вход" onClick={onClose} disabled={busy}><svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true"><path d="m6 6 12 12M18 6 6 18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/></svg></button>}
                    <span className="eyebrow">НАЧНИ СВОЮ ИСТОРИЮ</span>
                    <h2 id="auth-title">{isLogin ? 'С возвращением' : 'Создать аккаунт'}</h2>
                    <p className="auth-caption">{isLogin ? 'Войди, чтобы продолжить свой путь.' : 'Один профиль — новые возможности.'}</p>
                    {isLogin&&<div className={`auth-methods ${method==='phone'?'by-phone':''}`} role="tablist" aria-label="Способ входа">
                        <span aria-hidden="true"/>{[['email','Почта'],['phone','Телефон']].map(([value,label])=><button key={value} type="button" role="tab" aria-selected={method===value} disabled={busy} onClick={()=>{setMethod(value);setError('')}}>{label}</button>)}
                    </div>}
                    {isLogin&&method==='phone'?<PhoneLogin onSuccess={onClose} onBusy={setBusy}/>:<form onSubmit={submit} className="auth-form">
                        {!isLogin && <fieldset className="auth-roles"><legend>Как ты будешь использовать Trudomix?</legend>
                            <div className="auth-role-options">{[['candidate', 'Ищу работу'], ['employer', 'Ищу сотрудников']].map(([value, title]) => <label key={value} className={role === value ? 'selected' : ''}>
                                <input type="radio" name="role" value={value} checked={role === value} onChange={() => setRole(value)} disabled={busy} />{title}
                            </label>)}</div>
                        </fieldset>}
                        <label htmlFor="auth-email">Почта</label>
                        <input id="auth-email" ref={emailField} type="email" autoComplete="email" placeholder="Введите почту" value={email} onChange={event => setEmail(event.target.value)} disabled={busy} required />
                        <label htmlFor="auth-password">Пароль</label>
                        <div className="auth-password"><input id="auth-password" type={visible ? 'text' : 'password'} placeholder={isLogin?'Введите пароль':'Придумайте пароль'} autoComplete={isLogin ? 'current-password' : 'new-password'} value={password} onChange={event => setPassword(event.target.value)} minLength={isLogin ? undefined : 6} disabled={busy} required />
                            <button type="button" onClick={() => setVisible(value => !value)} aria-label={visible ? 'Скрыть пароль' : 'Показать пароль'}>{visible ? 'Скрыть' : 'Показать'}</button>
                        </div>
                        {error && <p className="auth-error" role="alert">{error}</p>}
                        <button type="submit" className="btn btn-primary auth-submit" disabled={busy}>{busy ? 'Подожди…' : isLogin ? 'Войти' : 'Зарегистрироваться'}<span aria-hidden="true">↗</span></button>
                    </form>}
                    <button type="button" className="auth-switch" disabled={busy} onClick={switchMode}>{isLogin ? 'Нет аккаунта? Создать' : 'Уже есть аккаунт? Войти'}</button>
                    {onClose && <button type="button" className="auth-guest" disabled={busy} onClick={onClose}>Пока просто посмотреть вакансии</button>}
                </div>
            </section>
        </div>
    </ModalPortal>
}
