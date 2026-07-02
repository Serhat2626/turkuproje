import { useState, useEffect, useCallback } from 'react'
import {
  generateScenario, listProjectScenarios, getScenarioDetail,
  updateScenario, approveScenario, rejectScenario,
} from '../../api/scenarioApi'
import type {
  ScenarioDraftSummaryResponse, ScenarioDraftResponse,
  ScenarioDraftEditPayload, ScenarioBeatEditPayload,
  ScenarioGenerateResponse, ScenarioStatus,
} from '../../types'
import { SCENARIO_BEAT_TYPES } from '../../types'
import FormCard from '../common/FormCard'
import StatusBadge from '../common/StatusBadge'
import LoadingSpinner from '../common/LoadingSpinner'
import ConfirmDialog from '../common/ConfirmDialog'
import NoteDialog from '../common/NoteDialog'
import ApiAlert from '../common/ApiAlert'

interface Props {
  contentProjectId: string
  projectStatus: string
}

const EDITABLE_STATUSES: ScenarioStatus[] = ['AI_GENERATED', 'NEEDS_REVIEW']
const ACTIONABLE_STATUSES: ScenarioStatus[] = ['AI_GENERATED', 'NEEDS_REVIEW']
const READONLY_STATUSES: ScenarioStatus[] = ['APPROVED', 'REJECTED', 'FAILED']

const BEAT_LABEL_MAP: Record<string, string> = Object.fromEntries(
  SCENARIO_BEAT_TYPES.map(b => [b.value, b.label])
)

