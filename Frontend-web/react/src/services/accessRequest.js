import { request } from './auth'

/** Estado e historial de la solicitud de acceso del cliente con sesión. */
export const getAccessRequest = () => request('cliente/solicitud/')

/** Envía una nueva solicitud después de un rechazo (Spring limita las solicitudes por día). */
export const requestAccessAgain = mensaje => request('cliente/solicitud/', { mensaje: mensaje || null })
