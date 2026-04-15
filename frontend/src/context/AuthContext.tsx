import { createContext, useContext, useState, type ReactNode } from 'react'
import { logout as apiLogout } from '../api/auth'

interface AuthContextValue {
  accessToken: string | null
  isAuthenticated: boolean
  setToken: (token: string) => void
  signOut: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [accessToken, setAccessToken] = useState<string | null>(
    () => localStorage.getItem('accessToken')
  )

  const setToken = (token: string) => {
    localStorage.setItem('accessToken', token)
    setAccessToken(token)
  }

  const signOut = async () => {
    try { await apiLogout() } catch { /* ignore */ }
    localStorage.removeItem('accessToken')
    setAccessToken(null)
  }

  return (
    <AuthContext.Provider value={{ accessToken, isAuthenticated: !!accessToken, setToken, signOut }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
