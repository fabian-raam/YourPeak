export const clientSections = {
  inicio: { path: '/', label: 'Inicio' },
  membresia: { path: '/mi-membresia', label: 'Mi membresía' },
  perfil: { path: '/mi-perfil', label: 'Mi perfil' },
  ejercicios: { path: '/cliente/ejercicios', label: 'Ejercicios' },
  disponibilidad: { path: '/cliente/disponibilidad', label: 'Disponibilidad' },
  'mi-rutina': { path: '/cliente/mi-rutina', label: 'Mi rutina' },
  nutricion: { path: '/cliente/nutricion', label: 'Nutrición' },
  progreso: { path: '/cliente/progreso', label: 'Mi progreso' },
}

export const clientSectionFromPath = path => Object.keys(clientSections).find(section => clientSections[section].path === path)
