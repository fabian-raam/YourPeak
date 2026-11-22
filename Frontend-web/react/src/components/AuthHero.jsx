import Brand from './Brand'

/** Solo el atleta es una fotografía: la composición se adapta con CSS. */
export default function AuthHero({ onNavigate }) {
  return <section className="brand-side" aria-label="Entrena con Your Peak">
    <Brand href="/" onNavigate={onNavigate} />
    <div className="hero-copy">
      <p className="hero-kicker">CADA DÍA, UN PASO MÁS</p>
      <h2>Encuentra<br className="hero-break-desktop" /> tu<br className="hero-break-mobile" /> <strong>mejor</strong><br /> versión.</h2>
      <p>Entrena con propósito.<br />Supera tus límites.</p>
    </div>
    <div className="hero-shape" aria-hidden="true" />
    <img className="hero-athlete" src="/assets/athlete.png" alt="Atleta de Your Peak" width="408" height="612" fetchPriority="high" />
  </section>
}
