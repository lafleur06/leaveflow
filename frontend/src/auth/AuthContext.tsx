import { useQuery, useQueryClient } from '@tanstack/react-query'
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { api, setUnauthorizedHandler, tokenStore } from '../api/client'
import type { Employee } from '../api/types'

interface AuthState {
  /** The logged-in employee, or null while logged out or still loading. */
  user: Employee | null
  isLoggedIn: boolean
  isLoading: boolean
  /** Set when the app logged the user out on its own (expired token). */
  sessionEnded: boolean
  login: (email: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [token, setToken] = useState(tokenStore.get)
  const [sessionEnded, setSessionEnded] = useState(false)

  const logout = useCallback(() => {
    tokenStore.clear()
    setToken(null)
    queryClient.clear()
  }, [queryClient])

  useEffect(() => {
    setUnauthorizedHandler(() => {
      setSessionEnded(true)
      logout()
    })
  }, [logout])

  const me = useQuery({ queryKey: ['me', token], queryFn: api.me, enabled: token !== null })

  const login = useCallback(async (email: string, password: string) => {
    const response = await api.login(email, password)
    tokenStore.set(response.accessToken)
    setSessionEnded(false)
    setToken(response.accessToken)
  }, [])

  const value = useMemo<AuthState>(
    () => ({
      user: me.data ?? null,
      isLoggedIn: token !== null,
      isLoading: token !== null && me.isPending,
      sessionEnded,
      login,
      logout,
    }),
    [me.data, me.isPending, token, sessionEnded, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// oxlint-disable-next-line react/only-export-components -- the hook belongs next to its provider
export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside <AuthProvider>')
  return context
}
