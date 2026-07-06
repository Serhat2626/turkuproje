import { describe, it, expect } from 'vitest'
import { readFileSync } from 'fs'
import { resolve } from 'path'

/**
 * FP-017 Task 04: Workflow Observability Panel safety & behavior tests.
 *
 * Source-inspection tests — no DOM rendering required.
 * Verifies safe rendering, forbidden patterns, label safety,
 * loading/error/empty states, and integration.
 */

const PANEL_SOURCE = readFileSync(
  resolve(__dirname, '../WorkflowObservabilityPanel.tsx'),
  'utf-8'
)

const WORKLIST_SOURCE = readFileSync(
  resolve(__dirname, '../ReviewWorklist.tsx'),
  'utf-8'
)

const API_SOURCE = readFileSync(
  resolve(__dirname, '../../../api/reviewApi.ts'),
  'utf-8'
)

const TYPES_SOURCE = readFileSync(
  resolve(__dirname, '../../../types/index.ts'),
  'utf-8'
)

describe('FP-017 Task 04 — WorkflowObservabilityPanel', () => {

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

    it('safeText XSS koruması', () => {
      expect(PANEL_SOURCE).toContain('.replace(/[<>]/g')
    })
  })

  // ── B. Internal-Only Labels ──────────────────────────────

  describe('B. Internal-Only Labels', () => {
    it('Dahili İş Akışı Gözlemi başlık', () => {
      expect(PANEL_SOURCE).toContain('Dahili İş Akışı Gözlemi')
    })

    it('Operasyonel Özet label', () => {
      expect(PANEL_SOURCE).toContain('Operasyonel Özet')
    })

    it('Güvenli Tanılar label', () => {
      expect(PANEL_SOURCE).toContain('Güvenli Tanılar')
    })

    it('Bu panel işlem çalıştırmaz disclaimer', () => {
      expect(PANEL_SOURCE).toContain('Bu panel işlem çalıştırmaz')
    })

    it('Yayın/export değildir disclaimer', () => {
      expect(PANEL_SOURCE).toContain('Yayın/export değildir')
    })

    it('Sağlayıcı/depolama detayı gösterilmez disclaimer', () => {
      expect(PANEL_SOURCE).toContain('Sağlayıcı/depolama detayı gösterilmez')
    })

    it('Güvenli Uyarılar label', () => {
      expect(PANEL_SOURCE).toContain('Güvenli Uyarılar')
    })
  })

  // ── C. Diagnostic Type Labels ────────────────────────────

  describe('C. Diagnostic Type Labels', () => {
    const expectedLabels = [
      'Eski İnceleme',
      'Önizleme Yok',
      'Hazırlık Belirlenemedi',
      'Pipeline Belirlenemedi',
      'Revizyon İstendi',
      'Hedef Bulunamadı',
      'İş Akışı Engeli',
      'Kısıtlı Görünürlük',
    ]
    expectedLabels.forEach(label => {
      it(`'${label}' label mevcut`, () => {
        expect(PANEL_SOURCE).toContain(label)
      })
    })
  })

  // ── D. Control Hint Labels ───────────────────────────────

  describe('D. Control Hint Labels', () => {
    const expectedLabels = [
      'Genel görünümü yenile',
      'Hazırlık durumunu kontrol et',
      'Güvenli önizlemeyi görüntüle',
      'Pipeline detayını incele',
      'Manuel inceleme öner',
    ]
    expectedLabels.forEach(label => {
      it(`'${label}' hint label mevcut`, () => {
        expect(PANEL_SOURCE).toContain(label)
      })
    })
  })

  // ── E. Loading/Error/Empty States ────────────────────────

  describe('E. Loading/Error/Empty States', () => {
    it('loading state mevcut', () => {
      expect(PANEL_SOURCE).toContain('Yükleniyor')
    })

    it('error state mevcut', () => {
      expect(PANEL_SOURCE).toContain('observability-error')
    })

    it('empty state mevcut', () => {
      expect(PANEL_SOURCE).toContain('Gözlemlenebilirlik verisi bulunamadı')
    })

    it('diagnostic empty state mevcut', () => {
      expect(PANEL_SOURCE).toContain('Tanı öğesi bulunamadı')
    })
  })

  // ── F. Summary Counts ────────────────────────────────────

  describe('F. Summary Counts', () => {
    it('countsByReviewState render', () => {
      expect(PANEL_SOURCE).toContain('countsByReviewState')
    })

    it('countsByPipelineStage render', () => {
      expect(PANEL_SOURCE).toContain('countsByPipelineStage')
    })

    it('countsByReadinessAvailability render', () => {
      expect(PANEL_SOURCE).toContain('countsByReadinessAvailability')
    })

    it('preview counts render', () => {
      expect(PANEL_SOURCE).toContain('previewAvailableCount')
      expect(PANEL_SOURCE).toContain('previewUnavailableCount')
    })

    it('revision count render', () => {
      expect(PANEL_SOURCE).toContain('revisionRequestedCount')
    })

    it('unavailable count render', () => {
      expect(PANEL_SOURCE).toContain('unavailableCount')
    })
  })

  // ── G. Diagnostic Card ───────────────────────────────────

  describe('G. Diagnostic Card', () => {
    it('diagnostic card test-id mevcut', () => {
      expect(PANEL_SOURCE).toContain('diagnostic-card')
    })

    it('diagnostic type badge mevcut', () => {
      expect(PANEL_SOURCE).toContain('diagnostic-type-badge')
    })

    it('diagnostic message render', () => {
      expect(PANEL_SOURCE).toContain('safeMessage')
    })

    it('diagnostic reason code render', () => {
      expect(PANEL_SOURCE).toContain('safeReasonCode')
    })

    it('diagnostic hints render', () => {
      expect(PANEL_SOURCE).toContain('diagnostic-hints')
    })
  })

  // ── H. Diagnostic Filter ────────────────────────────────

  describe('H. Diagnostic Filter', () => {
    it('diagnostic filter mevcut', () => {
      expect(PANEL_SOURCE).toContain('diagnostic-filter')
    })

    it('filter Tümü default', () => {
      expect(PANEL_SOURCE).toContain('Tümü')
    })

    it('filter by target type', () => {
      expect(PANEL_SOURCE).toContain('diagnosticFilter')
    })
  })

  // ── I. Forbidden Fields/Strings ──────────────────────────

  describe('I. Forbidden Fields/Strings', () => {
    const FORBIDDEN = [
      'publicUrl', 'signedUrl', 'cdnUrl', 'providerUrl',
      'storageKey', 'bucket', 'objectPath', 'rawPayload',
      'bytes', 'secret', 'apiKey',
      'stackTrace', 'exceptionClass',
      'publishStatus', 'exportStatus', 'publishedAt', 'exportedAt',
      'PROVIDER_ERROR', 'STORAGE_ERROR',
    ]

    FORBIDDEN.forEach(field => {
      it(`panel source '${field}' içermez`, () => {
        expect(PANEL_SOURCE).not.toContain(field)
      })
    })
  })

  // ── J. No Forbidden Controls ─────────────────────────────

  describe('J. No Forbidden Controls', () => {
    it('no publish button/handler', () => {
      expect(PANEL_SOURCE).not.toMatch(/onClick.*[Pp]ublish/)
      expect(PANEL_SOURCE).not.toMatch(/handlePublish/)
    })

    it('no export button/handler', () => {
      expect(PANEL_SOURCE).not.toMatch(/onClick.*[Ee]xport/)
      expect(PANEL_SOURCE).not.toMatch(/handleExport/)
    })

    it('no approve button/handler', () => {
      expect(PANEL_SOURCE).not.toMatch(/handleApprove/)
      expect(PANEL_SOURCE).not.toMatch(/handleAutoApprove/)
    })

    it('no regenerate button/handler', () => {
      expect(PANEL_SOURCE).not.toMatch(/handleRegenerate/)
      expect(PANEL_SOURCE).not.toMatch(/onClick.*[Rr]egenerate/)
    })

    it('no auto-fix button/handler', () => {
      expect(PANEL_SOURCE).not.toMatch(/handleAutoFix/)
      expect(PANEL_SOURCE).not.toMatch(/onClick.*auto.?fix/i)
    })
  })

  // ── K. API Client ────────────────────────────────────────

  describe('K. API Client', () => {
    it('getWorkflowObservabilitySummary endpoint doğru', () => {
      expect(API_SOURCE).toContain('/admin/workflow-observability/summary')
    })

    it('getWorkflowObservabilityDiagnostics endpoint doğru', () => {
      expect(API_SOURCE).toContain('/admin/workflow-observability/diagnostics')
    })

    it('observability functions export ediliyor', () => {
      expect(API_SOURCE).toContain('export function getWorkflowObservabilitySummary')
      expect(API_SOURCE).toContain('export function getWorkflowObservabilityDiagnostics')
    })
  })

  // ── L. Types ─────────────────────────────────────────────

  describe('L. Types', () => {
    it('SafeWorkflowDiagnosticType type tanımlı', () => {
      expect(TYPES_SOURCE).toContain('SafeWorkflowDiagnosticType')
    })

    it('SafeOperationalControlHint type tanımlı', () => {
      expect(TYPES_SOURCE).toContain('SafeOperationalControlHint')
    })

    it('SafeWorkflowDiagnosticItem interface tanımlı', () => {
      expect(TYPES_SOURCE).toContain('SafeWorkflowDiagnosticItem')
    })

    it('SafeWorkflowObservabilitySummary interface tanımlı', () => {
      expect(TYPES_SOURCE).toContain('SafeWorkflowObservabilitySummary')
    })

    it('types forbidden fields yok', () => {
      // Check specifically in the observability section
      const obsSection = TYPES_SOURCE.substring(TYPES_SOURCE.indexOf('Workflow Observability'))
      expect(obsSection).not.toContain('publicUrl')
      expect(obsSection).not.toContain('signedUrl')
      expect(obsSection).not.toContain('cdnUrl')
      expect(obsSection).not.toContain('providerUrl')
      expect(obsSection).not.toContain('storageKey')
    })
  })

  // ── M. ReviewWorklist Integration ────────────────────────

  describe('M. ReviewWorklist Integration', () => {
    it('WorkflowObservabilityPanel import mevcut', () => {
      expect(WORKLIST_SOURCE).toContain("import WorkflowObservabilityPanel from './WorkflowObservabilityPanel'")
    })

    it('WorkflowObservabilityPanel render ediliyor', () => {
      expect(WORKLIST_SOURCE).toContain('<WorkflowObservabilityPanel')
    })

    it('FP-017 Task 04 comment mevcut', () => {
      expect(WORKLIST_SOURCE).toContain('FP-017 Task 04')
    })

    it('existing ReviewConsole korunuyor', () => {
      expect(WORKLIST_SOURCE).toContain('<ReviewConsole')
    })

    it('existing ReviewPreviewPanel korunuyor', () => {
      expect(WORKLIST_SOURCE).toContain('<ReviewPreviewPanel')
    })

    it('existing InternalApprovalReadinessPanel korunuyor', () => {
      expect(WORKLIST_SOURCE).toContain('<InternalApprovalReadinessPanel')
    })

    it('existing PipelineOverviewPanel korunuyor', () => {
      expect(WORKLIST_SOURCE).toContain('<PipelineOverviewPanel')
    })

    it('existing ApprovedInternalInventoryPanel korunuyor', () => {
      expect(WORKLIST_SOURCE).toContain('<ApprovedInternalInventoryPanel')
    })

    it('existing InternalRevisionRequestPanel korunuyor', () => {
      expect(WORKLIST_SOURCE).toContain('<InternalRevisionRequestPanel')
    })
  })

  // ── N. Image Asset Safety ────────────────────────────────

  describe('N. Image Asset Safety', () => {
    it('no img src', () => {
      expect(PANEL_SOURCE).not.toMatch(/<img\s/)
    })

    it('no image URL/base64', () => {
      expect(PANEL_SOURCE).not.toContain('data:image/')
    })

    it('no storage/provider in render', () => {
      expect(PANEL_SOURCE).not.toContain('storageKey')
      expect(PANEL_SOURCE).not.toContain('providerUrl')
    })
  })
})
