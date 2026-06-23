import { useEffect, useMemo, useState } from 'react'
import { Navigate, Route, Routes, useNavigate, useParams } from 'react-router-dom'
import { getMeRequest, loginRequest, logoutRequest } from './api/auth'
import {
  createProcess as createProcessRequest,
  getDashboard,
  getHistory,
  getProcessById,
  getProcessReasons,
  getProcesses,
  updateProcess as updateProcessRequest,
} from './api/processes'
import { ROLES } from './constants/roles'
import AdminUsers from './pages/AdminUsers'
import ProtectedRoute from './routes/ProtectedRoute'
import { useAuthStore } from './store/authStore'
import foxLogo from './assets/fox-logo.jpg'
import './App.css'

const statusOptions = ['OPEN', 'IN_PROGRESS', 'PENDING', 'CLOSED', 'CANCELLED']
const processStages = [
  'Avisar sinistro',
  'Vistoria',
  'Documentacao',
  'Regulacao',
  'Analise',
  'Pagamento',
]
const fallbackReasons = [
  { id: '', name: 'Avaria de transporte' },
  { id: '', name: 'Falta de documento' },
  { id: '', name: 'Ressarcimento' },
]

const emptyDashboard = {
  total: 0,
  inProgress: 0,
  pending: 0,
  estimatedValue: 0,
}

function App() {
  const accessToken = useAuthStore((state) => state.accessToken)
  const refreshToken = useAuthStore((state) => state.refreshToken)
  const setAuth = useAuthStore((state) => state.setAuth)
  const clearAuth = useAuthStore((state) => state.clearAuth)
  const finishRestoring = useAuthStore((state) => state.finishRestoring)

  useEffect(() => {
    async function restoreSession() {
      if (!accessToken) {
        finishRestoring()
        return
      }

      try {
        const { data } = await getMeRequest()
        setAuth({ user: data, accessToken, refreshToken })
      } catch {
        clearAuth()
      }
    }

    restoreSession()
  }, [accessToken, refreshToken, setAuth, clearAuth, finishRestoring])

  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/" element={<WorkspaceApp />} />
        <Route path="/processes/:processId" element={<WorkspaceApp />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  )
}

