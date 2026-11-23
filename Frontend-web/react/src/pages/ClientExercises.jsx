import { useEffect, useRef, useState } from 'react'
import Icon from '../components/Icon'
import { getRoutineExercises } from '../services/routine'
import { normalizeSearch } from '../utils/format'
import './ClientExercises.css'

const illustrationFiles = import.meta.glob('../assets/exercises/*.jpg', { eager: true, query: '?url', import: 'default' })
const illustrations = new Map([
  ['aperturas-con-mancuernas', 'Acostado en un banco, con los brazos abiertos a los lados al inicio y las mancuernas juntas sobre el pecho al final.'],
  ['curl-de-biceps', 'Inicio de pie con las mancuernas junto a los muslos. Final con los codos flexionados y las mancuernas cerca de los hombros.'],
  ['elevaciones-laterales', 'De pie, con las mancuernas junto a los muslos al inicio y los brazos elevados hacia los lados al final.'],
  ['extension-de-triceps-en-polea', 'De pie frente a la polea, con los codos flexionados al inicio y la cuerda junto a los muslos al final.'],
  ['flexiones-de-pecho', 'Inicio con los brazos extendidos. Final con los codos flexionados y el pecho cerca del suelo.'],
  ['jalon-al-pecho', 'Inicio sentado con la barra sobre la cabeza. Final con la barra frente a la parte alta del pecho.'],
  ['peso-muerto-rumano', 'Inicio de pie con las mancuernas frente a los muslos. Final con la cadera hacia atrás y las mancuernas junto a las piernas.'],
  ['plancha-frontal', 'Posición de sostén sobre los antebrazos y las puntas de los pies, con el cuerpo alineado.'],
  ['prensa-de-piernas', 'Con los pies en la plataforma, las rodillas comienzan flexionadas y terminan casi extendidas.'],
  ['press-de-banca', 'Acostado en el banco, con la barra cerca del pecho al inicio y elevada sobre el torso al final.'],
  ['press-inclinado-con-mancuernas', 'En un banco inclinado, con las mancuernas junto al pecho al inicio y elevadas por encima del pecho al final.'],
  ['press-militar', 'De pie, con las mancuernas a la altura de los hombros al inicio y elevadas sobre la cabeza al final.'],
  ['remo-con-mancuerna', 'Con una mano y una rodilla apoyadas en el banco, la mancuerna cuelga al inicio y se acerca a la cadera al final.'],
  ['saltos-de-tijera', 'Inicio con los pies juntos y brazos abajo. Final con los pies separados y los brazos elevados.'],
  ['sentadilla-goblet', 'Con una mancuerna frente al pecho, comienza de pie y termina con la cadera baja y las rodillas flexionadas.'],
  ['zancadas-caminando', 'Inicio de pie. Final con una pierna adelantada y la rodilla de atrás cerca del suelo.'],
].flatMap(([key, alt]) => {
  const src = illustrationFiles[`../assets/exercises/${key}.jpg`]
  return src ? [[key, { src, alt, hold: key === 'plancha-frontal' }]] : []
}))

export default function ClientExercises({ onExpired }) {
  const [exercises, setExercises] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [attempt, setAttempt] = useState(0)
  const [busqueda, setBusqueda] = useState('')
  const [grupo, setGrupo] = useState('Todos')
  const [nivel, setNivel] = useState('Todos')
  const [seleccionado, setSeleccionado] = useState(null)

  useEffect(() => {
    let current = true
    getRoutineExercises().then(data => {
      if (current) setExercises(data)
    }).catch(err => {
      if (!current) return
      if (err.status === 401) onExpired()
      else setError(err.message || 'No se pudo cargar el catálogo de ejercicios.')
    }).finally(() => {
      if (current) setLoading(false)
    })
    return () => { current = false }
  }, [attempt, onExpired])

  const grupos = ['Todos', ...new Set(exercises.map(item => item.grupoMuscular).filter(Boolean))]
  const niveles = ['Todos', ...new Set(exercises.map(item => item.nivelExperiencia).filter(Boolean))]
  const texto = normalizeSearch(busqueda.trim())
  const filtrados = exercises.filter(item => (grupo === 'Todos' || item.grupoMuscular === grupo)
    && (nivel === 'Todos' || item.nivelExperiencia === nivel)
    && normalizeSearch(`${item.nombre} ${item.grupoMuscular ?? ''} ${item.equipamiento ?? ''}`).includes(texto))

  function retry() {
    setError('')
    setLoading(true)
    setAttempt(previous => previous + 1)
  }

  function limpiarFiltros() {
    setBusqueda('')
    setGrupo('Todos')
    setNivel('Todos')
  }

  return <section className="client-exercises" aria-labelledby="client-page-title">
    {!loading && !error && exercises.length > 0 && <section className="client-exercise-filters" aria-label="Filtros de ejercicios">
      <div className="client-exercise-filter-topline">
        <label className="client-exercise-search"><Icon name="search" />
          <input type="search" value={busqueda} onChange={event => setBusqueda(event.target.value)}
            placeholder="Buscar ejercicio por nombre" aria-label="Buscar ejercicio por nombre" />
        </label>
        <span className="client-exercise-count" role="status">{filtrados.length} {filtrados.length === 1 ? 'ejercicio' : 'ejercicios'}</span>
      </div>
      <div className="client-filter-line"><strong>Grupo muscular</strong><div className="client-filter-options" role="group" aria-label="Filtrar por grupo muscular">
        {grupos.map(item => <button key={item} type="button" className={grupo === item ? 'selected' : ''}
          aria-pressed={grupo === item} onClick={() => setGrupo(item)}>{item}</button>)}
      </div></div>
      <div className="client-filter-line"><strong>Nivel</strong><div className="client-filter-options" role="group" aria-label="Filtrar por nivel">
        {niveles.map(item => <button key={item} type="button" className={nivel === item ? 'selected' : ''}
          aria-pressed={nivel === item} onClick={() => setNivel(item)}>{item}</button>)}
      </div></div>
    </section>}

    <section className="client-exercise-list" aria-label="Catálogo de ejercicios" aria-busy={loading}>
      {loading ? <div className="client-exercise-empty" role="status">Cargando ejercicios…</div>
        : error ? <div className="client-exercise-empty">
          <h2>No se pudo cargar el catálogo</h2><p role="alert">{error}</p>
          <button type="button" onClick={retry}>Reintentar</button>
        </div>
        : filtrados.length ? <ul className="client-exercise-grid">
          {filtrados.map(item => <li key={item.idEjercicio} className="client-exercise-card">
            <ExercisePreview illustration={getIllustration(item)} />
            <div className="client-exercise-card-body">
              <h2>{item.nombre}</h2>
              <p>{[item.grupoMuscular, item.equipamiento || 'No especificado'].filter(Boolean).join(' · ')}</p>
              <div className="client-exercise-card-footer">
                {item.nivelExperiencia && <span className="client-level-pill">{item.nivelExperiencia}</span>}
                <button type="button" className="client-exercise-detail" aria-label={`Ver detalle de ${item.nombre}`}
                  onClick={() => setSeleccionado(item)}>Ver detalle <Icon name="arrow" /></button>
              </div>
            </div>
          </li>)}
        </ul> : <div className="client-exercise-empty"><span><Icon name="search" /></span>
        {exercises.length ? <>
          <h2>No encontramos ejercicios</h2><p>Prueba con otro nombre o cambia los filtros.</p>
          <button type="button" onClick={limpiarFiltros}>Limpiar filtros</button>
        </> : <h2>No hay ejercicios disponibles</h2>}</div>}
    </section>

    {seleccionado && <ExerciseDetail key={seleccionado.idEjercicio} exercise={seleccionado}
      onClose={() => setSeleccionado(null)} />}
  </section>
}

