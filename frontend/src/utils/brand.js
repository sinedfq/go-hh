export const brandColors=['auto','black','blue','coral','cream','mint','navy','violet','white']
export function readBrand(){try{const saved=JSON.parse(localStorage.getItem('trudomix-brand')||'{}');return {design:Number.isInteger(saved.design)&&saved.design>=1&&saved.design<=9?saved.design:6,color:brandColors.includes(saved.color)?saved.color:'auto'}}catch{return {design:6,color:'auto'}}}
export function readTheme(){try{return document.documentElement.dataset.theme||localStorage.getItem('theme')||'light'}catch{return 'light'}}
export function brandImage(selection,theme){const color=selection.color==='auto'?(theme==='dark'?'black':'white'):selection.color;return `/loader-icons/icon_${String(selection.design).padStart(2,'0')}_${color}.svg`}
export function brandAccent(color){return {blue:'#248bff',violet:'#8651f7',navy:'#566bc7',mint:'#14b8a6',coral:'#ff6b69',cream:'#b58a31'}[color]||'#8651f7'}
