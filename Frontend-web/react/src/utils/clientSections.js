export const clientSections = {
  inicio: { path: '/', label: 'Inicio' },
  membresia: { path: '/mi-membresia', label: 'Mi membresía' },
  perfil: { path: '/mi-perfil', label: 'Mi perfil' },
}

export const clientSectionFromPath = path => Object.keys(clientSections).find(section => clientSections[section].path === path)
