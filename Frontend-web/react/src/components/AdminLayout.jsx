import Brand from './Brand'
import Icon from './Icon'
import { initials } from '../utils/format'
import '../pages/AdminPanel.css'

// Usuarios y planes comparten navegación, identidad y cierre de sesión.
export default function AdminLayout({ user, section, onNavigate, onLogout, logoutBusy, pendingCount, children }) {
  const link = (target, label, icon) => {
    const active = target === 'usuarios' ? section === 'usuarios' || section === 'clientes' : section === target
    return <a href={`/admin/${target}`}
    className={active ? 'active' : undefined}
    aria-current={active ? 'page' : undefined}
    onClick={event => {
      if (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return
      event.preventDefault(); onNavigate(target)
    }}><Icon name={icon} /><span>{label}</span>{target === 'usuarios' && pendingCount !== null && <b>{pendingCount}</b>}</a>
  }

  return <div className="admin-layout">
    <aside className="admin-sidebar">
      <Brand onNavigate={event => { event.preventDefault(); onNavigate('usuarios') }} />
      <span className="sidebar-label">ADMINISTRACIÓN</span>
      <nav aria-label="Administración">
        <button disabled><Icon name="grid" /><span>Resumen</span><small>Próximamente</small></button>
        {link('usuarios', 'Usuarios', 'users')}
        <button disabled><Icon name="activity" /><span>Entrenamientos</span><small>Próximamente</small></button>
        {link('membresias', 'Membresías', 'card')}
        {link('ejercicios', 'Ejercicios', 'dumbbell')}
        {link('alimentos', 'Alimentos', 'food')}
        {link('practica', 'Práctica React', 'activity')}
      </nav>
      <div className="sidebar-account"><span className="avatar">{initials(user)}</span><div><strong>{user.nombre}</strong><span>Administrador</span></div>
        <button className="icon-button" aria-label="Cerrar sesión" disabled={logoutBusy} onClick={onLogout}><Icon name="logout" /></button>
      </div>
    </aside>
    <div className={`admin-workspace light-workspace ${section === 'clientes' ? 'clients-workspace' : section === 'usuarios' ? 'requests-workspace' : ''}`}>
      {children}
    </div>
  </div>
}
