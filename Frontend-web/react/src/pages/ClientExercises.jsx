import { useMemo, useState } from 'react'
import Icon from '../components/Icon'
import './ClientExercises.css'

const exercises = [
  { id: 1, nombre: 'Press de banca', grupo: 'Pecho', nivel: 'Principiante', equipo: 'Barra y banco', medicion: 'Repeticiones', descripcion: 'Empuja la barra desde el pecho manteniendo los pies apoyados y la espalda estable.' },
  { id: 2, nombre: 'Press inclinado con mancuernas', grupo: 'Pecho', nivel: 'Principiante', equipo: 'Mancuernas y banco inclinado', medicion: 'Repeticiones', descripcion: 'Empuja las mancuernas desde un banco inclinado y baja con control.' },
  { id: 3, nombre: 'Flexiones de pecho', grupo: 'Pecho', nivel: 'Principiante', equipo: 'Peso corporal', medicion: 'Repeticiones', descripcion: 'Flexiona los brazos y acerca el pecho al suelo manteniendo el cuerpo alineado.' },
  { id: 4, nombre: 'Aperturas con mancuernas', grupo: 'Pecho', nivel: 'Intermedio', equipo: 'Mancuernas y banco', medicion: 'Repeticiones', descripcion: 'Abre y cierra los brazos con una ligera flexión de codos.' },
  { id: 5, nombre: 'Jalón al pecho', grupo: 'Espalda', nivel: 'Principiante', equipo: 'Polea alta y barra', medicion: 'Repeticiones', descripcion: 'Lleva la barra hacia la parte superior del pecho con control.' },
  { id: 6, nombre: 'Remo con mancuerna', grupo: 'Espalda', nivel: 'Principiante', equipo: 'Mancuerna y banco', medicion: 'Repeticiones', descripcion: 'Lleva la mancuerna hacia la cadera manteniendo la espalda estable.' },
  { id: 7, nombre: 'Peso muerto rumano', grupo: 'Espalda', nivel: 'Intermedio', equipo: 'Barra o mancuernas', medicion: 'Repeticiones', descripcion: 'Lleva la cadera hacia atrás y baja la carga manteniendo la espalda neutra.' },
  { id: 8, nombre: 'Sentadilla goblet', grupo: 'Piernas', nivel: 'Principiante', equipo: 'Mancuerna o pesa rusa', medicion: 'Repeticiones', descripcion: 'Desciende con una mancuerna frente al pecho y vuelve a subir.' },
  { id: 9, nombre: 'Prensa de piernas', grupo: 'Piernas', nivel: 'Principiante', equipo: 'Máquina de prensa', medicion: 'Repeticiones', descripcion: 'Empuja la plataforma sin bloquear las rodillas al extender.' },
  { id: 10, nombre: 'Elevaciones laterales', grupo: 'Hombros', nivel: 'Principiante', equipo: 'Mancuernas', medicion: 'Repeticiones', descripcion: 'Eleva los brazos hacia los lados hasta la altura de los hombros.' },
  { id: 11, nombre: 'Curl de bíceps', grupo: 'Brazos', nivel: 'Principiante', equipo: 'Mancuernas', medicion: 'Repeticiones', descripcion: 'Flexiona los codos para acercar las mancuernas a los hombros.' },
  { id: 12, nombre: 'Plancha frontal', grupo: 'Core', nivel: 'Principiante', equipo: 'Peso corporal', medicion: 'Tiempo', descripcion: 'Mantén el cuerpo alineado apoyado en antebrazos y pies.' },
]

const grupos = ['Todos', 'Pecho', 'Espalda', 'Piernas', 'Hombros', 'Brazos', 'Core']
const niveles = ['Todos', 'Principiante', 'Intermedio', 'Avanzado']
const normalizar = value => String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()

export default function ClientExercises() {
  const [busqueda, setBusqueda] = useState('')
  const [grupo, setGrupo] = useState('Todos')
  const [nivel, setNivel] = useState('Todos')
  const [seleccionado, setSeleccionado] = useState(null)

  const filtrados = useMemo(() => {
    const texto = normalizar(busqueda.trim())
    return exercises.filter(item => (grupo === 'Todos' || item.grupo === grupo)
      && (nivel === 'Todos' || item.nivel === nivel)
      && normalizar(`${item.nombre} ${item.grupo} ${item.equipo}`).includes(texto))
  }, [busqueda, grupo, nivel])

  function limpiarFiltros() {
    setBusqueda('')
    setGrupo('Todos')
    setNivel('Todos')
  }

  return <main className="client-exercises">
    <header className="client-page-heading">
      <div><h1>Ejercicios</h1>
        <p>Explora los ejercicios del gimnasio por grupo muscular y nivel.</p></div>
    </header>

    <section className="client-exercise-filters" aria-label="Filtros de ejercicios">
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
    </section>

    <section className="client-exercise-list" aria-label="Catálogo de ejercicios">
      {filtrados.length ? <div className="client-exercise-table-scroll"><table>
        <thead><tr><th>Ejercicio</th><th>Grupo muscular</th><th>Nivel</th><th>Equipamiento</th><th /></tr></thead>
        <tbody>{filtrados.map(item => <tr key={item.id}>
          <td><div className="client-exercise-name"><span><Icon name="dumbbell" /></span><strong>{item.nombre}</strong></div></td>
          <td>{item.grupo}</td><td><span className="client-level-pill">{item.nivel}</span></td><td>{item.equipo}</td>
          <td><button type="button" className="client-exercise-detail" onClick={() => setSeleccionado(item)}>Ver detalle <Icon name="arrow" /></button></td>
        </tr>)}</tbody>
      </table></div> : <div className="client-exercise-empty"><span><Icon name="search" /></span>
        <h2>No encontramos ejercicios</h2><p>Prueba con otro nombre o cambia los filtros.</p>
        <button type="button" onClick={limpiarFiltros}>Limpiar filtros</button></div>}
    </section>

    {seleccionado && <div className="client-detail-backdrop" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget) setSeleccionado(null) }}>
      <section className="client-detail-dialog" role="dialog" aria-modal="true" aria-labelledby="client-detail-title">
        <button className="client-detail-close" type="button" aria-label="Cerrar detalle" onClick={() => setSeleccionado(null)}><Icon name="close" /></button>
        <span className="client-page-kicker">EJERCICIO</span><h2 id="client-detail-title">{seleccionado.nombre}</h2>
        <p className="client-detail-description">{seleccionado.descripcion}</p>
        <dl><div><dt>Grupo muscular</dt><dd>{seleccionado.grupo}</dd></div>
          <div><dt>Nivel</dt><dd>{seleccionado.nivel}</dd></div>
          <div><dt>Equipamiento</dt><dd>{seleccionado.equipo}</dd></div>
          <div><dt>Tipo de medición</dt><dd>{seleccionado.medicion}</dd></div></dl>
        <div className="client-detail-note"><Icon name="activity" /><span>Realiza el movimiento con control y mantén una postura cómoda.</span></div>
        <button type="button" className="client-detail-done" onClick={() => setSeleccionado(null)}>Cerrar</button>
      </section>
    </div>}
  </main>
}
