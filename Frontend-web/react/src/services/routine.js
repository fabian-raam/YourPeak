import { request } from './auth'

const routinePath = 'cliente/rutina/'

export async function getRoutine() {
  return (await getRoutineSummary()).dias
}

export async function getRoutineSummary() {
  const data = await request(routinePath)
  if (!Array.isArray(data.dias)) throw new Error('No se pudo interpretar tu rutina semanal.')
  return data
}

export async function getRoutineExercises() {
  const data = await request(`${routinePath}ejercicios/`)
  if (!Array.isArray(data)) throw new Error('No se pudo interpretar el catálogo de ejercicios.')
  return data
}

/** Pide una propuesta de rutina con IA; no se guarda hasta llamar a saveRoutine. */
export async function generateRoutine() {
  const data = await request(`${routinePath}generar/`, {})
  if (!Array.isArray(data.dias)) throw new Error('No se pudo interpretar la rutina propuesta.')
  return data
}

export async function saveRoutine(dias) {
  const data = await request(routinePath, { dias }, { method: 'PUT' })
  if (!Array.isArray(data.dias)) throw new Error('No se pudo interpretar la rutina guardada.')
  return data.dias
}

/** Registra que el cliente completó hoy todas las series prescritas de un ejercicio. */
export async function completeRoutineExercise(idPlanEjercicio, series) {
  return request(`${routinePath}ejercicios/${idPlanEjercicio}/completar/`, { series })
}
