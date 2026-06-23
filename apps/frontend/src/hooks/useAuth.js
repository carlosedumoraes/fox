import { useAuthStore } from '../store/authStore'

export function useAuth() {
  return useAuthStore()
}

export function hasRole(role) {
  return useAuthStore.getState().hasRole(role)
}
