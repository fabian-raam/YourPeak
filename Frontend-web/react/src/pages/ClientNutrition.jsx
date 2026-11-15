import { useEffect, useMemo, useState } from 'react'
import Icon from '../components/Icon'
import {
  createIntake,
  deleteIntake,
  getNutritionDay,
  listNutritionFoods,
  updateIntake,
} from '../services/nutrition'
import './ClientNutrition.css'

const localDate = (date = new Date()) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const emptySummary = () => ({ kcal: 0, proteinaG: 0, carbohidratoG: 0, grasaG: 0, ingestas: [] })
const number = value => Number(value || 0)
const amountLabel = value => `${number(value).toLocaleString('es-PE', { maximumFractionDigits: 2 })}`
const formatDay = value => new Intl.DateTimeFormat('es-PE', {
  weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
}).format(new Date(`${value}T12:00:00`))
const formatTime = value => value
  ? new Intl.DateTimeFormat('es-PE', { hour: 'numeric', minute: '2-digit' }).format(new Date(value))
  : 'Hora no disponible'

export default function ClientNutrition({ onExpired }) {
  const [today] = useState(() => localDate())
  const [selectedDate, setSelectedDate] = useState(() => localDate())
  const [foods, setFoods] = useState([])
  const [summary, setSummary] = useState(emptySummary)
  const [loadingFoods, setLoadingFoods] = useState(true)
  const [catalogError, setCatalogError] = useState('')
  const [catalogRetry, setCatalogRetry] = useState(0)
  const [loadingDay, setLoadingDay] = useState(true)
  const [saving, setSaving] = useState(false)
  const [foodSearch, setFoodSearch] = useState('')
  const [foodId, setFoodId] = useState('')
  const [quantity, setQuantity] = useState('100')
  const [editingId, setEditingId] = useState(null)
  const [editingQuantity, setEditingQuantity] = useState('')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    let current = true
    setLoadingFoods(true)
    listNutritionFoods().then(result => {
      if (!current) return
      setFoods(result)
      setCatalogError('')
    }).catch(err => {
      if (!current) return
      if (err.status === 401) onExpired()
      else setCatalogError(err.message || 'No se pudo consultar el catálogo de alimentos.')
    }).finally(() => { if (current) setLoadingFoods(false) })
    return () => { current = false }
  }, [onExpired, catalogRetry])

  useEffect(() => {
    let current = true
    setLoadingDay(true)
    getNutritionDay(selectedDate).then(result => {
      if (!current) return
      setSummary({
        kcal: number(result.kcal),
        proteinaG: number(result.proteinaG),
        carbohidratoG: number(result.carbohidratoG),
        grasaG: number(result.grasaG),
        ingestas: Array.isArray(result.ingestas) ? result.ingestas : [],
      })
      setEditingId(null)
    }).catch(err => {
      if (!current) return
      if (err.status === 401) onExpired()
      else setError(err.message || 'No se pudo consultar el registro del día.')
    }).finally(() => { if (current) setLoadingDay(false) })
    return () => { current = false }
  }, [selectedDate, onExpired])

  const visibleFoods = useMemo(() => {
    const query = foodSearch.trim().toLocaleLowerCase('es')
    return foods.filter(food => !query || food.nombre.toLocaleLowerCase('es').includes(query))
  }, [foods, foodSearch])
  const selectedFood = foods.find(food => String(food.idAlimento) === foodId)
  const preview = selectedFood && number(quantity) > 0
    ? ['kcal', 'proteinaG', 'carbohidratoG', 'grasaG'].map(key =>
      number(selectedFood[key]) * number(quantity) / number(selectedFood.porcionBaseG))
    : null
  const isToday = selectedDate === today
  const canSubmit = Boolean(selectedFood && number(quantity) > 0 && !saving && !loadingFoods && !loadingDay)

  function moveDay(offset) {
    const date = new Date(`${selectedDate}T12:00:00`)
    date.setDate(date.getDate() + offset)
    const next = localDate(date)
    if (next <= today) {
      setSelectedDate(next)
      setError('')
      setNotice('')
    }
  }

  async function reloadDay() {
    const result = await getNutritionDay(selectedDate)
    setSummary({
      kcal: number(result.kcal),
      proteinaG: number(result.proteinaG),
      carbohidratoG: number(result.carbohidratoG),
      grasaG: number(result.grasaG),
      ingestas: Array.isArray(result.ingestas) ? result.ingestas : [],
    })
  }

  async function addFood(event) {
    event.preventDefault()
    if (!selectedFood || number(quantity) <= 0) return
    setSaving(true)
    setError('')
    setNotice('')
    try {
      await createIntake({ idAlimento: Number(foodId), cantidadG: Number(quantity) })
      await reloadDay()
      setFoodId('')
      setFoodSearch('')
      setQuantity('100')
      setNotice('Alimento agregado al registro de hoy.')
    } catch (err) {
      if (err.status === 401) onExpired()
      else setError(err.message || 'No se pudo registrar el alimento.')
    } finally { setSaving(false) }
  }

  async function saveEdit(item) {
    if (number(editingQuantity) <= 0) {
      setError('La cantidad debe ser mayor que cero.')
      return
    }
    setSaving(true)
    setError('')
    setNotice('')
    try {
      await updateIntake(item.idIngesta, {
        idAlimento: item.idAlimento,
        cantidadG: Number(editingQuantity),
      })
      await reloadDay()
      setEditingId(null)
      setNotice('Se actualizó la cantidad del consumo.')
    } catch (err) {
      if (err.status === 401) onExpired()
      else setError(err.message || 'No se pudo actualizar el consumo.')
    } finally { setSaving(false) }
  }

  async function removeItem(item) {
    if (!window.confirm(`¿Eliminar ${item.alimento} del registro?`)) return
    setSaving(true)
    setError('')
    setNotice('')
    try {
      await deleteIntake(item.idIngesta)
      await reloadDay()
      setNotice('Se eliminó el consumo del registro.')
    } catch (err) {
      if (err.status === 401) onExpired()
      else setError(err.message || 'No se pudo eliminar el consumo.')
    } finally { setSaving(false) }
  }

  return <section className="client-nutrition" aria-labelledby="client-nutrition-title">
    <header className="client-header">
      <h1 id="client-nutrition-title">Nutrición</h1>
      <p>Registra lo que comes y revisa el resumen de cada día.</p>
    </header>

    {notice && <div className="nutrition-notice" role="status">
      <Icon name="check" /><span>{notice}</span>
      <button type="button" aria-label="Cerrar mensaje" onClick={() => setNotice('')}><Icon name="close" /></button>
    </div>}
    {(catalogError || error) && <p className="nutrition-error" role="alert">{catalogError || error}</p>}

    <section className="nutrition-summary" aria-label="Resumen nutricional del día">
      {[
        { key: 'kcal', label: 'Calorías consumidas', unit: 'kcal', style: 'calories' },
        { key: 'proteinaG', label: 'Proteína', unit: 'g', style: 'protein' },
        { key: 'carbohidratoG', label: 'Carbohidratos', unit: 'g', style: 'carbs' },
        { key: 'grasaG', label: 'Grasas', unit: 'g', style: 'fat' },
      ].map(metric => <article className={`nutrition-stat ${metric.style}`} key={metric.key}>
        <span className="nutrition-stat-icon"><Icon name={metric.key === 'kcal' ? 'activity' : 'food'} /></span>
        <div><span>{metric.label}</span><strong>{loadingDay ? '—' : amountLabel(summary[metric.key])}<small> {metric.unit}</small></strong></div>
      </article>)}
    </section>

    <section className="nutrition-panel" aria-label="Registro de alimentos">
      <div className="nutrition-panel-heading">
        <div><h2>Registro diario</h2><p>Los consumos se agrupan por fecha, sin categorías de comida.</p></div>
        <div className="nutrition-date-control">
          <button type="button" aria-label="Día anterior" onClick={() => moveDay(-1)}><span aria-hidden="true">‹</span></button>
          <label htmlFor="nutrition-date">{formatDay(selectedDate)}</label>
          <input id="nutrition-date" type="date" value={selectedDate} max={today}
            onChange={event => event.target.value && setSelectedDate(event.target.value)} />
          <button type="button" aria-label="Día siguiente" disabled={isToday} onClick={() => moveDay(1)}><span aria-hidden="true">›</span></button>
          {!isToday && <button className="nutrition-today" type="button" onClick={() => setSelectedDate(today)}>Hoy</button>}
        </div>
      </div>

      {isToday ? <form className="nutrition-add-form" onSubmit={addFood}>
        <div className="nutrition-add-title"><div><h3>Agregar alimento</h3><p>Indica el alimento y la cantidad que consumiste.</p></div>
          <span>{loadingFoods ? 'Cargando catálogo...' : `${foods.length} alimentos disponibles`}</span></div>
        <div className="nutrition-add-fields">
          <label className="nutrition-food-search" htmlFor="nutrition-food-search">
            <span>Buscar alimento</span>
            <span className="nutrition-search-box"><Icon name="search" />
              <input id="nutrition-food-search" type="search" placeholder="Filtrar alimentos..."
                value={foodSearch} onChange={event => { setFoodSearch(event.target.value); setFoodId('') }} disabled={loadingFoods || saving} />
            </span>
          </label>
          <label className="nutrition-food-select" htmlFor="nutrition-food">
            <span>Alimento</span>
            <select id="nutrition-food" value={foodId} onChange={event => setFoodId(event.target.value)} disabled={loadingFoods || saving} required>
              <option value="">{loadingFoods ? 'Cargando catálogo...' : visibleFoods.length === 0
                ? (foodSearch ? 'No hay coincidencias' : 'El catálogo está vacío')
                : 'Selecciona un alimento'}</option>
              {visibleFoods.map(food => <option key={food.idAlimento} value={food.idAlimento}>{food.nombre}</option>)}
            </select>
          </label>
          <label className="nutrition-quantity" htmlFor="nutrition-quantity">
            <span>Cantidad (g)</span>
            <input id="nutrition-quantity" type="number" min="0.01" max="99999" step="0.01" value={quantity}
              onChange={event => setQuantity(event.target.value)} disabled={saving} required />
          </label>
          <button className="nutrition-add-button" type="submit" disabled={!canSubmit}>
            <Icon name="plus" />{saving ? 'Guardando...' : 'Agregar'}
          </button>
        </div>
        {catalogError && <button className="nutrition-retry-catalog" type="button" disabled={loadingFoods}
          onClick={() => { setError(''); setCatalogRetry(value => value + 1) }}>
          <Icon name="refresh" />{loadingFoods ? 'Cargando...' : 'Reintentar carga del catálogo'}
        </button>}
        {preview && <p className="nutrition-preview">Aproximado para esta cantidad: {amountLabel(preview[0])} kcal · {amountLabel(preview[1])} g proteína · {amountLabel(preview[2])} g carbohidratos · {amountLabel(preview[3])} g grasas</p>}
      </form> : <div className="nutrition-history-note"><Icon name="clock" />Estás revisando un día anterior. Los registros de ese día se pueden corregir o eliminar.</div>}

      <div className="nutrition-entry-heading">
        <h3>Alimentos registrados</h3>
        <span>{summary.ingestas.length} {summary.ingestas.length === 1 ? 'consumo' : 'consumos'}</span>
      </div>
      {loadingDay ? <div className="nutrition-loading" role="status">Cargando registro...</div>
        : summary.ingestas.length === 0 ? <div className="nutrition-empty">
          <span className="nutrition-empty-icon"><Icon name="food" /></span>
          <h3>Aún no hay alimentos registrados</h3>
          <p>{isToday ? 'Agrega lo que consumas durante el día y aparecerá aquí.' : 'No hay consumos guardados para esta fecha.'}</p>
        </div> : <div className="nutrition-entry-list">
          {summary.ingestas.map(item => <article className="nutrition-entry" key={item.idIngesta}>
            <div className="nutrition-entry-time"><Icon name="clock" />{formatTime(item.fechaHora)}</div>
            <div className="nutrition-entry-food"><strong>{item.alimento}</strong><span>{amountLabel(item.cantidadG)} g</span></div>
            <div className="nutrition-entry-values">
              <strong>{amountLabel(item.kcal)} kcal</strong>
              <span>{amountLabel(item.proteinaG)} g prot. · {amountLabel(item.carbohidratoG)} g carb. · {amountLabel(item.grasaG)} g grasa</span>
            </div>
            <div className="nutrition-entry-actions">
              {editingId === item.idIngesta ? <>
                <label className="sr-only" htmlFor={`edit-intake-${item.idIngesta}`}>Cantidad en gramos</label>
                <input id={`edit-intake-${item.idIngesta}`} type="number" min="0.01" max="99999" step="0.01"
                  value={editingQuantity} onChange={event => setEditingQuantity(event.target.value)} disabled={saving} />
                <button type="button" className="nutrition-action-save" onClick={() => saveEdit(item)} disabled={saving}>Guardar</button>
                <button type="button" onClick={() => setEditingId(null)} disabled={saving}>Cancelar</button>
              </> : <>
                <button type="button" onClick={() => { setEditingId(item.idIngesta); setEditingQuantity(String(item.cantidadG)) }} disabled={saving}>Editar</button>
                <button type="button" className="nutrition-action-delete" onClick={() => removeItem(item)} disabled={saving}>Eliminar</button>
              </>}
            </div>
          </article>)}
        </div>}
      <footer className="nutrition-panel-footer"><span>Valores calculados según la porción base del catálogo.</span>
        <button type="button" className="nutrition-refresh" onClick={() => {
          setLoadingDay(true)
          reloadDay().catch(err => {
            if (err.status === 401) onExpired()
            else setError(err.message || 'No se pudo actualizar el registro.')
          }).finally(() => setLoadingDay(false))
        }} disabled={loadingDay || saving}><Icon name="refresh" />Actualizar</button>
      </footer>
    </section>
  </section>
}
