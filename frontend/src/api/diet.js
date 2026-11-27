import { http } from './http'

export function fetchDietRecords(date) {
  return http.get('/diet/records', { params: { date } }).then((res) => res.data)
}

export function createDietRecord(payload) {
  return http.post('/diet/records', payload).then((res) => res.data)
}

export function deleteDietRecord(id) {
  return http.delete(`/diet/records/${id}`)
}

/** 图片识别：{ mealType, imageBase64 }。视觉模型耗时可能超过默认 10s，单独放宽 */
export function recognizeDiet(payload) {
  return http.post('/diet/recognize', payload, { timeout: 60000 }).then((res) => res.data)
}
