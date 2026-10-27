import axios from 'axios'
import {networkFailure} from './networkState'

const pending=new Set(['react-mount']),listeners=new Set()
let loading=true,timer,nextId=0,failure=null,attempt=0,probe=null
function notify(){listeners.forEach(listener=>listener())}
function settle(){
    clearTimeout(timer)
    if(!loading||pending.size)return
    timer=setTimeout(()=>{if(!pending.size&&loading){loading=false;notify()}},150)
}
axios.interceptors.request.use(config=>{
    if(loading&&!config.skipStartup){clearTimeout(timer);config.startupAttempt=attempt;config.startupId=++nextId;pending.add(nextId);if(!config.timeout)config.timeout=12000}
    else if(!config.timeout)config.timeout=config.data instanceof FormData?120000:20000
    return config
})
function finish(config){if(config?.startupAttempt===attempt){pending.delete(config.startupId);settle()}}
axios.interceptors.response.use(result=>{
    finish(result.config)
    if(!loading)window.dispatchEvent(new CustomEvent('trudomix-network',{detail:null}))
    return result
},error=>{
    const problem=networkFailure(error,navigator.onLine)
    if(loading&&error.config?.startupAttempt===attempt&&problem&&problem.kind!=='AUTH')failure=problem
    else if(!loading&&problem&&problem.kind!=='AUTH')window.dispatchEvent(new CustomEvent('trudomix-network',{detail:problem}))
    finish(error.config);return Promise.reject(error)
})
export const subscribeStartup=listener=>{listeners.add(listener);return()=>listeners.delete(listener)}
export const startupSnapshot=()=>loading
export const startupFailure=()=>failure
export function startupMounted(){pending.delete('react-mount');settle()}
export function beginStartupTask(){
    if(!loading)return()=>{}
    const generation=attempt,id=++nextId;clearTimeout(timer);pending.add(id)
    let finished=false
    return()=>{if(finished||generation!==attempt)return;finished=true;pending.delete(id);settle()}
}
export async function trackStartupModule(load){
    const done=beginStartupTask()
    try{return await load()}catch(error){if(loading)failure={state:'ERROR',kind:'APPLICATION',title:'Не удалось загрузить экран',message:'Проверьте соединение и повторите загрузку.'};throw error}finally{done()}
}
export async function checkConnection(){
    const controller=new AbortController(),timeout=setTimeout(()=>controller.abort(),8000)
    try{
        const result=await axios.get('/health',{signal:controller.signal,timeout:8000,skipStartup:true,headers:{'Cache-Control':'no-cache'}})
        if(result.data?.status!=='ok')throw new Error('Unexpected health response')
    }catch(error){throw networkFailure(controller.signal.aborted?{code:'ETIMEDOUT'}:error,navigator.onLine)||networkFailure({},navigator.onLine)}
    finally{clearTimeout(timeout)}
}
export async function retryStartup(){
    if(probe)return probe
    probe=(async()=>{
        const reloadModule=failure?.kind==='APPLICATION'
        await checkConnection()
        if(reloadModule){sessionStorage.setItem('trudomix-resume-startup','true');location.reload();return}
        attempt++;failure=null;loading=true;clearTimeout(timer);pending.clear();pending.add('react-mount');notify()
        window.dispatchEvent(new Event('trudomix-startup-retry'))
    })()
    try{await probe}finally{probe=null}
}
