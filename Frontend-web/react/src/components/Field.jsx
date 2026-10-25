import { useState } from 'react'

export default function Field({ label, name, error, hint, type = 'text', ...props }) {
  const [visible, setVisible] = useState(false)
  const password = type === 'password'
  return <div className="field">
    <label htmlFor={name}>{label}</label>
    <div className={`input-wrap ${error ? 'invalid' : ''}`}>
      {type !== 'date' && <span className="field-icon" aria-hidden="true"><img src={`/assets/${password ? 'lock' : type === 'email' ? 'mail' : 'user'}.svg`} alt="" /></span>}
      <input id={name} name={name} type={password && visible ? 'text' : type}
        aria-invalid={!!error} aria-describedby={error ? `${name}-error` : hint ? `${name}-hint` : undefined}
        {...props} />
      {password && <button type="button" className="visibility" onClick={() => setVisible(!visible)}
        aria-label={`${visible ? 'Ocultar' : 'Mostrar'} ${label.toLowerCase()}`} aria-pressed={visible}>
        <img src="/assets/eye.svg" alt="" />
      </button>}
    </div>
    {hint && <small id={`${name}-hint`}>{hint}</small>}
    {error && <small className="field-error" id={`${name}-error`}>{error}</small>}
  </div>
}
