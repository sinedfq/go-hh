import { createContext, useContext, useEffect, useState } from 'react'
import { brandAccent, brandImage, readBrand, readTheme } from '../utils/brand'
import './BrandContext.css'

const BrandContext=createContext(null)
export function BrandProvider({children}){
    const [selection,setSelection]=useState(readBrand),[theme,setTheme]=useState(readTheme)
    useEffect(()=>{
        const update=()=>setTheme(readTheme())
        const observer=new MutationObserver(update);observer.observe(document.documentElement,{attributes:true,attributeFilter:['data-theme']})
        const storage=event=>{if(event.key==='trudomix-brand')setSelection(readBrand());if(event.key==='theme')update()}
        window.addEventListener('storage',storage)
        return()=>{observer.disconnect();window.removeEventListener('storage',storage)}
    },[])
    const image=brandImage(selection,theme)
    useEffect(()=>{document.querySelectorAll('link[rel="icon"],link[rel="apple-touch-icon"]').forEach(link=>{link.href=image})},[image])
    function select(value){const next={design:value.design,color:value.color};setSelection(next);try{localStorage.setItem('trudomix-brand',JSON.stringify(next))}catch{/* Keep selection for this visit when storage is unavailable. */}}
    return <BrandContext.Provider value={{selection,theme,image,accent:brandAccent(selection.color),select}}>{children}</BrandContext.Provider>
}
export function useBrand(){return useContext(BrandContext)}
export function BrandLogo(props){const brand=useBrand();return <img {...props} className={`trudomix-brand-image ${props.className||''}`} src={brand?.image||brandImage(readBrand(),readTheme())}/>}
