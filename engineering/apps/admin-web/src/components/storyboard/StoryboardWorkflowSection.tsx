import { useState, useEffect, useCallback } from 'react'
import {
  generateStoryboardForScenario, listStoryboardsByContentProject, getStoryboardDetail,
  updateStoryboardDraft, approveStoryboardDraft, rejectStoryboardDraft,
} from '../../api/storyboardApi'
import type {
  StoryboardDraftSummaryResponse, StoryboardDraftResponse,
  StoryboardDraftEditPayload, StoryboardSceneEditPayload,
  StoryboardGenerateResponse, StoryboardStatus,
} from '../../types'
import FormCard from '../common/FormCard'
import StatusBadge from '../common/StatusBadge'
import LoadingSpinner from '../common/LoadingSpinner'
import ConfirmDialog from '../common/ConfirmDialog'
import NoteDialog from '../common/NoteDialog'
import ApiAlert from '../common/ApiAlert'

interface Props {
  contentProjectId: string
  projectStatus: string
  approvedScenarioId: string | null
}

const EDITABLE_STATUSES: StoryboardStatus[] = ['AI_GENERATED', 'NEEDS_REVIEW']
const ACTIONABLE_STATUSES: StoryboardStatus[] = ['AI_GENERATED', 'NEEDS_REVIEW']
const READONLY_STATUSES: StoryboardStatus[] = ['APPROVED', 'REJECTED', 'FAILED']

