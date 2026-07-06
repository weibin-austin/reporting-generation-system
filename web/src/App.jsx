import { useState } from 'react'
import { getToken, clearToken } from './api'
import Login from './components/Login'
import Dashboard from './components/Dashboard'

export default function App() {
  const [authed, setAuthed] = useState(!!getToken())

  if (!authed) {
    return <Login onLogin={() => setAuthed(true)} />
  }
  return (
    <Dashboard
      onLogout={() => {
        clearToken()
        setAuthed(false)
      }}
    />
  )
}
