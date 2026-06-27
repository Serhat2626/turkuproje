import EmptyState from '../common/EmptyState'
export default function PublishTab({ projectId }) {
  return (<div className="tab-content"><div className="tab-header"><h2 className="tab-title">📤 Yayın Paketi</h2></div><EmptyState icon="📤" title="Yayın modülü hazırlanıyor" message="YouTube, Instagram, Facebook yayın paketleri burada oluşturulacak. Platform bazlı başlık, açıklama, hashtag ve yayın zamanlaması." /></div>)
}
