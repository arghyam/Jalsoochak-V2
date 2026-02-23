export const AUTH_ROLES = {
  SUPER_ADMIN: 'super_user',
  STATE_ADMIN: 'state_admin',
  BUSINESS_USER: 'business_user',
} as const

export type AuthRole = (typeof AUTH_ROLES)[keyof typeof AUTH_ROLES]

export const AUTH_STORAGE_KEY = 'JALSOOCHAK_AUTH_TOKEN'
