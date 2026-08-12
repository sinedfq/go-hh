
export const digitsOnly = (value) => value.replace(/\D/g, '')

export const normalizePhone = (value) => {
    let digits = digitsOnly(value)
    if (!digits) return ''

    if (digits.startsWith('8')) digits = '7' + digits.slice(1)
    if (!digits.startsWith('7')) digits = '7' + digits

    return digits.slice(0, 11)
}

export const formatPhone = (value) => {
    const d = normalizePhone(value)
    if (!d) return ''

    let result = '+7'
    if (d.length > 1) result += ' (' + d.slice(1, 4)
    if (d.length >= 5) result += ')'
    if (d.length > 4) result += ' ' + d.slice(4, 7)
    if (d.length > 7) result += '-' + d.slice(7, 9)
    if (d.length > 9) result += '-' + d.slice(9, 11)

    return result
}

// ====== URL ======

export const normalizeUrl = (url) => {
    if (!url) return ''
    let trimmed = url.trim()
    if (!/^https?:\/\//i.test(trimmed)) {
        trimmed = 'https://' + trimmed
    }
    return trimmed
}

export const displayUrl = (url) => {
    if (!url) return ''
    return url.replace(/^https?:\/\//i, '')
}

// ====== TELEGRAM ======

export const normalizeTelegram = (tg) => {
    if (!tg) return ''
    let trimmed = tg.trim()
    if (trimmed.includes('t.me/')) return trimmed
    if (!trimmed.startsWith('@')) {
        trimmed = '@' + trimmed
    }
    return trimmed
}