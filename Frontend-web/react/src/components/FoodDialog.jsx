import { useEffect, useId, useRef, useState } from 'react'
import { createFood, getFood, updateFood } from '../services/foods'
import Icon from './Icon'

const emptyFood = {
  nombre: '', porcionBaseG: '', kcal: '', proteinaG: '', carbohidratoG: '', grasaG: '',
}
const numericFields = [
  ['porcionBaseG', 'Porción base', 4, true, 'g'],
  ['kcal', 'Energía', 5, false, 'kcal'],
  ['proteinaG', 'Proteína', 4, false, 'g'],
  ['carbohidratoG', 'Carbohidratos', 4, false, 'g'],
  ['grasaG', 'Grasa', 4, false, 'g'],
]
const format = value => new Intl.NumberFormat('es-PE', { maximumFractionDigits: 2 }).format(Number(value))

export default function FoodDialog({ foodId, initialMode, onClose, onSaved, onEdit, onExpired, onStale }) {
  const dialog = useRef(null)
  const saving = useRef(false)
  const titleId = useId()
  const [mode] = useState(initialMode)
  const [food, setFood] = useState(null)
  const [form, setForm] = useState(emptyFood)
  const [loading, setLoading] = useState(initialMode !== 'create')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [fields, setFields] = useState({})
  const [stale, setStale] = useState(false)
  const creating = mode === 'create'
  const editing = mode === 'edit'
  const formMode = creating || editing

  useEffect(() => {
    dialog.current?.showModal()
    if (foodId == null) return
    let current = true
    getFood(foodId).then(data => {
      if (!current) return
      setFood(data)
      setForm({
        nombre: data.nombre ?? '',
        porcionBaseG: String(data.porcionBaseG ?? ''),
        kcal: String(data.kcal ?? ''),
        proteinaG: String(data.proteinaG ?? ''),
        carbohidratoG: String(data.carbohidratoG ?? ''),
        grasaG: String(data.grasaG ?? ''),
      })
    }).catch(err => {
      if (!current) return
      if (err.status === 401) { onExpired(); return }
      setError(err.message); setStale(true)
    }).finally(() => { if (current) setLoading(false) })
    return () => { current = false }
  }, [foodId, onExpired])

  function close() {
    if (saving.current) return
    if (stale) onStale()
    onClose()
  }

  function change(event) {
    const { name, value } = event.target
    setForm(previous => ({ ...previous, [name]: value }))
    setFields(previous => ({ ...previous, [name]: undefined }))
    setError('')
  }

  function validate() {
    const nextFields = {}
    if (!form.nombre.trim()) nextFields.nombre = 'Ingresa el nombre del alimento.'
    if (form.nombre.trim().length > 120) nextFields.nombre = 'El nombre admite hasta 120 caracteres.'
    for (const [name, label, maxInteger, mustBePositive] of numericFields) {
      const value = form[name].trim()
      const pattern = new RegExp(`^\\d{1,${maxInteger}}(?:\\.\\d{1,2})?$`)
      const number = Number(value)
      if (!value || !pattern.test(value) || !Number.isFinite(number) || (mustBePositive ? number <= 0 : number < 0)) {
        nextFields[name] = mustBePositive
          ? `${label}: ingresa un valor mayor que cero, con hasta 2 decimales.`
          : `${label}: ingresa cero o un valor positivo, con hasta 2 decimales.`
      }
    }
    return nextFields
  }

  async function submit(event) {
    event.preventDefault()
    if (!formMode || saving.current || stale || loading) return
    const nextFields = validate()
    setFields(nextFields)
    if (Object.keys(nextFields).length) {
      document.getElementById(`food-${Object.keys(nextFields)[0]}`)?.focus()
      return
    }

    const data = {
      nombre: form.nombre.trim(),
      porcionBaseG: Number(form.porcionBaseG),
      kcal: Number(form.kcal),
      proteinaG: Number(form.proteinaG),
      carbohidratoG: Number(form.carbohidratoG),
      grasaG: Number(form.grasaG),
    }
    saving.current = true; setBusy(true); setError('')
    try {
      const saved = creating ? await createFood(data) : await updateFood(foodId, data)
      onSaved(saved, creating ? 'created' : 'edited')
    } catch (err) {
      if (err.status === 401) { onExpired(); return }
      setError(err.message)
      setFields(err.status === 409 ? { nombre: 'Ya existe un alimento con ese nombre.' } : err.fields || {})
      if ([403, 404].includes(err.status)) setStale(true)
    } finally { saving.current = false; setBusy(false) }
  }

  const title = creating ? 'Agregar alimento' : editing ? 'Editar alimento' : 'Detalle del alimento'
  const fieldError = (name, hint) => <>
    <small id={`food-${name}-hint`}>{hint}</small>
    {fields[name] && <small className="field-error" id={`food-${name}-error`}>{fields[name]}</small>}
  </>

  return <dialog ref={dialog} className="approval-dialog food-dialog" aria-labelledby={titleId}
    onCancel={event => { event.preventDefault(); close() }}>
    <div className="dialog-top"><span className="eyebrow">CATÁLOGO DE ALIMENTOS</span>
      <button type="button" className="icon-button" aria-label="Cerrar" disabled={busy} onClick={close}><Icon name="close" /></button></div>
    <h2 id={titleId}>{title}</h2>
    {loading ? <p role="status">Consultando alimento…</p> : <form onSubmit={submit} noValidate>
      {formMode && !stale && <fieldset className="food-form" disabled={busy}>
        <div className="field">
          <label htmlFor="food-nombre">Nombre del alimento</label>
          <div className={`input-wrap ${fields.nombre ? 'invalid' : ''}`}><input id="food-nombre" name="nombre" value={form.nombre}
            onChange={change} maxLength={120} required autoFocus aria-invalid={!!fields.nombre}
            aria-describedby={fields.nombre ? 'food-nombre-error' : 'food-nombre-hint'} placeholder="Ej. Avena" /></div>
          {fieldError('nombre', 'Nombre único, máximo 120 caracteres.')}
        </div>
        <div className="food-fields-grid">
          {numericFields.map(([name, label, maxInteger, mustBePositive, unit]) => <div className="field" key={name}>
            <label htmlFor={`food-${name}`}>{label}{name === 'porcionBaseG' ? ' (g)' : ''}</label>
            <div className={`input-wrap ${fields[name] ? 'invalid' : ''}`}><input id={`food-${name}`} name={name}
              type="number" min={mustBePositive ? '0.01' : '0'} max={maxInteger === 5 ? '99999.99' : '9999.99'} step="0.01"
              value={form[name]} onChange={change} required aria-invalid={!!fields[name]}
              aria-describedby={fields[name] ? `food-${name}-error` : `food-${name}-hint`} placeholder="0.00" /></div>
            {fieldError(name, name === 'porcionBaseG' ? `Mayor que 0; máximo ${maxInteger} dígitos enteros y 2 decimales.` : `Por porción base, en ${unit}; admite hasta 2 decimales.`)}
          </div>)}
        </div>
        <p className="food-form-note">Los valores de energía y macronutrientes corresponden a la porción base indicada.</p>
      </fieldset>}
      {!formMode && food && <>
        <div className="food-summary"><span className="stat-icon accent"><Icon name="food" /></span><div><h3>{food.nombre}</h3><span>Alimento #{food.idAlimento}</span></div></div>
        <dl className="detail-grid food-details">
          <div><dt>Porción base</dt><dd>{format(food.porcionBaseG)} g</dd></div>
          <div><dt>Energía</dt><dd>{format(food.kcal)} kcal</dd></div>
          <div><dt>Proteína</dt><dd>{format(food.proteinaG)} g</dd></div>
          <div><dt>Carbohidratos</dt><dd>{format(food.carbohidratoG)} g</dd></div>
          <div><dt>Grasa</dt><dd>{format(food.grasaG)} g</dd></div>
        </dl>
        <p className="food-form-note">Todos los valores nutricionales corresponden a la porción base del alimento.</p>
      </>}
      {error && <p className="alert" role="alert">{error}</p>}
      <div className="dialog-actions">
        <button type="button" className="secondary-button" disabled={busy} onClick={close}>{stale ? 'Volver al catálogo' : formMode ? 'Cancelar' : 'Cerrar'}</button>
        {!stale && formMode && <button className="primary compact" type="submit" disabled={busy}>{busy ? 'Guardando…' : creating ? 'Registrar alimento' : 'Guardar cambios'}</button>}
        {!stale && mode === 'detail' && food && <button className="primary compact" type="button" onClick={onEdit}>Editar alimento</button>}
      </div>
    </form>}
    {loading && <div className="dialog-actions"><button className="secondary-button" onClick={close}>Cancelar</button></div>}
  </dialog>
}
