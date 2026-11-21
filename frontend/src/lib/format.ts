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

const weekdayDayMonth = new Intl.DateTimeFormat('en-GB', { weekday: 'long', day: 'numeric', month: 'long' })
const dateTime = new Intl.DateTimeFormat('en-GB', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

/** "Thursday 23 April" */
export function formatWeekday(iso: string): string {
  return weekdayDayMonth.format(parseDate(iso))
}

/** An API timestamp ("2026-06-15T06:00:00Z") in the viewer's time zone: "15 Jun 2026, 09:00". */
export function formatDateTime(instant: string): string {
  return dateTime.format(new Date(instant))
}

export function isWeekend(iso: string): boolean {
  const day = parseDate(iso).getDay()
  return day === 0 || day === 6
}

/** The ISO date `count` days after `iso`. */
export function addDays(iso: string, count: number): string {
  const date = parseDate(iso)
  date.setDate(date.getDate() + count)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}
