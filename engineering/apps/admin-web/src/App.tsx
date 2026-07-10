import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './contexts/AuthContext'
import Layout from './components/layout/Layout'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import KnowledgeItemsPage from './pages/KnowledgeItemsPage'
import ContentProjectsPage from './pages/ContentProjectsPage'
import MediaAssetsPage from './pages/MediaAssetsPage'
import DraftReviewPage from './pages/DraftReviewPage'
import TurkuVideoWorkflowPage from './pages/TurkuVideoWorkflowPage'

/**
 * Korumalı route — authenticated olmayan kullanıcıyı login'e yönlendirir.
 */
function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isLoading } = useAuth()

  if (isLoading) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', color: '#94a3b8' }}>
        Yükleniyor...
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  return <>{children}</>
}

function AppRoutes() {
  const { isAuthenticated, isLoading } = useAuth()

  if (isLoading) {
    return null
  }

  return (
    <Routes>
      {/* Login — authenticated ise dashboard'a yönlendir */}
      <Route
        path="/login"
        element={isAuthenticated ? <Navigate to="/" replace /> : <LoginPage />}
      />

      {/* Korumalı rotalar */}
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route path="knowledge" element={<KnowledgeItemsPage />} />
        <Route path="projects" element={<ContentProjectsPage />} />
        <Route path="drafts/:draftId" element={<DraftReviewPage />} />
        <Route path="media" element={<MediaAssetsPage />} />
        <Route path="turku-video" element={<TurkuVideoWorkflowPage />} />
      </Route>

      {/* Bilinmeyen rotalar → login'e yönlendir */}
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}

function App() {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  )
}

export default App
