import ProductoCard from './ProductoCard'

export default function Catalogo({ productos, onAgregar }) {
  if (productos.length === 0) {
    return <p className="practice-cafe-empty">No hay productos que coincidan.</p>
  }

  return <div className="practice-product-grid">
    {productos.map(producto => <ProductoCard
      key={producto.id}
      producto={producto}
      onAgregar={onAgregar}
    />)}
  </div>
}
