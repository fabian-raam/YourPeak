import Icon from '../Icon'

export default function FilaPlan({ plan, onCambiarEstado, onEliminar }) {
  const activo = plan.estado === 'ACTIVO'
  return <tr>
    <td><strong>{plan.nombre}</strong><small>Plan #{plan.idPlanMembresia}</small></td>
    <td>{plan.duracionMeses} {plan.duracionMeses === 1 ? 'mes' : 'meses'}</td>
    <td><span className={`practice-plan-status ${activo ? 'active' : 'inactive'}`}>{activo ? 'Activo' : 'Inactivo'}</span></td>
    <td className="practice-plan-row-actions">
      <button type="button" onClick={() => onCambiarEstado(plan.idPlanMembresia)}>{activo ? 'Desactivar' : 'Activar'}</button>
      <button type="button" className="delete" onClick={() => onEliminar(plan.idPlanMembresia)} aria-label={`Eliminar ${plan.nombre}`}><Icon name="close" /></button>
    </td>
  </tr>
}
