import ClientToast from '../components/ClientToast'
import { useEffect, useState } from 'react'
import Icon from '../components/Icon'
import { getAvailability, saveAvailability } from '../services/availability'
import './ClientAvailability.css'

const weekdays = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo']
const dayInitials = 'LMXJVSD'
const emptyWeek = () => weekdays.map((nombre, index) => ({
  diaSemana: index + 1,
  disponible: false,
  horaInicio: '',
  horaFin: '',
  nombre,
}))

function fromApi(dias) {
  const week = emptyWeek()
  for (const item of dias) {
    const day = week[item.diaSemana - 1]
    if (!day) continue
    day.disponible = item.disponible
    day.horaInicio = item.horaInicio?.slice(0, 5) || ''
    day.horaFin = item.horaFin?.slice(0, 5) || ''
  }
  return week
}

function duration(day) {
  if (!day.disponible || !day.horaInicio || !day.horaFin) return 0
  const [startHour, startMinute] = day.horaInicio.split(':').map(Number)
  const [endHour, endMinute] = day.horaFin.split(':').map(Number)
  return endHour * 60 + endMinute - startHour * 60 - startMinute
}

function readableDuration(minutes) {
  if (!minutes) return '0 min'
  const hours = Math.floor(minutes / 60)
  const remaining = minutes % 60
  if (!hours) return `${remaining} min`
  if (!remaining) return `${hours} h`
  return `${hours} h ${remaining} min`
}

function toPayload(week) {
  return week.map(day => ({
    diaSemana: day.diaSemana,
    disponible: day.disponible,
    horaInicio: day.disponible ? day.horaInicio : null,
    horaFin: day.disponible ? day.horaFin : null,
  }))
}

function openTimePicker(input) {
  if (!input) return
  if (typeof input.showPicker === 'function') {
    try {
      input.showPicker()
      return
    } catch {
      // El control sigue siendo accesible con foco si el navegador no permite showPicker.
    }
  }
  input.focus()
}

function AvailabilityTimeField({ day, start, disabled, invalid, errorId, onChange }) {
  const key = start ? 'horaInicio' : 'horaFin'
  const label = start ? 'Desde' : 'Hasta'
  const id = `availability-${key}-${day.diaSemana}`
  return <div className="availability-time-field">
    <label htmlFor={id}>{label}<span className="sr-only"> · {day.nombre}</span></label>
    <span className="availability-time-input">
      <input id={id} type="time" value={day[key]} disabled={disabled}
        aria-invalid={invalid || undefined} aria-describedby={invalid ? errorId : undefined}
        onClick={event => openTimePicker(event.currentTarget)}
        onChange={event => onChange(day.diaSemana, { [key]: event.target.value })} />
      <button type="button" aria-label={`Elegir hora ${start ? 'de inicio' : 'final'} para ${day.nombre}`} disabled={disabled}
        onClick={event => openTimePicker(event.currentTarget.parentElement.querySelector('input'))}><Icon name="clock" /></button>
    </span>
  </div>
}

