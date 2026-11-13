export default function ResumenPlanes({ planes }) {
  const activos = planes.filter(plan => plan.estado === 'ACTIVO').length
  const inactivos = planes.length - activos

  return <section className="practice-plan-summary" aria-label="Resumen de planes">
    <article><span>Total de planes</span><strong>{planes.length}</strong></article>
    <article><span>Planes activos</span><strong>{activos}</strong></article>
    <article><span>Planes inactivos</span><strong>{inactivos}</strong></article>
  </section>
}
