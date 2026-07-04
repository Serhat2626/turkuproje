import { useState, useEffect, useCallback } from 'react'
import {
  generateVisualAssetPlan, listVisualAssetPlansByContentProject,
  getVisualAssetPlanDetail, updateVisualAssetPlan,
  updateSceneVisualPlan, updateThumbnailPlan,
  approveVisualAssetPlan, rejectVisualAssetPlan,
} from '../../api/visualAssetApi'
import type {
  VisualAssetPlanSummaryResponse, VisualAssetPlanResponse,
  VisualAssetPlanEditRequest, SceneVisualPlanEditRequest,
  ThumbnailPlanEditRequest, VisualAssetPlanStatus,
  SceneVisualPlanResponse, ThumbnailPlanResponse,
} from '../../types'
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

const EDITABLE_STATUSES: VisualAssetPlanStatus[] = ['AI_GENERATED', 'NEEDS_REVIEW', 'APPROVED']
const ACTIONABLE_STATUSES: VisualAssetPlanStatus[] = ['AI_GENERATED', 'NEEDS_REVIEW']

function VisualAssetWorkflowSection({ contentProjectId, approvedStoryboardId }: Props) {
  // ── State ───────────────────────────────────────────────
  const [plans, setPlans] = useState<VisualAssetPlanSummaryResponse[]>([])
  const [listLoading, setListLoading] = useState(false)
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [detail, setDetail] = useState<VisualAssetPlanResponse | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [genLoading, setGenLoading] = useState(false)
  const [saveLoading, setSaveLoading] = useState(false)
  const [actionLoading] = useState(false)

  // Edit mode
  const [editingPlan, setEditingPlan] = useState(false)
  const [editingSceneNum, setEditingSceneNum] = useState<number | null>(null)
  const [editingThumbnail, setEditingThumbnail] = useState(false)

  // Edit forms
  const [planForm, setPlanForm] = useState<VisualAssetPlanEditRequest>({})
  const [sceneForm, setSceneForm] = useState<SceneVisualPlanEditRequest>({})
  const [thumbForm, setThumbForm] = useState<ThumbnailPlanEditRequest>({})

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

  // ── Data Loading ────────────────────────────────────────
  const loadList = useCallback(async () => {
    setListLoading(true)
    try {
      const res = await listVisualAssetPlansByContentProject(contentProjectId)
      setPlans(res.data ?? [])
    } catch {
      setAlert({ type: 'error', message: 'Görsel plan listesi yüklenemedi' })
    } finally {
      setListLoading(false)
    }
  }, [contentProjectId])

  useEffect(() => { loadList() }, [loadList])

  async function loadDetail(id: string) {
    setDetailLoading(true)
    cancelAllEdits()
    try {
      const res = await getVisualAssetPlanDetail(id)
      setDetail(res.data)
      setSelectedId(id)
    } catch {
      setAlert({ type: 'error', message: 'Görsel plan detayı yüklenemedi' })
    } finally {
      setDetailLoading(false)
    }
  }

  // ── Edit Forms ──────────────────────────────────────────
  function cancelAllEdits() {
    setEditingPlan(false)
    setEditingSceneNum(null)
    setEditingThumbnail(false)
  }

  function initPlanForm(d: VisualAssetPlanResponse) {
    setPlanForm({
      title: d.title ?? '',
      summary: d.summary ?? '',
      overallVisualStyle: d.overallVisualStyle ?? '',
      aspectRatioStrategy: d.aspectRatioStrategy ?? '',
      culturalAuthenticityGuidance: d.culturalAuthenticityGuidance ?? '',
      characterContinuityGuidance: d.characterContinuityGuidance ?? '',
      locationContinuityGuidance: d.locationContinuityGuidance ?? '',
      colorPaletteGuidance: d.colorPaletteGuidance ?? '',
      reviewNotes: d.reviewNotes ?? '',
    })
    setEditingPlan(true)
  }

  function initSceneForm(s: SceneVisualPlanResponse) {
    setSceneForm({
      englishImagePrompt: s.englishImagePrompt ?? '',
      turkishVisualContext: s.turkishVisualContext ?? '',
      negativePrompt: s.negativePrompt ?? '',
      styleDirective: s.styleDirective ?? '',
      aspectRatio: s.aspectRatio ?? '',
      compositionNotes: s.compositionNotes ?? '',
      cameraFraming: s.cameraFraming ?? '',
      lightingNotes: s.lightingNotes ?? '',
      colorPalette: s.colorPalette ?? '',
      mood: s.mood ?? '',
      location: s.location ?? '',
      characters: s.characters ?? '',
      culturalDetails: s.culturalDetails ?? '',
      textOverlayGuidance: s.textOverlayGuidance ?? '',
      imageGenerationToolNotes: s.imageGenerationToolNotes ?? '',
      safetyNotes: s.safetyNotes ?? '',
    })
    setEditingSceneNum(s.sceneNumber)
  }

  function initThumbForm(t: ThumbnailPlanResponse) {
    setThumbForm({
      thumbnailPrompt: t.thumbnailPrompt ?? '',
      negativePrompt: t.negativePrompt ?? '',
      textOverlayGuidance: t.textOverlayGuidance ?? '',
      compositionNotes: t.compositionNotes ?? '',
      emotionalHook: t.emotionalHook ?? '',
      aspectRatio: t.aspectRatio ?? '',
      styleDirective: t.styleDirective ?? '',
      colorPalette: t.colorPalette ?? '',
    })
    setEditingThumbnail(true)
  }

  // ── Helpers ─────────────────────────────────────────────
  function extractError(err: unknown): string {
    const apiErr = err as { message?: string; errors?: { field: string; message: string }[] | null }
    const msgs: string[] = []
    if (apiErr.errors) msgs.push(...apiErr.errors.map(e => `${e.field}: ${e.message}`))
    if (apiErr.message && msgs.length === 0) msgs.push(apiErr.message)
    return msgs.join(' • ') || 'Bir hata oluştu'
  }

  function canEdit(): boolean {
    if (!detail) return false
    return EDITABLE_STATUSES.includes(detail.status)
  }

  function canApproveReject(): boolean {
    if (!detail) return false
    return ACTIONABLE_STATUSES.includes(detail.status)
  }

  function copyToClipboard(text: string) {
    navigator.clipboard.writeText(text).then(() => {
      setAlert({ type: 'success', message: 'Panoya kopyalandı' })
      setTimeout(() => setAlert(null), 2000)
    })
  }

  // ── Actions ─────────────────────────────────────────────
  async function handleGenerate() {
    if (!approvedStoryboardId) return
    setGenLoading(true)
    setAlert(null)
    try {
      await generateVisualAssetPlan(approvedStoryboardId)
      setAlert({ type: 'success', message: 'Görsel plan başarıyla üretildi' })
      await loadList()
    } catch (err) {
      setAlert({ type: 'error', message: extractError(err) })
    } finally {
      setGenLoading(false)
    }
  }

  async function handleSavePlan() {
    if (!detail) return
    setSaveLoading(true)
    setAlert(null)
    try {
      await updateVisualAssetPlan(detail.id, planForm)
      setAlert({ type: 'success', message: 'Görsel plan güncellendi' })
      setEditingPlan(false)
      await loadDetail(detail.id)
      await loadList()
    } catch (err) {
      setAlert({ type: 'error', message: extractError(err) })
    } finally {
      setSaveLoading(false)
    }
  }

  async function handleSaveScene(sceneNumber: number) {
    if (!detail) return
    setSaveLoading(true)
    setAlert(null)
    try {
      await updateSceneVisualPlan(detail.id, sceneNumber, sceneForm)
      setAlert({ type: 'success', message: `Sahne ${sceneNumber} güncellendi` })
      setEditingSceneNum(null)
      await loadDetail(detail.id)
      await loadList()
    } catch (err) {
      setAlert({ type: 'error', message: extractError(err) })
    } finally {
      setSaveLoading(false)
    }
  }

  async function handleSaveThumbnail() {
    if (!detail) return
    setSaveLoading(true)
    setAlert(null)
    try {
      await updateThumbnailPlan(detail.id, thumbForm)
      setAlert({ type: 'success', message: 'Thumbnail planı güncellendi' })
      setEditingThumbnail(false)
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
    setConfirmDialog({
      open: true,
      title: 'Görsel Planı Onayla',
      message: `"${detail.title ?? 'Görsel plan'}" onaylanacak. Devam etmek istiyor musunuz?`,
      variant: 'primary',
      action: async () => {
        setConfirmLoading(true)
        try {
          await approveVisualAssetPlan(detail.id)
          setAlert({ type: 'success', message: 'Görsel plan onaylandı' })
          setConfirmDialog(prev => ({ ...prev, open: false }))
          await loadDetail(detail.id)
          await loadList()
        } catch (err) {
          setAlert({ type: 'error', message: extractError(err) })
        } finally {
          setConfirmLoading(false)
        }
      },
    })
  }

  function handleReject() {
    if (!detail) return
    setNoteDialog({
      open: true,
      title: 'Görsel Planı Reddet',
      label: 'Red gerekçesi',
      variant: 'danger',
      action: async (reason: string) => {
        setNoteLoading(true)
        try {
          await rejectVisualAssetPlan(detail.id, { reason })
          setAlert({ type: 'success', message: 'Görsel plan reddedildi' })
          setNoteDialog(prev => ({ ...prev, open: false }))
          await loadDetail(detail.id)
          await loadList()
        } catch (err) {
          setAlert({ type: 'error', message: extractError(err) })
        } finally {
          setNoteLoading(false)
        }
      },
    })
  }

  // ── Render: Field ───────────────────────────────────────
  function renderField(label: string, value: string | null | undefined, copyable = false) {
    if (!value) return null
    return (
      <div className="detail-field">
        <span className="detail-label">{label}</span>
        <span className="detail-value-long" style={{ whiteSpace: 'pre-wrap' }}>
          {value}
          {copyable && (
            <button
              className="btn-action"
              style={{ marginLeft: 8, fontSize: '0.75rem' }}
              onClick={() => copyToClipboard(value)}
              title="Kopyala"
            >📋</button>
          )}
        </span>
      </div>
    )
  }

  function renderEditTextarea(
    label: string, value: string, field: string,
    setter: (fn: (prev: Record<string, unknown>) => Record<string, unknown>) => void,
    rows = 3
  ) {
    return (
      <div className="form-group">
        <label className="form-label">{label}</label>
        <textarea
          className="form-textarea"
          value={value}
          onChange={e => setter(prev => ({ ...prev, [field]: e.target.value }))}
          rows={rows}
        />
      </div>
    )
  }

  function renderEditInput(
    label: string, value: string, field: string,
    setter: (fn: (prev: Record<string, unknown>) => Record<string, unknown>) => void
  ) {
    return (
      <div className="form-group">
        <label className="form-label">{label}</label>
        <input
          type="text"
          className="form-input"
          value={value}
          onChange={e => setter(prev => ({ ...prev, [field]: e.target.value }))}
        />
      </div>
    )
  }

  // ── Render: Plan List ───────────────────────────────────
  function renderPlanList() {
    if (listLoading) return <LoadingSpinner text="Görsel planlar yükleniyor…" size="sm" />

    if (plans.length === 0) {
      return (
        <div className="empty-state" style={{ padding: '24px 0' }}>
          <div className="empty-state-icon">🎨</div>
          <div className="empty-state-title">Henüz görsel plan üretilmedi</div>
          <div className="empty-state-text">
            {approvedStoryboardId
              ? 'Aşağıdaki butonu kullanarak görsel plan üretebilirsiniz.'
              : 'Görsel plan üretmek için önce bir storyboard onaylanmalıdır.'}
          </div>
        </div>
      )
    }

    return (
      <div className="data-table-wrapper" style={{ marginBottom: 16 }}>
        <table className="data-table">
          <thead>
            <tr>
              <th>Başlık</th>
              <th>Versiyon</th>
              <th>Sahne</th>
              <th>Durum</th>
              <th>Oluşturulma</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {plans.map(p => (
              <tr key={p.id} className={selectedId === p.id ? 'row-selected' : ''}>
                <td className="cell-title">{p.title ?? '—'}</td>
                <td>v{p.version}</td>
                <td>{p.scenePlanCount} sahne{p.hasThumbnailPlan ? ' + 🖼️' : ''}</td>
                <td><StatusBadge status={p.status} /></td>
                <td className="cell-meta">{new Date(p.createdAt).toLocaleString('tr-TR')}</td>
                <td className="cell-actions">
                  <button className="btn-action" onClick={() => loadDetail(p.id)}>Detay</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    )
  }

  // ── Render: Plan Detail ─────────────────────────────────
  function renderPlanDetail() {
    if (detailLoading) return <LoadingSpinner text="Plan detayı yükleniyor…" size="sm" />
    if (!detail) return null

    const isEditablePlan = canEdit()

    return (
      <div style={{ marginTop: 16 }}>
        {/* Plan header + actions */}
        <div className="page-header-row" style={{ marginBottom: 12 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <StatusBadge status={detail.status} />
            <span style={{ fontWeight: 600 }}>{detail.title ?? 'Görsel Plan'}</span>
            <span className="cell-meta">v{detail.version}</span>
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            {canApproveReject() && (
              <>
                <button className="btn-action btn-action-success" onClick={handleApprove} disabled={actionLoading}>
                  ✓ Onayla
                </button>
                <button className="btn-action btn-action-danger" onClick={handleReject} disabled={actionLoading}>
                  ✕ Reddet
                </button>
              </>
            )}
            <button className="btn-action" onClick={() => { setSelectedId(null); setDetail(null) }}>
              ← Listeye Dön
            </button>
          </div>
        </div>

        {/* Rejection reason if rejected */}
        {detail.status === 'REJECTED' && detail.rejectionReason && (
          <div className="api-alert api-alert-error" style={{ marginBottom: 12 }}>
            <span className="api-alert-icon">⚠</span>
            <span className="api-alert-message">
              <strong>Red gerekçesi:</strong> {detail.rejectionReason}
            </span>
          </div>
        )}

        {/* FAILED status message */}
        {detail.status === 'FAILED' && (
          <div className="api-alert api-alert-error" style={{ marginBottom: 12 }}>
            <span className="api-alert-icon">❌</span>
            <span className="api-alert-message">
              Bu plan başarısız olmuştur. Onaylanamaz veya düzenlenemez. Yeni plan üretebilirsiniz.
            </span>
          </div>
        )}

        {/* Parent plan fields */}
        {renderParentPlanSection(isEditablePlan)}

        {/* Scene plans */}
        {detail.scenePlans && detail.scenePlans.length > 0 && (
          <FormCard icon="🎬" title={`Sahne Görsel Planları (${detail.scenePlans.length} sahne)`}>
            {[...detail.scenePlans]
              .sort((a, b) => a.sceneNumber - b.sceneNumber)
              .map(scene => renderSceneCard(scene, isEditablePlan))}
          </FormCard>
        )}

        {/* Thumbnail plan */}
        {detail.thumbnailPlan && renderThumbnailCard(detail.thumbnailPlan, isEditablePlan)}

        {/* Metadata */}
        <FormCard icon="ℹ️" title="Plan Metadata">
          <div className="detail-grid">
            {renderField('Provider', detail.providerType)}
            {renderField('Model', detail.modelName)}
            {renderField('Oluşturan', detail.generatedBy)}
            {renderField('Oluşturulma', detail.createdAt ? new Date(detail.createdAt).toLocaleString('tr-TR') : null)}
            {renderField('Düzenlenme', detail.editedAt ? new Date(detail.editedAt).toLocaleString('tr-TR') : null)}
            {renderField('Düzenleyen', detail.editedBy)}
            {renderField('Onaylanma', detail.approvedAt ? new Date(detail.approvedAt).toLocaleString('tr-TR') : null)}
            {renderField('Onaylayan', detail.approvedBy)}
            {renderField('Reddedilme', detail.rejectedAt ? new Date(detail.rejectedAt).toLocaleString('tr-TR') : null)}
            {renderField('Reddeden', detail.rejectedBy)}
          </div>
        </FormCard>
      </div>
    )
  }

  // ── Render: Parent Plan Section ─────────────────────────
  function renderParentPlanSection(isEditablePlan: boolean) {
    if (!detail) return null

    if (editingPlan) {
      return (
        <FormCard icon="✏️" title="Görsel Plan Düzenle" subtitle="Kayıt sonrası status → NEEDS_REVIEW olur">
          {renderEditInput('Başlık', planForm.title ?? '', 'title', setPlanForm as never)}
          {renderEditTextarea('Özet', planForm.summary ?? '', 'summary', setPlanForm as never)}
          {renderEditTextarea('Genel Görsel Stil', planForm.overallVisualStyle ?? '', 'overallVisualStyle', setPlanForm as never)}
          {renderEditInput('Aspect Ratio Stratejisi', planForm.aspectRatioStrategy ?? '', 'aspectRatioStrategy', setPlanForm as never)}
          {renderEditTextarea('Kültürel Doğruluk Rehberi', planForm.culturalAuthenticityGuidance ?? '', 'culturalAuthenticityGuidance', setPlanForm as never)}
          {renderEditTextarea('Karakter Sürekliliği', planForm.characterContinuityGuidance ?? '', 'characterContinuityGuidance', setPlanForm as never)}
          {renderEditTextarea('Mekân Sürekliliği', planForm.locationContinuityGuidance ?? '', 'locationContinuityGuidance', setPlanForm as never)}
          {renderEditTextarea('Renk Paleti Rehberi', planForm.colorPaletteGuidance ?? '', 'colorPaletteGuidance', setPlanForm as never)}
          {renderEditTextarea('İnceleme Notları', planForm.reviewNotes ?? '', 'reviewNotes', setPlanForm as never)}
          <div style={{ display: 'flex', gap: 10, marginTop: 12 }}>
            <button className="btn btn-primary" onClick={handleSavePlan} disabled={saveLoading}>
              {saveLoading ? 'Kaydediliyor…' : 'Kaydet'}
            </button>
            <button className="btn btn-secondary" onClick={() => setEditingPlan(false)} disabled={saveLoading}>İptal</button>
          </div>
        </FormCard>
      )
    }

    return (
      <FormCard icon="🎨" title="Görsel Plan Bilgileri">
        <div className="detail-grid">
          {renderField('Başlık', detail.title)}
          {renderField('Özet', detail.summary)}
          {renderField('Genel Görsel Stil', detail.overallVisualStyle)}
          {renderField('Aspect Ratio', detail.aspectRatioStrategy)}
          {renderField('Kültürel Doğruluk Rehberi', detail.culturalAuthenticityGuidance)}
          {renderField('Karakter Sürekliliği', detail.characterContinuityGuidance)}
          {renderField('Mekân Sürekliliği', detail.locationContinuityGuidance)}
          {renderField('Renk Paleti Rehberi', detail.colorPaletteGuidance)}
          {renderField('İnceleme Notları', detail.reviewNotes)}
        </div>
        {isEditablePlan && (
          <div className="detail-actions" style={{ marginTop: 12 }}>
            <button className="btn btn-primary" onClick={() => initPlanForm(detail)}>✏️ Düzenle</button>
          </div>
        )}
      </FormCard>
    )
  }

  // ── Render: Scene Card ──────────────────────────────────
  function renderSceneCard(scene: SceneVisualPlanResponse, isEditablePlan: boolean) {
    const isEditingThis = editingSceneNum === scene.sceneNumber

    if (isEditingThis) {
      return (
        <div key={scene.id} className="form-card" style={{ marginBottom: 12, border: '1px solid var(--border-color)', padding: 16, borderRadius: 8 }}>
          <h4 style={{ marginBottom: 12 }}>✏️ Sahne {scene.sceneNumber} Düzenle — {scene.sceneTitle ?? ''}</h4>
          {renderEditTextarea('İngilizce Image Prompt', sceneForm.englishImagePrompt ?? '', 'englishImagePrompt', setSceneForm as never, 5)}
          {renderEditTextarea('Türkçe Görsel Bağlam', sceneForm.turkishVisualContext ?? '', 'turkishVisualContext', setSceneForm as never, 3)}
          {renderEditTextarea('Negative Prompt', sceneForm.negativePrompt ?? '', 'negativePrompt', setSceneForm as never, 2)}
          {renderEditInput('Stil Yönergesi', sceneForm.styleDirective ?? '', 'styleDirective', setSceneForm as never)}
          {renderEditInput('Aspect Ratio', sceneForm.aspectRatio ?? '', 'aspectRatio', setSceneForm as never)}
          {renderEditTextarea('Kompozisyon Notları', sceneForm.compositionNotes ?? '', 'compositionNotes', setSceneForm as never, 2)}
          {renderEditInput('Kamera Çerçeveleme', sceneForm.cameraFraming ?? '', 'cameraFraming', setSceneForm as never)}
          {renderEditInput('Aydınlatma Notları', sceneForm.lightingNotes ?? '', 'lightingNotes', setSceneForm as never)}
          {renderEditInput('Renk Paleti', sceneForm.colorPalette ?? '', 'colorPalette', setSceneForm as never)}
          {renderEditInput('Atmosfer', sceneForm.mood ?? '', 'mood', setSceneForm as never)}
          {renderEditInput('Mekân', sceneForm.location ?? '', 'location', setSceneForm as never)}
          {renderEditInput('Karakterler', sceneForm.characters ?? '', 'characters', setSceneForm as never)}
          {renderEditTextarea('Kültürel Detaylar', sceneForm.culturalDetails ?? '', 'culturalDetails', setSceneForm as never, 2)}
          {renderEditInput('Metin Overlay', sceneForm.textOverlayGuidance ?? '', 'textOverlayGuidance', setSceneForm as never)}
          {renderEditInput('Araç Notları', sceneForm.imageGenerationToolNotes ?? '', 'imageGenerationToolNotes', setSceneForm as never)}
          {renderEditInput('Güvenlik Notları', sceneForm.safetyNotes ?? '', 'safetyNotes', setSceneForm as never)}
          <div style={{ display: 'flex', gap: 10, marginTop: 12 }}>
            <button className="btn btn-primary" onClick={() => handleSaveScene(scene.sceneNumber)} disabled={saveLoading}>
              {saveLoading ? 'Kaydediliyor…' : 'Kaydet'}
            </button>
            <button className="btn btn-secondary" onClick={() => setEditingSceneNum(null)} disabled={saveLoading}>İptal</button>
          </div>
        </div>
      )
    }

    return (
      <div key={scene.id} className="form-card" style={{ marginBottom: 12, border: '1px solid var(--border-color)', padding: 16, borderRadius: 8 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
          <h4 style={{ margin: 0 }}>
            🎬 Sahne {scene.sceneNumber} — {scene.sceneTitle ?? ''}
            {scene.sourceBeatType && <span className="cell-meta" style={{ marginLeft: 8 }}>[{scene.sourceBeatType}]</span>}
          </h4>
          {isEditablePlan && (
            <button className="btn-action" onClick={() => initSceneForm(scene)}>✏️ Düzenle</button>
          )}
        </div>
        <div className="detail-grid">
          {renderField('Türkçe Bağlam', scene.turkishVisualContext)}
          {renderField('İngilizce Image Prompt', scene.englishImagePrompt, true)}
          {renderField('Negative Prompt', scene.negativePrompt)}
          {renderField('Stil', scene.styleDirective)}
          {renderField('Aspect Ratio', scene.aspectRatio)}
          {renderField('Kompozisyon', scene.compositionNotes)}
          {renderField('Kamera', scene.cameraFraming)}
          {renderField('Aydınlatma', scene.lightingNotes)}
          {renderField('Renk Paleti', scene.colorPalette)}
          {renderField('Atmosfer', scene.mood)}
          {renderField('Mekân', scene.location)}
          {renderField('Karakterler', scene.characters)}
          {renderField('Kültürel Detaylar', scene.culturalDetails)}
          {renderField('Metin Overlay', scene.textOverlayGuidance)}
          {renderField('Araç Notları', scene.imageGenerationToolNotes)}
          {renderField('Güvenlik Notları', scene.safetyNotes)}
        </div>
      </div>
    )
  }

  // ── Render: Thumbnail Card ──────────────────────────────
  function renderThumbnailCard(thumb: ThumbnailPlanResponse, isEditablePlan: boolean) {
    if (editingThumbnail) {
      return (
        <FormCard icon="✏️" title="Thumbnail Planı Düzenle" subtitle="Kayıt sonrası status → NEEDS_REVIEW olur">
          {renderEditTextarea('Thumbnail Prompt', thumbForm.thumbnailPrompt ?? '', 'thumbnailPrompt', setThumbForm as never, 4)}
          {renderEditTextarea('Negative Prompt', thumbForm.negativePrompt ?? '', 'negativePrompt', setThumbForm as never, 2)}
          {renderEditInput('Metin Overlay', thumbForm.textOverlayGuidance ?? '', 'textOverlayGuidance', setThumbForm as never)}
          {renderEditTextarea('Kompozisyon Notları', thumbForm.compositionNotes ?? '', 'compositionNotes', setThumbForm as never, 2)}
          {renderEditInput('Duygusal Hook', thumbForm.emotionalHook ?? '', 'emotionalHook', setThumbForm as never)}
          {renderEditInput('Aspect Ratio', thumbForm.aspectRatio ?? '', 'aspectRatio', setThumbForm as never)}
          {renderEditInput('Stil Yönergesi', thumbForm.styleDirective ?? '', 'styleDirective', setThumbForm as never)}
          {renderEditInput('Renk Paleti', thumbForm.colorPalette ?? '', 'colorPalette', setThumbForm as never)}
          <div style={{ display: 'flex', gap: 10, marginTop: 12 }}>
            <button className="btn btn-primary" onClick={handleSaveThumbnail} disabled={saveLoading}>
              {saveLoading ? 'Kaydediliyor…' : 'Kaydet'}
            </button>
            <button className="btn btn-secondary" onClick={() => setEditingThumbnail(false)} disabled={saveLoading}>İptal</button>
          </div>
        </FormCard>
      )
    }

    return (
      <FormCard icon="🖼️" title="Thumbnail Planı">
        <div className="detail-grid">
          {thumb.sourceSceneNumber != null && renderField('Referans Sahne', `Sahne ${thumb.sourceSceneNumber}`)}
          {renderField('Thumbnail Prompt', thumb.thumbnailPrompt, true)}
          {renderField('Negative Prompt', thumb.negativePrompt)}
          {renderField('Metin Overlay', thumb.textOverlayGuidance)}
          {renderField('Kompozisyon', thumb.compositionNotes)}
          {renderField('Duygusal Hook', thumb.emotionalHook)}
          {renderField('Aspect Ratio', thumb.aspectRatio)}
          {renderField('Stil', thumb.styleDirective)}
          {renderField('Renk Paleti', thumb.colorPalette)}
        </div>
        {isEditablePlan && (
          <div className="detail-actions" style={{ marginTop: 12 }}>
            <button className="btn btn-primary" onClick={() => initThumbForm(thumb)}>✏️ Düzenle</button>
          </div>
        )}
      </FormCard>
    )
  }

  // ── Main Render ─────────────────────────────────────────
  return (
    <FormCard icon="🎨" title="Görsel Varlık Planlama (FP-006)" subtitle="AI destekli sahne bazlı görsel prompt planlama">
      {alert && <ApiAlert type={alert.type} message={alert.message} onClose={() => setAlert(null)} />}

      {/* Generate button */}
      {approvedStoryboardId && (
        <div style={{ marginBottom: 16 }}>
          <button
            className="btn btn-primary"
            onClick={handleGenerate}
            disabled={genLoading}
          >
            {genLoading ? '⏳ Üretiliyor…' : '🎨 Görsel Plan Üret'}
          </button>
          <span className="cell-meta" style={{ marginLeft: 8 }}>Onaylanmış storyboard'dan görsel prompt planı üretir</span>
        </div>
      )}

      {!approvedStoryboardId && plans.length === 0 && (
        <div className="api-alert api-alert-info" style={{ marginBottom: 12 }}>
          <span className="api-alert-icon">ℹ</span>
          <span className="api-alert-message">
            Görsel plan üretmek için önce bir storyboard onaylanmalıdır.
          </span>
        </div>
      )}

      {/* Plan list or detail */}
      {selectedId && detail ? renderPlanDetail() : renderPlanList()}

      {/* Dialogs */}
      <ConfirmDialog
        open={confirmDialog.open}
        title={confirmDialog.title}
        message={confirmDialog.message}
        variant={confirmDialog.variant}
        loading={confirmLoading}
        onConfirm={confirmDialog.action}
        onCancel={() => setConfirmDialog(prev => ({ ...prev, open: false }))}
      />
      <NoteDialog
        open={noteDialog.open}
        title={noteDialog.title}
        label={noteDialog.label}
        variant={noteDialog.variant}
        loading={noteLoading}
        onConfirm={noteDialog.action}
        onCancel={() => setNoteDialog(prev => ({ ...prev, open: false }))}
      />
    </FormCard>
  )
}

export default VisualAssetWorkflowSection
