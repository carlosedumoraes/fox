import { create } from 'zustand'
import { persist } from 'zustand/middleware'

const AUTH_STORAGE_KEY = 'fox-auth'

export const useAuthStore = create(
  persist(
    (set, get) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
      isRestoring: true,

      setAuth: ({ user, accessToken, refreshToken }) => {
        set({
          user,
          accessToken,
          refreshToken,
          isAuthenticated: Boolean(accessToken),
          isRestoring: false,
        })
      },

      finishRestoring: () => {
        set({ isRestoring: false })
      },

      logout: () => get().clearAuth(),

      clearAuth: () => {
        set({
          user: null,
          accessToken: null,
          refreshToken: null,
          isAuthenticated: false,
          isRestoring: false,
        })
        localStorage.removeItem(AUTH_STORAGE_KEY)
      },

      hasRole: (role) => {
        return get().user?.roles?.includes(role) || false
      },
    }),
    {
      name: AUTH_STORAGE_KEY,
      partialize: (state) => ({
        user: state.user,
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        isAuthenticated: state.isAuthenticated,
      }),
    },
  ),
)
