import { useEffect, useRef, useState } from 'react'
import { request } from '../services/auth'

let scriptLoad

function loadGoogle() {
  if (window.google?.accounts?.id) return Promise.resolve(window.google.accounts.id)
  scriptLoad ??= new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = 'https://accounts.google.com/gsi/client'
    script.async = true

    function fail() {
      window.clearTimeout(timeout)
      script.remove()
      reject(new Error('No se pudo cargar Google.'))
    }

    const timeout = window.setTimeout(fail, 10000)
    script.onerror = fail
    script.onload = () => {
      const api = window.google?.accounts?.id
      if (!api) return fail()
      window.clearTimeout(timeout)
      resolve(api)
    }
    document.head.append(script)
  }).catch(error => {
    scriptLoad = undefined
    throw error
  })
  return scriptLoad
}

export default function GoogleButton({ registering, busy, onCredential, revision }) {
  const container = useRef(null)
  const callback = useRef(onCredential)
  const [config, setConfig] = useState(null)
  const [unavailable, setUnavailable] = useState(false)

  useEffect(() => {
    callback.current = onCredential
  }, [onCredential])

  useEffect(() => {
    let active = true
    // La configuración crea el desafío asociado a la sesión del navegador.
    const timer = window.setTimeout(async () => {
      try {
        const data = await request('google/config/')
        if (active) {
          setConfig(data)
          setUnavailable(false)
        }
      } catch {
        if (active) setUnavailable(true)
      }
    }, 0)
    return () => {
      active = false
      window.clearTimeout(timer)
    }
  }, [revision])

  useEffect(() => {
    const target = container.current
    if (!config?.habilitado || !target) return
    let active = true
    let observer

    loadGoogle().then(api => {
      if (!active) return
      api.initialize({
        client_id: config.clientId,
        callback: result => {
          if (active && result.credential) callback.current(result.credential)
        },
        nonce: config.nonce,
        hd: config.dominio,
        use_fedcm_for_button: true,
      })
      let renderedWidth
      function render() {
        const width = Math.min(400, target.clientWidth)
        if (!width || width === renderedWidth) return
        renderedWidth = width
        target.replaceChildren()
        api.renderButton(target, {
          type: 'standard',
          theme: 'outline_dark',
          size: 'large',
          text: registering ? 'signup_with' : 'continue_with',
          shape: 'rectangular',
          locale: 'es',
          width,
        })
      }
      render()
      observer = new ResizeObserver(render)
      observer.observe(target)
    }).catch(() => {
      if (active) setUnavailable(true)
    })
    return () => {
      active = false
      observer?.disconnect()
      target.replaceChildren()
    }
  }, [config, registering])

  if (!config?.habilitado && !unavailable) return null

  const divider = <div className="google-divider" aria-hidden="true">o</div>
  return <div className="google-option">
    {!registering && divider}
    {unavailable && <p className="google-unavailable" role="status">Google no está disponible en este momento.</p>}
    <div className="google-button" ref={container} inert={busy} />
    {registering && divider}
  </div>
}
