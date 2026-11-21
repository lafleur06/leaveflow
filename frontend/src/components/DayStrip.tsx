import type { CSSProperties } from 'react'
import type { LeaveBalance } from '../api/types'
import { days, formatDate } from '../lib/format'

/**
 * The annual balance drawn as one tile per earned day: taken days filled, days waiting
 * for approval hatched, free days open. Reads at a glance, and the numbers sit beside it.
 */
export function DayStrip({ balance }: { balance: LeaveBalance }) {
  const { earnedDays, usedDays, pendingDays, availableDays, nextAccrualDate, nextAccrualDays } = balance

  if (earnedDays === 0) {
    return (
      <section className="balance" aria-labelledby="balance-heading">
        <h1 id="balance-heading" className="balance-headline">
          No annual leave yet
        </h1>
        <p className="balance-next">
          You earn your first {days(nextAccrualDays)} on {formatDate(nextAccrualDate)}, one year after you started.
          Sick and unpaid leave are available now.
        </p>
      </section>
    )
  }

  // Unused leave carries over, so long-serving employees can hold hundreds of days.
  // Past two months' worth, one square stands for five days to keep the strip to a few rows.
  const daysPerTile = earnedDays > 60 ? 5 : 1
  const tileCount = (count: number) => (count === 0 ? 0 : Math.max(1, Math.round(count / daysPerTile)))
  const tiles = [
    ...Array<string>(tileCount(usedDays)).fill('tile-used'),
    ...Array<string>(tileCount(pendingDays)).fill('tile-pending'),
    ...Array<string>(tileCount(availableDays)).fill('tile-free'),
  ]

  return (
    <section className="balance" aria-labelledby="balance-heading">
      <h1 id="balance-heading" className="balance-headline">
        <span className="balance-number">{availableDays}</span>
        {availableDays === 1 ? ' day of annual leave left' : ' days of annual leave left'}
      </h1>

      <ol className="day-strip" aria-hidden="true">
        {tiles.map((kind, index) => (
          <li key={index} className={`day-tile ${kind}`} style={{ '--i': index } as CSSProperties} />
        ))}
      </ol>

      <dl className="balance-legend">
        <div>
          <dt>
            <span className="legend-swatch tile-used" /> Taken
          </dt>
          <dd>{days(usedDays)}</dd>
        </div>
        <div>
          <dt>
            <span className="legend-swatch tile-pending" /> Waiting for approval
          </dt>
          <dd>{days(pendingDays)}</dd>
        </div>
        <div>
          <dt>
            <span className="legend-swatch tile-free" /> Free to book
          </dt>
          <dd>{days(availableDays)}</dd>
        </div>
      </dl>

      <p className="balance-next">
        {days(earnedDays)} earned over {balance.completedServiceYears} years of service. Next{' '}
        {days(nextAccrualDays)} arrive on {formatDate(nextAccrualDate)}.
        {daysPerTile > 1 && ` Each square stands for ${daysPerTile} days.`}
      </p>
    </section>
  )
}
