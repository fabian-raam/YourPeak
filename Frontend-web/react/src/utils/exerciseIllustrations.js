// Ilustraciones del catálogo (src/assets/exercises); el archivo se llama como el ejercicio sin tildes.
const files = import.meta.glob('../assets/exercises/*.jpg', { eager: true, query: '?url', import: 'default' })

export function exerciseSlug(name = '') {
  return name.normalize('NFD').replace(/\p{M}+/gu, '').toLowerCase().trim().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '')
}

export function uploadedExerciseImage(path) {
  const match = /^\/api\/ejercicios\/imagenes\/([a-f0-9]{32}\.(?:jpg|png))$/.exec(path)
  return match ? `/api/web/ejercicios/imagenes/${match[1]}/` : null
}

export function exerciseImage({ nombre, imagenRuta }) {
  const key = imagenRuta?.replace(/\.(?:jpg|jpeg|png)$/i, '')
  return uploadedExerciseImage(imagenRuta)
    ?? files[`../assets/exercises/${key}.jpg`]
    ?? files[`../assets/exercises/${exerciseSlug(nombre ?? '')}.jpg`]
    ?? null
}
