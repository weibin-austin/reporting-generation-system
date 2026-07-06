// Thin API client for the ClientService. In dev, paths are relative and the
// Vite proxy forwards them to the backend; set VITE_API_BASE for a decoupled
// deployment against a different origin.
const API_BASE = import.meta.env.VITE_API_BASE || ''
const TOKEN_KEY = 'reporting_jwt'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}
export function setToken(t) {
  localStorage.setItem(TOKEN_KEY, t)
}
export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export class ApiError extends Error {
  constructor(message, status) {
    super(message)
    this.status = status
  }
}

async function request(path, { method = 'GET', body, raw = false } = {}) {
  const token = getToken()
  const res = await fetch(API_BASE + path, {
    method,
    headers: {
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: 'Bearer ' + token } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  })
  if (res.status === 401) {
    clearToken()
    throw new ApiError('Unauthorized', 401)
  }
  if (!res.ok) {
    let message = 'Request failed'
    try {
      const data = await res.json()
      message = data.message || message
    } catch {
      /* no JSON body */
    }
    throw new ApiError(message, res.status)
  }
  if (raw) return res
  const text = await res.text()
  return text ? JSON.parse(text) : null
}

export const api = {
  login: (username, password) =>
    request('/auth/login', { method: 'POST', body: { username, password } }),
  listReports: () => request('/report'),
  createReport: (payload, isAsync) =>
    request(isAsync ? '/report/async' : '/report/sync', { method: 'POST', body: payload }),
  updateReport: (reqId, description) =>
    request(`/report/${reqId}`, { method: 'PUT', body: { description } }),
  deleteReport: (reqId) => request(`/report/${reqId}`, { method: 'DELETE' }),
  downloadFile: async (reqId, type) => {
    const res = await request(`/report/content/${reqId}/${type}`, { raw: true })
    const blob = await res.blob()
    const fileName =
      res.headers.get('fileName') || `report.${type === 'PDF' ? 'pdf' : 'xls'}`
    return { blob, fileName }
  },
}
