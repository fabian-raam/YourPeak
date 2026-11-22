import { useEffect, useId, useRef, useState } from 'react'
import { createPlan, getPlan, renamePlan, setPlanStatus } from '../services/memberships'
import Icon from './Icon'

const durationLabel = months => `${months} ${months === 1 ? 'mes' : 'meses'}`

function PlanStatus({ active }) {
  return <span className={`status-badge plan-status ${active ? 'is-active' : 'is-inactive'}`}><i />{active ? 'Activo' : 'Inactivo'}</span>
}

export default function PlanDialog({ planId, initialMode, onClose, onSaved, onExpired, onStale }) {
  const dialog = useRef(null)
  const saving = useRef(false)
  const titleId = useId()
  const [plan, setPlan] = useState(null)
  const [name, setName] = useState('')
  const [duration, setDuration] = useState('')
  const [loading, setLoading] = useState(initialMode !== 'create')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [fields, setFields] = useState({})
  const [stale, setStale] = useState(false)
  const creating = initialMode === 'create'
  const editing = initialMode === 'edit'
  const changingStatus = initialMode === 'status'
  const formMode = creating || editing
  const isActive = plan?.estado === 'ACTIVO'
  const nextStatus = isActive ? 'INACTIVO' : 'ACTIVO'
  const statusAction = isActive ? 'Desactivar plan' : 'Activar plan'

  useEffect(() => {
    dialog.current.showModal()
    if (planId == null) return
    let active = true
    // Consulta el estado actual antes de editar o confirmar una baja lógica.
    getPlan(planId).then(data => {
      if (!active) return
      setPlan(data); setName(data.nombre); setDuration(String(data.duracionMeses))
    }).catch(err => {
      if (!active) return
      if (err.status === 401) { onExpired(); return }
      setError(err.message); setStale(true)
    }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [planId, onExpired])

  function close() {
    if (saving.current) return
    if (stale) onStale()
    onClose()
  }

  async function submit(event) {
    event.preventDefault()
    if (saving.current || stale || loading || (!creating && !plan)) return
    const errors = {}
    if (formMode && (!name.trim() || name.trim().length > 60)) errors.nombre = 'Ingresa un nombre de entre 1 y 60 caracteres.'
    const months = Number(duration)
    if (creating && (!Number.isInteger(months) || months < 1 || months > 127)) errors.duracionMeses = 'Ingresa un número entero entre 1 y 127 meses.'
    setFields(errors); setError('')
    if (Object.keys(errors).length) {
      document.getElementById(`plan-${Object.keys(errors)[0]}`)?.focus()
      return
    }
    // El bloqueo inmediato evita dos escrituras por un doble clic.
    saving.current = true; setBusy(true)
    try {
      const result = creating ? await createPlan({ nombre: name.trim(), duracionMeses: months })
        : editing ? await renamePlan(planId, name.trim()) : await setPlanStatus(planId, nextStatus)
      onSaved(result, creating ? 'created' : editing ? 'edited' : 'status')
    } catch (err) {
      if (err.status === 401) { onExpired(); return }
      setError(err.message)
      setFields(err.status === 409 && formMode ? { nombre: 'Ya existe un plan con ese nombre.' } : err.fields || {})
      if ([403, 404].includes(err.status)) setStale(true)
    } finally { saving.current = false; setBusy(false) }
  }

  const title = creating ? 'Crear plan de membresía' : editing ? 'Editar nombre del plan'
    : changingStatus ? loading ? 'Cambiar estado del plan' : statusAction : plan?.nombre || 'Detalle del plan'

  return <dialog ref={dialog} className="approval-dialog plan-dialog catalog-plan-dialog catalog-dialog" data-mode={initialMode} aria-labelledby={titleId}
    onCancel={event => { event.preventDefault(); close() }}>
    <div className="dialog-top"><span className="eyebrow">PLANES DE MEMBRESÍA</span>
      <button className="icon-button" aria-label="Cerrar plan" disabled={busy} onClick={close}><Icon name="close" /></button></div>
    <h2 id={titleId}>{title}</h2>
    {loading ? <p role="status">Consultando el plan…</p> : <form onSubmit={submit} noValidate>
      {formMode && !stale && <fieldset disabled={busy}>
        <div className="field"><label htmlFor="plan-nombre">Nombre del plan</label>
          <div className={`input-wrap ${fields.nombre ? 'invalid' : ''}`}><input id="plan-nombre" name="nombre" value={name}
            onChange={event => { setName(event.target.value); setError(''); setFields(previous => ({ ...previous, nombre: undefined })) }}
            maxLength={60} required autoFocus autoComplete="off" placeholder="Ej. Plan trimestral"
            aria-invalid={!!fields.nombre} aria-describedby={fields.nombre ? 'plan-name-error' : 'plan-name-hint'} /></div>
          <div className="plan-field-note"><small id="plan-name-hint">Nombre único, máximo 60 caracteres.</small><small className="plan-name-count">{name.length} / 60</small></div>
          {fields.nombre && <small className="field-error" id="plan-name-error">{fields.nombre}</small>}
        </div>
        {creating ? <div className="field"><label htmlFor="plan-duracionMeses">Duración en meses</label>
          <div className={`input-wrap ${fields.duracionMeses ? 'invalid' : ''}`}><input id="plan-duracionMeses" name="duracionMeses"
            type="number" min="1" max="127" step="1" required value={duration} placeholder="Ej. 3"
            onChange={event => { setDuration(event.target.value); setError(''); setFields(previous => ({ ...previous, duracionMeses: undefined })) }}
            aria-invalid={!!fields.duracionMeses} aria-describedby={fields.duracionMeses ? 'plan-duration-error' : 'plan-duration-hint'} />
            <span className="plan-duration-unit" aria-hidden="true">meses</span></div>
          <small className="plan-duration-hint" id="plan-duration-hint"><Icon name="lock" /> Entre 1 y 127 meses. La duración queda fija al crear el plan.</small>
          {fields.duracionMeses && <small className="field-error" id="plan-duration-error">{fields.duracionMeses}</small>}
        </div> : <div className="field">
          <span className="plan-fixed-label">Duración</span>
          <div className="plan-fixed-duration"><Icon name="lock" /> {durationLabel(plan.duracionMeses)}</div>
          <small>La duración queda fija al crear el plan.</small>
        </div>}
        {editing && <div className="plan-current-status"><strong>Estado actual</strong><PlanStatus active={isActive} /></div>}
        <p className="plan-info"><Icon name="info" /><span>{creating
          ? <>El nuevo plan se creará con estado <strong className="plan-active-text">ACTIVO</strong>.</>
          : 'El cambio de nombre también se verá en las membresías históricas vinculadas a este plan.'}</span></p>
      </fieldset>}
      {!formMode && plan && <>
        {changingStatus && <div className="plan-confirm-name"><Icon name="card" /><strong>{plan.nombre}</strong></div>}
        <dl className="detail-grid"><div><dt>N.º de plan</dt><dd>#{plan.idPlanMembresia}</dd></div>
          <div><dt>Duración fija</dt><dd className="plan-duration-hint"><Icon name="lock" />{durationLabel(plan.duracionMeses)}</dd></div>
          <div><dt>Estado actual</dt><dd><PlanStatus active={isActive} /></dd></div></dl>
        {changingStatus && <p className={`plan-info${isActive ? ' plan-warning' : ''}`}><Icon name={isActive ? 'shield' : 'info'} /><span>{isActive
          ? 'El plan quedará inactivo. Se conservarán las membresías históricas y sus periodos registrados.'
          : 'El plan volverá a estar activo y conservará su duración original.'}</span></p>}
      </>}
      {error && <p className="alert" role="alert">{error}</p>}
      <div className="dialog-actions">
        <button type="button" className="secondary-button" disabled={busy} onClick={close}>{stale ? 'Volver a la lista' : formMode || changingStatus ? 'Cancelar' : 'Cerrar'}</button>
        {!stale && (formMode || changingStatus) && <button className={changingStatus && isActive ? 'primary compact catalog-deactivate' : 'primary compact'} type="submit" disabled={busy}>
          {busy ? changingStatus ? isActive ? 'Desactivando…' : 'Activando…' : 'Guardando…' : creating ? 'Crear plan' : editing ? 'Guardar cambios' : statusAction}</button>}
      </div>
    </form>}
    {loading && <div className="dialog-actions"><button className="secondary-button" onClick={close}>Cancelar</button></div>}
  </dialog>
}
