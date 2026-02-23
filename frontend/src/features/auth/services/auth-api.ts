import apiClient from '@/shared/lib/axios'
import { extractUserFromJWT } from '@/shared/utils/jwt'

export interface LoginRequest {
  phoneNumber: string
  password: string
}

export interface RegisterRequest {
  firstName: string
  lastName: string
  email: string
  phoneNumber: string
  password: string
  personType: string
  tenantId: string
}

export interface TokenResponse {
  access_token: string
  refresh_token: string
  id_token: string
  expires_in: number
  refresh_expires_in: number
  token_type: string
  user_role: string
  tenant_id: string
  person_id: string
}

export interface RefreshRequest {
  refreshToken: string
}

export interface LogoutRequest {
  refreshToken: string
}

export interface AuthUser {
  id: string
  name: string
  email: string
  role: string
  phoneNumber: string
  tenantId: string
  personId: string
}

export interface LoginResponse {
  user: AuthUser
  accessToken: string
  refreshToken: string
}

export const authApi = {
  login: async (payload: LoginRequest): Promise<LoginResponse> => {
    const response = await apiClient.post<TokenResponse>('/api/v2/auth/login', {
      username: payload.phoneNumber,
      password: payload.password,
    })
    const { access_token, refresh_token, id_token, user_role, tenant_id, person_id } = response.data

    const userFromToken = extractUserFromJWT(id_token)
    if (!userFromToken) {
      throw new Error('Failed to extract user information')
    }

    const user: AuthUser = {
      ...userFromToken,
      role: user_role ?? '',
      tenantId: tenant_id ?? '',
      personId: person_id ?? '',
    }

    return {
      user,
      accessToken: access_token,
      refreshToken: refresh_token,
    }
  },

  refresh: async (refreshToken: string): Promise<LoginResponse> => {
    const response = await apiClient.post<TokenResponse>('/api/v2/auth/refresh', {
      refreshToken,
    })
    const { access_token, refresh_token, id_token, user_role, tenant_id, person_id } = response.data

    if (!access_token || !refresh_token || !id_token) {
      throw new Error('Invalid token response')
    }

    const userFromToken = extractUserFromJWT(id_token)
    if (!userFromToken) {
      throw new Error('Failed to extract user information')
    }

    const user: AuthUser = {
      ...userFromToken,
      role: user_role ?? '',
      tenantId: tenant_id ?? '',
      personId: person_id ?? '',
    }

    return {
      user,
      accessToken: access_token,
      refreshToken: refresh_token,
    }
  },

  logout: async (refreshToken: string): Promise<void> => {
    await apiClient.post('/api/v2/auth/logout', {
      refreshToken,
    })
  },

  register: async (payload: RegisterRequest): Promise<void> => {
    await apiClient.post('/api/v2/auth/register', payload)
  },
}
