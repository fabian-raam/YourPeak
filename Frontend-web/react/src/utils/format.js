export const fullName = person => `${person.nombre || ''} ${person.apellido || ''}`.trim()
export const initials = person => `${person.nombre?.[0] || ''}${person.apellido?.[0] || ''}`.toUpperCase()

/** Una fecha de nacimiento no tiene zona horaria; evita moverla al día anterior. */
export function birthDate(value) {
  if (!value) return 'Sin registrar'
  const date = new Date(`${value}T12:00:00`)
  return Number.isNaN(date.getTime()) ? 'Sin registrar' : date.toLocaleDateString('es-PE', { day: '2-digit', month: 'long', year: 'numeric' })
}

export function registrationDate(value) {
  if (!value) return 'Sin registrar'
  // Spring guarda fechaAlta en UTC aunque su LocalDateTime no incluya el sufijo Z.
  const date = new Date(/[zZ]|[+-]\d{2}:\d{2}$/.test(value) ? value : `${value}Z`)
  return Number.isNaN(date.getTime()) ? 'Sin registrar' : date.toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric', timeZone: 'America/Lima' })
}
