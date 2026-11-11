import { useEffect, useRef } from 'react'
import Brand from '../components/Brand'
import Icon from '../components/Icon'
import './HomePage.css'

const photos = '/assets/home/'

export default function HomePage({ onNavigate }) {
  const page = useRef(null)

  useEffect(() => {
    const elements = page.current.querySelectorAll('[data-home-reveal]')
    if (!('IntersectionObserver' in window) || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
    const observer = new IntersectionObserver(entries => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.remove('home-awaiting')
          observer.unobserve(entry.target)
        }
      })
    }, { threshold: 0.12 })
    elements.forEach(element => { element.classList.add('home-awaiting'); observer.observe(element) })
    return () => { observer.disconnect(); elements.forEach(element => element.classList.remove('home-awaiting')) }
  }, [])

  const go = next => event => {
    if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return
    event.preventDefault()
    onNavigate(next)
  }

  return <div className="home-page" ref={page}>
    <a className="home-skip" href="#home-content">Saltar al contenido</a>
    <header className="home-header">
      <nav className="home-nav" aria-label="Navegación principal">
        <Brand href="/" onNavigate={go('inicio')} />
        <a className="home-nav-current" href="/" aria-current="page" onClick={go('inicio')}>Inicio</a>
        <div className="home-nav-access">
          <a className="home-login" href="/login" onClick={go('login')}>Iniciar sesión</a>
          <a className="home-button home-nav-register" href="/registro" onClick={go('registro')}>Registrarse</a>
        </div>
      </nav>
    </header>
    <main id="home-content" tabIndex="-1">
      <section className="home-hero" aria-labelledby="home-title">
        <div className="home-hero-copy">
          <p className="home-kicker">COMUNIDAD TECSUP</p>
          <h1 id="home-title">TU SIGUIENTE<br />NIVEL EMPIEZA<br /><span className="home-title-ending"><em>CONTIGO.</em><i aria-hidden="true" /></span></h1>
          <p className="home-intro">Un espacio para entrenar, superarte y formar parte de algo más.</p>
          <div className="home-hero-actions">
            <a className="home-button" href="/registro" onClick={go('registro')}>Quiero ser parte <Icon name="arrow" /></a>
            <a className="home-existing" href="/login" onClick={go('login')}>Ya tengo una cuenta</a>
          </div>
        </div>
        <div className="home-hero-photo">
          <img src={`${photos}athlete-silhouette.png`} alt="Silueta de un atleta iluminado sobre un fondo negro" width="612" height="408" fetchPriority="high" />
        </div>
      </section>
      <section className="home-space" aria-labelledby="home-space-title">
        <div className="home-space-heading" data-home-reveal>
          <p className="home-section-label">01 / TU ESPACIO<span aria-hidden="true" /></p>
          <h2 id="home-space-title">Más que venir a entrenar.</h2>
        </div>
        <div className="home-gallery">
          <figure data-home-reveal>
            <div className="home-photo-frame"><img src={`${photos}gym-weights.png`} alt="Mancuernas sobre el suelo de un gimnasio con iluminación tenue" width="960" height="540" loading="lazy" decoding="async" /></div>
            <figcaption><span>UN ESPACIO PARA ENFOCARTE</span><p>Haz una pausa en tu día. Dedica un momento a ti.</p></figcaption>
          </figure>
          <figure data-home-reveal>
            <div className="home-photo-frame"><img src={`${photos}gym-interior.png`} alt="Vista del gimnasio con bancos, máquinas y zona de pesas" width="640" height="359" loading="lazy" decoding="async" /></div>
            <figcaption><span>CADA DÍA, UN PASO MÁS</span><p>Empieza a tu ritmo y construye tu propia constancia.</p></figcaption>
          </figure>
        </div>
      </section>
    </main>
    <footer className="home-footer"><span>YOUR PEAK</span><p>Hecho para la comunidad TECSUP.</p><a href="/registro" onClick={go('registro')}>Sé parte <Icon name="arrow" /></a></footer>
  </div>
}
