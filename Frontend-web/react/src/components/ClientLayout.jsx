import { useEffect, useRef, useState } from 'react'
import Brand from './Brand'
import Icon from './Icon'
import { clientSections } from '../utils/clientSections'
import { fullName, initials, membershipDate } from '../utils/format'
import './ClientLayout.css'

const dateFormat = new Intl.DateTimeFormat('es-PE', { dateStyle: 'full', timeZone: 'America/Lima' })
const DAY = 86_400_000

/** Días que quedan del periodo vigente (incluido hoy) y su duración total. */
function remainingDays(plan) {
  if (!plan?.fechaInicio || !plan?.fechaVencimiento) return null
  const start = Date.parse(`${plan.fechaInicio}T00:00:00Z`)
  const end = Date.parse(`${plan.fechaVencimiento}T00:00:00Z`)
  const [year, month, day] = new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Lima' }).format(new Date()).split('-').map(Number)
  const today = Date.UTC(year, month - 1, day)
  const total = Math.max(1, Math.round((end - start) / DAY) + 1)
  const left = Math.min(total, Math.max(0, Math.round((end - Math.max(today, start)) / DAY) + 1))
  return { left, total }
}

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
  const plan = user?.membresia
  const daysLeft = remainingDays(plan)

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
          <span>{plan?.nombrePlan ? `Plan ${plan.nombrePlan}` : 'Membresía'}</span>
          <strong>{user?.membresiaVigente ? 'Vigente' : 'No vigente'}{daysLeft ? ` · ${daysLeft.left} ${daysLeft.left === 1 ? 'día' : 'días'}` : ''}</strong>
          {daysLeft && <span className="client-sidebar-progress" aria-hidden="true">
            <span style={{ width: `${(daysLeft.left / daysLeft.total) * 100}%` }} />
          </span>}
          {expiresLabel && <small>{expiresLabel}</small>}
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
        <div className="client-header-text">
          <h1 id="client-page-title" ref={heading} tabIndex={-1}>{title}</h1>
          <p>{subtitle}</p>
        </div>
        {section !== 'nutricion' && <a className="client-quick-action" href={clientSections.nutricion.path}
          onClick={event => handleNavigation(event, 'nutricion')}>
          <Icon name="plus" /> Registrar alimento
        </a>}
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
