import Cookies from 'js-cookie'

const TokenKey = 'Admin-Token'

function tokenCookieOptions() {
  return {
    path: '/',
    sameSite: 'strict' as const,
    secure: globalThis.location?.protocol === 'https:'
  }
}

export function getToken(): string | undefined {
  return Cookies.get(TokenKey)
}

export function setToken(token: string): string | undefined {
  return Cookies.set(TokenKey, token, tokenCookieOptions())
}

export function removeToken(): void {
  Cookies.remove(TokenKey, tokenCookieOptions())
}
