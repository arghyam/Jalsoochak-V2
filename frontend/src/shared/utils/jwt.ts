interface JWTPayload {
  sub: string
  email?: string
  name?: string
  preferred_username?: string
  exp?: number
  iat?: number
}

export interface UserFromJWT {
  id: string
  name: string
  email: string
  phoneNumber: string
}

export function parseJWT(token: string): JWTPayload | null {
  try {
    const base64Url = token.split('.')[1]
    if (!base64Url) return null

    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )

    return JSON.parse(jsonPayload) as JWTPayload
  } catch (error) {
    console.error('Failed to parse JWT:', error)
    return null
  }
}

export function extractUserFromJWT(idToken: string): UserFromJWT | null {
  const payload = parseJWT(idToken)
  if (!payload) return null

  return {
    id: payload.sub || '',
    name: payload.name || '',
    email: payload.email || '',
    phoneNumber: payload.preferred_username || '',
  }
}

export function isTokenExpired(token: string): boolean {
  const payload = parseJWT(token)
  if (!payload || !payload.exp) return true

  return Date.now() >= payload.exp * 1000
}
