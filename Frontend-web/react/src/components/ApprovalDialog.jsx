import { useEffect, useId, useRef, useState } from 'react'
import { approvePending, getPending } from '../services/admin'
import { listPlans } from '../services/memberships'
import { birthDate, fullName, initials, registrationDate } from '../utils/format'
import Icon from './Icon'

export default function ApprovalDialog({ clientId, onClose, onApproved, onExpired, onStale }) {
  const dialog = useRef(null)
  const titleId = useId()
  const [person, setPerson] = useState(null)
  const [plans, setPlans] = useState([])
  const [planId, setPlanId] = useState('')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [stale, setStale] = useState(false)

  useEffect(() => {
    dialog.current.showModal()
    let active = true
    Promise.all([getPending(clientId), listPlans()]).then(([data, catalog]) => {
      if (!active) return
      const available = catalog.filter(plan => plan.estado === 'ACTIVO')
      setPerson(data)
      setPlans(available)
      setPlanId(available.length ? String(available[0].idPlanMembresia) : '')
      if (!available.length) setError('No hay planes activos. Activa o crea un plan antes de aprobar al cliente.')
    })
      .catch(err => {
        if (!active) return
        if (err.status === 401) { onExpired(); return }
        setError(err.message)
        if ([403, 404, 409].includes(err.status)) setStale(true)
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [clientId, onExpired])

  function close() {
    if (busy) return
    if (stale) onStale()
    onClose()
  }

  async function approve() {
    if (busy || !person || stale) return
    if (!planId) { setError('Selecciona un plan activo para continuar.'); return }
    setBusy(true); setError('')
    try {
      const result = await approvePending(clientId, {
        idPlanMembresia: Number(planId),
        observacion: null,
      })
      onApproved(person, result)
    } catch (err) {
      if (err.status === 401) { onExpired(); return }
      setError([404, 409].includes(err.status)
        ? 'Esta solicitud ya no está pendiente. Actualiza la lista para consultar su estado.' : err.message)
      if ([403, 404, 409].includes(err.status)) setStale(true)
    } finally { setBusy(false) }
  }

  return <dialog ref={dialog} className="approval-dialog" aria-labelledby={titleId}
    onCancel={event => { event.preventDefault(); close() }}>
    <div className="dialog-top"><span className="eyebrow">REVISIÓN DE SOLICITUD</span>
      <button className="icon-button" aria-label="Cerrar detalle" disabled={busy} onClick={close}><Icon name="close" /></button>
    </div>
    <h2 id={titleId}>Un paso más hacia su meta.</h2>
    {loading ? <p role="status">Cargando los datos del usuario…</p> : person && <>
      <div className="dialog-person"><span className="avatar avatar-large">{initials(person)}</span><div><h3>{fullName(person)}</h3><span className="status-badge">Pendiente</span></div></div>
      <dl className="detail-grid">
        <div><dt>Correo institucional</dt><dd>{person.correo}</dd></div>
        <div><dt>Fecha de nacimiento</dt><dd>{birthDate(person.fechaNacimiento)}</dd></div>
        <div><dt>Fecha de registro</dt><dd>{registrationDate(person.fechaAlta)}</dd></div>
        <div><dt>N.º de solicitud</dt><dd>#{person.idCliente}</dd></div>
      </dl>
      <div className="field approval-plan-field">
        <label htmlFor="approval-plan">Plan de membresía</label>
        <select id="approval-plan" value={planId} onChange={event => { setPlanId(event.target.value); setError('') }} disabled={busy || !plans.length}>
          {plans.map(plan => <option key={plan.idPlanMembresia} value={plan.idPlanMembresia}>
            {plan.nombre} · {plan.duracionMeses} {plan.duracionMeses === 1 ? 'mes' : 'meses'}
          </option>)}
        </select>
        <small>El período empieza hoy o después de la última membresía vigente.</small>
      </div>
      <p className="approval-explanation"><Icon name="shield" /> Al aprobar, activarás su cuenta y podrá acceder a Your Peak.</p>
    </>}
    {error && <p className="alert" role="alert">{error}</p>}
    <div className="dialog-actions">
      <button className="secondary-button" disabled={busy} onClick={close}>{stale ? 'Volver a la lista' : 'Cancelar'}</button>
      {person && !stale && <button className="primary compact" disabled={busy || loading || !plans.length} onClick={approve}>
        <Icon name="check" />{busy ? 'Aprobando…' : 'Aprobar usuario'}
      </button>}
    </div>
  </dialog>
}
