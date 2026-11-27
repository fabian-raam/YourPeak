import { useEffect, useMemo, useState } from 'react'
import Icon from '../components/Icon'
import { getAdminActivity, getAdminOverview, listAdminTrainings, listPending } from '../services/admin'
import { listActiveClients } from '../services/memberships'
import { fullName, initials } from '../utils/format'
import './AdminOverview.css'
import './AdminHome.css'

const DAY = 86_400_000
const weekday = ['D', 'L', 'M', 'X', 'J', 'V', 'S']
const longDate = new Intl.DateTimeFormat('es-PE', { weekday: 'long', day: 'numeric', month: 'long', timeZone: 'America/Lima' })

/** Fecha de hoy en Lima como milisegundos UTC a medianoche, para contar días sin desfases. */
function todayUtc() {
  const [year, month, day] = new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Lima' }).format(new Date()).split('-').map(Number)
  return Date.UTC(year, month - 1, day)
}
const daysUntil = (date, today) => date ? Math.round((Date.parse(`${date}T00:00:00Z`) - today) / DAY) : null

function relative(value) {
  if (!value) return ''
  const minutes = Math.round((Date.now() - new Date(value).getTime()) / 60_000)
  if (minutes < 60) return `hace ${Math.max(1, minutes)} min`
  const hours = Math.round(minutes / 60)
  if (hours < 24) return `hace ${hours} h`
  const days = Math.round(hours / 24)
  return days === 1 ? 'hace 1 día' : `hace ${days} días`
}

function ActivityChart({ days }) {
  const max = Math.max(1, ...days.map(day => day.sesiones))
  return <div className="ah-chart" role="img"
    aria-label={`Sesiones por día: ${days.map(day => `${day.fecha} ${day.sesiones}`).join(', ')}`}>
    {days.map((day, index) => {
      const date = new Date(`${day.fecha}T00:00:00Z`)
      return <div className="ah-bar" key={day.fecha} title={`${day.fecha}: ${day.sesiones} sesiones`}>
        <span className="ah-bar-value">{day.sesiones || ''}</span>
        <i className={index === days.length - 1 ? 'is-today' : undefined} style={{ height: `${Math.max(3, day.sesiones / max * 100)}%` }} />
        <small>{weekday[date.getUTCDay()]}</small>
      </div>
    })}
  </div>
}

