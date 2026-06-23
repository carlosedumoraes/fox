import axios from 'axios'
import { useAuthStore } from '../store/authStore'

const apiBaseUrl = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '')

if (import.meta.env.DEV) {
  console.info(`API URL carregada: ${apiBaseUrl || 'same-origin'}`)
}

const apiClient = axios.create({
  baseURL: apiBaseUrl,
  headers: {
    'Content-Type': 'application/json',
  },
})

let refreshPromise = null

function isAuthUrl(url = '') {
  return url.startsWith('/auth/')
}

apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken

  if (token && !isAuthUrl(config.url)) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    const status = error.response?.status
    const isAuthEndpoint = isAuthUrl(originalRequest?.url)

    if (status !== 401 || originalRequest?._retry || isAuthEndpoint) {
      return Promise.reject(error)
    }

    const refreshToken = useAuthStore.getState().refreshToken

    if (!refreshToken) {
      useAuthStore.getState().clearAuth()
      return Promise.reject(error)
    }

    originalRequest._retry = true

    try {
      const refreshUrl = `${apiBaseUrl}/auth/refresh`
      refreshPromise =
        refreshPromise ||
        axios.post(refreshUrl, { refreshToken }, {
          headers: { 'Content-Type': 'application/json' },
        })

      const { data } = await refreshPromise
      refreshPromise = null

      useAuthStore.getState().setAuth({
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
        user: data.user,
      })

      originalRequest.headers.Authorization = `Bearer ${data.accessToken}`
      return apiClient(originalRequest)
    } catch (refreshError) {
      refreshPromise = null
      useAuthStore.getState().clearAuth()
      return Promise.reject(refreshError)
    }
  },
)

export default apiClient
