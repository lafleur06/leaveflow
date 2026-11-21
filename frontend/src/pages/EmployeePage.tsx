import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router'
import { ApiError, api } from '../api/client'
import type { Employee, EmployeeInput } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { ErrorMessage } from '../components/ErrorMessage'
import { days, formatDate } from '../lib/format'

const byTurkishName = new Intl.Collator('tr', { sensitivity: 'base' })

/** /people/new and /people/:id: the same form, plus account and leave panels for an existing person. */
export function EmployeePage() {
  const { id } = useParams()
  const employeeId = id === undefined || id === 'new' ? null : Number(id)

  const employee = useQuery({
    queryKey: ['employee', employeeId],
    queryFn: () => api.employee(employeeId!),
    enabled: employeeId !== null,
  })

  if (employeeId !== null && !employee.data) {
    return (
      <div className="page">
        <BackLink />
        {employee.error ? <ErrorMessage error={employee.error} /> : <p className="muted">Loading…</p>}
      </div>
    )
  }

  const person = employee.data ?? null
  return (
    <div className="page">
      <BackLink />
      <header className="page-header">
        <h1 className="page-title">{person ? `${person.firstName} ${person.lastName}` : 'Add an employee'}</h1>
        {person && !person.active && (
          <p className="notice">{person.firstName} has left the company. The record is kept for the leave history.</p>
        )}
      </header>
      <div className="page-columns people-edit">
        <EmployeeForm key={person?.id ?? 'new'} person={person} />
        {person && (
          <div className="people-side">
            <BalancePanel person={person} />
            <AccessPanel person={person} />
            {person.active && <LeavingPanel person={person} />}
          </div>
        )}
      </div>
    </div>
  )
}

function BackLink() {
  return (
    <Link to="/people" className="people-back">
      ← All people
    </Link>
  )
}

function toInput(person: Employee | null): EmployeeInput {
  return {
    firstName: person?.firstName ?? '',
    lastName: person?.lastName ?? '',
    email: person?.email ?? '',
    jobTitle: person?.jobTitle ?? '',
    hireDate: person?.hireDate ?? '',
    birthDate: person?.birthDate ?? '',
    departmentId: person?.department.id ?? 0,
    managerId: person?.manager?.id ?? null,
  }
}

