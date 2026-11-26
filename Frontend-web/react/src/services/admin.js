import { request } from './auth'

const base = 'admin/clientes/'

// La identidad administrativa la obtiene Django de la sesión, nunca de React.
export async function listPending() {
  const data = await request(`${base}pendientes/`)
  if (!Array.isArray(data)) throw new Error('No se pudo interpretar la lista de solicitudes.')
  return data
}

export const getPending = id => request(`${base}pendientes/${encodeURIComponent(id)}/`)
export const approvePending = (id, data) => request(`${base}${encodeURIComponent(id)}/aprobar/`, data)

// Lecturas del panel administrativo, protegidas por la sesión Django.
export const getAdminOverview = () => request('admin/resumen/')
export const listAdminTrainings = () => request('admin/entrenamientos/')
export const getAdminTraining = id => request(`admin/entrenamientos/${encodeURIComponent(id)}/`)
