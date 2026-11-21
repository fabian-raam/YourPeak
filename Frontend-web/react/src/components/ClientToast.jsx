import { useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import Icon from './Icon'
import './ClientToast.css'

export default function ClientToast({ title, message, onClose, inline = false }) {
  const close = useRef(onClose)
  const [leaving, setLeaving] = useState(false)
  useEffect(() => { close.current = onClose }, [onClose])
  useEffect(() => {
    const timer = window.setTimeout(() => setLeaving(true), 4000)
    return () => window.clearTimeout(timer)
  }, [message])
  useEffect(() => {
    if (!leaving) return
    const timer = window.setTimeout(() => close.current(), 220)
    return () => window.clearTimeout(timer)
  }, [leaving])

  const toast = <aside className={`client-toast${leaving ? ' client-toast-leaving' : ''}`} aria-label="Notificación">
    <span className="client-toast-icon" aria-hidden="true"><Icon name="check" /></span>
    <div className="client-toast-copy" role="status" aria-live="polite" aria-atomic="true">
      <strong>{title}</strong><p>{message}</p>
    </div>
    <button className="client-toast-close" type="button" aria-label="Cerrar notificación" onClick={() => setLeaving(true)}>
      <Icon name="close" />
    </button>
    <span key={message} className="client-toast-timer" aria-hidden="true" />
  </aside>

  return inline ? toast : createPortal(toast, document.body)
}
