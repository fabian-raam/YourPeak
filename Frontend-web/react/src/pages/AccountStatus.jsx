import { useState } from 'react'
import Icon from '../components/Icon'

/** Autenticarse permite consultar el estado; solo ACTIVO habilita el acceso del cliente. */
export default function AccountStatus({ user, onRefresh, onLogout, busy, error }) {
  const [checking, setChecking] = useState(false)
  const [notice, setNotice] = useState('')
  const pending = user.estado?.toUpperCase() === 'PENDIENTE'
  const membresiaVencida = user.estado?.toUpperCase() === 'INACTIVO'
    || (user.estado?.toUpperCase() === 'ACTIVO' && user.membresiaVigente === false)
  async function refresh() {
    setChecking(true); setNotice('')
    try {
      const current = await onRefresh()
      if (current?.estado?.toUpperCase() === 'INACTIVO' || current?.membresiaVigente === false) setNotice('Tu cuenta está inactiva. Consulta con la administración para renovar tu membresía.')
      if (current?.estado?.toUpperCase() === 'PENDIENTE') setNotice('Tu solicitud continúa pendiente de revisión.')
    } finally { setChecking(false) }
  }
  return <>
    <div className="account-status-icon"><Icon name={pending ? 'clock' : 'shield'} width="30" height="30" /></div>
    <span className="eyebrow">YOUR PEAK · TU CUENTA</span>
    <h1>{pending ? 'Tu próximo paso está cerca.' : 'Tu acceso aún no está disponible.'}</h1>
    {membresiaVencida && <p className="alert" role="status">Tu cuenta está aprobada, pero tu membresía venció. Consulta con la administración para renovarla.</p>}
    <p className="intro">{pending ? 'Recibimos tu registro. Un administrador revisará tu solicitud para activar tu cuenta.' : membresiaVencida ? 'Tu período terminó. Consulta con la administración para renovar tu membresía.' : 'Consulta con la administración del gimnasio para revisar el estado de tu cuenta.'}</p>
    <div className="profile"><strong>{user.nombre} {user.apellido}</strong><span>{user.correo}</span><span className="status-badge">{pending ? 'Pendiente de aprobación' : user.estado?.toUpperCase() === 'INACTIVO' ? 'Cuenta inactiva' : 'Acceso no habilitado'}</span></div>
    {error && <p className="alert" role="alert">{error}</p>}
    {notice && <p role="status" className="status-note">{notice}</p>}
    <button className="primary" disabled={checking || busy} onClick={refresh}>{checking ? 'Consultando…' : 'Consultar estado'}</button>
    <button className="text-button status-logout" disabled={checking || busy} onClick={onLogout}>Cerrar sesión</button>
  </>
}
