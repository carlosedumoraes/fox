import apiClient from './client'

export function loginRequest(payload) {
  return apiClient.post('/auth/login', payload)
}

export function refreshTokenRequest(refreshToken) {
  return apiClient.post('/auth/refresh', { refreshToken })
}

export function logoutRequest(refreshToken) {
  return apiClient.post('/auth/logout', { refreshToken })
}

export function getMeRequest() {
  return apiClient.get('/users/me')
}
