import type { LeaveStatus } from '../api/types'
import { statusLabel } from '../lib/format'

export function StatusBadge({ status }: { status: LeaveStatus }) {
  return <span className={`status-badge status-${status.toLowerCase()}`}>{statusLabel[status]}</span>
}
