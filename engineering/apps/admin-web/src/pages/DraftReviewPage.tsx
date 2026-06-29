import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getContentDraft, updateContentDraft } from '../api/draftApi'
import { getContentProject } from '../api/contentApi'
import type { ContentDraftResponse, ContentProjectResponse } from '../types'
import FormCard from '../components/common/FormCard'
import ApiAlert from '../components/common/ApiAlert'
import SubmitButton from '../components/common/SubmitButton'
import StatusBadge from '../components/common/StatusBadge'
import LoadingSpinner from '../components/common/LoadingSpinner'

import { canEditDraft } from '../utils/actionVisibility'

function DraftReviewPage() {
  const { draftId } = useParams<{ draftId: string }>()
  const navigate = useNavigate()

  const [draft, setDraft] = useState<ContentDraftResponse | null>(null)
  const [project, setProject] = useState<ContentProjectResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [editing, setEditing] = useState(false)

  const [form, setForm] = useState({ title: '', body: '', summary: '' })
  const [original, setOriginal] = useState({ title: '', body: '', summary: '' })

  const [alert, setAlert] = useState<{ type: 'success' | 'error'; message: string } | null>(null)

  // ── Load ──────────────────────────────────────────────
  useEffect(() => {
    if (draftId) loadDraft(draftId)
  }, [draftId])

  async function loadDraft(id: string) {
    setLoading(true)
    setAlert(null)
    try {
      const res = await getContentDraft(id)
      const d = res.data!
      setDraft(d)
      const formData = { title: d.title, body: d.body, summary: d.summary ?? '' }
      setForm(formData)
      setOriginal(formData)

      // Load linked project for context
      if (d.contentProjectId) {
        try {
          const projRes = await getContentProject(d.contentProjectId)
          setProject(projRes.data)
        } catch {
          // Project load is optional; draft can still be viewed
        }
      }
    } catch {
      setAlert({ type: 'error', message: 'Draft yüklenemedi' })
    } finally {
      setLoading(false)
    }
  }

  // ── Helpers ───────────────────────────────────────────
  const isEditable = draft ? canEditDraft(draft.status) : false

  const isDirty = form.title !== original.title ||
    form.body !== original.body ||
    form.summary !== original.summary

  function handleChange(e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) {
    setForm(prev => ({ ...prev, [e.target.name]: e.target.value }))
  }

  function extractError(err: unknown): string {
    const apiErr = err as { message?: string; errors?: { field: string; message: string }[] | null }
    const msgs: string[] = []
    if (apiErr.errors) msgs.push(...apiErr.errors.map(e => `${e.field}: ${e.message}`))
    if (apiErr.message && msgs.length === 0) msgs.push(apiErr.message)
    return msgs.join(' • ') || 'Bir hata oluştu'
  }

  // ── Save ──────────────────────────────────────────────
  async function handleSave(e: React.FormEvent) {
    e.preventDefault()
    if (!draft || !draftId) return
    if (!isDirty) return

    // Client-side validation matching backend rules
    if (form.title !== original.title && !form.title.trim()) {
      setAlert({ type: 'error', message: 'Başlık boş olamaz' })
      return
    }
    if (form.body !== original.body && !form.body.trim()) {
      setAlert({ type: 'error', message: 'İçerik metni boş olamaz' })
      return
    }

    setSaving(true)
    setAlert(null)
    try {
      // Only send changed fields
      const payload: Record<string, string> = {}
      if (form.title !== original.title) payload.title = form.title
      if (form.body !== original.body) payload.body = form.body
      if (form.summary !== original.summary) payload.summary = form.summary

      await updateContentDraft(draftId, payload)
      setAlert({ type: 'success', message: 'Draft güncellendi. Status → EDITED' })
      setEditing(false)
      loadDraft(draftId)
    } catch (err) {
      setAlert({ type: 'error', message: extractError(err) })
    } finally {
      setSaving(false)
    }
  }

  function handleCancelEdit() {
    setForm(original)
    setEditing(false)
  }

  function goBackToProject() {
    if (draft?.contentProjectId) {
      navigate('/projects')
    }
  }

  // ── Render ────────────────────────────────────────────
  if (loading) return <div className="fade-in"><LoadingSpinner text="Draft yükleniyor…" /></div>

  if (!draft) {
    return (
      <div className="fade-in">
        {alert && <ApiAlert type={alert.type} message={alert.message} onClose={() => setAlert(null)} />}
        <div className="empty-state">
          <div className="empty-state-icon">📄</div>
          <div className="empty-state-title">Draft bulunamadı</div>
          <div className="empty-state-text">
            <button className="btn-action" onClick={() => navigate('/projects')}>← İçerik Projelerine Dön</button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="fade-in">
      {alert && <ApiAlert type={alert.type} message={alert.message} onClose={() => setAlert(null)} />}

      {/* Header */}
      <div className="page-header-row">
        <div className="page-header">
          <h1 className="page-title">Draft İnceleme</h1>
          <p className="page-subtitle">
            <button className="btn-action" onClick={goBackToProject}>← İçerik Projesine Dön</button>
          </p>
        </div>
        <div className="page-actions">
          <StatusBadge status={draft.status} />
        </div>
      </div>

      {/* Human review safety note */}
      <div className="api-alert api-alert-info" style={{ marginBottom: 16 }}>
        <span className="api-alert-icon">ℹ</span>
        <span className="api-alert-message">
          Draft düzenleme sadece içeriği değiştirir. Proje onayı ve yayına hazırlama işlemleri İçerik Projesi ekranından yapılmalıdır.
        </span>
      </div>

      {/* Draft metadata */}
      <FormCard icon="📋" title="Draft Bilgileri">
        <div className="detail-grid">
          <div className="detail-field">
            <span className="detail-label">Draft ID</span>
            <span className="detail-value"><code style={{ fontSize: '0.75rem' }}>{draft.id}</code></span>
          </div>
          <div className="detail-field">
            <span className="detail-label">Proje</span>
            <span className="detail-value">
              {project ? project.knowledgeItemTitle : (draft.contentProjectId ?? '—')}
            </span>
          </div>
          <div className="detail-field">
            <span className="detail-label">Proje Durumu</span>
            {project ? <StatusBadge status={project.status} /> : <span className="detail-value">—</span>}
          </div>
          <div className="detail-field">
            <span className="detail-label">Oluşturan</span>
            <span className="detail-value">{draft.generatedBy}</span>
          </div>
          <div className="detail-field">
            <span className="detail-label">Draft Durumu</span>
            <StatusBadge status={draft.status} />
          </div>
          <div className="detail-field">
            <span className="detail-label">Oluşturulma</span>
            <span className="detail-value">{new Date(draft.createdAt).toLocaleString('tr-TR')}</span>
          </div>
          {draft.updatedAt && (
            <div className="detail-field">
              <span className="detail-label">Güncellenme</span>
              <span className="detail-value">{new Date(draft.updatedAt).toLocaleString('tr-TR')}</span>
            </div>
          )}
        </div>
      </FormCard>

      {/* Draft content — view or edit */}
      {editing && isEditable ? (
        <FormCard icon="✏️" title="Draft Düzenle" subtitle="Değişiklik sonrası draft status → EDITED olur">
          <form onSubmit={handleSave}>
            <div className="form-group">
              <label className="form-label" htmlFor="draft-title">Başlık</label>
              <input
                id="draft-title" name="title" type="text" className="form-input"
                value={form.title} onChange={handleChange}
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="draft-summary">Özet</label>
              <textarea
                id="draft-summary" name="summary" className="form-textarea"
                value={form.summary} onChange={handleChange} rows={3}
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="draft-body">İçerik Metni</label>
              <textarea
                id="draft-body" name="body" className="form-textarea"
                value={form.body} onChange={handleChange} rows={12}
                style={{ fontFamily: 'monospace', fontSize: '0.875rem', lineHeight: '1.6' }}
              />
            </div>

            <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
              <SubmitButton loading={saving} text="Kaydet" loadingText="Kaydediliyor…" />
              <button type="button" className="btn btn-secondary" onClick={handleCancelEdit} disabled={saving}>
                İptal
              </button>
              {!isDirty && (
                <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Henüz değişiklik yapılmadı</span>
              )}
            </div>
          </form>
        </FormCard>
      ) : (
        <FormCard icon="📄" title="Draft İçeriği">
          {/* Title */}
          <div className="detail-field" style={{ marginBottom: 16 }}>
            <span className="detail-label">Başlık</span>
            <span className="detail-value" style={{ fontSize: '1.05rem', fontWeight: 500 }}>{draft.title}</span>
          </div>

          {/* Summary */}
          {draft.summary && (
            <div className="detail-field" style={{ marginBottom: 16 }}>
              <span className="detail-label">Özet</span>
              <span className="detail-value-long">{draft.summary}</span>
            </div>
          )}

          {/* Body */}
          <div className="detail-field" style={{ marginBottom: 16 }}>
            <span className="detail-label">İçerik Metni</span>
            <div className="detail-value-long" style={{ fontFamily: 'monospace', fontSize: '0.875rem' }}>
              {draft.body}
            </div>
          </div>

          {/* Edit button */}
          {isEditable && (
            <div className="detail-actions">
              <button className="btn btn-primary" onClick={() => setEditing(true)}>
                ✏️ Düzenle
              </button>
            </div>
          )}

          {!isEditable && (
            <div className="api-alert api-alert-info" style={{ marginTop: 12 }}>
              <span className="api-alert-icon">ℹ</span>
              <span className="api-alert-message">
                Bu draft "{draft.status}" durumunda olduğu için düzenlenemez. Sadece GENERATED ve REVISION_NEEDED durumlarında düzenleme yapılabilir.
              </span>
            </div>
          )}
        </FormCard>
      )}
    </div>
  )
}

export default DraftReviewPage
