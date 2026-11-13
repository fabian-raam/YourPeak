import { useEffect, useRef, useState } from 'react'
import Icon from '../components/Icon'
import { clientSections } from '../utils/clientSections'
import { fullName, initials } from '../utils/format'

const dateFormat = new Intl.DateTimeFormat('es-PE', { dateStyle: 'full', timeZone: 'America/Lima' })

function ClientHome({ membershipStatus, onNavigate }) {
  return <div className="client-home-grid">
    <section className="client-card client-feature" aria-labelledby="client-home-membership">
      <span className="client-card-icon"><Icon name="card" /></span>
      <h2 id="client-home-membership">Membresía {membershipStatus.toLowerCase()}</h2>
      <button className="client-cta" type="button" onClick={() => onNavigate('membresia')}>
        Ver mi membresía <Icon name="arrow" />
      </button>
    </section>
    <section className="client-card client-home-profile" aria-labelledby="client-home-profile">
      <span className="client-card-icon"><Icon name="users" /></span>
      <h2 id="client-home-profile">Tu cuenta</h2>
      <p>Consulta tus datos personales y tu correo institucional.</p>
      <button className="client-text-link" type="button" onClick={() => onNavigate('perfil')}>
        Ver mi perfil <Icon name="arrow" />
      </button>
    </section>
  </div>
}

function ClientProfile({ user, accountName, avatar }) {
  return <section className="client-card client-profile" aria-labelledby="client-profile-name">
    <div className="client-card-heading">
      <span className="client-avatar client-profile-avatar" aria-hidden="true">{avatar}</span>
      <div><h2 id="client-profile-name">{accountName}</h2><p>Cliente</p></div>
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

export default function ClientAccountSection({ user, section, onNavigate }) {
  const heading = useRef(null)
  const [today] = useState(() => dateFormat.format(new Date()))
  const accountName = fullName(user) || 'Mi cuenta'
  const avatar = initials(user) || 'YP'
  const membershipStatus = user.membresiaVigente ? 'Vigente' : 'No vigente'
  const label = clientSections[section].label
  const title = section === 'inicio' ? (user.nombre ? `Hola, ${user.nombre}` : 'Inicio') : label
  const subtitle = section === 'inicio' ? today
    : section === 'perfil' ? 'Tus datos de cuenta.' : 'Consulta el estado de tu membresía.'

  useEffect(() => { heading.current?.focus({ preventScroll: true }) }, [section])

  return <>
    <header className="client-header">
      <div><h1 ref={heading} tabIndex={-1}>{title}</h1><p>{subtitle}</p></div>
    </header>
    {section === 'inicio' && <ClientHome membershipStatus={membershipStatus} onNavigate={onNavigate} />}
    {section === 'perfil' && <ClientProfile user={user} accountName={accountName} avatar={avatar} />}
    {section === 'membresia' && <ClientMembership status={membershipStatus} />}
  </>
}
