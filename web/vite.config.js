import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// In dev, proxy API calls to the ClientService so the SPA is same-origin
// (no CORS needed). Override the backend with VITE_API_TARGET if needed.
const apiTarget = process.env.VITE_API_TARGET || 'http://localhost:8080'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/auth': apiTarget,
      '/report': apiTarget,
    },
  },
})
