// Critical, framework-free startup renderer. No images or application chunks.
;(function(scope){
    const COLORS=['#8257E6','#5B43E1','#6333F6','#9474ED','#9653F5','#7965DD','#5578DA','#A879FF','#7050E8','#B57FC9','#5C3DDD','#8B6CE3']
    const clamp=x=>Math.max(0,Math.min(1,x))
    const smooth=x=>{x=clamp(x);return x*x*(3-2*x)}
    const DURATION=3.7 // Keep in sync with Android LoadingStyleMotion.IMPACT.
    function frame(t,reduced=false){
        if(reduced)t=DURATION
        t=Math.max(0,t)
        const ring=clamp((t-2)/1.35),glow=clamp((t-2)/1.5),fill=smooth((t-1.5)/.5)
        return {logo:1,scale:1,trace:smooth(t/1.65),fill,orbTrace:smooth((t-1.1)/.8),orb:fill,wave:ring,ringAlpha:ring>0&&ring<1?.9*smooth(ring/.15)*(1-smooth((ring-.15)/.75)):0,flash:smooth(glow/.35)*(1-smooth((glow-.35)/.65)),wordmark:smooth((t-2.6)/1),lines:0,done:t>=DURATION}
    }
    function lineWindow(t,delay){
        const u=clamp((t-delay)/2.5),headProgress=smooth(u/.87)
        const span=.72*(1-smooth((u-.45)/.55))
        return {headProgress,tailProgress:Math.max(0,headProgress-span),alpha:smooth(u/.07)*(1-smooth((u-.85)/.15))}
    }
    function trajectories(w,h,seed=1){
        const lines=[]
        for(let corner=0;corner<4;corner++)for(let j=0;j<3;j++){
            const left=corner%2===0,top=corner<2,cx=w/2,cy=h/2,short=Math.min(w,h),margin=12+short*.018
            const a={x:left?-margin-j*4:w+margin+j*4,y:top?-margin-(2-j)*4:h+margin+(2-j)*4},d={x:cx,y:cy}
            const dx=d.x-a.x,dy=d.y-a.y,length=Math.hypot(dx,dy),nx=-dy/length,ny=dx/length
            // One random flight per launch; broad lateral sweeps, continuous tangents.
            let bits=(seed^Math.imul(corner*3+j+1,0x9e3779b9))|0
            const random=()=>{bits^=bits<<13;bits^=bits>>>17;bits^=bits<<5;return (bits>>>0)/4294967296}
            const sign=random()<.5?-1:1,knots=[a]
            for(const [k,q] of [.20,.43,.66,.85].entries()){
                const base={x:a.x+dx*q,y:a.y+dy*q}
                let low=-Infinity,high=Infinity
                for(const [value,normal,edge] of [[base.x,nx,w],[base.y,ny,h]]){
                    if(Math.abs(normal)<1e-6)continue
                    const first=(8-value)/normal,last=(edge-8-value)/normal
                    low=Math.max(low,Math.min(first,last));high=Math.min(high,Math.max(first,last))
                }
                const requested=sign*(k%2?-1:1)*short*(.38+random()*.42)*Math.sin(Math.PI*q)
                const offset=(low<=high?Math.max(low,Math.min(high,requested)):0)*(.60+random()*.36)
                knots.push({x:base.x+nx*offset,y:base.y+ny*offset})
            }
            knots.push(d)
            const points=[a]
            for(let segment=0;segment<knots.length-1;segment++){
                const start=knots[segment],end=knots[segment+1],before=knots[Math.max(0,segment-1)],after=knots[Math.min(knots.length-1,segment+2)]
                const b={x:start.x+(end.x-before.x)/6,y:start.y+(end.y-before.y)/6},c={x:end.x-(after.x-start.x)/6,y:end.y-(after.y-start.y)/6}
                for(let n=1;n<=24;n++){
                    const t=n/24,u=1-t
                    points.push({x:u*u*u*start.x+3*u*u*t*b.x+3*u*t*t*c.x+t*t*t*end.x,y:u*u*u*start.y+3*u*u*t*b.y+3*u*t*t*c.y+t*t*t*end.y})
                }
            }
            lines.push({points,color:COLORS[corner*3+j],delay:j*.08+corner*.035})
        }
        return lines
    }
    scope.TrudomixSplashMath={frame,trajectories,smooth,lineWindow}
    if(typeof document==='undefined')return
    const host=document.getElementById('startup-splash')
    if(!host)return
    const canvas=host.querySelector('canvas'),ctx=canvas.getContext('2d'),logo=host.querySelector('.startup-logo'),wordmark=host.querySelector('.startup-wordmark')
    const letterLine=logo.querySelector('.letter-line'),letter=logo.querySelector('.slot-letter'),orbLine=logo.querySelector('.orb-line'),orb=logo.querySelector('.slot-orb'),halo=logo.querySelector('.slot-halo')
    let glowGradient
    const motion=matchMedia('(prefers-reduced-motion: reduce)')
    const flightSeed=scope.crypto?.getRandomValues?scope.crypto.getRandomValues(new Uint32Array(1))[0]:(Math.random()*4294967296)>>>0
    let w=0,h=0,logoSize=0,lines=[],raf=0,fadeTimer=0,watchdog=0,ready=false,closed=false,animationDone=false,last=0
    let problem=null,retryAction=null,busy=false
    const resume=sessionStorage.getItem('trudomix-resume-startup')==='true'
    sessionStorage.removeItem('trudomix-resume-startup')
    const started=performance.now()-(resume?DURATION*1000:0)
    host.dataset.state='INITIALIZING'
    function resize(){
        w=host.clientWidth;h=host.clientHeight
        const dpr=Math.min(devicePixelRatio||1,2)
        canvas.width=Math.round(w*dpr);canvas.height=Math.round(h*dpr)
        ctx.setTransform(dpr,0,0,dpr,0,0)
        glowGradient=ctx.createRadialGradient(w/2,h/2,0,w/2,h/2,Math.max(w,h)*.8)
        glowGradient.addColorStop(0,'rgba(173,112,255,.32)');glowGradient.addColorStop(.3,'rgba(116,75,244,.15)');glowGradient.addColorStop(1,'rgba(90,55,222,0)')
        logoSize=Math.min(310,Math.max(100,Math.min(w,h)*.55))
        host.style.setProperty('--startup-logo-size',logoSize+'px')
        host.style.setProperty('--startup-wordmark-offset',(logoSize*.425+25)+'px')
        draw(last)
    }
    function animatedLines(t,f){
        if(f.lines<=0)return
        ctx.lineWidth=1.5;ctx.lineCap='round';ctx.lineJoin='round'
        for(const line of lines){
            const state=lineWindow(t,line.delay),end=state.headProgress*(line.points.length-1),start=state.tailProgress*(line.points.length-1)
            if(end<=start||state.alpha<=0)continue
            const pointAt=value=>{const index=Math.floor(value),a=line.points[index],b=line.points[Math.min(index+1,line.points.length-1)],v=value-index;return {x:a.x+(b.x-a.x)*v,y:a.y+(b.y-a.y)*v}}
            const first=pointAt(start),last=pointAt(end)
            ctx.globalAlpha=f.lines*state.alpha;ctx.strokeStyle=line.color;ctx.beginPath();ctx.moveTo(first.x,first.y)
            for(let i=Math.floor(start)+1;i<=Math.floor(end);i++)ctx.lineTo(line.points[i].x,line.points[i].y)
            ctx.lineTo(last.x,last.y)
            ctx.stroke()
        }
    }
    function circularWave(f){
        if(f.ringAlpha<=0)return
        const dark=document.documentElement.dataset.theme==='dark',color=dark?'#D4D4D8':'#A1A1AA'
        const start=logoSize*.8,end=Math.max(start+20,Math.min(w,h)*.46)
        ctx.globalAlpha=f.ringAlpha;ctx.strokeStyle=color;ctx.lineWidth=1.4;ctx.shadowColor=color;ctx.shadowBlur=3
        ctx.beginPath();ctx.arc(w/2,h/2,start+(end-start)*smooth(f.wave),0,Math.PI*2);ctx.stroke();ctx.shadowBlur=0
    }
    function draw(t){
        last=t
        const f=frame(t,motion.matches)
        ctx.clearRect(0,0,w,h)
        if(f.flash>0){
            ctx.globalAlpha=f.flash;ctx.fillStyle=glowGradient;ctx.fillRect(0,0,w,h)
        }
        if(f.ringAlpha>0){ctx.globalAlpha=f.ringAlpha;ctx.strokeStyle='#B76AFF';ctx.lineWidth=2;ctx.beginPath();ctx.arc(w/2,h/2,logoSize*.242*(.5+4.5*f.wave),0,Math.PI*2);ctx.stroke()}
        ctx.globalAlpha=1
        letterLine.style.strokeDashoffset=1-f.trace
        letter.style.opacity=f.fill
        orbLine.style.strokeDashoffset=1-f.orbTrace
        orb.style.opacity=f.orb
        halo.style.opacity=f.flash*.18
        logo.style.opacity=f.logo;logo.style.transform=`translate(-50%,-50%) scale(${f.scale})`
        wordmark.style.opacity=f.wordmark;wordmark.style.transform=`translate(-50%,${10*(1-f.wordmark)}px)`
        const logoHidden=String(f.logo===0),nameHidden=String(f.wordmark===0)
        if(logo.getAttribute('aria-hidden')!==logoHidden)logo.setAttribute('aria-hidden',logoHidden)
        if(wordmark.getAttribute('aria-hidden')!==nameHidden)wordmark.setAttribute('aria-hidden',nameHidden)
        const stage=f.done?'ready':t>=2.8?'wordmark':t>=2.2?'wave':t>=1.3?'orb':'outline'
        if(host.dataset.stage!==stage)host.dataset.stage=stage
        return f
    }
    function clean(){cancelAnimationFrame(raf);clearTimeout(fadeTimer);clearTimeout(watchdog);removeEventListener('resize',resize);removeEventListener('pagehide',pageHidden);removeEventListener('error',resourceError,true);motion.removeEventListener('change',motionChanged);themeObserver.disconnect()}
    function finish(){
        if(animationDone&&problem){showProblem();return}
        if(closed||!ready||!animationDone)return
        closed=true;host.classList.add('startup-leaving')
        fadeTimer=setTimeout(()=>{clean();host.remove();const root=document.getElementById('root');root?.removeAttribute('inert');root?.removeAttribute('aria-hidden');document.dispatchEvent(new Event('trudomix-splash-finished'))},motion.matches?0:280)
    }
    function showProblem(){
        if(closed||!animationDone||!problem)return
        clearTimeout(watchdog)
        host.classList.add('startup-failed');host.dataset.state=problem.state||'ERROR'
        let panel=host.querySelector('.startup-error')
        if(!panel){
            host.insertAdjacentHTML('beforeend','<section class="startup-error" role="alert"><h2></h2><p></p><button type="button"><span class="startup-retry-spinner" aria-hidden="true"></span><span>Повторить подключение</span></button></section>')
            panel=host.querySelector('.startup-error');panel.querySelector('button').onclick=retry
        }
        panel.querySelector('h2').textContent=problem.title
        panel.querySelector('p').textContent=problem.message
        panel.querySelector('button').disabled=busy
        panel.querySelector('button').setAttribute('aria-busy',String(busy))
    }
    async function retry(){
        if(busy||closed)return
        busy=true;showProblem();host.dataset.state='INITIALIZING'
        try{
            if(retryAction){await retryAction()}
            else{
                const controller=new AbortController(),timeout=setTimeout(()=>controller.abort(),8000)
                try{const response=await fetch('/health',{signal:controller.signal,cache:'no-store'});if(!response.ok||(await response.json()).status!=='ok')throw Error('health')}
                finally{clearTimeout(timeout)}
                sessionStorage.setItem('trudomix-resume-startup','true');location.reload()
            }
        }catch(error){
            busy=false;problem=error?.title?error:{state:navigator.onLine?'SERVER_UNAVAILABLE':'OFFLINE',title:navigator.onLine?'Нет соединения с сервером':'Нет подключения к интернету',message:'Проверьте соединение и попробуйте снова.'};showProblem()
        }
    }
    function fail(){
        if(closed)return
        problem={state:'ERROR',title:'Не удалось завершить загрузку',message:'Проверьте соединение и повторите подключение.'}
        finish()
    }
    function resourceError(event){if(event.target?.tagName==='SCRIPT'&&/\/assets\/index-|\/src\/main/.test(event.target.src))fail()}
    function pageHidden(){scope.TrudomixSplash.destroy()}
    function tick(){
        const t=motion.matches?DURATION:Math.min(DURATION,(performance.now()-started)/1000)
        const f=draw(t)
        if(!problem)host.dataset.state=f.done?(ready?'READY':'INITIALIZING'):'ANIMATING'
        if(f.done){animationDone=true;finish()}else raf=requestAnimationFrame(tick)
    }
    function motionChanged(){cancelAnimationFrame(raf);if(motion.matches){draw(DURATION);animationDone=true;finish()}else if(!animationDone)tick()}
    const themeObserver=new MutationObserver(()=>draw(last))
    themeObserver.observe(document.documentElement,{attributes:true,attributeFilter:['data-theme']})
    scope.TrudomixSplash={
        ready(){ready=true;problem=null;finish()},
        result(error,onRetry){clearTimeout(watchdog);problem=error;retryAction=onRetry;busy=false;ready=!error;if(!error){host.classList.remove('startup-failed');host.querySelector('.startup-error')?.remove();host.dataset.state=animationDone?'READY':'ANIMATING'}finish()},
        inspect(){return {elapsed:last,ready,closed,animationDone,reduced:motion.matches,state:host.dataset.state||'ANIMATING',error:problem?.kind,busy,width:w,height:h,lineCount:lines.length,stage:host.dataset.stage,...frame(last,motion.matches)}},
        destroy(){closed=true;clean();host.remove();const root=document.getElementById('root');root?.removeAttribute('inert');root?.removeAttribute('aria-hidden')}
    }
    addEventListener('resize',resize);addEventListener('pagehide',pageHidden,{once:true});addEventListener('error',resourceError,true);motion.addEventListener('change',motionChanged)
    watchdog=setTimeout(fail,45000)
    resize();tick()
})(globalThis)
