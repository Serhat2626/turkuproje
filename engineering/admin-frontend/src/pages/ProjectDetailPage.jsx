import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { projectApi } from '../api/client.js'
import LoadingSpinner from '../components/common/LoadingSpinner.jsx'
import PipelineStepper from '../components/common/PipelineStepper.jsx'
import ResearchTab from '../components/tabs/ResearchTab.jsx'
import ScriptTab from '../components/tabs/ScriptTab.jsx'
import StoryboardTab from '../components/tabs/StoryboardTab.jsx'
import VisualsTab from '../components/tabs/VisualsTab.jsx'
import AudioTab from '../components/tabs/AudioTab.jsx'
import VideoTab from '../components/tabs/VideoTab.jsx'
import PublishTab from '../components/tabs/PublishTab.jsx'
import { ArrowLeft } from 'lucide-react'

const TAB_COMPONENTS = { research: ResearchTab, script: ScriptTab, storyboard: StoryboardTab, visuals: VisualsTab, audio: AudioTab, video: VideoTab, publish: PublishTab }

export default function ProjectDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [project, setProject] = useState(null)
  const [loading, setLoading] = useState(true)
  const [activeTab, setActiveTab] = useState('research')

  const loadProject = useCallback(async () => {
    try { const res = await projectApi.get(id); setProject(res.data) }
    catch (err) { console.error('Proje yüklenemedi:', err) }
    finally { setLoading(false) }
  }, [id])

  useEffect(() => { loadProject() }, [loadProject])
  if (loading) return <LoadingSpinner />
  if (!project) return <div>Proje bulunamadı</div>
  const ActiveTabComponent = TAB_COMPONENTS[activeTab]
  return (
    <div>
      <div className="page-header"><div><button className="btn btn-ghost btn-sm" onClick={() => navigate('/')} style={{ marginBottom: '8px' }}><ArrowLeft size={16} /> Dashboard</button><h1 className="page-title">{project.title}</h1><p className="page-subtitle">{project.topic}</p></div></div>
      <PipelineStepper status={project.status} activeTab={activeTab} onTabChange={setActiveTab} />
      <ActiveTabComponent projectId={id} project={project} onStatusChange={loadProject} />
    </div>
  )
}
