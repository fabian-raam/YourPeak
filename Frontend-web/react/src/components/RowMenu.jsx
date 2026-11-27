import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import Icon from './Icon'
import './RowMenu.css'

/**
 * Menú «⋯» de acciones de una fila. Se posiciona con coordenadas fijas para que la tabla
 * (que tiene desplazamiento horizontal) no lo recorte. Cada acción recibe el botón «⋯»
 * como currentTarget para devolverle el foco al cerrar el diálogo que abra.
 */
export default function RowMenu({ label, items }) {
  const [open, setOpen] = useState(false)
  const [position, setPosition] = useState(null)
  const trigger = useRef(null)
  const menu = useRef(null)

  useLayoutEffect(() => {
    if (!open) return
    const rect = trigger.current.getBoundingClientRect()
    const height = menu.current?.offsetHeight || 0
    const below = rect.bottom + 6 + height <= window.innerHeight
    setPosition({ top: below ? rect.bottom + 6 : rect.top - 6 - height, left: Math.max(8, rect.right - 200) })
    menu.current?.querySelector('button')?.focus()
  }, [open])

  useEffect(() => {
    if (!open) return undefined
    const outside = event => {
      if (!menu.current?.contains(event.target) && !trigger.current?.contains(event.target)) setOpen(false)
    }
    const key = event => {
      if (event.key === 'Escape') { setOpen(false); trigger.current?.focus() }
      if (!['ArrowDown', 'ArrowUp'].includes(event.key)) return
      const buttons = [...menu.current.querySelectorAll('button')]
      const index = buttons.indexOf(document.activeElement)
      buttons[(index + (event.key === 'ArrowDown' ? 1 : -1) + buttons.length) % buttons.length]?.focus()
      event.preventDefault()
    }
    const close = () => setOpen(false)
    document.addEventListener('pointerdown', outside)
    document.addEventListener('keydown', key)
    window.addEventListener('resize', close)
    window.addEventListener('scroll', close, true)
    return () => {
      document.removeEventListener('pointerdown', outside)
      document.removeEventListener('keydown', key)
      window.removeEventListener('resize', close)
      window.removeEventListener('scroll', close, true)
    }
  }, [open])

  return <div className="row-menu">
    <button ref={trigger} type="button" className="row-menu-trigger" aria-label={label} aria-haspopup="menu"
      aria-expanded={open} onClick={() => setOpen(value => !value)}><Icon name="more" /></button>
    {open && <div ref={menu} className="row-menu-list" role="menu" aria-label={label}
      style={position ? { top: position.top, left: position.left } : { visibility: 'hidden' }}>
      {items.map(item => <button key={item.label} type="button" role="menuitem" className={item.tone || undefined}
        onClick={() => { setOpen(false); item.onClick({ currentTarget: trigger.current }) }}>
        <Icon name={item.icon} /> {item.label}
      </button>)}
    </div>}
  </div>
}
