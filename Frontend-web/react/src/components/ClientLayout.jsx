import { useEffect, useRef, useState } from 'react'
import Brand from './Brand'
import Icon from './Icon'
import { clientSections } from '../utils/clientSections'
import { fullName, initials, membershipDate } from '../utils/format'
import './ClientLayout.css'

const dateFormat = new Intl.DateTimeFormat('es-PE', { dateStyle: 'full', timeZone: 'America/Lima' })

export default function ClientLayout({ user, section, onNavigate, onLogout, logoutBusy, error, children }) {
  const accountMenu = useRef(null)
  const trainingMenu = useRef(null)
  const heading = useRef(null)
  const [today] = useState(() => dateFormat.format(new Date()))
  const { label, description } = clientSections[section]
  const title = section === 'inicio' && user?.nombre ? `Hola, ${user.nombre}` : label
  const subtitle = section === 'inicio' ? today : description
  const membershipStatus = user?.membresiaVigente ? 'vigente' : 'no vigente'
  const expiration = user?.membresia?.fechaVencimiento
  const expiresLabel = expiration && `Vence el ${membershipDate(expiration)}`
  const isTraining = ['mi-rutina', 'ejercicios', 'disponibilidad'].includes(section)
  const accountName = fullName(user || {}) || 'Mi cuenta'
  const avatar = initials(user || {}) || 'YP'

  useEffect(() => {
    heading.current?.focus({ preventScroll: true })
  }, [section])

  function handleNavigation(event, target) {
    if (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return
    event.preventDefault()
    accountMenu.current?.removeAttribute('open')
    trainingMenu.current?.removeAttribute('open')
    onNavigate(target)
  }

  function link(target, content, className = 'client-nav-link') {
    return <a key={target} href={clientSections[target].path} className={className}
      aria-current={section === target ? 'page' : undefined}
      onClick={event => handleNavigation(event, target)}>{content}</a>
  }

  function trainingLinks() {
    return <>
      {link('mi-rutina', 'Mi rutina')}
      {link('ejercicios', 'Ejercicios')}
      {link('disponibilidad', 'Disponibilidad')}
    </>
  }

  return <div className="client-layout">
    <aside className="client-sidebar">
      <Brand href="/" onNavigate={event => handleNavigation(event, 'inicio')} />
      <nav aria-label="Área del cliente">
        {link('inicio', <><Icon name="home" /> Inicio</>)}
        <div className="client-nav-group">
          <div className="client-nav-heading"><Icon name="dumbbell" /><span>Entrenamiento</span></div>
          <div className="client-nav-submenu">
            {trainingLinks()}
          </div>
        </div>
        {link('nutricion', <><Icon name="food" /> Nutrición</>)}
        {link('progreso', <><Icon name="activity" /> Mi progreso</>)}
        {link('membresia', <><Icon name="card" /> Mi membresía</>)}
      </nav>
      <div className="client-sidebar-footer">
        <div className="client-sidebar-status">
          <strong>Membresía {membershipStatus}</strong>
          {expiresLabel && <span>{expiresLabel}</span>}
        </div>
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
      </div>
    </aside>

    <main className="client-workspace">
      <header className="client-header">
        <h1 id="client-page-title" ref={heading} tabIndex={-1}>{title}</h1>
        <p>{subtitle}</p>
        {section === 'inicio' && <span className="client-mobile-membership">Membresía {membershipStatus}{expiresLabel && ` · ${expiresLabel}`}</span>}
      </header>
      {error && <p className="client-error" role="alert">{error}</p>}
      {children}
    </main>
    <nav className="client-mobile-nav" aria-label="Navegación móvil">
      {link('inicio', <><Icon name="home" /> Inicio</>)}
      <details ref={trainingMenu} className="client-mobile-training"
        onBlur={event => {
          if (!event.currentTarget.contains(event.relatedTarget)) event.currentTarget.open = false
        }}
        onKeyDown={event => {
          if (event.key !== 'Escape') return
          event.currentTarget.open = false
          event.currentTarget.querySelector('summary')?.focus()
        }}>
        <summary className={isTraining ? 'client-nav-link client-training-current' : 'client-nav-link'}>
          <Icon name="dumbbell" /> Entrenar
        </summary>
        <div className="client-mobile-training-links">{trainingLinks()}</div>
      </details>
      {link('nutricion', <><Icon name="food" /> Nutrición</>)}
      {link('progreso', <><Icon name="activity" /> Progreso</>)}
      {link('membresia', <><Icon name="card" /> Membresía</>)}
    </nav>
  </div>
}
