import { useEffect, useRef, useState } from 'react'
import Brand from '../components/Brand'
import Icon from '../components/Icon'
import { clientSections } from '../utils/clientSections'
import { fullName, initials } from '../utils/format'
import './ClientArea.css'

const dateFormat = new Intl.DateTimeFormat('es-PE', { dateStyle: 'full', timeZone: 'America/Lima' })

function ClientHome({ membershipStatus, membershipLink, profileLink }) {
  return <div className="client-home-grid">
    <section className="client-card client-feature" aria-labelledby="client-home-membership">
      <span className="client-card-icon"><Icon name="card" /></span>
      <h2 id="client-home-membership">Membresía {membershipStatus.toLowerCase()}</h2>
      {membershipLink}
    </section>
    <section className="client-card client-home-profile" aria-labelledby="client-home-profile">
      <span className="client-card-icon"><Icon name="users" /></span>
      <h2 id="client-home-profile">Tu cuenta</h2>
      <p>Consulta tus datos personales y tu correo institucional.</p>
      {profileLink}
    </section>
  </div>
}

function ClientProfile({ user, accountName, avatar }) {
  return <section className="client-card client-profile" aria-labelledby="client-profile-name">
    <div className="client-card-heading">
      <span className="client-avatar client-profile-avatar" aria-hidden="true">{avatar}</span>
      <div>
        <h2 id="client-profile-name">{accountName}</h2>
        <p>Cliente</p>
      </div>
    </div>
    <dl className="client-facts">
      <div><dt>Nombre</dt><dd>{user.nombre || 'Sin registrar'}</dd></div>
      <div><dt>Apellido</dt><dd>{user.apellido || 'Sin registrar'}</dd></div>
      <div><dt>Correo institucional</dt><dd>{user.correo || 'Sin registrar'}</dd></div>
      <div><dt>Rol</dt><dd>Cliente</dd></div>
    </dl>
  </section>
}

function ClientMembership({ status }) {
  return <section className="client-card" aria-labelledby="client-membership-heading">
    <div className="client-card-heading">
      <span className="client-card-icon"><Icon name="card" /></span>
      <h2 id="client-membership-heading">Estado de la membresía</h2>
    </div>
    <dl className="client-facts">
      <div><dt>Estado</dt><dd><span className="client-membership-status">{status}</span></dd></div>
    </dl>
  </section>
}

export default function ClientArea({ user, section, onNavigate, onLogout, logoutBusy, error }) {
  const accountMenu = useRef(null)
  const heading = useRef(null)
  const [today] = useState(() => dateFormat.format(new Date()))
  const accountName = fullName(user) || 'Mi cuenta'
  const avatar = initials(user) || 'YP'
  const membershipStatus = user.membresiaVigente ? 'Vigente' : 'No vigente'
  const { label } = clientSections[section]
  const title = section === 'inicio' ? (user.nombre ? `Hola, ${user.nombre}` : 'Inicio') : label
  const subtitle = section === 'inicio' ? today
    : section === 'perfil' ? 'Tus datos de cuenta.' : 'Consulta el estado de tu membresía.'

  useEffect(() => {
    heading.current?.focus({ preventScroll: true })
  }, [section])

  function handleLink(event, target) {
    if (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return
    event.preventDefault()
    if (accountMenu.current) accountMenu.current.open = false
    onNavigate(target)
  }

  function link(target, className, content) {
    return <a href={clientSections[target].path} className={className}
      aria-current={section === target ? 'page' : undefined}
      onClick={event => handleLink(event, target)}>{content}</a>
  }

  return <div className="client-layout">
    <aside className="client-sidebar">
      <Brand href="/" onNavigate={event => handleLink(event, 'inicio')} />
      <nav aria-label="Área del cliente">
        {link('inicio', 'client-nav-link', <><Icon name="home" /> Inicio</>)}
        {link('membresia', 'client-nav-link', <><Icon name="card" /> Mi membresía</>)}
      </nav>
      <div className="client-sidebar-status">
        <strong>Membresía {membershipStatus.toLowerCase()}</strong>
      </div>
    </aside>
    <main className="client-workspace">
      <header className="client-header">
        <div>
          <h1 ref={heading} tabIndex={-1}>{title}</h1>
          <p>{subtitle}</p>
        </div>
        <details className="client-account" ref={accountMenu}
          onBlur={event => {
            if (!event.currentTarget.contains(event.relatedTarget)) event.currentTarget.open = false
          }}
          onKeyDown={event => {
            if (event.key !== 'Escape' || !event.currentTarget.open) return
            event.currentTarget.open = false
            event.currentTarget.querySelector('summary').focus()
          }}>
          <summary><span className="client-avatar" aria-hidden="true">{avatar}</span>
            <span className="client-account-name">{user.nombre || 'Mi cuenta'}</span><Icon name="chevron" width="16" height="16" /></summary>
          <div className="client-account-links">
            {link('perfil', undefined, 'Mi perfil')}
            <button type="button" onClick={onLogout} disabled={logoutBusy}>
              <Icon name="logout" /> {logoutBusy ? 'Cerrando sesión…' : 'Cerrar sesión'}
            </button>
          </div>
        </details>
      </header>
      {error && <p className="client-error" role="alert">{error}</p>}
      {section === 'inicio' && <ClientHome membershipStatus={membershipStatus}
        membershipLink={link('membresia', 'client-cta', <>Ver mi membresía <Icon name="arrow" /></>)}
        profileLink={link('perfil', 'client-text-link', <>Ver mi perfil <Icon name="arrow" /></>)} />}
      {section === 'perfil' && <ClientProfile user={user} accountName={accountName} avatar={avatar} />}
      {section === 'membresia' && <ClientMembership status={membershipStatus} />}
    </main>
  </div>
}
