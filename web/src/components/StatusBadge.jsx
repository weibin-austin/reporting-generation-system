export default function StatusBadge({ status }) {
  const cls =
    status === 'COMPLETED' ? 'ok' : status === 'FAILED' ? 'fail' : 'pending'
  return <span className={`badge ${cls}`}>{status}</span>
}
