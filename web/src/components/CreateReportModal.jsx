import { useState } from 'react'
import { api } from '../api'
import Modal from './Modal'

const EXAMPLE = `Student #, Name, Class, Score
s-008, Sarah, Class-A, B
s-009, Thomas, Class-A, B-
s-010, Joseph, Class-B, A-`

export default function CreateReportModal({ onClose, onCreated, onAuthError }) {
  const [description, setDescription] = useState('')
  const [submitter, setSubmitter] = useState('')
  const [csv, setCsv] = useState(EXAMPLE)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  function parse() {
    const lines = csv
      .split('\n')
      .map((l) => l.trim())
      .filter(Boolean)
    if (lines.length < 2) {
      throw new Error('Provide a header row and at least one data row')
    }
    const headers = lines[0].split(',').map((s) => s.trim())
    const data = lines.slice(1).map((l) => l.split(',').map((s) => s.trim()))
    return { headers, data }
  }

  async function submit(isAsync) {
    setError('')
    if (!description.trim() || !submitter.trim()) {
      setError('Description and submitter are required')
      return
    }
    let parsed
    try {
      parsed = parse()
    } catch (e) {
      setError(e.message)
      return
    }
    setBusy(true)
    try {
      await api.createReport({ description, submitter, ...parsed }, isAsync)
      onCreated()
    } catch (err) {
      if (err.status === 401) {
        onAuthError()
        return
      }
      setError(err.message || 'Create failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal title="New Report" onClose={onClose}>
      <label>
        Description
        <input
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Student Math Course Report"
          autoFocus
        />
      </label>
      <label>
        Submitter
        <input
          value={submitter}
          onChange={(e) => setSubmitter(e.target.value)}
          placeholder="Mrs. York"
        />
      </label>
      <label>
        Data <span className="muted">— first line is the header, comma-separated</span>
        <textarea
          rows={7}
          value={csv}
          onChange={(e) => setCsv(e.target.value)}
          spellCheck={false}
        />
      </label>
      {error && <div className="error">{error}</div>}
      <div className="modal-actions">
        <button className="btn btn-ghost" onClick={onClose} disabled={busy}>
          Cancel
        </button>
        <button className="btn" onClick={() => submit(true)} disabled={busy}>
          Create (Async)
        </button>
        <button className="btn btn-primary" onClick={() => submit(false)} disabled={busy}>
          {busy ? 'Creating…' : 'Create (Sync)'}
        </button>
      </div>
    </Modal>
  )
}
