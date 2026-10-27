// Resolve the current Wave before React starts.
(()=>{
 let theme='light',color='auto';
 try{theme=(localStorage.getItem('theme')==='dark'||(!['light','dark'].includes(localStorage.getItem('theme'))&&matchMedia('(prefers-color-scheme: dark)').matches))?'dark':'light';const saved=JSON.parse(localStorage.getItem('trudomix-brand')||'{}');if(saved.design===10&&['auto','black','white'].includes(saved.color))color=saved.color}catch{}
 document.documentElement.dataset.theme=theme;
 const image='/brand/wave-'+((color==='auto'?theme==='dark':color==='black')?'night':'light')+'.png';
 document.querySelectorAll('link[rel="icon"],link[rel="apple-touch-icon"]').forEach(link=>{link.href=image});
})();
