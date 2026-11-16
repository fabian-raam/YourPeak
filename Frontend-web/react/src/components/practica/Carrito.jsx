import { useEffect, useMemo, useRef } from 'react'
import Icon from '../Icon'

export default function Carrito({ lineas, onQuitar, onVaciar }) {
  const tituloOriginal = useRef(document.title)
  const cantidadUnidades = useMemo(() => lineas.reduce(
    (total, linea) => total + linea.cantidad, 0,
  ), [lineas])
  const total = useMemo(() => lineas.reduce(
    (suma, linea) => suma + Number(linea.precio) * linea.cantidad, 0,
  ), [lineas])

  useEffect(() => {
    console.log(`Carrito actualizado: ${cantidadUnidades} unidades`)
    document.title = `Carrito (${cantidadUnidades}) · Cafetería`
    return () => { document.title = tituloOriginal.current }
  }, [cantidadUnidades])

  return <aside className="practice-cart" aria-label="Carrito de compras">
    <div className="practice-cart-heading">
      <span className="practice-cart-icon"><Icon name="card" /></span>
      <div><h2>Carrito</h2><span>{cantidadUnidades} {cantidadUnidades === 1 ? 'unidad' : 'unidades'}</span></div>
    </div>

    {lineas.length === 0 && <p className="practice-cart-empty">El carrito está vacío.</p>}
    {lineas.length > 0 && <>
      <ul className="practice-cart-lines">
        {lineas.map(linea => <li key={linea.id}>
          <div><strong>{linea.nombre}</strong><span>{linea.cantidad} × S/ {Number(linea.precio).toFixed(2)}</span></div>
          <div className="practice-cart-line-total">
            <strong>S/ {(Number(linea.precio) * linea.cantidad).toFixed(2)}</strong>
            <button type="button" onClick={() => onQuitar(linea.id)} aria-label={`Quitar ${linea.nombre}`}>
              <Icon name="close" />
            </button>
          </div>
        </li>)}
      </ul>
      <div className="practice-cart-total"><span>Total</span><strong>S/ {total.toFixed(2)}</strong></div>
      <button className="practice-cart-clear" type="button" onClick={onVaciar}>Vaciar carrito</button>
    </>}
  </aside>
}
