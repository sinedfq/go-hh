import {useEffect,useState} from 'react'

const accents={purple:['Фиолетовый','#8257E6','#AD8CFF'],blue:['Синий','#245FCC','#91B7FF'],teal:['Бирюзовый','#006F70','#64D6CF'],green:['Зелёный','#2D703A','#8BDA99'],rose:['Розовый','#B02E64','#FFA2C5'],orange:['Оранжевый','#9A4F00','#FFB76C']}
export function applyAccent(){
    let key='purple';try{key=localStorage.getItem('trudomix-accent')||key}catch{}
    const colors=accents[key]||accents.purple,root=document.documentElement,dark=root.dataset.theme==='dark',color=colors[dark?2:1]
    root.style.setProperty('--accent',color);root.style.setProperty('--accent-hover',color);root.style.setProperty('--accent-secondary',color)
    root.style.setProperty('--accent-light',`color-mix(in srgb,${color} 14%,var(--surface))`)
}
export function AccentSync(){useEffect(()=>{applyAccent();const observer=new MutationObserver(applyAccent);observer.observe(document.documentElement,{attributes:true,attributeFilter:['data-theme']});window.addEventListener('storage',applyAccent);window.addEventListener('trudomix-accent',applyAccent);return()=>{observer.disconnect();window.removeEventListener('storage',applyAccent);window.removeEventListener('trudomix-accent',applyAccent)}},[]);return null}
export default function AppearanceOptions(){
    const [accent,setAccent]=useState(()=>{try{return localStorage.getItem('trudomix-accent')||'purple'}catch{return 'purple'}})
    const [style,setStyle]=useState(()=>{try{return localStorage.getItem('trudomix-style')||'impact'}catch{return 'impact'}})
    function chooseAccent(key){setAccent(key);try{localStorage.setItem('trudomix-accent',key)}catch{}window.dispatchEvent(new Event('trudomix-accent'))}
    function chooseStyle(key){setStyle(key);try{localStorage.setItem('trudomix-style',key)}catch{}}
    return <><h3>Цвет интерфейса</h3><div className="appearance-colors">{Object.entries(accents).map(([key,[title,color]])=><button type="button" key={key} aria-pressed={accent===key} onClick={()=>chooseAccent(key)}><span style={{background:color}}/>{title}</button>)}</div><h3>Анимация загрузки</h3><div className="appearance-styles">{Object.entries({impact:'Контур + импульс',neon:'Неоновая сборка',shutter:'Световые створки',pixel:'Пиксельная сборка + глитч'}).map(([key,title])=><button type="button" key={key} aria-pressed={style===key} onClick={()=>chooseStyle(key)}>{title}</button>)}</div><p>Стиль загрузки появится при следующем открытии сайта. Цвет интерфейса применяется сразу; логотип сохраняет исходные цвета.</p></>
}
