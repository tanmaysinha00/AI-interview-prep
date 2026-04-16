import { HashRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import PrivateRoute from './components/PrivateRoute'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import DashboardPage from './pages/DashboardPage'
import InterviewPage from './pages/InterviewPage'
import SummaryPage from './pages/SummaryPage'
import AdminDashboardPage from './pages/AdminDashboardPage'

export default function App() {
  return (
    <AuthProvider>
      <HashRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route
            path="/dashboard"
            element={<PrivateRoute><DashboardPage /></PrivateRoute>}
          />
          <Route
            path="/interview/:id"
            element={<PrivateRoute><InterviewPage /></PrivateRoute>}
          />
          <Route
            path="/interview/:id/summary"
            element={<PrivateRoute><SummaryPage /></PrivateRoute>}
          />
          <Route
            path="/admin"
            element={<PrivateRoute><AdminDashboardPage /></PrivateRoute>}
          />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </HashRouter>
    </AuthProvider>
  )
}
