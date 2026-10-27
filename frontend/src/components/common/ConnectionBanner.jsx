import {useEffect,useState,useRef} from 'react'
import NetworkErrorState from './NetworkErrorState'
import {checkConnection} from '../../utils/startupLoading'
import {networkFailure} from '../../utils/networkState'

export default function ConnectionBanner(){
    const [problem,setProblem]=useState(null),[busy,setBusy]=useState(false)
    const inFlight=useRef(false)
    useEffect(()=>{
        const response=event=>setProblem(event.detail)
        const offline=()=>setProblem(networkFailure({},false))
        window.addEventListener('trudomix-network',response);window.addEventListener('offline',offline)
        return()=>{window.removeEventListener('trudomix-network',response);window.removeEventListener('offline',offline)}
    },[])
    async function retry(){if(inFlight.current)return;inFlight.current=true;setBusy(true);try{await checkConnection();setProblem(null)}catch(error){setProblem(error)}finally{inFlight.current=false;setBusy(false)}}
    return problem?<div className="connection-banner"><NetworkErrorState error={problem} onRetry={retry} busy={busy} compact/></div>:null
}
