import { useEffect, useMemo, useState } from 'react'
import Icon from '../components/Icon'
import { getAdminTraining, listAdminTrainings } from '../services/admin'
import { initials } from '../utils/format'
import './AdminOverview.css'

const weekdays = ['', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo']
const dateTime = value => value
  ? new Date(value).toLocaleString('es-PE', { dateStyle: 'medium', timeStyle: 'short' })
  : 'Sin actividad registrada'

export default function AdminTrainings({ onExpired }) {
  const [clients, setClients] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState('TODOS')
  const [revision, setRevision] = useState(0)
  const [selected, setSelected] = useState(null)
  const [detail, setDetail] = useState(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [detailError, setDetailError] = useState('')

  useEffect(() => {
    let vigente = true
    setLoading(true)
    setError('')
    listAdminTrainings().then(result => {
      if (!vigente) return
      if (!Array.isArray(result)) throw new Error('No se pudo interpretar la lista de entrenamientos.')
      setClients(result)
    }).catch(err => {
      if (!vigente) return
      if (err.status === 401) onExpired()
      else setError(err.message)
    }).finally(() => { if (vigente) setLoading(false) })
    return () => { vigente = false }
  }, [onExpired, revision])

  const visibleClients = useMemo(() => {
    const search = query.trim().toLocaleLowerCase('es')
    return clients.filter(client => {
      const matchesStatus = status === 'TODOS' || client.estado === status
      const matchesQuery = `${client.nombre} ${client.apellido} ${client.correo}`.toLocaleLowerCase('es').includes(search)
      return matchesStatus && matchesQuery
    })
  }, [clients, query, status])

  async function toggleDetails(client) {
    if (selected === client.idCliente) {
      setSelected(null); setDetail(null); setDetailError('')
      return
    }
    setSelected(client.idCliente); setDetail(null); setDetailError(''); setDetailLoading(true)
    try {
      const result = await getAdminTraining(client.idCliente)
      setDetail(result)
    } catch (err) {
      if (err.status === 401) onExpired()
      else setDetailError(err.message)
    } finally {
      setDetailLoading(false)
    }
  }

  const activeCount = clients.filter(client => client.estado === 'ACTIVO').length
  const inactiveCount = clients.filter(client => client.estado === 'INACTIVO').length
  const sessionCount = clients.reduce((total, client) => total + (client.sesionesUltimos30Dias || 0), 0)

  return <main className="admin-main admin-overview-page admin-trainings-page">
    <header className="admin-heading">
      <div>
        <span className="eyebrow">SEGUIMIENTO DE CLIENTES</span>
        <h1>Entrenamientos</h1>
        <p>Consulta las rutinas semanales y la actividad reciente de tus clientes.</p>
      </div>
      <button className="secondary-button overview-refresh" onClick={() => setRevision(value => value + 1)} disabled={loading}>
        <Icon name="refresh" /> Actualizar
      </button>
    </header>

    <section className="training-stats" aria-label="Resumen de entrenamientos">
      <article><span>Clientes con acceso</span><strong>{loading ? '—' : activeCount}</strong></article>
      <article><span>Clientes inactivos</span><strong>{loading ? '—' : inactiveCount}</strong></article>
      <article><span>Sesiones registradas · 30 días</span><strong>{loading ? '—' : sessionCount}</strong></article>
    </section>

    {error && <div className="alert overview-error" role="alert">
      <span>{error}</span>
      <button className="secondary-button" onClick={() => setRevision(value => value + 1)}>Reintentar</button>
    </div>}

    <section className="training-panel" aria-labelledby="training-list-title">
      <div className="training-toolbar">
        <div>
          <h2 id="training-list-title">Rutinas y actividad</h2>
          <p>La actividad corresponde a los últimos 30 días.</p>
        </div>
        <label className="training-search">
          <Icon name="search" />
          <input value={query} onChange={event => setQuery(event.target.value)} placeholder="Buscar por cliente o correo" aria-label="Buscar cliente" />
        </label>
      </div>
      <div className="training-filters" role="group" aria-label="Filtrar por estado">
        {[
          ['TODOS', `Todos (${clients.length})`],
          ['ACTIVO', `Activos (${activeCount})`],
          ['INACTIVO', `Inactivos (${inactiveCount})`],
        ].map(([value, label]) => <button key={value} className={status === value ? 'selected' : ''} aria-pressed={status === value} onClick={() => setStatus(value)}>{label}</button>)}
      </div>

      {loading ? <div className="training-empty">Cargando rutinas y actividad…</div>
        : error ? null
          : visibleClients.length === 0 ? <div className="training-empty">
            <Icon name="search" /><strong>No hay clientes para mostrar</strong>
            <span>Prueba con otra búsqueda o cambia el filtro de estado.</span>
          </div>
          : <div className="training-table-wrap"><table className="training-table">
            <thead><tr><th>Cliente</th><th>Rutina actual</th><th>Actividad · 30 días</th><th>Estado</th><th><span className="visually-hidden">Acciones</span></th></tr></thead>
            <tbody>{visibleClients.map(client => <tr key={client.idCliente}>
              <td><div className="training-client"><span className="training-avatar">{initials(client)}</span><span><strong>{client.nombre} {client.apellido}</strong><small>{client.correo}</small></span></div></td>
              <td>{client.fechaRutina ? <><strong>{client.ejerciciosProgramados} ejercicios</strong><small>{client.diasProgramados} días · creada {dateTime(client.fechaRutina)}</small></> : <span className="training-muted">Sin rutina activa</span>}</td>
              <td><strong>{client.sesionesUltimos30Dias} sesiones</strong><small>{client.ultimaActividad ? `Última: ${dateTime(client.ultimaActividad)}` : 'Sin actividad registrada'}</small></td>
              <td><span className={`training-status ${client.estado === 'ACTIVO' ? 'active' : 'inactive'}`}><i />{client.estado === 'ACTIVO' ? 'Activo' : 'Inactivo'}</span></td>
              <td><button className="training-detail-button" onClick={() => toggleDetails(client)} aria-expanded={selected === client.idCliente}>
                {selected === client.idCliente ? 'Cerrar detalle' : 'Ver rutina'} <Icon name={selected === client.idCliente ? 'chevron' : 'arrow'} />
              </button></td>
            </tr>)}</tbody>
          </table></div>}
    </section>

    {selected !== null && <section className="training-detail" aria-live="polite" aria-label="Detalle del entrenamiento">
      {detailLoading ? <div className="training-empty">Cargando el detalle de la rutina…</div>
        : detailError ? <div className="alert overview-error" role="alert">{detailError}</div>
          : detail && <>
            <div className="training-detail-heading">
              <div><span className="eyebrow">DETALLE DEL CLIENTE</span><h2>{detail.nombre} {detail.apellido}</h2><p>{detail.correo}</p></div>
              <span className={`training-status ${detail.estado === 'ACTIVO' ? 'active' : 'inactive'}`}><i />{detail.estado === 'ACTIVO' ? 'Activo' : 'Inactivo'}</span>
            </div>
            <div className="training-detail-columns">
              <div className="training-days-block"><h3>Rutina semanal</h3>
                {detail.dias.length ? <div className="training-day-grid">{detail.dias.map(day => <article key={day.diaSemana}>
                  <strong>{weekdays[day.diaSemana] || `Día ${day.diaSemana}`}</strong>
                  <small>{day.horaInicio && day.horaFin ? `${day.horaInicio.slice(0, 5)} – ${day.horaFin.slice(0, 5)}` : 'Horario sin detalle'}</small>
                  <span>{day.ejercicios.length} ejercicios</span>
                  <ul>{day.ejercicios.map(exercise => <li key={exercise.idPlanEjercicio}>{exercise.nombre} · {exercise.series} series</li>)}</ul>
                </article>)}</div> : <p className="training-muted">Este cliente no tiene una rutina activa.</p>}
              </div>
              <div className="training-activity-block"><h3>Actividad reciente</h3>
                {detail.actividadReciente.length ? <ul>{detail.actividadReciente.map(item => <li key={`${item.fecha}-${item.titulo}`}><span><strong>{item.titulo}</strong><small>{new Date(`${item.fecha}T12:00:00`).toLocaleDateString('es-PE', { dateStyle: 'medium' })}</small></span><b>{item.ejercicios} ejercicios</b></li>)}</ul>
                  : <p className="training-muted">No hay sesiones registradas en los últimos 30 días.</p>}
              </div>
            </div>
          </>}
    </section>}
  </main>
}
