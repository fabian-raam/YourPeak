import Icon from '../components/Icon'
import useClientSummary from '../hooks/useClientSummary'
import { getClientMemberships } from '../services/clientMembership'
import { membershipDate } from '../utils/format'
import './ClientMembership.css'

const stateLabels = { VIGENTE: 'Vigente', PROGRAMADA: 'Programada', VENCIDA: 'Finalizada' }

export default function ClientMembership({ user, onExpired }) {
  const history = useClientSummary(getClientMemberships, onExpired)
  const period = history.data
    ? history.data.find(item => item.estadoPeriodo === 'VIGENTE')
    : user.membresia

  return <div className="client-membership-page">
    <section className="client-card" aria-labelledby="client-membership-heading">
      <div className="client-card-heading">
        <span className="client-card-icon"><Icon name="card" /></span>
        <div>
          <p className="client-membership-eyebrow">Plan vigente</p>
          <h2 id="client-membership-heading">{period?.nombrePlan ?? 'Mi plan'}</h2>
          {period && <p>{period.duracionMeses} {period.duracionMeses === 1 ? 'mes' : 'meses'}</p>}
        </div>
      </div>
      <dl className="client-facts client-membership-facts">
        <div><dt>Estado</dt><dd><span className="client-membership-status">
          {period || (!history.data && user.membresiaVigente) ? 'Vigente' : 'No vigente'}
        </span></dd></div>
        {period && <>
          <div><dt>Fecha de inicio</dt><dd>{membershipDate(period.fechaInicio, 'long')}</dd></div>
          <div><dt>Vencimiento</dt><dd>{membershipDate(period.fechaVencimiento, 'long')}</dd></div>
        </>}
      </dl>
    </section>

    <section className="client-card" aria-labelledby="client-membership-history">
      <h2 id="client-membership-history">Historial de membresías</h2>
      {history.error ? <>
        <p role="status">No se pudo cargar tu historial de membresías.</p>
        <button className="client-text-link" type="button" onClick={history.retry}>Reintentar</button>
      </> : !history.data ? <p role="status">Cargando tus membresías…</p>
        : history.data.length === 0 ? <p>Aún no tienes períodos registrados.</p>
          : <div className="client-membership-table" tabIndex={0} role="region" aria-label="Períodos de membresía">
            <table>
              <thead><tr><th scope="col">Plan</th><th scope="col">Inicio</th><th scope="col">Vencimiento</th><th scope="col">Estado</th></tr></thead>
              <tbody>{history.data.map(item => <tr key={item.idMembresia}>
                <th scope="row">{item.nombrePlan}</th>
                <td>{membershipDate(item.fechaInicio)}</td>
                <td>{membershipDate(item.fechaVencimiento)}</td>
                <td><span className={`client-membership-status${item.estadoPeriodo === 'VIGENTE' ? '' : ' is-neutral'}`}>
                  {stateLabels[item.estadoPeriodo] ?? item.estadoPeriodo}
                </span></td>
              </tr>)}</tbody>
            </table>
          </div>}
    </section>
  </div>
}
