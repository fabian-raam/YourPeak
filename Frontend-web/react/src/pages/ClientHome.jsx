import Icon from '../components/Icon'
import NutritionSummary from '../components/NutritionSummary'
import useClientSummary from '../hooks/useClientSummary'
import { getNutritionDay } from '../services/nutrition'
import { getProgressHistory } from '../services/progress'
import { getRoutineSummary } from '../services/routine'
import './ClientHome.css'

const limaClock = new Intl.DateTimeFormat('en-GB', {
  timeZone: 'America/Lima', year: 'numeric', month: '2-digit', day: '2-digit',
  hour: '2-digit', minute: '2-digit', hourCycle: 'h23',
})
const workoutDate = new Intl.DateTimeFormat('es-PE', {
  timeZone: 'UTC', weekday: 'long', day: 'numeric', month: 'short',
})
const weightDate = new Intl.DateTimeFormat('es-PE', {
  timeZone: 'America/Lima', day: 'numeric', month: 'short', year: 'numeric',
})
const number = new Intl.NumberFormat('es-PE', { maximumFractionDigits: 1 })
function nextWorkout(days, now = new Date()) {
  const { year, month, day, hour, minute } = Object.fromEntries(
    limaClock.formatToParts(now).map(({ type, value }) => [type, value]),
  )
  const today = new Date(Date.UTC(Number(year), Number(month) - 1, Number(day)))
  const currentTime = `${hour}:${minute}`

  for (let offset = 0; offset <= 7; offset += 1) {
    const date = new Date(today)
    date.setUTCDate(today.getUTCDate() + offset)
    const weekday = (date.getUTCDay() + 6) % 7 + 1
    const workout = days.find(item => item.diaSemana === weekday && item.disponible
      && item.ejercicios.length > 0 && (offset > 0 || item.horaFin?.slice(0, 5) > currentTime))
    if (workout) return { ...workout, date, offset }
  }
  return null
}

function CardMessage({ result, loading, error }) {
  if (!result.error) return <p role="status">{loading}</p>
  return <>
    <p role="status">{error}</p>
    <button className="client-text-link" type="button" onClick={result.retry}>Reintentar</button>
  </>
}

function WorkoutCard({ routine, onNavigate }) {
  const workout = routine.data && nextWorkout(routine.data.dias)
  const groups = workout ? [...new Set(workout.ejercicios.map(item => item.grupoMuscular).filter(Boolean))] : []
  const title = groups.length > 0 && groups.length <= 2 ? groups.join(' + ') : 'Rutina semanal'

  return <section className="client-card client-feature client-workout-card" aria-labelledby="client-home-workout">
    <img className="client-workout-athlete" src="/assets/home/athlete-silhouette.png" width="612" height="408" alt="" />
    <div className="client-workout-heading">
      <p className="client-dashboard-eyebrow">{workout?.offset === 0 ? 'Entrenamiento de hoy' : 'Próximo entrenamiento'}</p>
      {workout?.offset === 1 && <span className="client-workout-tomorrow">Mañana</span>}
    </div>
    <h2 id="client-home-workout">{workout ? title : 'Mi rutina'}</h2>
    {workout ? <>
      <p className="client-workout-date">{workoutDate.format(workout.date)}</p>
      <div className="client-workout-meta">
        <span><Icon name="dumbbell" />{workout.ejercicios.length} {workout.ejercicios.length === 1 ? 'ejercicio' : 'ejercicios'}</span>
        <span><Icon name="clock" />{workout.horaInicio.slice(0, 5)} – {workout.horaFin.slice(0, 5)}</span>
      </div>
    </> : routine.data ? <p>Aún no tienes ejercicios programados.</p>
      : <CardMessage result={routine} loading="Cargando tu rutina…" error="No se pudo cargar tu rutina." />}
    <button className="client-cta" type="button" onClick={() => onNavigate('mi-rutina', { fecha: workout?.date.toISOString().slice(0, 10) })}>
      {workout ? 'Ver entrenamiento' : 'Ver mi rutina'} <Icon name="arrow" />
    </button>
  </section>
}

export default function ClientHome({ onNavigate, onExpired }) {
  const routine = useClientSummary(getRoutineSummary, onExpired)
  const nutrition = useClientSummary(getNutritionDay, onExpired)
  const progress = useClientSummary(getProgressHistory, onExpired)
  const activity = routine.data?.actividadReciente ?? []
  const lastWeight = progress.data?.[0]
  const goal = Number(nutrition.data?.meta?.kcal ?? 0)
  const kcalRemaining = goal - Number(nutrition.data?.kcal ?? 0)

  return <div className="client-home-grid">
    <WorkoutCard routine={routine} onNavigate={onNavigate} />
    <NutritionSummary summary={nutrition.data} id="client-home-nutrition" title="Resumen nutricional"
      description={nutrition.data && (goal > 0 ? `Meta diaria de ${number.format(goal)} kcal` : 'Sin meta asignada')}>
      {!nutrition.data && <CardMessage result={nutrition} loading="Cargando tu resumen…" error="No se pudo cargar tu resumen." />}
      {goal > 0 && <p className="client-home-nutrition-note">
        {kcalRemaining > 0 ? `Te faltan ${number.format(kcalRemaining)} kcal para llegar a tu meta.`
          : kcalRemaining < 0 ? `Superaste tu meta por ${number.format(-kcalRemaining)} kcal.` : 'Alcanzaste tu meta diaria.'}
      </p>}
      <button className="client-text-link" type="button" onClick={() => onNavigate('nutricion')}>
        Ver nutrición <Icon name="arrow" />
      </button>
    </NutritionSummary>

    <section className="client-card client-dashboard-card" aria-labelledby="client-home-activity">
      <h2 id="client-home-activity">Actividad reciente</h2>
      {activity.length > 0 ? <ul className="client-recent-activity">
        {activity.map(entry => <li key={entry.fecha}>
          <span className="client-card-icon"><Icon name="check" /></span>
          <div><strong>{entry.titulo}</strong>
            <p>{workoutDate.format(new Date(`${entry.fecha}T00:00:00Z`))} · {entry.ejercicios} {entry.ejercicios === 1 ? 'ejercicio' : 'ejercicios'}</p>
          </div>
        </li>)}
      </ul> : routine.data ? <div className="client-home-activity-empty">
        <span className="client-card-icon"><Icon name="dumbbell" /></span>
        <div><p>Aún no hay ejercicios realizados registrados.</p>
          <button className="client-text-link" type="button" onClick={() => onNavigate('mi-rutina')}>
            Ver mi rutina <Icon name="arrow" />
          </button>
        </div>
      </div>
        : <CardMessage result={routine} loading="Cargando tu actividad…" error="No se pudo cargar tu actividad." />}
    </section>

    <section className="client-card client-dashboard-card client-home-progress" aria-labelledby="client-home-progress">
      <h2 id="client-home-progress">Mi progreso</h2>
      {lastWeight ? <>
        <p>Último peso registrado</p>
        <p className="client-home-weight"><strong>{number.format(lastWeight.pesoKg)}</strong> kg</p>
        <p>Registrado el {weightDate.format(new Date(`${lastWeight.fechaRegistro}Z`))}</p>
      </> : progress.data ? <p>Aún no tienes un peso registrado.</p>
        : <CardMessage result={progress} loading="Cargando tu progreso…" error="No se pudo cargar tu progreso." />}
      <button className="client-text-link" type="button" onClick={() => onNavigate('progreso')}>
        Ver mi evolución <Icon name="arrow" />
      </button>
    </section>
  </div>
}
