import Icon from '../components/Icon'

export default function UserStats({ active, pending, inactive, loading }) {
  const stats = [
    { label: 'Clientes activos', value: active, icon: 'users', tone: 'blue' },
    { label: 'Solicitudes pendientes', value: pending, icon: 'clock', tone: 'amber' },
    { label: 'Usuarios inactivos', value: inactive, icon: 'pause', tone: 'blue' },
  ]

  return <section className="user-stats" aria-label="Estadísticas de usuarios" aria-busy={loading}>
    {stats.map(stat => <article className="user-stat-card" key={stat.label}>
      <span className={`user-stat-icon ${stat.tone}`}><Icon name={stat.icon} /></span>
      <div><strong>{loading ? '—' : stat.value}</strong><span>{stat.label}</span></div>
    </article>)}
  </section>
}
