import { useEffect, useState } from 'react'
import Icon from '../components/Icon'
import { getClientProfile, saveClientProfile } from '../services/clientProfile'
import './ClientInitialProfile.css'

const blankForm = {
  idTipoObjetivo: '',
  idNivelExperiencia: '',
  idNivelActividad: '',
  pesoKg: '',
  tallaCm: '',
}

function formFromProfile(profile) {
  return {
    idTipoObjetivo: profile.idTipoObjetivo ? String(profile.idTipoObjetivo) : '',
    idNivelExperiencia: profile.idNivelExperiencia ? String(profile.idNivelExperiencia) : '',
    idNivelActividad: profile.idNivelActividad ? String(profile.idNivelActividad) : '',
    pesoKg: profile.pesoKg ?? '',
    tallaCm: profile.tallaCm ?? '',
  }
}

export default function ClientInitialProfile({ user, onExpired }) {
  const [catalogs, setCatalogs] = useState({ objetivos: [], nivelesExperiencia: [], nivelesActividad: [] })
  const [form, setForm] = useState(blankForm)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    let current = true
    getClientProfile().then(profile => {
      if (!current) return
      setCatalogs(profile.catalogos || { objetivos: [], nivelesExperiencia: [], nivelesActividad: [] })
      setForm(formFromProfile(profile))
    }).catch(err => {
      if (!current) return
      if (err.status === 401) onExpired()
      else setError(err.message || 'No se pudo cargar tu perfil.')
    }).finally(() => { if (current) setLoading(false) })
    return () => { current = false }
  }, [onExpired])

  function update(event) {
    const { name, value } = event.target
    setForm(previous => ({ ...previous, [name]: value }))
    setError('')
    setNotice('')
  }

  async function submit(event) {
    event.preventDefault()
    setError('')
    setNotice('')
    setSaving(true)
    try {
      const profile = await saveClientProfile({
        idTipoObjetivo: Number(form.idTipoObjetivo),
        idNivelExperiencia: Number(form.idNivelExperiencia),
        idNivelActividad: Number(form.idNivelActividad),
        pesoKg: Number(form.pesoKg),
        tallaCm: Number(form.tallaCm),
      })
      setForm(formFromProfile(profile))
      setCatalogs(profile.catalogos || catalogs)
      setNotice('Tu perfil se guardó correctamente.')
    } catch (err) {
      if (err.status === 401) onExpired()
      else setError(err.message || 'No se pudo guardar tu perfil. Revisa los datos e inténtalo de nuevo.')
    } finally {
      setSaving(false)
    }
  }

  return <section className="client-initial-profile" aria-labelledby="initial-profile-title">
    <header className="client-header">
      <div>
        <h1 id="initial-profile-title">Mi perfil</h1>
        <p>Cuéntanos sobre tus objetivos y tu experiencia para personalizar tu entrenamiento.</p>
      </div>
    </header>

    <section className="client-card initial-profile-card">
      <div className="initial-profile-intro">
        <span className="client-avatar" aria-hidden="true">
          {(user?.nombre?.[0] || 'C').toUpperCase()}{(user?.apellido?.[0] || '').toUpperCase()}
        </span>
        <div>
          <h2>{[user?.nombre, user?.apellido].filter(Boolean).join(' ') || 'Datos personales'}</h2>
          <p>{user?.correo || ''}</p>
        </div>
      </div>

      {error && <div className="initial-profile-alert" role="alert">{error}</div>}
      {notice && <div className="initial-profile-notice" role="status">{notice}</div>}
      {loading ? <p className="initial-profile-loading" role="status">Cargando tu perfil…</p> :
        <form className="initial-profile-form" onSubmit={submit}>
          <div className="initial-profile-fields">
            <label>
              <span>Objetivo</span>
              <select name="idTipoObjetivo" value={form.idTipoObjetivo} onChange={update} required disabled={saving}>
                <option value="">Selecciona tu objetivo</option>
                {catalogs.objetivos.map(option => <option key={option.id} value={option.id}>{option.nombre}</option>)}
              </select>
            </label>
            <label>
              <span>Nivel de experiencia</span>
              <select name="idNivelExperiencia" value={form.idNivelExperiencia} onChange={update} required disabled={saving}>
                <option value="">Selecciona tu experiencia</option>
                {catalogs.nivelesExperiencia.map(option => <option key={option.id} value={option.id}>{option.nombre}</option>)}
              </select>
            </label>
            <label>
              <span>Nivel de actividad</span>
              <select name="idNivelActividad" value={form.idNivelActividad} onChange={update} required disabled={saving}>
                <option value="">Selecciona tu actividad</option>
                {catalogs.nivelesActividad.map(option => <option key={option.id} value={option.id}>{option.nombre}</option>)}
              </select>
            </label>
            <label>
              <span>Peso (kg)</span>
              <input name="pesoKg" type="number" min="1" max="999.99" step="0.01" value={form.pesoKg}
                onChange={update} placeholder="Ej. 70.5" required disabled={saving} />
            </label>
            <label>
              <span>Talla (cm)</span>
              <input name="tallaCm" type="number" min="1" max="999.99" step="0.01" value={form.tallaCm}
                onChange={update} placeholder="Ej. 175" required disabled={saving} />
            </label>
          </div>
          <p className="initial-profile-help">Tus medidas se guardan como un registro para poder consultar cambios con el tiempo.</p>
          <div className="initial-profile-actions">
            <button type="submit" disabled={saving}>
              <Icon name="check" /> {saving ? 'Guardando…' : 'Guardar perfil'}
            </button>
          </div>
        </form>}
    </section>
  </section>
}
