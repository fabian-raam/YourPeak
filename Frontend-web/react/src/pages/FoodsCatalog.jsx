import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import Icon from '../components/Icon'
import FoodDialog from '../components/FoodDialog'
import { listFoods } from '../services/foods'
import './FoodsCatalog.css'

const PAGE_SIZE = 8
const normalize = value => String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()
const numberFormat = new Intl.NumberFormat('es-PE', { maximumFractionDigits: 2 })

export default function FoodsCatalog({ onExpired, logoutError }) {
  const [foods, setFoods] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [forbidden, setForbidden] = useState(false)
  const [query, setQuery] = useState('')
  const [calorieRange, setCalorieRange] = useState('all')
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
    listFoods().then(data => {
      if (current !== sequence.current) return
      setFoods(data); setForbidden(false)
    }).catch(err => {
      if (current !== sequence.current) return
      if (err.status === 401) { onExpired(); return }
      setError(err.message); setForbidden(err.status === 403)
    }).finally(() => { if (current === sequence.current) setLoading(false) })
    return () => { sequence.current += 1 }
  }, [onExpired, revision])

  const filtered = useMemo(() => {
    const search = normalize(query.trim())
    return foods.filter(food => {
      const calories = Number(food.kcal)
      const matchesRange = calorieRange === 'all'
        || (calorieRange === 'low' && calories <= 100)
        || (calorieRange === 'medium' && calories > 100 && calories <= 200)
        || (calorieRange === 'high' && calories > 200)
      return normalize(food.nombre).includes(search) && matchesRange
    })
      .sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'))
  }, [foods, query, calorieRange])
  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  const currentPage = Math.min(page, totalPages)
  const visible = filtered.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE)
  const count = loading || error ? '—' : foods.length

  function openDialog(mode, foodId, event) {
    lastTrigger.current = event.currentTarget
    setSelected({ mode, foodId })
  }

  function closeDialog() {
    setSelected(null)
    requestAnimationFrame(() => {
      if (lastTrigger.current?.isConnected) lastTrigger.current.focus()
      else document.getElementById('foods-heading')?.focus()
    })
  }

  function saved(food, action) {
    setNotice(`El alimento «${food.nombre}» fue ${action === 'created' ? 'registrado' : 'actualizado'} correctamente.`)
    closeDialog(); reload()
  }

  function editSelected() { setSelected(previous => ({ ...previous, mode: 'edit' })) }

  const format = value => numberFormat.format(Number(value))

  return <main className="admin-main foods-main">
    <div className="admin-heading"><div>
      <h1>Catálogo de alimentos</h1>
      <p>Registra porciones base y consulta sus valores nutricionales.</p></div>
      <div className="admin-heading-actions">
        <button className="secondary-button" onClick={reload} disabled={loading}><Icon name="refresh" />{loading ? 'Actualizando…' : 'Actualizar'}</button>
        <button className="primary compact" disabled={forbidden} onClick={event => openDialog('create', null, event)}><Icon name="plus" /> Agregar alimento</button>
      </div>
    </div>
    <section className="admin-stats foods-stats catalog-stats" aria-label="Resumen del catálogo de alimentos">
      <article><span className="stat-icon accent"><Icon name="food" /></span><div><span>Total de alimentos</span><strong>{count}</strong><small>Valores nutricionales por porción base</small></div></article>
    </section>
    {notice && <p className="alert success" role="status">{notice}</p>}
    {(error || logoutError) && <p className="alert" role="alert">{error || logoutError}</p>}
    <section className="users-panel" aria-label="Catálogo de alimentos" aria-busy={loading}>
      <div className="users-panel-heading"><h2 className="sr-only" id="foods-heading" tabIndex="-1">Catálogo de alimentos</h2>
      </div>
      <div className="users-toolbar foods-toolbar">
        <label className="search-input"><Icon name="search" /><input type="search" aria-label="Buscar alimentos"
          placeholder="Buscar por nombre…" value={query} onChange={event => { setQuery(event.target.value); setPage(1) }} /></label>
        <label className="sort-control">Calorías por porción
          <select value={calorieRange} onChange={event => { setCalorieRange(event.target.value); setPage(1) }}>
            <option value="all">Todos los rangos</option>
            <option value="low">Hasta 100 kcal</option>
            <option value="medium">101–200 kcal</option>
            <option value="high">Más de 200 kcal</option>
          </select>
        </label>
      </div>
      {loading ? <div className="table-state" role="status"><span className="loading-dot" /> Consultando alimentos…</div>
        : error ? <div className="table-state"><Icon name={forbidden ? 'shield' : 'refresh'} /><h3>{forbidden ? 'Acceso no disponible' : 'No pudimos cargar los alimentos'}</h3>
          <p>{forbidden ? 'Esta sección requiere una cuenta administradora.' : 'Comprueba la conexión y vuelve a consultar el catálogo.'}</p>
          {!forbidden && <button className="secondary-button" onClick={reload}>Volver a intentar</button>}</div>
          : !visible.length ? <div className="table-state"><Icon name={foods.length ? 'search' : 'food'} /><h3>{foods.length ? 'No encontramos coincidencias' : 'El catálogo está vacío'}</h3>
            <p>{foods.length ? 'Prueba con otro nombre o cambia el rango de calorías.' : 'Agrega el primer alimento para organizar sus valores nutricionales.'}</p>
            {foods.length > 0 && <button className="secondary-button" onClick={() => { setQuery(''); setCalorieRange('all'); setPage(1) }}>Limpiar filtros</button>}</div>
            : <div className="users-table-scroll"><table className="users-table foods-table"><caption className="sr-only">Catálogo de alimentos</caption>
              <thead><tr><th scope="col">Alimento</th><th scope="col">Porción base</th><th scope="col">Energía</th><th scope="col">Proteína</th><th scope="col">Carbohidratos</th><th scope="col">Grasa</th><th scope="col">Acciones</th></tr></thead>
              <tbody>{visible.map(food => <tr key={food.idAlimento}>
                <td><div className="user-cell"><span className="food-row-icon"><Icon name="food" /></span><div><strong>{food.nombre}</strong><span>Alimento #{food.idAlimento}</span></div></div></td>
                <td>{format(food.porcionBaseG)} g</td><td>{format(food.kcal)} kcal</td>
                <td>{format(food.proteinaG)} g</td><td>{format(food.carbohidratoG)} g</td><td>{format(food.grasaG)} g</td>
                <td><div className="plan-actions">
                  <button className="review-button" onClick={event => openDialog('detail', food.idAlimento, event)}>Ver</button>
                  <button className="review-button" onClick={event => openDialog('edit', food.idAlimento, event)}>Editar</button>
                </div></td>
              </tr>)}</tbody></table></div>}
      <div className="users-pagination"><span>{loading ? 'Actualizando catálogo' : error ? 'Catálogo no disponible' : `${filtered.length} ${filtered.length === 1 ? 'alimento' : 'alimentos'}`}</span>
        <div><button aria-label="Página anterior" disabled={currentPage <= 1 || loading || !!error} onClick={() => setPage(currentPage - 1)}>←</button>
          <span>{currentPage} / {totalPages}</span><button aria-label="Página siguiente" disabled={currentPage >= totalPages || loading || !!error} onClick={() => setPage(currentPage + 1)}>→</button></div></div>
    </section>
    <p className="membership-note"><Icon name="shield" /> Los datos nutricionales se guardan por porción base en el catálogo central.</p>
    {selected && <FoodDialog key={`${selected.mode}-${selected.foodId}`} foodId={selected.foodId} initialMode={selected.mode}
      onClose={closeDialog} onSaved={saved} onEdit={editSelected} onExpired={onExpired} onStale={reload} />}
  </main>
}
