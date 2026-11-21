/** Wordmark with the four-tile mark: two days taken, one pending, one free. */
export function Logo() {
  return (
    <p className="logo">
      <svg className="logo-mark" viewBox="0 0 32 32" aria-hidden="true">
        <rect x="2" y="2" width="12" height="12" rx="2" fill="var(--cobalt)" />
        <rect x="18" y="2" width="12" height="12" rx="2" fill="var(--cobalt)" />
        <rect x="2" y="18" width="12" height="12" rx="2" fill="var(--mustard)" />
        <rect x="19" y="19" width="10" height="10" rx="1.5" fill="none" stroke="var(--turquoise)" strokeWidth="2" />
      </svg>
      LeaveFlow
    </p>
  )
}
