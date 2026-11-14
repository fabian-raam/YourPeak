import { useEffect, useState } from 'react'
import Icon from '../components/Icon'
import { getRoutineExercises } from '../services/routine'
import './ClientExercises.css'

const normalizar = value => String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()

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
  const texto = normalizar(busqueda.trim())
  const filtrados = exercises.filter(item => (grupo === 'Todos' || item.grupoMuscular === grupo)
    && (nivel === 'Todos' || item.nivelExperiencia === nivel)
    && normalizar(`${item.nombre} ${item.grupoMuscular ?? ''} ${item.equipamiento ?? ''}`).includes(texto))

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

  return <section className="client-exercises" aria-labelledby="client-exercises-title">
    <header className="client-header">
      <div><h1 id="client-exercises-title">Ejercicios</h1>
        <p>Explora los ejercicios del gimnasio por grupo muscular y nivel.</p></div>
    </header>

    {!loading && !error && exercises.length > 0 && <section className="client-exercise-filters" aria-label="Filtros de ejercicios">
      <div className="client-exercise-filter-topline">
        <label className="client-exercise-search"><Icon name="search" />
          <input type="search" value={busqueda} onChange={event => setBusqueda(event.target.value)}
            placeholder="Buscar ejercicio por nombre" aria-label="Buscar ejercicio por nombre" />
        </label>
        <span className="client-exercise-count">{filtrados.length} {filtrados.length === 1 ? 'ejercicio' : 'ejercicios'}</span>
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
        : filtrados.length ? <div className="client-exercise-table-scroll"><table>
        <thead><tr><th>Ejercicio</th><th>Grupo muscular</th><th>Nivel</th><th>Equipamiento</th><th /></tr></thead>
        <tbody>{filtrados.map(item => <tr key={item.idEjercicio}>
          <td><div className="client-exercise-name"><span><Icon name="dumbbell" /></span><strong>{item.nombre}</strong></div></td>
          <td>{item.grupoMuscular}</td><td><span className="client-level-pill">{item.nivelExperiencia}</span></td>
          <td>{item.equipamiento || 'No especificado'}</td>
          <td><button type="button" className="client-exercise-detail" onClick={() => setSeleccionado(item)}>Ver detalle <Icon name="arrow" /></button></td>
        </tr>)}</tbody>
      </table></div> : <div className="client-exercise-empty"><span><Icon name="search" /></span>
        {exercises.length ? <>
          <h2>No encontramos ejercicios</h2><p>Prueba con otro nombre o cambia los filtros.</p>
          <button type="button" onClick={limpiarFiltros}>Limpiar filtros</button>
        </> : <h2>No hay ejercicios disponibles</h2>}</div>}
    </section>

    {seleccionado && <div className="client-detail-backdrop" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget) setSeleccionado(null) }}>
      <section className="client-detail-dialog" role="dialog" aria-modal="true" aria-labelledby="client-detail-title">
        <button className="client-detail-close" type="button" aria-label="Cerrar detalle" onClick={() => setSeleccionado(null)}><Icon name="close" /></button>
        <span className="client-page-kicker">EJERCICIO</span><h2 id="client-detail-title">{seleccionado.nombre}</h2>
        {seleccionado.descripcion && <p className="client-detail-description">{seleccionado.descripcion}</p>}
        <dl><div><dt>Grupo muscular</dt><dd>{seleccionado.grupoMuscular}</dd></div>
          <div><dt>Nivel</dt><dd>{seleccionado.nivelExperiencia}</dd></div>
          <div><dt>Equipamiento</dt><dd>{seleccionado.equipamiento || 'No especificado'}</dd></div>
          <div><dt>Tipo de medición</dt><dd>{seleccionado.tipoMedicion}</dd></div></dl>
        <ExerciseMovement />
        <button type="button" className="client-detail-done" onClick={() => setSeleccionado(null)}>Cerrar</button>
      </section>
    </div>}
  </section>
}

function ExerciseMovement() {
  return <div className="client-detail-note">
    <Icon name="activity" />
    <div><strong>El movimiento</strong><br />Demostración aún no disponible</div>
  </div>
}