function EmployeeForm({ person }: { person: Employee | null }) {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const location = useLocation()
  const [form, setForm] = useState<EmployeeInput>(() => toInput(person))
  const [saved, setSaved] = useState(false)
  const justCreated = (location.state as { created?: boolean } | null)?.created === true

  const departments = useQuery({ queryKey: ['departments'], queryFn: api.departments })
  const managers = useQuery({ queryKey: ['people', 'managers'], queryFn: () => api.employees({ active: true }) })

  const save = useMutation({
    mutationFn: (body: EmployeeInput) => (person ? api.updateEmployee(person.id, body) : api.createEmployee(body)),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ['people'] })
      queryClient.setQueryData(['employee', result.id], result)
      if (person) {
        setSaved(true)
      } else {
        navigate(`/people/${result.id}`, { state: { created: true } })
      }
    },
  })

  function update<K extends keyof EmployeeInput>(key: K, value: EmployeeInput[K]) {
    setSaved(false)
    setForm((current) => ({ ...current, [key]: value }))
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    save.mutate({ ...form, jobTitle: form.jobTitle?.trim() || null })
  }

  const fieldErrors = save.error instanceof ApiError ? save.error.fieldErrors : {}
  const managerOptions = (managers.data?.content ?? [])
    .filter((candidate) => candidate.id !== person?.id)
    .sort((a, b) => byTurkishName.compare(`${a.firstName} ${a.lastName}`, `${b.firstName} ${b.lastName}`))

  return (
    <section aria-labelledby="profile-heading">
      <h2 id="profile-heading" className="section-title">
        Profile
      </h2>
      {justCreated && !saved && !person?.hasPassword && (
        <p className="notice notice-success" role="status">
          Added. Set a password below so they can log in.
        </p>
      )}
      <form className="form" onSubmit={handleSubmit}>
        <div className="field-row">
          <TextField label="First name" value={form.firstName} error={fieldErrors.firstName} maxLength={50}
            onChange={(value) => update('firstName', value)} />
          <TextField label="Last name" value={form.lastName} error={fieldErrors.lastName} maxLength={50}
            onChange={(value) => update('lastName', value)} />
        </div>
        <TextField label="Work email" type="email" value={form.email} error={fieldErrors.email} maxLength={150}
          onChange={(value) => update('email', value)} />
        <TextField label="Job title" optional value={form.jobTitle ?? ''} error={fieldErrors.jobTitle} maxLength={100}
          onChange={(value) => update('jobTitle', value)} />
        <div className="field-row">
          <TextField label="Started on" type="date" value={form.hireDate} error={fieldErrors.hireDate}
            onChange={(value) => update('hireDate', value)} />
          <TextField label="Date of birth" type="date" value={form.birthDate} error={fieldErrors.birthDate}
            onChange={(value) => update('birthDate', value)} />
        </div>
        <p className="field-hint">
          Both dates set the annual leave: years of service, and the 20-day minimum for people aged 18 or younger
          or 50 and over.
        </p>
        <div className="field-row">
          <label className="field">
            <span className="field-label">Department</span>
            <select
              required
              value={form.departmentId || ''}
              onChange={(event) => update('departmentId', Number(event.target.value))}
            >
              <option value="" disabled>
                Choose…
              </option>
              {departments.data?.content.map((department) => (
                <option key={department.id} value={department.id}>
                  {department.name}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            <span className="field-label">Manager</span>
            <select
              value={form.managerId ?? ''}
              onChange={(event) => update('managerId', event.target.value === '' ? null : Number(event.target.value))}
            >
              <option value="">Nobody (HR approves their leave)</option>
              {managerOptions.map((candidate) => (
                <option key={candidate.id} value={candidate.id}>
                  {candidate.firstName} {candidate.lastName}
                </option>
              ))}
            </select>
          </label>
        </div>
        <ErrorMessage error={save.error} />
        {saved && (
          <p className="notice notice-success" role="status">
            Changes saved.
          </p>
        )}
        <button type="submit" className="button-primary" disabled={save.isPending}>
          {person ? 'Save changes' : 'Add employee'}
        </button>
      </form>
    </section>
  )
}

function TextField(props: {
  label: string
  value: string
  onChange: (value: string) => void
  error?: string
  type?: string
  maxLength?: number
  optional?: boolean
}) {
  return (
    <label className="field">
      <span className="field-label">
        {props.label} {props.optional && <span className="field-optional">(optional)</span>}
      </span>
      <input
        type={props.type ?? 'text'}
        required={!props.optional}
        maxLength={props.maxLength}
        value={props.value}
        onChange={(event) => props.onChange(event.target.value)}
      />
      {props.error && <span className="field-error">{props.error}</span>}
    </label>
  )
}

function BalancePanel({ person }: { person: Employee }) {
  const balance = useQuery({ queryKey: ['balance', person.id], queryFn: () => api.balance(person.id) })
  const b = balance.data
  return (
    <section className="people-panel" aria-labelledby="balance-heading">
      <h2 id="balance-heading" className="section-title">
        Annual leave
      </h2>
      <ErrorMessage error={balance.error} />
      {b && (
        <>
          <p className="people-balance">
            <span className="people-balance-number">{b.availableDays}</span> of {days(b.earnedDays)} free to book
          </p>
          <p className="field-hint">
            {days(b.usedDays)} taken, {days(b.pendingDays)} waiting for approval. Next {days(b.nextAccrualDays)} on{' '}
            {formatDate(b.nextAccrualDate)}.
          </p>
        </>
      )}
    </section>
  )
}

function AccessPanel({ person }: { person: Employee }) {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const [password, setPassword] = useState('')
  const [passwordSet, setPasswordSet] = useState(false)
  const isSelf = user?.id === person.id

  const refresh = (updated?: Employee) => {
    if (updated) queryClient.setQueryData(['employee', person.id], updated)
    queryClient.invalidateQueries({ queryKey: ['employee', person.id] })
    queryClient.invalidateQueries({ queryKey: ['people'] })
  }
  const role = useMutation({
    mutationFn: () => api.changeRole(person.id, person.role === 'HR' ? 'EMPLOYEE' : 'HR'),
    onSuccess: refresh,
  })
  const setNewPassword = useMutation({
    mutationFn: () => api.setPassword(person.id, password),
    onSuccess: () => {
      setPassword('')
      setPasswordSet(true)
      refresh()
    },
  })

  function handlePassword(event: FormEvent) {
    event.preventDefault()
    setPasswordSet(false)
    setNewPassword.mutate()
  }

  return (
    <section className="people-panel" aria-labelledby="access-heading">
      <h2 id="access-heading" className="section-title">
        Access
      </h2>
      <p>
        {person.role === 'HR'
          ? 'HR: manages people, holidays and reports, and decides for people without a manager.'
          : 'Employee: requests their own leave, and approves their team’s if anyone reports to them.'}
      </p>
      {isSelf ? (
        <p className="field-hint">You cannot change your own role; another HR colleague can.</p>
      ) : (
        <button type="button" className="button-secondary" disabled={role.isPending || !person.active}
          onClick={() => role.mutate()}>
          {person.role === 'HR' ? 'Remove HR rights' : 'Give HR rights'}
        </button>
      )}
      <ErrorMessage error={role.error} />

      {person.active && (
        <form className="form" onSubmit={handlePassword}>
          <label className="field">
            <span className="field-label">{person.hasPassword ? 'Reset password' : 'Set a first password'}</span>
            <input
              type="password"
              autoComplete="new-password"
              minLength={8}
              maxLength={72}
              required
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />
          </label>
          <p className="field-hint">At least 8 characters. Tell them in person; they can change it after logging in.</p>
          <ErrorMessage error={setNewPassword.error} />
          {passwordSet && (
            <p className="notice notice-success" role="status">
              Password set. {person.firstName} can log in now.
            </p>
          )}
          <button type="submit" className="button-secondary" disabled={setNewPassword.isPending}>
            {person.hasPassword ? 'Reset password' : 'Set password'}
          </button>
        </form>
      )}
    </section>
  )
}

function LeavingPanel({ person }: { person: Employee }) {
  const queryClient = useQueryClient()
  const deactivate = useMutation({
    mutationFn: () => api.deactivateEmployee(person.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['employee', person.id] })
      queryClient.invalidateQueries({ queryKey: ['people'] })
    },
  })

  return (
    <section className="people-panel" aria-labelledby="leaving-heading">
      <h2 id="leaving-heading" className="section-title">
        Leaving the company
      </h2>
      <p className="field-hint">
        They can no longer log in or request leave. Their history stays. Anyone reporting to them needs a new manager
        first.
      </p>
      <ErrorMessage error={deactivate.error} />
      <button
        type="button"
        className="button-danger"
        disabled={deactivate.isPending}
        onClick={() => {
          if (window.confirm(`Mark ${person.firstName} ${person.lastName} as having left?`)) deactivate.mutate()
        }}
      >
        Mark as left
      </button>
    </section>
  )
}
