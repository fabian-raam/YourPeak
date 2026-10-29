import test from 'node:test'
import assert from 'node:assert/strict'

const json = (value, status = 200) => new Response(JSON.stringify(value), {
  status, headers: { 'Content-Type': 'application/json' },
})
const freshRequest = async name => (await import(`../src/services/auth.js?test=${name}`)).request

test('consultar conserva la sesión sin solicitar CSRF ni enviar un cuerpo', async t => {
  const calls = []
  t.mock.method(globalThis, 'fetch', async (...args) => { calls.push(args); return json([]) })
  const request = await freshRequest('read')
  await request('admin/planes-membresia/')
  assert.equal(calls.length, 1)
  assert.equal(calls[0][1].method, 'GET')
  assert.equal(calls[0][1].credentials, 'same-origin')
  assert.equal(calls[0][1].body, undefined)
})

test('alta, edición y estado usan POST/PUT/PATCH y comparten un token CSRF', async t => {
  const calls = []
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    calls.push({ url, ...options })
    return json(url.endsWith('csrf/') ? { csrfToken: 'csrf-local' } : { nombre: 'Plan' })
  })
  const request = await freshRequest('writes')
  await request('admin/planes-membresia/', { nombre: 'Plan', duracionMeses: 3 })
  await request('admin/planes-membresia/7/', { nombre: 'Nuevo nombre' }, { method: 'PUT' })
  await request('admin/planes-membresia/7/estado/', { estado: 'INACTIVO' }, { method: 'PATCH' })
  assert.deepEqual(calls.map(call => call.method), ['GET', 'POST', 'PUT', 'PATCH'])
  for (const call of calls.slice(1)) {
    assert.equal(call.headers['X-CSRFToken'], 'csrf-local')
    assert.equal(call.headers['Content-Type'], 'application/json')
  }
  assert.deepEqual(JSON.parse(calls[1].body), { nombre: 'Plan', duracionMeses: 3 })
  assert.deepEqual(JSON.parse(calls[2].body), { nombre: 'Nuevo nombre' })
})

test('un 403 invalida el token para obtener uno nuevo en el siguiente intento', async t => {
  let tokens = 0
  let writes = 0
  t.mock.method(globalThis, 'fetch', async url => {
    if (url.endsWith('csrf/')) return json({ csrfToken: `token-${++tokens}` })
    return ++writes === 1 ? json({}, 403) : json({ estado: 'ACTIVO' })
  })
  const request = await freshRequest('csrf-expired')
  await assert.rejects(request('admin/planes-membresia/7/estado/', { estado: 'ACTIVO' }, { method: 'PATCH' }), err => err.status === 403)
  await request('admin/planes-membresia/7/estado/', { estado: 'ACTIVO' }, { method: 'PATCH' })
  assert.equal(tokens, 2)
})

test('los conflictos conservan el mensaje y los errores por campo del servidor', async t => {
  t.mock.method(globalThis, 'fetch', async url => url.endsWith('csrf/') ? json({ csrfToken: 'token' })
    : json({ detail: 'Nombre duplicado', campos: { nombre: 'Ya existe' } }, 409))
  const request = await freshRequest('conflict')
  await assert.rejects(request('admin/planes-membresia/', { nombre: 'Plan', duracionMeses: 1 }), err => {
    assert.equal(err.status, 409)
    assert.equal(err.message, 'Nombre duplicado')
    assert.equal(err.fields.nombre, 'Ya existe')
    return true
  })
})
