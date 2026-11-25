const WEEKDAYS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

export function weekdayOf(dateStr) {
  const date = new Date(`${dateStr}T00:00:00`)
  return WEEKDAYS[date.getDay()]
}

export function monthDay(dateStr) {
  const [, month, day] = dateStr.split('-')
  return `${Number(month)}/${Number(day)}`
}

export function formatDuration(minutes) {
  const value = Number(minutes) || 0
  const hours = Math.floor(value / 60)
  const rest = value % 60
  if (!hours) return `${rest} 分钟`
  return rest ? `${hours} 小时 ${rest} 分` : `${hours} 小时`
}

export function formatKm(value) {
  const number = Number(value) || 0
  return number % 1 === 0 ? String(number) : number.toFixed(1)
}

/**
 * 训练量展示：
 * 有氧 -> "5 公里"
 * 力量 -> "4×8 @60kg" / "4 组" / "60kg"
 */
export function formatLoad(item = {}) {
  if (item.distanceKm) {
    return `${formatKm(item.distanceKm)} 公里`
  }
  const sets = Number(item.sets) || 0
  const reps = Number(item.reps) || 0
  const weight = Number(item.weightKg) || 0

  let text = ''
  if (sets && reps) text = `${sets}×${reps}`
  else if (sets) text = `${sets} 组`
  else if (reps) text = `${reps} 次`

  if (weight) {
    text = text ? `${text} · ${formatKm(weight)} kg` : `${formatKm(weight)} kg`
  }
  return text
}

