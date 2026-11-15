import { request } from './auth'

const base = 'cliente/progreso/'

export async function getProgressHistory() {
  const data = await request(base)
  if (!Array.isArray(data)) throw new Error('No se pudo interpretar el historial de peso.')
  return data
}

export const recordWeight = pesoKg => request(base, { pesoKg }, { method: 'POST' })
