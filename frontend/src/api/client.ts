import axios from 'axios'

function resolveBaseUrl(): string {
  const raw = (import.meta.env.VITE_API_BASE_URL ?? '').trim()
  if (!raw) return 'http://localhost:8080'
  if (!raw.startsWith('http://') && !raw.startsWith('https://')) return `https://${raw}`
  return raw
}
const BASE_URL = resolveBaseUrl()

const api = axios.create({
  baseURL: BASE_URL,
  withCredentials: true, // send refresh-token HttpOnly cookie
  headers: { 'Content-Type': 'application/json' },
})

// Attach access token from localStorage to every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// On 401, try refreshing the access token once
let isRefreshing = false
let failedQueue: Array<{ resolve: (v: string) => void; reject: (e: unknown) => void }> = []

const processQueue = (error: unknown, token: string | null) => {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) reject(error)
    else resolve(token!)
  })
  failedQueue = []
}

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config
    if (error.response?.status === 401 && !original._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then((token) => {
          original.headers.Authorization = `Bearer ${token}`
          return api(original)
        })
      }

      original._retry = true
      isRefreshing = true

      try {
        const res = await axios.post(`${BASE_URL}/api/v1/auth/refresh`, {}, { withCredentials: true })
        const newToken: string = res.data.accessToken
        localStorage.setItem('accessToken', newToken)
        processQueue(null, newToken)
        original.headers.Authorization = `Bearer ${newToken}`
        return api(original)
      } catch (refreshError) {
        processQueue(refreshError, null)
        localStorage.removeItem('accessToken')
        window.location.hash = '#/login'
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }
    return Promise.reject(error)
  }
)

export default api
