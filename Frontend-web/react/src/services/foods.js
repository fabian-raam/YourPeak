import { request } from './auth'

const base = 'admin/alimentos/'
const foodPath = id => `${base}${encodeURIComponent(id)}/`

export async function listFoods() {
  const data = await request(base)
  if (!Array.isArray(data)) throw new Error('No se pudo interpretar la lista de alimentos.')
  return data
}

export const getFood = id => request(foodPath(id))
export const createFood = data => request(base, data)
export const updateFood = (id, data) => request(foodPath(id), data, { method: 'PUT' })
