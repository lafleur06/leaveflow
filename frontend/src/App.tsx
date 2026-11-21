import type { ReactElement } from 'react'
import { Navigate, Route, Routes } from 'react-router'
import { useAuth } from './auth/AuthContext'
import { Layout } from './components/Layout'
import { ApprovalsPage } from './pages/ApprovalsPage'
import { EmployeePage } from './pages/EmployeePage'
import { HolidaysPage } from './pages/HolidaysPage'
import { LoginPage } from './pages/LoginPage'
import { MyLeavePage } from './pages/MyLeavePage'
import { PeoplePage } from './pages/PeoplePage'
import { ReportsPage } from './pages/ReportsPage'

export default function App() {
  const { isLoggedIn, user } = useAuth()
  // HR pages for HR only; the API refuses everyone else anyway, this just keeps them out of the way.
  const hrOnly = (page: ReactElement) => (user && user.role !== 'HR' ? <Navigate to="/" replace /> : page)

  return (
    <Routes>
      <Route path="/login" element={isLoggedIn ? <Navigate to="/" replace /> : <LoginPage />} />
      <Route element={isLoggedIn ? <Layout /> : <Navigate to="/login" replace />}>
        <Route index element={<MyLeavePage />} />
        <Route path="approvals" element={<ApprovalsPage />} />
        <Route path="people" element={hrOnly(<PeoplePage />)} />
        <Route path="people/:id" element={hrOnly(<EmployeePage />)} />
        <Route path="holidays" element={hrOnly(<HolidaysPage />)} />
        <Route path="reports" element={hrOnly(<ReportsPage />)} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
