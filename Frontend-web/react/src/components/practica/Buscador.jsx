import Icon from '../Icon'

export default function Buscador({ valor, onCambiar }) {
  return <label className="practice-cafe-search">
    <Icon name="search" />
    <input
      type="search"
      value={valor}
      onChange={event => onCambiar(event.target.value)}
      placeholder="Buscar un producto"
      aria-label="Buscar un producto"
    />
  </label>
}
