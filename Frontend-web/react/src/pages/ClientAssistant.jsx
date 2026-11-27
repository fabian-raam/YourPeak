import { useEffect, useRef, useState } from 'react'
import Icon from '../components/Icon'
import { askAssistant } from '../services/assistant'
import './ClientAssistant.css'

const STORAGE_KEY = 'yp-asistente'
const MAX_QUESTION = 500
const suggestions = [
  { icon: 'food', text: '¿Qué puedo comer antes de entrenar?' },
  { icon: 'food', text: 'Dame ideas de cenas altas en proteína' },
  { icon: 'dumbbell', text: '¿Cómo hago una sentadilla con buena técnica?' },
  { icon: 'activity', text: '¿Cuánto debo descansar entre series?' },
  { icon: 'food', text: '¿Cuánta agua debería tomar al día?' },
  { icon: 'dumbbell', text: 'Ejercicios de calentamiento para piernas' },
]

// La conversación solo vive en esta pestaña; si el almacenamiento falla, empieza vacía.
function loadMessages() {
  try {
    const saved = JSON.parse(sessionStorage.getItem(STORAGE_KEY) || '[]')
    return Array.isArray(saved) ? saved.filter(item => item && typeof item.text === 'string') : []
  } catch {
    return []
  }
}

function saveMessages(messages) {
  try { sessionStorage.setItem(STORAGE_KEY, JSON.stringify(messages.slice(-40))) } catch { /* sin almacenamiento */ }
}

/** Párrafos y listas con guion del texto plano que devuelve el asistente. */
function FormattedText({ text }) {
  const blocks = []
  text.split('\n').forEach(raw => {
    const line = raw.trim()
    if (!line) { blocks.push(null); return }
    const item = line.match(/^[-•*]\s+(.*)$/)
    const last = blocks[blocks.length - 1]
    if (item) {
      if (Array.isArray(last)) last.push(item[1])
      else blocks.push([item[1]])
    } else if (typeof last === 'string') {
      blocks[blocks.length - 1] = `${last} ${line}`
    } else {
      blocks.push(line)
    }
  })
  return blocks.filter(block => block !== null).map((block, index) => Array.isArray(block)
    ? <ul key={index}>{block.map((entry, i) => <li key={i}>{entry}</li>)}</ul>
    : <p key={index}>{block}</p>)
}

