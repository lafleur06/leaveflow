import { parseDate } from '../lib/format'

const month = new Intl.DateTimeFormat('en-GB', { month: 'short' })

/**
 * A request's dates as a small calendar tile: the month above, the days below
 * ("Dec / 7–9", "Nov–Dec / 30–4"). The row around it gives the full date in words,
 * so the chip is decorative for screen readers.
 */
export function DateChip({ start, end }: { start: string; end: string }) {
  const from = parseDate(start)
  const to = parseDate(end)
  const sameMonth = from.getMonth() === to.getMonth() && from.getFullYear() === to.getFullYear()
  const months = sameMonth ? month.format(from) : `${month.format(from)}–${month.format(to)}`
  const days = start === end ? `${from.getDate()}` : `${from.getDate()}–${to.getDate()}`

  return (
    <span className="date-chip" aria-hidden="true">
      <span className="date-chip-month">{months}</span>
      <span className="date-chip-days">{days}</span>
    </span>
  )
}
