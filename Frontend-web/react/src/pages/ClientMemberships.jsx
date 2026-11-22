import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import Icon from '../components/Icon'
import ClientToast from '../components/ClientToast'
import UserManagementTabs from './UserManagementTabs'
import UserStats from './UserStats'
import { listPending } from '../services/admin'
import { listActiveClients, listInactiveClients, listClientMemberships, listPlans, renewClientMembership } from '../services/memberships'
import { fullName, initials } from '../utils/format'
import './AdminPanel.css'
import './ClientMemberships.css'

const dateLabel = value => value ? new Date(`${value}T12:00:00`).toLocaleDateString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric' }) : 'Sin período registrado'
const PAGE_SIZE = 7
const periodStatusLabel = { VIGENTE: 'Vigente', PROGRAMADA: 'Programada', VENCIDA: 'Vencida' }
const dayMs = 86_400_000
const limaDate = new Intl.DateTimeFormat('sv-SE', { timeZone: 'America/Lima' })
const readGymDate = () => limaDate.format(new Date())
const calendarTime = value => Date.parse(`${value}T00:00:00Z`)
const daysUntil = (value, today) => value ? (calendarTime(value) - calendarTime(today)) / dayMs : null

function estimateRenewal(history, plan, today) {
  if (!plan) return null
  const latestExpiry = history.reduce((latest, period) =>
    period.fechaVencimiento > latest ? period.fechaVencimiento : latest, '')
  const start = new Date(calendarTime(latestExpiry >= today ? latestExpiry : today))
  if (latestExpiry >= today) start.setUTCDate(start.getUTCDate() + 1)

  // LocalDate.plusMonths ajusta los días que no existen en el mes de destino.
  const end = new Date(start)
  end.setUTCDate(1)
  end.setUTCMonth(end.getUTCMonth() + Number(plan.duracionMeses))
  const lastDay = new Date(Date.UTC(end.getUTCFullYear(), end.getUTCMonth() + 1, 0)).getUTCDate()
  end.setUTCDate(Math.min(start.getUTCDate(), lastDay) - 1)
  return { start: start.toISOString().slice(0, 10), end: end.toISOString().slice(0, 10) }
}

