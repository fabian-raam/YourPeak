import { request } from './auth'

const base = 'admin/ejercicios/'
const exercisePath = id => `${base}${encodeURIComponent(id)}/`

export async function listExercises() {
  const data = await request(base)
  if (!Array.isArray(data)) throw new Error('No se pudo interpretar la lista de ejercicios.')
  return data
}

export const getExerciseCatalogs = () => request(`${base}catalogos/`)
export const getExercise = id => request(exercisePath(id))
export const createExercise = data => request(base, data)
export const updateExercise = (id, data) => request(exercisePath(id), data, { method: 'PUT' })
export const setExerciseStatus = (id, estado) => request(`${exercisePath(id)}estado/`, { estado }, { method: 'PATCH' })
