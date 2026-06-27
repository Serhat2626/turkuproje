import EmptyState from '../common/EmptyState'
export default function AudioTab({ projectId }) {
  return (<div className="tab-content"><div className="tab-header"><h2 className="tab-title">🎵 Ses & Müzik</h2></div><EmptyState icon="🎵" title="Ses modülü hazırlanıyor" message="Seslendirme ve enstrümantal müzik yönetimi burada yapılacak. Müzikleri dinleyebilecek, lisans bilgilerini görebileceksiniz." /></div>)
}
