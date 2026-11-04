import AdminPanel from './AdminPanel'
import ClientMemberships from './ClientMemberships'
import './UserManagement.css'

// Reúne las solicitudes de registro y el padrón de clientes bajo una sola sección.
export default function UserManagement({ section, onNavigate, onExpired, logoutError, onPendingCount, pendingCount }) {
  const clientsTab = section === 'clientes'

  return clientsTab
    ? <ClientMemberships section={section} onNavigate={onNavigate} pendingCount={pendingCount} onPendingCount={onPendingCount} onExpired={onExpired} logoutError={logoutError} />
    : <AdminPanel section={section} onNavigate={onNavigate} pendingCount={pendingCount} onExpired={onExpired} logoutError={logoutError} onPendingCount={onPendingCount} />
}
