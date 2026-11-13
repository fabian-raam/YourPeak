import { useEffect, useMemo, useState } from 'react'
import BuscadorPlanes from '../components/practica/BuscadorPlanes'
import FormularioPlan from '../components/practica/FormularioPlan'
import ListaPlanes from '../components/practica/ListaPlanes'
import ResumenPlanes from '../components/practica/ResumenPlanes'
import { planesIniciales } from '../practica/datosPlanes'
import './PlanesPractica.css'

export default function PlanesPractica() {
  const [planes, setPlanes] = useState(planesIniciales)
  const [busqueda, setBusqueda] = useState('')
  const [aviso, setAviso] = useState('')

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
    <header className="admin-heading"><div><span className="practice-kicker">PANTALLA DE PRÁCTICA</span>
      <h1>Catálogo de planes</h1><p>Ejemplo de componentes React y comunicación mediante props.</p></div></header>
    {aviso && <p className="practice-plan-notice" role="status">{aviso}</p>}
    <ResumenPlanes planes={planes} />
    <section className="practice-plan-panel" aria-label="Gestión de planes de ejemplo">
      <div className="practice-plan-toolbar"><BuscadorPlanes valor={busqueda} onCambiar={setBusqueda} />
        <span>{planesVisibles.length} de {planes.length} planes</span></div>
      <ListaPlanes planes={planesVisibles} onCambiarEstado={cambiarEstado} onEliminar={eliminarPlan} />
      <FormularioPlan onGuardar={guardarPlan} />
    </section>
    <p className="practice-plan-explanation"><strong>Flujo:</strong> esta pantalla guarda los planes. Pasa la lista a resumen y listado; cada fila avisa al padre para cambiar estado o eliminar; el formulario le avisa al padre para agregar.
          Los datos son de ejemplo y no se envían al servidor.</p>
  </main>
}