export default function AdminOverview({ onNavigate, onExpired }) {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [revision, setRevision] = useState(0)
  const [today] = useState(() => longDate.format(new Date()))

  useEffect(() => {
    let vigente = true
    setLoading(true)
    setError('')
    Promise.all([getAdminOverview(), getAdminActivity(28), listPending(), listActiveClients(), listAdminTrainings()])
      .then(([resumen, actividad, pendientes, activos, entrenamientos]) => {
        if (vigente) setData({ resumen, actividad, pendientes, activos, entrenamientos })
      }).catch(err => {
        if (!vigente) return
        if (err.status === 401) onExpired()
        else setError(err.message)
      }).finally(() => { if (vigente) setLoading(false) })
    return () => { vigente = false }
  }, [onExpired, revision])

  const view = useMemo(() => {
    if (!data) return null
    const today = todayUtc()
    const expiring = data.activos
      .map(person => ({ ...person, left: daysUntil(person.fechaVencimiento, today) }))
      .filter(person => person.left !== null && person.left >= 0 && person.left <= 7)
      .sort((a, b) => a.left - b.left)
    const recent = data.actividad.slice(-14)
    const previous = data.actividad.slice(0, -14)
    const sum = list => list.reduce((total, day) => total + day.sesiones, 0)
    const total = sum(recent)
    const before = sum(previous)
    const trend = before ? Math.round((total - before) / before * 100) : null
    const top = [...data.entrenamientos]
      .filter(person => person.sesionesUltimos30Dias > 0)
      .sort((a, b) => b.sesionesUltimos30Dias - a.sesionesUltimos30Dias)
      .slice(0, 4)
    const sessions30 = data.entrenamientos.reduce((sumSessions, person) => sumSessions + person.sesionesUltimos30Dias, 0)
    const pending = [...data.pendientes].sort((a, b) =>
      String(a.fechaSolicitud || a.fechaAlta).localeCompare(String(b.fechaSolicitud || b.fechaAlta)))
    return { expiring, recent, total, trend, top, sessions30, pending }
  }, [data])

  const kpis = [
    { label: 'Clientes activos', value: data?.resumen.clientesActivos, note: 'Con acceso al gimnasio', icon: 'users' },
    { label: 'Solicitudes pendientes', value: data?.resumen.solicitudesPendientes, note: 'Esperan revisión', icon: 'clock', tone: 'warn' },
    { label: 'Vencen en 7 días', value: view?.expiring.length, note: 'Membresías por renovar', icon: 'card', tone: view?.expiring.length ? 'danger' : '' },
    { label: 'Sesiones · 30 días', value: view?.sessions30, note: 'Días con series registradas', icon: 'activity', tone: 'ok' },
  ]
  const catalog = [
    { label: 'planes activos', value: data?.resumen.planesMembresiaActivos, icon: 'card', target: 'membresias' },
    { label: 'ejercicios', value: data?.resumen.ejerciciosActivos, icon: 'dumbbell', target: 'ejercicios' },
    { label: 'alimentos', value: data?.resumen.alimentosCatalogo, icon: 'food', target: 'alimentos' },
    { label: data?.resumen.clientesInactivos === 1 ? 'cliente inactivo' : 'clientes inactivos', value: data?.resumen.clientesInactivos, icon: 'pause', target: 'clientes' },
  ]
  const show = value => loading ? '—' : value ?? '—'

  return <main className="admin-main admin-overview-page ah-page">
    <header className="admin-heading">
      <div>
        <span className="eyebrow">{today}</span>
        <h1>Resumen</h1>
      </div>
      <div className="admin-heading-actions">
        <button className="secondary-button" onClick={() => setRevision(value => value + 1)} disabled={loading}>
          <Icon name="refresh" /> {loading ? 'Actualizando…' : 'Actualizar'}
        </button>
        <button className="secondary-button ah-soft" onClick={() => onNavigate('ejercicios')}><Icon name="dumbbell" /> Ejercicios</button>
        <button className="primary compact" onClick={() => onNavigate('membresias')}><Icon name="plus" /> Planes</button>
      </div>
    </header>

    {error && <div className="alert overview-error" role="alert">
      <span>{error}</span>
      <button className="secondary-button" onClick={() => setRevision(value => value + 1)}>Reintentar</button>
    </div>}

    <section className="ah-kpis" aria-label="Indicadores principales" aria-busy={loading}>
      {kpis.map(kpi => <article className={`ah-kpi ${kpi.tone || ''}`} key={kpi.label}>
        <span className="ah-kpi-icon"><Icon name={kpi.icon} /></span>
        <small>{kpi.label}</small>
        <strong>{show(kpi.value)}</strong>
        <em>{kpi.note}</em>
      </article>)}
    </section>

    <div className="ah-grid">
      <section className="ah-card" aria-labelledby="ah-attention">
        <div className="ah-card-head"><h2 id="ah-attention">Requiere atención</h2>
          <button className="ah-link" onClick={() => onNavigate('usuarios')}>Ver usuarios <Icon name="arrow" /></button></div>

        <h3 className="ah-sub">Solicitudes de registro</h3>
        {loading ? <p className="ah-empty">Cargando…</p> : !view?.pending.length
          ? <p className="ah-empty"><Icon name="check" /> No hay solicitudes por revisar.</p>
          : <ul className="ah-rows">{view.pending.slice(0, 3).map(person => <li key={person.idCliente}>
            <span className="ah-avatar">{initials(person)}</span>
            <div><strong>{fullName(person)}</strong><small>{person.correo} · {relative(person.fechaSolicitud)}</small></div>
            {person.numeroSolicitud > 1 && <span className="ah-pill warn">Reintento</span>}
            <button className="primary compact" onClick={() => onNavigate('usuarios')}>Revisar</button>
          </li>)}</ul>}
        {view?.pending.length > 3 && <p className="ah-more">y {view.pending.length - 3} más en Usuarios</p>}

        <h3 className="ah-sub">Membresías por vencer</h3>
        {loading ? <p className="ah-empty">Cargando…</p> : !view?.expiring.length
          ? <p className="ah-empty"><Icon name="check" /> Ninguna membresía vence en los próximos 7 días.</p>
          : <ul className="ah-rows">{view.expiring.slice(0, 4).map(person => <li key={person.idCliente}>
            <span className="ah-avatar alt">{initials(person)}</span>
            <div><strong>{fullName(person)}</strong><small>{person.correo}</small></div>
            <span className="ah-pill warn"><Icon name="clock" /> {person.left === 0 ? 'vence hoy' : person.left === 1 ? 'vence mañana' : `vence en ${person.left} días`}</span>
            <button className="secondary-button ah-soft" onClick={() => onNavigate('clientes')}>Renovar</button>
          </li>)}</ul>}
      </section>

      <div className="ah-col">
        <section className="ah-card" aria-labelledby="ah-activity">
          <div className="ah-card-head"><h2 id="ah-activity">Actividad de entrenamiento</h2><span className="ah-pill">Últimos 14 días</span></div>
          <div className="ah-big">
            <strong>{loading ? '—' : view?.total ?? 0}</strong><span>sesiones registradas</span>
            {view?.trend !== null && view?.trend !== undefined && <em className={view.trend < 0 ? 'down' : ''}>
              {view.trend < 0 ? '▼' : '▲'} {Math.abs(view.trend)}% vs. 14 días previos</em>}
          </div>
          {view && <ActivityChart days={view.recent} />}
        </section>

        <section className="ah-card" aria-labelledby="ah-top">
          <div className="ah-card-head"><h2 id="ah-top">Clientes más activos</h2>
            <button className="ah-link" onClick={() => onNavigate('entrenamientos')}>Ver entrenamientos <Icon name="arrow" /></button></div>
          {loading ? <p className="ah-empty">Cargando…</p> : !view?.top.length
            ? <p className="ah-empty">Aún no hay entrenamientos registrados en los últimos 30 días.</p>
            : <ol className="ah-rows ah-rank">{view.top.map((person, index) => <li key={person.idCliente}>
              <span className="ah-rank-n">{index + 1}</span>
              <span className="ah-avatar">{initials(person)}</span>
              <div><strong>{fullName(person)}</strong><small>Última actividad {relative(person.ultimaActividad)}</small></div>
              <b>{person.sesionesUltimos30Dias}<i> {person.sesionesUltimos30Dias === 1 ? 'sesión' : 'sesiones'}</i></b>
            </li>)}</ol>}
        </section>
      </div>
    </div>

    <nav className="ah-catalog" aria-label="Catálogos">
      {catalog.map(item => <button key={item.target} onClick={() => onNavigate(item.target)}>
        <Icon name={item.icon} /><b>{show(item.value)}</b><span>{item.label}</span>
      </button>)}
    </nav>
  </main>
}
