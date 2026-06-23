import apiClient from './client'

export async function getRoles() {
  const { data } = await apiClient.get('/roles')
  return data
}
