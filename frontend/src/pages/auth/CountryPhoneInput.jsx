import {useEffect,useRef,useState} from 'react'
import ModalPortal from '../../components/common/ModalPortal'
import {normalizePhone,formatPhone} from '../../utils/phone'

const countries=[['RU','🇷🇺','+7','Россия'],['KZ','🇰🇿','+7','Казахстан'],['BY','🇧🇾','+375','Беларусь'],['UA','🇺🇦','+380','Украина'],['UZ','🇺🇿','+998','Узбекистан'],['KG','🇰🇬','+996','Кыргызстан'],['AM','🇦🇲','+374','Армения'],['GE','🇬🇪','+995','Грузия'],['AZ','🇦🇿','+994','Азербайджан'],['TR','🇹🇷','+90','Турция'],['AE','🇦🇪','+971','ОАЭ'],['DE','🇩🇪','+49','Германия'],['FR','🇫🇷','+33','Франция'],['GB','🇬🇧','+44','Великобритания'],['US','🇺🇸','+1','США'],['CA','🇨🇦','+1','Канада'],['IT','🇮🇹','+39','Италия'],['ES','🇪🇸','+34','Испания'],['IL','🇮🇱','+972','Израиль'],['IN','🇮🇳','+91','Индия'],['CN','🇨🇳','+86','Китай'],['JP','🇯🇵','+81','Япония'],['AU','🇦🇺','+61','Австралия']]

export default function CountryPhoneInput({value,onChange,disabled}){
    const [preferred,setPreferred]=useState('RU'),[open,setOpen]=useState(false),[selected,setSelected]=useState(0)
    const wheel=useRef(null),trigger=useRef(null)
    const country=countries.find(item=>item[0]===preferred&&value.startsWith(item[2]))||[...countries].sort((a,b)=>b[2].length-a[2].length).find(item=>value.startsWith(item[2]))||['ZZ','🌐','+','Другой код']
    const national=value.slice(country[2].length)
    useEffect(()=>{if(open){const index=Math.max(0,countries.findIndex(item=>item[0]===country[0]));setSelected(index);wheel.current.scrollTop=index*48;wheel.current.focus({preventScroll:true})}},[open])
    function close(){setOpen(false);requestAnimationFrame(()=>trigger.current?.focus({preventScroll:true}))}
    function choose(index){const next=countries[index];setPreferred(next[0]);onChange(next[2]+national.slice(0,next[2]==='+7'?10:16-next[2].length));close()}
    function edit(event){
        const raw=event.target.value
        if(raw.startsWith('+')||(country[2]==='+7'&&raw.startsWith('8')&&raw.replace(/\D/g,'').length===11)){onChange(normalizePhone(raw));return}
        let digits=raw.replace(/\D/g,'').slice(0,country[2]==='+7'?10:16-country[2].length)
        const ordinal=raw.slice(0,event.target.selectionStart??raw.length).replace(/\D/g,'').length
        if(event.nativeEvent.inputType==='deleteContentBackward'&&digits===national&&ordinal>0)digits=digits.slice(0,ordinal-1)+digits.slice(ordinal)
        const next=country[2]+digits;onChange(next)
        const input=event.target;requestAnimationFrame(()=>{let count=0,position=0;for(;position<input.value.length;position++){if(/\d/.test(input.value[position]))count++;if(count>=ordinal){position++;break}};if(input.isConnected)input.setSelectionRange(position,position)})
    }
    const shown=country[2]==='+7'&&national?formatPhone(value).slice(3):national
    return <><div className="auth-phone-entry">
        <button ref={trigger} type="button" className="auth-country-trigger" aria-label="Выбрать код страны" disabled={disabled} onClick={()=>setOpen(true)}>{country[1]} {country[2]} <span aria-hidden="true">▾</span></button>
        <input id="auth-phone" type="tel" autoComplete="tel-national" placeholder={country[2]==='+7'?'(999) 123-45-67':'Номер телефона'} value={shown} onChange={edit} disabled={disabled} required/>
    </div>
    {open&&<ModalPortal><div className="country-picker-overlay" onKeyDown={event=>{event.stopPropagation();if(event.key==='Escape'){event.preventDefault();close()}}}>
        <section className="country-picker" role="dialog" aria-modal="true" aria-labelledby="country-picker-title">
            <div className="country-picker-heading"><h3 id="country-picker-title">Код страны</h3><button type="button" onClick={close}>Отмена</button></div>
            <div className="country-wheel-frame"><div className="country-wheel-focus" aria-hidden="true"/>
                <div ref={wheel} className="country-wheel" role="listbox" tabIndex="0" aria-label="Коды стран" aria-activedescendant={`country-${countries[selected][0]}`} onScroll={event=>setSelected(Math.max(0,Math.min(countries.length-1,Math.round(event.currentTarget.scrollTop/48))))} onKeyDown={event=>{if(['ArrowUp','ArrowDown','Enter'].includes(event.key)){event.preventDefault();if(event.key==='Enter')choose(selected);else wheel.current.scrollTo({top:Math.max(0,Math.min(countries.length-1,selected+(event.key==='ArrowDown'?1:-1)))*48,behavior:'smooth'})}}}>
                    {countries.map((item,index)=><div key={item[0]} id={`country-${item[0]}`} role="option" aria-selected={selected===index} className={selected===index?'selected':''} onClick={()=>choose(index)}><span>{item[1]}</span><strong>{item[2]}</strong><span>{item[3]}</span></div>)}
                </div>
            </div>
            <button type="button" className="btn btn-primary" onClick={()=>choose(selected)}>Выбрать {countries[selected][1]} {countries[selected][2]}</button>
        </section>
    </div></ModalPortal>}
    </>
}
