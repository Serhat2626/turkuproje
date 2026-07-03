import { useState, useEffect, useCallback } from 'react'
import {
  generatePlatformPackagesForStoryboard, listPlatformPackagesByContentProject,
  getPlatformPackageDetail, updatePlatformPackageDraft,
  approvePlatformPackageDraft, rejectPlatformPackageDraft,
  exportPlatformPackage,
} from '../../api/platformPackageApi'
import type {
  PlatformPackageDraftSummaryResponse, PlatformPackageDraftResponse,
  PlatformPackageDraftEditPayload, PlatformPackageSceneUsageEditPayload,
  PlatformPackageGenerateResponse, PlatformPackageStatus,
  PlatformPackageExportType,
} from '../../types'
import { PLATFORM_PACKAGE_TYPES, PLATFORM_PACKAGE_EXPORT_TYPES } from '../../types'
import FormCard from '../common/FormCard'
import StatusBadge from '../common/StatusBadge'
import LoadingSpinner from '../common/LoadingSpinner'
import ConfirmDialog from '../common/ConfirmDialog'
import NoteDialog from '../common/NoteDialog'
import ApiAlert from '../common/ApiAlert'

interface Props {
  contentProjectId: string
  approvedStoryboardId: string | null
}

const EDITABLE_STATUSES: PlatformPackageStatus[] = ['AI_GENERATED', 'NEEDS_REVIEW']
const ACTIONABLE_STATUSES: PlatformPackageStatus[] = ['AI_GENERATED', 'NEEDS_REVIEW']
const READONLY_STATUSES: PlatformPackageStatus[] = ['APPROVED', 'REJECTED', 'FAILED']

function packageTypeLabel(type: string): string {
  return PLATFORM_PACKAGE_TYPES.find(t => t.value === type)?.label ?? type
}

