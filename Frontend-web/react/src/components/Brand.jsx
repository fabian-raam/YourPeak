/** El símbolo es un recurso; el nombre conserva texto accesible y seleccionable. */
export default function Brand({ onNavigate, href = '/login' }) {
  return <a className="brand" href={href} onClick={onNavigate} aria-label="Your Peak, inicio">
    <img src="/assets/logo.svg" alt="" width="37" height="62" />
    <span>YOUR PEAK</span>
  </a>
}
