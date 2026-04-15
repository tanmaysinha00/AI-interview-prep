import api from './client'

export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
}

export const register = (email: string, password: string, displayName: string) =>
  api.post<AuthResponse>('/api/v1/auth/register', { email, password, displayName })

export const login = (email: string, password: string) =>
  api.post<AuthResponse>('/api/v1/auth/login', { email, password })

export const logout = () =>
  api.post('/api/v1/auth/logout')
