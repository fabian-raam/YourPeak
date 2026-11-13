import Brand from './Brand'
import Icon from './Icon'
import './ClientLayout.css'

const menuButton = (label, icon, active = false) => (
  <button className={`training-client-nav-item${active ? ' is-active' : ''}`} type="button" disabled={!active}>
    <Icon name={icon} /><span>{label}</span>
  </button>
)

export default function ClientLayout({ section, onNavigate, children }) {
  const openSection = (target, event) => {
    event.preventDefault()
    onNavigate(target)
  }

  return <div className="training-client-layout">
    <aside className="training-client-sidebar">
      <Brand onNavigate={event => openSection('ejercicios', event)} />

      <nav className="training-client-navigation" aria-label="Menú del cliente">
        {menuButton('Inicio', 'home')}
        <div className="training-client-nav-group">
          <div className="training-client-nav-heading"><Icon name="dumbbell" /><span>Entrenamiento</span></div>
          <div className="training-client-nav-submenu">
            <a className={`training-client-nav-item${section === 'mi-rutina' ? ' is-active' : ''}`} href="/cliente/mi-rutina"
              aria-current={section === 'mi-rutina' ? 'page' : undefined} onClick={event => openSection('mi-rutina', event)}>
              <span>Mi rutina</span>
            </a>
            <a className={`training-client-nav-item${section === 'ejercicios' ? ' is-active' : ''}`} href="/cliente/ejercicios"
              aria-current={section === 'ejercicios' ? 'page' : undefined} onClick={event => openSection('ejercicios', event)}>
              <span>Ejercicios</span>
            </a>
            <a className={`training-client-nav-item${section === 'disponibilidad' ? ' is-active' : ''}`} href="/cliente/disponibilidad"
              aria-current={section === 'disponibilidad' ? 'page' : undefined} onClick={event => openSection('disponibilidad', event)}>
              <Icon name="clock" /><span>Disponibilidad</span>
            </a>
          </div>
        </div>
        {menuButton('Nutrición', 'food')}
        {menuButton('Mi progreso', 'activity')}
        {menuButton('Mi membresía', 'card')}
      </nav>

      <div className="training-client-sidebar-membership">
        <span className="training-client-membership-dot" />
        <div><strong>Membresía activa</strong><span>Acceso vigente</span></div>
      </div>
    </aside>

    <div className="training-client-workspace">
      {children}
    </div>
  </div>
}
