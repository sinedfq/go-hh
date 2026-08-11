/**
 * Считает общий опыт работы из массива записей
 * @param {Array} workExperience - массив записей опыта
 * @returns {string|null} - форматированный опыт ("1 год 2 месяца") или null
 */
export function calculateTotalExperience(workExperience) {
  if (!workExperience || workExperience.length === 0) return null

  let totalMonths = 0
  const now = new Date()

  for (const exp of workExperience) {
    const start = new Date(exp.start_date)
    const end = exp.end_date ? new Date(exp.end_date) : now

    // Разница в месяцах
    let months = (end.getFullYear() - start.getFullYear()) * 12
    months += end.getMonth() - start.getMonth()

    // Если день конца меньше дня начала — вычитаем месяц
    if (end.getDate() < start.getDate()) {
      months -= 1
    }

    if (months > 0) {
      totalMonths += months
    }
  }

  if (totalMonths === 0) return 'Меньше месяца'

  const years = Math.floor(totalMonths / 12)
  const remainingMonths = totalMonths % 12

  const getYearWord = (n) => {
    const abs = Math.abs(n) % 100
    const lastDigit = abs % 10
    if (abs > 10 && abs < 20) return 'лет'
    if (lastDigit === 1) return 'год'
    if (lastDigit >= 2 && lastDigit <= 4) return 'года'
    return 'лет'
  }

  const getMonthWord = (n) => {
    const abs = Math.abs(n) % 100
    const lastDigit = abs % 10
    if (abs > 10 && abs < 20) return 'месяцев'
    if (lastDigit === 1) return 'месяц'
    if (lastDigit >= 2 && lastDigit <= 4) return 'месяца'
    return 'месяцев'
  }

  let result = ''
  if (years > 0) {
    result += `${years} ${getYearWord(years)}`
  }
  if (remainingMonths > 0) {
    if (result) result += ' '
    result += `${remainingMonths} ${getMonthWord(remainingMonths)}`
  }

  return result
}

/**
 * Форматирует дату для отображения
 */
export function formatDate(dateString) {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleDateString('ru-RU', { 
    month: 'short', 
    year: 'numeric' 
  })
}