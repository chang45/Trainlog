import { http } from './http'

/** 我的计划列表（含计划项） */
export function fetchPrograms() {
  return http.get('/training/programs').then((res) => res.data)
}

export function createProgram(payload) {
  return http.post('/training/programs', payload).then((res) => res.data)
}

export function updateProgram(id, payload) {
  return http.put(`/training/programs/${id}`, payload).then((res) => res.data)
}

export function deleteProgram(id) {
  return http.delete(`/training/programs/${id}`).then((res) => res.data)
}

export function activateProgram(id) {
  return http.put(`/training/programs/${id}/active`).then((res) => res.data)
}

export function addProgramItem(programId, payload) {
  return http.post(`/training/programs/${programId}/items`, payload).then((res) => res.data)
}

export function updateProgramItem(programId, itemId, payload) {
  return http.put(`/training/programs/${programId}/items/${itemId}`, payload).then((res) => res.data)
}

export function deleteProgramItem(programId, itemId) {
  return http.delete(`/training/programs/${programId}/items/${itemId}`).then((res) => res.data)
}
