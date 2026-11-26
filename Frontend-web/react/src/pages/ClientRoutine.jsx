import ClientToast from '../components/ClientToast'
import { useEffect, useMemo, useRef, useState } from 'react'
import Icon from '../components/Icon'
import { completeRoutineExercise, generateRoutine, getRoutineExercises, getRoutineSummary, saveRoutine } from '../services/routine'
import { exerciseImage } from '../utils/exerciseIllustrations'
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
  idPlanEjercicio: item.idPlanEjercicio,
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
const groupColors = {
  pecho: '#2166e8', espalda: '#0b0d12', piernas: '#f26b2d', hombros: '#7bc41f', brazos: '#9b6bff', core: '#2ec4b6', cardio: '#e5484d',
}
const groupColor = group => groupColors[normalizeSearch(group ?? '').trim()] ?? '#8e96a3'
const isoDate = date => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
const minutesBetween = (start, end) => {
  const toMinutes = value => value ? Number(value.slice(0, 2)) * 60 + Number(value.slice(3, 5)) : NaN
  const total = toMinutes(end) - toMinutes(start)
  return total > 0 ? total : null
}
const measureLabel = exercise => exercise.tipoMedicion === 'TIEMPO' ? `${exercise.series} × ${exercise.segundos} s` : `${exercise.series} × ${exercise.repeticiones}`
const dayTitle = day => {
  const groups = [...new Set(day.ejercicios.map(item => item.grupoMuscular).filter(Boolean))]
  return groups.length ? groups.slice(0, 2).join(' + ') : 'Entrenamiento'
}

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
  const [completionNotice, setCompletionNotice] = useState('')
  const [completedExerciseIds, setCompletedExerciseIds] = useState(() => new Set())
  const [completingExerciseId, setCompletingExerciseId] = useState(null)
  const [trainedDates, setTrainedDates] = useState(() => new Set())
  const [dragIndex, setDragIndex] = useState(null)

  useEffect(() => {
    let current = true
    Promise.all([getRoutineSummary(), getRoutineExercises()]).then(([summary, exercises]) => {
      if (!current) return
      const loaded = normalizeWeek(summary.dias)
      setCompletedExerciseIds(new Set(summary.realizadosHoy ?? []))
      setTrainedDates(new Set((summary.actividadReciente ?? []).map(item => item.fecha?.slice(0, 10))))
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

  /** Mueve un ejercicio del día seleccionado a otra posición; se guarda con «Guardar rutina». */
  function moveExercise(from, to) {
    const dayIndex = selectedDayNumber - 1
    setDays(current => current.map((day, index) => {
      if (index !== dayIndex || to < 0 || to >= day.ejercicios.length || from === to) return day
      const exercises = [...day.ejercicios]
      const [moved] = exercises.splice(from, 1)
      exercises.splice(to, 0, moved)
      return { ...day, ejercicios: exercises.map((item, order) => ({ ...item, orden: order + 1 })) }
    }))
    setNotice('')
  }

  function continueWorkout() {
    const pending = selectedDay.ejercicios.find(item => item.idPlanEjercicio && !completedExerciseIds.has(item.idPlanEjercicio))
    const target = pending && document.getElementById(`routine-exercise-${pending.idPlanEjercicio}`)
    target?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    target?.querySelector('.routine-complete-exercise')?.focus({ preventScroll: true })
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

  async function completeExercise(exercise) {
    const idPlanEjercicio = exercise.idPlanEjercicio
    if (!isToday || !idPlanEjercicio || completingExerciseId || completedExerciseIds.has(idPlanEjercicio)) return

    setCompletingExerciseId(idPlanEjercicio)
    setError('')
    setCompletionNotice('')
    const timed = exercise.tipoMedicion === 'TIEMPO'
    const series = Array.from({ length: exercise.series }, (_, index) => ({
      numeroSerie: index + 1,
      ...(timed ? { segundos: exercise.segundos } : { repeticiones: exercise.repeticiones }),
    }))

    try {
      await completeRoutineExercise(idPlanEjercicio, series)
      setCompletedExerciseIds(current => new Set(current).add(idPlanEjercicio))
      setCompletionNotice(`${exercise.nombre} quedó marcado como realizado hoy.`)
    } catch (err) {
      if (err.status === 401) onExpired()
      else if (err.status === 409 && err.message?.includes('ya fue registrado como completado hoy')) {
        setCompletedExerciseIds(current => new Set(current).add(idPlanEjercicio))
        setCompletionNotice(`${exercise.nombre} ya estaba registrado como realizado hoy.`)
      } else setError(err.message || 'No se pudo registrar el ejercicio realizado.')
    } finally {
      setCompletingExerciseId(null)
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
    {completionNotice && <ClientToast title="Avance registrado" message={completionNotice} onClose={() => setCompletionNotice('')} />}

    <div className="routine-toolbar">
      <div className="routine-week-navigation">
        <button type="button" aria-label="Semana anterior" onClick={() => changeWeek(-1)}>‹</button>
        <strong>Semana del {weekLabel}</strong>
        <button type="button" aria-label="Semana siguiente" onClick={() => changeWeek(1)}>›</button>
      </div>
    </div>

    <section className="routine-week" aria-label="Calendario semanal" aria-busy={loading}>
      <div className="routine-week-days" ref={weekStrip}>
        {currentWeekDays.map(({ day, date, index }) => {
          const selected = date.toDateString() === selectedDate.toDateString()
          const isCurrentDay = date.getTime() === today.getTime()
          const hasRoutine = day.disponible && day.ejercicios.length > 0
          const cover = hasRoutine && day.ejercicios.map(item => exerciseImage(item.nombre)).find(Boolean)
          const trained = trainedDates.has(isoDate(date))
          const doneToday = isCurrentDay ? day.ejercicios.filter(item => completedExerciseIds.has(item.idPlanEjercicio)).length : 0
          const progress = trained && !isCurrentDay ? 100 : hasRoutine ? doneToday / day.ejercicios.length * 100 : 0
          const minutes = minutesBetween(day.horaInicio, day.horaFin)
          return <button key={day.diaSemana} type="button"
            className={['routine-day-card', selected && 'selected', isCurrentDay && 'today', !day.disponible && 'rest', trained && 'trained'].filter(Boolean).join(' ')}
            aria-pressed={selected} disabled={loading || loadFailed} onClick={() => selectDate(date)}>
            <span className="routine-day-top"><small>{shortWeekdays[index]}</small><strong>{date.getDate()}</strong>
              {isCurrentDay ? <em className="routine-day-badge">HOY</em> : trained && <em className="routine-day-check" aria-label="Entrenado"><Icon name="check" /></em>}</span>
            {hasRoutine ? <>
              <span className="routine-day-thumb">{cover ? <img src={cover} alt="" loading="lazy" /> : <Icon name="dumbbell" />}</span>
              <span className="routine-day-title">{dayTitle(day)}</span>
              <span className="routine-day-status">{exerciseCount(day.ejercicios.length)}{minutes ? ` · ${minutes} min` : ''}</span>
              <span className="routine-day-progress" aria-hidden="true"><span style={{ width: `${progress}%` }} /></span>
            </> : <span className="routine-day-empty"><Icon name={day.disponible ? 'plus' : 'pause'} />{day.disponible ? 'Disponible' : 'Descanso'}</span>}
          </button>
        })}
        {loading && <div className="routine-loading" role="status">Cargando tu rutina…</div>}
      </div>
    </section>

    {!loading && !loadFailed && <div className="routine-layout" key={selectedDate.getTime()}>
      <div className="routine-main">
        {selectedDay.disponible ? <>
          <RoutineHero day={selectedDay} dateLabel={dateLabel} isToday={isToday}
            completed={selectedDay.ejercicios.filter(item => completedExerciseIds.has(item.idPlanEjercicio)).length}
            onContinue={continueWorkout} onAdd={openPicker} />
          <section className="routine-exercise-panel" aria-labelledby="routine-exercises-heading">
            <div className="routine-exercise-heading">
              <h3 id="routine-exercises-heading">Ejercicios del día</h3>
              {selectedDay.ejercicios.length > 1 && <span>Arrastra o usa ⋯ para reordenar</span>}
            </div>
            {selectedDay.ejercicios.length ? <ol className="routine-exercise-list">
              {selectedDay.ejercicios.map((exercise, index) => {
                const done = isToday && completedExerciseIds.has(exercise.idPlanEjercicio)
                const image = exerciseImage(exercise.nombre)
                return <li key={exercise.idEjercicio} id={exercise.idPlanEjercicio ? `routine-exercise-${exercise.idPlanEjercicio}` : undefined}
                  className={[addedId === exercise.idEjercicio && 'routine-exercise-added', done && 'is-done', dragIndex === index && 'is-dragging'].filter(Boolean).join(' ')}
                  draggable={!saving && !generating}
                  onDragStart={event => { setDragIndex(index); event.dataTransfer.effectAllowed = 'move' }}
                  onDragOver={event => { if (dragIndex !== null) event.preventDefault() }}
                  onDrop={event => { event.preventDefault(); if (dragIndex !== null) moveExercise(dragIndex, index); setDragIndex(null) }}
                  onDragEnd={() => setDragIndex(null)}>
                  <span className="routine-exercise-grip" aria-hidden="true">⋮⋮</span>
                  <span className="routine-exercise-index" aria-hidden="true">{String(index + 1).padStart(2, '0')}</span>
                  <span className="routine-exercise-thumb">{image ? <img src={image} alt="" loading="lazy" /> : <Icon name="dumbbell" />}</span>
                  <div className="routine-exercise-description">
                    <strong>{exercise.nombre}{exercise.origen === 'IA' && <span className="routine-ai-tag" title="Propuesto por el generador de rutinas">IA</span>}</strong>
                    <div className="routine-exercise-tags">
                      {exercise.grupoMuscular && <span><i style={{ background: groupColor(exercise.grupoMuscular) }} />{exercise.grupoMuscular}</span>}
                      <span>{measureLabel(exercise)}</span>
                      <span><Icon name="clock" />{exercise.descansoSegundos === 0 ? 'Sin descanso' : `${exercise.descansoSegundos} s`}</span>
                    </div>
                  </div>
                  <div className="routine-exercise-actions">
                    {isToday && exercise.idPlanEjercicio && (done
                      ? <span className="routine-exercise-completed"><Icon name="check" />Realizado</span>
                      : <button type="button" className="routine-complete-exercise"
                        disabled={saving || generating || completingExerciseId !== null}
                        onClick={() => completeExercise(exercise)}
                        aria-label={`Marcar ${exercise.nombre} como realizado`}>
                        <Icon name={completingExerciseId === exercise.idPlanEjercicio ? 'clock' : 'check'} />
                        {completingExerciseId === exercise.idPlanEjercicio ? 'Guardando…' : 'Marcar realizado'}
                      </button>)}
                    <details className="routine-exercise-menu"
                      onBlur={event => { if (!event.currentTarget.contains(event.relatedTarget)) event.currentTarget.open = false }}>
                      <summary aria-label={`Más acciones para ${exercise.nombre}`}>⋯</summary>
                      <div>
                        <button type="button" disabled={index === 0 || saving}
                          onClick={event => { moveExercise(index, index - 1); event.currentTarget.closest('details').open = false }}>Subir</button>
                        <button type="button" disabled={index === selectedDay.ejercicios.length - 1 || saving}
                          onClick={event => { moveExercise(index, index + 1); event.currentTarget.closest('details').open = false }}>Bajar</button>
                        <button type="button" className="is-danger" disabled={saving || completingExerciseId !== null}
                          onClick={() => removeExercise(exercise.idEjercicio)}>Quitar de la rutina</button>
                      </div>
                    </details>
                  </div>
                </li>
              })}
            </ol> : <div className="routine-empty-day"><span><Icon name="dumbbell" /></span>
              <strong>Aún no agregaste ejercicios</strong><p>Elige ejercicios del catálogo o genera tu rutina con IA.</p>
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
      </div>

      <aside className="routine-side">
        {selectedDay.ejercicios.length > 0 && <DaySummary day={selectedDay} isToday={isToday}
          completed={selectedDay.ejercicios.filter(item => completedExerciseIds.has(item.idPlanEjercicio)).length} />}
        <section className={`routine-ai${proposal ? ' has-proposal' : ''}`} aria-live="polite">
          <div className="routine-ai-head"><span className="routine-ai-icon"><Icon name="sparkles" /></span>
            <strong>{proposal ? (proposal.fuente === 'GEMINI' ? 'Propuesta generada con IA' : 'Propuesta generada por reglas') : 'Rutina con IA'}</strong></div>
          <p>{proposal ? proposal.mensaje : 'Te proponemos una semana de entrenamiento según tu perfil, tus horarios y el catálogo del gimnasio. Podrás revisarla antes de guardarla.'}</p>
          <button type="button" className="routine-ai-button" onClick={generate}
            disabled={loading || loadFailed || saving || generating || availableCount === 0}
            title={availableCount === 0 ? 'Configura tu disponibilidad para generar una rutina' : undefined}>
            <Icon name={generating ? 'clock' : 'sparkles'} />{generating ? 'Generando…' : proposal ? 'Generar otra' : 'Generar con IA'}
          </button>
          <small>Rutina orientativa. Si tienes lesiones o dudas de salud, consulta con un entrenador.</small>
        </section>
        {selectedDay.disponible && <button type="button" ref={addTrigger} className="routine-add-button routine-side-add" onClick={openPicker} disabled={saving || generating}>
          <Icon name="plus" />Agregar ejercicio
        </button>}
      </aside>
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
      <span>{availableCount} {availableCount === 1 ? 'día de entrenamiento' : 'días de entrenamiento'} · La rutina se repite cada semana{dirty ? ' · Tienes cambios sin guardar' : ''}</span>
      <div><button type="button" className="routine-reset" disabled={!dirty || loading || saving || generating} onClick={reset}>Restablecer</button>
        <button type="button" className="routine-save" disabled={!dirty || loading || saving || generating} onClick={save}>
          <Icon name={saving ? 'clock' : 'check'} />{saving ? 'Guardando…' : 'Guardar rutina'}
        </button></div>
    </footer>
  </section>
}

/** Portada del día: ilustración, grupos, totales y avance de hoy. */
function RoutineHero({ day, dateLabel, isToday, completed, onContinue, onAdd }) {
  const exercises = day.ejercicios
  const cover = exercises.map(item => exerciseImage(item.nombre)).find(Boolean)
  const minutes = minutesBetween(day.horaInicio, day.horaFin)
  const totalSeries = exercises.reduce((sum, item) => sum + Number(item.series || 0), 0)
  const hasAi = exercises.some(item => item.origen === 'IA')
  const allDone = exercises.length > 0 && completed === exercises.length
  return <section className={`routine-hero${exercises.length ? '' : ' is-empty'}`}>
    {cover && <div className="routine-hero-cover"><img src={cover} alt="" /></div>}
    <div className="routine-hero-copy">
      <span className="routine-hero-kicker">{dateLabel}{isToday ? ' · Hoy' : ''}{day.horaInicio && day.horaFin ? ` · ${formatTime(day.horaInicio)} – ${formatTime(day.horaFin)}` : ''}</span>
      <h2>{exercises.length ? dayTitle(day) : 'Día disponible'}</h2>
      {exercises.length > 0 ? <>
        <ul className="routine-hero-chips">
          <li><Icon name="dumbbell" />{exerciseCount(exercises.length)}</li>
          <li><Icon name="activity" />{totalSeries} series</li>
          {minutes && <li><Icon name="clock" />{minutes} min</li>}
          {hasAi && <li><Icon name="sparkles" />IA</li>}
        </ul>
        {isToday && <div className="routine-hero-progress">
          <span><strong>{completed} de {exercises.length}</strong> ejercicios realizados</span>
          <span className="routine-hero-bar" aria-hidden="true"><span style={{ width: `${completed / exercises.length * 100}%` }} /></span>
        </div>}
        {isToday && (allDone
          ? <p className="routine-hero-done"><Icon name="check" />¡Entrenamiento de hoy completado!</p>
          : <button type="button" className="routine-hero-button" onClick={onContinue}>
            <Icon name="arrow" />{completed ? 'Continuar entrenamiento' : 'Empezar entrenamiento'}</button>)}
      </> : <>
        <p className="routine-hero-note">Agrega ejercicios del catálogo o genera tu semana con IA.</p>
        <button type="button" className="routine-hero-button" onClick={onAdd}><Icon name="plus" />Agregar ejercicio</button>
      </>}
    </div>
  </section>
}

/** Totales del día y reparto por grupo muscular. */
function DaySummary({ day, isToday, completed }) {
  const minutes = minutesBetween(day.horaInicio, day.horaFin)
  const totalSeries = day.ejercicios.reduce((sum, item) => sum + Number(item.series || 0), 0)
  const groups = Object.entries(day.ejercicios.reduce((counts, item) => {
    const group = item.grupoMuscular || 'Otros'
    return { ...counts, [group]: (counts[group] ?? 0) + 1 }
  }, {})).sort((a, b) => b[1] - a[1])
  const max = Math.max(...groups.map(([, count]) => count))
  return <section className="routine-summary" aria-labelledby="routine-summary-heading">
    <h3 id="routine-summary-heading">Resumen del día</h3>
    <dl className="routine-summary-stats">
      <div><dt>series</dt><dd>{totalSeries}</dd></div>
      <div><dt>minutos</dt><dd>{minutes ?? '—'}</dd></div>
      <div><dt>{isToday ? 'hechos' : 'ejercicios'}</dt><dd>{isToday ? `${completed}/${day.ejercicios.length}` : day.ejercicios.length}</dd></div>
    </dl>
    <p className="routine-summary-label">Grupos trabajados</p>
    <ul className="routine-summary-groups">
      {groups.map(([group, count]) => <li key={group}>
        <span><i style={{ background: groupColor(group) }} />{group}</span>
        <span className="routine-summary-bar" aria-hidden="true"><span style={{ width: `${count / max * 100}%`, background: groupColor(group) }} /></span>
        <strong>{count}</strong>
      </li>)}
    </ul>
  </section>
}
