import { useEffect, useMemo, useState } from 'react'
import Icon from '../components/Icon'
import { getAvailability, saveAvailability } from '../services/availability'
import './ClientAvailability.css'

const weekdays = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo']
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

export default function ClientAvailability({ onExpired }) {
  const [week, setWeek] = useState(emptyWeek)
  const [savedWeek, setSavedWeek] = useState(emptyWeek)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
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
      else setError(err.message || 'No se pudo consultar tu disponibilidad.')
    }).finally(() => { if (current) setLoading(false) })
    return () => { current = false }
  }, [onExpired])

  const totalMinutes = useMemo(() => week.reduce((sum, day) => sum + Math.max(0, duration(day)), 0), [week])
  const availableCount = week.filter(day => day.disponible).length
  const dirty = JSON.stringify(week) !== JSON.stringify(savedWeek)

  function updateDay(dayNumber, changes) {
    setError('')
    setNoticeVisible(false)
    setWeek(days => days.map(day => day.diaSemana === dayNumber ? { ...day, ...changes } : day))
  }

  function reset() {
    setWeek(savedWeek.map(day => ({ ...day })))
    setError('')
    setNoticeVisible(false)
  }

  async function save(event) {
    event.preventDefault()
    const invalid = week.find(day => day.disponible && (!day.horaInicio || !day.horaFin || duration(day) <= 0))
    if (invalid) {
      setError(`Revisa el horario del ${invalid.nombre.toLowerCase()}: define una hora final posterior a la inicial.`)
      return
    }

    setSaving(true)
    setError('')
    setNoticeVisible(false)
    try {
      const saved = fromApi(await saveAvailability(toPayload(week)))
      setWeek(saved)
      setSavedWeek(saved)
      setNotice('Tu disponibilidad semanal se guardó correctamente.')
      setNoticeVisible(true)
    } catch (err) {
      if (err.status === 401) onExpired()
      else setError(err.message || 'No se pudo guardar tu disponibilidad.')
    } finally {
      setSaving(false)
    }
  }

  return <main className="client-availability">
    <header className="availability-page-heading">
      <h1>Disponibilidad</h1>
      <p>Indica en qué horarios puedes entrenar.</p>
    </header>

    {noticeVisible && <div className="availability-notice" role="status">
      <Icon name="check" /><span>{notice}</span>
      <button type="button" aria-label="Cerrar mensaje" onClick={() => setNoticeVisible(false)}><Icon name="close" /></button>
    </div>}
    {error && <p className="availability-error" role="alert">{error}</p>}

    <form className="availability-panel" onSubmit={save} aria-busy={loading || saving}>
      <div className="availability-panel-heading">
        <div><h2>Horarios semanales</h2><p>Elige los días y rangos en los que estás disponible.</p></div>
        <span className="availability-summary"><Icon name="clock" />
          {availableCount} {availableCount === 1 ? 'día disponible' : 'días disponibles'} · {readableDuration(totalMinutes)} por semana
        </span>
      </div>

      {!loading && availableCount === 0 && <div className="availability-empty-note">
        <Icon name="clock" /><span><strong>Sin horarios definidos</strong>Activa los días en que puedes entrenar y elige un intervalo.</span>
      </div>}

      <div className="availability-day-list">
        {week.map(day => <div className={`availability-day${day.disponible ? ' is-available' : ''}`} key={day.diaSemana}>
          <label className="availability-toggle" aria-label={`${day.disponible ? 'Desactivar' : 'Activar'} ${day.nombre}`}>
            <input type="checkbox" checked={day.disponible} disabled={loading || saving}
              onChange={event => updateDay(day.diaSemana, {
                disponible: event.target.checked,
                horaInicio: event.target.checked ? day.horaInicio : '',
                horaFin: event.target.checked ? day.horaFin : '',
              })} />
            <span aria-hidden="true"><i /></span>
          </label>
          <strong className="availability-day-name">{day.nombre}</strong>
          <div className="availability-time-field"><label htmlFor={`start-${day.diaSemana}`}>Desde</label>
            <span className="availability-time-input"><input id={`start-${day.diaSemana}`} type="time" value={day.horaInicio}
              disabled={!day.disponible || loading || saving}
              onClick={event => openTimePicker(event.currentTarget)}
              onChange={event => updateDay(day.diaSemana, { horaInicio: event.target.value })} />
              <button type="button" aria-label={`Elegir hora de inicio para ${day.nombre}`} disabled={!day.disponible || loading || saving}
                onClick={event => openTimePicker(event.currentTarget.parentElement.querySelector('input'))}><Icon name="clock" /></button></span></div>
          <div className="availability-time-field"><label htmlFor={`end-${day.diaSemana}`}>Hasta</label>
            <span className="availability-time-input"><input id={`end-${day.diaSemana}`} type="time" value={day.horaFin}
              disabled={!day.disponible || loading || saving}
              onClick={event => openTimePicker(event.currentTarget)}
              onChange={event => updateDay(day.diaSemana, { horaFin: event.target.value })} />
              <button type="button" aria-label={`Elegir hora final para ${day.nombre}`} disabled={!day.disponible || loading || saving}
                onClick={event => openTimePicker(event.currentTarget.parentElement.querySelector('input'))}><Icon name="clock" /></button></span></div>
          <span className={`availability-day-total${day.disponible ? ' active' : ''}`}>
            {!day.disponible ? 'No disponible' : duration(day) > 0 ? readableDuration(duration(day)) : 'Completa horario'}
          </span>
        </div>)}
        {loading && <div className="availability-loading" role="status">Cargando tus horarios…</div>}
      </div>

      <footer className="availability-actions">
        <span className="availability-timezone">La semana empieza el lunes · Horario local</span>
        <div><button className="availability-reset" type="button" onClick={reset} disabled={!dirty || loading || saving}>Restablecer</button>
          <button className="availability-save" type="submit" disabled={!dirty || loading || saving}>
            <Icon name={saving ? 'clock' : 'check'} />{saving ? 'Guardando…' : 'Guardar disponibilidad'}
          </button></div>
      </footer>
    </form>
  </main>
}