function WorkspaceApp() {
  const { processId } = useParams()
  const navigate = useNavigate()
  const [view, setView] = useState(processId ? 'editar' : 'cockpit')
  const [processos, setProcessos] = useState([])
  const [dashboard, setDashboard] = useState(emptyDashboard)
  const [reasons, setReasons] = useState(fallbackReasons)
  const [selectedProcess, setSelectedProcess] = useState(null)
  const [history, setHistory] = useState([])
  const [loadingList, setLoadingList] = useState(true)
  const [loadingProcess, setLoadingProcess] = useState(false)
  const [processNotFound, setProcessNotFound] = useState(false)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const refreshToken = useAuthStore((state) => state.refreshToken)
  const logout = useAuthStore((state) => state.logout)
  const currentUser = useAuthStore((state) => state.user)
  const canAccessAdmin = currentUser?.roles?.includes(ROLES.ADMIN) || false
  const [filters, setFilters] = useState({
    termo: '',
    operacao: 'Todas',
    seguradora: 'Todas',
    status: 'Todos',
  })

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      loadCockpit()
    }, 0)

    return () => window.clearTimeout(timeoutId)
  }, [])

  useEffect(() => {
    if (!processId) return

    const timeoutId = window.setTimeout(() => {
      setView('editar')
      loadSelectedProcess(processId)
    }, 0)

    return () => window.clearTimeout(timeoutId)
  }, [processId])

  const filtered = useMemo(() => {
    const termo = filters.termo.toLowerCase()
    return processos.filter((processo) => {
      const searchable = [
        processo.processNumber,
        processo.insuranceClaimNumber,
        processo.clientName,
        processo.dealershipName,
        processo.currentReasonName,
        processo.invoiceNumber,
        processo.chassis,
      ]
        .join(' ')
        .toLowerCase()

      return (
        searchable.includes(termo) &&
        (filters.operacao === 'Todas' || processo.operationType === filters.operacao) &&
        (filters.seguradora === 'Todas' || processo.insuranceCompany === filters.seguradora) &&
        (filters.status === 'Todos' || processo.status === filters.status)
      )
    })
  }, [filters, processos])

  async function loadCockpit() {
    setLoadingList(true)
    setError('')

    try {
      const [processPage, nextDashboard, nextReasons] = await Promise.all([
        getProcesses({ page: 0, size: 100 }),
        getDashboard(),
        getProcessReasons().catch(() => fallbackReasons),
      ])
      setProcessos(processPage.content || [])
      setDashboard(nextDashboard)
      setReasons(nextReasons.length ? nextReasons : fallbackReasons)
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Nao foi possivel carregar os processos.')
    } finally {
      setLoadingList(false)
    }
  }

  async function loadSelectedProcess(id) {
    setLoadingProcess(true)
    setError('')

    try {
      const [processData, historyData] = await Promise.all([getProcessById(id), getHistory(id)])
      setSelectedProcess(processData)
      setHistory(historyData)
      setProcessNotFound(false)
    } catch (requestError) {
      setSelectedProcess(null)
      setHistory([])
      setProcessNotFound(true)
      if (requestError.response?.status !== 404) {
        setError(requestError.response?.data?.message || 'Nao foi possivel carregar o processo.')
      }
    } finally {
      setLoadingProcess(false)
    }
  }

  function navigateTo(nextView) {
    setMessage('')
    setError('')

    if (nextView === 'cockpit') {
      setView('cockpit')
      navigate('/')
      loadCockpit()
      return
    }

    setView(nextView)
    navigate('/')
  }

  function openProcess(id) {
    navigate(`/processes/${id}`)
  }

  async function createProcess(payload) {
    setSaving(true)
    setError('')
    setMessage('')

    try {
      const createdProcess = await createProcessRequest(toProcessPayload(payload))
      setMessage('Processo criado com sucesso.')
      await loadCockpit()
      navigate(`/processes/${createdProcess.id}`)
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Nao foi possivel criar o processo.')
    } finally {
      setSaving(false)
    }
  }

  async function saveProcess(draft) {
    if (!selectedProcess?.id) return

    setSaving(true)
    setError('')
    setMessage('')

    try {
      const updatedProcess = await updateProcessRequest(selectedProcess.id, toProcessPayload(draft))
      setSelectedProcess(mergeProcessDraft(updatedProcess, draft))
      setHistory(await getHistory(updatedProcess.id))
      setMessage('Processo atualizado com sucesso.')
      await loadCockpit()
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Nao foi possivel salvar o processo.')
    } finally {
      setSaving(false)
    }
  }

  async function handleLogout() {
    try {
      if (refreshToken) {
        await logoutRequest(refreshToken)
      }
    } catch (logoutError) {
      void logoutError
    } finally {
      logout()
    }
  }

  return (
    <div className="app-shell">
      <Sidebar
        view={view}
        user={currentUser}
        onNavigate={navigateTo}
        onLogout={handleLogout}
        canAccessAdmin={canAccessAdmin}
      />
      <main className="workspace">
        {message && <div className="form-success">{message}</div>}
        {error && <div className="form-error">{error}</div>}

        {view === 'cockpit' && (
          <Cockpit
            processos={filtered}
            filters={filters}
            dashboard={dashboard}
            loading={loadingList}
            onFilter={setFilters}
            onOpen={openProcess}
            onRefresh={loadCockpit}
          />
        )}
        {view === 'abrir' && <Abertura reasons={reasons} onCreate={createProcess} saving={saving} />}
        {view === 'editar' && (
          <Edicao
            key={selectedProcess?.id || processId || 'empty'}
            processo={selectedProcess}
            reasons={reasons}
            history={history}
            loading={loadingProcess}
            notFound={processNotFound}
            saving={saving}
            onSave={saveProcess}
          />
        )}
        {view === 'usuarios' && canAccessAdmin && <AdminUsers />}
        {view === 'usuarios' && !canAccessAdmin && (
          <Cockpit
            processos={filtered}
            filters={filters}
            dashboard={dashboard}
            loading={loadingList}
            onFilter={setFilters}
            onOpen={openProcess}
            onRefresh={loadCockpit}
          />
        )}
      </main>
    </div>
  )
}

