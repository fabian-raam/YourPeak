import ClientToast from '../components/ClientToast'
import { useEffect, useMemo, useRef, useState } from 'react'
import Icon from '../components/Icon'
import { generateRoutine, getRoutine, getRoutineExercises, saveRoutine } from '../services/routine'
import { normalizeSearch } from '../utils/format'
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
  origen: item.origen === 'IA' ? 'IA' : 'USUARIO',
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
const exerciseCount = count => `${count} ${count === 1 ? 'ejercicio' : 'ejercicios'}`

function normalizeWeek(days = []) {
  const week = Array.from({ length: 7 }, (_, index) => ({
    diaSemana: index + 1, disponible: false, horaInicio: null, horaFin: null, ejercicios: [],
  }))
  days.forEach(day => {
    if (day.diaSemana >= 1 && day.diaSemana <= 7) week[day.diaSemana - 1] = normalizeDay(day)
  })
  return week
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
      origen: exercise.origen,
    })),
  }))
}

export default function ClientRoutine({ onExpired, onNavigate, initialDate }) {
  const dialog = useRef(null)
  const searchField = useRef(null)
  const addTrigger = useRef(null)
  const weekStrip = useRef(null)
  const [addedId, setAddedId] = useState(null)
  const [today] = useState(() => atMidnight(new Date()))
  const [selectedDate, setSelectedDate] = useState(() => {
    const date = new Date(`${initialDate}T00:00:00`)
    return Number.isNaN(date.getTime()) ? today : date
  })
  const weekStart = mondayOf(selectedDate)
  const [days, setDays] = useState(normalizeWeek)
  const [savedDays, setSavedDays] = useState(normalizeWeek)
  const [catalog, setCatalog] = useState([])
  const [selectedExerciseId, setSelectedExerciseId] = useState('')
  const [draft, setDraft] = useState(formDraft(null))
  const [search, setSearch] = useState('')
  const [showExerciseDialog, setShowExerciseDialog] = useState(false)
  const [loadFailed, setLoadFailed] = useState(false)
  const [loadAttempt, setLoadAttempt] = useState(0)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [generating, setGenerating] = useState(false)
  const [proposal, setProposal] = useState(null)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    let current = true
    Promise.all([getRoutine(), getRoutineExercises()]).then(([routine, exercises]) => {
      if (!current) return
      const loaded = normalizeWeek(routine)
      setDays(loaded)
      setSavedDays(loaded)
      setCatalog(exercises.filter(exercise => exercise.estado?.toUpperCase() === 'ACTIVO'))
    }).catch(err => {
      if (!current) return
      if (err.status === 401) onExpired()
      else {
        setLoadFailed(true)
        setError(err.message || 'No se pudo cargar tu rutina.')
      }
    }).finally(() => { if (current) setLoading(false) })
    return () => { current = false }
  }, [onExpired, loadAttempt])

  const selectedDayNumber = (selectedDate.getDay() + 6) % 7 + 1
  const selectedDay = days[selectedDayNumber - 1]
  const weekEnd = new Date(weekStart)
  weekEnd.setDate(weekEnd.getDate() + 6)
  const weekLabel = new Intl.DateTimeFormat('es-PE', {
    day: 'numeric', month: 'long', year: 'numeric',
  }).formatRange(weekStart, weekEnd)
  const dirty = JSON.stringify(days) !== JSON.stringify(savedDays)
  const availableCount = days.filter(day => day.disponible).length
  const filteredCatalog = useMemo(() => {
    const query = normalizeSearch(search.trim())
    const dayExerciseIds = new Set(selectedDay?.ejercicios.map(item => item.idEjercicio))
    return catalog.filter(exercise => !dayExerciseIds.has(exercise.idEjercicio)
      && normalizeSearch(`${exercise.nombre} ${exercise.grupoMuscular ?? ''} ${exercise.equipamiento ?? ''}`).includes(query))
  }, [catalog, search, selectedDay])
  const selectedExercise = filteredCatalog.find(item => String(item.idEjercicio) === selectedExerciseId)

  useEffect(() => {
    if (showExerciseDialog && dialog.current && !dialog.current.open) {
      dialog.current.showModal()
      searchField.current.focus()
    }
  }, [showExerciseDialog])

  useEffect(() => {
    const strip = weekStrip.current
    const active = strip?.querySelector('[aria-pressed="true"]')
    if (!active || strip.scrollWidth <= strip.clientWidth) return
    const left = active.offsetLeft - 4
    const right = left + active.offsetWidth + 8
    if (left < strip.scrollLeft) strip.scrollLeft = left
    else if (right > strip.scrollLeft + strip.clientWidth) strip.scrollLeft = right - strip.clientWidth
  }, [selectedDate])

  function retry() {
    setError('')
    setLoadFailed(false)
    setLoading(true)
    setLoadAttempt(value => value + 1)
  }

  function selectDate(date) {
    setSelectedDate(date)
    setAddedId(null)
  }

  function openPicker() {
    setSearch('')
    setSelectedExerciseId('')
    setDraft(formDraft(null))
    setShowExerciseDialog(true)
  }

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
    const nextSelected = new Date(selectedDate)
    nextSelected.setDate(nextSelected.getDate() + offset * 7)
    selectDate(nextSelected)
  }

  function updateExerciseSelection(value) {
    setSelectedExerciseId(value)
    const exercise = catalog.find(item => String(item.idEjercicio) === value)
    setDraft(formDraft(exercise))
  }

  function addExercise(event) {
    event.preventDefault()
    if (!selectedExercise || !selectedDay?.disponible || saving) return
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
      origen: 'USUARIO',
    }
    const dayIndex = selectedDayNumber - 1
    setDays(current => current.map((day, index) => index === dayIndex
      ? { ...day, ejercicios: [...day.ejercicios, prescription].map((item, order) => ({ ...item, orden: order + 1 })) }
      : day))
    setSelectedExerciseId('')
    setDraft(formDraft(null))
    setSearch('')
    setAddedId(prescription.idEjercicio)
    dialog.current.close()
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
    if (saving || !dirty) return
    setSaving(true)
    setError('')
    setNotice('')
    try {
      const saved = normalizeWeek(await saveRoutine(toPayload(days)))
      setDays(saved)
      setSavedDays(saved)
      setAddedId(null)
      setProposal(null)
      setNotice('Tu rutina semanal quedó guardada.')
    } catch (err) {
      if (err.status === 401) onExpired()
      else setError(err.message || 'No se pudo guardar tu rutina.')
    } finally {
      setSaving(false)
    }
  }

  function reset() {
    setAddedId(null)
    setDays(savedDays.map(day => ({ ...day, ejercicios: day.ejercicios.map(item => ({ ...item })) })))
    setProposal(null)
    setError('')
    setNotice('')
  }

  // La propuesta reemplaza el borrador en pantalla; solo se guarda con «Guardar rutina».
  async function generate() {
    setGenerating(true)
    setError('')
    setNotice('')
    try {
      const result = await generateRoutine()
      setDays(normalizeWeek(result.dias))
      setProposal({ fuente: result.fuente, mensaje: result.mensaje })
      setAddedId(null)
    } catch (err) {
      if (err.status === 401) onExpired()
      else setError(err.message || 'No se pudo generar la rutina.')
    } finally {
      setGenerating(false)
    }
  }

  const dateLabel = formatDate(selectedDate, { weekday: 'long', day: 'numeric', month: 'long' })
  const isToday = selectedDate.getTime() === today.getTime()
  const currentWeekDays = days.map((day, index) => {
    const date = new Date(weekStart)
    date.setDate(date.getDate() + index)
    return { day, date, index }
  })

  return <section className="client-routine" aria-labelledby="client-page-title">

    {error && <p className="routine-error" role="alert">{error}</p>}
    {loadFailed && <button className="client-text-link" type="button" onClick={retry}>Reintentar</button>}
    {notice && <ClientToast title="Rutina guardada" message={notice} onClose={() => setNotice('')} />}

    <section className={`routine-ai${proposal ? ' has-proposal' : ''}`} aria-live="polite">
      <span className="routine-ai-icon"><Icon name="sparkles" /></span>
      <div>
        <strong>{proposal ? (proposal.fuente === 'GEMINI' ? 'Propuesta generada con IA' : 'Propuesta generada por reglas') : 'Rutina con IA'}</strong>
        <p>{proposal ? proposal.mensaje : 'Te proponemos una semana de entrenamiento según tu perfil, tus horarios y el catálogo del gimnasio. Podrás revisarla antes de guardarla.'}</p>
        <small>Rutina orientativa. Si tienes lesiones o dudas de salud, consulta con un entrenador.</small>
      </div>
      <button type="button" className="routine-ai-button" onClick={generate}
        disabled={loading || loadFailed || saving || generating || availableCount === 0}
        title={availableCount === 0 ? 'Configura tu disponibilidad para generar una rutina' : undefined}>
        <Icon name={generating ? 'clock' : 'sparkles'} />{generating ? 'Generando…' : proposal ? 'Generar otra' : 'Generar con IA'}
      </button>
    </section>

    <section className="routine-week" aria-label="Calendario semanal" aria-busy={loading}>
      <div className="routine-week-navigation">
        <button type="button" aria-label="Semana anterior" onClick={() => changeWeek(-1)}>‹</button>
        <button type="button" aria-label="Semana siguiente" onClick={() => changeWeek(1)}>›</button>
        <strong>Semana del {weekLabel}</strong>
      </div>
      <div className="routine-week-days" ref={weekStrip}>
        {currentWeekDays.map(({ day, date, index }) => {
          const selected = date.toDateString() === selectedDate.toDateString()
          const isCurrentDay = date.getTime() === today.getTime()
          const hasRoutine = day.disponible && day.ejercicios.length > 0
          const label = hasRoutine ? exerciseCount(day.ejercicios.length) : 'Sin ejercicios'
          return <button key={day.diaSemana} type="button"
            className={`routine-day-card${selected ? ' selected' : ''}${!day.disponible ? ' rest' : ''}`}
            aria-pressed={selected} disabled={loading || loadFailed} onClick={() => selectDate(date)}>
            <span className="routine-day-top"><strong>{shortWeekdays[index]}</strong>{isCurrentDay && <small>HOY</small>}</span>
            <span className="routine-day-number">{date.getDate()}</span>
            <span className="routine-day-title">{day.disponible ? (hasRoutine ? day.ejercicios[0].nombre : 'Disponible') : 'Descanso'}</span>
            {day.disponible && <span className="routine-day-status">{label}</span>}
          </button>
        })}
        {loading && <div className="routine-loading" role="status">Cargando tu rutina…</div>}
      </div>
    </section>

    {!loading && !loadFailed && <div className="routine-day-content" key={selectedDate.getTime()}>
      {selectedDay.disponible ? <>
        <section className={`routine-workout-banner${selectedDay.ejercicios.length ? '' : ' is-available'}`}>
          <div><span>{dateLabel}{isToday ? ' · Hoy' : ''}</span>
            <h2>{selectedDay.ejercicios.length ? selectedDay.ejercicios[0].grupoMuscular || 'Entrenamiento' : 'Día disponible'}</h2></div>
          <div className="routine-workout-meta">
            <span><Icon name="dumbbell" />{exerciseCount(selectedDay.ejercicios.length)}</span>
            {selectedDay.horaInicio && selectedDay.horaFin && <span><Icon name="clock" />{formatTime(selectedDay.horaInicio)} – {formatTime(selectedDay.horaFin)}</span>}
          </div>
        </section>
        <section className="routine-exercise-panel" aria-labelledby="routine-exercises-heading">
          <div className="routine-exercise-heading">
            <h3 id="routine-exercises-heading">Ejercicios del día</h3>
            <button type="button" ref={addTrigger} className="routine-add-button" onClick={openPicker} disabled={saving || generating}>
              <Icon name="plus" />Agregar ejercicio
            </button>
          </div>
          {selectedDay.ejercicios.length ? <ol className="routine-exercise-list">
            {selectedDay.ejercicios.map((exercise, index) => <li key={exercise.idEjercicio}
              className={addedId === exercise.idEjercicio ? 'routine-exercise-added' : undefined}>
              <span className="routine-exercise-index" aria-hidden="true">{index + 1}</span>
              <div className="routine-exercise-description">
                <strong>{exercise.nombre}{exercise.origen === 'IA' && <span className="routine-ai-tag" title="Propuesto por el generador de rutinas">IA</span>}</strong>
                <small>{exercise.series} {exercise.series === 1 ? 'serie' : 'series'} × {exercise.tipoMedicion === 'TIEMPO' ? `${exercise.segundos} s` : `${exercise.repeticiones} rep.`}
                  {' · '}{exercise.descansoSegundos === 0 ? 'Sin descanso' : `Descanso ${exercise.descansoSegundos} s`}</small>
              </div>
              {exercise.grupoMuscular && <span className="routine-exercise-tag">{exercise.grupoMuscular}</span>}
              <button type="button" className="routine-remove-exercise" disabled={saving}
                onClick={() => removeExercise(exercise.idEjercicio)} aria-label={`Quitar ${exercise.nombre}`}><Icon name="close" /></button>
            </li>)}
          </ol> : <div className="routine-empty-day"><span><Icon name="dumbbell" /></span>
            <strong>Aún no agregaste ejercicios</strong><p>Elige ejercicios del catálogo para armar tu sesión de este día.</p>
          </div>}
        </section>
      </> : <section className="routine-rest-panel">
        <div><span className="routine-rest-kicker">{dateLabel}</span>
          <h2>Día de descanso</h2><p>No marcaste este día como disponible para entrenar.</p>
          <button type="button" onClick={() => onNavigate?.('disponibilidad')}>Configurar disponibilidad <Icon name="arrow" /></button>
        </div>
        <span className="routine-rest-icon"><Icon name="pause" /></span>
      </section>}

      {!selectedDay.ejercicios.length && nextWorkout && <section className="routine-next-workout">
        <span className="routine-next-icon"><Icon name="dumbbell" /></span>
        <div><small>Próximo entrenamiento disponible</small>
          <strong>{formatDate(nextWorkout.date, { weekday: 'long', day: 'numeric' })} · {exerciseCount(nextWorkout.day.ejercicios.length)}</strong>
          {nextWorkout.day.horaInicio && nextWorkout.day.horaFin && <span>{formatTime(nextWorkout.day.horaInicio)} – {formatTime(nextWorkout.day.horaFin)}</span>}</div>
        <button type="button" onClick={() => selectDate(nextWorkout.date)}>Ver ese día <Icon name="arrow" /></button>
      </section>}
    </div>}

    {showExerciseDialog && <dialog ref={dialog} className="routine-exercise-dialog" aria-labelledby="exercise-picker-title"
      onClose={() => { setShowExerciseDialog(false); addTrigger.current?.focus() }}>
      <header className="routine-exercise-dialog-heading">
        <div><span>{dateLabel}</span><h2 id="exercise-picker-title">Agregar ejercicio</h2>
          <p>Busca un ejercicio activo y define cómo lo realizarás este día.</p></div>
        <button type="button" className="routine-remove-exercise" aria-label="Cerrar selector" onClick={() => dialog.current.close()}><Icon name="close" /></button>
      </header>
      <form className="routine-add-form" onSubmit={addExercise}>
        <label className="routine-add-search"><Icon name="search" /><input ref={searchField} type="search" value={search}
          onChange={event => setSearch(event.target.value)} onKeyDown={event => { if (event.key === 'Enter') event.preventDefault() }}
          placeholder="Filtrar ejercicios" aria-label="Filtrar ejercicios disponibles" /></label>
        <fieldset className="routine-catalog">
          <legend>Ejercicio</legend>
          <p className="routine-result-count" role="status">{filteredCatalog.length} {filteredCatalog.length === 1 ? 'resultado' : 'resultados'}</p>
          <div className="routine-catalog-results">
            {filteredCatalog.map(exercise => {
              const checked = selectedExercise?.idEjercicio === exercise.idEjercicio
              return <label className="routine-catalog-option" key={exercise.idEjercicio}>
                <input className="sr-only" type="radio" name="routine-exercise" value={exercise.idEjercicio} checked={checked}
                  onChange={event => updateExerciseSelection(event.target.value)} required />
                <span className="routine-catalog-icon"><Icon name={checked ? 'check' : 'dumbbell'} /></span>
                <span className="routine-catalog-label"><strong>{exercise.nombre}</strong>
                  <small>{[exercise.grupoMuscular, exercise.equipamiento].filter(Boolean).join(' · ')}</small></span>
                {exercise.nivelExperiencia && <span className="routine-exercise-tag">{exercise.nivelExperiencia}</span>}
              </label>
            })}
            {!filteredCatalog.length && <p className="routine-catalog-empty">
              {search.trim() ? 'No hay coincidencias.' : 'No hay más ejercicios disponibles para este día.'}
            </p>}
          </div>
        </fieldset>
        <fieldset className="routine-prescription" disabled={!selectedExercise || saving}>
          <legend>Cómo lo realizarás</legend>
          <div className="routine-add-fields">
            <label>Series<input type="number" min="1" max="100" value={draft.series}
              onChange={event => setDraft(current => ({ ...current, series: event.target.value }))} required /></label>
            {selectedExercise?.tipoMedicion?.toUpperCase() === 'TIEMPO'
              ? <label>Segundos<input type="number" min="1" max="32767" value={draft.segundos}
                onChange={event => setDraft(current => ({ ...current, segundos: event.target.value }))} required /></label>
              : <label>Repeticiones<input type="number" min="1" max="127" value={draft.repeticiones}
                onChange={event => setDraft(current => ({ ...current, repeticiones: event.target.value }))} required /></label>}
            <label>Descanso (s)<input type="number" min="0" max="32767" value={draft.descansoSegundos}
              onChange={event => setDraft(current => ({ ...current, descansoSegundos: event.target.value }))} required /></label>
          </div>
        </fieldset>
        <div className="routine-editor-actions">
          <button type="button" className="routine-reset" onClick={() => dialog.current.close()}>Cancelar</button>
          <button className="routine-add-button" type="submit" disabled={!selectedExercise || saving}><Icon name="plus" />Agregar a rutina</button>
        </div>
      </form>
    </dialog>}

    <footer className="routine-actions">
      <span>{availableCount} {availableCount === 1 ? 'día disponible' : 'días disponibles'} · La rutina se repite cada semana</span>
      <div><button type="button" className="routine-reset" disabled={!dirty || loading || saving || generating} onClick={reset}>Restablecer</button>
        <button type="button" className="routine-save" disabled={!dirty || loading || saving || generating} onClick={save}>
          <Icon name={saving ? 'clock' : 'check'} />{saving ? 'Guardando…' : 'Guardar rutina'}
        </button></div>
    </footer>
  </section>
}
