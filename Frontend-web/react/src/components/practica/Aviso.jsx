import { useEffect } from 'react'
import Icon from '../Icon'

export default function Aviso({ aviso, onCerrar }) {
  useEffect(() => {
    if (!aviso) return undefined

    console.log(`Aviso agregado: ${aviso.nombre}`)
    const temporizador = window.setTimeout(onCerrar, 3000)
    return () => window.clearTimeout(temporizador)
  }, [aviso, onCerrar])

  if (!aviso) return null

  return <p className="practice-cafe-notice" role="status">
    <Icon name="check" /> Agregado: {aviso.nombre}
    <button type="button" onClick={onCerrar} aria-label="Cerrar aviso"><Icon name="close" /></button>
  </p>
}
