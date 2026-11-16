import Icon from '../Icon'

export default function ProductoCard({ producto, onAgregar }) {
  const agotado = producto.stock === 0

  return <article className="practice-product-card">
    <div className="practice-product-card-top">
      <span className="practice-product-icon"><Icon name="food" /></span>
      <span className={`practice-product-stock ${agotado ? 'is-empty' : ''}`}>
        {agotado ? 'Agotado' : `${producto.stock} disponibles`}
      </span>
    </div>
    <h3>{producto.nombre}</h3>
    <div className="practice-product-card-bottom">
      <strong>S/ {Number(producto.precio).toFixed(2)}</strong>
      <button type="button" onClick={() => onAgregar(producto)} disabled={agotado}>
        <Icon name="plus" /> Agregar
      </button>
    </div>
  </article>
}
