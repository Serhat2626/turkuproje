import { describe, it, expect } from 'vitest'
import { readFileSync } from 'fs'
import { resolve } from 'path'

/**
 * FP-014 Task 04: Internal Approval Readiness Panel safety & behavior tests.
 *
 * Source-inspection tests — no DOM rendering required.
 * Verifies safe rendering, forbidden patterns, label safety,
 * action taxonomy, IMAGE_ASSET safety, and integration expectations.
 */

const PANEL_SOURCE = readFileSync(
  resolve(__dirname, '../InternalApprovalReadinessPanel.tsx'),
  'utf-8'
)

const WORKLIST_SOURCE = readFileSync(
  resolve(__dirname, '../ReviewWorklist.tsx'),
  'utf-8'
)

const REVIEW_API_SOURCE = readFileSync(
  resolve(__dirname, '../../../api/reviewApi.ts'),
  'utf-8'
)

const TYPES_SOURCE = readFileSync(
  resolve(__dirname, '../../../types/index.ts'),
  'utf-8'
)

describe('FP-014 Task 04 — InternalApprovalReadinessPanel', () => {

  // ── A. Safe Rendering ────────────────────────────────────

  describe('A. Safe Rendering', () => {
    it('dangerouslySetInnerHTML kullanmıyor', () => {
      expect(PANEL_SOURCE).not.toContain('dangerouslySetInnerHTML')
    })

    it('innerHTML kullanmıyor', () => {
      expect(PANEL_SOURCE).not.toContain('.innerHTML')
    })

    it('raw HTML injection kullanmıyor', () => {
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

  // ── B. Availability State Rendering ──────────────────────

  describe('B. Availability State Rendering', () => {
    it('READY durumu dahili-only disclaimer gösteriyor', () => {
      expect(PANEL_SOURCE).toContain('readiness-disclaimer')
      expect(PANEL_SOURCE).toContain('yayın/export anlamına gelmez')
    })

    it('NOT_APPROVED durumu onay bekliyor mesajı gösteriyor', () => {
      expect(PANEL_SOURCE).toContain('readiness-not-approved')
      expect(PANEL_SOURCE).toContain('Manuel onay')
    })

    it('REJECTED durumu hazır değildir mesajı gösteriyor', () => {
      expect(PANEL_SOURCE).toContain('readiness-rejected')
      expect(PANEL_SOURCE).toContain('hazır değildir')
    })

    it('MISSING_TARGET / UNAVAILABLE durumu güvenli mesaj gösteriyor', () => {
      expect(PANEL_SOURCE).toContain('readiness-unavailable')
      expect(PANEL_SOURCE).toContain('MISSING_TARGET')
      expect(PANEL_SOURCE).toContain('UNAVAILABLE')
    })

    it('PREVIEW_UNAVAILABLE panel çökertmiyor', () => {
      expect(PANEL_SOURCE).toContain('PREVIEW_UNAVAILABLE')
      expect(PANEL_SOURCE).toContain("'Önizleme Kullanılamıyor'")
    })
  })

  // ── C. Safe Action Taxonomy ──────────────────────────────

  describe('C. Safe Action Taxonomy', () => {
    it('VIEW_SAFE_PREVIEW aksiyonu mevcut', () => {
      expect(PANEL_SOURCE).toContain('VIEW_SAFE_PREVIEW')
    })

    it('REVIEW_APPROVAL_DETAILS aksiyonu mevcut', () => {
      expect(PANEL_SOURCE).toContain('REVIEW_APPROVAL_DETAILS')
    })

    it('REQUEST_INTERNAL_REVISION aktif olarak gösteriliyor (FP-015)', () => {
      expect(PANEL_SOURCE).toContain('REQUEST_INTERNAL_REVISION')
      expect(PANEL_SOURCE).toContain('DEFERRED_ACTIONS')
      // REQUEST_INTERNAL_REVISION artık DEFERRED_ACTIONS dizisinde DEĞİL
      const deferredMatch = PANEL_SOURCE.match(/DEFERRED_ACTIONS[^]]*?\]/)
      if (deferredMatch) {
        expect(deferredMatch[0]).not.toContain('REQUEST_INTERNAL_REVISION')
      }
    })

    it('PREPARE_FOR_FUTURE_EXPORT bilgilendirme olarak gösteriliyor', () => {
      expect(PANEL_SOURCE).toContain('PREPARE_FOR_FUTURE_EXPORT')
      expect(PANEL_SOURCE).toContain('bilgilendirme')
    })

    it('REQUEST_INTERNAL_REVISION POST davranışı yok', () => {
      expect(PANEL_SOURCE).not.toContain("method: 'POST'")
      expect(PANEL_SOURCE).not.toContain('fetch(')
    })
  })

  // ── D. Forbidden Values ──────────────────────────────────

  describe('D. Forbidden Values', () => {
    const forbiddenActions = [
      'PUBLISH', 'EXPORT', 'PUBLIC_DELIVERY',
      'CREATE_PUBLIC_URL', 'CREATE_SIGNED_URL',
      'CDN_DELIVERY', 'USER_MEDIA_LIBRARY',
      'READY_FOR_PUBLISH', 'PUBLISHED', 'EXPORTED', 'COMPLETED',
    ]

    forbiddenActions.forEach(action => {
      it(`yasaklı aksiyon '${action}' panelde yok`, () => {
        // Check as exact string match (not substring of other values)
        const regex = new RegExp(`'${action}'|"${action}"`)
        expect(PANEL_SOURCE).not.toMatch(regex)
      })
    })
  })

  // ── E. IMAGE_ASSET Safety ────────────────────────────────

  describe('E. IMAGE_ASSET Safety', () => {
    it('img src kullanmıyor', () => {
      expect(PANEL_SOURCE).not.toMatch(/<img\s/)
      expect(PANEL_SOURCE).not.toContain('img src')
    })

    it('publicUrl alanı yok', () => {
      expect(PANEL_SOURCE).not.toContain('publicUrl')
    })

    it('signedUrl alanı yok', () => {
      expect(PANEL_SOURCE).not.toContain('signedUrl')
    })

    it('providerUrl alanı yok', () => {
      expect(PANEL_SOURCE).not.toContain('providerUrl')
    })

    it('storageKey alanı yok', () => {
      expect(PANEL_SOURCE).not.toContain('storageKey')
    })

    it('base64 alanı yok', () => {
      // Check for base64 as a field reference, not in JSDoc comments
      expect(PANEL_SOURCE).not.toMatch(/\.base64/)
      expect(PANEL_SOURCE).not.toMatch(/readiness\.base64/)
      expect(PANEL_SOURCE).not.toContain("'base64'")
    })

    it('rawPayload alanı yok', () => {
      expect(PANEL_SOURCE).not.toContain('rawPayload')
    })

    it('bytes alanı yok', () => {
      expect(PANEL_SOURCE).not.toContain('.bytes')
    })
  })

  // ── F. Error/Loading Safety ──────────────────────────────

  describe('F. Error/Loading Safety', () => {
    it('loading state data-testid mevcut', () => {
      expect(PANEL_SOURCE).toContain('readiness-loading')
    })

    it('error state data-testid mevcut', () => {
      expect(PANEL_SOURCE).toContain('readiness-error')
    })

    it('stackTrace göstermiyor', () => {
      expect(PANEL_SOURCE).not.toContain('stackTrace')
      expect(PANEL_SOURCE).not.toContain('stack_trace')
    })

    it('exception class göstermiyor', () => {
      // Check for Exception rendering, not JSDoc comments
      expect(PANEL_SOURCE).not.toContain('exception')
      expect(PANEL_SOURCE).not.toContain('.stackTrace')
    })

    it('provider/storage/config detayı göstermiyor', () => {
      expect(PANEL_SOURCE).not.toContain('providerDetail')
      expect(PANEL_SOURCE).not.toContain('storageDetail')
      expect(PANEL_SOURCE).not.toContain('configDetail')
    })
  })

  // ── G. Safe Labels ──────────────────────────────────────

  describe('G. Safe Labels', () => {
    it("'Dahili Hazır' label mevcut", () => {
      expect(PANEL_SOURCE).toContain('Dahili Hazır')
    })

    it("'Onay Bekliyor' label mevcut", () => {
      expect(PANEL_SOURCE).toContain('Onay Bekliyor')
    })

    it("'Reddedildi' label mevcut", () => {
      expect(PANEL_SOURCE).toContain('Reddedildi')
    })

    it("'Hedef Bulunamadı' label mevcut", () => {
      expect(PANEL_SOURCE).toContain('Hedef Bulunamadı')
    })

    it("'Önizleme Kullanılamıyor' label mevcut", () => {
      expect(PANEL_SOURCE).toContain('Önizleme Kullanılamıyor')
    })

    it("'bilgilendirme' deferred işareti mevcut", () => {
      expect(PANEL_SOURCE).toContain('bilgilendirme')
    })
  })

  // ── H. Data TestIDs ─────────────────────────────────────

  describe('H. Data TestIDs', () => {
    const expectedTestIds = [
      'readiness-panel',
      'readiness-loading',
      'readiness-error',
      'readiness-content',
      'readiness-availability',
      'readiness-disclaimer',
      'readiness-not-approved',
      'readiness-rejected',
      'readiness-unavailable',
      'readiness-safe-title',
      'readiness-safe-summary',
      'readiness-actions',
      'readiness-warnings',
    ]

    expectedTestIds.forEach(testId => {
      it(`data-testid="${testId}" mevcut`, () => {
        expect(PANEL_SOURCE).toContain(`data-testid="${testId}"`)
      })
    })
  })
})

// ── I. Integration ──────────────────────────────────────────

describe('FP-014 Task 04 — ReviewWorklist Integration', () => {
  it('ReviewWorklist InternalApprovalReadinessPanel import ediyor', () => {
    expect(WORKLIST_SOURCE).toContain("import InternalApprovalReadinessPanel from './InternalApprovalReadinessPanel'")
  })

  it('ReviewWorklist InternalApprovalReadinessPanel render ediyor', () => {
    expect(WORKLIST_SOURCE).toContain('<InternalApprovalReadinessPanel')
  })

  it('ReviewWorklist ReviewPreviewPanel hâlâ render ediyor', () => {
    expect(WORKLIST_SOURCE).toContain('<ReviewPreviewPanel')
  })

  it('ReviewWorklist ReviewConsole hâlâ render ediyor', () => {
    expect(WORKLIST_SOURCE).toContain('<ReviewConsole')
  })

  it('ReviewWorklist publish/export button yok', () => {
    // Only check for publish/export as button labels or data-testid, not in comments
    expect(WORKLIST_SOURCE).not.toContain('data-testid="publish')
    expect(WORKLIST_SOURCE).not.toContain('data-testid="export')
    expect(WORKLIST_SOURCE).not.toContain('>Yayınla<')
    expect(WORKLIST_SOURCE).not.toContain('>Export<')
  })

  it('ReviewWorklist dangerouslySetInnerHTML kullanmıyor', () => {
    expect(WORKLIST_SOURCE).not.toContain('dangerouslySetInnerHTML')
  })
})

// ── J. Types Safety ─────────────────────────────────────────

describe('FP-014 Task 04 — Types Safety', () => {
  it('SafeInternalAction tipi mevcut', () => {
    expect(TYPES_SOURCE).toContain('SafeInternalAction')
  })

  it('InternalApprovalReadinessAvailability tipi mevcut', () => {
    expect(TYPES_SOURCE).toContain('InternalApprovalReadinessAvailability')
  })

  it('SafeInternalApprovalReadiness interface mevcut', () => {
    expect(TYPES_SOURCE).toContain('SafeInternalApprovalReadiness')
  })

  const forbiddenTypeValues = [
    'PUBLISH', 'EXPORT', 'PUBLIC_DELIVERY',
    'CREATE_PUBLIC_URL', 'CREATE_SIGNED_URL',
    'CDN_DELIVERY', 'USER_MEDIA_LIBRARY',
    'READY_FOR_PUBLISH', 'PUBLISHED', 'EXPORTED', 'COMPLETED',
  ]

  forbiddenTypeValues.forEach(value => {
    it(`yasaklı değer '${value}' FP-014 type tanımlarında yok`, () => {
      // Extract FP-014 section only (stop before FP-019 additions)
      const fp014Start = TYPES_SOURCE.indexOf('FP-014 Task 04: Internal Approval Readiness')
      const fp019Start = TYPES_SOURCE.indexOf('FP-019')
      const fp014Section = fp019Start > fp014Start
        ? TYPES_SOURCE.substring(fp014Start, fp019Start)
        : TYPES_SOURCE.substring(fp014Start)
      expect(fp014Section).not.toContain(`'${value}'`)
    })
  })

  it('SafeInternalApprovalReadiness publicUrl alanı yok', () => {
    const fp014Section = TYPES_SOURCE.substring(
      TYPES_SOURCE.indexOf('SafeInternalApprovalReadiness')
    )
    expect(fp014Section).not.toContain('publicUrl')
    expect(fp014Section).not.toContain('signedUrl')
    expect(fp014Section).not.toContain('providerUrl')
    expect(fp014Section).not.toContain('storageKey')
    expect(fp014Section).not.toContain('base64')
    expect(fp014Section).not.toContain('rawPayload')
    expect(fp014Section).not.toContain('bytes')
    expect(fp014Section).not.toContain('secret')
    expect(fp014Section).not.toContain('apiKey')
  })
})

// ── K. API Client Safety ────────────────────────────────────

describe('FP-014 Task 04 — API Client Safety', () => {
  it('getInternalReadiness doğru endpoint kullanıyor', () => {
    expect(REVIEW_API_SOURCE).toContain('/internal-readiness')
  })

  it('getApprovedInternalReadiness doğru endpoint kullanıyor', () => {
    expect(REVIEW_API_SOURCE).toContain('/approved/internal-readiness')
  })

  it('reviewApi publish/export metodu yok', () => {
    const forbidden = [
      'publish', 'getPublicUrl', 'getSignedUrl',
      'createPublicUrl', 'createSignedUrl',
      'inspectStorage', 'inspectProvider', 'getProviderUrl',
      'getStorageKey', 'getRawPayload', 'getBase64',
    ]
    const exportedNames = (REVIEW_API_SOURCE.match(/export function (\w+)/g) || []).map(
      m => m.replace('export function ', '')
    )
    forbidden.forEach(fn => {
      expect(exportedNames).not.toContain(fn)
    })
  })
})
