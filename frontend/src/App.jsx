
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import MainLayout from './layouts/MainLayout'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import { useAuth } from './context/AuthContext'
import FindSizePage from './pages/FindSizePage'
import AdminDashboardPage from './pages/AdminDashboardPage'
import BrandsPage from './pages/BrandsPage'
import ComparePage from './pages/ComparePage'
import SizeGuidePage from './pages/SizeGuidePage'

function PlaceholderPage({ title }) {
  return (
    <div style={{ padding: '100px 7%', minHeight: '65vh', background: '#faf9f6' }}>
      <p style={{ color: '#a48665', letterSpacing: '2px', fontSize: '11px' }}>
        UNIVERSAL FIT
      </p>
      <h1 style={{ fontFamily: 'Georgia, serif', fontSize: '48px', marginTop: '15px' }}>
        {title}
      </h1>
      <p style={{ color: '#777168', marginTop: '15px' }}>
        This feature is coming next.
      </p>
    </div>
  )
}

function ProtectedRoute({ children }) {
  const { isAuthenticated } = useAuth()

  return isAuthenticated ? children : <Navigate to="/login" replace />
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<MainLayout />}>
          <Route path="/" element={<HomePage />} />

          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          <Route
            path="/find-size"
            element={
              <ProtectedRoute>
                <FindSizePage />
              </ProtectedRoute>
            }
          />

          <Route path="/admin" element={<ProtectedRoute><AdminDashboardPage /></ProtectedRoute>} />
          <Route path="/brands" element={<BrandsPage />} />
          <Route path="/compare" element={<ProtectedRoute><ComparePage /></ProtectedRoute>} />
          <Route path="/size-guide" element={<SizeGuidePage />} />
          <Route path="/how-it-works" element={<PlaceholderPage title="How It Works" />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

export default App;