function getIllustration(exercise) {
  const exerciseKey = normalizeSearch(exercise.nombre).trim().replace(/\s+/g, '-')
  return illustrations.get(exercise.imagenRuta) ?? illustrations.get(exerciseKey)
}

function ExercisePreview({ illustration }) {
  const [failedSrc, setFailedSrc] = useState(null)
  return <div className="client-exercise-preview" aria-hidden="true">
    {illustration && failedSrc !== illustration.src
      ? <img src={illustration.src} alt="" loading="lazy" decoding="async" onError={() => setFailedSrc(illustration.src)} />
      : <Icon name="dumbbell" />}
  </div>
}

function ExerciseDetail({ exercise, onClose }) {
  const dialog = useRef(null)
  const measurement = exercise.tipoMedicion?.toUpperCase()
  const measurementLabel = { TIEMPO: 'Tiempo', REPETICIONES: 'Repeticiones' }[measurement]

  useEffect(() => {
    const element = dialog.current
    if (!element.open) element.showModal()
  }, [])

  return <dialog ref={dialog} className="client-detail-dialog" aria-labelledby="client-detail-title"
    aria-describedby={exercise.descripcion ? 'client-detail-description' : undefined} onClose={onClose}
    onClick={event => {
      if (event.target !== event.currentTarget) return
      const { left, right, top, bottom } = event.currentTarget.getBoundingClientRect()
      if (event.clientX < left || event.clientX > right || event.clientY < top || event.clientY > bottom) dialog.current.close()
    }}>
    <header className="client-detail-heading">
      <div>
        <span className="client-page-kicker">Ejercicio</span>
        <h2 id="client-detail-title">{exercise.nombre}</h2>
        {exercise.descripcion && <p className="client-detail-description" id="client-detail-description">{exercise.descripcion}</p>}
      </div>
      <button type="button" className="client-detail-close" aria-label="Cerrar detalle" onClick={() => dialog.current.close()}>
        <Icon name="close" />
      </button>
    </header>
    <div className="client-detail-content">
      <section aria-labelledby="client-movement-heading">
        <h3 id="client-movement-heading">El movimiento</h3>
        <ExerciseMovement illustration={getIllustration(exercise)} />
      </section>
      <section aria-labelledby="client-exercise-facts">
        <h3 id="client-exercise-facts">Datos</h3>
        <dl>
          <div><dt>Grupo muscular</dt><dd>{exercise.grupoMuscular || 'No especificado'}</dd></div>
          <div><dt>Nivel</dt><dd>{exercise.nivelExperiencia || 'No especificado'}</dd></div>
          <div><dt>Equipamiento</dt><dd>{exercise.equipamiento || 'No especificado'}</dd></div>
          <div><dt>Tipo de medición</dt><dd>{measurementLabel || exercise.tipoMedicion || 'No especificado'}</dd></div>
        </dl>
      </section>
    </div>
    <footer className="client-detail-footer">
      <button type="button" className="client-detail-done" onClick={() => dialog.current.close()}>Cerrar</button>
    </footer>
  </dialog>
}

function ExerciseMovement({ illustration }) {
  const [imageFailed, setImageFailed] = useState(false)

  if (illustration && !imageFailed) return <figure className="client-exercise-movement">
    <img src={illustration.src} alt={illustration.alt} onError={() => setImageFailed(true)} />
    <figcaption className="client-movement-labels" aria-hidden="true">
      {illustration.hold ? <span>Posición de sostén</span> : <>
        <span><b>1</b> Inicio</span><span><b>2</b> Final</span>
      </>}
    </figcaption>
  </figure>

  return <div className="client-detail-note">
    <Icon name="activity" />
    <span>Demostración aún no disponible</span>
  </div>
}
