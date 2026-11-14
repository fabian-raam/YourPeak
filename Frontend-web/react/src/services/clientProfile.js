import { request } from './auth'

const path = 'cliente/perfil/'

export function getClientProfile() {
  return request(path)
}

export function saveClientProfile(profile) {
  return request(path, profile, { method: 'PUT' })
}
