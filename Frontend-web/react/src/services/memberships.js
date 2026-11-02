import { request } from './auth'

const base = 'admin/planes-membresia/'
const planPath = id => `${base}${encodeURIComponent(id)}/`
const clientPath = id => `admin/clientes/${encodeURIComponent(id)}/membresias/`

// Django conserva la sesión y comprueba el rol; no se envían claves internas.
export async function listPlans() {
  const data = await request(base)
  if (!Array.isArray(data)) throw new Error('No se pudo interpretar la lista de planes.')
  return data
}

export const getPlan = id => request(planPath(id))
export const createPlan = data => request(base, data)
export const renamePlan = (id, nombre) => request(planPath(id), { nombre }, { method: 'PUT' })
export const setPlanStatus = (id, estado) => request(`${planPath(id)}estado/`, { estado }, { method: 'PATCH' })
export async function listActiveClients() {
  const data = await request('admin/clientes/activos/')
  if (!Array.isArray(data)) throw new Error('No se pudo interpretar la lista de clientes.')
  return data
}
export async function listInactiveClients() {
  const data = await request('admin/clientes/inactivos/')
  if (!Array.isArray(data)) throw new Error('No se pudo interpretar la lista de clientes inactivos.')
  return data
}
export const listClientMemberships = id => request(clientPath(id))
export const renewClientMembership = (id, data) => request(clientPath(id), data)
