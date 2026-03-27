import axios from 'axios'
import { useAuthStore } from '@/app/store/auth-store'
import { getApiBaseUrl } from '@/config/runtime-config'

export const apiClient = axios.create({
  baseURL: getApiBaseUrl(),
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor to add access token
apiClient.interceptors.request.use(
  (config) => {
    const accessToken = useAuthStore.getState().accessToken
    if (accessToken) {
      config.headers = config.headers ?? {}
      config.headers.Authorization = `Bearer ${accessToken}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

let isRefreshing = false
let refreshPromise: Promise<string> | null = null
const pendingRequests: Array<(token: string) => void> = []

// Response interceptor with auto-refresh on 401
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error.response?.status
    const originalRequest = error.config

    // If no response or not 401, just propagate
    if (!status || status !== 401) {
      return Promise.reject(error)
    }

    // Do not attempt refresh for auth endpoints themselves
    if (
      originalRequest?.url?.includes('/api/v2/auth/login') ||
      originalRequest?.url?.includes('/api/v2/auth/refresh') ||
      originalRequest?.url?.includes('/api/v2/auth/register') ||
      originalRequest?._retry
    ) {
      return Promise.reject(error)
    }

    // If we don't have a refresh token, nothing to refresh
    const { refreshToken, setSessionExpired } = useAuthStore.getState()
    if (!refreshToken) {
      setSessionExpired()
      return Promise.reject(error)
    }

    // Mark request as retry to prevent infinite loops
    originalRequest._retry = true

    // If refresh is not already in progress, start it
    if (!isRefreshing) {
      isRefreshing = true

      refreshPromise = (async () => {
        try {
          const newAccessToken = await useAuthStore.getState().refreshAccessToken()
          return newAccessToken
        } catch (refreshError) {
          // Refresh failed → mark session expired
          setSessionExpired()
          throw refreshError
        } finally {
          isRefreshing = false
          refreshPromise = null
        }
      })()
    }

    try {
      const newAccessToken = await refreshPromise

      if (!newAccessToken) {
        throw new Error('Failed to refresh access token')
      }

      // After successful refresh, retry all queued pending requests
      for (const cb of pendingRequests) {
        cb(newAccessToken)
      }
      pendingRequests.length = 0

      // Retry the original request with new token
      if (originalRequest) {
        originalRequest.headers = originalRequest.headers ?? {}
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
        return apiClient(originalRequest)
      }
    } catch (refreshError) {
      // Clear pending requests on refresh failure
      pendingRequests.length = 0
      throw refreshError
    }

    throw error
  }
)

export default apiClient
