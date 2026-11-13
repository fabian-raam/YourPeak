import Icon from '../Icon'

export default function BuscadorPlanes({ valor, onCambiar }) {
  return <label className="practice-plan-search"><Icon name="search" />
    <input type="search" value={valor} onChange={event => onCambiar(event.target.value)} placeholder="Buscar un plan por nombre" />
  </label>
}
