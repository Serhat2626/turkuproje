import { describe, it, expect } from 'vitest'
import { readFileSync } from 'fs'
import { resolve } from 'path'

/**
 * FP-015 Task 04: Internal Revision Request Panel safety & behavior tests.
 *
 * Source-inspection tests — no DOM rendering required.
 * Verifies safe rendering, actor/state eligibility, forbidden patterns,
 * form safety, success/error display, and integration.
 */

const PANEL_SOURCE = readFileSync(
  resolve(__dirname, '../InternalRevisionRequestPanel.tsx'),
  'utf-8'
)

describe('FP-015 Task 04 — InternalRevisionRequestPanel', () => {

  // ── A. Safe Rendering ────────────────────────────────────

  describe('A. Safe Rendering', () => {
    it('dangerouslySetInnerHTML kullanmıyor', () => {
      expect(PANEL_SOURCE).not.toContain('dangerouslySetInnerHTML')
    })

    it('innerHTML kullanmıyor', () => {
      expect(PANEL_SOURCE).not.toContain('.innerHTML')
    })

    it('__html kullanmıyor', () => {
      expect(PANEL_SOURCE).not.toContain('__html')
    })

    it('safeText fonksiyonu mevcut', () => {
      expect(PANEL_SOURCE).toContain('function safeText')
    })

    it('metin çıktılarında safeText kullanıyor', () => {
      const safeTextCalls = (PANEL_SOURCE.match(/safeText\(/g) || []).length
      expect(safeTextCalls).toBeGreaterThanOrEqual(3)
    })
  })

  // ── B. Internal-Only Labels ──────────────────────────────

  describe('B. Internal-Only Labels', () => {
    it('panel başlığı dahili-only vurgusu yapıyor', () => {
      expect(PANEL_SOURCE).toContain('Dahili')
      expect(PANEL_SOURCE).toContain('İç Kullanım')
    })

    it('disclaimer mevcut', () => {
      expect(PANEL_SOURCE).toContain('revision-disclaimer')
    })

    it('yayın/export/yeniden üretim değil açıklaması var', () => {
      expect(PANEL_SOURCE).toContain('yayın/export')
    })

    it('button publish/export gibi görünmüyor', () => {
      expect(PANEL_SOURCE).not.toContain('>Yayınla<')
      expect(PANEL_SOURCE).not.toContain('>Export<')
      expect(PANEL_SOURCE).not.toContain('>Regenerate<')
      expect(PANEL_SOURCE).not.toContain('>Yeniden Üret<')
    })
  })

  // ── C. Actor Eligibility ─────────────────────────────────

  describe('C. Actor Eligibility', () => {
    it('REVIEWER eligible', () => {
      expect(PANEL_SOURCE).toContain("'REVIEWER'")
    })

    it('ADMIN eligible', () => {
      expect(PANEL_SOURCE).toContain("'ADMIN'")
    })

    it('ELIGIBLE_ACTORS tanımı mevcut', () => {
      expect(PANEL_SOURCE).toContain('ELIGIBLE_ACTORS')
    })

    it('ineligible aktörler için null döndürüyor', () => {
      expect(PANEL_SOURCE).toContain('if (!isActorEligible) return null')
    })
  })

  // ── D. State Eligibility ─────────────────────────────────

  describe('D. State Eligibility', () => {
    it('APPROVED_FOR_INTERNAL_USE eligible', () => {
      expect(PANEL_SOURCE).toContain("'APPROVED_FOR_INTERNAL_USE'")
    })

    it('CHANGES_REQUESTED eligible (idempotent path)', () => {
      expect(PANEL_SOURCE).toContain("'CHANGES_REQUESTED'")
    })

    it('ELIGIBLE_STATES tanımı mevcut', () => {
      expect(PANEL_SOURCE).toContain('ELIGIBLE_STATES')
    })

    it('ineligible state mesajı data-testid mevcut', () => {
      expect(PANEL_SOURCE).toContain('revision-ineligible')
    })
  })

  // ── E. Form Elements ─────────────────────────────────────

  describe('E. Form Elements', () => {
    it('form data-testid mevcut', () => {
      expect(PANEL_SOURCE).toContain('revision-form')
    })

    it('reason input data-testid mevcut', () => {
      expect(PANEL_SOURCE).toContain('revision-reason-input')
    })

    it('message input data-testid mevcut', () => {
      expect(PANEL_SOURCE).toContain('revision-message-input')
    })

    it('submit button data-testid mevcut', () => {
      expect(PANEL_SOURCE).toContain('revision-submit-button')
    })

    it('maxLength kısıtlamaları mevcut', () => {
      expect(PANEL_SOURCE).toContain('MAX_MESSAGE_LENGTH')
      expect(PANEL_SOURCE).toContain('MAX_REASON_LENGTH')
    })
  })

  // ── F. Success/Error Display ─────────────────────────────

  describe('F. Success/Error Display', () => {
    it('success data-testid mevcut', () => {
      expect(PANEL_SOURCE).toContain('revision-success')
    })

    it('error data-testid mevcut', () => {
      expect(PANEL_SOURCE).toContain('revision-error')
    })

    it('error role=alert mevcut', () => {
      expect(PANEL_SOURCE).toContain('role="alert"')
    })

    it('state transition gösteriyor', () => {
      expect(PANEL_SOURCE).toContain('revision-result-state')
    })

    it('audit kayıt gösteriyor', () => {
      expect(PANEL_SOURCE).toContain('revision-result-audit')
    })

    it('note ekleme gösteriyor', () => {
      expect(PANEL_SOURCE).toContain('revision-result-note')
    })
  })

  // ── G. Forbidden Values ──────────────────────────────────

  describe('G. Forbidden Values', () => {
    it('providerUrl alanı yok', () => {
      expect(PANEL_SOURCE).not.toContain('providerUrl')
    })

    it('storageKey alanı yok', () => {
      expect(PANEL_SOURCE).not.toContain('storageKey')
    })

    it('publicUrl alanı yok', () => {
      expect(PANEL_SOURCE).not.toContain('publicUrl')
    })

    it('signedUrl alanı yok', () => {
      expect(PANEL_SOURCE).not.toContain('signedUrl')
    })

    it('stackTrace göstermiyor', () => {
      expect(PANEL_SOURCE).not.toContain('stackTrace')
    })

    it('img src kullanmıyor', () => {
      expect(PANEL_SOURCE).not.toMatch(/<img\s/)
    })

    it('base64 alanı yok', () => {
      expect(PANEL_SOURCE).not.toMatch(/\.base64/)
    })

    it('rawPayload alanı yok', () => {
      expect(PANEL_SOURCE).not.toContain('rawPayload')
    })
  })

  // ── H. API Integration ───────────────────────────────────

  describe('H. API Integration', () => {
    it('requestInternalRevision API kullanıyor', () => {
      expect(PANEL_SOURCE).toContain('requestInternalRevision')
    })

    it('reviewApi import ediyor', () => {
      expect(PANEL_SOURCE).toContain("import * as reviewApi from '../../api/reviewApi'")
    })

    it('onRevisionRequested callback mevcut', () => {
      expect(PANEL_SOURCE).toContain('onRevisionRequested')
    })
  })
})