export default function ClientAssistant({ user, onExpired }) {
  const [messages, setMessages] = useState(loadMessages)
  const [question, setQuestion] = useState('')
  const [sending, setSending] = useState(false)
  const [remaining, setRemaining] = useState(null)
  const list = useRef(null)
  const input = useRef(null)

  useEffect(() => { saveMessages(messages) }, [messages])
  useEffect(() => {
    list.current?.scrollTo({ top: list.current.scrollHeight, behavior: 'smooth' })
  }, [messages, sending])

  async function send(text, base = messages) {
    const pregunta = text.trim()
    if (!pregunta || sending) return
    // Solo los pares pregunta-respuesta válidos forman parte del contexto que se envía.
    const historial = base.filter((item, i) => item.role === 'user'
      ? base[i + 1]?.role === 'assistant' && !base[i + 1].error && base[i + 1].onTopic !== false
      : !item.error && item.onTopic !== false)
      .slice(-12).map(item => ({ rol: item.role === 'user' ? 'usuario' : 'asistente', texto: item.text }))
    setMessages([...base, { role: 'user', text: pregunta }])
    setQuestion('')
    setSending(true)
    try {
      const data = await askAssistant(pregunta, historial)
      setRemaining({ left: data.consultasRestantesHoy, limit: data.limiteDiario })
      setMessages(previous => [...previous, { role: 'assistant', text: data.respuesta, onTopic: data.enTema }])
    } catch (err) {
      if (err.status === 401) { onExpired(); return }
      setMessages(previous => [...previous, { role: 'assistant', text: err.message, error: true, retry: pregunta }])
    } finally {
      setSending(false)
      input.current?.focus()
    }
  }

  // Quita la pregunta fallida y su error, y la vuelve a enviar.
  function retry(index) {
    send(messages[index].retry, messages.filter((_, i) => i !== index && i !== index - 1))
  }

  function reset() {
    setMessages([])
    setQuestion('')
    input.current?.focus()
  }

  const empty = messages.length === 0

  return <div className="ca-page">
    <section className="ca-chat" aria-label="Conversación con el asistente">
      <header className="ca-head">
        <span className="ca-avatar" aria-hidden="true"><Icon name="sparkles" /></span>
        <div>
          <h2>Coach Your Peak</h2>
          <p><span className="ca-dot" aria-hidden="true" /> Alimentación y ejercicio</p>
        </div>
        {!empty && <button type="button" className="ca-reset" onClick={reset} disabled={sending} aria-label="Nueva conversación">
          <Icon name="refresh" /> <span>Nueva conversación</span>
        </button>}
      </header>

      <div className="ca-messages" ref={list} aria-live="polite">
        {empty ? <div className="ca-welcome">
          <span className="ca-welcome-icon" aria-hidden="true"><Icon name="sparkles" /></span>
          <h3>Hola{user?.nombre ? `, ${user.nombre}` : ''}. ¿En qué te ayudo hoy?</h3>
          <p>Pregúntame sobre comidas, macronutrientes, hidratación, técnica de ejercicios, rutinas o recuperación.
            Uso tu perfil para darte respuestas a tu medida.</p>
          <div className="ca-suggestions">
            {suggestions.map(item => <button type="button" key={item.text} onClick={() => send(item.text)} disabled={sending}>
              <Icon name={item.icon} /> {item.text}
            </button>)}
          </div>
        </div> : messages.map((message, index) => <div key={index}
          className={`ca-msg ${message.role === 'user' ? 'is-user' : 'is-bot'}${message.error ? ' is-error' : ''}${message.onTopic === false ? ' is-off' : ''}`}>
          {message.role !== 'user' && <span className="ca-msg-avatar" aria-hidden="true"><Icon name={message.error ? 'info' : 'sparkles'} /></span>}
          <div className="ca-bubble">
            {message.role === 'user' ? <p>{message.text}</p> : <FormattedText text={message.text} />}
            {message.error && message.retry && <button type="button" className="ca-retry" onClick={() => retry(index)} disabled={sending}>
              <Icon name="refresh" /> Reintentar
            </button>}
          </div>
        </div>)}
        {sending && <div className="ca-msg is-bot">
          <span className="ca-msg-avatar" aria-hidden="true"><Icon name="sparkles" /></span>
          <div className="ca-bubble ca-typing" role="status" aria-label="El asistente está escribiendo"><i /><i /><i /></div>
        </div>}
      </div>

      <form className="ca-form" onSubmit={event => { event.preventDefault(); send(question) }}>
        <label htmlFor="ca-input" className="sr-only">Tu pregunta</label>
        <textarea id="ca-input" ref={input} rows={1} maxLength={MAX_QUESTION} value={question} disabled={sending}
          placeholder="Escribe tu pregunta sobre alimentación o ejercicio…"
          onChange={event => setQuestion(event.target.value)}
          onKeyDown={event => {
            if (event.key === 'Enter' && !event.shiftKey && !event.nativeEvent.isComposing) {
              event.preventDefault(); send(question)
            }
          }} />
        <button type="submit" className="ca-send" disabled={sending || !question.trim()} aria-label="Enviar pregunta">
          <Icon name="arrow" />
        </button>
      </form>
      <p className="ca-foot">
        {remaining ? `Te quedan ${remaining.left} de ${remaining.limit} consultas hoy · ` : ''}
        Orientación general: no reemplaza a un médico ni a un nutricionista.
      </p>
    </section>

    <aside className="ca-side">
      <section className="client-card ca-card">
        <h3>Qué puedo responder</h3>
        <ul className="ca-topics">
          <li><Icon name="food" /><div><strong>Alimentación</strong><small>Comidas, porciones, proteínas, hidratación y suplementos comunes.</small></div></li>
          <li><Icon name="dumbbell" /><div><strong>Ejercicio</strong><small>Técnica, rutinas, calentamiento, cardio y descanso.</small></div></li>
          <li><Icon name="shield" /><div><strong>Con cuidado</strong><small>Ante dolor, lesiones o condiciones médicas te recomendaré a un profesional.</small></div></li>
        </ul>
      </section>
      <section className="client-card ca-card ca-card-dark">
        <h3>Solo temas del gimnasio</h3>
        <p>Si preguntas por otros temas, el asistente te recordará que solo ayuda con alimentación y ejercicio.</p>
      </section>
    </aside>
  </div>
}
