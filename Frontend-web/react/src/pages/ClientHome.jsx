import Icon from '../components/Icon'
import useClientSummary from '../hooks/useClientSummary'
import { getNutritionDay } from '../services/nutrition'
import { getProgressHistory } from '../services/progress'
import { getRoutineSummary } from '../services/routine'
import { exerciseImage } from '../utils/exerciseIllustrations'
import './ClientHome.css'

const limaClock = new Intl.DateTimeFormat('en-GB', {
  timeZone: 'America/Lima', year: 'numeric', month: '2-digit', day: '2-digit',
  hour: '2-digit', minute: '2-digit', hourCycle: 'h23',
})
const workoutDate = new Intl.DateTimeFormat('es-PE', { timeZone: 'UTC', weekday: 'long', day: 'numeric', month: 'short' })
const shortDate = new Intl.DateTimeFormat('es-PE', { timeZone: 'UTC', day: 'numeric', month: 'short' })
const number = new Intl.NumberFormat('es-PE', { maximumFractionDigits: 1 })
const integer = new Intl.NumberFormat('es-PE', { maximumFractionDigits: 0 })
const weekLetters = ['L', 'M', 'X', 'J', 'V', 'S', 'D']
const macros = [
  { key: 'proteinaG', label: 'Proteínas', color: 'var(--home-blue)' },
  { key: 'carbohidratoG', label: 'Carbohidratos', color: 'var(--home-orange)' },
  { key: 'grasaG', label: 'Grasas', color: 'var(--home-lime)' },
]

/** Fecha de hoy en Lima como Date UTC a medianoche y la hora actual HH:mm. */
function limaToday(now = new Date()) {
  const { year, month, day, hour, minute } = Object.fromEntries(limaClock.formatToParts(now).map(({ type, value }) => [type, value]))
  return { today: new Date(Date.UTC(Number(year), Number(month) - 1, Number(day))), time: `${hour}:${minute}` }
}

const isoDate = date => date.toISOString().slice(0, 10)
const weekday = date => (date.getUTCDay() + 6) % 7 + 1

function nextWorkout(days) {
  const { today, time } = limaToday()
  for (let offset = 0; offset <= 7; offset += 1) {
    const date = new Date(today)
    date.setUTCDate(today.getUTCDate() + offset)
    const workout = days.find(item => item.diaSemana === weekday(date) && item.disponible
      && item.ejercicios.length > 0 && (offset > 0 || item.horaFin?.slice(0, 5) > time))
    if (workout) return { ...workout, date, offset }
  }
  return null
}

/** Lunes a domingo de la semana actual con el estado de cada día. */
function currentWeek(routine) {
  const { today } = limaToday()
  const monday = new Date(today)
  monday.setUTCDate(today.getUTCDate() - (weekday(today) - 1))
  const trained = new Set((routine?.actividadReciente ?? []).map(item => item.fecha?.slice(0, 10)))
  return weekLetters.map((letter, index) => {
    const date = new Date(monday)
    date.setUTCDate(monday.getUTCDate() + index)
    const plan = routine?.dias?.find(item => item.diaSemana === index + 1)
    return {
      letter, date, day: date.getUTCDate(),
      today: isoDate(date) === isoDate(today),
      planned: Boolean(plan?.disponible && plan.ejercicios.length > 0),
      available: Boolean(plan?.disponible),
      trained: trained.has(isoDate(date)),
    }
  })
}

function minutesBetween(start, end) {
  const toMinutes = value => value ? Number(value.slice(0, 2)) * 60 + Number(value.slice(3, 5)) : null
  const total = toMinutes(end) - toMinutes(start)
  return Number.isFinite(total) && total > 0 ? total : null
}

function CardMessage({ result, loading, error }) {
  if (!result.error) return <p className="ch-muted" role="status">{loading}</p>
  return <>
    <p className="ch-muted" role="status">{error}</p>
    <button className="client-text-link" type="button" onClick={result.retry}>Reintentar</button>
  </>
}

function WeekStrip({ week }) {
  return <ol className="ch-week" aria-label="Tu semana">
    {week.map(item => <li key={item.letter}
      className={[item.today && 'is-today', item.trained && 'is-trained', !item.available && 'is-rest'].filter(Boolean).join(' ')}
      aria-label={`${workoutDate.format(item.date)}${item.trained ? ', entrenado' : item.planned ? ', con entrenamiento' : item.available ? '' : ', descanso'}`}>
      <small>{item.letter}</small><strong>{item.day}</strong><span className={item.planned || item.trained ? 'dot' : 'dot is-empty'} />
    </li>)}
  </ol>
}

