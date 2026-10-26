import { request } from './auth'

const base = 'admin/clientes/'

// La identidad administrativa la obtiene Django de la sesión, nunca de React.
export async function listPending() {
  const data = await request(`${base}pendientes/`)
  if (!Array.isArray(data)) throw new Error('No se pudo interpretar la lista de solicitudes.')
  return data
}

export const getPending = id => request(`${base}pendientes/${encodeURIComponent(id)}/`)
export const approvePending = id => request(`${base}${encodeURIComponent(id)}/aprobar/`, {})
