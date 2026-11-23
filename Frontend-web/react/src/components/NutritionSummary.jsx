import './NutritionSummary.css'

const number = new Intl.NumberFormat('es-PE', { maximumFractionDigits: 1 })
const macros = [
  { key: 'proteinaG', label: 'Proteínas', color: 'var(--protein)' },
  { key: 'carbohidratoG', label: 'Carbohidratos', color: 'var(--carbs)' },
  { key: 'grasaG', label: 'Grasas', color: 'var(--fats)' },
]

export default function NutritionSummary({ summary, id, title, description, daily = false, children }) {
  const meta = summary?.meta
  const kcal = Number(summary?.kcal ?? 0)
  const hasCalorieGoal = meta?.kcal > 0
  const progress = hasCalorieGoal ? Math.min(kcal / meta.kcal, 1) : 0
  const trackColor = daily ? '#283a54' : '#e2e7f0'
  const ring = `conic-gradient(#5591ff ${progress * 100}%, ${trackColor} 0)`

  return <section className={`client-card nutrition-overview${daily ? ' nutrition-overview-daily' : ''}`} aria-labelledby={id}>
    {summary && (!daily || hasCalorieGoal) && <div className="client-nutrition-ring" style={{ '--ring': ring }}>
      {daily ? <div><strong>{number.format(Math.round(progress * 100))} %</strong></div>
        : <div><strong>{number.format(kcal)}</strong><span>kcal hoy</span></div>}
    </div>}
    <div className="client-nutrition-breakdown">
      <div className="nutrition-overview-heading">
        <h2 id={id}>{title}</h2>
        {daily && summary && <p className="nutrition-daily-kcal"><strong>{number.format(kcal)}</strong>
          <span>{meta?.kcal > 0 ? `/ ${number.format(meta.kcal)} kcal` : 'kcal'}</span>
        </p>}
        {description && <p className={daily ? 'nutrition-daily-goal' : undefined}>{description}</p>}
      </div>
      {summary ? <>
        <dl className="nutrition-overview-macros">{macros.map(macro => {
          const amount = Number(summary[macro.key] ?? 0)
          const goal = Number(meta?.[macro.key] ?? 0)
          return <div key={macro.key} style={{ '--macro-color': macro.color }}>
            <dt>{macro.label}</dt>
            <dd>
              <span><strong>{number.format(amount)}</strong>{goal > 0 ? ` / ${number.format(goal)}` : ''} g</span>
              {goal > 0 && <span className="nutrition-overview-track" aria-hidden="true"><span style={{ width: `${Math.min(amount / goal, 1) * 100}%` }} /></span>}
            </dd>
          </div>
        })}</dl>
      </> : null}
      {children}
    </div>
  </section>
}
