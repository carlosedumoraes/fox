import apiClient from './client'

export async function createProcess(payload) {
  const { data } = await apiClient.post('/processes', payload)
  return data
}

export async function getProcesses(params = {}) {
  const { data } = await apiClient.get('/processes', { params })
  return data
}

export async function getProcessById(id) {
  const { data } = await apiClient.get(`/processes/${id}`)
  return data
}

export async function updateProcess(id, payload) {
  const { data } = await apiClient.patch(`/processes/${id}`, payload)
  return data
}

export async function getDashboard() {
  const { data } = await apiClient.get('/processes/dashboard')
  return data
}

export async function getHistory(id) {
  const { data } = await apiClient.get(`/processes/${id}/history`)
  return data
}

export async function getProcessReasons() {
  const { data } = await apiClient.get('/process-reasons')
  return data
}
