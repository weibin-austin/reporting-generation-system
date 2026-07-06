import StatusBadge from './StatusBadge'

function formatTime(t) {
  if (!t) return '—'
  return new Date(t).toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export default function ReportTable({ reports, onDownload, onEdit, onDelete }) {
  if (reports.length === 0) {
    return (
      <div className="card empty">
        <div className="empty-mark">📄</div>
        <p>No reports yet.</p>
        <p className="muted">Create your first report to see it here.</p>
      </div>
    )
  }

  async function download(reqId, type) {
    try {
      const { blob, fileName } = await onDownload(reqId, type)
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = fileName
      a.click()
      URL.revokeObjectURL(url)
    } catch {
      alert('Download failed')
    }
  }

  return (
    <div className="card table-card">
      <table className="table">
        <thead>
          <tr>
            <th>Submitter</th>
            <th>Description</th>
            <th>Created</th>
            <th>PDF</th>
            <th>Excel</th>
            <th>Image</th>
            <th className="right">Actions</th>
          </tr>
        </thead>
        <tbody>
          {reports.map((r) => (
            <tr key={r.id}>
              <td className="strong">{r.submitter}</td>
              <td>{r.description}</td>
              <td className="muted nowrap">{formatTime(r.createdTime)}</td>
              <td>
                <StatusBadge status={r.pdfReportStatus} />
              </td>
              <td>
                <StatusBadge status={r.excelReportStatus} />
              </td>
              <td>
                <StatusBadge status={r.imageReportStatus} />
              </td>
              <td className="right actions">
                <button
                  className="link"
                  disabled={r.pdfReportStatus !== 'COMPLETED'}
                  onClick={() => download(r.id, 'PDF')}
                >
                  PDF
                </button>
                <button
                  className="link"
                  disabled={r.excelReportStatus !== 'COMPLETED'}
                  onClick={() => download(r.id, 'EXCEL')}
                >
                  Excel
                </button>
                <button
                  className="link"
                  disabled={r.imageReportStatus !== 'COMPLETED'}
                  onClick={() => download(r.id, 'IMAGE')}
                >
                  PNG
                </button>
                <button className="link" onClick={() => onEdit(r)}>
                  Edit
                </button>
                <button className="link danger" onClick={() => onDelete(r)}>
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
