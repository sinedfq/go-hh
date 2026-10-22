export function userError(error, { login = false } = {}) {
    const status = error?.response?.status
    const message = error?.response?.data?.error || (typeof error?.response?.data==='string'?error.response.data.trim():'')
    if(message==='gallery limit reached')return 'В галерее уже 6 фотографий. Удали одну, чтобы добавить новую.'
    if(message==='saved searches limit reached')return 'Сохранено уже 20 поисков. Удали ненужную подписку.'
    if(message==='invalid salary range'||message==='invalid salary')return 'Проверь диапазон зарплаты: сумма «до» должна быть не меньше суммы «от».'
    if (!status) return 'Нет соединения с сервером. Проверь интернет и попробуй снова.'
    if (login && (status === 401 || status === 403)) return 'Неверная почта или пароль.'
    if (message === 'password must be at least 6 characters') return 'Пароль должен содержать минимум 6 символов.'
    if (status === 409 || message === 'user already exists') return 'Аккаунт с такой почтой уже существует.'
    if (status === 400 || status === 422) return 'Проверь заполненные поля.'
    if (status === 401) return 'Войди в аккаунт, чтобы продолжить.'
    if (status === 403) return 'Недостаточно прав для этого действия.'
    if (status === 413) return 'Файл слишком большой. Выбери фото меньшего размера.'
    if (status === 429) return 'Слишком много попыток. Подожди немного.'
    return 'Сервис временно недоступен. Попробуй позже.'
}