export default function ClientMemberships({ section, onNavigate, pendingCount, onPendingCount, onExpired, logoutError }) {
  const membershipDialog = useRef(null)
  const historyRequest = useRef(0)
  const [clients, setClients] = useState([])
  const [inactiveClients, setInactiveClients] = useState([])
  const [pendingRequests, setPendingRequests] = useState([])
  const [scope, setScope] = useState('ACTIVO')
  const [page, setPage] = useState(1)
  const [plans, setPlans] = useState([])
  const [search, setSearch] = useState('')
  const [selected, setSelected] = useState(null)
  const [history, setHistory] = useState([])
  const [historyStatus, setHistoryStatus] = useState('loading')
  const [planId, setPlanId] = useState('')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [today, setToday] = useState(readGymDate)

  const load = useCallback(async () => {
    setToday(readGymDate())
    try {
      const [people, inactivePeople, requests, catalog] = await Promise.all([listActiveClients(), listInactiveClients(), listPending(), listPlans()])
      setClients(people)
      setInactiveClients(inactivePeople)
      setPendingRequests(requests)
      onPendingCount(requests.length)
      setPlans(catalog.filter(plan => plan.estado === 'ACTIVO'))
    } catch (err) {
      if (err.status === 401) onExpired()
      else setError(err.message)
    } finally { setLoading(false) }
  }, [onExpired, onPendingCount])

  const reload = useCallback(async () => {
    setLoading(true); setError('')
    await load()
  }, [load])

  // El efecto sincroniza la pantalla con la respuesta externa del backend.
  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { load() }, [load])

  useEffect(() => {
    if (selected && !membershipDialog.current.open) membershipDialog.current.showModal()
  }, [selected])

  const filtered = useMemo(() => {
    const needle = search.trim().toLocaleLowerCase('es')
    const source = scope === 'INACTIVO' ? inactiveClients : clients
    return source.filter(person => `${fullName(person)} ${person.correo}`.toLocaleLowerCase('es').includes(needle))
  }, [clients, inactiveClients, scope, search])
  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  const currentPage = Math.min(page, totalPages)
  const visible = filtered.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE)
  const selectedPlan = plans.find(plan => String(plan.idPlanMembresia) === planId)
  const preview = historyStatus === 'ready' && estimateRenewal(history, selectedPlan, today)

  async function openClient(person) {
    const current = ++historyRequest.current
    setToday(readGymDate())
    setSelected(person); setError(''); setNotice(''); setHistory([]); setPlanId(plans[0] ? String(plans[0].idPlanMembresia) : '')
    setHistoryStatus('loading')
    try {
      const periods = await listClientMemberships(person.idCliente)
      if (current !== historyRequest.current) return
      setHistory(periods)
      setHistoryStatus('ready')
    } catch (err) {
      if (current !== historyRequest.current) return
      setHistoryStatus('error')
      if (err.status === 401) onExpired()
      else setError(err.message)
    }
  }

  function closeClient() {
    if (busy) return
    historyRequest.current += 1
    setSelected(null)
    setError('')
  }

  async function submitRenewal(event) {
    event.preventDefault()
    if (busy || !selected || !planId || historyStatus !== 'ready') return
    setBusy(true); setError(''); setNotice('')
    try {
      const period = await renewClientMembership(selected.idCliente, { idPlanMembresia: Number(planId), observacion: null })
      setHistory(previous => [period, ...previous.filter(item => item.idMembresia !== period.idMembresia)])
      setNotice('La renovación quedó registrada y se agregó al historial.')
      await load()
    } catch (err) {
      if (err.status === 401) onExpired()
      else setError(err.message)
    } finally { setBusy(false) }
  }

  return <main className="admin-main membership-main client-membership-main">
    <div className="admin-heading"><div><h1>Clientes y períodos</h1><p>Consulta la vigencia y registra renovaciones sin perder días ya pagados.</p></div>
      <button className="secondary-button" onClick={reload} disabled={loading}><Icon name="refresh" />{loading ? 'Actualizando…' : 'Actualizar'}</button>
    </div>
    <UserStats active={clients.length} pending={pendingRequests.length} inactive={inactiveClients.length} loading={loading} />
    {((!selected && error) || logoutError) && <p className="alert" role="alert">{error || logoutError}</p>}
    <section className="users-panel client-list-panel" aria-label="Listado de clientes" aria-busy={loading}>
      <UserManagementTabs section={section} onNavigate={onNavigate} pendingCount={pendingCount} />
      <div className="client-filter-row">
        <div className="users-toolbar"><label className="search-input"><Icon name="search" /><input type="search" aria-label="Buscar clientes" placeholder="Buscar por nombre o correo" value={search} onChange={event => { setSearch(event.target.value); setPage(1) }} /></label></div>
        <div className="client-state-tabs" aria-label="Filtrar clientes por estado">
          <button type="button" aria-pressed={scope === 'ACTIVO'} onClick={() => { setScope('ACTIVO'); setPage(1) }}>Activos</button>
          <button type="button" aria-pressed={scope === 'INACTIVO'} onClick={() => { setScope('INACTIVO'); setPage(1) }}>Inactivos</button>
        </div>
      </div>
      {loading ? <div className="table-state" role="status"><span className="loading-dot" /> Consultando clientes…</div>
        : !visible.length ? <div className="table-state"><Icon name="users" /><h3>{search ? 'No encontramos coincidencias' : scope === 'INACTIVO' ? 'No hay clientes inactivos' : 'No hay clientes activos'}</h3><p>{search ? 'Prueba con otro nombre o correo.' : scope === 'INACTIVO' ? 'Los clientes cuyo período venció aparecerán aquí para renovarlos.' : 'Los clientes activos aparecerán aquí.'}</p></div>
          : <div className="users-table-scroll"><table className="users-table client-list-table"><caption className="sr-only">Clientes y membresías</caption><thead><tr><th scope="col">Cliente</th><th scope="col">Vigencia</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead><tbody>
            {visible.map(person => {
              const remaining = daysUntil(person.fechaVencimiento, today)
              const progress = remaining === null ? 0 : Math.min(100, Math.max(0, remaining / 30 * 100))
              const membershipActive = person.estadoUsuario === 'ACTIVO'
              const tone = !membershipActive || remaining === null ? 'muted' : remaining <= 15 ? 'urgent' : remaining <= 30 ? 'soon' : 'current'
              return <tr key={person.idCliente}>
                <td><div className="user-cell"><span className="avatar">{initials(person)}</span><div><strong>{fullName(person)}</strong><span>{person.correo}</span></div></div></td>
                <td className={`client-expiry-cell ${tone}`}>
                  <div><strong>{dateLabel(person.fechaVencimiento)}</strong>{remaining !== null && <span>{remaining < 0 ? `Venció hace ${Math.abs(remaining)} días` : remaining === 0 ? 'Vence hoy' : `Vence en ${remaining} días`}</span>}</div>
                  {remaining !== null && <div className="client-expiry-track" aria-hidden="true"><i style={{ width: `${progress}%` }} /></div>}
                </td>
                <td><span className={`status-badge ${membershipActive ? 'membership-current' : 'membership-expired'}`}><i />{membershipActive ? 'Activo' : 'Inactivo'}</span></td>
                <td><button className="review-button" onClick={() => openClient(person)}>{scope === 'INACTIVO' ? 'Renovar' : 'Ver / renovar'} <Icon name="arrow" /></button></td>
              </tr>
            })}
          </tbody></table></div>}
      {!loading && filtered.length > 0 && <footer className="client-pagination">
        <span>Mostrando {(currentPage - 1) * PAGE_SIZE + 1}–{Math.min(currentPage * PAGE_SIZE, filtered.length)} de {filtered.length}</span>
        <div>
          <button type="button" aria-label="Página anterior" disabled={currentPage === 1} onClick={() => setPage(currentPage - 1)}>‹</button>
          <strong aria-current="page">{currentPage}</strong>
          <button type="button" aria-label="Página siguiente" disabled={currentPage === totalPages} onClick={() => setPage(currentPage + 1)}>›</button>
        </div>
      </footer>}
    </section>
    {selected && <dialog ref={membershipDialog} className="approval-dialog plan-dialog client-membership-dialog" aria-labelledby="client-membership-title"
      onCancel={event => { event.preventDefault(); closeClient() }}>
      <header className="client-dialog-header">
        <span className="avatar">{initials(selected)}</span>
        <div><h2 id="client-membership-title">{fullName(selected)}</h2><p className="client-membership-email">{selected.correo}</p></div>
        <button className="icon-button" aria-label="Cerrar" disabled={busy} onClick={closeClient}><Icon name="close" /></button>
      </header>
      {error && <p className="alert" role="alert">{error}</p>}
      {notice && <ClientToast title="Membresía renovada" message={notice}
        inline onClose={() => setNotice('')} />}
      <section className="membership-timeline" aria-labelledby="membership-timeline-title">
        <h3 id="membership-timeline-title">Línea de tiempo</h3>
        {historyStatus === 'loading' && <p className="timeline-empty" role="status">Consultando períodos…</p>}
        {historyStatus === 'error' && <button type="button" className="text-button" onClick={() => openClient(selected)}>Volver a intentar</button>}
        {history.length ? <div className="membership-timeline-list">{history.map(period => {
          const remaining = daysUntil(period.fechaVencimiento, today)
          const current = period.estadoPeriodo === 'VIGENTE'
          const duration = (calendarTime(period.fechaVencimiento) - calendarTime(period.fechaInicio)) / dayMs + 1
          const progress = duration > 0 && remaining !== null ? Math.min(100, Math.max(0, (remaining + 1) / duration * 100)) : 0
          return <article className={`timeline-item ${period.estadoPeriodo.toLowerCase()}`} key={period.idMembresia}>
            <span className="timeline-marker" aria-hidden="true" />
            <div className="timeline-card">
              <div><strong>{period.nombrePlan}</strong><span>{dateLabel(period.fechaInicio)} – {dateLabel(period.fechaVencimiento)}</span></div>
              <b className={`timeline-status ${period.estadoPeriodo.toLowerCase()}`}>{periodStatusLabel[period.estadoPeriodo] || period.estadoPeriodo}</b>
              {current && <div className="timeline-progress" aria-hidden="true"><i style={{ width: `${progress}%` }} /></div>}
            </div>
          </article>
        })}</div> : historyStatus === 'ready' && <p className="timeline-empty">No hay períodos registrados para este cliente.</p>}
        {preview && <article className="timeline-item timeline-estimate">
          <span className="timeline-marker" aria-hidden="true" />
          <div className="timeline-card"><div>
            <small>Próximo período estimado</small>
            <strong>{selectedPlan.nombre}</strong>
            <span>{dateLabel(preview.start)} – {dateLabel(preview.end)}</span>
          </div></div>
        </article>}
      </section>
      <form onSubmit={submitRenewal}>
        <fieldset className="renewal-plan-picker" disabled={busy || !plans.length}>
          <legend>Elige el plan</legend>
          <div className="renewal-plan-grid">
            {plans.map(plan => {
              const selectedPlanCard = String(plan.idPlanMembresia) === planId
              return <label className={`renewal-plan-card ${selectedPlanCard ? 'selected' : ''}`} key={plan.idPlanMembresia}>
                <input className="sr-only" type="radio" name="renewal-plan" checked={selectedPlanCard} onChange={() => setPlanId(String(plan.idPlanMembresia))} />
                {selectedPlanCard && <span className="renewal-plan-check" aria-hidden="true">✓ Elegido</span>}
                <strong>{plan.duracionMeses}</strong>
                <span>{plan.duracionMeses === 1 ? 'mes' : 'meses'} · {plan.nombre}</span>
              </label>
            })}
          </div>
        </fieldset>
        {!plans.length && <p className="plan-info">No hay planes activos disponibles para renovar.</p>}
        <p className="renewal-note"><Icon name="info" /> Si hay días pagados, la renovación empieza después del último vencimiento. Si el período venció o aún no hay uno, empieza hoy.</p>
        <div className="renewal-dialog-actions"><button type="button" className="renewal-close" disabled={busy} onClick={closeClient}>Cerrar</button><button className="renewal-submit" disabled={busy || !selectedPlan || historyStatus !== 'ready'}>{busy ? 'Registrando…' : 'Registrar renovación'}</button></div>
      </form>
    </dialog>}
  </main>
}
