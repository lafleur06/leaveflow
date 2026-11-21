import { useQuery } from '@tanstack/react-query'
import { NavLink, Outlet } from 'react-router'
import { api } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { Logo } from './Logo'

export function Layout() {
  const { user, isLoading, logout } = useAuth()

  // Only used for the count next to "Team approvals".
  const waiting = useQuery({
    queryKey: ['approvals', user?.id],
    queryFn: () => api.leaveRequests({ managerId: user!.id, status: 'PENDING', size: 50 }),
    enabled: user !== null,
  })
  const waitingCount = waiting.data?.page.totalElements ?? 0

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <Logo />
        <nav className="sidebar-nav" aria-label="Main">
          <NavLink to="/" end className="nav-link">
            My leave
          </NavLink>
          <NavLink to="/approvals" className="nav-link">
            Team approvals
            {waitingCount > 0 && (
              <span className="nav-count" aria-label={`${waitingCount} waiting`}>
                {waitingCount}
              </span>
            )}
          </NavLink>
        </nav>
        {user && (
          <div className="sidebar-user">
            <p className="sidebar-user-name">
              {user.firstName} {user.lastName}
            </p>
            <p className="sidebar-user-meta">
              {user.jobTitle ?? user.department.name}
              {user.role === 'HR' && ', HR'}
            </p>
            <button type="button" className="button-link" onClick={logout}>
              Log out
            </button>
          </div>
        )}
      </aside>
      <main className="main">
        {isLoading || !user ? <p className="muted">Loading your profile…</p> : <Outlet />}
      </main>
    </div>
  )
}
