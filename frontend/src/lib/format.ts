import type { LeaveStatus, LeaveType } from '../api/types'

const dayMonthYear = new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })
const dayMonth = new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short' })

/** API dates are plain "2026-07-13" strings; parse them as local dates, not UTC midnight. */
export function parseDate(iso: string): Date {
  const [year, month, day] = iso.split('-').map(Number)
  return new Date(year, month - 1, day)
}

export function formatDate(iso: string): string {
  return dayMonthYear.format(parseDate(iso))
}

/** "13 – 17 Jul 2026", "30 Jun – 3 Jul 2026", or a single day. */
export function formatRange(startIso: string, endIso: string): string {
  if (startIso === endIso) return formatDate(startIso)
  const start = parseDate(startIso)
  const end = parseDate(endIso)
  if (start.getFullYear() !== end.getFullYear()) return `${formatDate(startIso)} – ${formatDate(endIso)}`
  if (start.getMonth() === end.getMonth()) return `${start.getDate()} – ${dayMonthYear.format(end)}`
  return `${dayMonth.format(start)} – ${dayMonthYear.format(end)}`
}

export function todayIso(): string {
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}

export function days(count: number): string {
  return count === 1 ? '1 day' : `${count} days`
}

export const leaveTypeLabel: Record<LeaveType, string> = {
  ANNUAL: 'Annual leave',
  SICK: 'Sick leave',
  UNPAID: 'Unpaid leave',
}

export const statusLabel: Record<LeaveStatus, string> = {
  PENDING: 'Waiting for approval',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  CANCELLED: 'Cancelled',
}
