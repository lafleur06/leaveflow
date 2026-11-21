import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { api } from '../api/client'
import type { DemoAccount } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { ErrorMessage } from '../components/ErrorMessage'
import { Logo } from '../components/Logo'

export function LoginPage() {
  const { login, sessionEnded } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<unknown>(null)
  const [submitting, setSubmitting] = useState(false)

  // Only the public demo answers this; elsewhere the list is empty and nothing is shown.
  const demo = useQuery({ queryKey: ['demo-accounts'], queryFn: api.demoAccounts, staleTime: Infinity })

  async function logIn(loginEmail: string, loginPassword: string) {
    setSubmitting(true)
    setError(null)
    try {
      await login(loginEmail, loginPassword)
    } catch (caught) {
      setError(caught)
      setSubmitting(false)
    }
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    void logIn(email, password)
  }

  function logInAsDemo(account: DemoAccount) {
    setEmail(account.email)
    setPassword(account.password)
    void logIn(account.email, account.password)
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
        {demo.data && demo.data.length > 0 ? (
          <section className="demo-accounts" aria-labelledby="demo-heading">
            <h2 id="demo-heading" className="demo-heading">
              Or look around as someone from the demo company
            </h2>
            <ul className="demo-list">
              {demo.data.map((account) => (
                <li key={account.email}>
                  <button
                    type="button"
                    className="demo-account"
                    disabled={submitting}
                    onClick={() => logInAsDemo(account)}
                  >
                    <span className="demo-name">{account.name}</span>
                    <span className="demo-persona">{account.persona}</span>
                  </button>
                </li>
              ))}
            </ul>
            <p className="login-help">
              Demo data only. The three accounts above cannot be changed, so the demo keeps working for the next
              visitor.
            </p>
          </section>
        ) : (
          <p className="login-help">No password yet? HR sets your first one.</p>
        )}
      </main>
    </div>
  )
}
