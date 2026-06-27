import EmptyState from '../common/EmptyState.jsx'
export default function VideoTab({ projectId }) {
  return (<div className="tab-content"><div className="tab-header"><h2 className="tab-title">🎥 Video</h2></div><EmptyState icon="🎥" title="Video modülü hazırlanıyor" message="Video önizleme, indirme ve YouTube/Shorts formatları burada yönetilecek" /></div>)
}
