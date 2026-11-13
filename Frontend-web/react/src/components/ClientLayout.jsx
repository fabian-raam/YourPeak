import { useRef } from 'react'
import Brand from './Brand'
import Icon from './Icon'
import { clientSections } from '../utils/clientSections'
import { fullName, initials } from '../utils/format'
import './ClientLayout.css'

export default function ClientLayout({ user, section, onNavigate, onLogout, logoutBusy, error, children }) {
  const accountMenu = useRef(null)
  const membershipStatus = user?.membresiaVigente ? 'vigente' : 'no vigente'
  const accountName = fullName(user || {}) || 'Mi cuenta'
  const avatar = initials(user || {}) || 'YP'

  function handleNavigation(event, target) {
    if (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return
    event.preventDefault()
    accountMenu.current?.removeAttribute('open')
    onNavigate(target)
  }

  function link(target, content, className = 'client-nav-link') {
    return <a key={target} href={clientSections[target].path} className={className}
      aria-current={section === target ? 'page' : undefined}
      onClick={event => handleNavigation(event, target)}>{content}</a>
  }

  return <div className="client-layout">
    <aside className="client-sidebar">
      <Brand href="/" onNavigate={event => handleNavigation(event, 'inicio')} />
      <nav aria-label="Área del cliente">
        {link('inicio', <><Icon name="home" /> Inicio</>)}
        <div className="client-nav-group">
          <div className="client-nav-heading"><Icon name="dumbbell" /><span>Entrenamiento</span></div>
          <div className="client-nav-submenu">
            {link('mi-rutina', 'Mi rutina')}
            {link('ejercicios', 'Ejercicios')}
            {link('disponibilidad', <><Icon name="clock" /> Disponibilidad</>)}
          </div>
        </div>
        <button className="client-nav-link client-nav-disabled" type="button" disabled>
          <Icon name="food" /> Nutrición
        </button>
        <button className="client-nav-link client-nav-disabled" type="button" disabled>
          <Icon name="activity" /> Mi progreso
        </button>
        {link('membresia', <><Icon name="card" /> Mi membresía</>)}
      </nav>
      <div className="client-sidebar-status">
        <strong>Membresía {membershipStatus}</strong>
        {user?.fechaVencimiento && <span>Vence el {user.fechaVencimiento}</span>}
      </div>
    </aside>

    <main className="client-workspace">
      <header className="client-shell-header">
        <details className="client-account" ref={accountMenu}
          onBlur={event => {
            if (!event.currentTarget.contains(event.relatedTarget)) event.currentTarget.open = false
          }}
          onKeyDown={event => {
            if (event.key !== 'Escape' || !event.currentTarget.open) return
            event.currentTarget.open = false
            event.currentTarget.querySelector('summary')?.focus()
          }}>
          <summary><span className="client-avatar" aria-hidden="true">{avatar}</span>
            <span className="client-account-name">{user?.nombre || accountName}</span>
            <Icon name="chevron" width="16" height="16" /></summary>
          <div className="client-account-links">
            {link('perfil', 'Mi perfil', 'client-account-link')}
            <button type="button" onClick={onLogout} disabled={logoutBusy}>
              <Icon name="logout" /> {logoutBusy ? 'Cerrando sesión…' : 'Cerrar sesión'}
            </button>
          </div>
        </details>
      </header>
      {error && <p className="client-error" role="alert">{error}</p>}
      {children}
    </main>
  </div>
}
