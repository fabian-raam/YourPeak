import ClientHome from './ClientHome'
import ClientMembership from './ClientMembership'

export default function ClientAccountSection({ user, section, onNavigate, onExpired }) {
  return section === 'inicio'
    ? <ClientHome onNavigate={onNavigate} onExpired={onExpired} />
    : <ClientMembership user={user} onExpired={onExpired} />
}
