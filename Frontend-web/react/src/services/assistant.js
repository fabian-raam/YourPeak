import { request } from './auth'

/** Pregunta al asistente de alimentación y ejercicio con los últimos mensajes como contexto. */
export const askAssistant = (pregunta, historial) => request('cliente/asistente/', { pregunta, historial })
