import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { api } from '../api/client'
import type { LeaveRequest } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { ErrorMessage } from '../components/ErrorMessage'
import { days, formatRange, leaveTypeLabel } from '../lib/format'

export function ApprovalsPage() {
  const { user } = useAuth()
  const waiting = useQuery({
    queryKey: ['approvals', user!.id],
    queryFn: () => api.leaveRequests({ managerId: user!.id, status: 'PENDING', size: 50, sort: 'startDate,asc' }),
  })
  const requests = waiting.data?.content ?? []

  return (
    <div className="page">
      <header className="page-header">
        <h1 className="page-title">Team approvals</h1>
        <p className="page-lead">
          Leave requested by the people who report to you, earliest first. Approved days come off their balance;
          rejected days go back.
        </p>
      </header>
      <ErrorMessage error={waiting.error} />
      {waiting.isSuccess && requests.length === 0 && (
        <p className="empty">Nothing is waiting for your decision.</p>
      )}
      <ul className="request-list">
        {requests.map((request) => (
          <ApprovalRow key={request.id} request={request} />
        ))}
      </ul>
    </div>
  )
}

function ApprovalRow({ request }: { request: LeaveRequest }) {
  const queryClient = useQueryClient()
  const [rejecting, setRejecting] = useState(false)
  const [note, setNote] = useState('')

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['approvals'] })
  const approve = useMutation({ mutationFn: () => api.approve(request.id), onSuccess: refresh })
  const reject = useMutation({ mutationFn: () => api.reject(request.id, note.trim()), onSuccess: refresh })
  const busy = approve.isPending || reject.isPending

  function handleReject(event: FormEvent) {
    event.preventDefault()
    reject.mutate()
  }

  return (
    <li className="request-row approval-row">
      <div className="request-main">
        <p className="request-person">{request.employee.name}</p>
        <p className="request-dates">{formatRange(request.startDate, request.endDate)}</p>
        <p className="request-meta">
          {leaveTypeLabel[request.type]}, {days(request.workingDays)}
        </p>
        {request.reason && <p className="request-note">“{request.reason}”</p>}
        <ErrorMessage error={approve.error ?? reject.error} />

        {rejecting && (
          <form className="reject-form" onSubmit={handleReject}>
            <label className="field">
              <span className="field-label">Why are you rejecting it?</span>
              <textarea
                rows={2}
                required
                maxLength={500}
                autoFocus
                value={note}
                onChange={(event) => setNote(event.target.value)}
              />
            </label>
            <div className="button-row">
              <button type="submit" className="button-danger" disabled={busy || note.trim() === ''}>
                Reject request
              </button>
              <button type="button" className="button-link" onClick={() => setRejecting(false)}>
                Keep it open
              </button>
            </div>
          </form>
        )}
      </div>

      {!rejecting && (
        <div className="request-side button-row">
          <button type="button" className="button-primary" disabled={busy} onClick={() => approve.mutate()}>
            Approve
          </button>
          <button type="button" className="button-secondary" disabled={busy} onClick={() => setRejecting(true)}>
            Reject…
          </button>
        </div>
      )}
    </li>
  )
}
