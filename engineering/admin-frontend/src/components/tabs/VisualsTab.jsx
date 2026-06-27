import EmptyState from '../common/EmptyState.jsx'
export default function VisualsTab({ projectId }) {
  return (<div className="tab-content"><div className="tab-header"><h2 className="tab-title">🖼️ Görseller & Promptlar</h2></div><EmptyState icon="🖼️" title="Görsel modülü hazırlanıyor" message="Her sahne için görsel/video promptları ve üretilen görseller burada görünecek" /></div>)
}
