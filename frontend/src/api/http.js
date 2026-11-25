import axios from 'axios'

export const TOKEN_KEY = 'trainlog.token'
export const TOKEN_NAME_KEY = 'trainlog.tokenName'
export const USER_KEY = 'trainlog.user'

export function hasToken() {
  return Boolean(localStorage.getItem(TOKEN_KEY))
}

/**
 * 保存登录态。
 * backend1 的 token 放在名为 tokenName 的请求头里（默认 admin-token），不是 Authorization: Bearer
 */
export function saveToken(token, tokenName = 'admin-token') {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(TOKEN_NAME_KEY, tokenName || 'admin-token')
}

export function saveUser(user) {
  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  }
}

export function getUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch (e) {
    return null
  }
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(TOKEN_NAME_KEY)
  localStorage.removeItem(USER_KEY)
}

export const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    // 头名由后端登录返回的 tokenName 决定，默认 admin-token
    const tokenName = localStorage.getItem(TOKEN_NAME_KEY) || 'admin-token'
    config.headers[tokenName] = token
  }
  return config
})

http.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message
    const url = error.config?.url || ''
    const isAuthEntry = url.includes('/auth/login') || url.includes('/auth/register')
    if (status === 401 && !isAuthEntry) {
      clearToken()
      if (!window.location.pathname.startsWith('/login')) {
        window.location.assign('/login')
      }
    }
    if (message) {
      return Promise.reject(new Error(message))
    }
    if (error.code === 'ECONNABORTED') {
      return Promise.reject(new Error('请求超时，请检查后端是否已启动'))
    }
    if (status === 404) {
      return Promise.reject(new Error('接口不存在：' + url))
    }
    return Promise.reject(new Error('网络异常，请稍后再试'))
  }
)
