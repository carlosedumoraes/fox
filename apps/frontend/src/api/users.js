import apiClient from './client'

export async function getUsers() {
  const { data } = await apiClient.get('/users')
  return data
}

export async function updateUser(id, payload) {
  const { data } = await apiClient.patch(`/users/${id}`, payload)
  return data
}

export async function updateRoles(id, roles) {
  const { data } = await apiClient.patch(`/users/${id}/roles`, { roles })
  return data
}

export async function enableUser(id) {
  const { data } = await apiClient.patch(`/users/${id}/enable`)
  return data
}

export async function disableUser(id) {
  const { data } = await apiClient.patch(`/users/${id}/disable`)
  return data
}
