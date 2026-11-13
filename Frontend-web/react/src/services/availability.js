import { request } from './auth'

const path = 'cliente/disponibilidad/'

export async function getAvailability() {
  const data = await request(path)
  if (!Array.isArray(data.dias)) throw new Error('No se pudo interpretar la disponibilidad semanal.')
  return data.dias
}

export async function saveAvailability(dias) {
  const data = await request(path, { dias }, { method: 'PUT' })
  if (!Array.isArray(data.dias)) throw new Error('No se pudo interpretar la disponibilidad guardada.')
  return data.dias
}
