import { create } from 'zustand'
import type { AuthUser, LoginRequest } from '@/features/auth/services/auth-api'
import { authApi } from '@/features/auth/services/auth-api'
import { AUTH_ROLES } from '@/shared/constants/auth'

const REFRESH_TOKEN_KEY = 'refresh_token'
const ACCESS_TOKEN_KEY = 'access_token'

export interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  user: AuthUser | null
  isAuthenticated: boolean
  loading: boolean
  error: string | null
  login: (payload: LoginRequest) => Promise<string>
  logout: () => Promise<void>
  bootstrap: () => Promise<void>
  sessionExpired: boolean
  setSessionExpired: () => void
  setTokens: (accessToken: string, refreshToken: string) => void
  refreshAccessToken: () => Promise<string>
}

export const useAuthStore = create<AuthState>()((set, get) => ({
  accessToken: null,
  refreshToken: null,
  user: null,
  isAuthenticated: false,
  loading: false,
  error: null,
  sessionExpired: false,

  login: async (payload: LoginRequest) => {
    set({ loading: true, error: null, sessionExpired: false })
    try {
      const { user, accessToken, refreshToken } = await authApi.login(payload)

      localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
      localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)

      set({
        accessToken,
        refreshToken,
        user,
        isAuthenticated: true,
        loading: false,
        error: null,
      })

      // Role-based redirect path
      if (user.role === AUTH_ROLES.SUPER_ADMIN) {
        return '/super-admin'
      } else if (user.role === AUTH_ROLES.STATE_ADMIN) {
        return '/state-admin'
      } else {
        return '/'
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Unable to login. Please try again.'
      set({
        loading: false,
        error: message,
        accessToken: null,
        refreshToken: null,
        user: null,
        isAuthenticated: false,
      })
      throw error
    }
  },

  logout: async () => {
    const { refreshToken } = get()
    if (refreshToken) {
      try {
        await authApi.logout(refreshToken)
      } catch {
        // Ignore logout errors
      }
    }

    localStorage.removeItem(REFRESH_TOKEN_KEY)
    localStorage.removeItem(ACCESS_TOKEN_KEY)

    set({
      accessToken: null,
      refreshToken: null,
      user: null,
      isAuthenticated: false,
      error: null,
      sessionExpired: false,
    })
  },

  bootstrap: async () => {
    const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)
    if (!refreshToken) {
      set({
        accessToken: null,
        refreshToken: null,
        user: null,
        isAuthenticated: false,
      })
      return
    }

    try {
      const {
        user,
        accessToken,
        refreshToken: newRefreshToken,
      } = await authApi.refresh(refreshToken)

      localStorage.setItem(REFRESH_TOKEN_KEY, newRefreshToken)
      localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)

      set({
        accessToken,
        refreshToken: newRefreshToken,
        user,
        isAuthenticated: true,
        sessionExpired: false,
        error: null,
      })
    } catch {
      localStorage.removeItem(REFRESH_TOKEN_KEY)
      localStorage.removeItem(ACCESS_TOKEN_KEY)

      set({
        accessToken: null,
        refreshToken: null,
        user: null,
        isAuthenticated: false,
      })
    }
  },

  setSessionExpired: () => {
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    localStorage.removeItem(ACCESS_TOKEN_KEY)

    set({
      accessToken: null,
      refreshToken: null,
      user: null,
      isAuthenticated: false,
      sessionExpired: true,
      error: 'Session expired. Please log in again.',
    })
  },

  setTokens: (accessToken: string, refreshToken: string) => {
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)

    set({
      accessToken,
      refreshToken,
    })
  },

  refreshAccessToken: async () => {
    const { refreshToken } = get()
    if (!refreshToken) {
      throw new Error('No refresh token available')
    }

    try {
      const {
        user,
        accessToken,
        refreshToken: newRefreshToken,
      } = await authApi.refresh(refreshToken)

      localStorage.setItem(REFRESH_TOKEN_KEY, newRefreshToken)
      localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)

      set({
        accessToken,
        refreshToken: newRefreshToken,
        user,
        isAuthenticated: true,
        sessionExpired: false,
      })

      return accessToken
    } catch (error) {
      // Clean up invalid tokens from localStorage
      localStorage.removeItem(REFRESH_TOKEN_KEY)
      localStorage.removeItem(ACCESS_TOKEN_KEY)

      // Reset authentication state
      set({
        accessToken: null,
        refreshToken: null,
        user: null,
        isAuthenticated: false,
      })

      // Re-throw error so axios interceptor can call setSessionExpired()
      throw error
    }
  },
}))