function WorkoutHero({ routine, onNavigate }) {
  const workout = routine.data && nextWorkout(routine.data.dias)
  const exercises = workout?.ejercicios ?? []
  const groups = [...new Set(exercises.map(item => item.grupoMuscular).filter(Boolean))]
  const title = groups.length > 0 && groups.length <= 2 ? groups.join(' + ') : 'Rutina semanal'
  const cover = exercises.map(exerciseImage).find(Boolean)
  const minutes = workout && minutesBetween(workout.horaInicio, workout.horaFin)
  const byAi = exercises.some(item => item.origen === 'IA')
  const eyebrow = !workout ? 'Mi rutina'
    : workout.offset === 0 ? `Entrenamiento de hoy · ${workout.horaInicio.slice(0, 5)} – ${workout.horaFin.slice(0, 5)}`
      : `${workout.offset === 1 ? 'Mañana' : workoutDate.format(workout.date)} · ${workout.horaInicio.slice(0, 5)}`

  return <section className="ch-hero" aria-labelledby="client-home-workout"
    style={cover ? { '--hero-cover': `url(${cover})` } : undefined}>
    <div className="ch-hero-copy">
      <p className="ch-eyebrow">{eyebrow}</p>
      <h2 id="client-home-workout">{workout ? title : 'Arma tu semana'}</h2>
      {workout ? <ul className="ch-chips">
        <li><Icon name="dumbbell" />{exercises.length} {exercises.length === 1 ? 'ejercicio' : 'ejercicios'}</li>
        {minutes && <li><Icon name="clock" />{minutes} min</li>}
        {byAi && <li><Icon name="sparkles" />Generada con IA</li>}
      </ul> : routine.data ? <p className="ch-hero-note">Genera tu rutina con IA o agrega ejercicios a tus días disponibles.</p>
        : <CardMessage result={routine} loading="Cargando tu rutina…" error="No se pudo cargar tu rutina." />}
      <div className="ch-hero-actions">
        <button className="ch-primary" type="button"
          onClick={() => onNavigate('mi-rutina', workout ? { fecha: isoDate(workout.date) } : undefined)}>
          {workout ? 'Ir a entrenar' : 'Ir a mi rutina'} <Icon name="arrow" />
        </button>
        {workout && <button className="ch-ghost" type="button" onClick={() => onNavigate('mi-rutina')}>Ver semana</button>}
      </div>
    </div>
    {exercises.length > 0 && <ul className="ch-hero-exercises" aria-label="Primeros ejercicios">
      {exercises.slice(0, 4).map(item => {
        const image = exerciseImage(item)
        return <li key={item.idPlanEjercicio ?? item.idEjercicio}>
          {image ? <img src={image} alt="" loading="lazy" /> : <span className="ch-hero-placeholder"><Icon name="dumbbell" /></span>}
          <div><strong>{item.nombre}</strong>
            <small>{item.series} × {item.segundos ? `${item.segundos} s` : item.repeticiones} · {item.descansoSegundos} s</small></div>
          {item.origen === 'IA' && <span className="ch-ia">IA</span>}
        </li>
      })}
    </ul>}
  </section>
}

function NutritionCard({ nutrition, onNavigate }) {
  const summary = nutrition.data
  const goal = Number(summary?.meta?.kcal ?? 0)
  const kcal = Number(summary?.kcal ?? 0)
  const progress = goal > 0 ? Math.min(kcal / goal, 1) : 0
  const remaining = Math.max(goal - kcal, 0)
  const entries = summary?.ingestas?.length ?? 0

  return <section className="client-card ch-card ch-nutrition" aria-labelledby="client-home-nutrition">
    <header className="ch-card-head">
      <h2 id="client-home-nutrition">Nutrición de hoy</h2>
      <button className="client-text-link" type="button" onClick={() => onNavigate('nutricion')}>Ver detalle <Icon name="arrow" /></button>
    </header>
    {summary ? <div className="ch-nutrition-body">
      <div className="ch-ring" style={{ '--progress': `${progress * 100}%`, '--ring-color': goal > 0 && kcal >= goal ? 'var(--home-lime)' : 'var(--home-blue)' }}>
        <div><strong>{integer.format(goal > 0 ? remaining : kcal)}</strong><span>{goal > 0 ? 'kcal restantes' : 'kcal hoy'}</span></div>
      </div>
      <dl className="ch-macros">{macros.map(macro => {
        const amount = Number(summary[macro.key] ?? 0)
        const target = Number(summary.meta?.[macro.key] ?? 0)
        return <div key={macro.key}>
          <dt>{macro.label}</dt>
          <dd><span><strong>{number.format(amount)}</strong>{target > 0 ? ` / ${number.format(target)}` : ''} g</span>
            <span className="ch-track" aria-hidden="true"><span style={{ width: `${target > 0 ? Math.min(amount / target, 1) * 100 : 0}%`, background: macro.color }} /></span>
          </dd>
        </div>
      })}</dl>
    </div> : <CardMessage result={nutrition} loading="Cargando tu resumen…" error="No se pudo cargar tu resumen." />}
    {summary && <p className="ch-foot">
      {entries > 0 ? `${entries} ${entries === 1 ? 'alimento registrado' : 'alimentos registrados'} · ${integer.format(kcal)} kcal`
        : 'Aún no registras alimentos hoy.'}
      {goal <= 0 && ' Completa tu perfil para calcular tu meta diaria.'}
    </p>}
  </section>
}

