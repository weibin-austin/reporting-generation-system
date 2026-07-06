# Reporting System — Web UI

A modern React (Vite) single-page app for the reporting generation system.
It talks to the ClientService REST API (`/auth`, `/report`).

## Develop

```bash
npm install
npm run dev        # http://localhost:5173
```

The dev server proxies `/auth` and `/report` to `http://localhost:8080`
(override with `VITE_API_TARGET`), so no CORS setup is needed locally.
Make sure the ClientService (and Excel/PDF services) are running first.

Demo login: **admin / password**.

## Build

```bash
npm run build      # outputs static assets to dist/
```

For a decoupled deployment, serve `dist/` from any static host and point it
at the API origin via `VITE_API_BASE` at build time; the ClientService
enables CORS for the origins in `app.cors.allowed-origins`.
