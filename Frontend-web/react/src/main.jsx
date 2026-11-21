import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './theme.css'
import './index.css'
import App from './App.jsx'

// Reinicia el desplazamiento al recargar la portada. Los enlaces directos conservan su ancla.
const navigation = performance.getEntriesByType('navigation')[0]
if (location.pathname === '/' && navigation?.type === 'reload') {
  const previousRestoration = history.scrollRestoration
  history.scrollRestoration = 'manual'
  if (location.hash) history.replaceState(history.state, '', location.pathname + location.search)
  window.addEventListener('pageshow', () => {
    requestAnimationFrame(() => {
      window.scrollTo({ top: 0, behavior: 'instant' })
      history.scrollRestoration = previousRestoration
    })
  }, { once: true })
}

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
