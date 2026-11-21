import { useQuery } from '@tanstack/react-query'
import { NavLink, Outlet } from 'react-router'
import { api } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { Logo } from './Logo'

export function Layout() {
  const { user, isLoading, logout } = useAuth()
  const isHr = user?.role === 'HR'

  // Only used for the count next to "Team approvals".
  const waiting = useQuery({
    queryKey: ['approvals', 'team', user?.id],
    queryFn: () => api.leaveRequests({ managerId: user!.id, status: 'PENDING', size: 50 }),
    enabled: user !== null,
  })
  // HR also decides for people who have no manager (but never on their own leave).
  const unmanaged = useQuery({
    queryKey: ['approvals', 'unmanaged'],
    queryFn: () => api.leaveRequests({ withoutManager: true, status: 'PENDING', size: 50 }),
    enabled: isHr,
  })
  const waitingCount =
    (waiting.data?.page.totalElements ?? 0) +
    (unmanaged.data?.content.filter((request) => request.employee.id !== user?.id).length ?? 0)

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <Logo />
        <nav className="sidebar-nav" aria-label="Main">
          <NavLink to="/" end className="nav-link">
            My leave
          </NavLink>
          <NavLink to="/approvals" className="nav-link">
            Approvals
            {waitingCount > 0 && (
              <span className="nav-count" aria-label={`${waitingCount} waiting`}>
                {waitingCount}
              </span>
            )}
          </NavLink>
          {isHr && (
            <>
              <p className="sidebar-group" id="hr-nav">
                Human resources
              </p>
              <NavLink to="/people" className="nav-link">
                People
              </NavLink>
              <NavLink to="/holidays" className="nav-link">
                Public holidays
              </NavLink>
              <NavLink to="/reports" className="nav-link">
                Reports
              </NavLink>
            </>
          )}
        </nav>
        {user && (
          <div className="sidebar-user">
            <p className="sidebar-user-name">
              {user.firstName} {user.lastName}
            </p>
            <p className="sidebar-user-meta">
              {user.jobTitle ?? user.department.name}
              {isHr && ', HR'}
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
