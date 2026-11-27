import { useCallback, useEffect, useState } from 'react'
import Brand from '../components/Brand'
import Icon from '../components/Icon'
import { getAccessRequest, requestAccessAgain } from '../services/accessRequest'
import { fullName, initials } from '../utils/format'
import './AccountStatus.css'

// Spring guarda las fechas de la solicitud en hora de Lima, sin zona: se muestran tal cual.
const dateTime = value => value ? new Intl.DateTimeFormat('es-PE', {
  day: 'numeric', month: 'short', year: 'numeric', hour: 'numeric', minute: '2-digit',
}).format(new Date(value)) : ''

const requestStatus = {
  PENDIENTE: { label: 'En revisión', tone: 'pending', icon: 'clock' },
  APROBADA: { label: 'Aprobada', tone: 'ok', icon: 'check' },
  RECHAZADA: { label: 'Rechazada', tone: 'rejected', icon: 'close' },
}

function accountState(user) {
  const estado = user.estado?.toUpperCase()
  if (estado === 'PENDIENTE') return {
    tone: 'pending', icon: 'clock', badge: 'En revisión', title: 'Tu solicitud está en revisión',
    text: 'Recibimos tu solicitud. Un administrador del gimnasio la revisará y activará tu cuenta con un plan de membresía.',
  }
  if (estado === 'RECHAZADO') return {
    tone: 'rejected', icon: 'close', badge: 'Solicitud rechazada', title: 'Tu solicitud no fue aprobada',
    text: 'Revisa el motivo que dejó el administrador. Cuando lo resuelvas, puedes volver a enviar tu solicitud.',
  }
  if (estado === 'ACTIVO' && user.membresiaVigente === false) return {
    tone: 'expired', icon: 'card', badge: 'Membresía vencida', title: 'Tu membresía venció',
    text: 'Tu cuenta sigue registrada. Acércate a la administración del gimnasio para renovar tu plan.',
  }
  if (estado === 'INACTIVO') return {
    tone: 'expired', icon: 'power', badge: 'Cuenta inactiva', title: 'Tu cuenta está inactiva',
    text: 'Tu último periodo terminó. La administración del gimnasio puede reactivarla al renovar tu membresía.',
  }
  return {
    tone: 'expired', icon: 'shield', badge: 'Acceso no habilitado', title: 'Tu acceso aún no está disponible',
    text: 'Consulta con la administración del gimnasio para revisar el estado de tu cuenta.',
  }
}

/** Pasos visibles de la solicitud: enviada, revisión y resultado. */
function Steps({ estado }) {
  const rejected = estado === 'RECHAZADO'
  const steps = [
    { label: 'Solicitud enviada', done: true },
    { label: 'Revisión del administrador', done: estado !== 'PENDIENTE', current: estado === 'PENDIENTE' },
    { label: rejected ? 'Rechazada' : 'Cuenta activa', done: rejected || estado === 'ACTIVO', failed: rejected },
  ]
  return <ol className="as-steps">
    {steps.map((step, index) => <li key={step.label}
      className={step.failed ? 'is-failed' : step.current ? 'is-current' : step.done ? 'is-done' : ''}>
      <span>{step.failed ? <Icon name="close" /> : step.done ? <Icon name="check" /> : index + 1}</span>
      {step.label}
    </li>)}
  </ol>
}