function PlatformPackageWorkflowSection({ contentProjectId, approvedStoryboardId }: Props) {
  // ── State ───────────────────────────────────────────────
  const [packages, setPackages] = useState<PlatformPackageDraftSummaryResponse[]>([])
  const [listLoading, setListLoading] = useState(false)
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [detail, setDetail] = useState<PlatformPackageDraftResponse | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [genLoading, setGenLoading] = useState(false)
  const [saveLoading, setSaveLoading] = useState(false)
  const [actionLoading, setActionLoading] = useState(false)

  // Edit form
  const [editForm, setEditForm] = useState<PlatformPackageDraftEditPayload>({})
  const [sceneUsageForms, setSceneUsageForms] = useState<PlatformPackageSceneUsageEditPayload[]>([])
  const [isEditing, setIsEditing] = useState(false)

  // Alert
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

  // Export (FP-005)
  const [exportLoading, setExportLoading] = useState<string | null>(null)
  const [copiedType, setCopiedType] = useState<string | null>(null)

  // ── Data Loading ────────────────────────────────────────
  const loadList = useCallback(async () => {
    setListLoading(true)
    try {
      const res = await listPlatformPackagesByContentProject(contentProjectId)
      setPackages(res.data ?? [])
    } catch {
      setAlert({ type: 'error', message: 'Platform paket listesi yüklenemedi' })
    } finally {
      setListLoading(false)
    }
  }, [contentProjectId])

  useEffect(() => { loadList() }, [loadList])

  async function loadDetail(id: string) {
    setDetailLoading(true)
    setIsEditing(false)
    try {
      const res = await getPlatformPackageDetail(id)
      setDetail(res.data)
      setSelectedId(id)
      if (res.data) initEditForm(res.data)
    } catch {
      setAlert({ type: 'error', message: 'Platform paketi detayı yüklenemedi' })
    } finally {
      setDetailLoading(false)
    }
  }

  function initEditForm(d: PlatformPackageDraftResponse) {
    setEditForm({
      title: d.title ?? '',
      captionOrDescription: d.captionOrDescription ?? '',
      hashtagsJson: d.hashtagsJson ?? '',
      callToAction: d.callToAction ?? '',
      targetDurationSeconds: d.targetDurationSeconds,
      aspectRatio: d.aspectRatio ?? '',
      contentStructureNotes: d.contentStructureNotes ?? '',
      sceneUsageNotes: d.sceneUsageNotes ?? '',
      reviewNotes: d.reviewNotes ?? '',
      videoTitle: d.videoTitle ?? '',
      longDescription: d.longDescription ?? '',
      chapterOutlineJson: d.chapterOutlineJson ?? '',
      pinnedCommentSuggestion: d.pinnedCommentSuggestion ?? '',
      thumbnailTextSuggestion: d.thumbnailTextSuggestion ?? '',
      shortHook: d.shortHook ?? '',
      shortCaption: d.shortCaption ?? '',
      overlayTextSuggestionsJson: d.overlayTextSuggestionsJson ?? '',
      shortHashtagSetJson: d.shortHashtagSetJson ?? '',
      shortCta: d.shortCta ?? '',
      durationGuidance: d.durationGuidance ?? '',
    })
    setSceneUsageForms((d.sceneUsages ?? []).map(su => ({
      sceneNumber: su.sceneNumber,
      usageType: su.usageType,
      included: su.included,
      adaptationNote: su.adaptationNote ?? '',
    })))
  }

  // ── Helpers ─────────────────────────────────────────────
  function extractError(err: unknown): string {
    const apiErr = err as { message?: string; errors?: { field: string; message: string }[] | null }
    const msgs: string[] = []
    if (apiErr.errors) msgs.push(...apiErr.errors.map(e => `${e.field}: ${e.message}`))
    if (apiErr.message && msgs.length === 0) msgs.push(apiErr.message)
    return msgs.join(' • ') || 'Bir hata oluştu'
  }

  function isReadOnly(): boolean {
    if (!detail) return true
    return READONLY_STATUSES.includes(detail.status)
  }

  function canApproveReject(): boolean {
    if (!detail) return false
    return ACTIONABLE_STATUSES.includes(detail.status)
  }

  function canEdit(): boolean {
    if (!detail) return false
    return EDITABLE_STATUSES.includes(detail.status)
  }

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

  // ── Actions ─────────────────────────────────────────────
  async function handleGenerate() {
    if (!approvedStoryboardId) return
    setGenLoading(true)
    setAlert(null)
    try {
      const res = await generatePlatformPackagesForStoryboard(approvedStoryboardId)
      const data: PlatformPackageGenerateResponse | null = res.data
      if (data?.success) {
        setAlert({ type: 'success', message: `${data.packageCount ?? 0} platform paketi üretildi` })
        await loadList()
      } else {
        setAlert({ type: 'error', message: data?.message ?? 'Platform paket üretimi başarısız' })
        await loadList()
      }
    } catch (err) {
      setAlert({ type: 'error', message: extractError(err) })
    } finally {
      setGenLoading(false)
    }
  }

  async function handleSave() {
    if (!detail) return
    setSaveLoading(true)
    setAlert(null)
    try {
      const payload: PlatformPackageDraftEditPayload = {
        ...editForm,
        sceneUsages: sceneUsageForms,
      }
      await updatePlatformPackageDraft(detail.id, payload)
      setAlert({ type: 'success', message: 'Platform paketi kaydedildi' })
      setIsEditing(false)
      await loadDetail(detail.id)
      await loadList()
    } catch (err) {
      setAlert({ type: 'error', message: extractError(err) })
    } finally {
      setSaveLoading(false)
    }
  }

  function handleApprove() {
    if (!detail) return
    openConfirm(
      'Platform Paketini Onayla',
      `${packageTypeLabel(detail.packageType)} paketini onaylamak istediğinize emin misiniz? Onay sonrası düzenlenemez.`,
      'primary',
      async () => {
        setActionLoading(true)
        try {
          await approvePlatformPackageDraft(detail.id)
          setAlert({ type: 'success', message: 'Platform paketi onaylandı' })
          await loadDetail(detail.id)
          await loadList()
        } catch (err) {
          setAlert({ type: 'error', message: extractError(err) })
        } finally {
          setActionLoading(false)
        }
      }
    )
  }

  function handleReject() {
    if (!detail) return
    openNote('Platform Paketini Reddet', 'Red Sebebi', 'danger', async (reason: string) => {
      setActionLoading(true)
      try {
        await rejectPlatformPackageDraft(detail.id, { reason })
        setAlert({ type: 'success', message: 'Platform paketi reddedildi' })
        await loadDetail(detail.id)
        await loadList()
      } catch (err) {
        setAlert({ type: 'error', message: extractError(err) })
      } finally {
        setActionLoading(false)
      }
    })
  }

  // ── FP-005: Export Handlers ──────────────────────────────

  async function handleExportDownload(packageId: string, exportType: PlatformPackageExportType) {
    setExportLoading(exportType)
    try {
      const res = await exportPlatformPackage(packageId, exportType)
      const data = res.data
      if (!data) throw new Error('Export verisi alınamadı')

      // Create blob and trigger download
      const blob = new Blob([data.content], { type: data.contentType })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = data.filename
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
      setAlert({ type: 'success', message: `${data.filename} indirildi` })
    } catch (err) {
      setAlert({ type: 'error', message: extractError(err) })
    } finally {
      setExportLoading(null)
    }
  }

  async function handleExportCopy(packageId: string, exportType: PlatformPackageExportType) {
    setExportLoading(exportType + '_copy')
    try {
      const res = await exportPlatformPackage(packageId, exportType)
      const data = res.data
      if (!data) throw new Error('Export verisi alınamadı')

      await navigator.clipboard.writeText(data.content)
      setCopiedType(exportType)
      setTimeout(() => setCopiedType(null), 2000)
    } catch (err) {
      setAlert({ type: 'error', message: extractError(err) })
    } finally {
      setExportLoading(null)
    }
  }

  // ── FP-005: Export Render Helpers ───────────────────────

  function renderExportActions(packageId: string) {
    return (
      <>
        {PLATFORM_PACKAGE_EXPORT_TYPES.map(et => (
          <button
            key={et.value}
            className="btn-action"
            data-testid={`export-btn-${et.value}`}
            disabled={!!exportLoading}
            onClick={() => handleExportDownload(packageId, et.value)}
            title={`${et.label} olarak indir`}
            style={{ fontSize: '0.75rem' }}
          >
            {exportLoading === et.value ? '⏳' : et.icon} {et.label}
          </button>
        ))}
      </>
    )
  }

  function renderExportSection(packageId: string) {
    return (
      <div style={{ marginTop: 16 }}>
        <h4 style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', marginBottom: 8 }}>
          📤 Export & Production Handoff
        </h4>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 8 }}>
          {PLATFORM_PACKAGE_EXPORT_TYPES.map(et => (
            <div
              key={et.value}
              className="form-card"
              data-testid={`export-card-${et.value}`}
              style={{ padding: '10px 14px', display: 'flex', flexDirection: 'column', gap: 6 }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <span style={{ fontSize: '1.1rem' }}>{et.icon}</span>
                <span style={{ fontWeight: 600, fontSize: '0.85rem' }}>{et.label}</span>
                <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>{et.ext}</span>
              </div>
              <div style={{ display: 'flex', gap: 6 }}>
                <button
                  className="btn-action"
                  data-testid={`export-download-${et.value}`}
                  disabled={!!exportLoading}
                  onClick={() => handleExportDownload(packageId, et.value)}
                  style={{ flex: 1, fontSize: '0.75rem', justifyContent: 'center' }}
                >
                  {exportLoading === et.value ? '⏳ İndiriliyor…' : '⬇️ İndir'}
                </button>
                <button
                  className="btn-action"
                  data-testid={`export-copy-${et.value}`}
                  disabled={!!exportLoading}
                  onClick={() => handleExportCopy(packageId, et.value)}
                  style={{
                    flex: 1, fontSize: '0.75rem', justifyContent: 'center',
                    ...(copiedType === et.value ? { color: 'var(--success, #00c864)', fontWeight: 700 } : {}),
                  }}
                >
                  {copiedType === et.value ? '✅ Kopyalandı!' :
                   exportLoading === et.value + '_copy' ? '⏳ Kopyalanıyor…' : '📋 Kopyala'}
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
    )
  }

  // ── Render: Package List ────────────────────────────────
  function renderPackageList() {
    if (listLoading) return <LoadingSpinner text="Platform paketleri yükleniyor…" size="sm" />

    return (
      <div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 12 }}>
          <h3 style={{ margin: 0, fontSize: '0.95rem', color: 'var(--text-secondary)' }}>
            Platform Paketleri ({packages.length})
          </h3>
          {approvedStoryboardId ? (
            <button
              className="btn btn-primary"
              style={{ fontSize: '0.8rem', padding: '6px 14px' }}
              disabled={genLoading}
              onClick={handleGenerate}
            >
              {genLoading && <span className="loading-spinner" />}
              📦 Paket Üret
            </button>
          ) : (
            <span style={{ fontSize: '0.8rem', color: 'var(--warning)', fontStyle: 'italic' }}>
              ⚠️ Platform paketi üretmek için önce onaylanmış storyboard gereklidir.
            </span>
          )}
        </div>

        {packages.length === 0 ? (
          <div className="empty-state" style={{ padding: 24 }}>
            <div className="empty-state-icon">📦</div>
            <div className="empty-state-title">Henüz platform paketi yok</div>
            <div className="empty-state-text">
              {approvedStoryboardId
                ? 'Onaylanmış storyboard\'dan platform paketleri üretmek için yukarıdaki butonu kullanın.'
                : 'Platform paketi üretmek için önce bir storyboard\'u onaylamanız gerekiyor.'}
            </div>
          </div>
        ) : (
          <div className="data-table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Platform</th>
                  <th>V</th>
                  <th>Başlık</th>
                  <th>Durum</th>
                  <th>Süre</th>
                  <th>Oran</th>
                  <th>Provider</th>
                  <th>Oluşturma</th>
                  <th>Aksiyonlar</th>
                </tr>
              </thead>
              <tbody>
                {packages.map(p => (
                  <tr key={p.id} style={selectedId === p.id ? { background: 'var(--bg-hover)' } : {}}>
                    <td style={{ fontWeight: 600, fontSize: '0.8rem' }}>{packageTypeLabel(p.packageType)}</td>
                    <td style={{ fontWeight: 600, color: 'var(--primary)' }}>v{p.version}</td>
                    <td>
                      <span className="cell-title" style={{ cursor: 'pointer' }} onClick={() => loadDetail(p.id)}>
                        {p.title ?? '(Başlıksız)'}
                      </span>
                    </td>
                    <td><StatusBadge status={p.status} /></td>
                    <td className="cell-meta">{p.targetDurationSeconds ? `${p.targetDurationSeconds}sn` : '—'}</td>
                    <td className="cell-meta">{p.aspectRatio ?? '—'}</td>
                    <td className="cell-meta">{p.providerType ?? '—'}</td>
                    <td className="cell-meta">{new Date(p.createdAt).toLocaleDateString('tr-TR')}</td>
                    <td className="cell-actions">
                      <button className="btn-action" onClick={() => loadDetail(p.id)}>
                        {selectedId === p.id ? '↻ Yenile' : 'Detay'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    )
  }

  // ── Render: Package Detail ──────────────────────────────
  function renderPackageDetail() {
    if (!selectedId) return null
    if (detailLoading) return <LoadingSpinner text="Platform paketi detayı yükleniyor…" size="sm" />
    if (!detail) return null

    const readonly = isReadOnly()
    const editing = isEditing && !readonly
    const isLongForm = detail.packageType === 'YOUTUBE_LONG_FORM'
    const isShortForm = ['YOUTUBE_SHORTS', 'INSTAGRAM_REELS', 'FACEBOOK_REELS'].includes(detail.packageType)

    return (
      <div style={{ marginTop: 16 }}>
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <h3 style={{ margin: 0, fontSize: '0.95rem', color: 'var(--text-secondary)' }}>
            📦 {packageTypeLabel(detail.packageType)} v{detail.version} — <StatusBadge status={detail.status} />
          </h3>
          <div style={{ display: 'flex', gap: 8 }}>
            {canEdit() && !editing && (
              <button className="btn-action" onClick={() => setIsEditing(true)}>✏️ Düzenle</button>
            )}
            {editing && (
              <>
                <button className="btn btn-primary" style={{ fontSize: '0.8rem', padding: '6px 14px' }}
                  disabled={saveLoading} onClick={handleSave}>
                  {saveLoading && <span className="loading-spinner" />}
                  💾 Kaydet
                </button>
                <button className="btn-action" onClick={() => { setIsEditing(false); if (detail) initEditForm(detail) }}>
                  İptal
                </button>
              </>
            )}
            {canApproveReject() && (
              <>
                <button className="btn-action btn-action-success" disabled={actionLoading} onClick={handleApprove}>
                  ✓ Onayla
                </button>
                <button className="btn-action btn-action-danger" disabled={actionLoading} onClick={handleReject}>
                  ✕ Reddet
                </button>
              </>
            )}
            {/* FP-005: Export buttons — APPROVED only */}
            {detail.status === 'APPROVED' && renderExportActions(detail.id)}
          </div>
        </div>

        {/* Metadata */}
        <div className="detail-grid" style={{ marginBottom: 16 }}>
          <div className="detail-field">
            <span className="detail-label">Oluşturan</span>
            <span className="detail-value">{detail.generatedBy ?? '—'}</span>
          </div>
          <div className="detail-field">
            <span className="detail-label">Provider</span>
            <span className="detail-value">{detail.providerType ?? '—'} / {detail.modelName ?? '—'}</span>
          </div>
          <div className="detail-field">
            <span className="detail-label">Oluşturma</span>
            <span className="detail-value">{new Date(detail.createdAt).toLocaleString('tr-TR')}</span>
          </div>
          {detail.editedAt && (
            <div className="detail-field">
              <span className="detail-label">Düzenleme</span>
              <span className="detail-value">{new Date(detail.editedAt).toLocaleString('tr-TR')} ({detail.editedBy})</span>
            </div>
          )}
          {detail.approvedAt && (
            <div className="detail-field">
              <span className="detail-label">Onay</span>
              <span className="detail-value">{new Date(detail.approvedAt).toLocaleString('tr-TR')} ({detail.approvedBy})</span>
            </div>
          )}
          {detail.rejectedAt && (
            <div className="detail-field">
              <span className="detail-label">Red</span>
              <span className="detail-value">{new Date(detail.rejectedAt).toLocaleString('tr-TR')} ({detail.rejectedBy})</span>
            </div>
          )}
          {detail.rejectionReason && (
            <div className="detail-field" style={{ gridColumn: '1 / -1' }}>
              <span className="detail-label">Red Sebebi</span>
              <span className="detail-value" style={{ color: 'var(--danger)' }}>{detail.rejectionReason}</span>
            </div>
          )}
        </div>

        {/* Common Content Fields */}
        <h4 style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', marginBottom: 8 }}>📝 Genel İçerik</h4>
        <div style={{ display: 'grid', gap: 12, gridTemplateColumns: '1fr 1fr', marginBottom: 16 }}>
          {renderField('Başlık', 'title', editForm.title, editing)}
          {renderField('Açıklama / Caption', 'captionOrDescription', editForm.captionOrDescription, editing, true)}
          {renderField('Hashtag (JSON)', 'hashtagsJson', editForm.hashtagsJson, editing)}
          {renderField('Call-to-Action', 'callToAction', editForm.callToAction, editing)}
          {renderField('Hedef Süre (sn)', 'targetDurationSeconds', String(editForm.targetDurationSeconds ?? ''), editing)}
          {renderField('Aspect Ratio', 'aspectRatio', editForm.aspectRatio, editing)}
          <div style={{ gridColumn: '1 / -1' }}>
            {renderField('İçerik Yapısı Notları', 'contentStructureNotes', editForm.contentStructureNotes, editing, true)}
          </div>
          <div style={{ gridColumn: '1 / -1' }}>
            {renderField('Sahne Kullanım Notları', 'sceneUsageNotes', editForm.sceneUsageNotes, editing, true)}
          </div>
          <div style={{ gridColumn: '1 / -1' }}>
            {renderField('Review Notları', 'reviewNotes', editForm.reviewNotes, editing, true)}
          </div>
        </div>

        {/* YouTube Long-Form Fields */}
        {isLongForm && (
          <>
            <h4 style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', marginBottom: 8 }}>🎬 YouTube Long-Form</h4>
            <div style={{ display: 'grid', gap: 12, gridTemplateColumns: '1fr 1fr', marginBottom: 16 }}>
              {renderField('Video Başlığı', 'videoTitle', editForm.videoTitle, editing)}
              {renderField('Thumbnail Metin', 'thumbnailTextSuggestion', editForm.thumbnailTextSuggestion, editing)}
              <div style={{ gridColumn: '1 / -1' }}>
                {renderField('Video Açıklaması', 'longDescription', editForm.longDescription, editing, true)}
              </div>
              {renderField('Chapter (JSON)', 'chapterOutlineJson', editForm.chapterOutlineJson, editing, true)}
              {renderField('Sabitlenecek Yorum', 'pinnedCommentSuggestion', editForm.pinnedCommentSuggestion, editing, true)}
            </div>
          </>
        )}

        {/* Short-Form / Reels Fields */}
        {isShortForm && (
          <>
            <h4 style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', marginBottom: 8 }}>📱 Short-Form / Reels</h4>
            <div style={{ display: 'grid', gap: 12, gridTemplateColumns: '1fr 1fr', marginBottom: 16 }}>
              {renderField('Hook (İlk 3sn)', 'shortHook', editForm.shortHook, editing)}
              {renderField('Kısa CTA', 'shortCta', editForm.shortCta, editing)}
              <div style={{ gridColumn: '1 / -1' }}>
                {renderField('Kısa Caption', 'shortCaption', editForm.shortCaption, editing, true)}
              </div>
              {renderField('Overlay Texts (JSON)', 'overlayTextSuggestionsJson', editForm.overlayTextSuggestionsJson, editing)}
              {renderField('Short Hashtags (JSON)', 'shortHashtagSetJson', editForm.shortHashtagSetJson, editing)}
              {renderField('Süre Rehberi', 'durationGuidance', editForm.durationGuidance, editing)}
            </div>
          </>
        )}

        {/* Scene Usages */}
        <h4 style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', marginBottom: 8 }}>
          🎬 Sahne Kullanımları ({detail.sceneUsages?.length ?? 0})
        </h4>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {sceneUsageForms.map((su, idx) => (
            <div key={su.sceneNumber} className="form-card" style={{ padding: '10px 14px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontWeight: 600, fontSize: '0.85rem', color: 'var(--primary)' }}>
                  Sahne {su.sceneNumber}
                  <span style={{ marginLeft: 8, fontSize: '0.75rem', padding: '2px 6px', borderRadius: 4,
                    background: su.included ? 'var(--success-light, rgba(0,200,100,0.15))' : 'var(--danger-light, rgba(200,0,0,0.15))',
                    color: su.included ? 'var(--success, #00c864)' : 'var(--danger, #ff4444)' }}>
                    {su.included ? '✓ Dahil' : '✕ Hariç'}
                  </span>
                  <span style={{ marginLeft: 8, fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                    [{su.usageType}]
                  </span>
                </span>
                {editing && (
                  <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                    <select className="form-input" style={{ width: 120, fontSize: '0.8rem', padding: '4px 8px' }}
                      value={su.usageType} onChange={e => setSceneUsageForms(prev =>
                        prev.map((s, i) => i === idx ? { ...s, usageType: e.target.value } : s))}>
                      <option value="PRIMARY">PRIMARY</option>
                      <option value="ADAPTED">ADAPTED</option>
                      <option value="PARTIAL">PARTIAL</option>
                      <option value="OMITTED">OMITTED</option>
                    </select>
                    <label style={{ fontSize: '0.8rem', display: 'flex', alignItems: 'center', gap: 4 }}>
                      <input type="checkbox" checked={su.included ?? true}
                        onChange={e => setSceneUsageForms(prev =>
                          prev.map((s, i) => i === idx ? { ...s, included: e.target.checked } : s))} />
                      Dahil
                    </label>
                  </div>
                )}
              </div>
              {editing ? (
                <div style={{ marginTop: 6 }}>
                  <label className="form-label" style={{ fontSize: '0.75rem' }}>Adaptasyon Notu</label>
                  <input className="form-input" style={{ fontSize: '0.8rem' }} value={su.adaptationNote ?? ''}
                    onChange={e => setSceneUsageForms(prev =>
                      prev.map((s, i) => i === idx ? { ...s, adaptationNote: e.target.value } : s))} />
                </div>
              ) : su.adaptationNote ? (
                <div style={{ marginTop: 4, fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                  💡 {su.adaptationNote}
                </div>
              ) : null}
            </div>
          ))}
        </div>

        {/* FP-005: Export Section — APPROVED only */}
        {detail.status === 'APPROVED' && renderExportSection(detail.id)}
      </div>
    )
  }

  function renderField(label: string, key: string, value: string | undefined, editing: boolean, textarea = false) {
    return (
      <div className="form-group" style={{ margin: 0 }}>
        <label className="form-label" style={{ fontSize: '0.8rem' }}>{label}</label>
        {editing ? (
          textarea ? (
            <textarea
              className="form-input"
              style={{ minHeight: 60, fontSize: '0.85rem', resize: 'vertical' }}
              value={value ?? ''}
              onChange={e => setEditForm(prev => ({ ...prev, [key]: e.target.value }))}
            />
          ) : (
            <input
              className="form-input"
              style={{ fontSize: '0.85rem' }}
              value={value ?? ''}
              onChange={e => setEditForm(prev => ({
                ...prev,
                [key]: key === 'targetDurationSeconds' ? (e.target.value ? Number(e.target.value) : null) : e.target.value
              }))}
              type={key === 'targetDurationSeconds' ? 'number' : 'text'}
            />
          )
        ) : (
          <span className="detail-value" style={{ fontSize: '0.85rem' }}>
            {value || '—'}
          </span>
        )}
      </div>
    )
  }

  // ── Main Render ─────────────────────────────────────────
  return (
    <FormCard icon="📦" title="Platform Paketleri İş Akışı" subtitle="FP-004 — Platform paketi üretim ve review">
      {alert && <ApiAlert type={alert.type} message={alert.message} onClose={() => setAlert(null)} />}

      {renderPackageList()}
      {renderPackageDetail()}

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
    </FormCard>
  )
}

export default PlatformPackageWorkflowSection
