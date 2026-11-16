import { useCallback, useMemo, useState } from 'react'
import Aviso from '../components/practica/Aviso'
import Buscador from '../components/practica/Buscador'
import Carrito from '../components/practica/Carrito'
import Catalogo from '../components/practica/Catalogo'
import { productosIniciales } from './datos'
import './Practica.css'

const propsDocumentadas = [
  ['productos', 'arreglo de objetos', 'Practica', 'Catalogo'],
  ['onAgregar', 'función', 'Practica', 'Catalogo y ProductoCard'],
  ['producto', 'objeto', 'Catalogo', 'ProductoCard'],
  ['lineas', 'arreglo de objetos', 'Practica', 'Carrito'],
  ['onQuitar', 'función', 'Practica', 'Carrito'],
  ['onVaciar', 'función', 'Practica', 'Carrito'],
  ['valor', 'texto', 'Practica', 'Buscador'],
  ['onCambiar', 'función', 'Practica', 'Buscador'],
  ['aviso', 'objeto o null', 'Practica', 'Aviso'],
  ['onCerrar', 'función', 'Practica', 'Aviso'],
]

export default function Practica() {
  const [carrito, setCarrito] = useState([])
  const [busqueda, setBusqueda] = useState('')
  const [aviso, setAviso] = useState(null)
  const cerrarAviso = useCallback(() => setAviso(null), [])

  const productosVisibles = useMemo(() => {
    const texto = busqueda.trim().toLocaleLowerCase('es-PE')
    return productosIniciales.filter(producto =>
      producto.nombre.toLocaleLowerCase('es-PE').includes(texto))
  }, [busqueda])

  function agregarAlCarrito(producto) {
    console.log(`Clic en Agregar: ${producto.nombre}`)
    setCarrito(actual => {
      const yaAgregado = actual.some(linea => linea.id === producto.id)
      if (yaAgregado) {
        return actual.map(linea => linea.id === producto.id
          ? { ...linea, cantidad: linea.cantidad + 1 }
          : linea)
      }

      return [...actual, {
        id: producto.id,
        nombre: producto.nombre,
        precio: producto.precio,
        cantidad: 1,
      }]
    })
    setAviso({ id: `${Date.now()}-${Math.random()}`, nombre: producto.nombre })
  }

  function quitarDelCarrito(id) {
    setCarrito(actual => actual.filter(linea => linea.id !== id))
  }

  return <section className="practice-cafe">
    <header className="practice-cafe-heading">
      <div><span className="practice-kicker">PARTE 1 · EJERCICIOS 1–8</span>
        <h2>Mini catálogo con carrito</h2>
        <p>Practica cómo los datos bajan por props y los eventos avisan al componente padre.</p>
      </div>
      <span className="practice-data-note">Datos de ejemplo · no se envían al servidor</span>
    </header>

    <Aviso aviso={aviso} onCerrar={cerrarAviso} />

    <div className="practice-cafe-toolbar">
      <Buscador valor={busqueda} onCambiar={setBusqueda} />
      <span>{productosVisibles.length} de {productosIniciales.length} productos</span>
    </div>

    <div className="practice-cafe-layout">
      <section className="practice-catalog" aria-label="Catálogo de cafetería">
        <div className="practice-section-heading"><h3>Catálogo</h3><span>El precio llega como texto</span></div>
        <Catalogo productos={productosVisibles} onAgregar={agregarAlCarrito} />
      </section>
      <Carrito lineas={carrito} onQuitar={quitarDelCarrito} onVaciar={() => setCarrito([])} />
    </div>

    <details className="practice-learning-notes">
      <summary>Ejercicios 2, 3, 4, 5, 6, 7 y 8 · props, eventos y estado elevado</summary>
      <div className="practice-notes-content">
        <h3>Props que se pasan entre componentes</h3>
        <div className="practice-props-table-wrap"><table className="practice-props-table">
          <thead><tr><th>Prop</th><th>Tipo</th><th>La define</th><th>La lee</th></tr></thead>
          <tbody>{propsDocumentadas.map(([nombre, tipo, origen, destino]) => <tr key={nombre}>
            <td><code>{nombre}</code></td><td>{tipo}</td><td>{origen}</td><td>{destino}</td>
          </tr>)}</tbody>
        </table></div>
        <p><strong>Por qué ProductoCard no cambia producto:</strong> las props son datos que recibe para mostrar. Si necesita cambiar el carrito, llama a <code>onAgregar(producto)</code>; la función del padre actualiza el estado. En Django se parece al <code>context</code> de una vista que entrega valores a un template para representarlos.</p>

        <h3>Evento correcto y error que evita</h3>
        <pre><code>{'// Correcto: el clic llama a la función después.\n<button onClick={() => onAgregar(producto)}>Agregar</button>\n\n// Incorrecto: ejecuta la función durante el render.\n<button onClick={onAgregar(producto)}>Agregar</button>'}</code></pre>
        <p>Al pulsar “Agregar”, abre la consola del navegador para ver el nombre registrado. El aviso desaparece a los 3 segundos y limpia su temporizador cuando cambia o se desmonta.</p>

        <h3>Quién guarda y comparte cada dato</h3>
        <p><code>Practica</code> guarda el carrito y el texto de búsqueda. <code>Catalogo</code> y <code>Carrito</code> son hermanos: ambos reciben sus datos desde el padre. El total y las unidades se calculan con <code>reduce()</code>; no usan otro estado porque se derivan de las líneas. Agregar usa <code>map()</code> y spread; quitar usa <code>filter()</code>; vaciar crea un arreglo vacío. No se muta el estado con <code>push</code> ni <code>splice</code>.</p>

        <h3>useEffect en Carrito y Aviso</h3>
        <ul>
          <li><code>[]</code>: el efecto se ejecuta al montar y su limpieza al desmontar.</li>
          <li><code>[cantidadUnidades]</code>: se ejecuta al montar y cuando cambia la cantidad de unidades.</li>
          <li>Sin arreglo de dependencias: se ejecuta después de cada render.</li>
          <li>En desarrollo, <code>StrictMode</code> vuelve a ejecutar algunos efectos para detectar limpiezas faltantes; por eso un mensaje de consola puede aparecer dos veces.</li>
        </ul>
      </div>
    </details>
  </section>
}
