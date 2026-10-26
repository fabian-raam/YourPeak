import { useState } from 'react'
import Icon from '../components/Icon'

/** Autenticarse permite consultar el estado; solo ACTIVO habilita el acceso del cliente. */
export default function AccountStatus({ user, onRefresh, onLogout, busy, error }) {
  const [checking, setChecking] = useState(false)
  const [notice, setNotice] = useState('')
  const pending = user.estado?.toUpperCase() === 'PENDIENTE'
  async function refresh() {
    setChecking(true); setNotice('')
    try {
      const current = await onRefresh()
      if (current?.estado?.toUpperCase() === 'PENDIENTE') setNotice('Tu solicitud continúa pendiente de revisión.')
    } finally { setChecking(false) }
  }
  return <>
    <div className="account-status-icon"><Icon name={pending ? 'clock' : 'shield'} width="30" height="30" /></div>
    <span className="eyebrow">YOUR PEAK · TU CUENTA</span>
    <h1>{pending ? 'Tu próximo paso está cerca.' : 'Tu acceso aún no está disponible.'}</h1>
    <p className="intro">{pending ? 'Recibimos tu registro. Un administrador revisará tu solicitud para activar tu cuenta.' : 'Consulta con la administración del gimnasio para revisar el estado de tu cuenta.'}</p>
    <div className="profile"><strong>{user.nombre} {user.apellido}</strong><span>{user.correo}</span><span className="status-badge">{pending ? 'Pendiente de aprobación' : 'Acceso no habilitado'}</span></div>
    {error && <p className="alert" role="alert">{error}</p>}
    {notice && <p role="status" className="status-note">{notice}</p>}
    <button className="primary" disabled={checking || busy} onClick={refresh}>{checking ? 'Consultando…' : 'Consultar estado'}</button>
    <button className="text-button status-logout" disabled={checking || busy} onClick={onLogout}>Cerrar sesión</button>
  </>
}
