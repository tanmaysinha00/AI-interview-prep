import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { login } from '../api/auth'
import { useAuth } from '../context/AuthContext'

export default function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [isPending, setIsPending] = useState(false)
  const [loading, setLoading] = useState(false)
  const { setToken } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError('')
    setIsPending(false)
    setLoading(true)
    try {
      const res = await login(email, password)
      setToken(res.data.accessToken)
      // role is set synchronously inside setToken
      const decoded = (() => {
        try { return JSON.parse(atob(res.data.accessToken.split('.')[1])) } catch { return {} }
      })()
      if (decoded.role === 'ADMIN') {
        navigate('/admin')
      } else {
        navigate('/dashboard')
      }
    } catch (err: unknown) {
      const detail = (err as { response?: { data?: { detail?: string } } })?.response?.data?.detail ?? ''
      if (detail.toLowerCase().includes('pending')) {
        setIsPending(true)
      } else {
        setError(detail || 'Invalid credentials. Please try again.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-950 flex items-center justify-center px-4">
      <div className="w-full max-w-sm">
        <h1 className="text-white text-2xl font-semibold text-center mb-8">Sign in</h1>
        <form onSubmit={handleSubmit} className="space-y-4">
          {isPending && (
            <div className="bg-yellow-900/40 border border-yellow-600 text-yellow-300 text-sm px-4 py-3 rounded-lg">
              <p className="font-medium">Account pending approval</p>
              <p className="text-yellow-400 mt-1">Your account is awaiting admin approval. You'll be able to sign in once it's activated.</p>
            </div>
          )}
          {error && (
            <div className="bg-red-900/40 border border-red-700 text-red-300 text-sm px-4 py-2 rounded-lg">
              {error}
            </div>
          )}
          <div>
            <label className="text-gray-400 text-sm block mb-1.5">Email</label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full bg-gray-800 border border-gray-700 text-white rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              placeholder="you@example.com"
            />
          </div>
          <div>
            <label className="text-gray-400 text-sm block mb-1.5">Password</label>
            <input
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full bg-gray-800 border border-gray-700 text-white rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              placeholder="••••••••"
            />
          </div>
          <button
            type="submit"
            disabled={loading}
            className="w-full bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white rounded-lg py-2 text-sm font-medium transition-colors"
          >
            {loading ? 'Signing in…' : 'Sign in'}
          </button>
        </form>
        <p className="text-gray-500 text-sm text-center mt-6">
          Don't have an account?{' '}
          <Link to="/register" className="text-indigo-400 hover:text-indigo-300">Register</Link>
        </p>
      </div>
    </div>
  )
}
