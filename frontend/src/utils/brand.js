export const brandColors=['auto','black','white']
export function readBrand(){try{const saved=JSON.parse(localStorage.getItem('trudomix-brand')||'{}');return {design:10,color:saved.design===10&&brandColors.includes(saved.color)?saved.color:'auto'}}catch{return {design:10,color:'auto'}}}
export function readTheme(){try{return document.documentElement.dataset.theme||localStorage.getItem('theme')||'light'}catch{return 'light'}}
export function brandImage(selection,theme){const dark=selection.color==='auto'?theme==='dark':selection.color==='black';return `/brand/wave-${dark?'night':'light'}.png`}
export function brandAccent(){return '#8651f7'}
