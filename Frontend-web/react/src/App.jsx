import { useCallback, useEffect, useState } from 'react'
import Field from './components/Field'
import { request } from './services/auth'
import AuthHero from './components/AuthHero'
import UserManagement from './pages/UserManagement'
import MembershipPlans from './pages/MembershipPlans'
import ExercisesCatalog from './pages/ExercisesCatalog'
import FoodsCatalog from './pages/FoodsCatalog'
import PlanesPractica from './pages/PlanesPractica'
import AdminLayout from './components/AdminLayout'
import AccountStatus from './pages/AccountStatus'
import ClientAccountSection from './pages/ClientAccountSection'
import HomePage from './pages/HomePage'
import { clientSectionFromPath, clientSections } from './utils/clientSections'
import ClientLayout from './components/ClientLayout'
import ClientExercises from './pages/ClientExercises'
import ClientAvailability from './pages/ClientAvailability'
import ClientRoutine from './pages/ClientRoutine'
import './App.css'

const empty = { nombre: '', apellido: '', fechaNacimiento: '', correo: '', contrasena: '', confirmar_contrasena: '' }
const emailValid = value => /^[^@\s]+@tecsup\.edu\.pe$/.test(value.trim().toLowerCase())
const publicPageFromPath = path => path === '/' ? 'inicio' : path === '/registro' ? 'registro' : 'login'
const localDate = (offset = 0) => {
  const date = new Date()
  date.setDate(date.getDate() + offset)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

export default function App() {
  const [pendingCount, setPendingCount] = useState(null)
  const sectionFromPath = path => ['usuarios', 'clientes', 'membresias', 'ejercicios', 'alimentos', 'practica'].find(section => path === `/admin/${section}`) || 'usuarios'
  const [adminSection, setAdminSection] = useState(sectionFromPath(location.pathname))
  const [clientSection, setClientSection] = useState(() => clientSectionFromPath(location.pathname) || 'inicio')
  const [page, setPage] = useState(publicPageFromPath(location.pathname))
  const [values, setValues] = useState(empty)
  const [fields, setFields] = useState({})
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const registering = page === 'registro'
  const isAdmin = user?.rol === 'ADMINISTRADOR'
  const isActive = user?.rol === 'CLIENTE' && user.estado?.toUpperCase() === 'ACTIVO'
    && user.membresiaVigente === true

  const sessionExpired = useCallback(() => {
    setUser(null); setPendingCount(null); setPage('login'); setValues(empty)
    setFields({}); setNotice('')
    setError('Tu sesión terminó. Inicia sesión de nuevo.')
    history.replaceState({}, '', '/login')
  }, [])

  async function refreshProfile() {
    setError('')
    try {
      const data = await request('perfil/')
      setUser(data.usuario)
      return data.usuario
    } catch (err) {
      if (err.status === 401) sessionExpired()
      else setError(err.message)
      return null
    }
  }

  // La URL refleja el acceso que informa el servidor, no un rol guardado en el navegador.
  useEffect(() => {
    if (loading) return
    const path = user
      ? isAdmin ? `/admin/${adminSection}` : isActive ? clientSections[clientSection].path : '/pendiente'
      : clientSectionFromPath(location.pathname) && location.pathname !== '/' ? '/login' : location.pathname
    if (location.pathname !== path) history.replaceState({}, '', path)
  }, [user, isAdmin, isActive, adminSection, clientSection, loading, page])

  useEffect(() => {
    let active = true
    request('perfil/').then(data => { if (active) setUser(data.usuario) })
      .catch(err => { if (active && err.status !== 401) setError(err.message) })
      .finally(() => { if (active) setLoading(false) })
    const onPop = () => {
      setAdminSection(sectionFromPath(location.pathname))
      setClientSection(clientSectionFromPath(location.pathname) || 'inicio')
      setPage(publicPageFromPath(location.pathname))
      setError(''); setFields({}); setValues(empty); setNotice('')
    }
    window.addEventListener('popstate', onPop)
    return () => { active = false; window.removeEventListener('popstate', onPop) }
  }, [])
  useEffect(() => {
    const adminTitles = { membresias: 'Planes', clientes: 'Usuarios', ejercicios: 'Ejercicios', alimentos: 'Alimentos', practica: 'Práctica React', usuarios: 'Usuarios' }
    const title = isAdmin ? `${adminTitles[adminSection]} · Administración`
      : isActive ? clientSections[clientSection].label
        : user ? 'Mi cuenta' : page === 'inicio' ? 'Inicio' : registering ? 'Crear cuenta' : 'Iniciar sesión'
    document.title = `${title} | YourPeak`
  }, [user, registering, isAdmin, isActive, adminSection, clientSection, page])

  function navigateAdmin(section) {
    if (section === adminSection) return
    history.pushState({}, '', `/admin/${section}`)
    setAdminSection(section)
  }

  function navigateClient(section) {
    if (section === clientSection) return
    history.pushState({}, '', clientSections[section].path)
    setClientSection(section)
    window.scrollTo(0, 0)
  }

  function navigate(next) {
    history.pushState({}, '', next === 'inicio' ? '/' : next === 'registro' ? '/registro' : '/login')
    setPage(next); setValues(empty); setFields({}); setError(''); setNotice('')
    window.scrollTo(0, 0)
  }
  function update(event) {
    const { name, value } = event.target
    setValues(previous => ({ ...previous, [name]: value }))
    setFields(previous => ({ ...previous, [name]: undefined }))
  }
  async function submit(event) {
    event.preventDefault()
    const errors = {}
    if (!emailValid(values.correo)) errors.correo = 'Usa tu correo institucional @tecsup.edu.pe.'
    if (registering) {
      if (!values.nombre.trim()) errors.nombre = 'Ingresa tu nombre.'
      if (!values.apellido.trim()) errors.apellido = 'Ingresa tu apellido.'
      if (!values.fechaNacimiento || values.fechaNacimiento >= localDate()) errors.fechaNacimiento = 'Ingresa una fecha anterior a hoy.'
      if (values.contrasena.length < 8) errors.contrasena = 'Usa al menos 8 caracteres.'
      if (values.contrasena !== values.confirmar_contrasena) errors.confirmar_contrasena = 'Las contraseñas no coinciden.'
    }
    setFields(errors); setError(''); setNotice('')
    if (Object.keys(errors).length) {
      setError('Revisa los campos indicados.')
      document.getElementById(Object.keys(errors)[0])?.focus()
      return
    }
    setBusy(true)
    try {
      const data = await request(registering ? 'registro/' : 'login/', { ...values, correo: values.correo.trim().toLowerCase() })
      if (registering) {
        const correo = values.correo
        navigate('login'); setValues({ ...empty, correo }); setNotice(data.detail)
      } else {
        setClientSection('inicio')
        setUser(data.usuario); setValues(empty); history.replaceState({}, '', '/')
      }
    } catch (err) { setError(err.message); setFields(err.fields || {}) }
    finally { setBusy(false) }
  }
  async function logout() {
    setBusy(true); setError('')
    try { await request('logout/', {}); setUser(null); setPendingCount(null); navigate('login') }
    catch (err) { setError(err.message) }
    finally { setBusy(false) }
  }
  const field = (name, label, props = {}) => <Field key={name} name={name} label={label} value={values[name]}
    onChange={update} error={fields[name]} required disabled={busy} {...props} />

  if (user && isAdmin) return <AdminLayout user={user} section={adminSection} onNavigate={navigateAdmin}
    onLogout={logout} logoutBusy={busy} pendingCount={pendingCount}>
    {adminSection === 'membresias'
      ? <MembershipPlans onExpired={sessionExpired} logoutError={error} />
      : adminSection === 'ejercicios'
          ? <ExercisesCatalog onExpired={sessionExpired} logoutError={error} />
          : adminSection === 'alimentos'
            ? <FoodsCatalog onExpired={sessionExpired} logoutError={error} />
            : adminSection === 'practica'
              ? <PlanesPractica />
            : <UserManagement section={adminSection} onNavigate={navigateAdmin} onExpired={sessionExpired}
                pendingCount={pendingCount}
                logoutError={error} onPendingCount={setPendingCount} />}
  </AdminLayout>

  if (isActive) return <ClientLayout user={user} section={clientSection} onNavigate={navigateClient}
    onLogout={logout} logoutBusy={busy} error={error}>
    {['inicio', 'perfil', 'membresia'].includes(clientSection)
      ? <ClientAccountSection user={user} section={clientSection} onNavigate={navigateClient} />
      : clientSection === 'disponibilidad'
        ? <ClientAvailability onExpired={sessionExpired} />
        : clientSection === 'mi-rutina'
          ? <ClientRoutine onExpired={sessionExpired} onNavigate={navigateClient} />
          : <ClientExercises onExpired={sessionExpired} />}
  </ClientLayout>

  if (!user && page === 'inicio') return <HomePage onNavigate={navigate} />
  return <main className={`auth-layout ${registering ? 'register-layout' : ''}`}>
    <AuthHero registering={registering} onNavigate={event => { event.preventDefault(); if (!busy) navigate('inicio') }} />
    <section className="form-side" aria-label="Acceso a YourPeak">
      {!user && <a className="auth-home-link" href="/" onClick={event => { if (event.button === 0 && !event.ctrlKey && !event.metaKey && !event.shiftKey && !event.altKey) { event.preventDefault(); if (!busy) navigate('inicio') } }}>← Volver al inicio</a>}
      <div className="auth-card" key={loading ? 'loading' : user ? 'profile' : page}>
        {loading ? <p role="status">Comprobando tu sesión…</p> : user ?
          <AccountStatus user={user} onRefresh={refreshProfile} onLogout={logout} busy={busy} error={error} /> : <>
          <span className="eyebrow">COMUNIDAD TECSUP</span>
          <h1>{registering ? 'Crea tu cuenta' : 'Iniciar sesión'}</h1>
          <p className="intro">{registering ? 'Comienza tu camino con Your Peak.' : 'Bienvenido de nuevo a Your Peak.'}</p>
          {notice && <p className="alert success" role="status">{notice}</p>}
          {error && <p className="alert" role="alert">{error}</p>}
          <form onSubmit={submit}>
            <fieldset disabled={busy}>
              {registering && <>
                <div className="name-row">
                  {field('nombre', 'Nombre', { autoComplete: 'given-name', maxLength: 100, placeholder: 'Tu nombre' })}
                  {field('apellido', 'Apellido', { autoComplete: 'family-name', maxLength: 100, placeholder: 'Tu apellido' })}
                </div>
                {field('fechaNacimiento', 'Fecha de nacimiento', { type: 'date', autoComplete: 'bday', max: localDate(-1) })}
              </>}
              {field('correo', 'Correo institucional', { type: 'email', autoComplete: 'username', maxLength: 254,
                placeholder: 'nombre@tecsup.edu.pe', hint: 'Acceso exclusivo con tu correo @tecsup.edu.pe.' })}
              {field('contrasena', 'Contraseña', { type: 'password', autoComplete: registering ? 'new-password' : 'current-password',
                maxLength: 128, placeholder: 'Ingresa tu contraseña', hint: registering ? 'Entre 8 y 128 caracteres. Puedes usar una frase.' : undefined })}
              {registering && field('confirmar_contrasena', 'Confirmar contraseña', { type: 'password', autoComplete: 'new-password', maxLength: 128, placeholder: 'Repite tu contraseña' })}
              <button className="primary" type="submit">{busy ? 'Un momento…' : registering ? 'Crear cuenta' : 'Iniciar sesión'}</button>
            </fieldset>
          </form>
          <p className="switch">{registering ? '¿Ya tienes una cuenta?' : '¿No tienes una cuenta?'}{' '}
            <a href={registering ? '/login' : '/registro'} aria-disabled={busy} onClick={event => { event.preventDefault(); if (!busy) navigate(registering ? 'login' : 'registro') }}>
              {registering ? 'Inicia sesión' : 'Regístrate'}
            </a>
          </p>
        </>}
      </div>
    </section>
  </main>
}
