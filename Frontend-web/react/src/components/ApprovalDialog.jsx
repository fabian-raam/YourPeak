import { useEffect, useId, useRef, useState } from 'react'
import { approvePending, getPending } from '../services/admin'
import { birthDate, fullName, initials, registrationDate } from '../utils/format'
import Icon from './Icon'

export default function ApprovalDialog({ clientId, onClose, onApproved, onExpired, onStale }) {
  const dialog = useRef(null)
  const titleId = useId()
  const [person, setPerson] = useState(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [stale, setStale] = useState(false)

  useEffect(() => {
    dialog.current.showModal()
    let active = true
    getPending(clientId).then(data => { if (active) setPerson(data) })
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
    setBusy(true); setError('')
    try {
      await approvePending(clientId)
      onApproved(person)
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
      <p className="approval-explanation"><Icon name="shield" /> Al aprobar, activarás su cuenta y podrá acceder a Your Peak.</p>
    </>}
    {error && <p className="alert" role="alert">{error}</p>}
    <div className="dialog-actions">
      <button className="secondary-button" disabled={busy} onClick={close}>{stale ? 'Volver a la lista' : 'Cancelar'}</button>
      {person && !stale && <button className="primary compact" disabled={busy} onClick={approve}>
        <Icon name="check" />{busy ? 'Aprobando…' : 'Aprobar usuario'}
      </button>}
    </div>
  </dialog>
}
