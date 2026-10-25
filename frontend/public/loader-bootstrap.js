// Runs before React so the supplied animation is visible while the bundle loads.
(()=>{
    let theme='light',design=6,color='auto'
    try{
        theme=localStorage.getItem('theme')==='dark'?'dark':'light'
        const saved=JSON.parse(localStorage.getItem('trudomix-brand')||'{}')
        if(Number.isInteger(saved.design)&&saved.design>=1&&saved.design<=9)design=saved.design
        if(['auto','black','blue','coral','cream','mint','navy','violet','white'].includes(saved.color))color=saved.color
    }catch{/* Safe defaults without storage. */}
    document.documentElement.dataset.theme=theme
    const image=`/loader-icons/icon_${String(design).padStart(2,'0')}_${color==='auto'?(theme==='dark'?'black':'white'):color}.svg`
    const logo=document.querySelector('#root .trudomix-loader-logo');if(logo)logo.src=image
    document.querySelector('#root .trudomix-loader')?.style.setProperty('--loader-accent',({blue:'#248bff',violet:'#8651f7',navy:'#566bc7',mint:'#14b8a6',coral:'#ff6b69',cream:'#b58a31'})[color]||'#8651f7')
    document.querySelectorAll('link[rel="icon"],link[rel="apple-touch-icon"]').forEach(link=>{link.href=image})
})()
