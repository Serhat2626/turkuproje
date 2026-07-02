import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  listContentProjects, getContentProject, createContentProject,
  generateAiDraft, retryAiDraft, startContentProjectReview,
  approveContentProject, requestContentProjectRevision, rejectContentProject,
  markContentProjectReadyForPublish, cancelContentProject,
} from '../api/contentApi'
import { listKnowledgeItems } from '../api/knowledgeApi'
import type {
  ContentProjectResponse, ContentProjectSummaryResponse,
  KnowledgeItemSummaryResponse, AiGenerationSummaryResponse,
} from '../types'
import {
  CONTENT_PURPOSES, TARGET_AUDIENCES, CONTENT_TONES,
  CONTENT_PROJECT_STATUSES, LANGUAGES,
} from '../types'
import FormCard from '../components/common/FormCard'
import ApiAlert from '../components/common/ApiAlert'
import SubmitButton from '../components/common/SubmitButton'
import StatusBadge from '../components/common/StatusBadge'
import LoadingSpinner from '../components/common/LoadingSpinner'
import ConfirmDialog from '../components/common/ConfirmDialog'
import NoteDialog from '../components/common/NoteDialog'
import ScenarioWorkflowSection from '../components/scenario/ScenarioWorkflowSection'

type View = 'list' | 'create' | 'detail'

const EMPTY_FORM = {
  knowledgeItemId: '', contentPurpose: '', targetAudience: '',
  tone: '', language: 'tr', formatHint: '', lengthHint: '',
}

import {
  canGenerateAiDraft, canRetryAiDraft, canStartReview,
  canApproveProject, canRequestRevision, canRejectProject,
  canMarkReadyForPublish, canCancelProject,
} from '../utils/actionVisibility'

