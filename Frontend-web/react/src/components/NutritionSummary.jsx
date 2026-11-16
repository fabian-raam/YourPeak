import './NutritionSummary.css'

const number = new Intl.NumberFormat('es-PE', { maximumFractionDigits: 1 })
const macros = [
  { key: 'proteinaG', label: 'Proteínas', energy: 4, color: 'var(--protein)' },
  { key: 'carbohidratoG', label: 'Carbohidratos', energy: 4, color: 'var(--carbs)' },
  { key: 'grasaG', label: 'Grasas', energy: 9, color: 'var(--fats)' },
]

export default function NutritionSummary({ summary, id, title, description, children }) {
  const meta = summary?.meta
  const totals = macros.map(macro => Number(summary?.[macro.key] ?? 0) * macro.energy)
  const macroEnergy = totals.reduce((sum, value) => sum + value, 0)
  const kcal = Number(summary?.kcal ?? 0)
  const progress = meta?.kcal > 0 ? Math.min(kcal / meta.kcal, 1) : 1
  const stops = totals.reduce((positions, value) => {
    const length = macroEnergy ? value / macroEnergy * 100 * progress : 0
    return [...positions, Math.min(100, positions.at(-1) + length)]
  }, [0])
  const segments = macros.map((macro, index) => `${macro.color} ${stops[index]}% ${stops[index + 1]}%`)

  return <section className="client-card nutrition-overview" aria-labelledby={id}>
    {summary && <div className="client-nutrition-ring" style={{ '--ring': `conic-gradient(${segments.join(', ')}, #e2e7f0 ${stops.at(-1)}% 100%)` }}>
      <div><strong>{number.format(kcal)}</strong><span>kcal{meta?.kcal > 0 ? ` · de ${number.format(meta.kcal)}` : ''}</span></div>
    </div>}
    <div className="client-nutrition-breakdown">
      <div className="nutrition-overview-heading"><h2 id={id}>{title}</h2>{description && <p>{description}</p>}</div>
      {summary ? <>
        <dl className="nutrition-overview-macros">{macros.map((macro, index) => {
          const amount = Number(summary[macro.key] ?? 0)
          const goal = Number(meta?.[macro.key] ?? 0)
          const fraction = goal > 0 ? amount / goal : macroEnergy ? totals[index] / macroEnergy : 0
          return <div key={macro.key} style={{ '--macro-color': macro.color }}>
            <dt>{macro.label}</dt>
            <dd>
              <span><strong>{number.format(amount)}</strong>{goal > 0 ? ` / ${number.format(goal)}` : ''} g</span>
              <span className="nutrition-overview-track" aria-hidden="true"><span style={{ width: `${Math.min(fraction, 1) * 100}%` }} /></span>
            </dd>
          </div>
        })}</dl>
      </> : null}
      {children}
    </div>
  </section>
}
