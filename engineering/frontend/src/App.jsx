import { Routes, Route } from 'react-router-dom'
import Layout from './components/layout/Layout.jsx'
import DashboardPage from './pages/DashboardPage.jsx'
import NewProjectPage from './pages/NewProjectPage.jsx'
import ProjectDetailPage from './pages/ProjectDetailPage.jsx'

function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/projects/new" element={<NewProjectPage />} />
        <Route path="/projects/:id" element={<ProjectDetailPage />} />
      </Route>
    </Routes>
  )
}

export default App