function Login() {
  const navigate = useNavigate()
  const setAuth = useAuthStore((state) => state.setAuth)
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [credentials, setCredentials] = useState({
    email: '',
    password: '',
  })

  if (isAuthenticated) {
    return <Navigate to="/" replace />
  }

  async function submit(event) {
    event.preventDefault()
    setLoading(true)
    setError('')

    try {
      const { data } = await loginRequest(credentials)
      setAuth(data)
      navigate('/', { replace: true })
    } catch (loginError) {
      const message = loginError.response?.data?.message || 'Nao foi possivel entrar. Confira email e senha.'
      setError(message)
    } finally {
      setLoading(false)
    }
  }

  function update(field, value) {
    setCredentials((current) => ({ ...current, [field]: value }))
  }

  return (
    <main className="login-page">
      <section className="login-panel">
        <div className="brand-lockup">
          <img className="brand-mark" src={foxLogo} alt="FOX" />
          <div>
            <strong>FOX Sinistros</strong>
            <span>Portal FOX</span>
          </div>
        </div>
        <form onSubmit={submit} className="login-form">
          <h1>Faca seu login</h1>
          <p>Digite login e senha.</p>
          {error && <div className="form-error">{error}</div>}
          <label>
            Usuario
            <input
              name="email"
              type="email"
              autoComplete="username"
              value={credentials.email}
              onChange={(event) => update('email', event.target.value)}
            />
          </label>
          <label>
            Senha
            <input
              name="password"
              type="password"
              autoComplete="current-password"
              value={credentials.password}
              onChange={(event) => update('password', event.target.value)}
            />
          </label>
          <button className="primary-action" type="submit" disabled={loading}>
            {loading ? 'Carregando...' : 'Entrar'}
          </button>
          <button className="link-action" type="button">
            Esqueci a senha
          </button>
        </form>
      </section>
      <aside className="login-context">
        <p>2026</p>
        <h2>FOX moderna para abertura, cockpit e edicao de processos.</h2>
      </aside>
    </main>
  )
}

function Sidebar({ view, user, onNavigate, onLogout, canAccessAdmin }) {
  const items = [
    { id: 'cockpit', label: 'Cockpit', marker: 'CO' },
    { id: 'abrir', label: 'Abrir processo', marker: 'AB' },
  ]
  const menuItems = canAccessAdmin ? [...items, { id: 'usuarios', label: 'Usuarios', marker: 'US' }] : items

  return (
    <aside className="sidebar">
      <button className="brand-button" type="button" onClick={() => onNavigate('cockpit')}>
        <img className="brand-logo" src={foxLogo} alt="FOX" />
        <strong>
          FOX Sinistros
          <small>Administracao</small>
        </strong>
      </button>

      <nav aria-label="Menu principal">
        {menuItems.map((item) => (
          <button
            type="button"
            key={item.id}
            className={view === item.id ? 'active' : ''}
            onClick={() => onNavigate(item.id)}
          >
            <span>{item.marker}</span>
            {item.label}
          </button>
        ))}
      </nav>

      <div className="sidebar-footer">
        <div className="user-chip">
          <span>{getUserInitial(user?.name || user?.email)}</span>
          <strong>
            {user?.name || 'Usuario'}
            <small>{user?.email}</small>
          </strong>
        </div>
        <button className="ghost-action" type="button" onClick={onLogout}>
          Sair
        </button>
      </div>
    </aside>
  )
}

