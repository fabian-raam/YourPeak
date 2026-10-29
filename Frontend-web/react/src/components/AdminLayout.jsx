import Brand from './Brand'
import Icon from './Icon'
import { initials } from '../utils/format'
import '../pages/AdminPanel.css'

// Usuarios y planes comparten navegación, identidad y cierre de sesión.
export default function AdminLayout({ user, section, onNavigate, onLogout, logoutBusy, pendingCount, children }) {
  const memberships = section === 'membresias'
  const link = (target, label, icon) => <a href={`/admin/${target}`}
    className={section === target ? 'active' : undefined}
    aria-current={section === target ? 'page' : undefined}
    onClick={event => {
      if (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return
      event.preventDefault(); onNavigate(target)
    }}><Icon name={icon} /><span>{label}</span>{target === 'usuarios' && pendingCount !== null && <b>{pendingCount}</b>}</a>

  return <div className="admin-layout">
    <aside className="admin-sidebar">
      <Brand onNavigate={event => { event.preventDefault(); onNavigate('usuarios') }} />
      <span className="sidebar-label">ADMINISTRACIÓN</span>
      <nav aria-label="Administración">
        <button disabled><Icon name="grid" /><span>Resumen</span><small>Próximamente</small></button>
        {link('usuarios', 'Usuarios', 'users')}
        <button disabled><Icon name="activity" /><span>Entrenamientos</span><small>Próximamente</small></button>
        {link('membresias', 'Membresías', 'card')}
      </nav>
      <div className="sidebar-note"><Icon name="shield" /><p>El primer paso<br /><strong>empieza contigo.</strong></p>
        <span>{memberships ? 'Organiza los planes que acompañarán las metas de nuestra comunidad.' : 'Revisa cada solicitud y acompaña a nuestra comunidad hacia su mejor versión.'}</span></div>
      <div className="sidebar-account"><span className="avatar">{initials(user)}</span><div><strong>{user.nombre}</strong><span>Administrador</span></div>
        <button className="icon-button" aria-label="Cerrar sesión" disabled={logoutBusy} onClick={onLogout}><Icon name="logout" /></button>
      </div>
    </aside>
    <div className="admin-workspace">
      <header className="admin-topbar"><span>Administración <span aria-hidden="true">/</span> <strong>{memberships ? 'Membresías' : 'Usuarios'}</strong></span>
        <span className="admin-role"><Icon name="shield" /> Acceso de administrador</span></header>
      {children}
    </div>
  </div>
}
