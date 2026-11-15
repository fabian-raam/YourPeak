import { request } from './auth'

const base = 'cliente/nutricion/'

export async function listNutritionFoods() {
  const data = await request(`${base}alimentos/`)
  if (!Array.isArray(data)) throw new Error('No se pudo interpretar el catálogo de alimentos.')
  return data
}

export const getNutritionDay = date => request(`${base}${date ? `?fecha=${encodeURIComponent(date)}` : ''}`)
export const createIntake = data => request(`${base}ingestas/`, data)
export const updateIntake = (id, data) => request(`${base}ingestas/${encodeURIComponent(id)}/`, data, { method: 'PUT' })
export const deleteIntake = id => request(`${base}ingestas/${encodeURIComponent(id)}/`, undefined, { method: 'DELETE' })
