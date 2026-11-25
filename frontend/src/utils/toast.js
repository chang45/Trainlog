import { reactive } from 'vue'

export const toasts = reactive([])

let seed = 0

export function showToast(message, type = 'success') {
  const id = ++seed
  toasts.push({ id, message, type })
  window.setTimeout(() => removeToast(id), 2600)
}

export function removeToast(id) {
  const index = toasts.findIndex((item) => item.id === id)
  if (index >= 0) toasts.splice(index, 1)
}
