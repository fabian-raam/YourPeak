import { useEffect, useMemo, useState } from 'react'
import Icon from '../components/Icon'
import { getProgressHistory, recordWeight } from '../services/progress'
import './ClientProgress.css'

const weightValue = value => Number(value || 0)
const formatWeight = value => weightValue(value).toLocaleString('es-PE', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 2,
})
const formatDate = value => {
  if (!value) return 'Fecha no disponible'
  const date = new Date(`${value}Z`)
  return new Intl.DateTimeFormat('es-PE', {
    day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
  }).format(date)
}

export default function ClientProgress({ onExpired, onNavigate }) {
  const [records, setRecords] = useState([])
  const [weight, setWeight] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  async function loadHistory() {
    setLoading(true)
    try {
      setRecords(await getProgressHistory())
      setError('')
    } catch (err) {
      if (err.status === 401) onExpired()
      else setError(err.message || 'No se pudo consultar tu historial de peso.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadHistory() }, [])

  const chronologicalRecords = useMemo(() => [...records].reverse(), [records])
  const currentWeight = records.length ? weightValue(records[0].pesoKg) : null
  const initialWeight = records.length ? weightValue(records[records.length - 1].pesoKg) : null
  const change = currentWeight !== null && initialWeight !== null ? currentWeight - initialWeight : null
  const chart = useMemo(() => {
    if (chronologicalRecords.length < 2) return null
    const values = chronologicalRecords.map(item => weightValue(item.pesoKg))
    const min = Math.min(...values)
    const max = Math.max(...values)
    const span = max - min || 1
    const points = values.map((value, index) => ({
      x: 48 + index * (624 / (values.length - 1)),
      y: 174 - ((value - min) / span) * 128,
    }))
    return { points, min, max, path: points.map((point, index) => `${index ? 'L' : 'M'} ${point.x} ${point.y}`).join(' ') }
  }, [chronologicalRecords])

  async function submit(event) {
    event.preventDefault()
    const parsedWeight = Number(weight)
    if (!Number.isFinite(parsedWeight) || parsedWeight <= 0) {
      setError('Ingresa un peso mayor que cero.')
      return
    }
    setSaving(true)
    setError('')
    setNotice('')
    try {
      await recordWeight(parsedWeight)
      setWeight('')
      await loadHistory()
      setNotice('Tu peso se agregó al historial.')
    } catch (err) {
      if (err.status === 401) onExpired()
      else setError(err.message || 'No se pudo registrar tu peso.')
    } finally {
      setSaving(false)
    }
  }

  return <section className="client-progress">
    <header className="client-header">
      <div><h1>Mi progreso</h1><p>Registra tu peso y observa cómo cambia con el tiempo.</p></div>
    </header>

    {error && <p className="progress-error" role="alert">{error}</p>}
    {notice && <div className="progress-notice" role="status"><Icon name="check" /><span>{notice}</span>
      <button type="button" aria-label="Cerrar mensaje" onClick={() => setNotice('')}><Icon name="close" /></button>
    </div>}

    <section className="progress-stats" aria-label="Resumen de peso">
      <article><span className="progress-stat-icon"><Icon name="activity" /></span>
        <div><span>Peso actual</span><strong>{loading ? '—' : currentWeight === null ? '—' : <>{formatWeight(currentWeight)} <small>kg</small></>}</strong></div>
      </article>
      <article><span className="progress-stat-icon"><Icon name="refresh" /></span>
        <div><span>Variación desde el inicio</span><strong>{loading ? '—' : change === null ? '—' : <>{change > 0 ? '+' : ''}{formatWeight(change)} <small>kg</small></>}</strong></div>
      </article>
      <article><span className="progress-stat-icon"><Icon name="clock" /></span>
        <div><span>Mediciones</span><strong>{loading ? '—' : records.length}</strong></div>
      </article>
    </section>

    <section className="progress-panel">
      <div className="progress-panel-heading"><div><h2>Evolución del peso</h2>
        <p>Las mediciones se guardan con la fecha en que las registras.</p></div></div>
      {loading ? <p className="progress-empty" role="status">Cargando historial...</p>
        : chart ? <div className="progress-chart-wrap">
          <div className="progress-chart-labels"><span>{formatWeight(chart.max)} kg</span><span>{formatWeight(chart.min)} kg</span></div>
          <svg className="progress-chart" viewBox="0 0 720 210" role="img" aria-label="Gráfico de evolución del peso">
            <path className="progress-chart-grid" d="M48 46H672M48 110H672M48 174H672" />
            <path className="progress-chart-line" d={chart.path} />
            {chart.points.map((point, index) => <circle key={`${point.x}-${index}`} className="progress-chart-point" cx={point.x} cy={point.y} r="5" />)}
          </svg>
          <div className="progress-chart-dates"><span>{formatDate(chronologicalRecords[0].fechaRegistro)}</span>
            <span>{formatDate(chronologicalRecords[chronologicalRecords.length - 1].fechaRegistro)}</span></div>
        </div> : <div className="progress-empty"><span className="progress-empty-icon"><Icon name="activity" /></span>
          <strong>{records.length ? 'Registra otra medición para ver la evolución' : 'Aún no tienes mediciones de peso'}</strong>
          <span>Tu medición inicial del perfil aparecerá aquí cuando completes tu primera medición.</span>
          {!records.length && <button className="progress-profile-link" type="button" onClick={() => onNavigate('perfil')}>Completar perfil inicial</button>}
        </div>}
    </section>

    <section className="progress-panel progress-register">
      <div className="progress-panel-heading"><div><h2>Registrar peso</h2><p>Agrega una nueva medición a tu historial.</p></div></div>
      <form onSubmit={submit}>
        <label htmlFor="progress-weight">Peso actual (kg)</label>
        {!loading && records.length === 0 && <p className="progress-form-hint">Completa primero tu perfil con tu peso y talla iniciales. Después podrás añadir aquí nuevas mediciones de peso.</p>}
        <div className="progress-register-controls">
          <input id="progress-weight" type="number" min="0.01" max="999.99" step="0.01" inputMode="decimal"
            placeholder="Ej. 68.5" value={weight} onChange={event => setWeight(event.target.value)} disabled={saving || loading || records.length === 0} required />
          <button type="submit" disabled={saving || loading || records.length === 0 || !weight}><Icon name="plus" />{saving ? 'Guardando...' : 'Registrar peso'}</button>
        </div>
      </form>
    </section>

    <section className="progress-panel progress-history">
      <div className="progress-panel-heading"><div><h2>Historial</h2>
        <p>{records.length} {records.length === 1 ? 'medición registrada' : 'mediciones registradas'}</p></div></div>
      {loading ? <p className="progress-empty">Cargando historial...</p>
        : records.length === 0 ? <p className="progress-history-empty">Cuando registres tu peso, aparecerá aquí.</p>
          : <ol>{records.map(record => <li key={record.idRegistroFisico}>
            <span className="progress-history-date">{formatDate(record.fechaRegistro)}</span>
            <strong>{formatWeight(record.pesoKg)} <small>kg</small></strong>
          </li>)}</ol>}
    </section>
  </section>
}
