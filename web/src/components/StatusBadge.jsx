export default function StatusBadge({ status }) {
  if (!status) return <span className="muted">—</span>
  const cls =
    status === 'COMPLETED' ? 'ok' : status === 'FAILED' ? 'fail' : 'pending'
  return <span className={`badge ${cls}`}>{status}</span>
}
