import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import {
  getUsers, approveUser, rejectUser, suspendUser, reactivateUser, getMetrics,
  type AdminUser, type PlatformMetrics
} from '../api/admin'

// ── Stat card ──────────────────────────────────────────────────────────────
function StatCard({ label, value, sub, color }: { label: string; value: number | string; sub?: string; color?: string }) {
  return (
    <div className="bg-gray-800 rounded-xl p-5 flex flex-col gap-1">
      <span className="text-gray-400 text-xs uppercase tracking-wide">{label}</span>
      <span className={`text-3xl font-bold ${color ?? 'text-white'}`}>{value}</span>
      {sub && <span className="text-gray-500 text-xs">{sub}</span>}
    </div>
  )
}

// ── Mini bar chart ─────────────────────────────────────────────────────────
function BarChart({ data, color, label }: { data: { date: string; count: number }[]; color: string; label: string }) {
  const max = Math.max(...data.map(d => d.count), 1)
  const recent = data.slice(-14) // last 14 days for readability
  return (
    <div>
      <p className="text-gray-400 text-xs mb-3">{label}</p>
      <div className="flex items-end gap-1 h-24">
        {recent.map((d) => (
          <div key={d.date} className="flex-1 flex flex-col items-center gap-1 group relative">
            <div
              className={`w-full rounded-sm ${color} opacity-80 group-hover:opacity-100 transition-opacity`}
              style={{ height: `${Math.max((d.count / max) * 88, 2)}px` }}
            />
            <div className="absolute bottom-full mb-1 hidden group-hover:block bg-gray-900 text-white text-xs px-2 py-1 rounded whitespace-nowrap z-10">
              {d.date.slice(5)}: {d.count}
            </div>
          </div>
        ))}
      </div>
      <div className="flex justify-between mt-1">
        <span className="text-gray-600 text-xs">{recent[0]?.date.slice(5)}</span>
        <span className="text-gray-600 text-xs">{recent[recent.length - 1]?.date.slice(5)}</span>
      </div>
    </div>
  )
}

