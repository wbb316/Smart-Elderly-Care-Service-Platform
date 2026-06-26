import Cookies from 'js-cookie'

const TokenKey = 'Admin-Token'

export function getToken(): string | undefined {
  return Cookies.get(TokenKey)
}

export function setToken(token: string): string | undefined {
  const isSecure = location.protocol === 'https:'
  return Cookies.set(TokenKey, token, { secure: isSecure, sameSite: 'Strict' })
}

export function removeToken(): void {
  Cookies.remove(TokenKey)
}
