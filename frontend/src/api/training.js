import { http } from './http'

/**
 * 说明：训练类型已经改成前端固定分类（见 utils/trainingTypes.js），
 * 不再调用 /training/types，所以这里去掉了 fetchTypes。
 */

export function fetchRecords(params = {}) {
  return http.get('/training/records', { params }).then((res) => res.data)
}

export function createRecord(payload) {
  return http.post('/training/records', payload).then((res) => res.data)
}

export function updateRecord(id, payload) {
  return http.put(`/training/records/${id}`, payload).then((res) => res.data)
}

export function deleteRecord(id) {
  return http.delete(`/training/records/${id}`).then((res) => res.data)
}

export function fetchStats(days = 30) {
  return http.get('/training/stats', { params: { days } }).then((res) => res.data)
}
