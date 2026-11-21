import type {
  Employee,
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

export const api = {
  login: (email: string, password: string) =>
    request<TokenResponse>('/api/auth/login', { method: 'POST', body: json({ email, password }) }),

  me: () => request<Employee>('/api/me'),

  balance: (employeeId: number) => request<LeaveBalance>(`/api/employees/${employeeId}/leave-balance`),

  leaveRequests: (params: Record<string, string | number>) => {
    const query = new URLSearchParams(Object.entries(params).map(([k, v]) => [k, String(v)]))
    return request<Page<LeaveRequest>>(`/api/leave-requests?${query}`)
  },

  submitLeave: (body: NewLeaveRequest) =>
    request<LeaveRequest>('/api/leave-requests', { method: 'POST', body: json(body) }),

  approve: (id: number, note?: string) =>
    request<LeaveRequest>(`/api/leave-requests/${id}/approve`, { method: 'POST', body: json({ note }) }),

  reject: (id: number, note: string) =>
    request<LeaveRequest>(`/api/leave-requests/${id}/reject`, { method: 'POST', body: json({ note }) }),

  cancel: (id: number) => request<LeaveRequest>(`/api/leave-requests/${id}/cancel`, { method: 'POST' }),
}
