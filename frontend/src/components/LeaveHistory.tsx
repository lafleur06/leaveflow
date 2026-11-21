import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import type { LeaveAction } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { formatDateTime } from '../lib/format'
import { ErrorMessage } from './ErrorMessage'

const verb: Record<LeaveAction, string> = {
  SUBMITTED: 'Requested',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  CANCELLED: 'Cancelled',
}

/** The audit trail of one request, oldest first, as a short timeline. */
export function LeaveHistory({ requestId }: { requestId: number }) {
  const { user } = useAuth()
  const history = useQuery({ queryKey: ['history', requestId], queryFn: () => api.history(requestId) })

  if (history.error) return <ErrorMessage error={history.error} />
  if (!history.data) return <p className="muted">Loading the history…</p>

  return (
    <ol className="history-list">
      {history.data.map((event, index) => (
        <li key={index} className={`history-step history-${event.action.toLowerCase()}`}>
          <span className="history-what">
            {verb[event.action]}
            {event.actor && ` by ${event.actor.id === user?.id ? 'you' : event.actor.name}`}
          </span>
          <time className="history-when" dateTime={event.occurredAt}>
            {formatDateTime(event.occurredAt)}
          </time>
          {event.note && <span className="history-note">“{event.note}”</span>}
        </li>
      ))}
    </ol>
  )
}
