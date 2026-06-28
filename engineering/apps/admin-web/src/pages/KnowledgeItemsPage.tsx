import { useState } from 'react'
import { createKnowledgeItem } from '../api/knowledgeApi'
import type { KnowledgeItemResponse } from '../types'
import { KNOWLEDGE_ITEM_TYPES, LANGUAGES } from '../types'
import FormCard from '../components/common/FormCard'
import ApiAlert from '../components/common/ApiAlert'
import SubmitButton from '../components/common/SubmitButton'
import StatusBadge from '../components/common/StatusBadge'

function KnowledgeItemsPage() {
  const [form, setForm] = useState({ title: '', type: '', language: 'tr', summary: '' })
  const [loading, setLoading] = useState(false)
  const [alert, setAlert] = useState<{ type: 'success' | 'error'; message: string } | null>(null)
  const [createdItem, setCreatedItem] = useState<KnowledgeItemResponse | null>(null)

  function handleChange(e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) {
    setForm(prev => ({ ...prev, [e.target.name]: e.target.value }))
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setLoading(true)
    setAlert(null)
    setCreatedItem(null)
    try {
      const res = await createKnowledgeItem(form)
      setCreatedItem(res.data)
      setAlert({ type: 'success', message: `"${res.data!.title}" başarıyla oluşturuldu.` })
      setForm({ title: '', type: '', language: 'tr', summary: '' })
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
        <h1 className="page-title">Bilgi Bankası</h1>
        <p className="page-subtitle">Yeni bilgi öğesi oluşturun</p>
      </div>

      {alert && <ApiAlert type={alert.type} message={alert.message} onClose={() => setAlert(null)} />}

      <FormCard icon="🔍" title="Yeni Bilgi Öğesi" subtitle="Bilgi bankasına yeni bir kültürel bilgi öğesi ekleyin">
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label" htmlFor="ki-title">Başlık *</label>
            <input
              id="ki-title"
              name="title"
              type="text"
              className="form-input"
              value={form.title}
              onChange={handleChange}
              placeholder="Örn: Çanakkale Türküsü"
              required
            />
          </div>

          <div className="form-row">
            <div className="form-group">
              <label className="form-label" htmlFor="ki-type">Tür *</label>
              <select
                id="ki-type"
                name="type"
                className="form-select"
                value={form.type}
                onChange={handleChange}
                required
              >
                <option value="">Seçiniz...</option>
                {KNOWLEDGE_ITEM_TYPES.map(t => (
                  <option key={t.value} value={t.value}>{t.label}</option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="ki-language">Dil *</label>
              <select
                id="ki-language"
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
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="ki-summary">Özet</label>
            <textarea
              id="ki-summary"
              name="summary"
              className="form-textarea"
              value={form.summary}
              onChange={handleChange}
              placeholder="Kısa açıklama..."
              rows={4}
            />
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
            <span className="created-item-label">Durum:</span>
            <StatusBadge status={createdItem.status} />
          </div>
        </div>
      )}
    </div>
  )
}

export default KnowledgeItemsPage
