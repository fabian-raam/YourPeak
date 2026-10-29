let csrfToken

export class ApiError extends Error {
  constructor(message, status, fields = {}) {
    super(message)
    this.status = status
    this.fields = fields
  }
}

export async function request(path, data, options = {}) {
  const method = options.method || (data === undefined ? 'GET' : 'POST')
  // Todas las escrituras, incluidas PUT y PATCH, conservan la protección CSRF.
  const writing = !['GET', 'HEAD', 'OPTIONS'].includes(method)
  const headers = { Accept: 'application/json' }
  if (writing) {
    if (!csrfToken) {
      const response = await request('csrf/')
      csrfToken = response.csrfToken
    }
    headers['Content-Type'] = 'application/json'
    headers['X-CSRFToken'] = csrfToken
  }
  let response
  try {
    response = await fetch(`/api/web/${path}`, {
      method,
      headers,
      credentials: 'same-origin',
      cache: 'no-store',
      body: data === undefined ? undefined : JSON.stringify(data),
    })
  } catch {
    throw new ApiError('No se pudo conectar. Comprueba que Django esté ejecutándose.', 0)
  }
  const result = await response.json().catch(() => ({}))
  if (!response.ok) {
    if (response.status === 403) csrfToken = undefined
    throw new ApiError(result.detail || (response.status === 403
      ? 'La sesión del formulario venció. Inténtalo de nuevo.'
      : 'No se pudo completar la solicitud.'), response.status, result.campos)
  }
  if (result.csrfToken) csrfToken = result.csrfToken
  if (path === 'logout/') csrfToken = undefined
  return result
}
