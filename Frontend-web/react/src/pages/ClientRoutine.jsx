import { useEffect, useMemo, useState } from 'react'
import Icon from '../components/Icon'
import { getRoutine, getRoutineExercises, saveRoutine } from '../services/routine'
import './ClientRoutine.css'

const shortWeekdays = ['LUN', 'MAR', 'MIÉ', 'JUE', 'VIE', 'SÁB', 'DOM']
const atMidnight = date => new Date(date.getFullYear(), date.getMonth(), date.getDate())
const mondayOf = date => {
  const monday = atMidnight(date)
  monday.setDate(monday.getDate() - (monday.getDay() + 6) % 7)
  return monday
}
const formatDate = (date, options) => new Intl.DateTimeFormat('es-PE', options).format(date)
const normalizeExercise = item => ({
  idEjercicio: item.idEjercicio,
  nombre: item.nombre,
  grupoMuscular: item.grupoMuscular,
  tipoMedicion: item.tipoMedicion?.toUpperCase(),
  orden: item.orden,
  series: item.series,
  repeticiones: item.repeticiones,
  segundos: item.segundos,
  descansoSegundos: item.descansoSegundos,
})
const normalizeDay = day => ({
  diaSemana: day.diaSemana,
  disponible: day.disponible,
  horaInicio: day.horaInicio,
  horaFin: day.horaFin,
  ejercicios: [...(day.ejercicios || [])].sort((a, b) => a.orden - b.orden).map(normalizeExercise),
})
const formatTime = value => {
  if (!value) return ''
  const [hour, minute] = value.split(':').map(Number)
  return new Intl.DateTimeFormat('es-PE', { hour: 'numeric', minute: '2-digit' })
    .format(new Date(2020, 0, 1, hour, minute))
}
const formatDuration = item => item.tipoMedicion === 'TIEMPO'
  ? `${item.series} × ${item.segundos} s`
  : `${item.series} × ${item.repeticiones}`
const formatRest = seconds => seconds === 0 ? 'Sin descanso' : `${seconds} s`

function emptyDay(dayNumber) {
  return { diaSemana: dayNumber, disponible: false, horaInicio: null, horaFin: null, ejercicios: [] }
}

function formDraft(exercise) {
  const isTimed = exercise?.tipoMedicion?.toUpperCase() === 'TIEMPO'
  return { series: 3, repeticiones: isTimed ? '' : 10, segundos: isTimed ? 45 : '', descansoSegundos: 60 }
}

function toPayload(days) {
  return days.map(day => ({
    diaSemana: day.diaSemana,
    ejercicios: day.ejercicios.map(exercise => ({
      idEjercicio: exercise.idEjercicio,
      series: exercise.series,
      repeticiones: exercise.tipoMedicion === 'TIEMPO' ? null : exercise.repeticiones,
      segundos: exercise.tipoMedicion === 'TIEMPO' ? exercise.segundos : null,
      descansoSegundos: exercise.descansoSegundos,
    })),
  }))
}

