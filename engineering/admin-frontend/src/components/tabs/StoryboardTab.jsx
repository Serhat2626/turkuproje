import EmptyState from '../common/EmptyState.jsx'
export default function StoryboardTab({ projectId }) {
  return (<div className="tab-content"><div className="tab-header"><h2 className="tab-title">🎬 Storyboard</h2></div><EmptyState icon="🎬" title="Storyboard modülü hazırlanıyor" message="Senaryo onaylandıktan sonra sahne sahne storyboard burada oluşturulacak" /></div>)
}
