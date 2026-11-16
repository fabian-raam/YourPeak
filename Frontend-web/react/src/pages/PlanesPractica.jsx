import { useEffect, useMemo, useState } from 'react'
import BuscadorPlanes from '../components/practica/BuscadorPlanes'
import FormularioPlan from '../components/practica/FormularioPlan'
import ListaPlanes from '../components/practica/ListaPlanes'
import ResumenPlanes from '../components/practica/ResumenPlanes'
import { planesIniciales } from '../practica/datosPlanes'
import Practica from '../practica/Practica'
import './PlanesPractica.css'

export default function PlanesPractica() {
  const [planes, setPlanes] = useState(planesIniciales)
  const [busqueda, setBusqueda] = useState('')
  const [aviso, setAviso] = useState('')
  const [parteActiva, setParteActiva] = useState(1)

  useEffect(() => {
    const tituloAnterior = document.title
    document.title = 'Práctica React · Planes'
    return () => { document.title = tituloAnterior }
  }, [])

  useEffect(() => {
    if (!aviso) return undefined
    const temporizador = window.setTimeout(() => setAviso(''), 3000)
    return () => window.clearTimeout(temporizador)
  }, [aviso])

  const planesVisibles = useMemo(() => planes.filter(plan =>
    plan.nombre.toLocaleLowerCase('es-PE').includes(busqueda.trim().toLocaleLowerCase('es-PE'))), [planes, busqueda])

  function guardarPlan(datos) {
    const duplicado = planes.some(plan => plan.nombre.toLocaleLowerCase('es-PE') === datos.nombre.toLocaleLowerCase('es-PE'))
    if (duplicado) return 'Ya existe un plan con ese nombre.'
    const nuevoId = Math.max(0, ...planes.map(plan => plan.idPlanMembresia)) + 1
    setPlanes(actuales => [...actuales, { idPlanMembresia: nuevoId, ...datos, estado: 'ACTIVO' }])
    setAviso(`Se agregó «${datos.nombre}».`)
    return ''
  }

  function cambiarEstado(id) {
    setPlanes(actuales => actuales.map(plan => plan.idPlanMembresia === id
      ? { ...plan, estado: plan.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO' } : plan))
    setAviso('Se actualizó el estado del plan.')
  }

  function eliminarPlan(id) {
    setPlanes(actuales => actuales.filter(plan => plan.idPlanMembresia !== id))
    setAviso('Se eliminó el plan.')
  }

  return <main className="admin-main practice-plans-page">
    <header className="admin-heading"><div><span className="practice-kicker">PRÁCTICA REACT</span>
      <h1>Comunicación entre componentes</h1><p>Props, estado, eventos y efectos en dos ejercicios guiados.</p></div></header>
    <nav className="practice-exercise-tabs" aria-label="Ejercicios de React">
      <button type="button" aria-pressed={parteActiva === 1} onClick={() => setParteActiva(1)}>
        Parte 1 · Catálogo y carrito
      </button>
      <button type="button" aria-pressed={parteActiva === 2} onClick={() => setParteActiva(2)}>
        Parte 2 · Panel de planes
      </button>
    </nav>

    {parteActiva === 1 ? <Practica /> : <>
      {aviso && <p className="practice-plan-notice" role="status">{aviso}</p>}
      <ResumenPlanes planes={planes} />
      <section className="practice-plan-panel" aria-label="Gestión de planes de ejemplo">
        <div className="practice-plan-toolbar"><BuscadorPlanes valor={busqueda} onCambiar={setBusqueda} />
          <span>{planesVisibles.length} de {planes.length} planes</span></div>
        <ListaPlanes planes={planesVisibles} onCambiarEstado={cambiarEstado} onEliminar={eliminarPlan} />
        <FormularioPlan onGuardar={guardarPlan} />
      </section>
      <p className="practice-plan-explanation"><strong>Flujo:</strong> esta pantalla guarda los planes. Pasa la lista a resumen y listado; cada fila avisa al padre para cambiar estado o eliminar; el formulario le avisa al padre para agregar. Los datos son de ejemplo y no se envían al servidor.</p>
      <details className="practice-plan-explanation practice-component-map">
        <summary>Ejercicio 9 · Mapa de componentes y comunicación</summary>
        <p><strong>Entidad principal:</strong> planes de membresía. <code>PlanesPractica</code> es el padre y conserva la lista, la búsqueda y el aviso. Envía datos hacia los hijos mediante props; los hijos notifican acciones de vuelta mediante funciones callback.</p>
        <pre><code>{'PlanesPractica (padre: estado y lógica)\n├── BuscadorPlanes (texto + onCambiar)\n├── ResumenPlanes (planes → conteos derivados)\n├── ListaPlanes (planes filtrados + callbacks)\n│   └── FilaPlan (un plan + cambiar estado / eliminar)\n└── FormularioPlan (onGuardar → valida y avisa al padre)'}</code></pre>
        <p>El resumen depende de <code>planes</code>, y el listado depende de <code>planes</code> y <code>busqueda</code>. Como ambos necesitan la misma información, el estado vive en el padre. El formulario no agrega directamente: llama <code>onGuardar(datos)</code>, y el padre actualiza el estado.</p>
      </details>
    </>}
  </main>
}
