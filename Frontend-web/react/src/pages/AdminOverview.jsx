import { useEffect, useState } from 'react'
import Icon from '../components/Icon'
import { getAdminOverview } from '../services/admin'
import './AdminOverview.css'

const metrics = [
  { key: 'clientesActivos', label: 'Clientes activos', note: 'Con acceso al gimnasio', icon: 'users' },
  { key: 'solicitudesPendientes', label: 'Solicitudes pendientes', note: 'Esperan revisión', icon: 'clock', tone: 'amber' },
  { key: 'clientesInactivos', label: 'Clientes inactivos', note: 'Sin membresía vigente', icon: 'pause', tone: 'neutral' },
  { key: 'planesMembresiaActivos', label: 'Planes disponibles', note: 'Activos en el catálogo', icon: 'card' },
  { key: 'ejerciciosActivos', label: 'Ejercicios activos', note: 'En el catálogo del gimnasio', icon: 'dumbbell' },
  { key: 'alimentosCatalogo', label: 'Alimentos', note: 'En el catálogo nutricional', icon: 'food' },
]

export default function AdminOverview({ onNavigate, onExpired }) {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [revision, setRevision] = useState(0)

  useEffect(() => {
    let vigente = true
    setLoading(true)
    setError('')
    getAdminOverview().then(result => {
      if (vigente) setData(result)
    }).catch(err => {
      if (!vigente) return
      if (err.status === 401) onExpired()
      else setError(err.message)
    }).finally(() => { if (vigente) setLoading(false) })
    return () => { vigente = false }
  }, [onExpired, revision])

  return <main className="admin-main admin-overview-page">
    <header className="admin-heading">
      <div>
        <span className="eyebrow">PANEL ADMINISTRATIVO</span>
        <h1>Resumen</h1>
        <p>Vista general de clientes y catálogos de Your Peak.</p>
      </div>
      <button className="secondary-button overview-refresh" onClick={() => setRevision(value => value + 1)} disabled={loading}>
        <Icon name="refresh" /> Actualizar
      </button>
    </header>

    {error && <div className="alert overview-error" role="alert">
      <span>{error}</span>
      <button className="secondary-button" onClick={() => setRevision(value => value + 1)}>Reintentar</button>
    </div>}

    <section className="overview-stats" aria-label="Indicadores del panel">
      {metrics.map(metric => <article className={`overview-stat ${metric.tone || ''}`} key={metric.key}>
        <span className="overview-stat-icon"><Icon name={metric.icon} /></span>
        <div>
          <span>{metric.label}</span>
          <strong>{loading ? '—' : data?.[metric.key] ?? '—'}</strong>
          <small>{metric.note}</small>
        </div>
      </article>)}
    </section>

    <section className="overview-shortcuts" aria-labelledby="overview-shortcuts-title">
      <div className="overview-section-heading">
        <div>
          <span className="eyebrow">ACCESO RÁPIDO</span>
          <h2 id="overview-shortcuts-title">Administración</h2>
        </div>
      </div>
      <div className="overview-shortcut-grid">
        <button className="overview-shortcut" onClick={() => onNavigate('usuarios')}>
          <span className="overview-shortcut-icon"><Icon name="users" /></span>
          <span><strong>Gestionar usuarios</strong><small>Revisa solicitudes y membresías de clientes.</small></span>
          <Icon name="arrow" />
        </button>
        <button className="overview-shortcut" onClick={() => onNavigate('entrenamientos')}>
          <span className="overview-shortcut-icon"><Icon name="activity" /></span>
          <span><strong>Ver entrenamientos</strong><small>Consulta rutinas y actividad reciente.</small></span>
          <Icon name="arrow" />
        </button>
        <button className="overview-shortcut" onClick={() => onNavigate('membresias')}>
          <span className="overview-shortcut-icon"><Icon name="card" /></span>
          <span><strong>Administrar planes</strong><small>Consulta y actualiza los planes disponibles.</small></span>
          <Icon name="arrow" />
        </button>
      </div>
    </section>
  </main>
}
