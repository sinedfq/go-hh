// Snapshot the old theme and reveal the new document from the actual button center.
let active
export function themeWaveOrigin(rect,width,height){
    const x=rect.left+rect.width/2,y=rect.top+rect.height/2
    return {x,y,radius:Math.hypot(Math.max(x,width-x),Math.max(y,height-y))}
}

export function changeThemeWithWave(button,apply){
    active?.skipTransition()
    const root=document.documentElement
    if(!document.startViewTransition||matchMedia('(prefers-reduced-motion: reduce)').matches){apply();return}
    const {x,y,radius}=themeWaveOrigin(button.getBoundingClientRect(),innerWidth,innerHeight)
    root.classList.add('theme-wave-active')
    let applied=false
    const commit=()=>{if(!applied){applied=true;apply()}}
    let transition
    try{transition=document.startViewTransition(commit);active=transition}
    catch{root.classList.remove('theme-wave-active');commit();return}
    transition.ready.then(()=>{
        if(active!==transition)return
        return root.animate({clipPath:[`circle(0px at ${x}px ${y}px)`,`circle(${radius}px at ${x}px ${y}px)`]},
            {duration:620,easing:'cubic-bezier(.22,.61,.36,1)',pseudoElement:'::view-transition-new(root)'}).finished
    }).catch(()=>{}).finally(()=>{
        if(active===transition){active=null;root.classList.remove('theme-wave-active')}
    })
}
