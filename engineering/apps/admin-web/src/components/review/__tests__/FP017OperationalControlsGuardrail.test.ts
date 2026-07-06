import { describe, it, expect } from 'vitest'
import { readFileSync } from 'fs'
import { resolve } from 'path'

/**
 * FP-017 Task 05: Operational Controls Guardrail Hardening (frontend).
 *
 * Proves all control hints are non-mutating labels.
 * No hint executes workflow actions. No executable buttons.
 */

const PANEL_SOURCE = readFileSync(
  resolve(__dirname, '../WorkflowObservabilityPanel.tsx'),
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

const WORKLIST_SOURCE = readFileSync(
  resolve(__dirname, '../ReviewWorklist.tsx'),
  'utf-8'
)

describe('FP-017 Task 05 — Operational Controls Guardrail (Frontend)', () => {

  // ── A. Hints as Labels Only ──────────────────────────────

  describe('A. Hints Rendered as Labels', () => {
    it('hint label map kullanılıyor', () => {
      expect(PANEL_SOURCE).toContain('CONTROL_HINT_LABELS')
    })

    it('hint label text olarak render ediliyor', () => {
      // Hints are rendered via CONTROL_HINT_LABELS[h] or safeText(h)
      expect(PANEL_SOURCE).toContain('CONTROL_HINT_LABELS[h]')
    })

    it('hint onClick handler yok', () => {
      // The hints section should not have onClick on individual hints
      expect(PANEL_SOURCE).not.toMatch(/diagnostic-card__hint-label.*onClick/)
    })

    it('hint <button> yok', () => {
      expect(PANEL_SOURCE).not.toContain('<button')
    })

    it('hint <a href> yok', () => {
      expect(PANEL_SOURCE).not.toContain('<a href')
      expect(PANEL_SOURCE).not.toContain('<a ')
    })
  })

  // ── B. No Executable Controls ────────────────────────────

  describe('B. No Executable Controls', () => {
    it('no handlePublish', () => {
      expect(PANEL_SOURCE).not.toContain('handlePublish')
    })

    it('no handleExport', () => {
      expect(PANEL_SOURCE).not.toContain('handleExport')
    })

    it('no handleApprove', () => {
      expect(PANEL_SOURCE).not.toContain('handleApprove')
    })

    it('no handleRegenerate', () => {
      expect(PANEL_SOURCE).not.toContain('handleRegenerate')
    })

    it('no handleAutoFix', () => {
      expect(PANEL_SOURCE).not.toContain('handleAutoFix')
    })

    it('no executeHint / runControl', () => {
      expect(PANEL_SOURCE).not.toContain('executeHint')
      expect(PANEL_SOURCE).not.toContain('runControl')
      expect(PANEL_SOURCE).not.toContain('executeControl')
    })
  })

  // ── C. No Operational Control API Methods ─────────────────

  describe('C. No Operational Control API Methods', () => {
    it('no executeHint API function', () => {
      expect(API_SOURCE).not.toContain('executeHint')
      expect(API_SOURCE).not.toContain('runControl')
      expect(API_SOURCE).not.toContain('executeControl')
    })

    it('no POST to workflow-observability', () => {
      // API should not contain a POST call to workflow-observability
      expect(API_SOURCE).not.toMatch(/post.*workflow-observability/i)
      expect(API_SOURCE).not.toMatch(/method.*POST.*workflow-observability/i)
    })

    it('observability API functions are GET-only', () => {
      // Both observability functions use request<T>(url) which is GET
      expect(API_SOURCE).toContain("'/admin/workflow-observability/summary'")
      expect(API_SOURCE).toContain('/admin/workflow-observability/diagnostics')
    })
  })

  // ── D. Refresh Only Re-fetches GET ────────────────────────

  describe('D. Refresh Behavior', () => {
    it('loadSummary calls getWorkflowObservabilitySummary', () => {
      expect(PANEL_SOURCE).toContain('getWorkflowObservabilitySummary')
    })

    it('no POST call in panel', () => {
      expect(PANEL_SOURCE).not.toMatch(/\.post\(/)
      expect(PANEL_SOURCE).not.toMatch(/method.*POST/)
    })
  })

  // ── E. No Unsafe HTML ────────────────────────────────────

  describe('E. No Unsafe HTML', () => {
    it('no dangerouslySetInnerHTML', () => {
      expect(PANEL_SOURCE).not.toContain('dangerouslySetInnerHTML')
    })

    it('no innerHTML', () => {
      expect(PANEL_SOURCE).not.toContain('.innerHTML')
    })

    it('no __html', () => {
      expect(PANEL_SOURCE).not.toContain('__html')
    })
  })

  // ── F. Type Safety ───────────────────────────────────────

  describe('F. Type Safety', () => {
    it('SafeOperationalControlHint 5 values', () => {
      const hintValues = [
        'REFRESH_OVERVIEW', 'RECHECK_READINESS', 'VIEW_SAFE_PREVIEW',
        'REVIEW_PIPELINE_DETAIL', 'REQUEST_MANUAL_INVESTIGATION',
      ]
      hintValues.forEach(v => {
        expect(TYPES_SOURCE).toContain(`'${v}'`)
      })
    })

    it('no forbidden hint types', () => {
      const obsSection = TYPES_SOURCE.substring(TYPES_SOURCE.indexOf('SafeOperationalControlHint'))
      const endIdx = obsSection.indexOf('export interface')
      const hintSection = obsSection.substring(0, endIdx > 0 ? endIdx : 500)
      const forbidden = [
        'PUBLISH', 'EXPORT', 'APPROVE', 'AUTO_FIX', 'REGENERATE',
        'PROVIDER_RETRY', 'STORAGE_RETRY', 'CREATE_PUBLIC_URL',
        'CREATE_SIGNED_URL', 'CDN_DELIVERY',
      ]
      forbidden.forEach(f => {
        expect(hintSection).not.toContain(`'${f}'`)
      })
    })
  })

  // ── G. Forbidden Strings ─────────────────────────────────

  describe('G. Forbidden Strings in Panel', () => {
    const FORBIDDEN = [
      'publicUrl', 'signedUrl', 'cdnUrl', 'providerUrl',
      'storageKey', 'bucket', 'objectPath', 'rawPayload',
      'bytes', 'apiKey',
      'stackTrace', 'exceptionClass',
      'publishStatus', 'exportStatus', 'publishedAt', 'exportedAt',
      'PROVIDER_ERROR', 'STORAGE_ERROR',
      'PROVIDER_RETRY', 'STORAGE_RETRY',
      'CREATE_PUBLIC_URL', 'CREATE_SIGNED_URL', 'CDN_DELIVERY',
    ]

    FORBIDDEN.forEach(field => {
      it(`'${field}' yok`, () => {
        expect(PANEL_SOURCE).not.toContain(field)
      })
    })
  })

  // ── H. Integration Safety ────────────────────────────────

  describe('H. Integration Safety', () => {
    it('WorkflowObservabilityPanel worklist icerisinde mevcut', () => {
      expect(WORKLIST_SOURCE).toContain('<WorkflowObservabilityPanel')
    })

    it('worklist dangerouslySetInnerHTML yok', () => {
      expect(WORKLIST_SOURCE).not.toContain('dangerouslySetInnerHTML')
    })

    it('existing panels korunuyor', () => {
      expect(WORKLIST_SOURCE).toContain('<ReviewConsole')
      expect(WORKLIST_SOURCE).toContain('<ReviewPreviewPanel')
      expect(WORKLIST_SOURCE).toContain('<PipelineOverviewPanel')
    })
  })
})
