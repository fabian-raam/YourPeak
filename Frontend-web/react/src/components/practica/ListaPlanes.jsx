import FilaPlan from './FilaPlan'

export default function ListaPlanes({ planes, onCambiarEstado, onEliminar }) {
  if (!planes.length) return <p className="practice-plan-empty">No hay planes que coincidan con la búsqueda.</p>
  return <div className="practice-plan-table-scroll"><table className="practice-plan-table">
    <thead><tr><th>Plan</th><th>Duración</th><th>Estado</th><th>Acciones</th></tr></thead>
    <tbody>{planes.map(plan => <FilaPlan key={plan.idPlanMembresia} plan={plan}
      onCambiarEstado={onCambiarEstado} onEliminar={onEliminar} />)}</tbody>
  </table></div>
}
