export function normalizePhone(value){
    let digits=String(value).replace(/\D/g,'').slice(0,15)
    if(!String(value).startsWith('+')&&digits.length===11&&digits.startsWith('8'))digits='7'+digits.slice(1)
    if(!String(value).startsWith('+')&&digits.length===10)digits='7'+digits
    return digits?'+'+digits:''
}
export function formatPhone(value){
    const phone=normalizePhone(value)
    if(!phone.startsWith('+7')||phone.length>12)return phone
    const digits=phone.slice(2)
    let result='+7'
    if(digits)result+=' ('+digits.slice(0,3)
    if(digits.length>3)result+=') '+digits.slice(3,6)
    if(digits.length>6)result+='-'+digits.slice(6,8)
    if(digits.length>8)result+='-'+digits.slice(8,10)
    return result
}
export function browserPhoneKey(){
    return [...crypto.getRandomValues(new Uint8Array(32))].map(x=>x.toString(16).padStart(2,'0')).join('')
}
