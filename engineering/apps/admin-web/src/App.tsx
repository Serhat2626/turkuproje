import { Routes, Route } from 'react-router-dom'
import Layout from './components/layout/Layout'
import DashboardPage from './pages/DashboardPage'
import KnowledgeItemsPage from './pages/KnowledgeItemsPage'
import ContentProjectsPage from './pages/ContentProjectsPage'
import MediaAssetsPage from './pages/MediaAssetsPage'
import DraftReviewPage from './pages/DraftReviewPage'

function App() {
  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route index element={<DashboardPage />} />
        <Route path="knowledge" element={<KnowledgeItemsPage />} />
        <Route path="projects" element={<ContentProjectsPage />} />
        <Route path="drafts/:draftId" element={<DraftReviewPage />} />
        <Route path="media" element={<MediaAssetsPage />} />
      </Route>
    </Routes>
  )
}

export default App

