import { useEffect, useRef, useState } from 'react'
import Brand from '../components/Brand'
import Icon from '../components/Icon'
import './HomePage.css'

const photos = '/assets/home/'
const sections = [['inicio', 'Inicio'], ['nosotros', 'Nosotros'], ['como-funciona', 'Cómo funciona']]
const features = [
  ['dumbbell', 'Planifica tus rutinas', 'Organiza tu semana de entrenamiento.'],
  ['food', 'Registra tu nutrición', 'Lleva el registro de lo que comes.'],
  ['activity', 'Sigue tu progreso', 'Consulta tus mediciones y avances.'],
]
const steps = [
  ['Crea tu cuenta', 'Regístrate con tu correo institucional para solicitar acceso.'],
  ['Espera la aprobación', 'Cuando aprueben tu solicitud, podrás iniciar sesión.'],
  ['Organiza tu entrenamiento', 'Entra a tu panel y empieza a registrar tu actividad a tu ritmo.'],
]

export default function HomePage({ onNavigate }) {
  const page = useRef(null)
  const [activeSection, setActiveSection] = useState(() => location.hash.slice(1) || 'inicio')

  useEffect(() => {
    const header = page.current.querySelector('.home-header')
    const targets = sections.map(([id]) => document.getElementById(id))
    function updateSection() {
      const offset = header.getBoundingClientRect().height + 24
      const current = targets.findLast(target => target.getBoundingClientRect().top <= offset)
      setActiveSection(current?.id ?? 'inicio')
    }
    const initialTarget = targets.find(target => `#${target.id}` === location.hash)
    if (initialTarget) initialTarget.scrollIntoView({ behavior: 'instant' })
    updateSection()
    window.addEventListener('scroll', updateSection, { passive: true })
    window.addEventListener('resize', updateSection)
    return () => {
      window.removeEventListener('scroll', updateSection)
      window.removeEventListener('resize', updateSection)
    }
  }, [])

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
        <Brand href="#inicio" />
        <div className="home-nav-sections">
          {sections.map(([id, label]) => <a key={id} href={`#${id}`} aria-current={activeSection === id ? 'location' : undefined}>{label}</a>)}
        </div>
        <div className="home-nav-access">
          <a className="home-login" href="/login" onClick={go('login')}>Iniciar sesión</a>
          <a className="home-button home-nav-register" href="/registro" onClick={go('registro')}>Registrarse</a>
        </div>
      </nav>
    </header>
    <main id="home-content" tabIndex="-1">
      <section id="inicio" className="home-hero" aria-labelledby="home-title">
        <div className="home-hero-copy">
          <p className="home-kicker">Proyecto para la comunidad <img src={`${photos}tecsup.png`} alt="Tecsup" width="84" height="24" /></p>
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
      <ul className="home-features home-container" aria-label="Funciones de Your Peak">
        {features.map(([icon, title, description]) => <li key={title}><Icon name={icon} /><div><strong>{title}</strong><p>{description}</p></div></li>)}
      </ul>
      <section id="nosotros" className="home-space home-container" aria-labelledby="home-space-title">
        <div className="home-space-heading" data-home-reveal>
          <div><p className="home-section-label">Nosotros<span aria-hidden="true" /></p>
            <h2 id="home-space-title">Más que venir a entrenar.</h2></div>
          <p>Your Peak es un espacio para la comunidad Tecsup donde puedes organizar tu entrenamiento, registrar tu nutrición y seguir tu progreso en un solo lugar.</p>
        </div>
        <div className="home-gallery">
          <figure data-home-reveal>
            <div className="home-photo-frame"><img src={`${photos}dumbbells-editorial.jpg`} alt="Mancuernas sobre el suelo de un gimnasio" width="960" height="540" loading="lazy" decoding="async" /></div>
            <figcaption><span>UN ESPACIO PARA ENFOCARTE</span><p>Haz una pausa en tu día. Dedica un momento a ti.</p></figcaption>
          </figure>
          <figure data-home-reveal>
            <div className="home-photo-frame"><img src={`${photos}gym-interior.png`} alt="Vista del gimnasio con bancos, máquinas y zona de pesas" width="640" height="359" loading="lazy" decoding="async" /></div>
            <figcaption><span>CADA DÍA, UN PASO MÁS</span><p>Empieza a tu ritmo y construye tu propia constancia.</p></figcaption>
          </figure>
        </div>
      </section>
      <section id="como-funciona" className="home-how" aria-labelledby="home-how-title">
        <div className="home-container">
          <p className="home-section-label">Cómo funciona<span aria-hidden="true" /></p>
          <h2 id="home-how-title">Tres pasos para empezar.</h2>
          <ol className="home-steps">
            {steps.map(([title, description], index) => <li key={title}>
              <span aria-hidden="true">{String(index + 1).padStart(2, '0')}</span>
              <h3>{title}</h3><p>{description}</p>
            </li>)}
          </ol>
        </div>
      </section>
      <section className="home-join" aria-labelledby="home-join-title">
        <div className="home-container">
          <div><h2 id="home-join-title">Tu siguiente nivel<br /><em>te espera.</em></h2><p>Crea tu cuenta y solicita tu acceso.</p></div>
          <div className="home-hero-actions">
            <a className="home-button" href="/registro" onClick={go('registro')}>Quiero ser parte <Icon name="arrow" /></a>
            <a className="home-existing" href="/login" onClick={go('login')}>Ya tengo una cuenta</a>
          </div>
        </div>
      </section>
    </main>
    <footer className="home-footer"><div className="home-container">
      <Brand href="#inicio" />
      <p>Hecho para la comunidad Tecsup.</p>
      <nav aria-label="Enlaces del pie de página">
        {sections.map(([id, label]) => <a key={id} href={`#${id}`}>{label}</a>)}
        <a href="/login" onClick={go('login')}>Iniciar sesión</a>
      </nav>
    </div></footer>
  </div>
}
