import { describe, it, expect } from 'vitest'
import { readFileSync } from 'fs'
import { resolve } from 'path'

/**
 * FP-012 Task 10: Review Worklist component safety regression tests.
 */

const WORKLIST_SOURCE = readFileSync(
  resolve(__dirname, '../ReviewWorklist.tsx'),
  'utf-8'
)

const CONSOLE_SOURCE = readFileSync(
  resolve(__dirname, '../ReviewConsole.tsx'),
  'utf-8'
)

describe('ReviewWorklist Safety Regression', () => {
  // ── Safe Rendering ──────────────────────────────────────

  it('worklist dangerouslySetInnerHTML kullanmıyor', () => {
    expect(WORKLIST_SOURCE).not.toContain('dangerouslySetInnerHTML')
  })

  it('worklist innerHTML kullanmıyor', () => {
    expect(WORKLIST_SOURCE).not.toContain('.innerHTML')
  })

  it('console dangerouslySetInnerHTML kullanmıyor', () => {
    expect(CONSOLE_SOURCE).not.toContain('dangerouslySetInnerHTML')
  })

  // ── No Publish/Export ───────────────────────────────────

  it('worklist publish button yok', () => {
    expect(WORKLIST_SOURCE).not.toContain("'PUBLISH'")
    expect(WORKLIST_SOURCE).not.toContain('"PUBLISH"')
  })

  it('worklist export button yok', () => {
    expect(WORKLIST_SOURCE).not.toContain("'EXPORT'")
    expect(WORKLIST_SOURCE).not.toContain('"EXPORT"')
  })

  it('worklist COMPLETED yok', () => {
    expect(WORKLIST_SOURCE).not.toContain("'COMPLETED'")
    expect(WORKLIST_SOURCE).not.toContain('"COMPLETED"')
  })

  it('worklist READY_FOR_PUBLISH yok', () => {
    expect(WORKLIST_SOURCE).not.toContain('READY_FOR_PUBLISH')
  })

  // ── No Forbidden Fields ─────────────────────────────────

  it('worklist providerUrl yok', () => {
    expect(WORKLIST_SOURCE).not.toContain('providerUrl')
  })

  it('worklist storageKey yok', () => {
    expect(WORKLIST_SOURCE).not.toContain('storageKey')
  })

  it('worklist publicUrl yok', () => {
    expect(WORKLIST_SOURCE).not.toContain('publicUrl')
  })

  it('worklist signedUrl yok', () => {
    expect(WORKLIST_SOURCE).not.toContain('signedUrl')
  })

  it('worklist rawPayload yok', () => {
    expect(WORKLIST_SOURCE).not.toContain('rawPayload')
  })

  it('worklist stackTrace yok', () => {
    expect(WORKLIST_SOURCE).not.toContain('stackTrace')
  })

  it('worklist apiKey yok', () => {
    expect(WORKLIST_SOURCE).not.toContain('apiKey')
  })

  // ── Console Integration ─────────────────────────────────

  it('worklist ReviewConsole bileşenini import ediyor', () => {
    expect(WORKLIST_SOURCE).toContain("import ReviewConsole from './ReviewConsole'")
  })

  it('worklist ReviewConsole bileşenini render ediyor', () => {
    expect(WORKLIST_SOURCE).toContain('<ReviewConsole')
  })

  it('worklist hedef kontekstini yüklüyor', () => {
    expect(WORKLIST_SOURCE).toContain('getTargetContext')
  })

  it('worklist iş listesini yüklüyor', () => {
    expect(WORKLIST_SOURCE).toContain('getWorklist')
  })

  // ── Filters ─────────────────────────────────────────────

  it('worklist durum filtresi mevcut', () => {
    expect(WORKLIST_SOURCE).toContain('review-filter-state')
  })

  it('worklist hedef tipi filtresi mevcut', () => {
    expect(WORKLIST_SOURCE).toContain('review-filter-target-type')
  })

  // ── Safe Text ───────────────────────────────────────────

  it('worklist safeText fonksiyonu kullanıyor', () => {
    expect(WORKLIST_SOURCE).toContain('function safeText')
  })

  // ── Generic Status Dropdown ─────────────────────────────

  it('worklist generic status dropdown yok', () => {
    expect(WORKLIST_SOURCE).not.toContain('statusDropdown')
    expect(WORKLIST_SOURCE).not.toContain('status-dropdown')
  })

  // ── APPROVED_FOR_INTERNAL_USE wording ───────────────────

  it('worklist APPROVED_FOR_INTERNAL_USE dahili olarak gösteriliyor', () => {
    expect(WORKLIST_SOURCE).toContain('Dahili Onay')
  })

  // ── FP-013 Task 04: Preview Integration ─────────────────

  it('worklist PACKAGE hedef tipi seçeneği mevcut', () => {
    expect(WORKLIST_SOURCE).toContain("'PACKAGE'")
  })

  it('worklist ReviewPreviewPanel import ediyor', () => {
    expect(WORKLIST_SOURCE).toContain("import ReviewPreviewPanel from './ReviewPreviewPanel'")
  })

  it('worklist ReviewPreviewPanel render ediyor', () => {
    expect(WORKLIST_SOURCE).toContain('<ReviewPreviewPanel')
  })

  // ── FP-015 Task 04: Inventory + Revision Integration ────

  it('worklist ApprovedInternalInventoryPanel import ediyor', () => {
    expect(WORKLIST_SOURCE).toContain("import ApprovedInternalInventoryPanel from './ApprovedInternalInventoryPanel'")
  })

  it('worklist ApprovedInternalInventoryPanel render ediyor', () => {
    expect(WORKLIST_SOURCE).toContain('<ApprovedInternalInventoryPanel')
  })

  it('worklist InternalRevisionRequestPanel import ediyor', () => {
    expect(WORKLIST_SOURCE).toContain("import InternalRevisionRequestPanel from './InternalRevisionRequestPanel'")
  })

  it('worklist InternalRevisionRequestPanel render ediyor', () => {
    expect(WORKLIST_SOURCE).toContain('<InternalRevisionRequestPanel')
  })

  it('worklist selectedReadiness state mevcut', () => {
    expect(WORKLIST_SOURCE).toContain('selectedReadiness')
  })

  it('worklist getInternalReadiness yüklüyor', () => {
    expect(WORKLIST_SOURCE).toContain('getInternalReadiness')
  })

  it('worklist handleRevisionRequested callback mevcut', () => {
    expect(WORKLIST_SOURCE).toContain('handleRevisionRequested')
  })

  it('worklist onRevisionRequested prop geçiyor', () => {
    expect(WORKLIST_SOURCE).toContain('onRevisionRequested={handleRevisionRequested}')
  })

  // ── FP-015 Task 04: IMAGE_ASSET Safety ──────────────────

  it('worklist img src kullanmıyor', () => {
    expect(WORKLIST_SOURCE).not.toMatch(/<img\s/)
  })

  it('worklist base64 yok', () => {
    expect(WORKLIST_SOURCE).not.toMatch(/\.base64/)
    expect(WORKLIST_SOURCE).not.toContain("'base64'")
  })
})
