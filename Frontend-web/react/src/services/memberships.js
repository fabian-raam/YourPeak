import { request } from './auth'

const base = 'admin/planes-membresia/'
const planPath = id => `${base}${encodeURIComponent(id)}/`

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
