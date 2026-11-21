import { useState } from 'react'
import type { FormEvent } from 'react'
import { useAuth } from '../auth/AuthContext'
import { ErrorMessage } from '../components/ErrorMessage'
import { Logo } from '../components/Logo'

export function LoginPage() {
  const { login, sessionEnded } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<unknown>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      await login(email, password)
    } catch (caught) {
      setError(caught)
      setSubmitting(false)
    }
  }

  return (
    <div className="login">
      <div className="login-tiles" aria-hidden="true" />
      <main className="login-panel">
        <Logo />
        <h1 className="login-title">Log in to plan your time off</h1>
        {sessionEnded && <p className="notice">Your session has ended. Log in again to continue.</p>}
        <form className="form" onSubmit={handleSubmit}>
          <label className="field">
            <span className="field-label">Work email</span>
            <input
              type="email"
              autoComplete="username"
              required
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />
          </label>
          <label className="field">
            <span className="field-label">Password</span>
            <input
              type="password"
              autoComplete="current-password"
              required
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />
          </label>
          <ErrorMessage error={error} />
          <button type="submit" className="button-primary" disabled={submitting}>
            {submitting ? 'Logging in…' : 'Log in'}
          </button>
        </form>
        <p className="login-help">No password yet? HR sets your first one.</p>
      </main>
    </div>
  )
}
