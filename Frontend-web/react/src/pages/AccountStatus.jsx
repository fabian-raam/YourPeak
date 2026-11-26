import { useState } from 'react'
import Icon from '../components/Icon'

export default function AccountStatus({ user, onRefresh, onLogout, busy, error }) {
  const [checking, setChecking] = useState(false)
  const [notice, setNotice] = useState('')
  const estado = user.estado?.toUpperCase()
  const pending = estado === 'PENDIENTE'
  const inactive = estado === 'INACTIVO'
  const expired = estado === 'ACTIVO' && user.membresiaVigente === false
  const title = pending ? 'Tu cuenta está en revisión' : expired ? 'Tu membresía venció' : 'Tu acceso aún no está disponible'
  const description = pending
    ? 'Recibimos tu registro. Un administrador revisará tu solicitud para activar tu cuenta.'
    : expired || inactive
      ? 'Consulta con la administración del gimnasio para renovar tu membresía.'
      : 'Consulta con la administración del gimnasio para revisar el estado de tu cuenta.'
  const label = pending ? 'Pendiente de aprobación'
    : expired ? 'Membresía vencida' : inactive ? 'Cuenta inactiva' : 'Acceso no habilitado'

  async function refresh() {
    setChecking(true)
    setNotice('')
    try {
      const current = await onRefresh()
      const currentStatus = current?.estado?.toUpperCase()
      if (currentStatus === 'PENDIENTE') {
        setNotice('Tu solicitud continúa pendiente de revisión.')
      } else if (currentStatus === 'INACTIVO') {
        setNotice('Tu cuenta sigue inactiva.')
      } else if (currentStatus === 'ACTIVO' && current.membresiaVigente === false) {
        setNotice('Todavía no tienes una membresía vigente.')
      }
    } finally {
      setChecking(false)
    }
  }

  return <>
    <h1>{title}</h1>
    <p className="intro">{description}</p>
    <div className="account-summary">
      <span className={`account-badge${pending ? ' is-pending' : ''}`}>
        <Icon name={pending ? 'clock' : 'shield'} width="16" height="16" />
        {label}
      </span>
      <strong>{user.nombre} {user.apellido}</strong>
      <span className="account-email">{user.correo}</span>
    </div>
    {error && <p className="alert" role="alert">{error}</p>}
    {notice && <p role="status" className="status-note">{notice}</p>}
    <button type="button" className="primary" disabled={checking || busy} onClick={refresh}>
      {checking ? 'Consultando…' : 'Consultar estado'}
    </button>
    <button type="button" className="text-button status-logout" disabled={checking || busy} onClick={onLogout}>
      Cerrar sesión
    </button>
  </>
}