function WeightSparkline({ points }) {
  const values = points.map(item => Number(item.pesoKg))
  const min = Math.min(...values) - 0.3
  const max = Math.max(...values) + 0.3
  const width = 260
  const height = 64
  const coords = values.map((value, index) => [
    6 + index * (width - 12) / Math.max(values.length - 1, 1),
    6 + (max - value) / (max - min) * (height - 14),
  ])
  const line = coords.map(([x, y]) => `${x.toFixed(1)},${y.toFixed(1)}`).join(' ')
  const [lastX, lastY] = coords.at(-1)
  return <svg className="ch-spark" viewBox={`0 0 ${width} ${height}`} preserveAspectRatio="none" aria-hidden="true">
    <defs><linearGradient id="ch-spark-fill" x1="0" x2="0" y1="0" y2="1">
      <stop offset="0" stopColor="#2166e8" stopOpacity=".28" /><stop offset="1" stopColor="#2166e8" stopOpacity="0" />
    </linearGradient></defs>
    <polygon points={`6,${height} ${line} ${width - 6},${height}`} fill="url(#ch-spark-fill)" />
    <polyline points={line} fill="none" stroke="#2166e8" strokeWidth="2.5" strokeLinejoin="round" />
    <circle cx={lastX} cy={lastY} r="4" fill="#2166e8" />
  </svg>
}

function WeightCard({ progress, onNavigate }) {
  const history = progress.data ?? []
  const latest = history[0]
  const previous = history[1]
  const change = latest && previous ? Number(latest.pesoKg) - Number(previous.pesoKg) : null
  const ascending = [...history].reverse()
  const since = ascending[0]?.fechaRegistro

  return <section className="client-card ch-card ch-kpi" aria-labelledby="client-home-weight">
    <h2 id="client-home-weight" className="ch-kpi-title">Peso actual</h2>
    {latest ? <>
      <p className="ch-kpi-value"><strong>{number.format(latest.pesoKg)}</strong><span>kg</span>
        {change !== null && change !== 0 && <em className={change < 0 ? 'is-down' : 'is-up'}>
          {change < 0 ? '▼' : '▲'} {number.format(Math.abs(change))} kg</em>}
      </p>
      {ascending.length > 1 ? <WeightSparkline points={ascending} />
        : <p className="ch-muted">Registra otra medición para ver tu tendencia.</p>}
      <p className="ch-foot">{history.length} {history.length === 1 ? 'medición' : 'mediciones'}{since && ` · desde ${shortDate.format(new Date(`${since.slice(0, 10)}T00:00:00Z`))}`}</p>
    </> : progress.data ? <p className="ch-muted">Aún no tienes un peso registrado.</p>
      : <CardMessage result={progress} loading="Cargando tu progreso…" error="No se pudo cargar tu progreso." />}
    <button className="client-text-link" type="button" onClick={() => onNavigate('progreso')}>Ver mi evolución <Icon name="arrow" /></button>
  </section>
}

function WeekCard({ routine, week }) {
  const planned = week.filter(item => item.planned).length
  const trained = week.filter(item => item.trained).length
  const workout = routine.data && nextWorkout(routine.data.dias)
  const activity = routine.data?.actividadReciente ?? []

  return <section className="client-card ch-card ch-kpi" aria-labelledby="client-home-week">
    <h2 id="client-home-week" className="ch-kpi-title">Esta semana</h2>
    <p className="ch-kpi-value"><strong>{trained}</strong><span>/ {planned} {planned === 1 ? 'entreno' : 'entrenos'}</span></p>
    {planned > 0 && <div className="ch-segments" aria-hidden="true">
      {Array.from({ length: planned }, (_, index) => <span key={index} className={index < trained ? 'is-done' : undefined} />)}
    </div>}
    <p className="ch-foot">{workout ? `Próximo: ${workout.offset === 0 ? 'hoy' : workoutDate.format(workout.date)} ${workout.horaInicio.slice(0, 5)}`
      : routine.data ? 'Sin entrenamientos programados.' : ''}</p>
    {activity.length > 0 && <ul className="ch-activity">
      {activity.slice(0, 2).map(entry => <li key={entry.fecha}>
        <span className="dot" /><div><strong>{entry.titulo}</strong>
          <small>{workoutDate.format(new Date(`${entry.fecha.slice(0, 10)}T00:00:00Z`))} · {entry.ejercicios} {entry.ejercicios === 1 ? 'ejercicio' : 'ejercicios'}</small></div>
      </li>)}
    </ul>}
  </section>
}

export default function ClientHome({ onNavigate, onExpired }) {
  const routine = useClientSummary(getRoutineSummary, onExpired)
  const nutrition = useClientSummary(getNutritionDay, onExpired)
  const progress = useClientSummary(getProgressHistory, onExpired)
  const week = currentWeek(routine.data)

  return <div className="client-home">
    {routine.data && <WeekStrip week={week} />}
    <div className="ch-grid">
      <WorkoutHero routine={routine} onNavigate={onNavigate} />
      <NutritionCard nutrition={nutrition} onNavigate={onNavigate} />
      <WeightCard progress={progress} onNavigate={onNavigate} />
      <WeekCard routine={routine} week={week} />
    </div>
  </div>
}
