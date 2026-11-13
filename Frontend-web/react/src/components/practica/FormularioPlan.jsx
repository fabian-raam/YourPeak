import { useState } from 'react'
import Icon from '../Icon'

export default function FormularioPlan({ onGuardar }) {
  const [nombre, setNombre] = useState('')
  const [duracionMeses, setDuracionMeses] = useState('')
  const [error, setError] = useState('')

  function enviar(event) {
    event.preventDefault()
    const nombreLimpio = nombre.trim()
    const duracion = Number(duracionMeses)
    if (!nombreLimpio) { setError('Escribe el nombre del plan.'); return }
    if (!Number.isInteger(duracion) || duracion < 1 || duracion > 36) {
      setError('La duración debe ser de 1 a 36 meses.')
      return
    }
    const resultado = onGuardar({ nombre: nombreLimpio, duracionMeses: duracion })
    if (resultado) { setError(resultado); return }
    setNombre('')
    setDuracionMeses('')
    setError('')
  }

  return <form className="practice-plan-form" onSubmit={enviar}>
    <h2>Agregar un plan</h2>
    <div className="practice-plan-form-fields">
      <label>Nombre del plan<input value={nombre} onChange={event => { setNombre(event.target.value); setError('') }} maxLength={80} placeholder="Ej. Plan mensual" /></label>
      <label>Duración (meses)<input type="number" min="1" max="36" step="1" value={duracionMeses} onChange={event => { setDuracionMeses(event.target.value); setError('') }} placeholder="Ej. 1" /></label>
      <button type="submit"><Icon name="plus" />Guardar plan</button>
    </div>
    {error && <p role="alert">{error}</p>}
  </form>
}
