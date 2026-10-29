import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import Icon from '../components/Icon'
import PlanDialog from '../components/PlanDialog'
import { listPlans } from '../services/memberships'
import './MembershipPlans.css'

const PAGE_SIZE = 8
const normalize = value => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()

export default function MembershipPlans({ onExpired, logoutError }) {
  const [plans, setPlans] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [forbidden, setForbidden] = useState(false)
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState('all')
  const [page, setPage] = useState(1)
  const [revision, setRevision] = useState(0)
  const [selected, setSelected] = useState(null)
  const sequence = useRef(0)
  const lastTrigger = useRef(null)

  const reload = useCallback(() => {
    setLoading(true); setError(''); setRevision(previous => previous + 1)
  }, [])

  useEffect(() => {
    const current = ++sequence.current
    listPlans().then(data => {
      if (current !== sequence.current) return
      setPlans(data); setForbidden(false)
    }).catch(err => {
      if (current !== sequence.current) return
      if (err.status === 401) { onExpired(); return }
      setError(err.message); setForbidden(err.status === 403)
    }).finally(() => { if (current === sequence.current) setLoading(false) })
    // Una respuesta anterior no debe sobrescribir la lista recién guardada.
    return () => { sequence.current += 1 }
  }, [onExpired, revision])

  const filtered = useMemo(() => {
    const search = normalize(query.trim())
    return plans.filter(plan => (status === 'all' || plan.estado === status)
      && normalize(`${plan.nombre} ${plan.idPlanMembresia}`).includes(search))
      .sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'))
  }, [plans, query, status])
  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  const currentPage = Math.min(page, totalPages)
  const visible = filtered.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE)
  const activeCount = plans.filter(plan => plan.estado === 'ACTIVO').length
  const unavailable = loading || !!error
  const count = value => unavailable ? '—' : value

  function openDialog(mode, planId, event) {
    lastTrigger.current = event.currentTarget
    setSelected({ mode, planId })
  }

  function closeDialog() {
    setSelected(null)
    requestAnimationFrame(() => {
      if (lastTrigger.current?.isConnected) lastTrigger.current.focus()
      else document.getElementById('plans-heading')?.focus()
    })
  }

  function saved(plan, action) {
    const message = action === 'created' ? 'creado' : action === 'edited' ? 'actualizado' : plan.estado === 'ACTIVO' ? 'reactivado' : 'desactivado'
    setNotice(`El plan «${plan.nombre}» fue ${message} correctamente.`)
    closeDialog(); reload()
  }

  return <main className="admin-main membership-main">
    <div className="admin-heading"><div><span className="eyebrow">UN PLAN PARA CADA NUEVA META</span>
      <h1>Planes que acompañan.<br className="mobile-break" /> <span>Metas que avanzan.</span></h1>
      <p>Organiza el catálogo de planes de membresía de Your Peak.</p></div>
      <button className="primary compact" disabled={forbidden} onClick={event => openDialog('create', null, event)}><Icon name="plus" /> Crear plan</button>
    </div>
    <section className="admin-stats" aria-label="Resumen de planes">
      <article><span className="stat-icon accent"><Icon name="card" /></span><div><span>Total de planes</span><strong>{count(plans.length)}</strong><small>Catálogo de membresías</small></div></article>
      <article><span className="stat-icon green"><Icon name="check" /></span><div><span>Planes activos</span><strong>{count(activeCount)}</strong><small>Habilitados en el catálogo</small></div></article>
      <article><span className="stat-icon neutral"><Icon name="pause" /></span><div><span>Planes inactivos</span><strong>{count(plans.length - activeCount)}</strong><small>Conservan su historial</small></div></article>
    </section>
    {notice && <p className="alert success" role="status">{notice}</p>}
    {(error || logoutError) && <p className="alert" role="alert">{error || logoutError}</p>}
    <section className="users-panel" aria-labelledby="plans-heading" aria-busy={loading}>
      <div className="users-panel-heading"><div><h2 id="plans-heading" tabIndex="-1">Catálogo de planes <span>{count(plans.length)}</span></h2>
        <p>Consulta, edita y administra la disponibilidad de tus planes.</p></div>
        <button className="secondary-button" onClick={reload} disabled={loading}><Icon name="refresh" />{loading ? 'Actualizando…' : 'Actualizar'}</button></div>
      <div className="users-toolbar"><label className="search-input"><Icon name="search" /><input type="search" aria-label="Buscar planes"
        placeholder="Buscar por nombre o número…" value={query} onChange={event => { setQuery(event.target.value); setPage(1) }} /></label>
        <label className="sort-control">Estado <select value={status} onChange={event => { setStatus(event.target.value); setPage(1) }}>
          <option value="all">Todos los estados</option><option value="ACTIVO">Activos</option><option value="INACTIVO">Inactivos</option></select></label></div>
      {loading ? <div className="table-state" role="status"><span className="loading-dot" /> Consultando planes…</div>
        : error ? <div className="table-state"><Icon name={forbidden ? 'shield' : 'refresh'} /><h3>{forbidden ? 'Acceso no disponible' : 'No pudimos cargar los planes'}</h3>
          <p>{forbidden ? 'Esta sección requiere una cuenta administradora.' : 'Comprueba la conexión y vuelve a consultar el catálogo.'}</p>
          {!forbidden && <button className="secondary-button" onClick={reload}>Volver a intentar</button>}</div>
          : !visible.length ? <div className="table-state"><Icon name={plans.length ? 'search' : 'card'} /><h3>{plans.length ? 'No encontramos coincidencias' : 'Tu primer plan empieza aquí'}</h3>
            <p>{plans.length ? 'Prueba con otro nombre o cambia el filtro de estado.' : 'Crea un plan para comenzar a organizar el catálogo de membresías.'}</p>
            {plans.length > 0 && <button className="secondary-button" onClick={() => { setQuery(''); setStatus('all'); setPage(1) }}>Limpiar filtros</button>}</div>
            : <div className="users-table-scroll"><table className="users-table plans-table"><caption className="sr-only">Planes de membresía</caption>
              <thead><tr><th scope="col">Plan</th><th scope="col">Duración</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead>
              <tbody>{visible.map(plan => <tr key={plan.idPlanMembresia}>
                <td><div className="user-cell"><span className="plan-row-icon"><Icon name="card" /></span><div><strong>{plan.nombre}</strong><span>Plan #{plan.idPlanMembresia}</span></div></div></td>
                <td>{plan.duracionMeses} {plan.duracionMeses === 1 ? 'mes' : 'meses'}</td>
                <td><span className={`status-badge plan-status ${plan.estado === 'ACTIVO' ? 'is-active' : 'is-inactive'}`}><i />{plan.estado === 'ACTIVO' ? 'Activo' : 'Inactivo'}</span></td>
                <td><div className="plan-actions">
                  <button className="review-button" aria-label={`Ver ${plan.nombre}`} onClick={event => openDialog('detail', plan.idPlanMembresia, event)}>Ver</button>
                  <button className="review-button" aria-label={`Editar ${plan.nombre}`} onClick={event => openDialog('edit', plan.idPlanMembresia, event)}>Editar</button>
                  <button className="secondary-button" aria-label={`${plan.estado === 'ACTIVO' ? 'Desactivar' : 'Reactivar'} ${plan.nombre}`}
                    onClick={event => openDialog('status', plan.idPlanMembresia, event)}>{plan.estado === 'ACTIVO' ? 'Desactivar' : 'Reactivar'}</button>
                </div></td>
              </tr>)}</tbody></table></div>}
      <div className="users-pagination"><span>{loading ? 'Actualizando catálogo' : error ? 'Catálogo no disponible' : `${filtered.length} ${filtered.length === 1 ? 'plan' : 'planes'}`}</span>
        <div><button aria-label="Página anterior" disabled={currentPage <= 1 || unavailable} onClick={() => setPage(currentPage - 1)}>←</button>
          <span>{currentPage} / {totalPages}</span><button aria-label="Página siguiente" disabled={currentPage >= totalPages || unavailable} onClick={() => setPage(currentPage + 1)}>→</button></div></div>
    </section>
    <p className="membership-note"><Icon name="shield" /> Desactivar un plan conserva las membresías históricas. La duración se define al crearlo.</p>
    {selected && <PlanDialog key={`${selected.mode}-${selected.planId}`} planId={selected.planId} initialMode={selected.mode}
      onClose={closeDialog} onSaved={saved} onExpired={onExpired} onStale={reload} />}
  </main>
}