function StoryboardWorkflowSection({ contentProjectId, projectStatus: _projectStatus, approvedScenarioId }: Props) {
  // ── State ───────────────────────────────────────────────
  const [storyboards, setStoryboards] = useState<StoryboardDraftSummaryResponse[]>([])
  const [listLoading, setListLoading] = useState(false)
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [detail, setDetail] = useState<StoryboardDraftResponse | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [genLoading, setGenLoading] = useState(false)
  const [saveLoading, setSaveLoading] = useState(false)
  const [actionLoading, setActionLoading] = useState(false)

  // Edit form
  const [editForm, setEditForm] = useState<StoryboardDraftEditPayload>({})
  const [sceneForms, setSceneForms] = useState<StoryboardSceneEditPayload[]>([])
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

  // ── Data Loading ────────────────────────────────────────
  const loadList = useCallback(async () => {
    setListLoading(true)
    try {
      const res = await listStoryboardsByContentProject(contentProjectId)
      setStoryboards(res.data ?? [])
    } catch {
      setAlert({ type: 'error', message: 'Storyboard listesi yüklenemedi' })
    } finally {
      setListLoading(false)
    }
  }, [contentProjectId])

  useEffect(() => { loadList() }, [loadList])

  async function loadDetail(id: string) {
    setDetailLoading(true)
    setIsEditing(false)
    try {
      const res = await getStoryboardDetail(id)
      setDetail(res.data)
      setSelectedId(id)
      if (res.data) initEditForm(res.data)
    } catch {
      setAlert({ type: 'error', message: 'Storyboard detayı yüklenemedi' })
    } finally {
      setDetailLoading(false)
    }
  }

  function initEditForm(d: StoryboardDraftResponse) {
    setEditForm({
      title: d.title ?? '',
      summary: d.summary ?? '',
      targetPlatform: d.targetPlatform ?? '',
      aspectRatio: d.aspectRatio ?? '',
      estimatedDurationSeconds: d.estimatedDurationSeconds,
      visualStyleGuide: d.visualStyleGuide ?? '',
      narrationStyle: d.narrationStyle ?? '',
      reviewNotes: d.reviewNotes ?? '',
    })
    const forms: StoryboardSceneEditPayload[] = (d.scenes ?? []).map(s => ({
      sceneNumber: s.sceneNumber,
      sceneTitle: s.sceneTitle ?? '',
      visualDescription: s.visualDescription ?? '',
      narrationText: s.narrationText ?? '',
      onScreenText: s.onScreenText ?? '',
      cameraDirection: s.cameraDirection ?? '',
      transitionNote: s.transitionNote ?? '',
      mood: s.mood ?? '',
      location: s.location ?? '',
      characters: s.characters ?? '',
      estimatedDurationSeconds: s.estimatedDurationSeconds,
    }))
    setSceneForms(forms)
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
    if (!approvedScenarioId) return
    setGenLoading(true)
    setAlert(null)
    try {
      const res = await generateStoryboardForScenario(approvedScenarioId)
      const data: StoryboardGenerateResponse | null = res.data
      if (data?.success) {
        setAlert({ type: 'success', message: `Storyboard üretildi: ${data.storyboardTitle ?? 'v' + data.version} (${data.sceneCount} sahne)` })
        await loadList()
        if (data.storyboardDraftId) loadDetail(data.storyboardDraftId)
      } else {
        setAlert({ type: 'error', message: data?.message ?? 'Storyboard üretimi başarısız' })
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
      const payload: StoryboardDraftEditPayload = {
        ...editForm,
        scenes: sceneForms,
      }
      await updateStoryboardDraft(detail.id, payload)
      setAlert({ type: 'success', message: 'Storyboard kaydedildi' })
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
    openConfirm('Storyboard\'u Onayla', 'Bu storyboard\'u onaylamak istediğinize emin misiniz? Onay sonrası storyboard düzenlenemez.', 'primary', async () => {
      setActionLoading(true)
      try {
        await approveStoryboardDraft(detail.id)
        setAlert({ type: 'success', message: 'Storyboard onaylandı' })
        await loadDetail(detail.id)
        await loadList()
      } catch (err) {
        setAlert({ type: 'error', message: extractError(err) })
      } finally {
        setActionLoading(false)
      }
    })
  }

  function handleReject() {
    if (!detail) return
    openNote('Storyboard\'u Reddet', 'Red Sebebi', 'danger', async (reason: string) => {
      setActionLoading(true)
      try {
        await rejectStoryboardDraft(detail.id, { reason })
        setAlert({ type: 'success', message: 'Storyboard reddedildi' })
        await loadDetail(detail.id)
        await loadList()
      } catch (err) {
        setAlert({ type: 'error', message: extractError(err) })
      } finally {
        setActionLoading(false)
      }
    })
  }

  // ── Render: Storyboard List ────────────────────────────
  function renderStoryboardList() {
    if (listLoading) return <LoadingSpinner text="Storyboard'lar yükleniyor…" size="sm" />

    return (
      <div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 12 }}>
          <h3 style={{ margin: 0, fontSize: '0.95rem', color: 'var(--text-secondary)' }}>
            Storyboard Versiyonları ({storyboards.length})
          </h3>
          {approvedScenarioId ? (
            <button
              className="btn btn-primary"
              style={{ fontSize: '0.8rem', padding: '6px 14px' }}
              disabled={genLoading}
              onClick={handleGenerate}
            >
              {genLoading && <span className="loading-spinner" />}
              🎞️ Storyboard Üret
            </button>
          ) : (
            <span style={{ fontSize: '0.8rem', color: 'var(--warning)', fontStyle: 'italic' }}>
              ⚠️ Storyboard üretmek için önce onaylanmış senaryo gereklidir.
            </span>
          )}
        </div>

        {storyboards.length === 0 ? (
          <div className="empty-state" style={{ padding: 24 }}>
            <div className="empty-state-icon">🎞️</div>
            <div className="empty-state-title">Henüz storyboard yok</div>
            <div className="empty-state-text">
              {approvedScenarioId
                ? 'Onaylanmış senaryodan storyboard üretmek için yukarıdaki butonu kullanın.'
                : 'Storyboard üretmek için önce bir senaryoyu onaylamanız gerekiyor.'}
            </div>
          </div>
        ) : (
          <div className="data-table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th>V</th>
                  <th>Başlık</th>
                  <th>Durum</th>
                  <th>Sahne</th>
                  <th>Provider</th>
                  <th>Oluşturma</th>
                  <th>Aksiyonlar</th>
                </tr>
              </thead>
              <tbody>
                {storyboards.map(s => (
                  <tr key={s.id} style={selectedId === s.id ? { background: 'var(--bg-hover)' } : {}}>
                    <td style={{ fontWeight: 600, color: 'var(--primary)' }}>v{s.version}</td>
                    <td>
                      <span className="cell-title" style={{ cursor: 'pointer' }} onClick={() => loadDetail(s.id)}>
                        {s.title ?? '(Başlıksız)'}
                      </span>
                    </td>
                    <td><StatusBadge status={s.status} /></td>
                    <td style={{ textAlign: 'center' }}>{s.sceneCount}</td>
                    <td className="cell-meta">{s.providerType ?? '—'}</td>
                    <td className="cell-meta">{new Date(s.createdAt).toLocaleDateString('tr-TR')}</td>
                    <td className="cell-actions">
                      <button className="btn-action" onClick={() => loadDetail(s.id)}>
                        {selectedId === s.id ? '↻ Yenile' : 'Detay'}
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

  // ── Render: Storyboard Detail ──────────────────────────
  function renderStoryboardDetail() {
    if (!selectedId) return null
    if (detailLoading) return <LoadingSpinner text="Storyboard detayı yükleniyor…" size="sm" />
    if (!detail) return null

    const readonly = isReadOnly()
    const editing = isEditing && !readonly

    return (
      <div style={{ marginTop: 16 }}>
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <h3 style={{ margin: 0, fontSize: '0.95rem', color: 'var(--text-secondary)' }}>
            🎞️ Storyboard v{detail.version} — <StatusBadge status={detail.status} />
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
        </div>

        {/* Content Fields */}
        <div style={{ display: 'grid', gap: 12, gridTemplateColumns: '1fr 1fr', marginBottom: 16 }}>
          {renderField('Başlık', 'title', editForm.title, editing)}
          {renderField('Özet', 'summary', editForm.summary, editing, true)}
          {renderField('Hedef Platform', 'targetPlatform', editForm.targetPlatform, editing)}
          {renderField('En Boy Oranı', 'aspectRatio', editForm.aspectRatio, editing)}
          {renderField('Süre (sn)', 'estimatedDurationSeconds', String(editForm.estimatedDurationSeconds ?? ''), editing)}
          {renderField('Görsel Stil', 'visualStyleGuide', editForm.visualStyleGuide, editing)}
          {renderField('Anlatım Stili', 'narrationStyle', editForm.narrationStyle, editing)}
          <div style={{ gridColumn: '1 / -1' }}>
            {renderField('Review Notları', 'reviewNotes', editForm.reviewNotes, editing, true)}
          </div>
        </div>

        {/* Scenes */}
        <h4 style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', marginBottom: 8 }}>
          🎬 Sahneler ({detail.scenes?.length ?? 0})
        </h4>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {sceneForms.map((scene, idx) => (
            <div key={scene.sceneNumber} className="form-card" style={{ padding: '12px 16px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                <span style={{ fontWeight: 600, fontSize: '0.85rem', color: 'var(--primary)' }}>
                  {scene.sceneNumber}. {scene.sceneTitle || '(İsimsiz Sahne)'}
                  {detail.scenes?.[idx]?.sourceBeatType && (
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginLeft: 8 }}>
                      [{detail.scenes[idx].sourceBeatType}]
                    </span>
                  )}
                </span>
                {editing && (
                  <input
                    type="number" className="form-input"
                    style={{ width: 80, fontSize: '0.8rem', padding: '4px 8px' }}
                    placeholder="Süre (sn)"
                    value={scene.estimatedDurationSeconds ?? ''}
                    onChange={e => {
                      const val = e.target.value ? Number(e.target.value) : null
                      setSceneForms(prev => prev.map((s, i) => i === idx ? { ...s, estimatedDurationSeconds: val } : s))
                    }}
                  />
                )}
                {!editing && scene.estimatedDurationSeconds && (
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                    {scene.estimatedDurationSeconds}sn
                  </span>
                )}
              </div>

              {editing ? (
                <div style={{ display: 'grid', gap: 8, gridTemplateColumns: '1fr 1fr' }}>
                  {renderSceneField('Sahne Adı', idx, 'sceneTitle', scene.sceneTitle)}
                  {renderSceneField('Mood', idx, 'mood', scene.mood)}
                  {renderSceneField('Mekan', idx, 'location', scene.location)}
                  {renderSceneField('Karakterler', idx, 'characters', scene.characters)}
                  {renderSceneField('Kamera', idx, 'cameraDirection', scene.cameraDirection)}
                  {renderSceneField('Geçiş', idx, 'transitionNote', scene.transitionNote)}
                  {renderSceneField('Ekran Yazısı', idx, 'onScreenText', scene.onScreenText)}
                  <div style={{ gridColumn: '1 / -1' }}>
                    {renderSceneField('Görsel Açıklama', idx, 'visualDescription', scene.visualDescription, true)}
                  </div>
                  <div style={{ gridColumn: '1 / -1' }}>
                    {renderSceneField('Narration', idx, 'narrationText', scene.narrationText, true)}
                  </div>
                </div>
              ) : (
                <div style={{ display: 'grid', gap: 6, fontSize: '0.85rem', lineHeight: 1.6 }}>
                  {scene.visualDescription && (
                    <div><strong style={{ color: 'var(--text-secondary)' }}>Görsel:</strong> {scene.visualDescription}</div>
                  )}
                  {scene.narrationText && (
                    <div><strong style={{ color: 'var(--text-secondary)' }}>Narration:</strong> {scene.narrationText}</div>
                  )}
                  <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                    {scene.mood && <span>🎭 {scene.mood}</span>}
                    {scene.location && <span>📍 {scene.location}</span>}
                    {scene.characters && <span>👤 {scene.characters}</span>}
                    {scene.cameraDirection && <span>📷 {scene.cameraDirection}</span>}
                    {scene.transitionNote && <span>🔄 {scene.transitionNote}</span>}
                    {scene.onScreenText && <span>💬 {scene.onScreenText}</span>}
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
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
              onChange={e => setEditForm(prev => ({ ...prev, [key]: key === 'estimatedDurationSeconds' ? (e.target.value ? Number(e.target.value) : null) : e.target.value }))}
              type={key === 'estimatedDurationSeconds' ? 'number' : 'text'}
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

  function renderSceneField(label: string, idx: number, key: string, value: string | undefined, textarea = false) {
    return (
      <div className="form-group" style={{ margin: 0 }}>
        <label className="form-label" style={{ fontSize: '0.75rem' }}>{label}</label>
        {textarea ? (
          <textarea
            className="form-input"
            style={{ minHeight: 60, fontSize: '0.8rem', resize: 'vertical' }}
            value={value ?? ''}
            onChange={e => {
              const val = e.target.value
              setSceneForms(prev => prev.map((s, i) => i === idx ? { ...s, [key]: val } : s))
            }}
          />
        ) : (
          <input
            className="form-input"
            style={{ fontSize: '0.8rem' }}
            value={value ?? ''}
            onChange={e => {
              const val = e.target.value
              setSceneForms(prev => prev.map((s, i) => i === idx ? { ...s, [key]: val } : s))
            }}
          />
        )}
      </div>
    )
  }

  // ── Main Render ─────────────────────────────────────────
  return (
    <FormCard icon="🎞️" title="Storyboard İş Akışı" subtitle="FP-003 — Storyboard üretim ve review">
      {alert && <ApiAlert type={alert.type} message={alert.message} onClose={() => setAlert(null)} />}

      {renderStoryboardList()}
      {renderStoryboardDetail()}

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

export default StoryboardWorkflowSection
