import {networkFailure} from '../../utils/networkState'
import './NetworkErrorState.css'
import AppLoader from './AppLoader'

export default function NetworkErrorState({error,onRetry,busy=false,compact=false}){
    const problem=error?.title?error:networkFailure(error,navigator.onLine)
    if(!problem)return null
    return <section className={`network-error-state ${compact?'network-error-compact':''}`} role="alert">
        <div><strong>{problem.title}</strong><p>{problem.message}</p></div>
        {onRetry&&<button className="btn btn-secondary" type="button" onClick={onRetry} disabled={busy} aria-busy={busy}>{busy&&<AppLoader inline/>}{busy?'Подключение…':'Повторить подключение'}</button>}
    </section>
}
