import Icon from '../components/Icon'

export default function UserManagementTabs({ section, onNavigate, pendingCount }) {
  const clientsTab = section === 'clientes'

  return <nav className="user-management-tabs" aria-label="Apartados de usuarios">
    <button type="button" className={clientsTab ? 'active' : ''} aria-current={clientsTab ? 'page' : undefined}
      onClick={() => onNavigate('clientes')}>
      <Icon name="users" /> Clientes
    </button>
    <button type="button" className={!clientsTab ? 'active' : ''} aria-current={!clientsTab ? 'page' : undefined}
      onClick={() => onNavigate('usuarios')}>
      <Icon name="users" /> Solicitudes {pendingCount !== null && <span className="user-tab-count">{pendingCount}</span>}
    </button>
  </nav>
}