function ScenarioWorkflowSection({ contentProjectId, projectStatus: _projectStatus }: Props) {
  // ── State ───────────────────────────────────────────────
  const [scenarios, setScenarios] = useState<ScenarioDraftSummaryResponse[]>([])
  const [listLoading, setListLoading] = useState(false)
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [detail, setDetail] = useState<ScenarioDraftResponse | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [genLoading, setGenLoading] = useState(false)
  const [saveLoading, setSaveLoading] = useState(false)
  const [actionLoading, setActionLoading] = useState(false)

  // Edit form
  const [editForm, setEditForm] = useState<ScenarioDraftEditPayload>({})
  const [beatForms, setBeatForms] = useState<ScenarioBeatEditPayload[]>([])
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
      const res = await listProjectScenarios(contentProjectId)
      setScenarios(res.data ?? [])
    } catch {
      setAlert({ type: 'error', message: 'Senaryo listesi yüklenemedi' })
    } finally {
      setListLoading(false)
    }
  }, [contentProjectId])

  useEffect(() => { loadList() }, [loadList])

  async function loadDetail(id: string) {
    setDetailLoading(true)
    setIsEditing(false)
    try {
      const res = await getScenarioDetail(id)
      setDetail(res.data)
      setSelectedId(id)
      if (res.data) initEditForm(res.data)
    } catch {
      setAlert({ type: 'error', message: 'Senaryo detayı yüklenemedi' })
    } finally {
      setDetailLoading(false)
    }
  }

  function initEditForm(d: ScenarioDraftResponse) {
    setEditForm({
      title: d.title ?? '',
      summary: d.summary ?? '',
      targetAudience: d.targetAudience ?? '',
      culturalObjective: d.culturalObjective ?? '',
      tone: d.tone ?? '',
      estimatedDurationSeconds: d.estimatedDurationSeconds,
      setting: d.setting ?? '',
      characters: d.characters ?? '',
      reviewNotes: d.reviewNotes ?? '',
    })
    // Ensure all 6 beats exist in form
    const beatsMap = new Map(d.beats.map(b => [b.type, b]))
    const forms: ScenarioBeatEditPayload[] = SCENARIO_BEAT_TYPES.map(bt => {
      const existing = beatsMap.get(bt.value)
      return {
        type: bt.value,
        label: existing?.label ?? bt.label,
        body: existing?.body ?? '',
        orderIndex: existing?.orderIndex ?? bt.order,
        estimatedDurationSeconds: existing?.estimatedDurationSeconds ?? null,
      }
    })
    setBeatForms(forms)
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
    setGenLoading(true)
    setAlert(null)
    try {
      const res = await generateScenario(contentProjectId)
      const data: ScenarioGenerateResponse | null = res.data
      if (data?.success) {
        setAlert({ type: 'success', message: `Senaryo üretildi: ${data.scenarioTitle ?? 'v' + data.version}` })
        await loadList()
        if (data.scenarioDraftId) loadDetail(data.scenarioDraftId)
      } else {
        setAlert({ type: 'error', message: data?.message ?? 'Senaryo üretimi başarısız' })
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
      const payload: ScenarioDraftEditPayload = {
        ...editForm,
        beats: beatForms,
      }
      await updateScenario(detail.id, payload)
      setAlert({ type: 'success', message: 'Senaryo kaydedildi' })
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
    openConfirm('Senaryoyu Onayla', 'Bu senaryoyu onaylamak istediğinize emin misiniz? Onay sonrası senaryo düzenlenemez.', 'primary', async () => {
      setActionLoading(true)
      try {
        await approveScenario(detail.id)
        setAlert({ type: 'success', message: 'Senaryo onaylandı' })
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
    openNote('Senaryoyu Reddet', 'Red Sebebi', 'danger', async (reason: string) => {
      setActionLoading(true)
      try {
        await rejectScenario(detail.id, { reason })
        setAlert({ type: 'success', message: 'Senaryo reddedildi' })
        await loadDetail(detail.id)
        await loadList()
      } catch (err) {
        setAlert({ type: 'error', message: extractError(err) })
      } finally {
        setActionLoading(false)
      }
    })
  }

  // ── Render: Scenario List ───────────────────────────────
  function renderScenarioList() {
    if (listLoading) return <LoadingSpinner text="Senaryolar yükleniyor…" size="sm" />

    return (
      <div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 12 }}>
          <h3 style={{ margin: 0, fontSize: '0.95rem', color: 'var(--text-secondary)' }}>
            Senaryo Versiyonları ({scenarios.length})
          </h3>
          <button
            className="btn btn-primary"
            style={{ fontSize: '0.8rem', padding: '6px 14px' }}
            disabled={genLoading}
            onClick={handleGenerate}
          >
            {genLoading && <span className="loading-spinner" />}
            🎬 Senaryo Üret
          </button>
        </div>

        {scenarios.length === 0 ? (
          <div className="empty-state" style={{ padding: 24 }}>
            <div className="empty-state-icon">🎬</div>
            <div className="empty-state-title">Henüz senaryo yok</div>
            <div className="empty-state-text">
              Onaylanmış içerik taslağından senaryo üretmek için yukarıdaki butonu kullanın.
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
                  <th>Oluşturma</th>
                  <th>Aksiyonlar</th>
                </tr>
              </thead>
              <tbody>
                {scenarios.map(s => (
                  <tr key={s.id} style={selectedId === s.id ? { background: 'var(--bg-hover)' } : {}}>
                    <td style={{ fontWeight: 600, color: 'var(--primary)' }}>v{s.version}</td>
                    <td>
                      <span className="cell-title" style={{ cursor: 'pointer' }} onClick={() => loadDetail(s.id)}>
                        {s.title ?? '(Başlıksız)'}
                      </span>
                    </td>
                    <td><StatusBadge status={s.status} /></td>
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

  // ── Render: Scenario Detail ─────────────────────────────
  function renderScenarioDetail() {
    if (!selectedId) return null
    if (detailLoading) return <LoadingSpinner text="Senaryo detayı yükleniyor…" size="sm" />
    if (!detail) return null

    const readonly = isReadOnly()
    const editing = isEditing && !readonly

    return (
      <div style={{ marginTop: 16 }}>
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <h3 style={{ margin: 0, fontSize: '0.95rem', color: 'var(--text-secondary)' }}>
            📋 Senaryo v{detail.version} — <StatusBadge status={detail.status} />
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
          {renderField('Hedef Kitle', 'targetAudience', editForm.targetAudience, editing)}
          {renderField('Kültürel Amaç', 'culturalObjective', editForm.culturalObjective, editing, true)}
          {renderField('Ton', 'tone', editForm.tone, editing)}
          {renderField('Süre (sn)', 'estimatedDurationSeconds', String(editForm.estimatedDurationSeconds ?? ''), editing)}
          {renderField('Mekan', 'setting', editForm.setting, editing)}
          {renderField('Karakterler', 'characters', editForm.characters, editing)}
          <div style={{ gridColumn: '1 / -1' }}>
            {renderField('Review Notları', 'reviewNotes', editForm.reviewNotes, editing, true)}
          </div>
        </div>

        {/* Beats */}
        <h4 style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', marginBottom: 8 }}>
          🎵 Beat'ler ({detail.beats.length}/6)
        </h4>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {beatForms.map((beat, idx) => (
            <div key={beat.type} className="form-card" style={{ padding: '12px 16px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                <span style={{ fontWeight: 600, fontSize: '0.85rem', color: 'var(--primary)' }}>
                  {idx + 1}. {BEAT_LABEL_MAP[beat.type] ?? beat.type}
                </span>
                {editing && (
                  <input
                    type="number" className="form-input"
                    style={{ width: 80, fontSize: '0.8rem', padding: '4px 8px' }}
                    placeholder="Süre (sn)"
                    value={beat.estimatedDurationSeconds ?? ''}
                    onChange={e => {
                      const val = e.target.value ? Number(e.target.value) : null
                      setBeatForms(prev => prev.map((b, i) => i === idx ? { ...b, estimatedDurationSeconds: val } : b))
                    }}
                  />
                )}
                {!editing && beat.estimatedDurationSeconds && (
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                    {beat.estimatedDurationSeconds}sn
                  </span>
                )}
              </div>
              {editing ? (
                <textarea
                  className="form-input"
                  style={{ minHeight: 80, fontSize: '0.85rem', resize: 'vertical' }}
                  value={beat.body}
                  onChange={e => {
                    const val = e.target.value
                    setBeatForms(prev => prev.map((b, i) => i === idx ? { ...b, body: val } : b))
                  }}
                />
              ) : (
                <p style={{ margin: 0, fontSize: '0.85rem', lineHeight: 1.6, whiteSpace: 'pre-wrap', color: 'var(--text-primary)' }}>
                  {beat.body || <span style={{ color: 'var(--text-muted)', fontStyle: 'italic' }}>(İçerik yok)</span>}
                </p>
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

  // ── Main Render ─────────────────────────────────────────
  return (
    <FormCard icon="🎬" title="Senaryo İş Akışı" subtitle="FP-002 — Senaryo üretim ve review">
      {alert && <ApiAlert type={alert.type} message={alert.message} onClose={() => setAlert(null)} />}

      {renderScenarioList()}
      {renderScenarioDetail()}

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

export default ScenarioWorkflowSection
