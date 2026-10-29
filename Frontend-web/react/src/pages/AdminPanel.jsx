import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import Icon from '../components/Icon'
import ApprovalDialog from '../components/ApprovalDialog'
import { listPending } from '../services/admin'
import { fullName, initials, registrationDate } from '../utils/format'
import './AdminPanel.css'

const PAGE_SIZE = 8
const normalize = value => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()

export default function AdminPanel({ onExpired, logoutError, onPendingCount }) {
  const [clients, setClients] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [query, setQuery] = useState('')
  const [sort, setSort] = useState('oldest')
  const [page, setPage] = useState(1)
  const [selected, setSelected] = useState(null)
  const [lastUpdated, setLastUpdated] = useState(null)
  const [forbidden, setForbidden] = useState(false)
  const [revision, setRevision] = useState(0)
  const sequence = useRef(0)
  const lastTrigger = useRef(null)

  const reload = useCallback(() => {
    setLoading(true); setError('')
    setRevision(previous => previous + 1)
  }, [])

  // Descarta respuestas antiguas para que una recarga no reponga filas ya aprobadas.
  useEffect(() => {
    const current = ++sequence.current
    listPending().then(data => {
      if (sequence.current !== current) return
      setClients(data); onPendingCount(data.length); setLastUpdated(new Date()); setForbidden(false)
    }).catch(err => {
      if (sequence.current !== current) return
      if (err.status === 401) { onExpired(); return }
      if (err.status === 403) { setForbidden(true); setClients([]) }
      setError(err.message)
    }).finally(() => { if (sequence.current === current) setLoading(false) })
    return () => { sequence.current += 1 }
  }, [onExpired, revision, onPendingCount])

  const filtered = useMemo(() => {
    const search = normalize(query.trim())
    return clients.filter(person => normalize(`${fullName(person)} ${person.correo} ${person.idCliente}`).includes(search))
      .sort((a, b) => sort === 'name' ? fullName(a).localeCompare(fullName(b), 'es')
        : (sort === 'newest' ? -1 : 1) * String(a.fechaAlta || '').localeCompare(String(b.fechaAlta || '')))
  }, [clients, query, sort])
  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  const currentPage = Math.min(page, totalPages)
  const visible = filtered.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE)

  function closeDetail() {
    setSelected(null)
    // La fila puede desaparecer al aprobar; entonces devuelve el foco al título.
    requestAnimationFrame(() => {
      if (lastTrigger.current?.isConnected) lastTrigger.current.focus()
      else document.getElementById('users-heading')?.focus()
    })
  }

  function approved(person) {
    sequence.current += 1
    setClients(previous => previous.filter(item => item.idCliente !== person.idCliente))
    setNotice(`${fullName(person)} fue aprobado. Su cuenta ya está activa.`)
    closeDetail()
    reload()
  }

  return <>
      <main className="admin-main">
        <div className="admin-heading"><div><span className="eyebrow">CADA NUEVA META EMPIEZA CON UNA OPORTUNIDAD</span><h1>Tu comunidad.<br className="mobile-break" /> <span>Su próximo paso.</span></h1><p>Revisa los registros y da la bienvenida a los próximos miembros de Your Peak.</p></div>
          <button className="secondary-button" onClick={reload} disabled={loading}><Icon name="refresh" />{loading ? 'Actualizando…' : 'Actualizar'}</button>
        </div>
        <section className="admin-stats" aria-label="Resumen de solicitudes">
          <article><span className="stat-icon accent"><Icon name="users" /></span><div><span>Solicitudes pendientes</span><strong>{loading ? '…' : clients.length}</strong><small>Esperan una revisión</small></div></article>
          <article><span className="stat-icon amber"><Icon name="clock" /></span><div><span>Primera solicitud pendiente</span><strong className="stat-date">{loading ? '…' : clients.length ? registrationDate([...clients].sort((a, b) => String(a.fechaAlta).localeCompare(String(b.fechaAlta)))[0].fechaAlta) : 'Sin pendientes'}</strong><small>Atiende primero a quienes llegaron antes</small></div></article>
          <article><span className="stat-icon green"><Icon name="shield" /></span><div><span>Comunidad institucional</span><strong className="stat-domain">@tecsup.edu.pe</strong><small>El acceso comienza con una aprobación</small></div></article>
        </section>
        {notice && <p className="alert success" role="status">{notice}</p>}
        {(error || logoutError) && <p className="alert" role="alert">{error || logoutError}{!forbidden && <button className="text-button" onClick={reload} disabled={loading}>Volver a intentar</button>}</p>}
        <section className="users-panel" aria-labelledby="users-heading" aria-busy={loading}>
          <div className="users-panel-heading"><div><h2 id="users-heading" tabIndex="-1">Aprobación de usuarios <span>{clients.length}</span></h2><p>Confirma sus datos antes de activar su cuenta.</p></div><span className="live-label"><i /> Pendientes de aprobación</span></div>
          <div className="users-toolbar"><label className="search-input"><Icon name="search" /><input type="search" aria-label="Buscar solicitudes" placeholder="Buscar por nombre, correo o número…" value={query} onChange={event => { setQuery(event.target.value); setPage(1) }} /></label>
            <label className="sort-control">Ordenar por <select value={sort} onChange={event => { setSort(event.target.value); setPage(1) }}><option value="oldest">Más antiguos</option><option value="newest">Más recientes</option><option value="name">Nombre A–Z</option></select></label>
          </div>
          {loading ? <div className="table-state" role="status"><span className="loading-dot" /> Consultando solicitudes…</div>
            : forbidden ? <div className="table-state"><Icon name="shield" /><h3>Acceso no disponible</h3><p>Tu cuenta no tiene permisos para consultar estas solicitudes.</p></div>
              : !visible.length ? <div className="table-state"><Icon name={query ? 'search' : 'check'} /><h3>{query ? 'No encontramos coincidencias' : error ? 'No pudimos cargar las solicitudes' : 'Todo al día'}</h3><p>{query ? 'Prueba con otro nombre o correo institucional.' : error ? 'Actualiza la lista para volver a intentarlo.' : 'Las nuevas solicitudes aparecerán aquí para que puedas revisarlas.'}</p></div>
                : <div className="users-table-scroll"><table className="users-table"><caption className="sr-only">Usuarios pendientes de aprobación</caption><thead><tr><th scope="col">Usuario</th><th scope="col">Registro</th><th scope="col">Estado</th><th scope="col"><span className="sr-only">Acciones</span></th></tr></thead><tbody>
                  {visible.map(person => <tr key={person.idCliente}><td><div className="user-cell"><span className="avatar">{initials(person)}</span><div><strong>{fullName(person)}</strong><span>{person.correo}</span></div></div></td><td><span>{registrationDate(person.fechaAlta)}</span><small>Solicitud #{person.idCliente}</small></td><td><span className="status-badge"><i /> Pendiente</span></td><td><button className="review-button" aria-label={`Revisar a ${fullName(person)}`} onClick={event => { lastTrigger.current = event.currentTarget; setSelected(person.idCliente) }}>Revisar <Icon name="arrow" /></button></td></tr>)}
                </tbody></table></div>}
          <div className="users-pagination"><span>{loading ? 'Actualizando lista' : `${filtered.length} ${filtered.length === 1 ? 'solicitud' : 'solicitudes'}${query ? ' encontradas' : ' pendientes'}`}</span><div><button aria-label="Página anterior" disabled={currentPage <= 1 || loading} onClick={() => setPage(currentPage - 1)}>←</button><span>{currentPage} / {totalPages}</span><button aria-label="Página siguiente" disabled={currentPage >= totalPages || loading} onClick={() => setPage(currentPage + 1)}>→</button></div></div>
        </section>
        <footer className="admin-footer"><span>YOUR PEAK <b>·</b> Cada aprobación abre una nueva posibilidad.</span><span>{lastUpdated ? `Actualizado a las ${lastUpdated.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' })}` : 'Esperando actualización'}</span></footer>
      </main>
    {selected !== null && <ApprovalDialog key={selected} clientId={selected} onClose={closeDetail} onApproved={approved} onExpired={onExpired} onStale={reload} />}
  </>
}
