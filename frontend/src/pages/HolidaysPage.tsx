import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { api } from '../api/client'
import { ErrorMessage } from '../components/ErrorMessage'
import { formatWeekday, isWeekend } from '../lib/format'

export function HolidaysPage() {
  const queryClient = useQueryClient()
  const [year, setYear] = useState(() => new Date().getFullYear())
  const [date, setDate] = useState('')
  const [name, setName] = useState('')

  const holidays = useQuery({ queryKey: ['holidays', year], queryFn: () => api.holidays(year) })
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['holidays'] })
  const add = useMutation({
    mutationFn: () => api.addHoliday(date, name.trim()),
    onSuccess: (created) => {
      setDate('')
      setName('')
      setYear(Number(created.date.slice(0, 4)))
      refresh()
    },
  })
  const remove = useMutation({ mutationFn: api.deleteHoliday, onSuccess: refresh })

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    add.mutate()
  }

  const list = holidays.data ?? []
  const onWeekdays = list.filter((holiday) => !isWeekend(holiday.date)).length

  return (
    <div className="page">
      <header className="page-header">
        <h1 className="page-title">Public holidays</h1>
        <p className="page-lead">
          These days are not counted as leave. A request is counted when it is made, so changing a holiday here does not
          change requests that already exist.
        </p>
      </header>

      <div className="page-columns holiday-columns">
        <section aria-labelledby="holiday-year">
          <div className="holiday-year">
            <button type="button" className="button-secondary" aria-label="Previous year"
              onClick={() => setYear(year - 1)}>
              ‹
            </button>
            <h2 id="holiday-year" className="holiday-year-label">
              {year}
            </h2>
            <button type="button" className="button-secondary" aria-label="Next year"
              onClick={() => setYear(year + 1)}>
              ›
            </button>
          </div>
          <ErrorMessage error={holidays.error ?? remove.error} />
          {holidays.isSuccess && list.length === 0 && (
            <p className="empty">No public holidays entered for {year} yet. Add them on the right.</p>
          )}
          {list.length > 0 && (
            <p className="field-hint">
              {list.length} holidays, {onWeekdays} of them on a weekday.
            </p>
          )}
          <ul className="holiday-list">
            {list.map((holiday) => (
              <li key={holiday.id} className={`holiday-row${isWeekend(holiday.date) ? ' is-weekend' : ''}`}>
                <span className="holiday-date">{formatWeekday(holiday.date)}</span>
                <span className="holiday-name">
                  {holiday.name}
                  {isWeekend(holiday.date) && <span className="holiday-weekend">Falls on a weekend</span>}
                </span>
                <button
                  type="button"
                  className="button-link"
                  disabled={remove.isPending}
                  aria-label={`Remove ${holiday.name}`}
                  onClick={() => {
                    if (window.confirm(`Remove ${holiday.name}?`)) remove.mutate(holiday.id)
                  }}
                >
                  Remove
                </button>
              </li>
            ))}
          </ul>
        </section>

        <section className="new-request" aria-labelledby="add-holiday">
          <h2 id="add-holiday" className="section-title">
            Add a holiday
          </h2>
          <form className="form" onSubmit={handleSubmit}>
            <label className="field">
              <span className="field-label">Date</span>
              <input type="date" required value={date} onChange={(event) => setDate(event.target.value)} />
            </label>
            <label className="field">
              <span className="field-label">Name</span>
              <input required maxLength={100} value={name} onChange={(event) => setName(event.target.value)} />
            </label>
            <p className="field-hint">
              Religious holidays move every year; add them once the official dates are announced. Add each day of a
              multi-day holiday separately.
            </p>
            <ErrorMessage error={add.error} />
            <button type="submit" className="button-primary" disabled={add.isPending}>
              Add holiday
            </button>
          </form>
        </section>
      </div>
    </div>
  )
}
