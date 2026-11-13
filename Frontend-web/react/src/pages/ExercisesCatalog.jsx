import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import Icon from '../components/Icon'
import ExerciseDialog from '../components/ExerciseDialog'
import { getExerciseCatalogs, listExercises } from '../services/exercises'
import './ExercisesCatalog.css'

const PAGE_SIZE = 8
const normalize = value => String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()

export default function ExercisesCatalog({ onExpired, logoutError }) {
  const [exercises, setExercises] = useState([])
  const [groups, setGroups] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [forbidden, setForbidden] = useState(false)
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState('all')
  const [group, setGroup] = useState('all')
  const [page, setPage] = useState(1)
  const [revision, setRevision] = useState(0)
  const [selected, setSelected] = useState(null)
  const sequence = useRef(0)
  const lastTrigger = useRef(null)

  const reload = useCallback(() => {
    setLoading(true); setError(''); setRevision(previous => previous + 1)
  }, [])

  useEffect(() => {
    const current = ++sequence.current
    Promise.all([listExercises(), getExerciseCatalogs()]).then(([items, catalogs]) => {
      if (current !== sequence.current) return
      setExercises(items); setGroups(catalogs.gruposMusculares || []); setForbidden(false)
    }).catch(err => {
      if (current !== sequence.current) return
      if (err.status === 401) { onExpired(); return }
      setError(err.message); setForbidden(err.status === 403)
    }).finally(() => { if (current === sequence.current) setLoading(false) })
    return () => { sequence.current += 1 }
  }, [onExpired, revision])

  const filtered = useMemo(() => {
    const search = normalize(query.trim())
    return exercises.filter(exercise => (status === 'all' || exercise.estado === status)
      && (group === 'all' || String(exercise.idGrupoMuscular) === group)
      && normalize(`${exercise.nombre} ${exercise.grupoMuscular} ${exercise.nivelExperiencia} ${exercise.equipamiento}`).includes(search))
      .sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'))
  }, [exercises, query, status, group])
  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  const currentPage = Math.min(page, totalPages)
  const visible = filtered.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE)
  const activeCount = exercises.filter(item => item.estado === 'ACTIVO').length
  const unavailable = loading || !!error
  const count = value => unavailable ? '—' : value

  function openDialog(mode, exerciseId, event) {
    lastTrigger.current = event.currentTarget
    setSelected({ mode, exerciseId })
  }

  function closeDialog() {
    setSelected(null)
    requestAnimationFrame(() => {
      if (lastTrigger.current?.isConnected) lastTrigger.current.focus()
      else document.getElementById('exercises-heading')?.focus()
    })
  }

  function saved(exercise, action) {
    const verb = action === 'created' ? 'creado' : action === 'edited' ? 'actualizado' : exercise.estado === 'ACTIVO' ? 'reactivado' : 'desactivado'
    setNotice(`El ejercicio «${exercise.nombre}» fue ${verb} correctamente.`)
    closeDialog(); reload()
  }

  function editSelected() { setSelected(previous => ({ ...previous, mode: 'edit' })) }

  return <main className="admin-main exercise-main">
    <div className="admin-heading"><div>
      <h1>Catálogo de ejercicios</h1>
      <p>Administra los ejercicios, sus grupos musculares y niveles recomendados.</p></div>
      <div className="admin-heading-actions">
        <button className="secondary-button" onClick={reload} disabled={loading}><Icon name="refresh" />{loading ? 'Actualizando…' : 'Actualizar'}</button>
        <button className="primary compact" disabled={forbidden} onClick={event => openDialog('create', null, event)}><Icon name="plus" /> Agregar ejercicio</button>
      </div>
    </div>
    <section className="admin-stats catalog-stats" aria-label="Resumen del catálogo de ejercicios">
      <article><span className="stat-icon accent"><Icon name="dumbbell" /></span><div><span>Total de ejercicios</span><strong>{count(exercises.length)}</strong><small>En el catálogo</small></div></article>
      <article><span className="stat-icon green"><Icon name="check" /></span><div><span>Disponibles</span><strong>{count(activeCount)}</strong><small>Activos para asignar</small></div></article>
      <article><span className="stat-icon neutral"><Icon name="pause" /></span><div><span>Inactivos</span><strong>{count(exercises.length - activeCount)}</strong><small>Conservados en el catálogo</small></div></article>
    </section>
    {notice && <p className="alert success" role="status">{notice}</p>}
    {(error || logoutError) && <p className="alert" role="alert">{error || logoutError}</p>}
    <section className="users-panel" aria-label="Catálogo de ejercicios" aria-busy={loading}>
      <div className="users-panel-heading"><h2 className="sr-only" id="exercises-heading" tabIndex="-1">Catálogo de ejercicios</h2>
      </div>
      <div className="users-toolbar exercise-toolbar">
        <label className="search-input"><Icon name="search" /><input type="search" aria-label="Buscar ejercicios"
          placeholder="Buscar por nombre, grupo o equipamiento…" value={query} onChange={event => { setQuery(event.target.value); setPage(1) }} /></label>
        <label className="sort-control">Grupo <select value={group} onChange={event => { setGroup(event.target.value); setPage(1) }}>
          <option value="all">Todos los grupos</option>{groups.map(item => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></label>
        <label className="sort-control">Estado <select value={status} onChange={event => { setStatus(event.target.value); setPage(1) }}>
          <option value="all">Todos</option><option value="ACTIVO">Activos</option><option value="INACTIVO">Inactivos</option></select></label>
      </div>
      {loading ? <div className="table-state" role="status"><span className="loading-dot" /> Consultando ejercicios…</div>
        : error ? <div className="table-state"><Icon name={forbidden ? 'shield' : 'refresh'} /><h3>{forbidden ? 'Acceso no disponible' : 'No pudimos cargar los ejercicios'}</h3>
          <p>{forbidden ? 'Esta sección requiere una cuenta administradora.' : 'Comprueba la conexión y vuelve a consultar el catálogo.'}</p>
          {!forbidden && <button className="secondary-button" onClick={reload}>Volver a intentar</button>}</div>
          : !visible.length ? <div className="table-state"><Icon name={exercises.length ? 'search' : 'dumbbell'} /><h3>{exercises.length ? 'No encontramos coincidencias' : 'El catálogo está vacío'}</h3>
            <p>{exercises.length ? 'Prueba con otro nombre o cambia los filtros.' : 'Agrega el primer ejercicio para comenzar a organizar el catálogo.'}</p>
            {exercises.length > 0 && <button className="secondary-button" onClick={() => { setQuery(''); setStatus('all'); setGroup('all'); setPage(1) }}>Limpiar filtros</button>}</div>
            : <div className="users-table-scroll"><table className="users-table exercise-table"><caption className="sr-only">Catálogo de ejercicios</caption>
              <thead><tr><th scope="col">Ejercicio</th><th scope="col">Grupo muscular</th><th scope="col">Nivel mínimo</th><th scope="col">Medición</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead>
              <tbody>{visible.map(item => <tr key={item.idEjercicio}>
                <td><div className="user-cell"><span className="exercise-row-icon"><Icon name="dumbbell" /></span><div><strong>{item.nombre}</strong><span>Ejercicio #{item.idEjercicio}</span></div></div></td>
                <td>{item.grupoMuscular}</td><td>{item.nivelExperiencia}</td><td>{item.tipoMedicion}</td>
                <td><span className={`status-badge plan-status ${item.estado === 'ACTIVO' ? 'is-active' : 'is-inactive'}`}><i />{item.estado === 'ACTIVO' ? 'Activo' : 'Inactivo'}</span></td>
                <td><div className="plan-actions">
                  <button className="review-button" onClick={event => openDialog('detail', item.idEjercicio, event)}>Ver</button>
                  <button className="review-button" onClick={event => openDialog('edit', item.idEjercicio, event)}>Editar</button>
                  <button className={item.estado === 'ACTIVO' ? 'secondary-button catalog-deactivate' : 'secondary-button'}
                    onClick={event => openDialog('status', item.idEjercicio, event)}>{item.estado === 'ACTIVO' ? 'Desactivar' : 'Reactivar'}</button>
                </div></td>
              </tr>)}</tbody></table></div>}
      <div className="users-pagination"><span>{loading ? 'Actualizando catálogo' : error ? 'Catálogo no disponible' : `${filtered.length} ${filtered.length === 1 ? 'ejercicio' : 'ejercicios'}`}</span>
        <div><button aria-label="Página anterior" disabled={currentPage <= 1 || unavailable} onClick={() => setPage(currentPage - 1)}>←</button>
          <span>{currentPage} / {totalPages}</span><button aria-label="Página siguiente" disabled={currentPage >= totalPages || unavailable} onClick={() => setPage(currentPage + 1)}>→</button></div></div>
    </section>
    <p className="membership-note"><Icon name="shield" /> Los ejercicios se desactivan de forma lógica para conservar sus datos y referencias.</p>
    {selected && <ExerciseDialog key={`${selected.mode}-${selected.exerciseId}`} exerciseId={selected.exerciseId} initialMode={selected.mode}
      onClose={closeDialog} onSaved={saved} onEdit={editSelected} onExpired={onExpired} onStale={reload} />}
  </main>
}
