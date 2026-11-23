import { useState } from 'react'
import Icon from '../components/Icon'
import useClientSummary from '../hooks/useClientSummary'
import { getClientMemberships } from '../services/clientMembership'
import { membershipDate } from '../utils/format'
import './ClientMembership.css'

const stateLabels = { VIGENTE: 'Vigente', PROGRAMADA: 'Programada', VENCIDA: 'Finalizada' }
const limaDate = new Intl.DateTimeFormat('sv-SE', { timeZone: 'America/Lima' })
const dayMs = 86_400_000
const dateValue = value => Date.parse(value + 'T00:00:00Z')
const dateLabel = (value, month) => Number.isFinite(dateValue(value)) ? membershipDate(value, month) : 'Sin fecha'

function MembershipStatus({ state }) {
  return <span className="client-membership-badge" data-state={state}>
    {state === 'VIGENTE' && <Icon name="check" />}
    {stateLabels[state] ?? state}
  </span>
}

function CurrentPlan({ period, isCurrent }) {
  const [currentDay] = useState(() => dateValue(limaDate.format(new Date())))
  const start = dateValue(period?.fechaInicio)
  const end = dateValue(period?.fechaVencimiento)
  const hasValidDates = Number.isFinite(start) && Number.isFinite(end) && end >= start
  const remainingDays = (end - currentDay) / dayMs
  const elapsed = Math.min(1, Math.max(0, (currentDay - start) / (end - start + dayMs)))
  const duration = period?.duracionMeses

  return <section className="client-membership-hero" aria-labelledby="client-membership-heading">
    <span className="client-membership-credential"><Icon name="card" /></span>
    <div className="client-membership-plan">
      <p className="client-membership-eyebrow">Tu plan</p>
      <h2 id="client-membership-heading">{period?.nombrePlan ?? (isCurrent ? 'Mi plan' : 'Sin plan vigente')}</h2>
      <div className="client-membership-meta">
        <MembershipStatus state={isCurrent ? 'VIGENTE' : 'No vigente'} />
        {duration > 0 && <span>{duration} {duration === 1 ? 'mes' : 'meses'}</span>}
      </div>
    </div>
    {period && <div className="client-membership-validity">
      <p className="client-membership-eyebrow">Vencimiento</p>
      <strong>{dateLabel(period.fechaVencimiento, 'long')}</strong>
      {isCurrent && hasValidDates && remainingDays >= 0 &&
        <div className="client-membership-track" aria-hidden="true"><span style={{ width: elapsed * 100 + '%' }} /></div>}
      <div className="client-membership-dates">
        <span>Desde el {dateLabel(period.fechaInicio)}</span>
        {isCurrent && hasValidDates && remainingDays >= 0 && <b>
          {remainingDays === 0 ? 'Vence hoy' : remainingDays === 1 ? '1 día para vencer' : remainingDays + ' días para vencer'}
        </b>}
      </div>
    </div>}
  </section>
}

export default function ClientMembership({ user, onExpired }) {
  const history = useClientSummary(getClientMemberships, onExpired)
  const periods = history.data
  const period = periods ? periods.find(item => item.estadoPeriodo === 'VIGENTE') : user.membresia
  const isCurrent = Boolean(period || (!periods && user.membresiaVigente))
  // El historial llega del período más reciente al más antiguo.
  const nextPeriod = periods?.findLast(item => item.estadoPeriodo === 'PROGRAMADA')

  return <div className="client-membership-page">
    <CurrentPlan period={period} isCurrent={isCurrent} />

    {nextPeriod && <section className="client-membership-next" aria-labelledby="client-membership-next">
      <span className="client-membership-calendar"><Icon name="clock" /></span>
      <div>
        <h2 id="client-membership-next">Próxima membresía</h2>
        <p><strong>{nextPeriod.nombrePlan}</strong> · Inicia el {dateLabel(nextPeriod.fechaInicio, 'long')}</p>
      </div>
      <MembershipStatus state="PROGRAMADA" />
    </section>}

    <section className="client-card client-membership-history" aria-labelledby="client-membership-history">
      <header>
        <h2 id="client-membership-history">Historial de membresías</h2>
        {periods?.length > 0 && <p>{periods.length} {periods.length === 1 ? 'período registrado' : 'períodos registrados'}</p>}
      </header>
      {history.error ? <div className="client-membership-message">
        <p role="status">No se pudo cargar tu historial de membresías.</p>
        <button className="client-text-link" type="button" onClick={history.retry}>Reintentar</button>
      </div> : !periods ? <p className="client-membership-message" role="status">Cargando tus membresías.</p>
        : periods.length === 0 ? <p className="client-membership-message">Aún no tienes períodos registrados.</p>
          : <table className="client-membership-table" role="table" aria-labelledby="client-membership-history">
            <thead role="rowgroup"><tr role="row">
              <th scope="col" role="columnheader">Plan</th>
              <th scope="col" role="columnheader">Inicio</th>
              <th scope="col" role="columnheader">Vencimiento</th>
              <th scope="col" role="columnheader">Estado</th>
            </tr></thead>
            <tbody role="rowgroup">{periods.map(item => <tr role="row" key={item.idMembresia}>
              <th scope="row" role="rowheader">{item.nombrePlan}</th>
              <td role="cell" className="client-membership-start">{dateLabel(item.fechaInicio)}</td>
              <td role="cell" className="client-membership-end">{dateLabel(item.fechaVencimiento)}</td>
              <td role="cell" className="client-membership-state"><MembershipStatus state={item.estadoPeriodo} /></td>
            </tr>)}</tbody>
          </table>}
    </section>
  </div>
}
