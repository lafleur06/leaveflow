import type {
  AbsenceReport,
  DemoAccount,
  Department,
  Employee,
  EmployeeInput,
  Holiday,
  LeaveEvent,
  LeaveBalance,
  LeaveRequest,
  NewLeaveRequest,
  Page,
  TokenResponse,
} from './types'

/** An error response from the API, in RFC 9457 problem+json form. */
export class ApiError extends Error {
  readonly status: number
  /** Per-field validation messages, when the API sends them. */
  readonly fieldErrors: Record<string, string>

  constructor(status: number, detail: string, fieldErrors: Record<string, string> = {}) {
    super(detail)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

const TOKEN_KEY = 'leaveflow.token'

// The token lives in sessionStorage: it survives a page reload but not closing the tab.
// Any script on the page can read it, so the app renders no user HTML and keeps tokens short-lived.
export const tokenStore = {
  get: () => sessionStorage.getItem(TOKEN_KEY),
  set: (token: string) => sessionStorage.setItem(TOKEN_KEY, token),
  clear: () => sessionStorage.removeItem(TOKEN_KEY),
}

let onUnauthorized: () => void = () => {}

/** Called when a request fails with 401, so the app can return to the login page. */
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json, application/problem+json')
  if (init.body) headers.set('Content-Type', 'application/json')
  const token = tokenStore.get()
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const response = await fetch(path, { ...init, headers })

  if (response.status === 401 && token) {
    tokenStore.clear()
    onUnauthorized()
  }
  if (!response.ok) {
    const problem = await response.json().catch(() => null)
    throw new ApiError(
      response.status,
      problem?.detail ?? `The server answered ${response.status}. Try again in a moment.`,
      problem?.errors ?? {},
    )
  }
  return response.status === 204 ? (undefined as T) : ((await response.json()) as T)
}

const json = (body: unknown) => JSON.stringify(body)

const queryString = (params: Record<string, string | number | boolean | undefined | null>) =>
  new URLSearchParams(
    Object.entries(params)
      .filter(([, value]) => value !== undefined && value !== null && value !== '')
      .map(([key, value]) => [key, String(value)]),
  ).toString()

export type ReportFormat = 'csv' | 'xlsx'

/**
 * Downloads a report file. A plain link cannot send the bearer token, so the file is fetched
 * like any other request and handed to the browser under the name the server suggests.
 */
async function downloadReport(path: string, params: Record<string, string | number | undefined>, format: ReportFormat) {
  const token = tokenStore.get()
  const response = await fetch(`${path}?${queryString({ ...params, format })}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })
  if (!response.ok) {
    const problem = await response.json().catch(() => null)
    throw new ApiError(response.status, problem?.detail ?? `The server answered ${response.status}.`, {})
  }
  const disposition = response.headers.get('Content-Disposition') ?? ''
  const fileName = /filename="([^"]+)"/.exec(disposition)?.[1] ?? `report.${format}`
  const url = URL.createObjectURL(await response.blob())
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.click()
  URL.revokeObjectURL(url)
}

export const api = {
  login: (email: string, password: string) =>
    request<TokenResponse>('/api/auth/login', { method: 'POST', body: json({ email, password }) }),

  me: () => request<Employee>('/api/me'),

  /** The demo accounts, or an empty list when the server is not a demo (the endpoint is missing). */
  demoAccounts: () =>
    request<DemoAccount[]>('/api/demo/accounts').catch((error: unknown) => {
      if (error instanceof ApiError && error.status === 404) return []
      throw error
    }),

  balance: (employeeId: number) => request<LeaveBalance>(`/api/employees/${employeeId}/leave-balance`),

  leaveRequests: (params: Record<string, string | number | boolean>) =>
    request<Page<LeaveRequest>>(`/api/leave-requests?${queryString(params)}`),

  history: (id: number) => request<LeaveEvent[]>(`/api/leave-requests/${id}/history`),

  submitLeave: (body: NewLeaveRequest) =>
    request<LeaveRequest>('/api/leave-requests', { method: 'POST', body: json(body) }),

  approve: (id: number, note?: string) =>
    request<LeaveRequest>(`/api/leave-requests/${id}/approve`, { method: 'POST', body: json({ note }) }),

  reject: (id: number, note: string) =>
    request<LeaveRequest>(`/api/leave-requests/${id}/reject`, { method: 'POST', body: json({ note }) }),

  cancel: (id: number) => request<LeaveRequest>(`/api/leave-requests/${id}/cancel`, { method: 'POST' }),

  // ---------- HR ----------

  employees: (params: { q?: string; departmentId?: number; active?: boolean; page?: number }) =>
    request<Page<Employee>>(`/api/employees?${queryString({ ...params, size: 100, sort: 'lastName,asc' })}`),

  employee: (id: number) => request<Employee>(`/api/employees/${id}`),

  createEmployee: (body: EmployeeInput) => request<Employee>('/api/employees', { method: 'POST', body: json(body) }),

  updateEmployee: (id: number, body: EmployeeInput) =>
    request<Employee>(`/api/employees/${id}`, { method: 'PUT', body: json(body) }),

  deactivateEmployee: (id: number) => request<void>(`/api/employees/${id}`, { method: 'DELETE' }),

  changeRole: (id: number, role: Employee['role']) =>
    request<Employee>(`/api/employees/${id}/role`, { method: 'PUT', body: json({ role }) }),

  setPassword: (id: number, newPassword: string) =>
    request<void>(`/api/employees/${id}/password`, { method: 'PUT', body: json({ newPassword }) }),

  departments: () => request<Page<Department>>('/api/departments?size=100&sort=name,asc'),

  createDepartment: (name: string) =>
    request<Department>('/api/departments', { method: 'POST', body: json({ name }) }),

  deleteDepartment: (id: number) => request<void>(`/api/departments/${id}`, { method: 'DELETE' }),

  holidays: (year: number) => request<Holiday[]>(`/api/holidays?year=${year}`),

  addHoliday: (date: string, name: string) =>
    request<Holiday>('/api/holidays', { method: 'POST', body: json({ date, name }) }),

  deleteHoliday: (id: number) => request<void>(`/api/holidays/${id}`, { method: 'DELETE' }),

  absences: (from: string, to: string) =>
    request<AbsenceReport>(`/api/reports/absences?${queryString({ from, to })}`),

  downloadReport,
}
