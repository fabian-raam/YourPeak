import { request } from './auth'

export async function getClientMemberships() {
  const periods = await request('cliente/membresias/')
  if (!Array.isArray(periods)) throw new Error('No se pudo interpretar tu historial de membresías.')
  return periods
}
