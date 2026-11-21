import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useDeferredValue, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router'
import { api } from '../api/client'
import type { Department, Employee } from '../api/types'
import { ErrorMessage } from '../components/ErrorMessage'

const byTurkishName = new Intl.Collator('tr', { sensitivity: 'base' })

export function PeoplePage() {
  const [search, setSearch] = useState('')
  const [departmentId, setDepartmentId] = useState<number | ''>('')
  const [includeFormer, setIncludeFormer] = useState(false)
  // Typing does not fire a request per keystroke while the previous answer is still on its way.
  const q = useDeferredValue(search.trim())

  const departments = useQuery({ queryKey: ['departments'], queryFn: api.departments })
  const people = useQuery({
    queryKey: ['people', q, departmentId, includeFormer],
    queryFn: () =>
      api.employees({
        q: q || undefined,
        departmentId: departmentId === '' ? undefined : departmentId,
        active: includeFormer ? undefined : true,
      }),
    placeholderData: (previous) => previous,
  })
  // Sorted here as well: the database collation puts Ç, Ş and Ü after Z; Turkish puts them in place.
  const list = [...(people.data?.content ?? [])].sort((a, b) =>
    byTurkishName.compare(`${a.lastName} ${a.firstName}`, `${b.lastName} ${b.firstName}`),
  )

  return (
    <div className="page">
      <header className="page-header people-header">
        <div>
          <h1 className="page-title">People</h1>
          <p className="page-lead">Everyone who can request leave, who they report to, and whether they can log in.</p>
        </div>
        <Link to="/people/new" className="button-primary people-add">
          Add employee
        </Link>
      </header>

      <div className="page-columns people-columns">
        <section aria-labelledby="people-heading">
          <h2 id="people-heading" className="visually-hidden">
            Employees
          </h2>
          <div className="people-filters">
            <label className="field">
              <span className="field-label">Find by name or email</span>
              <input type="search" value={search} onChange={(event) => setSearch(event.target.value)} />
            </label>
            <label className="field">
              <span className="field-label">Department</span>
              <select
                value={departmentId}
                onChange={(event) => setDepartmentId(event.target.value === '' ? '' : Number(event.target.value))}
              >
                <option value="">All departments</option>
                {departments.data?.content.map((department) => (
                  <option key={department.id} value={department.id}>
                    {department.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="people-check">
              <input
                type="checkbox"
                checked={includeFormer}
                onChange={(event) => setIncludeFormer(event.target.checked)}
              />
              Include people who have left
            </label>
          </div>

          <ErrorMessage error={people.error} />
          {people.isSuccess && list.length === 0 && (
            <p className="empty">Nobody matches. Try another name, or add the person.</p>
          )}
          <ul className="people-list">
            {list.map((person) => (
              <PersonRow key={person.id} person={person} />
            ))}
          </ul>
          {people.data && people.data.page.totalElements > list.length && (
            <p className="field-hint">
              Showing {list.length} of {people.data.page.totalElements}. Narrow the search to find someone.
            </p>
          )}
        </section>

        <DepartmentsPanel departments={departments.data?.content ?? []} error={departments.error} />
      </div>
    </div>
  )
}

function PersonRow({ person }: { person: Employee }) {
  return (
    <li>
      <Link to={`/people/${person.id}`} className={`people-row${person.active ? '' : ' is-former'}`}>
        <span className="people-name">
          {person.firstName} {person.lastName}
        </span>
        <span className="people-role">
          {person.jobTitle ? `${person.jobTitle}, ${person.department.name}` : person.department.name}
        </span>
        <span className="people-manager">
          {person.manager ? `Reports to ${person.manager.name}` : 'No manager; HR approves their leave'}
        </span>
        <span className="people-flags">
          {person.role === 'HR' && <span className="people-flag flag-hr">HR</span>}
          {!person.active && <span className="people-flag flag-former">Left</span>}
          {person.active && !person.hasPassword && <span className="people-flag flag-nologin">No password yet</span>}
        </span>
      </Link>
    </li>
  )
}

function DepartmentsPanel({ departments, error }: { departments: Department[]; error: unknown }) {
  const queryClient = useQueryClient()
  const [name, setName] = useState('')
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['departments'] })

  const create = useMutation({
    mutationFn: () => api.createDepartment(name.trim()),
    onSuccess: () => {
      setName('')
      refresh()
    },
  })
  const remove = useMutation({ mutationFn: api.deleteDepartment, onSuccess: refresh })

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    create.mutate()
  }

  return (
    <aside className="people-departments" aria-labelledby="departments-heading">
      <h2 id="departments-heading" className="section-title">
        Departments
      </h2>
      <ErrorMessage error={error ?? remove.error} />
      <ul className="people-department-list">
        {departments.map((department) => (
          <li key={department.id}>
            <span>{department.name}</span>
            <button
              type="button"
              className="button-link"
              disabled={remove.isPending}
              aria-label={`Remove ${department.name}`}
              onClick={() => {
                if (window.confirm(`Remove the ${department.name} department?`)) remove.mutate(department.id)
              }}
            >
              Remove
            </button>
          </li>
        ))}
      </ul>
      <p className="field-hint">A department can only be removed once nobody works in it.</p>
      <form className="form" onSubmit={handleSubmit}>
        <label className="field">
          <span className="field-label">New department</span>
          <input required maxLength={100} value={name} onChange={(event) => setName(event.target.value)} />
        </label>
        <ErrorMessage error={create.error} />
        <button type="submit" className="button-secondary" disabled={create.isPending || name.trim() === ''}>
          Add department
        </button>
      </form>
    </aside>
  )
}
