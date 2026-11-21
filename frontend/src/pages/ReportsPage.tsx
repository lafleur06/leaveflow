import { useMutation, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import type { CSSProperties, ReactNode } from 'react'
import { api } from '../api/client'
import type { ReportFormat } from '../api/client'
import type { Absence, LeaveType } from '../api/types'
import { ErrorMessage } from '../components/ErrorMessage'
import { addDays, formatDate, isWeekend, leaveTypeLabel, parseDate, todayIso } from '../lib/format'

const WEEKS = 4

export function ReportsPage() {
  return (
    <div className="page">
      <header className="page-header">
        <h1 className="page-title">Reports</h1>
        <p className="page-lead">
          Who is away in the coming weeks, and the numbers behind it as spreadsheets.
        </p>
      </header>
      <AwayCalendar />
      <Downloads />
    </div>
  )
}

/* ---------- who is away: one row per person, one square per day ---------- */

function AwayCalendar() {
  const from = todayIso()
  const to = addDays(from, WEEKS * 7 - 1)
  const dates = Array.from({ length: WEEKS * 7 }, (_, i) => addDays(from, i))

  const absences = useQuery({ queryKey: ['absences', from, to], queryFn: () => api.absences(from, to) })
  const years = [...new Set([from.slice(0, 4), to.slice(0, 4)])].map(Number)
  const holidays = useQuery({
    queryKey: ['holidays', ...years],
    queryFn: async () => (await Promise.all(years.map(api.holidays))).flat(),
  })
  const holidayNames = new Map((holidays.data ?? []).map((holiday) => [holiday.date, holiday.name]))

  // One row per person, in order of who leaves first.
  const people = new Map<number, { name: string; department: string; absences: Absence[] }>()
  for (const absence of absences.data?.rows ?? []) {
    const row = people.get(absence.employeeId) ?? { name: absence.employee, department: absence.department, absences: [] }
    row.absences.push(absence)
    people.set(absence.employeeId, row)
  }

  return (
    <section className="away" aria-labelledby="away-heading">
      <div className="away-heading-row">
        <h2 id="away-heading" className="section-title">
          Who is away, {formatDate(from)} to {formatDate(to)}
        </h2>
        <ul className="away-legend" aria-label="Legend">
          {(['ANNUAL', 'SICK', 'UNPAID'] as LeaveType[]).map((type) => (
            <li key={type}>
              <span className={`away-swatch away-${type.toLowerCase()}`} /> {leaveTypeLabel[type]}
            </li>
          ))}
          <li>
            <span className="away-swatch away-swatch-holiday" /> Public holiday
          </li>
        </ul>
      </div>
      <ErrorMessage error={absences.error} />
      {absences.isSuccess && people.size === 0 && (
        <p className="empty">Nobody has approved leave in the next {WEEKS} weeks.</p>
      )}
      {people.size > 0 && (
        <div className="away-scroll">
          <table className="away-grid" style={{ '--days': dates.length } as CSSProperties}>
            <thead>
              <tr>
                <th scope="col" className="away-person-head">
                  <span className="visually-hidden">Person</span>
                </th>
                {dates.map((date) => (
                  <th key={date} scope="col" className={dayClass(date, holidayNames)} title={holidayNames.get(date)}>
                    <span className="away-weekday">{weekdayLetter(date)}</span>
                    <span className="away-daynum">{parseDate(date).getDate()}</span>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {[...people.entries()].map(([id, person]) => (
                <tr key={id}>
                  <th scope="row" className="away-person">
                    <span className="away-name">{person.name}</span>
                    <span className="away-dept">{person.department}</span>
                  </th>
                  {dates.map((date) => (
                    <td key={date} className={dayClass(date, holidayNames)}>
                      {cellFor(date, person.absences, holidayNames)}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}

function weekdayLetter(iso: string): string {
  return ['S', 'M', 'T', 'W', 'T', 'F', 'S'][parseDate(iso).getDay()]
}

function dayClass(date: string, holidays: Map<string, string>): string {
  if (holidays.has(date)) return 'away-day is-holiday'
  if (isWeekend(date)) return 'away-day is-weekend'
  return 'away-day'
}

/** A filled square on the working days of an absence; weekends and holidays are not leave days. */
function cellFor(date: string, absences: Absence[], holidays: Map<string, string>): ReactNode {
  const absence = absences.find((a) => a.startDate <= date && date <= a.endDate)
  if (!absence || isWeekend(date) || holidays.has(date)) return null
  const label = `${leaveTypeLabel[absence.type]}, ${formatDate(date)}`
  return <span className={`away-tile away-${absence.type.toLowerCase()}`} role="img" aria-label={label} title={label} />
}

/* ---------- downloads ---------- */

function Downloads() {
  const departments = useQuery({ queryKey: ['departments'], queryFn: api.departments })
  const [departmentId, setDepartmentId] = useState('')
  const [year, setYear] = useState(() => new Date().getFullYear())
  const [from, setFrom] = useState(todayIso())
  const [to, setTo] = useState(() => addDays(todayIso(), 30))

  return (
    <section aria-labelledby="downloads-heading">
      <h2 id="downloads-heading" className="section-title">
        Download as a spreadsheet
      </h2>
      <div className="report-cards">
        <ReportCard
          title="Leave balances"
          description="Every current employee: days earned, taken, waiting for approval and free, as of today."
          path="/api/reports/leave-balances"
          params={{ departmentId: departmentId || undefined }}
        >
          <label className="field">
            <span className="field-label">Department</span>
            <select value={departmentId} onChange={(event) => setDepartmentId(event.target.value)}>
              <option value="">All departments</option>
              {departments.data?.content.map((department) => (
                <option key={department.id} value={department.id}>
                  {department.name}
                </option>
              ))}
            </select>
          </label>
        </ReportCard>

        <ReportCard
          title="Leave taken per department"
          description="Approved requests, people and working days, by department and type of leave."
          path="/api/reports/leave-usage"
          params={{ year }}
        >
          <label className="field">
            <span className="field-label">Year</span>
            <input type="number" min={2000} max={2100} value={year}
              onChange={(event) => setYear(Number(event.target.value))} />
          </label>
        </ReportCard>

        <ReportCard
          title="Absences"
          description="Approved leave that overlaps the dates you choose, up to a year."
          path="/api/reports/absences"
          params={{ from, to }}
        >
          <div className="field-row">
            <label className="field">
              <span className="field-label">From</span>
              <input type="date" required value={from} onChange={(event) => setFrom(event.target.value)} />
            </label>
            <label className="field">
              <span className="field-label">To</span>
              <input type="date" required value={to} onChange={(event) => setTo(event.target.value)} />
            </label>
          </div>
        </ReportCard>
      </div>
    </section>
  )
}

function ReportCard(props: {
  title: string
  description: string
  path: string
  params: Record<string, string | number | undefined>
  children: ReactNode
}) {
  const download = useMutation({
    mutationFn: (format: ReportFormat) => api.downloadReport(props.path, props.params, format),
  })
  return (
    <article className="report-card">
      <h3 className="report-title">{props.title}</h3>
      <p className="report-description">{props.description}</p>
      {props.children}
      <ErrorMessage error={download.error} />
      <div className="button-row">
        <button type="button" className="button-primary" disabled={download.isPending}
          onClick={() => download.mutate('xlsx')}>
          Excel
        </button>
        <button type="button" className="button-secondary" disabled={download.isPending}
          onClick={() => download.mutate('csv')}>
          CSV
        </button>
      </div>
    </article>
  )
}
