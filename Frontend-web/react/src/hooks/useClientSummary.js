import { useEffect, useState } from 'react'

export default function useClientSummary(load, onExpired) {
  const [result, setResult] = useState({})
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let active = true
    load().then(data => {
      if (active) setResult({ data })
    }).catch(error => {
      if (!active) return
      if (error.status === 401) onExpired()
      else setResult({ error: true })
    })
    return () => { active = false }
  }, [load, onExpired, attempt])

  function retry() {
    setResult({})
    setAttempt(current => current + 1)
  }

  return { ...result, retry }
}
