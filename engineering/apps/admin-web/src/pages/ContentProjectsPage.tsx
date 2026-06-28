import { useState } from 'react'
import { createContentProject } from '../api/contentApi'
import type { ContentProjectResponse } from '../types'
import { CONTENT_TYPES, TARGET_PLATFORMS, LANGUAGES } from '../types'
import FormCard from '../components/common/FormCard'
import ApiAlert from '../components/common/ApiAlert'
import SubmitButton from '../components/common/SubmitButton'
import StatusBadge from '../components/common/StatusBadge'

function ContentProjectsPage() {
  const [form, setForm] = useState({
    knowledgeItemId: '', title: '', contentType: '', targetPlatform: '', language: 'tr'
  })
  const [loading, setLoading] = useState(false)
  const [alert, setAlert] = useState<{ type: 'success' | 'error'; message: string } | null>(null)
  const [createdItem, setCreatedItem] = useState<ContentProjectResponse | null>(null)

  function handleChange(e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) {
    setForm(prev => ({ ...prev, [e.target.name]: e.target.value }))
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setLoading(true)
    setAlert(null)
    setCreatedItem(null)
    try {
      const res = await createContentProject(form)
      setCreatedItem(res.data)
      setAlert({ type: 'success', message: `"${res.data!.title}" başarıyla oluşturuldu.` })
      setForm({ knowledgeItemId: '', title: '', contentType: '', targetPlatform: '', language: 'tr' })
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
        <h1 className="page-title">İçerik Projeleri</h1>
        <p className="page-subtitle">Yeni içerik projesi oluşturun</p>
      </div>

      {alert && <ApiAlert type={alert.type} message={alert.message} onClose={() => setAlert(null)} />}

      <FormCard icon="📚" title="Yeni İçerik Projesi" subtitle="Bir bilgi öğesinden yeni içerik projesi başlatın">
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label" htmlFor="cp-knowledgeItemId">Bilgi Öğesi ID *</label>
            <input
              id="cp-knowledgeItemId"
              name="knowledgeItemId"
              type="text"
              className="form-input"
              value={form.knowledgeItemId}
              onChange={handleChange}
              placeholder="Knowledge Item UUID"
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="cp-title">Başlık *</label>
            <input
              id="cp-title"
              name="title"
              type="text"
              className="form-input"
              value={form.title}
              onChange={handleChange}
              placeholder="Örn: Çanakkale Türküsü Hikâyesi"
              required
            />
          </div>

          <div className="form-row">
            <div className="form-group">
              <label className="form-label" htmlFor="cp-contentType">İçerik Türü *</label>
              <select
                id="cp-contentType"
                name="contentType"
                className="form-select"
                value={form.contentType}
                onChange={handleChange}
                required
              >
                <option value="">Seçiniz...</option>
                {CONTENT_TYPES.map(t => (
                  <option key={t.value} value={t.value}>{t.label}</option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="cp-targetPlatform">Hedef Platform *</label>
              <select
                id="cp-targetPlatform"
                name="targetPlatform"
                className="form-select"
                value={form.targetPlatform}
                onChange={handleChange}
                required
              >
                <option value="">Seçiniz...</option>
                {TARGET_PLATFORMS.map(t => (
                  <option key={t.value} value={t.value}>{t.label}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="cp-language">Dil *</label>
            <select
              id="cp-language"
              name="language"
              className="form-select"
              value={form.language}
              onChange={handleChange}
              required
            >
              <option value="">Seçiniz...</option>
              {LANGUAGES.map(l => (
                <option key={l.value} value={l.value}>{l.label}</option>
              ))}
            </select>
          </div>

          <SubmitButton loading={loading} text="Oluştur" loadingText="Oluşturuluyor…" />
        </form>
      </FormCard>

      {createdItem && (
        <div className="created-item-info">
          <div className="created-item-row">
            <span className="created-item-label">ID:</span>
            <code className="created-item-value">{createdItem.id}</code>
          </div>
          <div className="created-item-row">
            <span className="created-item-label">Slug:</span>
            <code className="created-item-value">{createdItem.slug}</code>
          </div>
          <div className="created-item-row">
            <span className="created-item-label">Tür:</span>
            <code className="created-item-value">{createdItem.contentType}</code>
          </div>
          <div className="created-item-row">
            <span className="created-item-label">Durum:</span>
            <StatusBadge status={createdItem.status} />
          </div>
        </div>
      )}
    </div>
  )
}

export default ContentProjectsPage
