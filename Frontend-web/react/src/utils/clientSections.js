export const clientSections = {
  inicio: { path: '/', label: 'Inicio' },
  membresia: {
    path: '/mi-membresia',
    label: 'Mi membresía',
    description: 'Consulta tu plan y sus fechas.',
  },
  perfil: {
    path: '/mi-perfil',
    label: 'Mi perfil',
    description: 'Cuéntanos sobre tus objetivos y tu experiencia para personalizar tu entrenamiento.',
  },
  ejercicios: {
    path: '/cliente/ejercicios',
    label: 'Ejercicios',
    description: 'Explora los ejercicios del gimnasio por grupo muscular y nivel.',
  },
  disponibilidad: {
    path: '/cliente/disponibilidad',
    label: 'Disponibilidad',
    description: 'Indica qué días y a qué horas puedes entrenar.',
  },
  'mi-rutina': {
    path: '/cliente/mi-rutina',
    label: 'Mi rutina',
    description: 'Tu planificación semanal de entrenamiento',
  },
  nutricion: {
    path: '/cliente/nutricion',
    label: 'Nutrición',
    description: 'Registra lo que comes y revisa el resumen de cada día.',
  },
  progreso: {
    path: '/cliente/progreso',
    label: 'Mi progreso',
    description: 'Registra tu peso y observa cómo cambia con el tiempo.',
  },
}

export const clientSectionFromPath = path => Object.keys(clientSections).find(section => clientSections[section].path === path)
