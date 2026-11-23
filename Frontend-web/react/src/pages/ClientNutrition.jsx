import ClientToast from '../components/ClientToast'
import { useEffect, useMemo, useRef, useState } from 'react'
import Icon from '../components/Icon'
import NutritionSummary from '../components/NutritionSummary'
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
const validQuantity = value => Number(value) >= 0.01 && Number(value) <= 99999
const amountLabel = value => `${number(value).toLocaleString('es-PE', { maximumFractionDigits: 2 })}`
const normalizeSummary = result => ({ ...result, ingestas: Array.isArray(result.ingestas) ? result.ingestas : [] })
const formatDay = value => new Intl.DateTimeFormat('es-PE', {
  weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
}).format(new Date(`${value}T12:00:00`))
const formatTime = value => value
  ? new Intl.DateTimeFormat('es-PE', { hour: 'numeric', minute: '2-digit' }).format(new Date(value))
  : 'Hora no disponible'
const limaDate = new Intl.DateTimeFormat('sv-SE', { timeZone: 'America/Lima', dateStyle: 'short' })

export default function ClientNutrition({ onExpired }) {
  const registration = useRef(null)
  const confirmation = useRef(null)
  const searchInput = useRef(null)
  const dayHeading = useRef(null)
  const [deleting, setDeleting] = useState(null)
  const [today] = useState(() => limaDate.format(new Date()))
  const [selectedDate, setSelectedDate] = useState(today)
  const [foods, setFoods] = useState([])
  const [summary, setSummary] = useState(emptySummary)
  const [loadingFoods, setLoadingFoods] = useState(true)
  const [catalogError, setCatalogError] = useState('')
  const [catalogRetry, setCatalogRetry] = useState(0)
  const [loadingDay, setLoadingDay] = useState(true)
  const [dayRevision, setDayRevision] = useState(0)
  const [saving, setSaving] = useState(false)
  const [showRegister, setShowRegister] = useState(false)
  const [dayError, setDayError] = useState('')
  const [foodSearch, setFoodSearch] = useState('')
  const [foodId, setFoodId] = useState('')
  const [quantity, setQuantity] = useState('100')
  const [editingId, setEditingId] = useState(null)
  const [editingQuantity, setEditingQuantity] = useState('')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    let current = true
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
    getNutritionDay(selectedDate).then(result => {
      if (!current) return
      setSummary(normalizeSummary(result))
      setEditingId(null)
    }).catch(err => {
      if (!current) return
      if (err.status === 401) onExpired()
      else setDayError(err.message || 'No se pudo consultar el registro del día.')
    }).finally(() => { if (current) setLoadingDay(false) })
    return () => { current = false }
  }, [selectedDate, dayRevision, onExpired])

  useEffect(() => {
    if (showRegister && registration.current && !registration.current.open) {
      registration.current.showModal()
      searchInput.current?.focus()
    }
    if (deleting && confirmation.current && !confirmation.current.open) confirmation.current.showModal()
  }, [showRegister, deleting])

  const visibleFoods = useMemo(() => {
    const query = foodSearch.trim().toLocaleLowerCase('es')
    return foods.filter(food => !query || food.nombre.toLocaleLowerCase('es').includes(query))
  }, [foods, foodSearch])
  const selectedFood = foods.find(food => String(food.idAlimento) === foodId)
  const preview = selectedFood && validQuantity(quantity) && number(selectedFood.porcionBaseG) > 0
    ? ['kcal', 'proteinaG', 'carbohidratoG', 'grasaG'].map(key =>
      number(selectedFood[key]) * number(quantity) / number(selectedFood.porcionBaseG))
    : null
  const isToday = selectedDate === today
  const canSubmit = Boolean(selectedFood && validQuantity(quantity) && !saving && !loadingFoods && !loadingDay && !dayError)
  const goal = Number(summary.meta?.kcal ?? 0)
  const difference = goal - number(summary.kcal)
  let summaryDescription = 'Sin meta diaria asignada'
  if (goal > 0) {
    if (difference > 0) summaryDescription = `Quedan ${amountLabel(difference)} kcal para tu meta diaria`
    else if (difference < 0) summaryDescription = `Pasaste tu meta diaria por ${amountLabel(-difference)} kcal`
    else summaryDescription = 'Alcanzaste tu meta diaria'
  }

  function selectDate(date) {
    if (!date || date > today || date === selectedDate || saving) return
    setSelectedDate(date)
    setSummary(emptySummary())
    setLoadingDay(true)
    setDayError('')
    setError('')
    setNotice('')
    setEditingId(null)
    setShowRegister(false)
  }

  function moveDay(offset) {
    const date = new Date(`${selectedDate}T12:00:00`)
    date.setDate(date.getDate() + offset)
    selectDate(localDate(date))
  }

  function refreshDay() {
    setLoadingDay(true)
    setDayError('')
    setDayRevision(value => value + 1)
  }

  async function addFood(event) {
    event.preventDefault()
    if (!canSubmit || !isToday) return
    setSaving(true)
    setError('')
    setNotice('')
    try {
      await createIntake({ idAlimento: Number(foodId), cantidadG: Number(quantity) })
      setFoodId('')
      setFoodSearch('')
      setQuantity('100')
      registration.current?.close()
      setNotice('Alimento agregado al registro de hoy.')
      refreshDay()
    } catch (err) {
      if (err.status === 401) onExpired()
      else setError(err.message || 'No se pudo registrar el alimento.')
    } finally { setSaving(false) }
  }

  async function saveEdit(item) {
    if (saving || loadingDay || !validQuantity(editingQuantity)) {
      setError('Ingresa una cantidad entre 0.01 y 99,999 g.')
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
      setEditingId(null)
      setNotice('Se actualizó la cantidad del consumo.')
      refreshDay()
    } catch (err) {
      if (err.status === 401) onExpired()
      else setError(err.message || 'No se pudo actualizar el consumo.')
    } finally { setSaving(false) }
  }

  async function removeItem() {
    if (!deleting || saving || loadingDay) return
    setSaving(true)
    setError('')
    setNotice('')
    try {
      await deleteIntake(deleting.idIngesta)
      confirmation.current?.close()
      dayHeading.current?.focus()
      setNotice('Se eliminó el consumo del registro.')
      refreshDay()
    } catch (err) {
      if (err.status === 401) onExpired()
      else setError(err.message || 'No se pudo eliminar el consumo.')
    } finally { setSaving(false) }
  }

  return <section className="client-nutrition" aria-labelledby="client-page-title">
    {notice && <ClientToast title="Registro actualizado" message={notice} onClose={() => setNotice('')} />}
    {(dayError || (error && !showRegister && !deleting)) && <p className="nutrition-error" role="alert">{dayError || error}</p>}

    <div className="nutrition-toolbar">
      <div className="nutrition-date-control">
        <button type="button" aria-label="Día anterior" disabled={saving || loadingDay} onClick={() => moveDay(-1)}><Icon name="chevron" /></button>
        <button type="button" aria-label="Día siguiente" disabled={isToday || saving || loadingDay} onClick={() => moveDay(1)}><Icon name="chevron" /></button>
        <input id="nutrition-date" type="date" value={selectedDate} max={today} disabled={saving || loadingDay}
          onChange={event => selectDate(event.target.value)} />
        <label htmlFor="nutrition-date">{formatDay(selectedDate)}</label>
        {isToday ? <span className="nutrition-today-badge">Hoy</span> : <button className="nutrition-today" type="button" disabled={saving || loadingDay} onClick={() => selectDate(today)}>Hoy</button>}
      </div>
      {isToday && <button className="nutrition-add-button" type="button" aria-haspopup="dialog"
        onClick={() => { setError(''); setShowRegister(true) }} disabled={saving || loadingDay || Boolean(dayError)}>
        <Icon name="plus" />Registrar alimento
      </button>}
    </div>

    <NutritionSummary daily summary={loadingDay || dayError ? null : summary} id="nutrition-day-summary" title="Resumen del día"
      description={!loadingDay && !dayError && summaryDescription}>
      {loadingDay && <p role="status">Cargando resumen…</p>}
    </NutritionSummary>

    {isToday && showRegister && <dialog className="nutrition-dialog" ref={registration} aria-labelledby="nutrition-register-title"
      onClose={() => { setShowRegister(false); setError('') }}
      onCancel={event => { if (saving) event.preventDefault() }}>
      <header className="nutrition-dialog-heading">
        <div><span className="nutrition-dialog-date">{formatDay(selectedDate)} · Hoy</span>
          <h2 id="nutrition-register-title">Registrar alimento</h2>
          <p>Busca un alimento e indica cuánto consumiste.</p></div>
        <button type="button" className="nutrition-dialog-close" aria-label="Cerrar registro" disabled={saving}
          onClick={() => registration.current.close()}><Icon name="close" /></button>
      </header>
      <form onSubmit={addFood}>
        <div className="nutrition-add-form">
          {error && <p className="nutrition-error" role="alert">{error}</p>}
          <label className="nutrition-search-box"><Icon name="search" />
            <input ref={searchInput} type="search" placeholder="Buscar alimento" aria-label="Buscar alimento"
              value={foodSearch} onChange={event => { setFoodSearch(event.target.value); setFoodId('') }}
              onKeyDown={event => { if (event.key === 'Enter') event.preventDefault() }} disabled={saving} />
          </label>
          {loadingFoods ? <p className="nutrition-food-note" role="status">Cargando catálogo.</p> : catalogError ? <div>
            <p className="nutrition-error" role="alert">{catalogError}</p>
            <button className="nutrition-refresh" type="button" onClick={() => { setLoadingFoods(true); setCatalogRetry(value => value + 1) }}>
              <Icon name="refresh" />Reintentar
            </button>
          </div> : <fieldset className="nutrition-food-list" disabled={saving}>
            <legend className="sr-only">Alimento</legend>
            {visibleFoods.map(food => <label className="nutrition-food-option" key={food.idAlimento}>
              <input type="radio" className="sr-only" name="nutrition-food" value={food.idAlimento}
                checked={foodId === String(food.idAlimento)} onChange={event => setFoodId(event.target.value)} required />
              <span className="nutrition-food-option-main"><Icon name={foodId === String(food.idAlimento) ? 'check' : 'food'} />
                <strong>{food.nombre}</strong></span>
              <small>{amountLabel(food.kcal)} kcal / {amountLabel(food.porcionBaseG)} g</small>
            </label>)}
            {visibleFoods.length === 0 && <p className="nutrition-food-note" role="status">
              {foodSearch.trim() ? 'No hay coincidencias.' : 'El catálogo está vacío.'}
            </p>}
          </fieldset>}
          <div className="nutrition-portion">
            <label className="nutrition-quantity" htmlFor="nutrition-quantity">
              <span>Cantidad (g)</span>
              <input id="nutrition-quantity" type="number" min="0.01" max="99999" step="0.01" value={quantity}
                onChange={event => setQuantity(event.target.value)} disabled={saving} required />
            </label>
            {preview && <div className="nutrition-preview"><small>Estimado para esta cantidad</small>
              <strong>{amountLabel(preview[0])} kcal</strong>
              <span>{amountLabel(preview[1])} g prot. · {amountLabel(preview[2])} g carb. · {amountLabel(preview[3])} g grasa</span>
            </div>}
          </div>
        </div>
        <footer className="nutrition-dialog-footer">
          <button className="nutrition-secondary" type="button" onClick={() => registration.current.close()} disabled={saving}>Cancelar</button>
          <button className="nutrition-add-button" type="submit" disabled={!canSubmit}>
            <Icon name="plus" />{saving ? 'Guardando.' : 'Guardar registro'}
          </button>
        </footer>
      </form>
    </dialog>}

    <section className="nutrition-panel" aria-labelledby="nutrition-entries-title">
      <div className="nutrition-entry-heading">
        <h2 id="nutrition-entries-title" tabIndex={-1} ref={dayHeading}>Registros del día</h2>
        {!loadingDay && !dayError && <span>{summary.ingestas.length} {summary.ingestas.length === 1 ? 'registro' : 'registros'}</span>}
      </div>
      {loadingDay ? <div className="nutrition-loading" role="status">Cargando registro...</div>
        : dayError ? null : summary.ingestas.length === 0 ? <div className="nutrition-empty">
          <span className="nutrition-empty-icon"><Icon name="food" /></span>
          <div><h3>Aún no registraste alimentos para este día</h3>
          <p>{isToday ? 'Usa «Registrar alimento» para agregar el primero.' : 'No hay consumos guardados para esta fecha.'}</p></div>
        </div> : <div className="nutrition-entry-list">
          {summary.ingestas.map(item => <article className="nutrition-entry" key={item.idIngesta}>
            <div className="nutrition-entry-time"><Icon name="clock" />{formatTime(item.fechaHora)}</div>
            <div className="nutrition-entry-food"><strong>{item.alimento}</strong><span>{amountLabel(item.cantidadG)} g</span></div>
            <div className="nutrition-entry-values">
              <strong>{amountLabel(item.kcal)} kcal</strong>
              <span>{amountLabel(item.proteinaG)} g prot. · {amountLabel(item.carbohidratoG)} g carb. · {amountLabel(item.grasaG)} g grasa</span>
            </div>
            <div className="nutrition-entry-actions">
              {editingId === item.idIngesta ? <form className="nutrition-entry-edit"
                onSubmit={event => { event.preventDefault(); saveEdit(item) }}>
                <label htmlFor={`edit-intake-${item.idIngesta}`}>Cantidad (g)</label>
                <input id={`edit-intake-${item.idIngesta}`} type="number" min="0.01" max="99999" step="0.01"
                  value={editingQuantity} onChange={event => setEditingQuantity(event.target.value)} disabled={saving} required />
                <button type="submit" className="nutrition-action-save" disabled={saving}>Guardar</button>
                <button type="button" onClick={() => { setEditingId(null); setError('') }} disabled={saving}>Cancelar</button>
              </form> : <>
                <button type="button" onClick={() => { setError(''); setEditingId(item.idIngesta); setEditingQuantity(String(item.cantidadG)) }} disabled={saving}>Editar</button>
                <button type="button" className="nutrition-action-delete" aria-haspopup="dialog"
                  onClick={() => { setError(''); setDeleting(item) }} disabled={saving}>Eliminar</button>
              </>}
            </div>
          </article>)}
        </div>}
      <footer className="nutrition-panel-footer"><span>Valores calculados según la porción base del catálogo.</span>
        <button type="button" className="nutrition-refresh" onClick={refreshDay}
          disabled={loadingDay || saving}><Icon name="refresh" />Actualizar</button>
      </footer>
    </section>
    {deleting && <dialog className="nutrition-dialog nutrition-delete-dialog" ref={confirmation}
      aria-labelledby="nutrition-delete-title" aria-describedby="nutrition-delete-description"
      onClose={() => { setDeleting(null); setError('') }}
      onCancel={event => { if (saving) event.preventDefault() }}>
      <header className="nutrition-dialog-heading"><div>
        <h2 id="nutrition-delete-title">Eliminar registro</h2>
        <p id="nutrition-delete-description">Se eliminará {deleting.alimento} ({amountLabel(deleting.cantidadG)} g) del registro de ese día.</p>
      </div></header>
      {error && <div className="nutrition-delete-error"><p className="nutrition-error" role="alert">{error}</p></div>}
      <footer className="nutrition-dialog-footer">
        <button type="button" className="nutrition-secondary" onClick={() => confirmation.current.close()} disabled={saving}>Cancelar</button>
        <button type="button" className="nutrition-danger" onClick={removeItem} disabled={saving}>{saving ? 'Eliminando.' : 'Eliminar registro'}</button>
      </footer>
    </dialog>}
  </section>
}