export default function ClientRoutine({ onExpired, onNavigate }) {
  const [today] = useState(() => atMidnight(new Date()))
  const [weekStart, setWeekStart] = useState(() => mondayOf(today))
  const [selectedDate, setSelectedDate] = useState(today)
  const [days, setDays] = useState(() => Array.from({ length: 7 }, (_, index) => emptyDay(index + 1)))
  const [savedDays, setSavedDays] = useState(() => Array.from({ length: 7 }, (_, index) => emptyDay(index + 1)))
  const [catalog, setCatalog] = useState([])
  const [selectedExerciseId, setSelectedExerciseId] = useState('')
  const [draft, setDraft] = useState(formDraft(null))
  const [search, setSearch] = useState('')
  const [showExerciseScreen, setShowExerciseScreen] = useState(false)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    let current = true
    Promise.all([getRoutine(), getRoutineExercises()]).then(([routine, exercises]) => {
      if (!current) return
      const loaded = Array.from({ length: 7 }, (_, index) => emptyDay(index + 1))
      routine.forEach(day => {
        if (day.diaSemana >= 1 && day.diaSemana <= 7) loaded[day.diaSemana - 1] = normalizeDay(day)
      })
      setDays(loaded)
      setSavedDays(loaded)
      setCatalog(exercises.filter(exercise => exercise.estado?.toUpperCase() === 'ACTIVO'))
    }).catch(err => {
      if (!current) return
      if (err.status === 401) onExpired()
      else setError(err.message || 'No se pudo cargar tu rutina.')
    }).finally(() => { if (current) setLoading(false) })
    return () => { current = false }
  }, [onExpired])

  const selectedDayNumber = (selectedDate.getDay() + 6) % 7 + 1
  const selectedDay = days[selectedDayNumber - 1]
  const selectedExercise = catalog.find(item => String(item.idEjercicio) === selectedExerciseId)
  const weekEnd = new Date(weekStart)
  weekEnd.setDate(weekEnd.getDate() + 6)
  const dirty = JSON.stringify(days) !== JSON.stringify(savedDays)
  const availableCount = days.filter(day => day.disponible).length
  const filteredCatalog = useMemo(() => {
    const query = search.trim().toLocaleLowerCase('es-PE')
    const dayExerciseIds = new Set(selectedDay?.ejercicios.map(item => item.idEjercicio))
    return catalog.filter(exercise => !dayExerciseIds.has(exercise.idEjercicio)
      && `${exercise.nombre} ${exercise.grupoMuscular}`.toLocaleLowerCase('es-PE').includes(query))
  }, [catalog, search, selectedDay])

  let nextWorkout = null
  for (let step = 1; step <= 7; step += 1) {
    const dayNumber = (selectedDayNumber - 1 + step) % 7 + 1
    const candidate = days[dayNumber - 1]
    if (candidate?.disponible && candidate.ejercicios.length) {
      const date = new Date(selectedDate)
      date.setDate(date.getDate() + step)
      nextWorkout = { date, day: candidate }
      break
    }
  }

  function changeWeek(offset) {
    const nextStart = new Date(weekStart)
    nextStart.setDate(nextStart.getDate() + offset * 7)
    setWeekStart(nextStart)
    const nextSelected = new Date(selectedDate)
    nextSelected.setDate(nextSelected.getDate() + offset * 7)
    setSelectedDate(nextSelected)
  }

  function updateExerciseSelection(value) {
    setSelectedExerciseId(value)
    const exercise = catalog.find(item => String(item.idEjercicio) === value)
    setDraft(formDraft(exercise))
  }

  function addExercise(event) {
    event.preventDefault()
    if (!selectedExercise || !selectedDay?.disponible) return
    const isTimed = selectedExercise.tipoMedicion?.toUpperCase() === 'TIEMPO'
    const prescription = {
      idEjercicio: selectedExercise.idEjercicio,
      nombre: selectedExercise.nombre,
      grupoMuscular: selectedExercise.grupoMuscular,
      tipoMedicion: selectedExercise.tipoMedicion.toUpperCase(),
      series: Number(draft.series),
      repeticiones: isTimed ? null : Number(draft.repeticiones),
      segundos: isTimed ? Number(draft.segundos) : null,
      descansoSegundos: Number(draft.descansoSegundos),
    }
    const dayIndex = selectedDayNumber - 1
    setDays(current => current.map((day, index) => index === dayIndex
      ? { ...day, ejercicios: [...day.ejercicios, prescription].map((item, order) => ({ ...item, orden: order + 1 })) }
      : day))
    setSelectedExerciseId('')
    setDraft(formDraft(null))
    setSearch('')
    setShowExerciseScreen(false)
    setError('')
    setNotice('')
  }

  function removeExercise(idEjercicio) {
    const dayIndex = selectedDayNumber - 1
    setDays(current => current.map((day, index) => index === dayIndex
      ? { ...day, ejercicios: day.ejercicios.filter(item => item.idEjercicio !== idEjercicio)
        .map((item, order) => ({ ...item, orden: order + 1 })) }
      : day))
    setError('')
    setNotice('')
  }

  async function save(event) {
    event.preventDefault()
    setSaving(true)
    setError('')
    setNotice('')
    try {
      const result = await saveRoutine(toPayload(days))
      const saved = Array.from({ length: 7 }, (_, index) => emptyDay(index + 1))
      result.forEach(day => {
        if (day.diaSemana >= 1 && day.diaSemana <= 7) saved[day.diaSemana - 1] = normalizeDay(day)
      })
      setDays(saved)
      setSavedDays(saved)
      setNotice('Tu rutina semanal quedó guardada.')
    } catch (err) {
      if (err.status === 401) onExpired()
      else setError(err.message || 'No se pudo guardar tu rutina.')
    } finally {
      setSaving(false)
    }
  }

  function reset() {
    setDays(savedDays.map(day => ({ ...day, ejercicios: day.ejercicios.map(item => ({ ...item })) })))
    setError('')
    setNotice('')
  }

  const isToday = selectedDate.getTime() === today.getTime()
  const currentWeekDays = days.map((day, index) => {
    const date = new Date(weekStart)
    date.setDate(date.getDate() + index)
    return { day, date, index }
  })

  return <section className="client-routine" aria-labelledby="client-routine-title">
    <header className="client-header">
      <div><h1 id="client-routine-title">Mi rutina</h1><p>Tu planificación semanal de entrenamiento</p></div>
    </header>

    {error && <p className="routine-error" role="alert">{error}</p>}
    {notice && <div className="routine-notice" role="status"><Icon name="check" /><span>{notice}</span>
      <button type="button" aria-label="Cerrar mensaje" onClick={() => setNotice('')}><Icon name="close" /></button></div>}

    <section className="routine-week" aria-label="Calendario semanal" aria-busy={loading}>
      <div className="routine-week-toolbar">
        <div className="routine-week-navigation">
          <button type="button" aria-label="Semana anterior" onClick={() => changeWeek(-1)}>‹</button>
          <button type="button" aria-label="Semana siguiente" onClick={() => changeWeek(1)}>›</button>
          <strong>Semana del {formatDate(weekStart, { day: 'numeric' })} al {formatDate(weekEnd, { day: 'numeric', month: 'long' })}</strong>
        </div>
        <span>{formatDate(weekStart, { month: 'long', year: 'numeric' })}</span>
      </div>
      <div className="routine-week-days">
        {currentWeekDays.map(({ day, date, index }) => {
          const selected = date.toDateString() === selectedDate.toDateString()
          const isCurrentDay = date.getTime() === today.getTime()
          const hasRoutine = day.disponible && day.ejercicios.length > 0
          const label = hasRoutine ? `${day.ejercicios.length} ejercicios` : 'Sin ejercicios'
          return <button key={day.diaSemana} type="button"
            className={`routine-day-card${selected ? ' selected' : ''}${!day.disponible ? ' rest' : ''}`}
            aria-pressed={selected} disabled={loading} onClick={() => setSelectedDate(date)}>
            <span className="routine-day-top"><strong>{shortWeekdays[index]} {date.getDate()}</strong>{isCurrentDay && <small>HOY</small>}</span>
            <span className="routine-day-title">{day.disponible ? (hasRoutine ? day.ejercicios[0].nombre : 'Disponible') : 'Descanso'}</span>
            {day.disponible && <span className="routine-day-status">{label}</span>}
          </button>
        })}
        {loading && <div className="routine-loading" role="status">Cargando tu rutina…</div>}
      </div>
    </section>

    {!loading && selectedDay && (selectedDay.disponible ? <>
      <section className="routine-workout-banner">
        <div><span>{formatDate(selectedDate, { weekday: 'long', day: 'numeric', month: 'long' }).toUpperCase()}{isToday ? ' · HOY' : ''}</span>
          <h2>{selectedDay.ejercicios.length ? selectedDay.ejercicios[0].grupoMuscular || 'Entrenamiento' : 'Día disponible'}</h2></div>
        <div className="routine-workout-meta">
          <span><Icon name="dumbbell" />{selectedDay.ejercicios.length} ejercicios</span>
          <span><Icon name="clock" />{formatTime(selectedDay.horaInicio)} – {formatTime(selectedDay.horaFin)}</span>
        </div>
      </section>

      <section className="routine-exercise-panel">
        <div className="routine-table-heading"><h3>Ejercicios del día</h3><span>Series, repeticiones o tiempo y descanso</span></div>
        {selectedDay.ejercicios.length ? <div className="routine-table-scroll"><table>
          <thead><tr><th>N.º</th><th>Ejercicio</th><th>Series × reps / tiempo</th><th>Descanso</th><th><span className="sr-only">Acciones</span></th></tr></thead>
          <tbody>{selectedDay.ejercicios.map((exercise, index) => <tr key={exercise.idEjercicio}>
            <td>{index + 1}</td><td><strong>{exercise.nombre}</strong><small>{exercise.grupoMuscular}</small></td>
            <td>{formatDuration(exercise)}</td><td>{formatRest(exercise.descansoSegundos)}</td>
            <td><button type="button" className="routine-remove-exercise" disabled={saving}
              onClick={() => removeExercise(exercise.idEjercicio)} aria-label={`Quitar ${exercise.nombre}`}><Icon name="close" /></button></td>
          </tr>)}</tbody>
        </table></div> : <div className="routine-empty-day"><span><Icon name="dumbbell" /></span>
          <strong>Aún no agregaste ejercicios</strong><p>Elige ejercicios del catálogo para armar tu sesión de este día.</p>
        </div>}

        {showExerciseScreen ? <section className="routine-exercise-screen">
          <div className="routine-exercise-screen-heading">
            <button type="button" className="routine-back-button" onClick={() => setShowExerciseScreen(false)}>
              <Icon name="arrow" /> Volver a mi rutina
            </button>
            <span>{formatDate(selectedDate, { weekday: 'long', day: 'numeric', month: 'long' })}</span>
            <h2>Agregar ejercicio</h2>
            <p>Busca un ejercicio activo y define c?mo lo realizar?s este d?a.</p>
          </div>
          <form className="routine-add-form routine-add-screen-form" onSubmit={addExercise}>
            <label className="routine-add-search"><Icon name="search" /><input value={search} onChange={event => setSearch(event.target.value)}
              placeholder="Filtrar ejercicios" aria-label="Filtrar ejercicios disponibles" /></label>
            <div className="routine-add-fields">
              <label className="routine-exercise-select"><span>Ejercicio</span><select value={selectedExerciseId} onChange={event => updateExerciseSelection(event.target.value)} disabled={saving} required>
                <option value="">Selecciona un ejercicio</option>
                {filteredCatalog.map(exercise => <option key={exercise.idEjercicio} value={exercise.idEjercicio}>{exercise.nombre} ? {exercise.grupoMuscular}</option>)}
              </select></label>
              <label><span>Series</span><input type="number" min="1" max="100" value={draft.series}
                onChange={event => setDraft(current => ({ ...current, series: event.target.value }))} disabled={!selectedExercise || saving} required /></label>
              {selectedExercise?.tipoMedicion?.toUpperCase() === 'TIEMPO'
                ? <label><span>Segundos</span><input type="number" min="1" max="32767" value={draft.segundos}
                  onChange={event => setDraft(current => ({ ...current, segundos: event.target.value }))} disabled={saving} required /></label>
                : <label><span>Repeticiones</span><input type="number" min="1" max="127" value={draft.repeticiones}
                  onChange={event => setDraft(current => ({ ...current, repeticiones: event.target.value }))} disabled={!selectedExercise || saving} required /></label>}
              <label><span>Descanso (s)</span><input type="number" min="0" max="32767" value={draft.descansoSegundos}
                onChange={event => setDraft(current => ({ ...current, descansoSegundos: event.target.value }))} disabled={!selectedExercise || saving} required /></label>
            </div>
            {selectedExercise && <div className="routine-selected-exercise"><span><Icon name="dumbbell" /></span>
              <div><strong>{selectedExercise.nombre}</strong><small>{selectedExercise.grupoMuscular}</small></div></div>}
            {!filteredCatalog.length && <p className="routine-catalog-empty">No hay m?s ejercicios disponibles para este d?a.</p>}
            <div className="routine-editor-actions">
              <button type="button" className="routine-reset" onClick={() => setShowExerciseScreen(false)}>Cancelar</button>
              <button className="routine-add-button" type="submit" disabled={!selectedExercise || saving}><Icon name="plus" />Agregar a rutina</button>
            </div>
          </form>
        </section> : <div className="routine-add-entry">
          <div><h4>Completa tu entrenamiento</h4><p>Agrega ejercicios activos a la rutina de este d?a.</p></div>
          <button type="button" className="routine-add-button" onClick={() => { setSearch(''); setSelectedExerciseId(''); setDraft(formDraft(null)); setShowExerciseScreen(true) }}>
            <Icon name="plus" />Agregar ejercicio
          </button>
        </div>}
      </section>
    </> : <>
      <section className="routine-rest-panel">
        <span className="routine-rest-kicker">{formatDate(selectedDate, { weekday: 'long', day: 'numeric', month: 'long' }).toUpperCase()}</span>
        <h2>Día de descanso</h2><p>No marcaste este día como disponible para entrenar.</p>
        <button type="button" onClick={() => onNavigate?.('disponibilidad')}>Configurar disponibilidad <Icon name="arrow" /></button>
      </section>
      {nextWorkout && <section className="routine-next-workout">
        <span className="routine-next-icon"><Icon name="dumbbell" /></span>
        <div><small>Próximo entrenamiento disponible</small>
          <strong>{formatDate(nextWorkout.date, { weekday: 'long', day: 'numeric' })} · {nextWorkout.day.ejercicios.length} ejercicios</strong>
          <span>{formatTime(nextWorkout.day.horaInicio)} – {formatTime(nextWorkout.day.horaFin)}</span></div>
        <button type="button" onClick={() => setSelectedDate(nextWorkout.date)}>Ver ese día <Icon name="arrow" /></button>
      </section>}
    </>)}

    <footer className="routine-actions">
      <span>{availableCount} {availableCount === 1 ? 'día disponible' : 'días disponibles'} · La rutina se repite cada semana</span>
      <div><button type="button" className="routine-reset" disabled={!dirty || loading || saving} onClick={reset}>Restablecer</button>
        <button type="button" className="routine-save" disabled={!dirty || loading || saving} onClick={save}>
          <Icon name={saving ? 'clock' : 'check'} />{saving ? 'Guardando…' : 'Guardar rutina'}
        </button></div>
    </footer>
  </section>
}
