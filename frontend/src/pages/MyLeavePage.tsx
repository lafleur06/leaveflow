import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError, api } from '../api/client'
import type { LeaveRequest, LeaveType } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { DateChip } from '../components/DateChip'
import { DayStrip } from '../components/DayStrip'
import { ErrorMessage } from '../components/ErrorMessage'
import { LeaveHistory } from '../components/LeaveHistory'
import { StatusBadge } from '../components/StatusBadge'
import { days, formatRange, leaveTypeLabel, todayIso } from '../lib/format'

export function MyLeavePage() {
  const { user } = useAuth()
  const employeeId = user!.id

  const balance = useQuery({ queryKey: ['balance', employeeId], queryFn: () => api.balance(employeeId) })
  const requests = useQuery({
    queryKey: ['my-requests', employeeId],
    queryFn: () => api.leaveRequests({ employeeId, size: 50, sort: 'startDate,desc' }),
  })

  return (
    <div className="page">
      {balance.data ? <DayStrip balance={balance.data} /> : <ErrorMessage error={balance.error} />}

      <div className="page-columns">
        <NewRequestForm />
        <section className="requests" aria-labelledby="requests-heading">
          <h2 id="requests-heading" className="section-title">
            Your requests
          </h2>
          <ErrorMessage error={requests.error} />
          {requests.data?.content.length === 0 && (
            <p className="empty">Nothing requested yet. Your requests and their decisions will appear here.</p>
          )}
          <ul className="request-list">
            {requests.data?.content.map((request) => (
              <MyRequestRow key={request.id} request={request} />
            ))}
          </ul>
        </section>
      </div>
    </div>
  )
}

function NewRequestForm() {
  const queryClient = useQueryClient()
  const [type, setType] = useState<LeaveType>('ANNUAL')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [reason, setReason] = useState('')
  const [requested, setRequested] = useState<LeaveRequest | null>(null)

  const submit = useMutation({
    mutationFn: api.submitLeave,
    onSuccess: (created) => {
      setRequested(created)
      setStartDate('')
      setEndDate('')
      setReason('')
      queryClient.invalidateQueries({ queryKey: ['balance'] })
      queryClient.invalidateQueries({ queryKey: ['my-requests'] })
    },
  })

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setRequested(null)
    submit.mutate({ type, startDate, endDate, reason: reason.trim() || undefined })
  }

  const fieldErrors = submit.error instanceof ApiError ? submit.error.fieldErrors : {}
  // Sick leave may be reported afterwards; the others start today at the earliest.
  const minDate = type === 'SICK' ? undefined : todayIso()

  return (
    <section className="new-request" aria-labelledby="new-request-heading">
      <h2 id="new-request-heading" className="section-title">
        Request leave
      </h2>
      <form className="form" onSubmit={handleSubmit}>
        <label className="field">
          <span className="field-label">Type</span>
          <select value={type} onChange={(event) => setType(event.target.value as LeaveType)}>
            {(Object.keys(leaveTypeLabel) as LeaveType[]).map((value) => (
              <option key={value} value={value}>
                {leaveTypeLabel[value]}
              </option>
            ))}
          </select>
        </label>
        <div className="field-row">
          <label className="field">
            <span className="field-label">First day</span>
            <input
              type="date"
              required
              min={minDate}
              value={startDate}
              onChange={(event) => {
                setStartDate(event.target.value)
                if (!endDate || endDate < event.target.value) setEndDate(event.target.value)
              }}
            />
            {fieldErrors.startDate && <span className="field-error">{fieldErrors.startDate}</span>}
          </label>
          <label className="field">
            <span className="field-label">Last day</span>
            <input
              type="date"
              required
              min={startDate || minDate}
              value={endDate}
              onChange={(event) => setEndDate(event.target.value)}
            />
            {fieldErrors.endDate && <span className="field-error">{fieldErrors.endDate}</span>}
          </label>
        </div>
        <label className="field">
          <span className="field-label">
            Note for your manager <span className="field-optional">(optional)</span>
          </span>
          <textarea rows={3} maxLength={500} value={reason} onChange={(event) => setReason(event.target.value)} />
        </label>
        <p className="field-hint">Weekends and public holidays are not counted.</p>
        <ErrorMessage error={submit.error} />
        {requested && (
          <p className="notice notice-success" role="status">
            Requested {days(requested.workingDays)} of {leaveTypeLabel[requested.type].toLowerCase()} for{' '}
            {formatRange(requested.startDate, requested.endDate)}. Your manager has been asked to approve it.
          </p>
        )}
        <button type="submit" className="button-primary" disabled={submit.isPending}>
          {submit.isPending ? 'Requesting…' : 'Request leave'}
        </button>
      </form>
    </section>
  )
}

function MyRequestRow({ request }: { request: LeaveRequest }) {
  const queryClient = useQueryClient()
  const [showHistory, setShowHistory] = useState(false)
  const cancel = useMutation({
    mutationFn: () => api.cancel(request.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['balance'] })
      queryClient.invalidateQueries({ queryKey: ['my-requests'] })
      queryClient.invalidateQueries({ queryKey: ['history', request.id] })
    },
  })

  const cancellable =
    request.status === 'PENDING' || (request.status === 'APPROVED' && request.startDate > todayIso())

  return (
    <li className={`request-row is-${request.status.toLowerCase()}`}>
      <DateChip start={request.startDate} end={request.endDate} />
      <div className="request-main">
        <p className="request-dates">{formatRange(request.startDate, request.endDate)}</p>
        <p className="request-meta">
          {leaveTypeLabel[request.type]}, {days(request.workingDays)}
        </p>
        {request.decisionNote && (
          <p className="request-note">
            {request.decidedBy?.name}: “{request.decisionNote}”
          </p>
        )}
        <ErrorMessage error={cancel.error} />
        <div className="request-actions">
          <button
            type="button"
            className="button-link history-toggle"
            aria-expanded={showHistory}
            onClick={() => setShowHistory(!showHistory)}
          >
            {showHistory ? 'Hide history' : 'History'}
          </button>
          {cancellable && (
            <button
              type="button"
              className="button-link request-cancel"
              disabled={cancel.isPending}
              onClick={() => {
                if (window.confirm(`Cancel your leave for ${formatRange(request.startDate, request.endDate)}?`)) {
                  cancel.mutate()
                }
              }}
            >
              Cancel request
            </button>
          )}
        </div>
        {showHistory && <LeaveHistory requestId={request.id} />}
      </div>
      <div className="request-side">
        <StatusBadge status={request.status} />
      </div>
    </li>
  )
}
