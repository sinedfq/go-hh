import {useLayoutEffect} from 'react'
import {beginStartupTask,subscribeStartup,startupSnapshot} from './startupLoading'

// Startup covers only visible assets; infinite feeds and later pages remain lazy.
export function useStartupVisuals(enabled){
 useLayoutEffect(()=>{
  if(!enabled||!startupSnapshot())return
  let disposed=false
  const cleanups=[],seen=new WeakSet()
  const afterPaint=done=>requestAnimationFrame(()=>requestAnimationFrame(done))
  const fontsDone=beginStartupTask()
  const fontDeadline=setTimeout(fontsDone,8000)
  Promise.resolve(document.fonts?.ready).then(()=>afterPaint(()=>{clearTimeout(fontDeadline);fontsDone()}))
  const inspect=()=>{
   for(const img of document.querySelectorAll('#root img')){
    const box=img.getBoundingClientRect()
    if(seen.has(img)||!img.currentSrc&&!img.src||box.bottom<0||box.top>innerHeight||box.width===0||box.height===0)continue
    seen.add(img)
    const done=beginStartupTask()
    let finished=false
    const loaded=()=>{
     if(finished)return
     finished=true;clearTimeout(deadline)
     Promise.resolve(img.decode?.()).catch(()=>{}).then(()=>afterPaint(done))
    }
    const deadline=setTimeout(()=>{finished=true;done()},8000)
    if(img.complete)loaded()
    else{img.addEventListener('load',loaded,{once:true});img.addEventListener('error',loaded,{once:true})}
    cleanups.push(()=>{finished=true;clearTimeout(deadline);img.removeEventListener('load',loaded);img.removeEventListener('error',loaded);done()})
   }
  }
  const observer=new MutationObserver(inspect)
  observer.observe(document.getElementById('root'),{childList:true,subtree:true,attributes:true,attributeFilter:['src']})
  const unsubscribe=subscribeStartup(()=>{if(!startupSnapshot())observer.disconnect()})
  inspect()
  return()=>{if(disposed)return;disposed=true;observer.disconnect();unsubscribe();clearTimeout(fontDeadline);fontsDone();cleanups.forEach(fn=>fn())}
 },[enabled])
}
