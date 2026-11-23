import { useEffect, useState } from 'react'
import ClientToast from '../components/ClientToast'
import Icon from '../components/Icon'
import { getClientProfile, saveClientProfile } from '../services/clientProfile'
import { fullName, initials } from '../utils/format'
import './ClientInitialProfile.css'

const emptyCatalogs = { objetivos: [], nivelesExperiencia: [], nivelesActividad: [] }
const sexLabels = { HOMBRE: 'Hombre', MUJER: 'Mujer' }

function formFromProfile(profile) {
  return {
    idTipoObjetivo: String(profile.idTipoObjetivo ?? ''),
    idNivelExperiencia: String(profile.idNivelExperiencia ?? ''),
    idNivelActividad: String(profile.idNivelActividad ?? ''),
    sexo: profile.sexo ?? '',
    pesoKg: profile.pesoKg ?? '',
    tallaCm: profile.tallaCm ?? '',
  }
}

export default function ClientInitialProfile({ user, onExpired }) {
  const [catalogs, setCatalogs] = useState(emptyCatalogs)
  const [form, setForm] = useState(() => formFromProfile({}))
  const [savedSex, setSavedSex] = useState('')
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [loadAttempt, setLoadAttempt] = useState(0)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    let current = true
    getClientProfile().then(profile => {
      if (!current) return
      setCatalogs(profile.catalogos || emptyCatalogs)
      setForm(formFromProfile(profile))
      setSavedSex(profile.sexo || '')
    }).catch(err => {
      if (!current) return
      if (err.status === 401) onExpired()
      else setLoadError(err.message || 'No se pudo cargar tu perfil.')
    }).finally(() => { if (current) setLoading(false) })
    return () => { current = false }
  }, [onExpired, loadAttempt])

  function retry() {
    setLoadError('')
    setLoading(true)
    setLoadAttempt(value => value + 1)
  }

  function update(event) {
    const { name, value } = event.target
    setForm(previous => ({ ...previous, [name]: value }))
    setError('')
    setNotice('')
  }

  async function submit(event) {
    event.preventDefault()
    if (saving) return
    setError('')
    setNotice('')
    setSaving(true)
    try {
      const profile = await saveClientProfile({
        idTipoObjetivo: Number(form.idTipoObjetivo),
        idNivelExperiencia: Number(form.idNivelExperiencia),
        idNivelActividad: Number(form.idNivelActividad),
        sexo: savedSex || form.sexo,
        pesoKg: Number(form.pesoKg),
        tallaCm: Number(form.tallaCm),
      })
      setForm(formFromProfile(profile))
      setSavedSex(profile.sexo || '')
      if (profile.catalogos) setCatalogs(profile.catalogos)
      setNotice('Tu perfil se guardó correctamente.')
    } catch (err) {
      if (err.status === 401) onExpired()
      else setError(err.message || 'No se pudo guardar tu perfil. Revisa los datos e inténtalo de nuevo.')
    } finally {
      setSaving(false)
    }
  }

  return <section className="client-initial-profile" aria-labelledby="client-page-title">
    <div className="initial-profile-intro">
      <span className="initial-profile-avatar" aria-hidden="true">{initials(user || {}) || 'C'}</span>
      <div>
        <h2>{fullName(user || {}) || 'Datos personales'}</h2>
        {user?.correo && <p>{user.correo}</p>}
      </div>
    </div>

    {notice && <ClientToast title="Perfil guardado" message={notice} onClose={() => setNotice('')} />}

    {loading ? <p className="initial-profile-loading" role="status">Cargando tu perfil…</p>
      : loadError ? <div className="initial-profile-alert">
        <p role="alert">{loadError}</p>
        <button className="client-text-link" type="button" onClick={retry}>Reintentar</button>
      </div> : <form className="initial-profile-form" onSubmit={submit}>
        <div className="initial-profile-panels">
          <section className="initial-profile-panel" aria-labelledby="initial-profile-training">
            <h3 id="initial-profile-training">Entrenamiento</h3>
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
            </div>
          </section>

          <section className="initial-profile-panel initial-profile-physical" aria-labelledby="initial-profile-measurements">
            <h3 id="initial-profile-measurements">Datos físicos</h3>
            <div className="initial-profile-fields initial-profile-measures">
              <label>
                <span>Peso<span className="sr-only"> (kg)</span></span>
                <span className="initial-profile-input">
                  <input name="pesoKg" type="number" min="1" max="999.99" step="0.01" inputMode="decimal" value={form.pesoKg}
                    onChange={update} placeholder="Ej. 70.5" required disabled={saving} />
                  <span aria-hidden="true">kg</span>
                </span>
              </label>
              <label>
                <span>Talla<span className="sr-only"> (cm)</span></span>
                <span className="initial-profile-input">
                  <input name="tallaCm" type="number" min="1" max="999.99" step="0.01" inputMode="decimal" value={form.tallaCm}
                    onChange={update} placeholder="Ej. 175" required disabled={saving} />
                  <span aria-hidden="true">cm</span>
                </span>
              </label>
              <div className="initial-profile-sex">
                <label>
                  <span>Sexo</span>
                  {savedSex ? <span className="initial-profile-input">
                    <input type="text" value={sexLabels[savedSex] ?? savedSex} readOnly aria-describedby="initial-profile-sex-help" />
                    <span aria-hidden="true"><Icon name="lock" /></span>
                  </span> : <select name="sexo" value={form.sexo} onChange={update} required disabled={saving}
                    aria-describedby="initial-profile-sex-help">
                    <option value="">Selecciona tu sexo</option>
                    {Object.entries(sexLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                  </select>}
                </label>
                <p id="initial-profile-sex-help">{savedSex ? 'Este dato queda fijo después de guardarlo.' : 'Puedes elegirlo antes de guardar. Después quedará fijo.'}</p>
              </div>
            </div>
            <p className="initial-profile-help"><Icon name="activity" /> Si cambias tu peso o talla, se añade una medición a tu historial.</p>
          </section>
        </div>

        <div className="initial-profile-actions">
          {error && <p className="initial-profile-alert" role="alert">{error}</p>}
          <button type="submit" disabled={saving}>
            <Icon name="check" /> {saving ? 'Guardando…' : 'Guardar perfil'}
          </button>
        </div>
      </form>}
  </section>
}