export default function ClientAvailability({ onExpired }) {
  const [week, setWeek] = useState(emptyWeek)
  const [savedWeek, setSavedWeek] = useState(emptyWeek)
  const [loading, setLoading] = useState(true)
  const [loadFailed, setLoadFailed] = useState(false)
  const [attempt, setAttempt] = useState(0)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [noticeVisible, setNoticeVisible] = useState(false)

  useEffect(() => {
    let current = true
    getAvailability().then(result => {
      if (!current) return
      const loaded = fromApi(result)
      setWeek(loaded)
      setSavedWeek(loaded)
    }).catch(err => {
      if (!current) return
      if (err.status === 401) onExpired()
      else {
        setError(err.message || 'No se pudo consultar tu disponibilidad.')
        setLoadFailed(true)
      }
    }).finally(() => { if (current) setLoading(false) })
    return () => { current = false }
  }, [attempt, onExpired])

  const schedule = week.map((day, index) => {
    const saved = savedWeek[index]
    const minutes = duration(day)
    const modified = day.disponible !== saved.disponible
      || (day.disponible && (day.horaInicio !== saved.horaInicio || day.horaFin !== saved.horaFin))
    const invalid = day.disponible && minutes <= 0
    return { day, minutes, modified, invalid }
  })
  const totalMinutes = schedule.reduce((sum, item) => sum + Math.max(0, item.minutes), 0)
  const availableCount = week.filter(day => day.disponible).length
  const pendingCount = schedule.filter(item => item.modified).length
  const invalidCount = schedule.filter(item => item.invalid).length
  const busy = loading || saving

  function updateDay(dayNumber, changes) {
    setError('')
    setNoticeVisible(false)
    setWeek(days => days.map(day => day.diaSemana === dayNumber ? { ...day, ...changes } : day))
  }

  function reset() {
    setWeek(savedWeek)
    setError('')
    setNoticeVisible(false)
  }

  function retry() {
    setError('')
    setLoadFailed(false)
    setLoading(true)
    setAttempt(previous => previous + 1)
  }

  async function save(event) {
    event.preventDefault()
    if (busy || loadFailed || !pendingCount || invalidCount) return
    setSaving(true)
    setError('')
    setNoticeVisible(false)
    try {
      const saved = fromApi(await saveAvailability(toPayload(week)))
      setWeek(saved)
      setSavedWeek(saved)
      setNoticeVisible(true)
    } catch (err) {
      if (err.status === 401) onExpired()
      else setError(err.message || 'No se pudo guardar tu disponibilidad.')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <p className="availability-loading" role="status">Cargando tus horarios.</p>

  return <section className="client-availability" aria-labelledby="client-page-title">
    {noticeVisible && <ClientToast title="Disponibilidad guardada" message="Tu disponibilidad semanal se guardó correctamente."
      onClose={() => setNoticeVisible(false)} />}
    {error && <p className="availability-error" role="alert">{error}</p>}
    {loadFailed ? <button className="availability-reset" type="button" onClick={retry}>Reintentar</button> : <>
      <section className="availability-summary" aria-labelledby="availability-summary-title">
        <div>
          <h2 id="availability-summary-title">Resumen semanal</h2>
          <div className="availability-summary-totals">
            <span><strong>{availableCount}</strong> {availableCount === 1 ? 'día disponible' : 'días disponibles'}</span>
            <span><strong>{readableDuration(totalMinutes)}</strong> por semana</span>
          </div>
        </div>
        <ul className="availability-summary-days" aria-label="Disponibilidad por día">
          {schedule.map(({ day, invalid }, index) => <li key={day.diaSemana}
            className={invalid ? 'is-invalid' : day.disponible ? 'is-available' : undefined} title={day.nombre}>
            <span aria-hidden="true">{dayInitials[index]}</span>
            <span className="sr-only">{day.nombre} · {invalid ? 'Horario por corregir' : day.disponible ? 'Disponible' : 'No disponible'}</span>
          </li>)}
        </ul>
        {invalidCount > 0 && <p className="availability-summary-warning" role="status">
          Sin contar {invalidCount} {invalidCount === 1 ? 'horario por corregir' : 'horarios por corregir'}
        </p>}
      </section>

      <form className="availability-panel" onSubmit={save} aria-busy={saving}>
        <header className="availability-panel-heading"><h2>Horarios semanales</h2></header>
        <div className="availability-day-list">
          {schedule.map(({ day, minutes, modified, invalid }) => {
            const errorId = `availability-error-${day.diaSemana}`
            const disabled = !day.disponible || saving
            return <div className={`availability-day${day.disponible ? ' is-available' : ''}${invalid ? ' is-invalid' : ''}`} key={day.diaSemana}>
              <div className="availability-day-title">
                <label className="availability-toggle">
                  <input type="checkbox" role="switch" aria-label={`${day.nombre} disponible`} checked={day.disponible} disabled={saving}
                    onChange={event => updateDay(day.diaSemana, {
                      disponible: event.target.checked,
                      horaInicio: event.target.checked ? day.horaInicio : '',
                      horaFin: event.target.checked ? day.horaFin : '',
                    })} />
                  <span aria-hidden="true"><i /></span>
                </label>
                <strong className="availability-day-name">{day.nombre}</strong>
                {modified && <span className="availability-modified">Modificado</span>}
              </div>
              <AvailabilityTimeField day={day} start disabled={disabled} invalid={invalid && !day.horaInicio}
                errorId={errorId} onChange={updateDay} />
              <AvailabilityTimeField day={day} disabled={disabled} invalid={invalid && (!day.horaFin || Boolean(day.horaInicio))}
                errorId={errorId} onChange={updateDay} />
              <span className="availability-day-total">
                {day.disponible ? (invalid ? '' : readableDuration(minutes)) : 'No disponible'}
              </span>
              {invalid && <p className="availability-field-error" id={errorId}>
                <Icon name="info" />{!day.horaInicio || !day.horaFin
                  ? 'Completa las horas de inicio y fin.'
                  : 'La hora de fin debe ser posterior a la de inicio.'}
              </p>}
            </div>
          })}
        </div>
        <footer className="availability-actions">
          <div className="availability-actions-meta">
            <span className="availability-timezone">La semana empieza el lunes · Horario local</span>
            <span className="availability-pending" role="status">
              {pendingCount > 0 && `${pendingCount} ${pendingCount === 1 ? 'cambio' : 'cambios'} sin guardar`}
            </span>
          </div>
          <div className="availability-action-buttons">
            <button className="availability-reset" type="button" onClick={reset} disabled={!pendingCount || saving}>Restablecer</button>
            <button className="availability-save" type="submit" disabled={!pendingCount || saving || invalidCount > 0}>
              <Icon name={saving ? 'clock' : 'check'} />{saving ? 'Guardando.' : 'Guardar disponibilidad'}
            </button>
          </div>
        </footer>
      </form>
    </>}
  </section>
}
