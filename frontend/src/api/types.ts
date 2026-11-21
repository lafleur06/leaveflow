// Shapes returned by the LeaveFlow API (see /v3/api-docs).

export type Role = 'EMPLOYEE' | 'HR'
export type LeaveType = 'ANNUAL' | 'SICK' | 'UNPAID'
export type LeaveStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED'

export interface Ref {
  id: number
  name: string
}

export interface Employee {
  id: number
  firstName: string
  lastName: string
  email: string
  jobTitle: string | null
  hireDate: string
  birthDate: string | null
  active: boolean
  role: Role
  department: Ref
  manager: Ref | null
}

export interface LeaveBalance {
  employeeId: number
  completedServiceYears: number
  earnedDays: number
  usedDays: number
  pendingDays: number
  availableDays: number
  nextAccrualDate: string
  nextAccrualDays: number
}

export interface LeaveRequest {
  id: number
  employee: Ref
  type: LeaveType
  startDate: string
  endDate: string
  workingDays: number
  reason: string | null
  status: LeaveStatus
  decidedBy: Ref | null
  decidedAt: string | null
  decisionNote: string | null
  createdAt: string
}

export interface Page<T> {
  content: T[]
  page: { size: number; number: number; totalElements: number; totalPages: number }
}

export interface TokenResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
}

export interface NewLeaveRequest {
  type: LeaveType
  startDate: string
  endDate: string
  reason?: string
}

/** Offered on the login page when the server runs the public demo. */
export interface DemoAccount {
  name: string
  email: string
  password: string
  persona: string
}
