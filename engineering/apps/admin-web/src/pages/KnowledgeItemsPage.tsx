import { useState, useEffect, useCallback } from 'react'
import {
  listKnowledgeItems,
  getKnowledgeItem,
  createKnowledgeItem,
  updateKnowledgeItem,
  markKnowledgeItemReady,
  archiveKnowledgeItem,
} from '../api/knowledgeApi'
import type { KnowledgeItemResponse, KnowledgeItemSummaryResponse } from '../types'
import { KNOWLEDGE_ITEM_TYPES, KNOWLEDGE_ITEM_STATUSES, LANGUAGES } from '../types'
import FormCard from '../components/common/FormCard'
import ApiAlert from '../components/common/ApiAlert'
import SubmitButton from '../components/common/SubmitButton'
import StatusBadge from '../components/common/StatusBadge'
import LoadingSpinner from '../components/common/LoadingSpinner'
import ConfirmDialog from '../components/common/ConfirmDialog'

type View = 'list' | 'create' | 'detail'

const EMPTY_FORM = {
  title: '', type: '', language: 'tr', summary: '', description: '',
  category: '', city: '', district: '', locationText: '', notes: '',
}

function KnowledgeItemsPage() {
  // ── State ─────────────────────────────────────────────
  const [view, setView] = useState<View>('list')
  const [items, setItems] = useState<KnowledgeItemSummaryResponse[]>([])
  const [listLoading, setListLoading] = useState(false)
  const [statusFilter, setStatusFilter] = useState('')
  const [searchQuery, setSearchQuery] = useState('')

  const [form, setForm] = useState(EMPTY_FORM)
  const [formLoading, setFormLoading] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)

  const [detail, setDetail] = useState<KnowledgeItemResponse | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)

  const [alert, setAlert] = useState<{ type: 'success' | 'error'; message: string } | null>(null)

  const [confirmDialog, setConfirmDialog] = useState<{
    open: boolean; title: string; message: string;
    variant: 'primary' | 'danger'; action: () => Promise<void>;
  }>({ open: false, title: '', message: '', variant: 'primary', action: async () => {} })
  const [confirmLoading, setConfirmLoading] = useState(false)

  // ── Data Loading ──────────────────────────────────────
  const loadList = useCallback(async () => {
    setListLoading(true)
    try {
      const res = await listKnowledgeItems(statusFilter || undefined)
      setItems(res.data ?? [])
    } catch {
      setAlert({ type: 'error', message: 'Liste yüklenemedi' })
    } finally {
      setListLoading(false)
    }
  }, [statusFilter])

  useEffect(() => {
    if (view === 'list') loadList()
  }, [view, loadList])

  async function loadDetail(id: string) {
    setDetailLoading(true)
    setAlert(null)
    try {
      const res = await getKnowledgeItem(id)
      setDetail(res.data)
    } catch {
      setAlert({ type: 'error', message: 'Detay yüklenemedi' })
    } finally {
      setDetailLoading(false)
    }
  }

  // ── Navigation ────────────────────────────────────────
  function goList() {
    setView('list')
    setDetail(null)
    setEditingId(null)
    setAlert(null)
  }

  function goCreate() {
    setForm(EMPTY_FORM)
    setEditingId(null)
    setView('create')
    setAlert(null)
  }

  function goDetail(id: string) {
    setView('detail')
    setAlert(null)
    loadDetail(id)
  }

  function goEdit(item: KnowledgeItemResponse) {
    setForm({
      title: item.title,
      type: item.type,
      language: item.language,
      summary: item.summary ?? '',
      description: item.description ?? '',
      category: item.category ?? '',
      city: item.city ?? '',
      district: item.district ?? '',
      locationText: item.locationText ?? '',
      notes: item.notes ?? '',
    })
    setEditingId(item.id)
    setView('create')
    setAlert(null)
  }

  // ── Form Handlers ─────────────────────────────────────
  function handleChange(e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) {
    setForm(prev => ({ ...prev, [e.target.name]: e.target.value }))
  }

  function extractError(err: unknown): string {
    const apiErr = err as { message?: string; errors?: { field: string; message: string }[] | null }
    const msgs: string[] = []
    if (apiErr.errors) msgs.push(...apiErr.errors.map(e => `${e.field}: ${e.message}`))
    if (apiErr.message && msgs.length === 0) msgs.push(apiErr.message)
    return msgs.join(' • ') || 'Bir hata oluştu'
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setFormLoading(true)
    setAlert(null)
    try {
      if (editingId) {
        await updateKnowledgeItem(editingId, form)
        setAlert({ type: 'success', message: 'Bilgi öğesi güncellendi.' })
        goDetail(editingId)
      } else {
        const res = await createKnowledgeItem(form)
        setAlert({ type: 'success', message: `"${res.data!.title}" başarıyla oluşturuldu.` })
        goDetail(res.data!.id)
      }
    } catch (err) {
      setAlert({ type: 'error', message: extractError(err) })
    } finally {
      setFormLoading(false)
    }
  }

  // ── Action Handlers (action endpoints only) ───────────
  function openConfirm(title: string, message: string, variant: 'primary' | 'danger', action: () => Promise<void>) {
    setConfirmDialog({ open: true, title, message, variant, action })
  }

  async function handleConfirm() {
    setConfirmLoading(true)
    try {
      await confirmDialog.action()
    } finally {
      setConfirmLoading(false)
      setConfirmDialog(prev => ({ ...prev, open: false }))
    }
  }

  async function handleMarkReady(id: string) {
    openConfirm('Hazır Yap', 'Bu bilgi öğesini READY statüsüne geçirmek istediğinize emin misiniz?', 'primary', async () => {
      try {
        await markKnowledgeItemReady(id)
        setAlert({ type: 'success', message: 'Bilgi öğesi READY yapıldı.' })
        if (view === 'detail') loadDetail(id)
        else loadList()
      } catch (err) {
        setAlert({ type: 'error', message: extractError(err) })
      }
    })
  }

  async function handleArchive(id: string) {
    openConfirm('Arşivle', 'Bu bilgi öğesini arşivlemek istediğinize emin misiniz? Bu işlem geri alınamaz.', 'danger', async () => {
      try {
        await archiveKnowledgeItem(id)
        setAlert({ type: 'success', message: 'Bilgi öğesi arşivlendi.' })
        if (view === 'detail') loadDetail(id)
        else loadList()
      } catch (err) {
        setAlert({ type: 'error', message: extractError(err) })
      }
    })
  }

  // ── Helpers ───────────────────────────────────────────
  const typeLabel = (val: string) => KNOWLEDGE_ITEM_TYPES.find(t => t.value === val)?.label ?? val

  const filteredItems = searchQuery
    ? items.filter(i => i.title.toLowerCase().includes(searchQuery.toLowerCase()))
    : items

  // ── Render: LIST ──────────────────────────────────────
  function renderList() {
    return (
      <>
        <div className="page-header-row">
          <div className="page-header">
            <h1 className="page-title">Bilgi Bankası</h1>
            <p className="page-subtitle">Kültürel bilgi öğelerini yönetin</p>
          </div>
          <div className="page-actions">
            <button className="btn btn-primary" onClick={goCreate}>+ Yeni Öğe</button>
          </div>
        </div>

        {/* Filter bar */}
        <div className="filter-bar">
          <select
            className="form-select"
            value={statusFilter}
            onChange={e => setStatusFilter(e.target.value)}
          >
            <option value="">Tüm Durumlar</option>
            {KNOWLEDGE_ITEM_STATUSES.map(s => (
              <option key={s.value} value={s.value}>{s.label}</option>
            ))}
          </select>
          <input
            type="text"
            className="form-input"
            placeholder="Başlık ara…"
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
          />
        </div>

        {listLoading ? (
          <LoadingSpinner text="Yükleniyor…" />
        ) : filteredItems.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">📚</div>
            <div className="empty-state-title">Bilgi öğesi bulunamadı</div>
            <div className="empty-state-text">Yeni bir konu ekleyerek başlayın.</div>
          </div>
        ) : (
          <div className="data-table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Başlık</th>
                  <th>Tür</th>
                  <th>Kategori</th>
                  <th>Durum</th>
                  <th>Dil</th>
                  <th>Aksiyonlar</th>
                </tr>
              </thead>
              <tbody>
                {filteredItems.map(item => (
                  <tr key={item.id}>
                    <td>
                      <span className="cell-title" style={{ cursor: 'pointer' }} onClick={() => goDetail(item.id)}>
                        {item.title}
                      </span>
                    </td>
                    <td className="cell-meta">{typeLabel(item.type)}</td>
                    <td className="cell-meta">{item.category ?? '—'}</td>
                    <td><StatusBadge status={item.status} /></td>
                    <td className="cell-meta">{item.language.toUpperCase()}</td>
                    <td className="cell-actions">
                      <button className="btn-action" onClick={() => goDetail(item.id)}>Detay</button>
                      {item.status === 'DRAFT' && (
                        <button className="btn-action btn-action-success" onClick={() => handleMarkReady(item.id)}>
                          Hazır Yap
                        </button>
                      )}
                      {(item.status === 'DRAFT' || item.status === 'READY') && (
                        <button className="btn-action btn-action-danger" onClick={() => handleArchive(item.id)}>
                          Arşivle
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </>
    )
  }

  // ── Render: CREATE / EDIT ─────────────────────────────
  function renderForm() {
    const isEditing = !!editingId
    return (
      <>
        <div className="page-header">
          <h1 className="page-title">{isEditing ? 'Bilgi Öğesi Düzenle' : 'Yeni Bilgi Öğesi'}</h1>
          <p className="page-subtitle">
            <button className="btn-action" onClick={goList}>← Listeye Dön</button>
          </p>
        </div>

        <FormCard icon={isEditing ? '✏️' : '🔍'} title={isEditing ? 'Düzenle' : 'Yeni Öğe'} subtitle="Bilgi bankasına kültürel bilgi öğesi ekleyin veya düzenleyin">
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="form-label" htmlFor="ki-title">Başlık *</label>
              <input id="ki-title" name="title" type="text" className="form-input"
                value={form.title} onChange={handleChange} placeholder="Örn: Çanakkale Türküsü" required />
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="ki-type">Tür *</label>
                <select id="ki-type" name="type" className="form-select" value={form.type} onChange={handleChange} required>
                  <option value="">Seçiniz…</option>
                  {KNOWLEDGE_ITEM_TYPES.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label" htmlFor="ki-language">Dil *</label>
                <select id="ki-language" name="language" className="form-select" value={form.language} onChange={handleChange} required>
                  <option value="">Seçiniz…</option>
                  {LANGUAGES.map(l => <option key={l.value} value={l.value}>{l.label}</option>)}
                </select>
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="ki-category">Kategori</label>
                <input id="ki-category" name="category" type="text" className="form-input"
                  value={form.category} onChange={handleChange} placeholder="türkü, gelenek, tarihî olay" />
              </div>
              <div className="form-group">
                <label className="form-label" htmlFor="ki-city">İl</label>
                <input id="ki-city" name="city" type="text" className="form-input"
                  value={form.city} onChange={handleChange} placeholder="Örn: Çanakkale" />
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="ki-district">İlçe</label>
                <input id="ki-district" name="district" type="text" className="form-input"
                  value={form.district} onChange={handleChange} />
              </div>
              <div className="form-group">
                <label className="form-label" htmlFor="ki-locationText">Konum Metni</label>
                <input id="ki-locationText" name="locationText" type="text" className="form-input"
                  value={form.locationText} onChange={handleChange} />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="ki-summary">Özet</label>
              <textarea id="ki-summary" name="summary" className="form-textarea"
                value={form.summary} onChange={handleChange} placeholder="Kısa açıklama…" rows={3} />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="ki-description">Açıklama</label>
              <textarea id="ki-description" name="description" className="form-textarea"
                value={form.description} onChange={handleChange} placeholder="Detaylı açıklama…" rows={5} />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="ki-notes">Notlar</label>
              <textarea id="ki-notes" name="notes" className="form-textarea"
                value={form.notes} onChange={handleChange} placeholder="Ek notlar…" rows={3} />
            </div>

            <SubmitButton loading={formLoading} text={isEditing ? 'Güncelle' : 'Oluştur'} loadingText={isEditing ? 'Güncelleniyor…' : 'Oluşturuluyor…'} />
          </form>
        </FormCard>
      </>
    )
  }

  // ── Render: DETAIL ────────────────────────────────────
  function renderDetail() {
    if (detailLoading) return <LoadingSpinner text="Detay yükleniyor…" />
    if (!detail) return <div className="empty-state"><div className="empty-state-title">Öğe bulunamadı</div></div>

    const isArchived = detail.status === 'ARCHIVED'

    return (
      <>
        <div className="page-header-row">
          <div className="page-header">
            <h1 className="page-title">{detail.title}</h1>
            <p className="page-subtitle">
              <button className="btn-action" onClick={goList}>← Listeye Dön</button>
            </p>
          </div>
          <div className="page-actions">
            <StatusBadge status={detail.status} />
          </div>
        </div>

        <FormCard icon="📋" title="Detay Bilgileri">
          <div className="detail-grid">
            <div className="detail-field">
              <span className="detail-label">Tür</span>
              <span className="detail-value">{typeLabel(detail.type)}</span>
            </div>
            <div className="detail-field">
              <span className="detail-label">Dil</span>
              <span className="detail-value">{detail.language.toUpperCase()}</span>
            </div>
            <div className="detail-field">
              <span className="detail-label">Kategori</span>
              <span className="detail-value">{detail.category ?? '—'}</span>
            </div>
            <div className="detail-field">
              <span className="detail-label">İl</span>
              <span className="detail-value">{detail.city ?? '—'}</span>
            </div>
            <div className="detail-field">
              <span className="detail-label">İlçe</span>
              <span className="detail-value">{detail.district ?? '—'}</span>
            </div>
            <div className="detail-field">
              <span className="detail-label">Konum</span>
              <span className="detail-value">{detail.locationText ?? '—'}</span>
            </div>
            <div className="detail-field">
              <span className="detail-label">Slug</span>
              <span className="detail-value"><code>{detail.slug}</code></span>
            </div>
            <div className="detail-field">
              <span className="detail-label">ID</span>
              <span className="detail-value"><code style={{ fontSize: '0.75rem' }}>{detail.id}</code></span>
            </div>
          </div>

          {detail.summary && (
            <div className="detail-field" style={{ marginBottom: 16 }}>
              <span className="detail-label">Özet</span>
              <span className="detail-value-long">{detail.summary}</span>
            </div>
          )}

          {detail.description && (
            <div className="detail-field" style={{ marginBottom: 16 }}>
              <span className="detail-label">Açıklama</span>
              <span className="detail-value-long">{detail.description}</span>
            </div>
          )}

          {detail.notes && (
            <div className="detail-field" style={{ marginBottom: 16 }}>
              <span className="detail-label">Notlar</span>
              <span className="detail-value-long">{detail.notes}</span>
            </div>
          )}

          {/* Action buttons — no generic status update, action endpoints only */}
          <div className="detail-actions">
            {!isArchived && (
              <button className="btn btn-primary" onClick={() => goEdit(detail)}>✏️ Düzenle</button>
            )}
            {detail.status === 'DRAFT' && (
              <button className="btn-action btn-action-success" onClick={() => handleMarkReady(detail.id)}>
                ✓ Hazır Yap
              </button>
            )}
            {(detail.status === 'DRAFT' || detail.status === 'READY') && (
              <button className="btn-action btn-action-danger" onClick={() => handleArchive(detail.id)}>
                🗄 Arşivle
              </button>
            )}
          </div>
        </FormCard>
      </>
    )
  }

  // ── Main Render ───────────────────────────────────────
  return (
    <div className="fade-in">
      {alert && <ApiAlert type={alert.type} message={alert.message} onClose={() => setAlert(null)} />}

      {view === 'list' && renderList()}
      {view === 'create' && renderForm()}
      {view === 'detail' && renderDetail()}

      <ConfirmDialog
        open={confirmDialog.open}
        title={confirmDialog.title}
        message={confirmDialog.message}
        variant={confirmDialog.variant}
        loading={confirmLoading}
        onConfirm={handleConfirm}
        onCancel={() => setConfirmDialog(prev => ({ ...prev, open: false }))}
      />
    </div>
  )
}

export default KnowledgeItemsPage
