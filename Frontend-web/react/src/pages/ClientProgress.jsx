import ClientToast from '../components/ClientToast'
import { useEffect, useMemo, useRef, useState } from 'react'
import Icon from '../components/Icon'
import { getProgressHistory, recordWeight } from '../services/progress'
import './ClientProgress.css'

const weightFormat = new Intl.NumberFormat('es-PE', { minimumFractionDigits: 1, maximumFractionDigits: 2 })
const axisFormat = new Intl.NumberFormat('es-PE', { maximumFractionDigits: 2 })
const shortDate = new Intl.DateTimeFormat('es-PE', { day: 'numeric', month: 'short' })
const fullDate = new Intl.DateTimeFormat('es-PE', { day: 'numeric', month: 'short', year: 'numeric' })
const clock = new Intl.DateTimeFormat('es-PE', { hour: 'numeric', minute: '2-digit' })
const parseTime = value => value ? Date.parse(/(?:Z|[+-]\d{2}:\d{2})$/i.test(value) ? value : value + 'Z') : NaN
const dateLabel = (formatter, time) => Number.isFinite(time) ? formatter.format(time) : 'Fecha no disponible'
const weightLabel = value => weightFormat.format(Number(value))

const chartWidth = 476
const plot = { left: 56, right: 439, top: 25, bottom: 173 }
const dateLabelY = plot.bottom + 32
const chartHeight = dateLabelY + 11

function createChart(records) {
  const readings = records.map(record => ({
    id: record.idRegistroFisico, kg: Number(record.pesoKg), time: parseTime(record.fechaRegistro),
  }))
  const values = readings.map(point => point.kg)
  const low = Math.min(...values)
  const high = Math.max(...values)
  const gridStep = Math.max(0.5, Math.ceil((high - low) / 1.5) / 2)
  const minimum = Math.floor(low / gridStep) * gridStep
  const maximum = Math.max(Math.ceil(high / gridStep) * gridStep, minimum + 2 * gridStep)
  const dates = readings.map(point => point.time)
  const first = dates[0]
  const span = dates.at(-1) - first
  const useTime = dates.every(Number.isFinite) && span > 0
  const points = readings.map((point, index) => ({
    ...point,
    x: plot.left + (useTime ? (point.time - first) / span : index / (readings.length - 1)) * (plot.right - plot.left),
    y: plot.bottom - (point.kg - minimum) / (maximum - minimum) * (plot.bottom - plot.top),
  }))
  const ticks = [maximum, (minimum + maximum) / 2, minimum]
  const line = points.map(({ x, y }, index) => (index ? 'L' : 'M') + x + ' ' + y).join(' ')
  return { points, ticks, line, area: line + 'V' + plot.bottom + 'H' + plot.left + 'Z' }
}

function WeightChart({ records, recentId, activeId, onSelect }) {
  const chart = useMemo(() => createChart(records), [records])
  const first = chart.points[0]
  const last = chart.points.at(-1)
  const active = chart.points.find(point => point.id === activeId) ?? last

  return <div className="progress-chart-wrap">
    <div className="progress-chart" role="group" aria-label="Gráfico de evolución del peso">
      <svg viewBox={'0 0 ' + chartWidth + ' ' + chartHeight} aria-hidden="true">
        <text x="8" y="14">kg</text>
        {chart.ticks.map((tick, index) => {
          const y = plot.top + (plot.bottom - plot.top) * index / 2
          return <g key={tick}><path className="progress-chart-grid" d={'M' + plot.left + ' ' + y + 'H' + plot.right} />
            <text x={plot.left - 8} y={y + 4} textAnchor="end">{axisFormat.format(tick)}</text>
          </g>
        })}
        <path className="progress-chart-area" d={chart.area} />
        <path className="progress-chart-line" d={chart.line} />
        <path className="progress-chart-guide" d={'M' + active.x + ' ' + active.y + 'V' + plot.bottom} />
        <text x={first.x} y={dateLabelY} textAnchor="middle">{dateLabel(shortDate, first.time)}</text>
        {dateLabel(fullDate, first.time) !== dateLabel(fullDate, last.time) &&
          <text x={last.x} y={dateLabelY} textAnchor="middle">{dateLabel(shortDate, last.time)}</text>}
      </svg>
      {chart.points.map(point => <button key={point.id} type="button"
        className={'progress-chart-point' + (point.id === active.id ? ' is-active' : '') + (point.id === recentId ? ' is-new' : '')}
        style={{ left: (point.x * 100 / chartWidth) + '%', top: (point.y * 100 / chartHeight) + '%' }}
        aria-label={weightLabel(point.kg) + ' kg, ' + dateLabel(fullDate, point.time) + ', ' + dateLabel(clock, point.time)}
        aria-pressed={point.id === active.id} onMouseEnter={() => onSelect(point.id)}
        onFocus={() => onSelect(point.id)} onClick={() => onSelect(point.id)} />)}
      <div className="progress-chart-reading" aria-hidden="true"
        style={{ '--point-x': (active.x * 100 / chartWidth) + '%', '--point-y': (active.y * 100 / chartHeight) + '%' }}>
        <strong>{weightLabel(active.kg)} kg</strong><span>{dateLabel(shortDate, active.time)}</span>
      </div>
    </div>
  </div>
}

