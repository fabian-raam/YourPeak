import { request } from './auth'

const routinePath = 'cliente/rutina/'

export async function getRoutine() {
  const data = await request(routinePath)
  if (!Array.isArray(data.dias)) throw new Error('No se pudo interpretar tu rutina semanal.')
  return data.dias
}

export async function getRoutineExercises() {
  const data = await request(`${routinePath}ejercicios/`)
  if (!Array.isArray(data)) throw new Error('No se pudo interpretar el catálogo de ejercicios.')
  return data
}

export async function saveRoutine(dias) {
  const data = await request(routinePath, { dias }, { method: 'PUT' })
  if (!Array.isArray(data.dias)) throw new Error('No se pudo interpretar la rutina guardada.')
  return data.dias
}
