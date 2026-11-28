import { useEffect, useId, useRef, useState } from 'react'
import { createExercise, getExercise, getExerciseCatalogs, setExerciseStatus, updateExercise } from '../services/exercises'
import Icon from './Icon'
import { exerciseImage, exerciseSlug, uploadedExerciseImage } from '../utils/exerciseIllustrations'

const emptyExercise = {
  nombre: '', idGrupoMuscular: '', idNivelExperiencia: '', idTipoMedicion: '',
  descripcion: '', equipamiento: '',
}

const imageTypes = ['image/jpeg', 'image/png']

function readImage(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result.split(',')[1])
    reader.onerror = () => reject(new Error('No se pudo leer la imagen seleccionada.'))
    reader.readAsDataURL(file)
  })
}

export default function ExerciseDialog({ exerciseId, initialMode, onClose, onSaved, onEdit, onExpired, onStale }) {
  const dialog = useRef(null)
  const saving = useRef(false)
  const titleId = useId()
  const [mode, setMode] = useState(initialMode)
  const [exercise, setExercise] = useState(null)
  const [catalogs, setCatalogs] = useState(null)
  const [form, setForm] = useState(emptyExercise)
  const [loading, setLoading] = useState(initialMode !== 'create')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [fields, setFields] = useState({})
  const [stale, setStale] = useState(false)
  const [image, setImage] = useState(null)
  const [imageError, setImageError] = useState('')
  const fileInput = useRef(null)
  const creating = mode === 'create'
  const editing = mode === 'edit'
  const changingStatus = mode === 'status'
  const formMode = creating || editing
  const active = exercise?.estado === 'ACTIVO'
  const currentImage = exercise ? exerciseImage(exercise) : null
  const imagePreview = image?.url ?? currentImage

  useEffect(() => {
    if (image) return () => URL.revokeObjectURL(image.url)
  }, [image])

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

  function changeImage(event) {
    const file = event.target.files[0]
    if (!file) return
    setImage(null)
    setError('')
    setFields(previous => ({ ...previous, imagenBase64: undefined }))
    if (!imageTypes.includes(file.type) || file.size === 0 || file.size > 1024 * 1024) {
      setImageError('Selecciona una imagen JPG o PNG de hasta 1 MB.')
      return
    }
    setImageError('')
    setImage({ file, url: URL.createObjectURL(file) })
  }

  function discardImage() {
    fileInput.current.value = ''
    setImage(null)
    setImageError('')
    setError('')
    setFields(previous => ({ ...previous, imagenBase64: undefined }))
  }

  async function submit(event) {
    event.preventDefault()
    if (saving.current || stale || loading || imageError) return
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
        ...(image && { imagenBase64: await readImage(image.file) }),
        ...(editing && !image && !exercise.imagenRuta && currentImage
          && { imagenRuta: exerciseSlug(exercise.nombre) }),
      }
      const result = formMode
        ? creating ? await createExercise(datos) : await updateExercise(exercise.idEjercicio, datos)
        : await setExerciseStatus(exerciseId, active ? 'INACTIVO' : 'ACTIVO')
      if (datos.imagenBase64 && (!uploadedExerciseImage(result.imagenRuta) || result.imagenRuta === exercise?.imagenRuta)) {
        setExercise(result)
        setMode('edit')
        setError('Los datos del ejercicio se guardaron, pero el servidor no confirmó la imagen. Comprueba que Spring Boot esté ejecutando la versión que admite la carga de imágenes y vuelve a guardar.')
        onStale()
        return
      }
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

  return <dialog ref={dialog} className="approval-dialog exercise-dialog catalog-dialog" data-mode={mode} aria-labelledby={titleId}
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
        <div className="field"><label htmlFor="exercise-equipamiento">Equipamiento</label><div className="input-wrap"><input id="exercise-equipamiento" name="equipamiento" value={form.equipamiento} onChange={change} maxLength={100} placeholder="Ej. Barra" /></div><small>Opcional.</small></div>
        <div className="field exercise-image-field">
          <label htmlFor="exercise-imagen">Imagen del ejercicio</label>
          {imagePreview && <img src={imagePreview} alt={image ? 'Vista previa de la nueva imagen' : 'Imagen actual del ejercicio'} />}
          <input ref={fileInput} id="exercise-imagen" type="file" accept="image/jpeg,image/png"
            onChange={changeImage} aria-invalid={Boolean(imageError || fields.imagenBase64)}
            aria-describedby="exercise-image-hint" />
          <small id="exercise-image-hint">Opcional. JPG o PNG, hasta 1 MB y 2048 × 2048 píxeles.</small>
          {(imageError || fields.imagenBase64) && <small className="field-error" role="alert">{imageError || fields.imagenBase64}</small>}
          {(image || imageError) && <button type="button" className="secondary-button" onClick={discardImage}>Quitar selección</button>}
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
        </dl>
        {changingStatus && <p className={active ? "plan-info plan-warning" : "plan-info"}><Icon name={active ? "shield" : "info"} /><span>{active ? 'El ejercicio dejará de estar disponible en el catálogo. Se conservarán sus datos.' : 'El ejercicio volverá a estar disponible en el catálogo.'}</span></p>}
      </>}
      {error && <p className="alert" role="alert">{error}</p>}
      <div className="dialog-actions">
        <button type="button" className="secondary-button" disabled={busy} onClick={close}>{stale ? 'Volver al catálogo' : formMode || changingStatus ? 'Cancelar' : 'Cerrar'}</button>
        {!stale && (formMode || changingStatus) && <button className={changingStatus && active ? 'primary compact catalog-deactivate' : 'primary compact'} type="submit" disabled={busy || Boolean(imageError)}>
          {busy ? 'Guardando…' : creating ? 'Crear ejercicio' : editing ? 'Guardar cambios' : active ? 'Desactivar' : 'Activar'}</button>}
        {!stale && mode === 'detail' && exercise && <button className="primary compact" type="button" onClick={onEdit}>Editar ejercicio</button>}
      </div>
    </form>}
    {loading && <div className="dialog-actions"><button className="secondary-button" onClick={close}>Cancelar</button></div>}
  </dialog>
}