export default function ClientProgress({ onExpired, onNavigate }) {
  const dialog = useRef(null)
  const registerButton = useRef(null)
  const input = useRef(null)
  const [records, setRecords] = useState([])
  const [weight, setWeight] = useState('')
  const [loading, setLoading] = useState(true)
  const [retry, setRetry] = useState(0)
  const [saving, setSaving] = useState(false)
  const [historyError, setHistoryError] = useState('')
  const [formError, setFormError] = useState('')
  const [recentId, setRecentId] = useState(null)
  const [activeId, setActiveId] = useState(null)

  useEffect(() => {
    let current = true
    getProgressHistory().then(history => { if (current) setRecords(history) }).catch(err => {
      if (!current) return
      if (err.status === 401) onExpired()
      else setHistoryError(err.message || 'No se pudo consultar tu historial de peso.')
    }).finally(() => { if (current) setLoading(false) })
    return () => { current = false }
  }, [onExpired, retry])

  const latest = records[0]
  const initial = records.at(-1)
  const chronologicalRecords = useMemo(() => [...records].reverse(), [records])
  const hasTrend = records.length > 1
  const change = hasTrend ? Number((Number(latest.pesoKg) - Number(initial.pesoKg)).toFixed(2)) : 0
  const latestTime = parseTime(latest?.fechaRegistro)
  const canRegister = !loading && !historyError && Boolean(latest)

  function openDialog() {
    setWeight('')
    setFormError('')
    dialog.current?.showModal()
    input.current?.focus()
  }

  async function submit(event) {
    event.preventDefault()
    if (saving || !canRegister) return
    const parsedWeight = Number(weight)
    if (!Number.isFinite(parsedWeight) || parsedWeight < 0.01 || parsedWeight > 999.99) {
      setFormError('Ingresa un peso entre 0.01 y 999.99 kg.')
      return
    }
    setSaving(true)
    setFormError('')
    try {
      const record = await recordWeight(parsedWeight)
      setRecords(previous => [record, ...previous.filter(item => item.idRegistroFisico !== record.idRegistroFisico)])
      setRecentId(record.idRegistroFisico)
      setActiveId(record.idRegistroFisico)
      dialog.current?.close()
    } catch (err) {
      if (err.status === 401) onExpired()
      else setFormError(err.message || 'No se pudo registrar tu peso.')
    } finally { setSaving(false) }
  }

  return <section className="client-progress" aria-labelledby="client-page-title">
    {recentId !== null && <ProgressNotice key={recentId} />}
    {historyError ? <div className="progress-error" role="alert">
      <p>{historyError}</p><button type="button" className="progress-secondary" onClick={() => {
        setHistoryError('')
        setLoading(true)
        setRetry(value => value + 1)
      }}>Reintentar</button>
    </div> : <>
      <section className="progress-hero" aria-label="Resumen de peso">
        <div>
          <h2>{records.length === 1 ? 'Peso de partida' : 'Último peso registrado'}</h2>
          {loading ? <p className="progress-status" role="status">Cargando tu progreso…</p>
            : latest ? <>
              <p className="progress-weight">{weightLabel(latest.pesoKg)} <small>kg</small></p>
              <p className="progress-hero-date">Registrado el {dateLabel(fullDate, latestTime)}</p>
            </> : <p className="progress-status">Completa tu perfil para guardar tu primera medición.</p>}
        </div>
        {latest && <div className="progress-stats">
          {hasTrend && <p><strong>{change > 0 ? '+' : ''}{weightLabel(change)} kg</strong><span>desde la primera medición</span></p>}
          <p><strong>{records.length}</strong><span>{hasTrend ? 'mediciones' : 'medición'}</span></p>
        </div>}
        {canRegister && <button ref={registerButton} className="progress-primary" type="button" aria-haspopup="dialog" onClick={openDialog}>
          <Icon name="plus" />Registrar peso
        </button>}
        {!loading && !latest && <button className="progress-primary" type="button" onClick={() => onNavigate('perfil')}>Completar perfil</button>}
      </section>

      {!loading && <div className="progress-columns">
        <section className="progress-panel progress-trend" aria-labelledby="progress-trend-title">
          <header className="progress-panel-heading">
            <h2 id="progress-trend-title">Evolución del peso</h2>
            {hasTrend && <p>{dateLabel(fullDate, parseTime(initial.fechaRegistro))} – {dateLabel(fullDate, latestTime)}</p>}
          </header>
          {hasTrend ? <WeightChart records={chronologicalRecords} recentId={recentId} activeId={activeId} onSelect={setActiveId} />
            : <div className="progress-empty"><Icon name="activity" />
              <p>{latest ? 'Registra una segunda medición para ver tu evolución.' : 'Aquí podrás ver cómo cambia tu peso con el tiempo.'}</p></div>}
        </section>

        <section className="progress-panel progress-history" aria-labelledby="progress-history-title">
          <header className="progress-panel-heading"><h2 id="progress-history-title">Historial</h2>
            <p>{records.length} {records.length === 1 ? 'medición registrada' : 'mediciones registradas'}</p>
          </header>
          {latest ? <ol>{records.map((record, index) => {
            const time = parseTime(record.fechaRegistro)
            return <li key={record.idRegistroFisico} className={record.idRegistroFisico === recentId ? 'is-new' : undefined}>
              <span className="progress-history-icon"><Icon name="clock" /></span>
              <div className="progress-history-date"><strong>{dateLabel(fullDate, time)}</strong>
                <div>{Number.isFinite(time) && dateLabel(clock, time)}{index === 0 && <span className="progress-latest">Última</span>}</div></div>
              <strong className="progress-history-weight">{weightLabel(record.pesoKg)} <small>kg</small></strong>
            </li>
          })}</ol> : <p className="progress-history-empty">Tus mediciones aparecerán aquí.</p>}
        </section>
      </div>}
    </>}

    <dialog ref={dialog} className="progress-dialog" aria-labelledby="progress-dialog-title"
      aria-describedby="progress-dialog-description" onClose={() => registerButton.current?.focus()}
      onCancel={event => { if (saving) event.preventDefault() }}>
      <form onSubmit={submit}>
        <header className="progress-dialog-heading">
          <div><h2 id="progress-dialog-title">Registrar peso</h2><p id="progress-dialog-description">Agrega una medición a tu historial de hoy.</p></div>
          <button className="progress-close" type="button" aria-label="Cerrar registro de peso" disabled={saving}
            onClick={() => dialog.current.close()}><Icon name="close" /></button>
        </header>
        <div className="progress-dialog-body">
          <label htmlFor="progress-weight">Peso actual (kg)</label>
          <input ref={input} id="progress-weight" type="number" min="0.01" max="999.99" step="0.01" inputMode="decimal"
            placeholder="Ej. 68.5" autoComplete="off" value={weight} onChange={event => setWeight(event.target.value)} disabled={saving} required />
          {formError && <p className="progress-error" role="alert">{formError}</p>}
        </div>
        <footer className="progress-dialog-footer">
          <button className="progress-secondary" type="button" disabled={saving} onClick={() => dialog.current.close()}>Cancelar</button>
          <button className="progress-primary" type="submit" disabled={saving || !weight}>
            <Icon name="check" />{saving ? 'Guardando…' : 'Guardar peso'}</button>
        </footer>
      </form>
    </dialog>
  </section>
}

function ProgressNotice() {
  const [visible, setVisible] = useState(true)
  return visible && <ClientToast title="Peso registrado" message="Tu peso se agregó al historial." onClose={() => setVisible(false)} />
}
