export function networkFailure(error, online = true) {
    if(error?.code==='ERR_CANCELED')return null
    const status=error?.response?.status
    if(status===401||status===403)return {state:'ERROR',kind:'AUTH',title:'Необходимо войти в аккаунт',message:'Войдите снова, чтобы продолжить.'}
    if(status>=500)return {state:'ERROR',kind:'SERVER',title:'Ошибка сервера',message:'Сервер временно не может обработать запрос. Попробуйте подключиться ещё раз.'}
    if(status)return null
    if(error?.code==='ECONNABORTED'||error?.code==='ETIMEDOUT')return {state:'ERROR',kind:'TIMEOUT',title:'Сервер не ответил вовремя',message:'Соединение заняло слишком много времени. Попробуйте ещё раз.'}
    if(!online)return {state:'OFFLINE',kind:'OFFLINE',title:'Нет подключения к интернету',message:'Проверьте Wi-Fi или мобильное соединение и попробуйте снова.'}
    return {state:'SERVER_UNAVAILABLE',kind:'UNAVAILABLE',title:'Нет соединения с сервером',message:'Проверьте подключение к интернету. Если оно работает, сервер может быть временно недоступен.'}
}
