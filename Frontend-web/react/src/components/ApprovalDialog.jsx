import { useEffect, useId, useRef, useState } from 'react'
import { approvePending, getPending, rejectPending } from '../services/admin'
import { listPlans } from '../services/memberships'
import { birthDate, fullName, initials, registrationDate } from '../utils/format'
import Icon from './Icon'
import './ApprovalDialog.css'

const reasons = ['No se registró el pago de la inscripción.', 'Los datos no coinciden con los de la institución.', 'Debe acercarse a recepción para validar su identidad.']

export default function ApprovalDialog({ clientId, onClose, onApproved, onRejected, onExpired, onStale }) {
  const dialog = useRef(null)
  const titleId = useId()
  const [person, setPerson] = useState(null)
  const [plans, setPlans] = useState([])
  const [planId, setPlanId] = useState('')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [stale, setStale] = useState(false)
  const [rejecting, setRejecting] = useState(false)
  const [reason, setReason] = useState('')

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

  async function reject() {
    if (busy || !person || stale) return
    const motivo = reason.trim()
    if (motivo.length < 5) { setError('Escribe el motivo del rechazo (mínimo 5 caracteres).'); return }
    setBusy(true); setError('')
    try {
      await rejectPending(clientId, motivo)
      onRejected(person)
    } catch (err) {
      if (err.status === 401) { onExpired(); return }
      setError([404, 409].includes(err.status)
        ? 'Esta solicitud ya no está pendiente. Actualiza la lista para consultar su estado.' : err.message)
      if ([403, 404, 409].includes(err.status)) setStale(true)
    } finally { setBusy(false) }
  }

  return <dialog ref={dialog} className="approval-dialog user-approval-dialog" aria-labelledby={titleId}
    onCancel={event => { event.preventDefault(); close() }}>
    <div className="dialog-top"><span className="eyebrow">REVISIÓN DE SOLICITUD</span>
      <button className="icon-button" aria-label="Cerrar detalle" disabled={busy} onClick={close}><Icon name="close" /></button>
    </div>
    <h2 id={titleId}>{rejecting ? 'Rechazar solicitud' : 'Revisar solicitud'}</h2>
    <div className="approval-content">
    {loading ? <p role="status">Cargando los datos del usuario…</p> : person && <>
      <div className="dialog-person">
        <span className="avatar avatar-large" aria-hidden="true">{initials(person)}</span>
        <div className="approval-identity"><h3>{fullName(person)}</h3><p>{person.correo}</p></div>
        <span className="status-badge"><i aria-hidden="true" />Pendiente</span>
      </div>
      <dl className="detail-grid">
        <div><dt>Fecha de nacimiento</dt><dd>{birthDate(person.fechaNacimiento)}</dd></div>
        <div><dt>Fecha de registro</dt><dd>{registrationDate(person.fechaAlta)}</dd></div>
        <div><dt>Intento</dt><dd>{person.numeroSolicitud > 1 ? `${person.numeroSolicitud}.ª solicitud` : 'Primera solicitud'}</dd></div>
      </dl>
      {(person.mensajeCliente || person.motivoRechazoAnterior) && <div className="approval-history">
        {person.motivoRechazoAnterior && <p><b>Rechazo anterior:</b> {person.motivoRechazoAnterior}</p>}
        {person.mensajeCliente && <p><b>Mensaje del cliente:</b> {person.mensajeCliente}</p>}
      </div>}
      {rejecting ? <div className="field approval-plan-field">
        <label htmlFor="reject-reason">Motivo del rechazo</label>
        <textarea id="reject-reason" className="approval-reason" rows={3} maxLength={500} value={reason} disabled={busy}
          placeholder="El cliente verá este motivo en su cuenta." onChange={event => { setReason(event.target.value); setError('') }} />
        <div className="approval-reason-chips">
          {reasons.map(text => <button type="button" key={text} disabled={busy} onClick={() => { setReason(text); setError('') }}>{text}</button>)}
        </div>
        <small>El cliente podrá corregirlo y volver a solicitar acceso (máximo 3 solicitudes por día).</small>
      </div> : <div className="field approval-plan-field">
        <label htmlFor="approval-plan">Plan de membresía</label>
        <select id="approval-plan" value={planId} onChange={event => { setPlanId(event.target.value); setError('') }} disabled={busy || !plans.length}>
          {plans.map(plan => <option key={plan.idPlanMembresia} value={plan.idPlanMembresia}>
            {plan.nombre} · {plan.duracionMeses} {plan.duracionMeses === 1 ? 'mes' : 'meses'}
          </option>)}
        </select>
        <small>El período empieza hoy o después de la última membresía vigente.</small>
      </div>}
    </>}
    {error && <p className="alert" role="alert">{error}</p>}
    </div>
    <footer className="approval-footer">
      <p className="approval-explanation"><Icon name={rejecting ? 'info' : 'shield'} /> {rejecting
        ? 'La cuenta no se activará. El cliente verá el motivo en «Mi solicitud».'
        : 'Al aprobar, activarás su cuenta y podrá acceder a Your Peak.'}</p>
      <div className="dialog-actions">
      {rejecting
        ? <button className="secondary-button" disabled={busy} onClick={() => { setRejecting(false); setError('') }}>Volver</button>
        : <button className="secondary-button" disabled={busy} onClick={close}>{stale ? 'Volver a la lista' : 'Cancelar'}</button>}
      {person && !stale && !rejecting && <button className="secondary-button reject-button" disabled={busy || loading} onClick={() => { setRejecting(true); setError('') }}>
        <Icon name="close" />Rechazar
      </button>}
      {person && !stale && (rejecting
        ? <button className="primary compact reject-confirm" disabled={busy} onClick={reject}>
          <Icon name="close" />{busy ? 'Rechazando…' : 'Confirmar rechazo'}
        </button>
        : <button className="primary compact" disabled={busy || loading || !plans.length} onClick={approve}>
          <Icon name="check" />{busy ? 'Aprobando…' : 'Aprobar usuario'}
        </button>)}
      </div>
    </footer>
  </dialog>
}
