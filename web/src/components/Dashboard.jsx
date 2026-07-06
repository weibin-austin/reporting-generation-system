import { useCallback, useEffect, useState } from 'react'
import { api, ApiError } from '../api'
import ReportTable from './ReportTable'
import CreateReportModal from './CreateReportModal'
import EditReportModal from './EditReportModal'

export default function Dashboard({ onLogout }) {
  const [reports, setReports] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showCreate, setShowCreate] = useState(false)
  const [editing, setEditing] = useState(null)

  const load = useCallback(async () => {
    try {
      const res = await api.listReports()
      setReports(res.data || [])
      setError('')
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onLogout()
        return
      }
      setError('Could not load reports')
    } finally {
      setLoading(false)
    }
  }, [onLogout])

  useEffect(() => {
    load()
    // poll so async report statuses (PENDING -> COMPLETED) update on their own
    const id = setInterval(load, 4000)
    return () => clearInterval(id)
  }, [load])

  async function handleDelete(report) {
    if (!window.confirm(`Delete report "${report.description}"?`)) return
    try {
      await api.deleteReport(report.id)
      load()
    } catch (err) {
      if (err.status === 401) onLogout()
      else alert('Delete failed')
    }
  }

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">
          <span className="brand-mark">◆</span> Reporting System
        </div>
        <div className="topbar-actions">
          <button className="btn btn-ghost" onClick={load}>
            Refresh
          </button>
          <button className="btn btn-primary" onClick={() => setShowCreate(true)}>
            + New Report
          </button>
          <button className="btn btn-ghost" onClick={onLogout}>
            Sign out
          </button>
        </div>
      </header>

      <main className="content">
        <div className="page-head">
          <h1>Reports</h1>
          <span className="pill-count">{reports.length}</span>
        </div>

        {error && <div className="banner error">{error}</div>}

        {loading ? (
          <div className="muted center pad">Loading…</div>
        ) : (
          <ReportTable
            reports={reports}
            onDownload={api.downloadFile}
            onEdit={setEditing}
            onDelete={handleDelete}
          />
        )}
      </main>

      {showCreate && (
        <CreateReportModal
          onClose={() => setShowCreate(false)}
          onCreated={() => {
            setShowCreate(false)
            load()
          }}
          onAuthError={onLogout}
        />
      )}
      {editing && (
        <EditReportModal
          report={editing}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null)
            load()
          }}
          onAuthError={onLogout}
        />
      )}
    </div>
  )
}