function ContentProjectsPage() {
  const navigate = useNavigate()
  // ── State ─────────────────────────────────────────────
  const [view, setView] = useState<View>('list')
  const [items, setItems] = useState<ContentProjectSummaryResponse[]>([])
  const [listLoading, setListLoading] = useState(false)
  const [statusFilter, setStatusFilter] = useState('')

  const [form, setForm] = useState(EMPTY_FORM)
  const [formLoading, setFormLoading] = useState(false)
  const [readyKIs, setReadyKIs] = useState<KnowledgeItemSummaryResponse[]>([])
  const [kiLoading, setKiLoading] = useState(false)

  const [detail, setDetail] = useState<ContentProjectResponse | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [actionLoading, setActionLoading] = useState(false)

  const [aiResult, setAiResult] = useState<AiGenerationSummaryResponse | null>(null)

  const [alert, setAlert] = useState<{ type: 'success' | 'error'; message: string } | null>(null)

  // Dialogs
  const [confirmDialog, setConfirmDialog] = useState<{
    open: boolean; title: string; message: string;
    variant: 'primary' | 'danger'; action: () => Promise<void>;
  }>({ open: false, title: '', message: '', variant: 'primary', action: async () => {} })
  const [confirmLoading, setConfirmLoading] = useState(false)

  const [noteDialog, setNoteDialog] = useState<{
    open: boolean; title: string; label: string;
    variant: 'primary' | 'danger';
    action: (note: string) => Promise<void>;
  }>({ open: false, title: '', label: '', variant: 'primary', action: async () => {} })
  const [noteLoading, setNoteLoading] = useState(false)

  // ── Data Loading ──────────────────────────────────────
  const loadList = useCallback(async () => {
    setListLoading(true)
    try {
      const res = await listContentProjects(statusFilter || undefined)
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
    setAiResult(null)
    try {
      const res = await getContentProject(id)
      setDetail(res.data)
    } catch {
      setAlert({ type: 'error', message: 'Detay yüklenemedi' })
    } finally {
      setDetailLoading(false)
    }
  }

  async function loadReadyKIs() {
    setKiLoading(true)
    try {
      const res = await listKnowledgeItems('READY')
      setReadyKIs(res.data ?? [])
    } catch {
      setReadyKIs([])
    } finally {
      setKiLoading(false)
    }
  }

  // ── Navigation ────────────────────────────────────────
  function goList() { setView('list'); setDetail(null); setAiResult(null); setAlert(null) }
  function goCreate() { setForm(EMPTY_FORM); setView('create'); setAlert(null); loadReadyKIs() }
  function goDetail(id: string) { setView('detail'); setAlert(null); loadDetail(id) }

  // ── Helpers ───────────────────────────────────────────
  function handleChange(e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) {
    setForm(prev => ({ ...prev, [e.target.name]: e.target.value }))
  }

  function extractError(err: unknown): string {
    const apiErr = err as { message?: string; errors?: { field: string; message: string }[] | null }
    const msgs: string[] = []
    if (apiErr.errors) msgs.push(...apiErr.errors.map(e => `${e.field}: ${e.message}`))
    if (apiErr.message && msgs.length === 0) msgs.push(apiErr.message)
    return msgs.join(' • ') || 'Bir hata oluştu'
  }

  const purposeLabel = (v: string | null) => CONTENT_PURPOSES.find(p => p.value === v)?.label ?? v ?? '—'
  const audienceLabel = (v: string | null) => TARGET_AUDIENCES.find(a => a.value === v)?.label ?? v ?? '—'
  const toneLabel = (v: string | null) => CONTENT_TONES.find(t => t.value === v)?.label ?? v ?? '—'

  // ── Form Submit ───────────────────────────────────────
  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setFormLoading(true)
    setAlert(null)
    try {
      const res = await createContentProject({
        knowledgeItemId: form.knowledgeItemId,
        contentPurpose: form.contentPurpose || undefined,
        targetAudience: form.targetAudience || undefined,
        tone: form.tone || undefined,
        language: form.language,
        formatHint: form.formatHint || undefined,
        lengthHint: form.lengthHint || undefined,
      })
      setAlert({ type: 'success', message: 'Proje başarıyla oluşturuldu.' })
      goDetail(res.data!.id)
    } catch (err) {
      setAlert({ type: 'error', message: extractError(err) })
    } finally {
      setFormLoading(false)
    }
  }

  // ── Action Helpers ────────────────────────────────────
  function openConfirm(title: string, message: string, variant: 'primary' | 'danger', action: () => Promise<void>) {
    setConfirmDialog({ open: true, title, message, variant, action })
  }

  async function handleConfirm() {
    setConfirmLoading(true)
    try { await confirmDialog.action() }
    finally { setConfirmLoading(false); setConfirmDialog(p => ({ ...p, open: false })) }
  }

  function openNote(title: string, label: string, variant: 'primary' | 'danger', action: (note: string) => Promise<void>) {
    setNoteDialog({ open: true, title, label, variant, action })
  }

  async function handleNote(note: string) {
    setNoteLoading(true)
    try { await noteDialog.action(note) }
    finally { setNoteLoading(false); setNoteDialog(p => ({ ...p, open: false })) }
  }

  // ── Actions (action endpoints only — no generic status update) ──
  async function handleGenerateAiDraft(id: string) {
    setActionLoading(true)
    setAlert(null)
    setAiResult(null)
    try {
      const res = await generateAiDraft(id)
      setAiResult(res.data)
      if (res.data?.success) {
        setAlert({ type: 'success', message: 'AI draft başarıyla oluşturuldu.' })
      } else {
        setAlert({ type: 'error', message: res.data?.message ?? 'AI üretimi başarısız.' })
      }
      loadDetail(id)
    } catch (err) {
      setAlert({ type: 'error', message: extractError(err) })
    } finally {
      setActionLoading(false)
    }
  }

  async function handleRetry(id: string) {
    setActionLoading(true)
    setAlert(null)
    setAiResult(null)
    try {
      const res = await retryAiDraft(id)
      setAiResult(res.data)
      if (res.data?.success) {
        setAlert({ type: 'success', message: 'Retry başarılı, yeni draft oluşturuldu.' })
      } else {
        setAlert({ type: 'error', message: res.data?.message ?? 'Retry başarısız.' })
      }
      loadDetail(id)
    } catch (err) {
      setAlert({ type: 'error', message: extractError(err) })
    } finally {
      setActionLoading(false)
    }
  }

  async function handleStartReview(id: string) {
    openConfirm('Review Başlat', 'Bu projenin review sürecini başlatmak istediğinize emin misiniz?', 'primary', async () => {
      try {
        await startContentProjectReview(id)
        setAlert({ type: 'success', message: 'Review süreci başlatıldı.' })
        loadDetail(id)
      } catch (err) { setAlert({ type: 'error', message: extractError(err) }) }
    })
  }

  async function handleApprove(id: string) {
    openConfirm('Onayla', 'Bu projeyi onaylamak istediğinize emin misiniz?', 'primary', async () => {
      try {
        await approveContentProject(id)
        setAlert({ type: 'success', message: 'Proje onaylandı.' })
        loadDetail(id)
      } catch (err) { setAlert({ type: 'error', message: extractError(err) }) }
    })
  }

  function handleRequestRevision(id: string) {
    openNote('Revizyon Talep Et', 'Revizyon Notu', 'primary', async (note: string) => {
      try {
        await requestContentProjectRevision(id, { reviewNote: note })
        setAlert({ type: 'success', message: 'Revizyon talep edildi.' })
        loadDetail(id)
      } catch (err) { setAlert({ type: 'error', message: extractError(err) }) }
    })
  }

  function handleReject(id: string) {
    openNote('Reddet', 'Red Notu', 'danger', async (note: string) => {
      try {
        await rejectContentProject(id, { reviewNote: note })
        setAlert({ type: 'success', message: 'Proje reddedildi.' })
        loadDetail(id)
      } catch (err) { setAlert({ type: 'error', message: extractError(err) }) }
    })
  }

  async function handlePublish(id: string) {
    openConfirm('Yayına Hazır', 'Bu projeyi yayına hazır olarak işaretlemek istediğinize emin misiniz?', 'primary', async () => {
      try {
        await markContentProjectReadyForPublish(id)
        setAlert({ type: 'success', message: 'Proje yayına hazır olarak işaretlendi.' })
        loadDetail(id)
      } catch (err) { setAlert({ type: 'error', message: extractError(err) }) }
    })
  }

  async function handleCancel(id: string) {
    openConfirm('İptal Et', 'Bu projeyi iptal etmek istediğinize emin misiniz? Bu işlem geri alınamaz.', 'danger', async () => {
      try {
        await cancelContentProject(id)
        setAlert({ type: 'success', message: 'Proje iptal edildi.' })
        loadDetail(id)
      } catch (err) { setAlert({ type: 'error', message: extractError(err) }) }
    })
  }

  // ── Render: LIST ──────────────────────────────────────
  function renderList() {
    return (
      <>
        <div className="page-header-row">
          <div className="page-header">
            <h1 className="page-title">İçerik Projeleri</h1>
            <p className="page-subtitle">AI destekli içerik projelerini yönetin</p>
          </div>
          <div className="page-actions">
            <button className="btn btn-primary" onClick={goCreate}>+ Yeni Proje</button>
          </div>
        </div>

        <div className="filter-bar">
          <select className="form-select" value={statusFilter} onChange={e => setStatusFilter(e.target.value)}>
            <option value="">Tüm Durumlar</option>
            {CONTENT_PROJECT_STATUSES.map(s => (
              <option key={s.value} value={s.value}>{s.label}</option>
            ))}
          </select>
        </div>

        {listLoading ? (
          <LoadingSpinner text="Yükleniyor…" />
        ) : items.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">📝</div>
            <div className="empty-state-title">İçerik projesi bulunamadı</div>
            <div className="empty-state-text">Bilgi bankasından bir konu seçerek proje oluşturun.</div>
          </div>
        ) : (
          <div className="data-table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Bilgi Öğesi</th>
                  <th>Amaç</th>
                  <th>Durum</th>
                  <th>Dil</th>
                  <th>Oluşturma</th>
                  <th>Aksiyonlar</th>
                </tr>
              </thead>
              <tbody>
                {items.map(item => (
                  <tr key={item.id}>
                    <td>
                      <span className="cell-title" style={{ cursor: 'pointer' }} onClick={() => goDetail(item.id)}>
                        {item.knowledgeItemTitle}
                      </span>
                    </td>
                    <td className="cell-meta">{purposeLabel(item.contentPurpose)}</td>
                    <td><StatusBadge status={item.status} /></td>
                    <td className="cell-meta">{item.language.toUpperCase()}</td>
                    <td className="cell-meta">{new Date(item.createdAt).toLocaleDateString('tr-TR')}</td>
                    <td className="cell-actions">
                      <button className="btn-action" onClick={() => goDetail(item.id)}>Detay</button>
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

  // ── Render: CREATE ────────────────────────────────────
  function renderCreate() {
    return (
      <>
        <div className="page-header">
          <h1 className="page-title">Yeni İçerik Projesi</h1>
          <p className="page-subtitle">
            <button className="btn-action" onClick={goList}>← Listeye Dön</button>
          </p>
        </div>

        <FormCard icon="📚" title="Proje Bilgileri" subtitle="Bir bilgi öğesinden yeni içerik projesi başlatın">
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="form-label" htmlFor="cp-ki">Bilgi Öğesi (READY) *</label>
              {kiLoading ? (
                <LoadingSpinner text="Yükleniyor…" size="sm" />
              ) : readyKIs.length === 0 ? (
                <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>Hazır bilgi öğesi bulunamadı. Önce bir bilgi öğesini READY yapın.</p>
              ) : (
                <select id="cp-ki" name="knowledgeItemId" className="form-select" value={form.knowledgeItemId} onChange={handleChange} required>
                  <option value="">Seçiniz…</option>
                  {readyKIs.map(ki => (
                    <option key={ki.id} value={ki.id}>{ki.title} ({ki.type})</option>
                  ))}
                </select>
              )}
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="cp-purpose">İçerik Amacı</label>
                <select id="cp-purpose" name="contentPurpose" className="form-select" value={form.contentPurpose} onChange={handleChange}>
                  <option value="">Seçiniz…</option>
                  {CONTENT_PURPOSES.map(p => <option key={p.value} value={p.value}>{p.label}</option>)}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label" htmlFor="cp-audience">Hedef Kitle</label>
                <select id="cp-audience" name="targetAudience" className="form-select" value={form.targetAudience} onChange={handleChange}>
                  <option value="">Seçiniz…</option>
                  {TARGET_AUDIENCES.map(a => <option key={a.value} value={a.value}>{a.label}</option>)}
                </select>
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="cp-tone">Ton</label>
                <select id="cp-tone" name="tone" className="form-select" value={form.tone} onChange={handleChange}>
                  <option value="">Seçiniz…</option>
                  {CONTENT_TONES.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label" htmlFor="cp-language">Dil *</label>
                <select id="cp-language" name="language" className="form-select" value={form.language} onChange={handleChange} required>
                  {LANGUAGES.map(l => <option key={l.value} value={l.value}>{l.label}</option>)}
                </select>
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="cp-format">Format İpucu</label>
                <input id="cp-format" name="formatHint" type="text" className="form-input"
                  value={form.formatHint} onChange={handleChange} placeholder="Örn: madde listesi" />
              </div>
              <div className="form-group">
                <label className="form-label" htmlFor="cp-length">Uzunluk İpucu</label>
                <input id="cp-length" name="lengthHint" type="text" className="form-input"
                  value={form.lengthHint} onChange={handleChange} placeholder="Örn: 300 kelime" />
              </div>
            </div>

            <SubmitButton loading={formLoading} text="Oluştur" loadingText="Oluşturuluyor…" />
          </form>
        </FormCard>
      </>
    )
  }

  // ── Render: DETAIL ────────────────────────────────────
  function renderDetail() {
    if (detailLoading) return <LoadingSpinner text="Detay yükleniyor…" />
    if (!detail) return <div className="empty-state"><div className="empty-state-title">Proje bulunamadı</div></div>

    const s = detail.status
    return (
      <>
        <div className="page-header-row">
          <div className="page-header">
            <h1 className="page-title">{detail.knowledgeItemTitle}</h1>
            <p className="page-subtitle">
              <button className="btn-action" onClick={goList}>← Listeye Dön</button>
            </p>
          </div>
          <div className="page-actions">
            <StatusBadge status={s} />
          </div>
        </div>

        {/* Metadata */}
        <FormCard icon="📋" title="Proje Bilgileri">
          <div className="detail-grid">
            <div className="detail-field">
              <span className="detail-label">Amaç</span>
              <span className="detail-value">{purposeLabel(detail.contentPurpose)}</span>
            </div>
            <div className="detail-field">
              <span className="detail-label">Hedef Kitle</span>
              <span className="detail-value">{audienceLabel(detail.targetAudience)}</span>
            </div>
            <div className="detail-field">
              <span className="detail-label">Ton</span>
              <span className="detail-value">{toneLabel(detail.tone)}</span>
            </div>
            <div className="detail-field">
              <span className="detail-label">Dil</span>
              <span className="detail-value">{detail.language.toUpperCase()}</span>
            </div>
            <div className="detail-field">
              <span className="detail-label">Format</span>
              <span className="detail-value">{detail.formatHint ?? '—'}</span>
            </div>
            <div className="detail-field">
              <span className="detail-label">Uzunluk</span>
              <span className="detail-value">{detail.lengthHint ?? '—'}</span>
            </div>
            <div className="detail-field">
              <span className="detail-label">Retry</span>
              <span className="detail-value">{detail.retryCount}/3</span>
            </div>
            <div className="detail-field">
              <span className="detail-label">ID</span>
              <span className="detail-value"><code style={{ fontSize: '0.75rem' }}>{detail.id}</code></span>
            </div>
          </div>
        </FormCard>

        {/* Error info */}
        {detail.lastErrorType && (
          <FormCard icon="⚠️" title="Son Hata">
            <div className="detail-grid">
              <div className="detail-field">
                <span className="detail-label">Hata Tipi</span>
                <span className="detail-value">{detail.lastErrorType}</span>
              </div>
              <div className="detail-field">
                <span className="detail-label">Mesaj</span>
                <span className="detail-value">{detail.lastErrorMessage ?? '—'}</span>
              </div>
            </div>
          </FormCard>
        )}

        {/* AI generation result */}
        {aiResult && (
          <FormCard icon={aiResult.success ? '✅' : '❌'} title={aiResult.success ? 'AI Draft Oluşturuldu' : 'AI Üretimi Başarısız'}>
            <div className="detail-grid">
              {aiResult.draftTitle && (
                <div className="detail-field">
                  <span className="detail-label">Draft Başlığı</span>
                  <span className="detail-value">{aiResult.draftTitle}</span>
                </div>
              )}
              <div className="detail-field">
                <span className="detail-label">Proje Durumu</span>
                <StatusBadge status={aiResult.projectStatus} />
              </div>
              {aiResult.errorType && (
                <div className="detail-field">
                  <span className="detail-label">Hata Tipi</span>
                  <span className="detail-value">{aiResult.errorType}</span>
                </div>
              )}
              {aiResult.retryCount != null && (
                <div className="detail-field">
                  <span className="detail-label">Deneme</span>
                  <span className="detail-value">{aiResult.retryCount}/{aiResult.maxRetryCount}</span>
                </div>
              )}
            </div>
            {aiResult.success && aiResult.contentDraftId && (
              <div className="detail-actions" style={{ borderTop: 'none', paddingTop: 8 }}>
                <button className="btn btn-primary" onClick={() => navigate(`/drafts/${aiResult.contentDraftId}`)}>
                  📄 Taslağı Aç
                </button>
              </div>
            )}
          </FormCard>
        )}

        {/* Safety note */}
        {(s === 'DRAFT_READY' || s === 'IN_REVIEW') && (
          <div className="api-alert api-alert-info" style={{ marginBottom: 16 }}>
            <span className="api-alert-icon">ℹ</span>
            <span className="api-alert-message">
              Bu proje henüz insan onayı almamıştır. AI taslağı yalnızca review ve açık onay sonrası yayınlanabilir.
            </span>
          </div>
        )}

        {/* Stable draft open button — uses backend latestDraftId, persists after reload */}
        {detail.latestDraftId && (
          <FormCard icon="📄" title="Son Taslak">
            <div className="detail-actions" style={{ borderTop: 'none', paddingTop: 0 }}>
              <button className="btn btn-primary" onClick={() => navigate(`/drafts/${detail.latestDraftId}`)}>
                📄 Taslağı Aç
              </button>
            </div>
          </FormCard>
        )}

        {/* Action buttons — status-driven, action endpoints only */}
        <FormCard icon="⚡" title="Aksiyonlar">
          <div className="detail-actions" style={{ borderTop: 'none', paddingTop: 0 }}>
            {canGenerateAiDraft(s) && (
              <button className="btn btn-primary" disabled={actionLoading} onClick={() => handleGenerateAiDraft(detail.id)}>
                {actionLoading && <span className="loading-spinner" />}
                🤖 AI Draft Üret
              </button>
            )}
            {canRetryAiDraft(s) && (
              <button className="btn btn-primary" disabled={actionLoading} onClick={() => handleRetry(detail.id)}>
                {actionLoading && <span className="loading-spinner" />}
                🔄 Yeniden Dene
              </button>
            )}
            {canStartReview(s) && (
              <button className="btn-action btn-action-success" onClick={() => handleStartReview(detail.id)}>
                👁 Review Başlat
              </button>
            )}
            {canApproveProject(s) && (
              <button className="btn-action btn-action-success" onClick={() => handleApprove(detail.id)}>
                ✓ Onayla
              </button>
            )}
            {canRequestRevision(s) && (
              <button className="btn-action" onClick={() => handleRequestRevision(detail.id)}>
                ↩ Revizyon
              </button>
            )}
            {canRejectProject(s) && (
              <button className="btn-action btn-action-danger" onClick={() => handleReject(detail.id)}>
                ✕ Reddet
              </button>
            )}
            {canMarkReadyForPublish(s) && (
              <button className="btn-action btn-action-success" onClick={() => handlePublish(detail.id)}>
                ✓✓ Yayına Hazır
              </button>
            )}
            {canCancelProject(s) && (
              <button className="btn-action btn-action-danger" onClick={() => handleCancel(detail.id)}>
                — İptal
              </button>
            )}
          </div>
        </FormCard>

        {/* FP-002: Scenario Workflow Section */}
        <ScenarioWorkflowSection contentProjectId={detail.id} projectStatus={s} />
      </>
    )
  }

  // ── Main Render ───────────────────────────────────────
  return (
    <div className="fade-in">
      {alert && <ApiAlert type={alert.type} message={alert.message} onClose={() => setAlert(null)} />}

      {view === 'list' && renderList()}
      {view === 'create' && renderCreate()}
      {view === 'detail' && renderDetail()}

      <ConfirmDialog
        open={confirmDialog.open} title={confirmDialog.title}
        message={confirmDialog.message} variant={confirmDialog.variant}
        loading={confirmLoading} onConfirm={handleConfirm}
        onCancel={() => setConfirmDialog(p => ({ ...p, open: false }))}
      />
      <NoteDialog
        open={noteDialog.open} title={noteDialog.title}
        label={noteDialog.label} variant={noteDialog.variant}
        loading={noteLoading} onConfirm={handleNote}
        onCancel={() => setNoteDialog(p => ({ ...p, open: false }))}
      />
    </div>
  )
}

export default ContentProjectsPage
