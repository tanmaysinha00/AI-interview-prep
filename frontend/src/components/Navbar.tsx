import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Navbar() {
  const { isAuthenticated, signOut } = useAuth()
  const navigate = useNavigate()

  const handleLogout = async () => {
    await signOut()
    navigate('/login')
  }

  return (
    <nav className="bg-gray-900 border-b border-gray-700 px-6 py-3 flex items-center justify-between">
      <Link to="/" className="text-white font-semibold text-lg tracking-tight">
        AI Interview Prep
      </Link>
      <div className="flex items-center gap-4">
        {isAuthenticated ? (
          <>
            <Link to="/dashboard" className="text-gray-300 hover:text-white text-sm transition-colors">
              Dashboard
            </Link>
            <button
              onClick={handleLogout}
              className="text-gray-300 hover:text-white text-sm transition-colors"
            >
              Sign out
            </button>
          </>
        ) : (
          <>
            <Link to="/login" className="text-gray-300 hover:text-white text-sm transition-colors">
              Sign in
            </Link>
            <Link
              to="/register"
              className="bg-indigo-600 hover:bg-indigo-500 text-white text-sm px-4 py-1.5 rounded-lg transition-colors"
            >
              Get started
            </Link>
          </>
        )}
      </div>
    </nav>
  )
}
