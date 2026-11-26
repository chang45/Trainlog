import { http } from './http'

/**
 * 用火山引擎方舟大模型估算一次训练消耗的热量。
 * 入参：{ type, durationMinutes, distanceKm, sets, reps, weightKg, intensity }
 * 返回：{ calories, source, explanation }
 */
export function estimateCalories(payload) {
  return http.post('/ai/calories', payload).then((res) => res.data)
}

/**
 * 根据身高体重推算每天的基础代谢和总消耗。
 * 不传参数时用当前登录用户资料里的身高体重。
 */
export function estimateDailyCalories(payload = {}) {
  return http.post('/ai/daily-calories', payload).then((res) => res.data)
}
