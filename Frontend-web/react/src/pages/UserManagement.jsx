import AdminPanel from './AdminPanel'
import ClientMemberships from './ClientMemberships'
import Icon from '../components/Icon'
import './UserManagement.css'

// Reúne las solicitudes de registro y el padrón de clientes bajo una sola sección.
export default function UserManagement({ section, onNavigate, onExpired, logoutError, onPendingCount }) {
  const clientsTab = section === 'clientes'

  return <>
    <nav className="user-management-tabs" aria-label="Apartados de usuarios">
      <button type="button" className={clientsTab ? 'active' : ''} aria-current={clientsTab ? 'page' : undefined}
        onClick={() => onNavigate('clientes')}>
        <Icon name="users" /> Clientes
      </button>
      <button type="button" className={!clientsTab ? 'active' : ''} aria-current={!clientsTab ? 'page' : undefined}
        onClick={() => onNavigate('usuarios')}>
        <Icon name="users" /> Solicitudes
      </button>
    </nav>
    {clientsTab
      ? <ClientMemberships onExpired={onExpired} logoutError={logoutError} />
      : <AdminPanel onExpired={onExpired} logoutError={logoutError} onPendingCount={onPendingCount} />}
  </>
}