// ── Status badge ───────────────────────────────────────────────────────────
function StatusBadge({ status }: { status: string }) {
  const styles: Record<string, string> = {
    ACTIVE: 'bg-green-900/50 text-green-400 border-green-700',
    PENDING: 'bg-yellow-900/50 text-yellow-400 border-yellow-700',
    SUSPENDED: 'bg-red-900/50 text-red-400 border-red-700',
  }
  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs border ${styles[status] ?? 'bg-gray-700 text-gray-300 border-gray-600'}`}>
      {status}
    </span>
  )
}

// ── Main page ──────────────────────────────────────────────────────────────
export default function AdminDashboardPage() {
  const { signOut, role } = useAuth()
  const navigate = useNavigate()

  const [users, setUsers] = useState<AdminUser[]>([])
  const [metrics, setMetrics] = useState<PlatformMetrics | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionLoading, setActionLoading] = useState<string | null>(null)
  const [tab, setTab] = useState<'pending' | 'all'>('pending')
  const [search, setSearch] = useState('')

  useEffect(() => {
    if (role !== 'ADMIN') { navigate('/dashboard'); return }
    loadAll()
  }, [])

  async function loadAll() {
    setLoading(true)
    setError('')
    try {
      const [usersRes, metricsRes] = await Promise.all([getUsers(), getMetrics()])
      setUsers(usersRes.data)
      setMetrics(metricsRes.data)
    } catch {
      setError('Failed to load admin data.')
    } finally {
      setLoading(false)
    }
  }

  async function handleAction(id: string, action: 'approve' | 'reject' | 'suspend' | 'reactivate') {
    setActionLoading(id + action)
    try {
      let updated: AdminUser
      if (action === 'approve') updated = (await approveUser(id)).data
      else if (action === 'reject') updated = (await rejectUser(id)).data
      else if (action === 'suspend') updated = (await suspendUser(id)).data
      else updated = (await reactivateUser(id)).data

      setUsers(prev => prev.map(u => u.id === id ? updated : u))
    } catch {
      alert('Action failed. Please try again.')
    } finally {
      setActionLoading(null)
    }
  }

  const filtered = users
    .filter(u => tab === 'pending' ? u.status === 'PENDING' : true)
    .filter(u =>
      !search ||
      u.email.toLowerCase().includes(search.toLowerCase()) ||
      u.displayName.toLowerCase().includes(search.toLowerCase())
    )

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-950 flex items-center justify-center">
        <div className="text-gray-400">Loading admin dashboard…</div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-950 text-white">
      {/* Header */}
      <header className="bg-gray-900 border-b border-gray-800 px-6 py-4 flex items-center justify-between">
        <div>
          <h1 className="text-lg font-semibold">Admin Dashboard</h1>
          <p className="text-gray-500 text-xs mt-0.5">Platform management & analytics</p>
        </div>
        <button
          onClick={async () => { await signOut(); navigate('/login') }}
          className="text-gray-400 hover:text-white text-sm transition-colors"
        >
          Sign out
        </button>
      </header>

      <main className="max-w-7xl mx-auto px-6 py-8 space-y-10">
        {error && (
          <div className="bg-red-900/40 border border-red-700 text-red-300 text-sm px-4 py-3 rounded-lg">
            {error}
          </div>
        )}

        {/* ── Platform metrics ── */}
        {metrics && (
          <section>
            <h2 className="text-gray-300 text-sm font-semibold uppercase tracking-wider mb-4">Platform Overview</h2>

            {/* User counts */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
              <StatCard label="Total Users" value={metrics.totalUsers} />
              <StatCard label="Active" value={metrics.activeUsers} color="text-green-400" />
              <StatCard label="Pending" value={metrics.pendingUsers} color="text-yellow-400" sub="awaiting approval" />
              <StatCard label="Suspended" value={metrics.suspendedUsers} color="text-red-400" />
            </div>

            {/* Growth & engagement */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
              <StatCard label="New (7d)" value={metrics.newUsersLast7Days} sub={`${metrics.newUsersLast30Days} last 30d`} />
              <StatCard label="Active Users (7d)" value={metrics.activeUsersLast7Days} sub="≥1 interview" />
              <StatCard label="Dormant" value={metrics.dormantUsers} color="text-gray-400" sub="no interviews yet" />
              <StatCard label="Power Users" value={metrics.powerUsers} color="text-indigo-400" sub="3+ interviews" />
            </div>

            {/* Interview & token stats */}
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 mb-8">
              <StatCard label="Total Interviews" value={metrics.totalInterviews} />
              <StatCard label="Interviews (7d)" value={metrics.interviewsLast7Days} />
              <StatCard label="Total Tokens Used" value={metrics.totalTokensUsed.toLocaleString()} />
            </div>

            {/* Time-series charts */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
              <div className="bg-gray-800 rounded-xl p-5">
                <h3 className="text-white text-sm font-medium mb-4">New Users (last 30 days)</h3>
                {metrics.newUsersByDay.length > 0 ? (
                  <BarChart data={metrics.newUsersByDay} color="bg-indigo-500" label="Daily new registrations" />
                ) : (
                  <p className="text-gray-600 text-sm">No data yet</p>
                )}
              </div>
              <div className="bg-gray-800 rounded-xl p-5">
                <h3 className="text-white text-sm font-medium mb-4">Interviews (last 30 days)</h3>
                {metrics.interviewsByDay.length > 0 ? (
                  <BarChart data={metrics.interviewsByDay} color="bg-green-500" label="Daily interviews started" />
                ) : (
                  <p className="text-gray-600 text-sm">No data yet</p>
                )}
              </div>
            </div>
          </section>
        )}

        {/* ── User management ── */}
        <section>
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-4">
            <h2 className="text-gray-300 text-sm font-semibold uppercase tracking-wider">User Management</h2>
            <div className="flex items-center gap-3">
              <input
                type="text"
                placeholder="Search by email or name…"
                value={search}
                onChange={e => setSearch(e.target.value)}
                className="bg-gray-800 border border-gray-700 text-white text-sm rounded-lg px-3 py-1.5 w-56 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
              <button
                onClick={loadAll}
                className="text-gray-400 hover:text-white text-sm px-3 py-1.5 bg-gray-800 border border-gray-700 rounded-lg transition-colors"
              >
                Refresh
              </button>
            </div>
          </div>

          {/* Tabs */}
          <div className="flex gap-1 mb-4 bg-gray-800 p-1 rounded-lg w-fit">
            {(['pending', 'all'] as const).map(t => (
              <button
                key={t}
                onClick={() => setTab(t)}
                className={`px-4 py-1.5 rounded-md text-sm font-medium transition-colors capitalize ${
                  tab === t ? 'bg-gray-700 text-white' : 'text-gray-400 hover:text-white'
                }`}
              >
                {t === 'pending' ? `Pending (${users.filter(u => u.status === 'PENDING').length})` : `All (${users.length})`}
              </button>
            ))}
          </div>

          {/* Table */}
          <div className="bg-gray-800 rounded-xl overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-700">
                    <th className="text-left text-gray-400 font-medium px-4 py-3">User</th>
                    <th className="text-left text-gray-400 font-medium px-4 py-3">Status</th>
                    <th className="text-right text-gray-400 font-medium px-4 py-3">Logins</th>
                    <th className="text-right text-gray-400 font-medium px-4 py-3">Interviews</th>
                    <th className="text-right text-gray-400 font-medium px-4 py-3">Avg Score</th>
                    <th className="text-right text-gray-400 font-medium px-4 py-3">Tokens</th>
                    <th className="text-right text-gray-400 font-medium px-4 py-3">Joined</th>
                    <th className="text-center text-gray-400 font-medium px-4 py-3">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-700/50">
                  {filtered.length === 0 && (
                    <tr>
                      <td colSpan={8} className="text-center text-gray-500 py-8">
                        {tab === 'pending' ? 'No users pending approval' : 'No users found'}
                      </td>
                    </tr>
                  )}
                  {filtered.map(user => (
                    <tr key={user.id} className="hover:bg-gray-700/30 transition-colors">
                      <td className="px-4 py-3">
                        <div className="font-medium text-white">{user.displayName}</div>
                        <div className="text-gray-400 text-xs">{user.email}</div>
                      </td>
                      <td className="px-4 py-3">
                        <StatusBadge status={user.status} />
                      </td>
                      <td className="px-4 py-3 text-right text-gray-300">{user.loginCount}</td>
                      <td className="px-4 py-3 text-right text-gray-300">{user.totalInterviews}</td>
                      <td className="px-4 py-3 text-right text-gray-300">
                        {user.totalInterviews > 0 ? user.avgScore.toFixed(1) : '—'}
                      </td>
                      <td className="px-4 py-3 text-right text-gray-300">{user.totalTokensUsed.toLocaleString()}</td>
                      <td className="px-4 py-3 text-right text-gray-400 text-xs">
                        {new Date(user.createdAt).toLocaleDateString()}
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center justify-center gap-2 flex-wrap">
                          {user.status === 'PENDING' && (
                            <>
                              <ActionButton
                                label="Approve"
                                color="green"
                                loading={actionLoading === user.id + 'approve'}
                                onClick={() => handleAction(user.id, 'approve')}
                              />
                              <ActionButton
                                label="Reject"
                                color="red"
                                loading={actionLoading === user.id + 'reject'}
                                onClick={() => handleAction(user.id, 'reject')}
                              />
                            </>
                          )}
                          {user.status === 'ACTIVE' && (
                            <ActionButton
                              label="Suspend"
                              color="orange"
                              loading={actionLoading === user.id + 'suspend'}
                              onClick={() => handleAction(user.id, 'suspend')}
                            />
                          )}
                          {user.status === 'SUSPENDED' && (
                            <ActionButton
                              label="Reactivate"
                              color="blue"
                              loading={actionLoading === user.id + 'reactivate'}
                              onClick={() => handleAction(user.id, 'reactivate')}
                            />
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </section>
      </main>
    </div>
  )
}

function ActionButton({
  label, color, loading, onClick
}: { label: string; color: 'green' | 'red' | 'orange' | 'blue'; loading: boolean; onClick: () => void }) {
  const styles = {
    green: 'bg-green-900/40 hover:bg-green-800/60 text-green-400 border-green-700',
    red: 'bg-red-900/40 hover:bg-red-800/60 text-red-400 border-red-700',
    orange: 'bg-orange-900/40 hover:bg-orange-800/60 text-orange-400 border-orange-700',
    blue: 'bg-blue-900/40 hover:bg-blue-800/60 text-blue-400 border-blue-700',
  }
  return (
    <button
      disabled={loading}
      onClick={onClick}
      className={`px-2.5 py-1 rounded-md text-xs border font-medium disabled:opacity-50 transition-colors ${styles[color]}`}
    >
      {loading ? '…' : label}
    </button>
  )
}
