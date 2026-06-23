import { useCallback, useEffect, useMemo, useState } from 'react'
import { getRoles } from '../../api/roles'
import { disableUser, enableUser, getUsers, updateRoles, updateUser } from '../../api/users'
import { ROLES } from '../../constants/roles'
import { useAuthStore } from '../../store/authStore'

const emptyMessage = 'Nenhum usuario encontrado.'

export default function AdminUsers() {
  const currentUser = useAuthStore((state) => state.user)
  const [users, setUsers] = useState([])
  const [roles, setRoles] = useState([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [editingUser, setEditingUser] = useState(null)
  const [draft, setDraft] = useState(null)

  const filteredUsers = useMemo(() => {
    const term = search.trim().toLowerCase()

    if (!term) return users

    return users.filter((user) =>
      [user.name, user.email].join(' ').toLowerCase().includes(term),
    )
  }, [search, users])

  const loadUsers = useCallback(async (editingUserId) => {
    setLoading(true)
    setError('')

    try {
      const [nextUsers, nextRoles] = await Promise.all([getUsers(), getRoles()])
      setUsers(nextUsers)
      setRoles(nextRoles)

      if (editingUserId) {
        const refreshedUser = nextUsers.find((user) => user.id === editingUserId)
        if (refreshedUser) {
          setEditingUser(refreshedUser)
          setDraft(createDraft(refreshedUser))
        }
      }
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Nao foi possivel carregar os usuarios.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      loadUsers()
    }, 0)

    return () => window.clearTimeout(timeoutId)
  }, [loadUsers])

  function openEdit(user) {
    setMessage('')
    setError('')
    setEditingUser(user)
    setDraft(createDraft(user))
  }

  function backToList() {
    setEditingUser(null)
    setDraft(null)
    setSaving(false)
    setError('')
  }

  function updateDraft(field, value) {
    setDraft((current) => ({ ...current, [field]: value }))
  }

  function toggleRole(role) {
    setDraft((current) => {
      const nextRoles = current.roles.includes(role)
        ? current.roles.filter((item) => item !== role)
        : [...current.roles, role]

      return { ...current, roles: nextRoles }
    })
  }

  async function saveEdit(event) {
    event.preventDefault()
    setSaving(true)
    setError('')
    setMessage('')

    const isSelf = editingUser.id === currentUser?.id
    const removesOwnAdmin = isSelf && editingUser.roles.includes(ROLES.ADMIN) && !draft.roles.includes(ROLES.ADMIN)

    if (removesOwnAdmin) {
      setError('Voce nao pode remover sua propria role ADMIN.')
      setSaving(false)
      return
    }

    try {
      await updateUser(editingUser.id, {
        name: draft.name,
        email: draft.email,
      })

      if (rolesChanged(editingUser.roles, draft.roles)) {
        await updateRoles(editingUser.id, draft.roles)
      }

      await loadUsers(editingUser.id)
      setMessage('Usuario atualizado com sucesso.')
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Nao foi possivel salvar o usuario.')
    } finally {
      setSaving(false)
    }
  }

  async function toggleActive() {
    const isSelf = editingUser.id === currentUser?.id

    if (isSelf && editingUser.active) {
      setError('Voce nao pode desativar sua propria conta.')
      return
    }

    const action = editingUser.active ? 'desativar' : 'ativar'
    const confirmed = window.confirm(`Deseja ${action} ${editingUser.name}?`)

    if (!confirmed) return

    setSaving(true)
    setError('')
    setMessage('')

    try {
      if (editingUser.active) {
        await disableUser(editingUser.id)
      } else {
        await enableUser(editingUser.id)
      }

      await loadUsers(editingUser.id)
      setMessage(`Usuario ${editingUser.active ? 'desativado' : 'ativado'} com sucesso.`)
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Nao foi possivel alterar o status.')
    } finally {
      setSaving(false)
    }
  }

  if (editingUser && draft) {
    return (
      <UserEditScreen
        draft={draft}
        error={error}
        message={message}
        roles={roles}
        saving={saving}
        user={editingUser}
        onBack={backToList}
        onSave={saveEdit}
        onStatusChange={toggleActive}
        onToggleRole={toggleRole}
        onUpdateDraft={updateDraft}
      />
    )
  }

  return (
    <section className="screen admin-users-screen">
      <div className="screen-heading">
        <div>
          <span className="eyebrow">Administracao</span>
          <h1>Usuarios</h1>
        </div>
        <button className="ghost-action" type="button" onClick={loadUsers} disabled={loading}>
          Atualizar
        </button>
      </div>

      <div className="filter-bar admin-filter-bar">
        <label>
          Buscar por nome ou email
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="usuario@test.com"
          />
        </label>
      </div>

      {message && <div className="form-success">{message}</div>}
      {error && <div className="form-error">{error}</div>}

      <div className="table-panel">
        <div className="table-title">
          <strong>{filteredUsers.length} usuarios encontrados</strong>
          <span>{loading ? 'Carregando...' : 'Dados reais do backend'}</span>
        </div>
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Nome</th>
                <th>Email</th>
                <th>Roles</th>
                <th>Status</th>
                <th>Criado em</th>
                <th>Acoes</th>
              </tr>
            </thead>
            <tbody>
              {!loading && filteredUsers.length === 0 && (
                <tr>
                  <td colSpan="6">{emptyMessage}</td>
                </tr>
              )}
              {filteredUsers.map((user) => (
                <tr key={user.id}>
                  <td>
                    <strong>{user.name}</strong>
                  </td>
                  <td>{user.email}</td>
                  <td>
                    <RoleList roles={user.roles} />
                  </td>
                  <td>
                    <span className={`status-pill ${user.active ? 'success' : 'warning'}`}>
                      {user.active ? 'Ativo' : 'Inativo'}
                    </span>
                  </td>
                  <td>{dateTimeBr(user.createdAt)}</td>
                  <td>
                    <div className="table-actions">
                      <button className="icon-button" type="button" onClick={() => openEdit(user)}>
                        Editar
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  )
}

function UserEditScreen({
  draft,
  error,
  message,
  roles,
  saving,
  user,
  onBack,
  onSave,
  onStatusChange,
  onToggleRole,
  onUpdateDraft,
}) {
  return (
    <section className="screen admin-users-screen">
      <div className="screen-heading">
        <div>
          <span className="eyebrow">Usuario</span>
          <h1>Editar acesso</h1>
        </div>
        <button className="ghost-action" type="button" onClick={onBack}>
          Voltar
        </button>
      </div>

      {message && <div className="form-success">{message}</div>}
      {error && <div className="form-error">{error}</div>}

      <form className="user-edit-layout" onSubmit={onSave}>
        <div className="user-edit-main">
          <section className="edit-section">
            <div className="section-heading">
              <div>
                <span className="eyebrow">Dados basicos</span>
                <h2>{user.name}</h2>
              </div>
              <span className={`status-pill ${user.active ? 'success' : 'warning'}`}>
                {user.active ? 'Ativo' : 'Inativo'}
              </span>
            </div>

            <div className="field-grid two-columns">
              <label className="field">
                Nome
                <input value={draft.name} onChange={(event) => onUpdateDraft('name', event.target.value)} />
              </label>
              <label className="field">
                Email
                <input
                  type="email"
                  value={draft.email}
                  onChange={(event) => onUpdateDraft('email', event.target.value)}
                />
              </label>
            </div>
          </section>

          <section className="edit-section">
            <div className="section-heading">
              <div>
                <span className="eyebrow">Permissoes</span>
                <h2>Roles do usuario</h2>
              </div>
            </div>

            <div className="role-grid">
              {roles.map((role) => (
                <label key={role.name} className="role-card">
                  <input
                    type="checkbox"
                    checked={draft.roles.includes(role.name)}
                    onChange={() => onToggleRole(role.name)}
                  />
                  <span>
                    <strong>{role.name}</strong>
                    {role.description && <small>{role.description}</small>}
                  </span>
                </label>
              ))}
            </div>
          </section>
        </div>

        <aside className="user-edit-side">
          <section className="edit-section">
            <span className="eyebrow">Status</span>
            <h2>{user.active ? 'Conta ativa' : 'Conta inativa'}</h2>
            <p>
              Usuarios inativos nao devem conseguir manter acesso ao sistema. Tokens ativos sao invalidados pelo backend.
            </p>
            <button className="ghost-action danger-action" type="button" onClick={onStatusChange} disabled={saving}>
              {user.active ? 'Desativar usuario' : 'Ativar usuario'}
            </button>
          </section>

          <section className="edit-section">
            <span className="eyebrow">Seguranca</span>
            <h2>Reset de senha</h2>
            <p>Preparado para fluxo administrativo futuro.</p>
            <button className="ghost-action" type="button" disabled>
              Resetar senha
            </button>
          </section>

          <div className="edit-actions">
            <button className="ghost-action" type="button" onClick={onBack}>
              Cancelar
            </button>
            <button className="primary-action compact" type="submit" disabled={saving}>
              {saving ? 'Salvando...' : 'Salvar alteracoes'}
            </button>
          </div>
        </aside>
      </form>
    </section>
  )
}

function RoleList({ roles }) {
  return (
    <div className="role-list">
      {roles.map((role) => (
        <span key={role} className="status-pill neutral">
          {role}
        </span>
      ))}
    </div>
  )
}

function createDraft(user) {
  return {
    name: user.name,
    email: user.email,
    roles: [...user.roles],
  }
}

function rolesChanged(currentRoles, nextRoles) {
  const current = [...currentRoles].sort().join('|')
  const next = [...nextRoles].sort().join('|')
  return current !== next
}

function dateTimeBr(value) {
  if (!value) return '-'

  return new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value))
}
