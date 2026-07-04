import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'

// FE-NEW-03: ~30 files log full API response payloads (including other users' profile
// data returned in list responses) unconditionally. Rather than touch every call site,
// gate console.log itself so it no-ops in production builds — dev/local behavior is
// unchanged, production builds no longer leave response data sitting in the console.
if (!import.meta.env.DEV) {
  console.log = () => {};
}

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
