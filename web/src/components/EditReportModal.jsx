import { useState } from 'react'
import { api } from '../api'
import Modal from './Modal'

export default function EditReportModal({ report, onClose, onSaved, onAuthError }) {
  const [description, setDescription] = useState(report.description)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function save() {
    if (!description.trim()) {
      setError('Description is required')
      return
    }
    setBusy(true)
    try {
      await api.updateReport(report.id, description)
      onSaved()
    } catch (err) {
      if (err.status === 401) {
        onAuthError()
        return
      }
      setError('Update failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal title="Edit Report" onClose={onClose}>
      <label>
        Description
        <input
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          autoFocus
        />
      </label>
      {error && <div className="error">{error}</div>}
      <div className="modal-actions">
        <button className="btn btn-ghost" onClick={onClose} disabled={busy}>
          Cancel
        </button>
        <button className="btn btn-primary" onClick={save} disabled={busy}>
          {busy ? 'Saving…' : 'Save'}
        </button>
      </div>
    </Modal>
  )
}