export default function AccountStatus({ user, onRefresh, onLogout, busy, error, onExpired }) {
  const [tab, setTab] = useState('estado')
  const [data, setData] = useState(null)
  const [loadError, setLoadError] = useState('')
  const [checking, setChecking] = useState(false)
  const [notice, setNotice] = useState('')
  const [message, setMessage] = useState('')
  const [sending, setSending] = useState(false)
  const [formError, setFormError] = useState('')
  const state = accountState(user)
  const estado = user.estado?.toUpperCase()
  const isClient = user.rol === 'CLIENTE'
  const history = data?.historial || []
  const latest = history[0]
  const lastRejection = history.find(item => item.estado === 'RECHAZADA')

  const load = useCallback(async () => {
    if (!isClient) return
    try {
      setData(await getAccessRequest())
      setLoadError('')
    } catch (err) {
      if (err.status === 401) onExpired()
      else setLoadError(err.message)
    }
  }, [isClient, onExpired])

  useEffect(() => { load() }, [load, user.estado])

  // Mientras la solicitud está en revisión se consulta el estado cada minuto.
  useEffect(() => {
    if (estado !== 'PENDIENTE') return undefined
    const timer = setInterval(() => { if (!document.hidden) onRefresh() }, 60_000)
    return () => clearInterval(timer)
  }, [estado, onRefresh])

  async function refresh() {
    setChecking(true); setNotice('')
    try {
      const current = await onRefresh()
      const status = current?.estado?.toUpperCase()
      if (status === 'PENDIENTE') setNotice('Tu solicitud sigue en revisión.')
      else if (status === 'RECHAZADO') setNotice('Tu solicitud fue revisada. Mira el motivo abajo.')
      else if (status === 'INACTIVO') setNotice('Tu cuenta sigue inactiva.')
      else if (status === 'ACTIVO' && current.membresiaVigente === false) setNotice('Todavía no tienes una membresía vigente.')
      await load()
    } finally { setChecking(false) }
  }

  async function sendAgain(event) {
    event.preventDefault()
    setSending(true); setFormError(''); setNotice('')
    try {
      setData(await requestAccessAgain(message.trim()))
      setMessage('')
      await onRefresh()
      setNotice('Enviamos tu nueva solicitud. Te avisaremos aquí cuando sea revisada.')
    } catch (err) {
      if (err.status === 401) onExpired()
      else { setFormError(err.message); load() }
    } finally { setSending(false) }
  }

  const tabButton = (id, label, count) => <button type="button" role="tab" id={`as-tab-${id}`}
    aria-selected={tab === id} aria-controls={`as-panel-${id}`} onClick={() => setTab(id)}>
    {label}{count ? <i>{count}</i> : null}
  </button>

  return <div className="as-page">
    <header className="as-top">
      <Brand href="/" onNavigate={event => event.preventDefault()} />
      <div className="as-user">
        <span className="as-avatar" aria-hidden="true">{initials(user) || 'YP'}</span>
        <div><strong>{fullName(user)}</strong><small>{user.correo}</small></div>
        <button type="button" className="as-logout" disabled={busy || sending} onClick={onLogout}>
          <Icon name="logout" /> <span>Cerrar sesión</span>
        </button>
      </div>
    </header>

    <main className="as-main">
      <p className="as-kicker">Mi cuenta</p>
      <h1 tabIndex={-1}>Mi solicitud<span>.</span></h1>

      {isClient && <div className="as-tabs" role="tablist" aria-label="Secciones de mi solicitud">
        {tabButton('estado', 'Estado')}
        {tabButton('historial', 'Historial', history.length)}
      </div>}

      {(error || loadError) && <p className="as-alert" role="alert">{error || loadError}</p>}
      {notice && <p className="as-notice" role="status">{notice}</p>}

      {tab === 'estado' ? <div id="as-panel-estado" role="tabpanel" aria-labelledby="as-tab-estado" className="as-grid">
        <section className={`as-hero is-${state.tone}`}>
          <span className="as-badge"><Icon name={state.icon} /> {state.badge}</span>
          <h2>{state.title}</h2>
          <p>{state.text}</p>
          {['PENDIENTE', 'RECHAZADO'].includes(estado) && <Steps estado={estado} />}
          <div className="as-hero-actions">
            <button type="button" className="as-primary" disabled={checking || busy} onClick={refresh}>
              <Icon name="refresh" /> {checking ? 'Consultando…' : 'Consultar estado'}
            </button>
            {estado === 'PENDIENTE' && <small>Se actualiza solo cada minuto.</small>}
          </div>
        </section>

        <div className="as-side">
          {estado === 'RECHAZADO' && <section className="as-card">
            <h3>Motivo del rechazo</h3>
            <blockquote className="as-reason">{lastRejection?.motivoRechazo || 'El administrador no indicó un motivo.'}</blockquote>
            {lastRejection?.fechaResolucion && <small className="as-muted">Revisada el {dateTime(lastRejection.fechaResolucion)}</small>}

            <form className="as-form" onSubmit={sendAgain}>
              <label htmlFor="as-message">Mensaje para el administrador <span>(opcional)</span></label>
              <textarea id="as-message" maxLength={300} rows={3} value={message} disabled={!data?.puedeSolicitar || sending}
                placeholder="Ej.: Ya realicé el pago en recepción." onChange={event => setMessage(event.target.value)} />
              <div className="as-form-foot">
                <small className="as-muted">{data ? data.puedeSolicitar
                  ? `Te quedan ${data.solicitudesRestantesHoy} de ${data.limiteDiario} solicitudes hoy.`
                  : `Alcanzaste el máximo de ${data.limiteDiario} solicitudes por hoy. Podrás intentarlo mañana.` : ''}</small>
                <small className="as-muted">{message.length}/300</small>
              </div>
              {formError && <p className="as-alert" role="alert">{formError}</p>}
              <button type="submit" className="as-primary" disabled={!data?.puedeSolicitar || sending}>
                <Icon name="arrow" /> {sending ? 'Enviando…' : 'Volver a solicitar acceso'}
              </button>
            </form>
          </section>}

          {estado === 'PENDIENTE' && <section className="as-card">
            <h3>Mientras tanto</h3>
            <ul className="as-tips">
              <li><Icon name="card" /><div><strong>Ten a la mano tu pago</strong><small>La administración asigna tu plan al aprobarte.</small></div></li>
              <li><Icon name="clock" /><div><strong>Revisión en horario de atención</strong><small>Suele resolverse el mismo día.</small></div></li>
              <li><Icon name="info" /><div><strong>Si te rechazan, verás el motivo aquí</strong><small>Podrás corregirlo y volver a solicitar.</small></div></li>
            </ul>
          </section>}

          {isClient && latest && <section className="as-card">
            <h3>Solicitud actual</h3>
            <dl className="as-facts">
              <div><dt>Número</dt><dd>#{latest.numero}</dd></div>
              <div><dt>Estado</dt><dd>{requestStatus[latest.estado]?.label || latest.estado}</dd></div>
              <div><dt>Enviada</dt><dd>{dateTime(latest.fechaSolicitud)}</dd></div>
              <div><dt>Revisada</dt><dd>{latest.fechaResolucion ? dateTime(latest.fechaResolucion) : 'Pendiente'}</dd></div>
            </dl>
          </section>}
        </div>
      </div> : <section id="as-panel-historial" role="tabpanel" aria-labelledby="as-tab-historial" className="as-card as-history">
        {!history.length ? <p className="as-muted">Aún no hay solicitudes registradas.</p> : <ol>
          {history.map(item => {
            const status = requestStatus[item.estado] || { label: item.estado, tone: 'pending', icon: 'info' }
            return <li key={item.numero} className={`is-${status.tone}`}>
              <span className="as-dot"><Icon name={status.icon} /></span>
              <div>
                <div className="as-history-head">
                  <strong>Solicitud #{item.numero}</strong>
                  <span className={`as-pill is-${status.tone}`}>{status.label}</span>
                </div>
                <small className="as-muted">Enviada el {dateTime(item.fechaSolicitud)}
                  {item.fechaResolucion && ` · revisada el ${dateTime(item.fechaResolucion)}`}</small>
                {item.mensajeCliente && <p><b>Tu mensaje:</b> {item.mensajeCliente}</p>}
                {item.motivoRechazo && <p><b>Motivo:</b> {item.motivoRechazo}</p>}
              </div>
            </li>
          })}
        </ol>}
      </section>}
    </main>
  </div>
}