function Cockpit({ processos, filters, dashboard, loading, onFilter, onOpen, onRefresh }) {
  return (
    <section className="screen">
      <div className="screen-heading">
        <div>
          <span className="eyebrow">Cockpit Principal Montador</span>
          <h1>Processos</h1>
        </div>
        <button className="ghost-action" type="button" onClick={onRefresh} disabled={loading}>
          Atualizar
        </button>
      </div>

      <div className="metric-grid">
        <Metric label="Registros" value={loading ? '...' : dashboard.total} />
        <Metric label="Em andamento" value={loading ? '...' : dashboard.inProgress} tone="blue" />
        <Metric label="Pendentes" value={loading ? '...' : dashboard.pending} tone="amber" />
        <Metric label="Valor estimado" value={loading ? '...' : money(dashboard.estimatedValue)} tone="green" />
      </div>

      <div className="filter-bar">
        <label>
          Numero, NF ou chassi
          <input
            value={filters.termo}
            onChange={(event) => onFilter({ ...filters, termo: event.target.value })}
            placeholder="DC-2026-000001"
          />
        </label>
        <Select label="Operacao" value={filters.operacao} options={['Todas', 'Montador', 'Embarcador']} onChange={(value) => onFilter({ ...filters, operacao: value })} />
        <Select label="Seguradora" value={filters.seguradora} options={['Todas', 'SURA', 'SOMPO']} onChange={(value) => onFilter({ ...filters, seguradora: value })} />
        <Select label="Status" value={filters.status} options={['Todos', ...statusOptions]} onChange={(value) => onFilter({ ...filters, status: value })} />
      </div>

      <div className="table-panel">
        <div className="table-title">
          <strong>{loading ? 'Carregando processos' : `${processos.length} registros encontrados`}</strong>
          <span>Dados reais do backend</span>
        </div>
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th></th>
                <th>Processo</th>
                <th>Sinistro Seguradora</th>
                <th>Cliente</th>
                <th>Motivo</th>
                <th>Data do Aviso</th>
                <th>Ultima modificacao</th>
                <th>Etapa</th>
                <th>Valor</th>
              </tr>
            </thead>
            <tbody>
              {loading && <SkeletonRows columns={9} rows={4} />}
              {!loading && processos.length === 0 && (
                <tr>
                  <td colSpan="9">
                    <div className="empty-state">Nenhum processo encontrado.</div>
                  </td>
                </tr>
              )}
              {!loading && processos.map((processo) => (
                <tr key={processo.id}>
                  <td>
                    <button className="icon-button" type="button" onClick={() => onOpen(processo.id)}>
                      Ver
                    </button>
                  </td>
                  <td>
                    <strong>{processo.processNumber}</strong>
                    <span>{processo.dealershipName || '-'}</span>
                  </td>
                  <td>{processo.insuranceClaimNumber || 'Nao integrado'}</td>
                  <td>{processo.clientName || '-'}</td>
                  <td>{processo.currentReasonName || '-'}</td>
                  <td>{dateBr(processo.claimDate)}</td>
                  <td>{dateBr(processo.updatedAt)}</td>
                  <td>
                    <StatusPill status={processo.currentStageName || processo.status} />
                  </td>
                  <td>{money(processo.estimatedValue)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  )
}

function Abertura({ reasons, onCreate, saving }) {
  const [form, setForm] = useState({
    cliente: 'AGCO',
    seguradora: 'SURA',
    operacao: 'Montador',
    concessionaria: 'AGCO Center Campinas',
    motivoId: '',
    motivo: '',
    dataAviso: '',
    dataOcorrencia: '',
    chassi: '',
    notaFiscal: '',
    oss: '',
    transportadora: '',
    cidade: '',
    uf: 'SP',
    valorEstimado: 0,
    descricao: '',
  })

  function update(field, value) {
    setForm((current) => ({ ...current, [field]: value }))
  }

  return (
    <section className="screen">
      <div className="screen-heading">
        <div>
          <span className="eyebrow">Abertura - Montador</span>
          <h1>Novo processo</h1>
        </div>
      </div>
      <form
        className="form-layout"
        onSubmit={(event) => {
          event.preventDefault()
          onCreate(form)
        }}
      >
        <FieldGrid>
          <Select label="Cliente" value={form.cliente} options={['AGCO', 'GEFCO', 'Volkswagen', 'Hyundai']} onChange={(value) => update('cliente', value)} />
          <Select label="Seguradora" value={form.seguradora} options={['SURA', 'SOMPO']} onChange={(value) => update('seguradora', value)} />
          <Select label="Operacao" value={form.operacao} options={['Montador', 'Embarcador']} onChange={(value) => update('operacao', value)} />
          <TextField label="Concessionaria" value={form.concessionaria} onChange={(value) => update('concessionaria', value)} />
          <ReasonSelect
            label="Motivo"
            reasons={reasons}
            valueId={form.motivoId}
            valueName={form.motivo}
            onChange={(reason) => {
              update('motivoId', reason.id)
              update('motivo', reason.name)
            }}
          />
          <TextField label="Chassi" value={form.chassi} onChange={(value) => update('chassi', value)} />
          <TextField label="Numero Nota Fiscal" value={form.notaFiscal} onChange={(value) => update('notaFiscal', value)} />
          <TextField label="Numero OSS" value={form.oss} onChange={(value) => update('oss', value)} />
          <TextField label="Transportadora" value={form.transportadora} onChange={(value) => update('transportadora', value)} />
          <TextField label="Data do aviso" type="date" value={form.dataAviso} onChange={(value) => update('dataAviso', value)} />
          <TextField label="Data da ocorrencia" type="date" value={form.dataOcorrencia} onChange={(value) => update('dataOcorrencia', value)} />
          <TextField label="Cidade" value={form.cidade} onChange={(value) => update('cidade', value)} />
          <TextField label="UF" value={form.uf} onChange={(value) => update('uf', value)} />
        </FieldGrid>
        <label className="textarea-field">
          Descricao da ocorrencia
          <textarea value={form.descricao} onChange={(event) => update('descricao', event.target.value)} />
        </label>
        <div className="form-footer">
          <button className="primary-action" type="submit" disabled={saving}>
            {saving ? 'Abrindo...' : 'Abrir processo'}
          </button>
        </div>
      </form>
    </section>
  )
}

function Edicao({ processo, reasons, history, loading, notFound, saving, onSave }) {
  if (loading) {
    return <div className="state-card">Carregando processo...</div>
  }

  if (notFound) {
    return <div className="state-card">Processo nao encontrado.</div>
  }

  if (!processo) {
    return <div className="state-card">Abra um processo pelo cockpit.</div>
  }

  return (
    <ProcessEditForm
      key={processo.id}
      processo={processo}
      reasons={reasons}
      history={history}
      saving={saving}
      onSave={onSave}
    />
  )
}

function ProcessEditForm({ processo, reasons, history, saving, onSave }) {
  const [draft, setDraft] = useState(toFormProcess(processo))
  const [tab, setTab] = useState('Dados da ocorrencia')

  function update(field, value) {
    setDraft((current) => ({ ...current, [field]: value }))
  }

  return (
    <section className="screen">
      <div className="process-header">
        <div>
          <span className="eyebrow">Edicao do processo</span>
          <h1>{processo.processNumber}</h1>
        </div>
        <div className="header-actions">
          <button className="ghost-action" type="button" disabled>
            Inserir historico
          </button>
          <button className="primary-action compact" type="button" onClick={() => onSave(draft)} disabled={saving}>
            {saving ? 'Salvando...' : 'Atualizar dados'}
          </button>
        </div>
      </div>

      <div className="summary-grid">
        <Summary label="Operacao" value={draft.operacao || '-'} />
        <Summary label="Motivo" value={draft.motivo || '-'} />
        <Summary label="Status" value={draft.status || '-'} />
        <Summary label="Sinistro Seguradora" value={draft.sinistroSeguradora || 'Nao integrado'} />
      </div>

      <Timeline current={processo.currentStageName || draft.status} steps={processStages} />

      <div className="tabs">
        {['Dados da ocorrencia', 'Vistoria', 'Documentos', 'Pagamentos', 'Historico'].map((item) => (
          <button key={item} type="button" className={tab === item ? 'active' : ''} onClick={() => setTab(item)}>
            {item}
          </button>
        ))}
      </div>

      {tab === 'Dados da ocorrencia' && (
        <div className="form-layout">
          <FieldGrid>
            <TextField label="Concessionaria" value={draft.concessionaria} onChange={(value) => update('concessionaria', value)} />
            <Select label="Status atual" value={draft.status} options={statusOptions} onChange={(value) => update('status', value)} />
            <ReasonSelect
              label="Motivo atual"
              reasons={reasons}
              valueId={draft.motivoId}
              valueName={draft.motivo}
              onChange={(reason) => {
                update('motivoId', reason.id)
                update('motivo', reason.name)
              }}
            />
            <TextField label="Data do aviso" type="date" value={draft.dataAviso} onChange={(value) => update('dataAviso', value)} />
            <TextField label="Data da ocorrencia" type="date" value={draft.dataOcorrencia} onChange={(value) => update('dataOcorrencia', value)} />
            <TextField label="Chassi" value={draft.chassi} onChange={(value) => update('chassi', value)} />
            <TextField label="Numero Nota Fiscal" value={draft.notaFiscal} onChange={(value) => update('notaFiscal', value)} />
            <TextField label="Transportadora" value={draft.transportadora} onChange={(value) => update('transportadora', value)} />
            <TextField label="Cidade" value={draft.cidade} onChange={(value) => update('cidade', value)} />
            <TextField label="UF" value={draft.uf} onChange={(value) => update('uf', value)} />
            <TextField label="Valor estimado" type="number" value={draft.valorEstimado} onChange={(value) => update('valorEstimado', Number(value))} />
            <TextField label="Numero OSS" value={draft.oss} onChange={(value) => update('oss', value)} />
          </FieldGrid>
          <label className="textarea-field">
            Descricao da ocorrencia
            <textarea value={draft.descricao} onChange={(event) => update('descricao', event.target.value)} />
          </label>
        </div>
      )}

      {tab === 'Vistoria' && (
        <InfoPanel
          title="Vistoria"
          rows={[
            ['Prestador', 'A definir'],
            ['Status', 'Modulo futuro'],
            ['Data prevista', '-'],
            ['Parecer', 'Ainda sem integracao real.'],
          ]}
        />
      )}

      {tab === 'Documentos' && (
        <div className="document-list">
          <div className="document-item">
            <strong>Anexos</strong>
            <span>Modulo futuro</span>
            <StatusPill status="PENDING" />
          </div>
        </div>
      )}

      {tab === 'Pagamentos' && (
        <InfoPanel
          title="Pagamentos / Encerramentos"
          rows={[
            ['Valor estimado', money(draft.valorEstimado)],
            ['Franquia', '-'],
            ['Status de pagamento', 'Modulo futuro'],
            ['Encerramento', '-'],
          ]}
        />
      )}

      {tab === 'Historico' && (
        <ol className="history-list">
          {history.length === 0 && <li>Nenhum historico registrado.</li>}
          {history.map((item) => (
            <li key={item.id}>
              <strong>{dateTimeBr(item.createdAt)}</strong>
              <span>{item.message}</span>
            </li>
          ))}
        </ol>
      )}
    </section>
  )
}

function Timeline({ current, steps = processStages }) {
  const normalizedCurrent = normalizeStage(current)
  const currentIndex = Math.max(steps.findIndex((etapa) => normalizeStage(etapa) === normalizedCurrent), 0)
  return (
    <div className="timeline">
      {steps.map((etapa, index) => {
        const done = index < currentIndex
        const active = normalizeStage(etapa) === normalizedCurrent
        return (
          <div className={done ? 'done' : active ? 'active' : ''} key={etapa}>
            <span></span>
            <strong>{etapa}</strong>
            <small>{done ? 'Concluido' : active ? 'Atual' : 'A iniciar'}</small>
          </div>
        )
      })}
    </div>
  )
}

function Metric({ label, value, tone = 'red' }) {
  return (
    <div className={`metric ${tone}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function Summary({ label, value }) {
  return (
    <div className="summary-item">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function FieldGrid({ children }) {
  return <div className="field-grid">{children}</div>
}

function TextField({ label, value, onChange, type = 'text' }) {
  return (
    <label className="field">
      {label}
      <input type={type} value={value ?? ''} onChange={(event) => onChange(event.target.value)} />
    </label>
  )
}

function Select({ label, value, options, onChange, placeholder = '-' }) {
  return (
    <label className="field">
      {label}
      <select value={value ?? ''} onChange={(event) => onChange(event.target.value)}>
        {options.map((option) => (
          <option key={option || 'empty'} value={option}>
            {option ? statusLabel(option) : placeholder}
          </option>
        ))}
      </select>
    </label>
  )
}

function ReasonSelect({ label, reasons, valueId, valueName, onChange }) {
  const selectedValue = getReasonSelectValue(reasons, valueId, valueName)

  return (
    <label className="field">
      {label}
      <select
        value={selectedValue}
        onChange={(event) => {
          const reason = findReasonBySelectValue(reasons, event.target.value)
          onChange(reason)
        }}
      >
        <option value="">Sem motivo cadastrado</option>
        {reasons.map((reason) => {
          const value = reason.id || reason.name
          return (
            <option key={value} value={value}>
              {reason.name}
            </option>
          )
        })}
      </select>
    </label>
  )
}

function StatusPill({ status }) {
  const kind = ['PENDING', 'Falta de documento'].includes(status)
    ? 'warning'
    : ['CLOSED', 'Aprovado', 'Pagamento'].includes(status)
      ? 'success'
      : 'neutral'
  return <span className={`status-pill ${kind}`}>{statusLabel(status)}</span>
}

function InfoPanel({ title, rows }) {
  return (
    <div className="info-panel">
      <h2>{title}</h2>
      {rows.map(([label, value]) => (
        <div className="info-row" key={label}>
          <span>{label}</span>
          <strong>{value}</strong>
        </div>
      ))}
    </div>
  )
}

function SkeletonRows({ rows, columns }) {
  return Array.from({ length: rows }).map((_, rowIndex) => (
    <tr key={rowIndex}>
      {Array.from({ length: columns }).map((__, columnIndex) => (
        <td key={columnIndex}>
          <span className="skeleton-line"></span>
        </td>
      ))}
    </tr>
  ))
}

function getUserInitial(value = '') {
  return value.trim().charAt(0).toUpperCase() || 'U'
}

function toProcessPayload(form) {
  return {
    operationType: emptyToNull(form.operacao),
    clientName: emptyToNull(form.cliente),
    insuranceCompany: emptyToNull(form.seguradora),
    dealershipName: emptyToNull(form.concessionaria),
    currentReasonId: emptyToNull(form.motivoId),
    currentReasonName: emptyToNull(form.motivo),
    insuranceClaimNumber: emptyToNull(form.sinistroSeguradora),
    chassis: emptyToNull(form.chassi),
    invoiceNumber: emptyToNull(form.notaFiscal),
    ossNumber: emptyToNull(form.oss),
    carrierName: emptyToNull(form.transportadora),
    occurrenceDate: emptyToNull(form.dataOcorrencia),
    claimDate: emptyToNull(form.dataAviso),
    city: emptyToNull(form.cidade),
    state: emptyToNull(form.uf),
    estimatedValue: Number(form.valorEstimado || 0),
    description: emptyToNull(form.descricao),
    status: emptyToNull(form.status) || 'OPEN',
  }
}

function toFormProcess(processo) {
  return {
    cliente: processo?.clientName || 'AGCO',
    seguradora: processo?.insuranceCompany || 'SURA',
    operacao: processo?.operationType || 'Montador',
    concessionaria: processo?.dealershipName || '',
    motivoId: processo?.currentReasonId || '',
    motivo: processo?.currentReasonName || processo?.currentReason?.name || '',
    status: processo?.status || 'OPEN',
    sinistroSeguradora: processo?.insuranceClaimNumber || '',
    dataAviso: toDateInput(processo?.claimDate),
    dataOcorrencia: toDateInput(processo?.occurrenceDate),
    chassi: processo?.chassis || '',
    notaFiscal: processo?.invoiceNumber || '',
    oss: processo?.ossNumber || '',
    transportadora: processo?.carrierName || '',
    cidade: processo?.city || '',
    uf: processo?.state || 'SP',
    valorEstimado: Number(processo?.estimatedValue || 0),
    descricao: processo?.description || '',
  }
}

function mergeProcessDraft(processo, draft) {
  return {
    ...processo,
    currentReasonId: processo.currentReasonId || emptyToNull(draft.motivoId),
    currentReasonName: processo.currentReasonName || emptyToNull(draft.motivo),
  }
}

function getReasonSelectValue(reasons, valueId, valueName) {
  if (valueId) return valueId

  if (valueName) {
    const matchedReason = reasons.find((reason) => reason.name === valueName)
    return matchedReason?.id || matchedReason?.name || valueName
  }

  return ''
}

function findReasonBySelectValue(reasons, value) {
  if (!value) {
    return { id: '', name: '' }
  }

  return reasons.find((reason) => reason.id === value || reason.name === value) || { id: '', name: value }
}

function emptyToNull(value) {
  if (value === null || value === undefined) return null
  const normalized = String(value).trim()
  return normalized === '' ? null : normalized
}

function toDateInput(value) {
  if (!value) return ''
  return String(value).slice(0, 10)
}

function dateBr(value) {
  const date = toDateInput(value)
  if (!date) return '-'
  const [year, month, day] = date.split('-')
  return `${day}/${month}/${year}`
}

function dateTimeBr(value) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value))
}

function money(value) {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
    maximumFractionDigits: 0,
  }).format(Number(value || 0))
}

function statusLabel(value) {
  const labels = {
    OPEN: 'Aberto',
    IN_PROGRESS: 'Em andamento',
    PENDING: 'Pendente',
    CLOSED: 'Concluido',
    CANCELLED: 'Cancelado',
  }
  return labels[value] || value || '-'
}

function normalizeStage(value = '') {
  return String(value)
    .trim()
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
}

export default App
