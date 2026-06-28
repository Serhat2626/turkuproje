import { useState } from 'react'
import { registerMediaAsset } from '../api/mediaApi'
import type { MediaAssetResponse } from '../types'
import { ASSET_TYPES, STORAGE_PROVIDERS } from '../types'
import FormCard from '../components/common/FormCard'
import ApiAlert from '../components/common/ApiAlert'
import SubmitButton from '../components/common/SubmitButton'

function MediaAssetsPage() {
  const [form, setForm] = useState({
    assetType: '', storageProvider: '', bucket: 'anadolu-media', objectKey: '',
    fileName: '', mimeType: '', sizeBytes: '', durationSeconds: ''
  })
  const [loading, setLoading] = useState(false)
  const [alert, setAlert] = useState<{ type: 'success' | 'error'; message: string } | null>(null)
  const [createdItem, setCreatedItem] = useState<MediaAssetResponse | null>(null)

  function handleChange(e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) {
    setForm(prev => ({ ...prev, [e.target.name]: e.target.value }))
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setLoading(true)
    setAlert(null)
    setCreatedItem(null)
    try {
      const payload = {
        assetType: form.assetType,
        storageProvider: form.storageProvider,
        bucket: form.bucket,
        objectKey: form.objectKey,
        fileName: form.fileName || undefined,
        mimeType: form.mimeType || undefined,
        sizeBytes: form.sizeBytes === '' ? null : Number(form.sizeBytes),
        durationSeconds: form.durationSeconds === '' ? null : Number(form.durationSeconds),
      }
      const res = await registerMediaAsset(payload)
      setCreatedItem(res.data)
      setAlert({ type: 'success', message: `Medya varlığı başarıyla kaydedildi.` })
      setForm({
        assetType: '', storageProvider: '', bucket: 'anadolu-media', objectKey: '',
        fileName: '', mimeType: '', sizeBytes: '', durationSeconds: ''
      })
    } catch (err: unknown) {
      const apiErr = err as { message?: string; errors?: { field: string; message: string }[] | null }
      const msgs: string[] = []
      if (apiErr.errors) msgs.push(...apiErr.errors.map(e => `${e.field}: ${e.message}`))
      if (apiErr.message && msgs.length === 0) msgs.push(apiErr.message)
      setAlert({ type: 'error', message: msgs.join(' • ') || 'Bir hata oluştu' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1 className="page-title">Medya Varlıkları</h1>
        <p className="page-subtitle">Yeni medya varlığı kaydedin</p>
      </div>

      {alert && <ApiAlert type={alert.type} message={alert.message} onClose={() => setAlert(null)} />}

      <FormCard icon="🎨" title="Yeni Medya Kaydı" subtitle="Bir medya dosyasının metadata bilgisini sisteme kaydedin">
        <form onSubmit={handleSubmit}>
          <div className="form-row">
            <div className="form-group">
              <label className="form-label" htmlFor="ma-assetType">Varlık Türü *</label>
              <select
                id="ma-assetType"
                name="assetType"
                className="form-select"
                value={form.assetType}
                onChange={handleChange}
                required
              >
                <option value="">Seçiniz...</option>
                {ASSET_TYPES.map(t => (
                  <option key={t.value} value={t.value}>{t.label}</option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="ma-storageProvider">Depolama *</label>
              <select
                id="ma-storageProvider"
                name="storageProvider"
                className="form-select"
                value={form.storageProvider}
                onChange={handleChange}
                required
              >
                <option value="">Seçiniz...</option>
                {STORAGE_PROVIDERS.map(t => (
                  <option key={t.value} value={t.value}>{t.label}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label className="form-label" htmlFor="ma-bucket">Bucket *</label>
              <input
                id="ma-bucket"
                name="bucket"
                type="text"
                className="form-input"
                value={form.bucket}
                onChange={handleChange}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="ma-objectKey">Nesne Anahtarı *</label>
              <input
                id="ma-objectKey"
                name="objectKey"
                type="text"
                className="form-input"
                value={form.objectKey}
                onChange={handleChange}
                placeholder="images/canakkale/scene-001.png"
                required
              />
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label className="form-label" htmlFor="ma-fileName">Dosya Adı</label>
              <input
                id="ma-fileName"
                name="fileName"
                type="text"
                className="form-input"
                value={form.fileName}
                onChange={handleChange}
                placeholder="scene-001.png"
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="ma-mimeType">MIME Türü</label>
              <input
                id="ma-mimeType"
                name="mimeType"
                type="text"
                className="form-input"
                value={form.mimeType}
                onChange={handleChange}
                placeholder="image/png"
              />
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label className="form-label" htmlFor="ma-sizeBytes">Boyut (byte)</label>
              <input
                id="ma-sizeBytes"
                name="sizeBytes"
                type="number"
                className="form-input"
                value={form.sizeBytes}
                onChange={handleChange}
                min={0}
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="ma-durationSeconds">Süre (saniye)</label>
              <input
                id="ma-durationSeconds"
                name="durationSeconds"
                type="number"
                className="form-input"
                value={form.durationSeconds}
                onChange={handleChange}
                min={0}
              />
            </div>
          </div>

          <SubmitButton loading={loading} text="Kaydet" loadingText="Kaydediliyor…" />
        </form>
      </FormCard>

      {createdItem && (
        <div className="created-item-info">
          <div className="created-item-row">
            <span className="created-item-label">ID:</span>
            <code className="created-item-value">{createdItem.id}</code>
          </div>
          <div className="created-item-row">
            <span className="created-item-label">Nesne:</span>
            <code className="created-item-value">{createdItem.objectKey}</code>
          </div>
          <div className="created-item-row">
            <span className="created-item-label">Tür:</span>
            <code className="created-item-value">{createdItem.assetType}</code>
          </div>
        </div>
      )}
    </div>
  )
}

export default MediaAssetsPage
