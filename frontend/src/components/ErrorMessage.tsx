import { ApiError } from '../api/client'

/** Shows what the API said went wrong; falls back to a network hint. */
export function ErrorMessage({ error }: { error: unknown }) {
  if (!error) return null
  const message =
    error instanceof ApiError
      ? error.message
      : 'The server could not be reached. Check that the API is running on port 8080.'
  return (
    <p className="form-error" role="alert">
      {message}
    </p>
  )
}
