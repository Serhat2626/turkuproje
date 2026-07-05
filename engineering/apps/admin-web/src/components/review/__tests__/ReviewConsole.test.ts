import { describe, it, expect } from 'vitest'
import { readFileSync } from 'fs'
import { resolve } from 'path'

/**
 * FP-012 Task 09: Review Console component safety regression tests.
 *
 * Verifies safe rendering rules without requiring a DOM render,
 * by inspecting the component source code for forbidden patterns.
 */

const COMPONENT_SOURCE = readFileSync(
  resolve(__dirname, '../ReviewConsole.tsx'),
  'utf-8'
)

describe('ReviewConsole Safety Regression', () => {
  // ── Safe Rendering ──────────────────────────────────────

  it('dangerouslySetInnerHTML kullanmıyor', () => {
    expect(COMPONENT_SOURCE).not.toContain('dangerouslySetInnerHTML')
  })

  it('innerHTML kullanmıyor', () => {
    expect(COMPONENT_SOURCE).not.toContain('.innerHTML')
  })

  // ── No Publish/Export UI ────────────────────────────────

  it('publish button/label yok', () => {
    // Check that no publish action label or button text exists
    expect(COMPONENT_SOURCE).not.toContain("'PUBLISH'")
    expect(COMPONENT_SOURCE).not.toContain('"PUBLISH"')
    expect(COMPONENT_SOURCE).not.toContain("'Yayınla'")
  })

  it('export button/label yok', () => {
    expect(COMPONENT_SOURCE).not.toContain("'EXPORT'")
    expect(COMPONENT_SOURCE).not.toContain('"EXPORT"')
  })

  it('COMPLETED action yok', () => {
    expect(COMPONENT_SOURCE).not.toContain("'COMPLETED'")
    expect(COMPONENT_SOURCE).not.toContain('"COMPLETED"')
    expect(COMPONENT_SOURCE).not.toContain("'MARK_COMPLETED'")
  })

  it('READY_FOR_PUBLISH action yok', () => {
    expect(COMPONENT_SOURCE).not.toContain('READY_FOR_PUBLISH')
  })

  it('AUTO_APPROVE action yok', () => {
    expect(COMPONENT_SOURCE).not.toContain('AUTO_APPROVE')
  })

  // ── No Forbidden Field Display ──────────────────────────

  it('providerUrl display yok', () => {
    expect(COMPONENT_SOURCE).not.toContain('providerUrl')
  })

  it('storageKey display yok', () => {
    expect(COMPONENT_SOURCE).not.toContain('storageKey')
  })

  it('publicUrl display yok', () => {
    expect(COMPONENT_SOURCE).not.toContain('publicUrl')
  })

  it('signedUrl display yok', () => {
    expect(COMPONENT_SOURCE).not.toContain('signedUrl')
  })

  it('rawPayload display yok', () => {
    expect(COMPONENT_SOURCE).not.toContain('rawPayload')
  })

  it('base64 display yok', () => {
    expect(COMPONENT_SOURCE).not.toContain('base64')
  })

  it('stackTrace display yok', () => {
    expect(COMPONENT_SOURCE).not.toContain('stackTrace')
  })

  it('apiKey display yok', () => {
    expect(COMPONENT_SOURCE).not.toContain('apiKey')
  })

  // ── APPROVED_FOR_INTERNAL_USE Wording ───────────────────

  it('APPROVED_FOR_INTERNAL_USE dahili olarak gösteriliyor', () => {
    expect(COMPONENT_SOURCE).toContain('Dahili Onay')
    expect(COMPONENT_SOURCE).toContain('İç Kullanım')
  })

  it('APPROVED_FOR_INTERNAL_USE yayın/export onayı olarak gösterilmiyor', () => {
    expect(COMPONENT_SOURCE).not.toContain('Yayın Onayı')
    expect(COMPONENT_SOURCE).not.toContain('Export Onayı')
    expect(COMPONENT_SOURCE).not.toContain('Publish Approved')
  })

  // ── CREATOR/SYSTEM Cannot Approve ───────────────────────

  it('CREATOR approve butonu gösterilmiyor (kaynak kodu kontrolü)', () => {
    // Component should filter out APPROVE_FOR_INTERNAL_USE for CREATOR
    expect(COMPONENT_SOURCE).toContain("actorRole === 'CREATOR'")
    expect(COMPONENT_SOURCE).toContain('APPROVE_FOR_INTERNAL_USE')
  })

  it('SYSTEM approve butonu gösterilmiyor (kaynak kodu kontrolü)', () => {
    expect(COMPONENT_SOURCE).toContain("actorRole === 'SYSTEM'")
  })

  // ── Safe Text Helper ────────────────────────────────────

  it('safeText fonksiyonu tanımlanmış', () => {
    expect(COMPONENT_SOURCE).toContain('function safeText')
  })

  it('safeText fonksiyonu HTML etiketlerini temizliyor', () => {
    expect(COMPONENT_SOURCE).toContain('[<>]')
  })

  it('safeText fonksiyonu javascript: URIlerini temizliyor', () => {
    expect(COMPONENT_SOURCE).toContain('javascript:')
  })

  // ── Generic Status Dropdown/Input ───────────────────────

  it('generic status dropdown yok', () => {
    expect(COMPONENT_SOURCE).not.toContain('statusDropdown')
    expect(COMPONENT_SOURCE).not.toContain('status-dropdown')
    expect(COMPONENT_SOURCE).not.toContain('generic-status')
  })

  it('arbitrary status text input yok', () => {
    expect(COMPONENT_SOURCE).not.toContain('statusInput')
    expect(COMPONENT_SOURCE).not.toContain('status-input')
  })

  // ── FP-012 Task 11: Availability-Driven Actions ─────────

  it('getAvailableActions çağrısı yapıyor', () => {
    expect(COMPONENT_SOURCE).toContain('getAvailableActions')
  })

  it('availableActions state yönetimi var', () => {
    expect(COMPONENT_SOURCE).toContain('setAvailableActions')
  })

  it('action disabled özelliği availability\'den geliyor', () => {
    expect(COMPONENT_SOURCE).toContain('disabled={!action.enabled')
  })

  it('disabled reason tooltip gösteriyor', () => {
    expect(COMPONENT_SOURCE).toContain('disabledReasonCode')
  })

  // ── FP-012 Task 11: UX Hardening ───────────────────────

  it('retry button mevcut', () => {
    expect(COMPONENT_SOURCE).toContain('review-retry-btn')
    expect(COMPONENT_SOURCE).toContain('Tekrar Dene')
  })

  it('char count gösterimi mevcut', () => {
    expect(COMPONENT_SOURCE).toContain('review-note-count')
  })

  it('note validation error gösterimi mevcut', () => {
    expect(COMPONENT_SOURCE).toContain('review-note-error')
  })

  it('empty actions mesajı mevcut', () => {
    expect(COMPONENT_SOURCE).toContain('review-no-actions')
    expect(COMPONENT_SOURCE).toContain('Kullanılabilir işlem yok')
  })

  it('empty timeline mesajı mevcut', () => {
    expect(COMPONENT_SOURCE).toContain('review-timeline-empty')
  })

  it('loading indicator mevcut', () => {
    expect(COMPONENT_SOURCE).toContain('review-loading')
  })

  it('onActionComplete callback desteği var', () => {
    expect(COMPONENT_SOURCE).toContain('onActionComplete')
  })

  it('maxLength güvenlik sınırı uygulanıyor', () => {
    expect(COMPONENT_SOURCE).toContain('MAX_NOTE_LENGTH')
    expect(COMPONENT_SOURCE).toContain('MAX_REASON_LENGTH')
  })

  it('note trim ve slice uygulanıyor', () => {
    expect(COMPONENT_SOURCE).toContain('note.trim().slice(0, MAX_NOTE_LENGTH)')
  })
})
