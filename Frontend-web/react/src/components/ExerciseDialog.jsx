import { useEffect, useId, useRef, useState } from 'react'
import { createExercise, getExercise, getExerciseCatalogs, setExerciseStatus, updateExercise } from '../services/exercises'
import Icon from './Icon'

const emptyExercise = {
  nombre: '', idGrupoMuscular: '', idNivelExperiencia: '', idTipoMedicion: '',
  descripcion: '', equipamiento: '', imagenRuta: '',
}

export default function ExerciseDialog({ exerciseId, initialMode, onClose, onSaved, onEdit, onExpired, onStale }) {
  const dialog = useRef(null)
  const saving = useRef(false)
  const titleId = useId()
  const [mode] = useState(initialMode)
  const [exercise, setExercise] = useState(null)
  const [catalogs, setCatalogs] = useState(null)
  const [form, setForm] = useState(emptyExercise)
  const [loading, setLoading] = useState(initialMode !== 'create')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [fields, setFields] = useState({})
  const [stale, setStale] = useState(false)
  const creating = mode === 'create'
  const editing = mode === 'edit'
  const changingStatus = mode === 'status'
  const formMode = creating || editing
  const active = exercise?.estado === 'ACTIVO'

  useEffect(() => {
    dialog.current?.showModal()
    let current = true
    const load = async () => {
      try {
        const options = await getExerciseCatalogs()
        if (!current) return
        setCatalogs(options)
        if (exerciseId == null) return
        const selected = await getExercise(exerciseId)
        if (!current) return
        setExercise(selected)
        setForm({
          nombre: selected.nombre ?? '',
          idGrupoMuscular: String(selected.idGrupoMuscular ?? ''),
          idNivelExperiencia: String(selected.idNivelExperiencia ?? ''),
          idTipoMedicion: String(selected.idTipoMedicion ?? ''),
          descripcion: selected.descripcion ?? '',
          equipamiento: selected.equipamiento ?? '',
          imagenRuta: selected.imagenRuta ?? '',
        })
      } catch (err) {
        if (!current) return
        if (err.status === 401) { onExpired(); return }
        setError(err.message)
        setStale(true)
      } finally { if (current) setLoading(false) }
    }
    load()
    return () => { current = false }
  }, [exerciseId, onExpired])

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

  async function submit(event) {
    event.preventDefault()
    if (saving.current || stale || loading) return
    const nextFields = {}
    if (formMode) {
      if (!form.nombre.trim()) nextFields.nombre = 'Ingresa el nombre del ejercicio.'
      for (const [field, label] of [
        ['idGrupoMuscular', 'Selecciona un grupo muscular.'],
        ['idNivelExperiencia', 'Selecciona el nivel de experiencia.'],
        ['idTipoMedicion', 'Selecciona el tipo de medición.'],
      ]) if (!form[field]) nextFields[field] = label
    }
    setFields(nextFields)
    if (Object.keys(nextFields).length) {
      document.getElementById(`exercise-${Object.keys(nextFields)[0]}`)?.focus()
      return
    }

    saving.current = true; setBusy(true); setError('')
    try {
      const datos = {
        ...form,
        nombre: form.nombre.trim(),
        idGrupoMuscular: Number(form.idGrupoMuscular),
        idNivelExperiencia: Number(form.idNivelExperiencia),
        idTipoMedicion: Number(form.idTipoMedicion),
        descripcion: form.descripcion.trim() || null,
        equipamiento: form.equipamiento.trim() || null,
        imagenRuta: form.imagenRuta.trim() || null,
      }
      const result = formMode
        ? creating ? await createExercise(datos) : await updateExercise(exerciseId, datos)
        : await setExerciseStatus(exerciseId, active ? 'INACTIVO' : 'ACTIVO')
      onSaved(result, creating ? 'created' : editing ? 'edited' : 'status')
    } catch (err) {
      if (err.status === 401) { onExpired(); return }
      setError(err.message)
      setFields(err.status === 409 ? { nombre: 'Ya existe un ejercicio con ese nombre.' } : err.fields || {})
      if ([403, 404].includes(err.status)) setStale(true)
    } finally { saving.current = false; setBusy(false) }
  }

  const title = creating ? 'Agregar ejercicio' : editing ? 'Editar ejercicio' : changingStatus ? active ? 'Desactivar ejercicio' : 'Reactivar ejercicio' : 'Detalle del ejercicio'
  const select = (name, label, options, hint) => <div className="field exercise-field">
    <label htmlFor={`exercise-${name}`}>{label}</label>
    <div className={`input-wrap exercise-select-wrap ${fields[name] ? 'invalid' : ''}`}>
      <select id={`exercise-${name}`} name={name} value={form[name]} onChange={change} disabled={busy || (!formMode)}
        aria-invalid={!!fields[name]} aria-describedby={fields[name] ? `exercise-${name}-error` : undefined}>
        <option value="">Selecciona una opción</option>
        {(options || []).map(option => <option key={option.id} value={option.id}>{option.nombre}</option>)}
      </select>
    </div>
    {fields[name] && <small className="field-error" id={`exercise-${name}-error`}>{fields[name]}</small>}
    {hint && <small>{hint}</small>}
  </div>

  return <dialog ref={dialog} className="approval-dialog exercise-dialog" aria-labelledby={titleId}
    onCancel={event => { event.preventDefault(); close() }}>
    <div className="dialog-top"><span className="eyebrow">CATÁLOGO DE EJERCICIOS</span>
      <button type="button" className="icon-button" aria-label="Cerrar" disabled={busy} onClick={close}><Icon name="close" /></button></div>
    <h2 id={titleId}>{title}</h2>
    {loading ? <p role="status">Consultando información del ejercicio…</p> : <form onSubmit={submit} noValidate>
      {formMode && !stale && <fieldset className="exercise-form" disabled={busy}>
        <div className="field">
          <label htmlFor="exercise-nombre">Nombre</label>
          <div className={`input-wrap ${fields.nombre ? 'invalid' : ''}`}><input id="exercise-nombre" name="nombre" value={form.nombre}
            onChange={change} maxLength={100} required autoFocus aria-invalid={!!fields.nombre}
            aria-describedby={fields.nombre ? 'exercise-nombre-error' : 'exercise-nombre-hint'} placeholder="Ej. Sentadilla" /></div>
          <small id="exercise-nombre-hint">Nombre único, máximo 100 caracteres.</small>
          {fields.nombre && <small className="field-error" id="exercise-nombre-error">{fields.nombre}</small>}
        </div>
        <div className="exercise-select-grid">
          {select('idGrupoMuscular', 'Grupo muscular', catalogs?.gruposMusculares)}
          {select('idNivelExperiencia', 'Nivel mínimo', catalogs?.nivelesExperiencia)}
          {select('idTipoMedicion', 'Tipo de medición', catalogs?.tiposMedicion)}
        </div>
        <div className="field"><label htmlFor="exercise-descripcion">Descripción</label>
          <div className="input-wrap exercise-textarea-wrap"><textarea id="exercise-descripcion" name="descripcion" value={form.descripcion}
            onChange={change} maxLength={500} rows="3" placeholder="Describe la ejecución o el objetivo del ejercicio." /></div>
          <small>Opcional, máximo 500 caracteres.</small>
        </div>
        <div className="exercise-select-grid">
          <div className="field"><label htmlFor="exercise-equipamiento">Equipamiento</label><div className="input-wrap"><input id="exercise-equipamiento" name="equipamiento" value={form.equipamiento} onChange={change} maxLength={100} placeholder="Ej. Barra" /></div><small>Opcional.</small></div>
          <div className="field"><label htmlFor="exercise-imagenRuta">Ruta de imagen</label><div className="input-wrap"><input id="exercise-imagenRuta" name="imagenRuta" value={form.imagenRuta} onChange={change} maxLength={255} placeholder="/images/sentadilla.png" /></div><small>Opcional; ruta del recurso.</small></div>
        </div>
      </fieldset>}
      {!formMode && exercise && <>
        <div className="exercise-summary"><span className="stat-icon accent"><Icon name="dumbbell" /></span><div><h3>{exercise.nombre}</h3>
          <span className={`status-badge plan-status ${active ? 'is-active' : 'is-inactive'}`}><i />{active ? 'Activo' : 'Inactivo'}</span></div></div>
        <dl className="detail-grid exercise-details">
          <div><dt>Grupo muscular</dt><dd>{exercise.grupoMuscular}</dd></div>
          <div><dt>Nivel mínimo</dt><dd>{exercise.nivelExperiencia}</dd></div>
          <div><dt>Medición</dt><dd>{exercise.tipoMedicion}</dd></div>
          <div><dt>Equipamiento</dt><dd>{exercise.equipamiento || 'No especificado'}</dd></div>
          {exercise.descripcion && <div className="exercise-description"><dt>Descripción</dt><dd>{exercise.descripcion}</dd></div>}
          {exercise.imagenRuta && <div className="exercise-description"><dt>Ruta de imagen</dt><dd>{exercise.imagenRuta}</dd></div>}
        </dl>
        {changingStatus && <p className="plan-info">{active ? 'El ejercicio dejará de estar disponible en el catálogo. Se conservarán sus datos.' : 'El ejercicio volverá a estar disponible en el catálogo.'}</p>}
      </>}
      {error && <p className="alert" role="alert">{error}</p>}
      <div className="dialog-actions">
        <button type="button" className="secondary-button" disabled={busy} onClick={close}>{stale ? 'Volver al catálogo' : formMode || changingStatus ? 'Cancelar' : 'Cerrar'}</button>
        {!stale && (formMode || changingStatus) && <button className={changingStatus && active ? 'primary compact catalog-deactivate' : 'primary compact'} type="submit" disabled={busy}>
          {busy ? 'Guardando…' : creating ? 'Crear ejercicio' : editing ? 'Guardar cambios' : active ? 'Desactivar' : 'Reactivar'}</button>}
        {!stale && mode === 'detail' && exercise && <button className="primary compact" type="button" onClick={onEdit}>Editar ejercicio</button>}
      </div>
    </form>}
    {loading && <div className="dialog-actions"><button className="secondary-button" onClick={close}>Cancelar</button></div>}
  </dialog>
}
