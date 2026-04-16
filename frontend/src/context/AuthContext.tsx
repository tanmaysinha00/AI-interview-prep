import { createContext, useContext, useState, type ReactNode } from 'react'
import { logout as apiLogout } from '../api/auth'

function decodeRole(token: string): string | null {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.role ?? null
  } catch {
    return null
  }
}

interface AuthContextValue {
  accessToken: string | null
  role: string | null
  isAuthenticated: boolean
  setToken: (token: string) => void
  signOut: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [accessToken, setAccessToken] = useState<string | null>(
    () => localStorage.getItem('accessToken')
  )
  const [role, setRole] = useState<string | null>(() => {
    const t = localStorage.getItem('accessToken')
    return t ? decodeRole(t) : null
  })

  const setToken = (token: string) => {
    localStorage.setItem('accessToken', token)
    setAccessToken(token)
    setRole(decodeRole(token))
  }

  const signOut = async () => {
    try { await apiLogout() } catch { /* ignore */ }
    localStorage.removeItem('accessToken')
    setAccessToken(null)
    setRole(null)
  }

  return (
    <AuthContext.Provider value={{ accessToken, role, isAuthenticated: !!accessToken, setToken, signOut }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
