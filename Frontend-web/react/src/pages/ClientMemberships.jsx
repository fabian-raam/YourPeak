import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import Icon from '../components/Icon'
import { listActiveClients, listInactiveClients, listClientMemberships, listPlans, renewClientMembership } from '../services/memberships'
import { fullName, initials } from '../utils/format'
import './AdminPanel.css'
import './ClientMemberships.css'

const dateLabel = value => value ? new Date(`${value}T12:00:00`).toLocaleDateString('es-PE') : 'Sin período registrado'

export default function ClientMemberships({ onExpired, logoutError }) {
  const membershipDialog = useRef(null)
  const [clients, setClients] = useState([])
  const [inactiveClients, setInactiveClients] = useState([])
  const [scope, setScope] = useState('ACTIVO')
  const [plans, setPlans] = useState([])
  const [search, setSearch] = useState('')
  const [selected, setSelected] = useState(null)
  const [history, setHistory] = useState([])
  const [planId, setPlanId] = useState('')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const load = useCallback(async () => {
    try {
      const [people, inactivePeople, catalog] = await Promise.all([listActiveClients(), listInactiveClients(), listPlans()])
      setClients(people)
      setInactiveClients(inactivePeople)
      setPlans(catalog.filter(plan => plan.estado === 'ACTIVO'))
    } catch (err) {
      if (err.status === 401) onExpired()
      else setError(err.message)
    } finally { setLoading(false) }
  }, [onExpired])

  const reload = useCallback(async () => {
    setLoading(true); setError('')
    await load()
    setLoading(false)
  }, [load])

  // El efecto sincroniza la pantalla con la respuesta externa del backend.
  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { load() }, [load])

  useEffect(() => {
    if (selected && !membershipDialog.current.open) membershipDialog.current.showModal()
  }, [selected])

  const visible = useMemo(() => {
    const needle = search.trim().toLocaleLowerCase('es')
    const source = scope === 'INACTIVO' ? inactiveClients : clients
    return source.filter(person => `${fullName(person)} ${person.correo}`.toLocaleLowerCase('es').includes(needle))
  }, [clients, inactiveClients, scope, search])

  async function openClient(person) {
    setSelected(person); setError(''); setNotice(''); setHistory([]); setPlanId(plans[0] ? String(plans[0].idPlanMembresia) : '')
    try { setHistory(await listClientMemberships(person.idCliente)) }
    catch (err) { if (err.status === 401) onExpired(); else setError(err.message) }
  }

  async function submitRenewal(event) {
    event.preventDefault()
    if (!selected || !planId) return
    setBusy(true); setError(''); setNotice('')
    try {
      await renewClientMembership(selected.idCliente, { idPlanMembresia: Number(planId), observacion: null })
      setHistory(await listClientMemberships(selected.idCliente))
      setNotice('La renovación quedó registrada y se agregó al historial.')
      await load()
    } catch (err) {
      if (err.status === 401) onExpired()
      else setError(err.message)
    } finally { setBusy(false) }
  }

  return <main className="admin-main membership-main client-membership-main">
    <div className="admin-heading"><div><span className="eyebrow">02 / MEMBRESÍAS</span><h1>Clientes y períodos</h1><p>Consulta la vigencia y registra renovaciones sin perder días ya pagados.</p></div>
      <button className="secondary-button" onClick={reload} disabled={loading}><Icon name="refresh" />{loading ? 'Actualizando…' : 'Actualizar'}</button>
    </div>
    {(error || logoutError) && <p className="alert" role="alert">{error || logoutError}</p>}
    {notice && <p className="alert success" role="status">{notice}</p>}
    <section className="users-panel" aria-busy={loading}>
      <div className="users-panel-heading"><h2>{scope === 'INACTIVO' ? 'Clientes inactivos' : 'Clientes activos'}</h2><span className="live-label"><i /> {visible.length} clientes</span></div>
      <div className="client-state-tabs" aria-label="Filtrar clientes por estado">
        <button type="button" aria-pressed={scope === 'ACTIVO'} onClick={() => setScope('ACTIVO')}>Activos ({clients.length})</button>
        <button type="button" aria-pressed={scope === 'INACTIVO'} onClick={() => setScope('INACTIVO')}>Inactivos ({inactiveClients.length})</button>
      </div>
      <div className="users-toolbar"><label className="search-input"><Icon name="search" /><input type="search" aria-label="Buscar clientes" placeholder="Buscar por nombre o correo…" value={search} onChange={event => setSearch(event.target.value)} /></label></div>
      {loading ? <div className="table-state" role="status"><span className="loading-dot" /> Consultando clientes…</div>
        : !visible.length ? <div className="table-state"><Icon name="users" /><h3>{search ? 'No encontramos coincidencias' : scope === 'INACTIVO' ? 'No hay clientes inactivos' : 'No hay clientes activos'}</h3><p>{search ? 'Prueba con otro nombre o correo.' : scope === 'INACTIVO' ? 'Los clientes cuyo período venció aparecerán aquí para renovarlos.' : 'Los clientes activos aparecerán aquí.'}</p></div>
          : <div className="users-table-scroll"><table className="users-table plans-table"><caption className="sr-only">Clientes y membresías</caption><thead><tr><th scope="col">Cliente</th><th scope="col">Vigencia</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead><tbody>
            {visible.map(person => <tr key={person.idCliente}>
              <td><div className="user-cell"><span className="avatar">{initials(person)}</span><div><strong>{fullName(person)}</strong><span>{person.correo}</span></div></div></td>
              <td>{dateLabel(person.fechaVencimiento)}</td>
              <td><span className={`status-badge ${person.estadoUsuario === 'ACTIVO' ? 'membership-current' : 'membership-expired'}`}><i />{person.estadoUsuario === 'INACTIVO' ? 'Inactivo' : 'Activo'}</span><small className="membership-expiry-note">{person.estadoMembresia === 'VENCIDA' ? 'Membresía vencida' : person.estadoMembresia === 'SIN_MEMBRESIA' ? 'Sin período' : 'Membresía vigente'}</small></td>
              <td><button className="review-button" onClick={() => openClient(person)}>{scope === 'INACTIVO' ? 'Renovar' : 'Ver / renovar'} <Icon name="arrow" /></button></td>
            </tr>)}
          </tbody></table></div>}
    </section>
    {selected && <dialog ref={membershipDialog} className="approval-dialog plan-dialog client-membership-dialog" aria-labelledby="client-membership-title"
      onCancel={event => { event.preventDefault(); if (!busy) setSelected(null) }}>
      <div className="dialog-top"><span className="eyebrow">HISTORIAL DE MEMBRESÍA</span><button className="icon-button" aria-label="Cerrar" disabled={busy} onClick={() => setSelected(null)}>×</button></div>
      <h2 id="client-membership-title">{fullName(selected)}</h2><p className="client-membership-email">{selected.correo}</p>
      {error && <p className="alert" role="alert">{error}</p>}{notice && <p className="alert success" role="status">{notice}</p>}
      <div className="membership-history">{history.length ? history.map(period => <article key={period.idMembresia}>
        <div><strong>{period.nombrePlan}</strong><span>{dateLabel(period.fechaInicio)} — {dateLabel(period.fechaVencimiento)}</span></div><b className={`membership-tag ${period.estadoPeriodo.toLowerCase()}`}>{period.estadoPeriodo}</b>
      </article>) : <p>No hay períodos registrados.</p>}</div>
      <form onSubmit={submitRenewal}>
        <label htmlFor="renewal-plan">Plan para renovar</label>
        <select id="renewal-plan" value={planId} onChange={event => setPlanId(event.target.value)} disabled={busy || !plans.length} required>
          {plans.map(plan => <option key={plan.idPlanMembresia} value={plan.idPlanMembresia}>{plan.nombre} · {plan.duracionMeses} {plan.duracionMeses === 1 ? 'mes' : 'meses'}</option>)}
        </select>
        {!plans.length && <p className="plan-info">No hay planes activos disponibles para renovar.</p>}
        <p className="plan-info">La renovación anticipada comenzará al día siguiente del vencimiento actual.</p>
        <div className="dialog-actions"><button type="button" className="secondary-button" disabled={busy} onClick={() => setSelected(null)}>Cerrar</button><button className="primary compact" disabled={busy || !plans.length}>{busy ? 'Guardando…' : 'Registrar renovación'}</button></div>
      </form>
    </dialog>}
  </main>
}
