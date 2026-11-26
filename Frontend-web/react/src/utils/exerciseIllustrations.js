// Ilustraciones del catálogo (src/assets/exercises); el archivo se llama como el ejercicio sin tildes.
const files = import.meta.glob('../assets/exercises/*.jpg', { eager: true, query: '?url', import: 'default' })

export function exerciseSlug(name = '') {
  return name.normalize('NFD').replace(/\p{M}+/gu, '').toLowerCase().trim().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '')
}

/** URL de la ilustración del ejercicio o null si no tiene. */
export function exerciseImage(name) {
  return files[`../assets/exercises/${exerciseSlug(name)}.jpg`] ?? null
}
