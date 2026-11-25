import { http, saveToken, saveUser } from './http'

/**
 * 把后端返回的登录信息落盘：token + 头名 + 当前用户
 */
function persistSession(data) {
  saveToken(data.token, data.tokenName)
  saveUser(data.user)
}

/**
 * backend1: GET /api/captcha -> { id, image, ttlSeconds }
 * 这里统一成前端使用的 captchaId / expiresIn，视图层不用关心后端字段名
 */
export function fetchCaptcha() {
  return http.get('/captcha').then((res) => {
    const data = res.data || {}
    return {
      captchaId: data.id,
      image: data.image,
      expiresIn: data.ttlSeconds ?? 120
    }
  })
}

/**
 * backend1: POST /api/auth/login
 * 入参 { username, password, captchaId, captchaCode }
 * 返回 { token, tokenName, expiresIn, user }
 */
export function login(payload) {
  return http.post('/auth/login', payload).then((res) => {
    persistSession(res.data)
    return res.data
  })
}

/**
 * backend1: POST /api/auth/register
 * 注册成功直接返回 token（注册即登录），不需要再调一次登录
 */
export function register(payload) {
  return http.post('/auth/register', payload).then((res) => {
    persistSession(res.data)
    return res.data
  })
}
