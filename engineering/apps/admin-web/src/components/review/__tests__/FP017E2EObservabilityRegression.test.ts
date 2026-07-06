import { describe, it, expect } from 'vitest'
import { readFileSync } from 'fs'
import { resolve } from 'path'

/**
 * FP-017 Task 06: E2E Observability Regression & Safety Hardening (frontend).
 *
 * Full-stack regression: panel renders, worklist integration,
 * all panels together, forbidden patterns, safe states.
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

describe('FP-017 Task 06 — E2E Observability Regression (Frontend)', () => {

  // ── A. Panel Renders Summary + Diagnostics ───────────────

  describe('A. Panel Renders Summary + Diagnostics', () => {
    it('summary section mevcut', () => {
      expect(PANEL_SOURCE).toContain('observability-summary')
    })

    it('diagnostics section mevcut', () => {
      expect(PANEL_SOURCE).toContain('observability-diagnostics')
    })

    it('review state counts render', () => {
      expect(PANEL_SOURCE).toContain('countsByReviewState')
    })

    it('pipeline stage counts render', () => {
      expect(PANEL_SOURCE).toContain('countsByPipelineStage')
    })

    it('readiness counts render', () => {
      expect(PANEL_SOURCE).toContain('countsByReadinessAvailability')
    })

    it('preview counts render', () => {
      expect(PANEL_SOURCE).toContain('previewAvailableCount')
      expect(PANEL_SOURCE).toContain('previewUnavailableCount')
    })

    it('revision count render', () => {
      expect(PANEL_SOURCE).toContain('revisionRequestedCount')
    })

    it('diagnostic cards render', () => {
      expect(PANEL_SOURCE).toContain('diagnostic-card')
    })
  })

  // ── B. Target Type Filter ────────────────────────────────

  describe('B. Target Type Filter', () => {
    it('filter select mevcut', () => {
      expect(PANEL_SOURCE).toContain('diagnostic-filter-target-type')
    })

    it('all 5 target types in options', () => {
      expect(PANEL_SOURCE).toContain('CONTENT_PROJECT')
      expect(PANEL_SOURCE).toContain('SCENARIO')
      expect(PANEL_SOURCE).toContain('STORYBOARD')
      expect(PANEL_SOURCE).toContain('PACKAGE')
      expect(PANEL_SOURCE).toContain('IMAGE_ASSET')
    })

    it('filter uses diagnosticFilter state', () => {
      expect(PANEL_SOURCE).toContain('diagnosticFilter')
    })

    it('API supports targetType filter', () => {
      expect(API_SOURCE).toContain("params?.targetType")
    })
  })

  // ── C. Refresh Only Re-fetches GET ────────────────────────

  describe('C. Refresh Only GET', () => {
    it('loadSummary calls getWorkflowObservabilitySummary', () => {
      expect(PANEL_SOURCE).toContain('getWorkflowObservabilitySummary')
    })

    it('no POST method in panel', () => {
      expect(PANEL_SOURCE).not.toMatch(/\.post\s*\(/)
    })

    it('no fetch with POST in panel', () => {
      expect(PANEL_SOURCE).not.toMatch(/method.*POST/)
    })
  })

  // ── D. Hints as Non-Executing Labels ──────────────────────

  describe('D. Hints as Non-Executing Labels', () => {
    it('hints rendered via label map', () => {
      expect(PANEL_SOURCE).toContain('CONTROL_HINT_LABELS')
    })

    it('no button element', () => {
      expect(PANEL_SOURCE).not.toContain('<button')
    })

    it('no anchor element', () => {
      expect(PANEL_SOURCE).not.toContain('<a ')
    })

    it('no onClick on hint items', () => {
      expect(PANEL_SOURCE).not.toMatch(/hint-label.*onClick/)
    })
  })

  // ── E. ReviewWorklist All Panels Together ─────────────────

  describe('E. ReviewWorklist All Panels Together', () => {
    it('WorkflowObservabilityPanel', () => {
      expect(WORKLIST_SOURCE).toContain('<WorkflowObservabilityPanel')
    })

    it('PipelineOverviewPanel', () => {
      expect(WORKLIST_SOURCE).toContain('<PipelineOverviewPanel')
    })

    it('ApprovedInternalInventoryPanel', () => {
      expect(WORKLIST_SOURCE).toContain('<ApprovedInternalInventoryPanel')
    })

    it('InternalRevisionRequestPanel', () => {
      expect(WORKLIST_SOURCE).toContain('<InternalRevisionRequestPanel')
    })

    it('InternalApprovalReadinessPanel', () => {
      expect(WORKLIST_SOURCE).toContain('<InternalApprovalReadinessPanel')
    })

    it('ReviewPreviewPanel', () => {
      expect(WORKLIST_SOURCE).toContain('<ReviewPreviewPanel')
    })

    it('ReviewConsole', () => {
      expect(WORKLIST_SOURCE).toContain('<ReviewConsole')
    })
  })

  // ── F. Loading/Error/Empty/Degraded States ────────────────

  describe('F. Loading/Error/Empty/Degraded States', () => {
    it('loading state', () => {
      expect(PANEL_SOURCE).toContain('observability-loading')
    })

    it('error state', () => {
      expect(PANEL_SOURCE).toContain('observability-error')
    })

    it('empty state', () => {
      expect(PANEL_SOURCE).toContain('observability-empty')
    })

    it('diagnostic empty state', () => {
      expect(PANEL_SOURCE).toContain('diagnostic-empty')
    })

    it('warnings section', () => {
      expect(PANEL_SOURCE).toContain('observability-warnings')
    })
  })

  // ── G. No Unsafe HTML ────────────────────────────────────

  describe('G. No Unsafe HTML', () => {
    it('panel: no dangerouslySetInnerHTML', () => {
      expect(PANEL_SOURCE).not.toContain('dangerouslySetInnerHTML')
    })

    it('panel: no innerHTML', () => {
      expect(PANEL_SOURCE).not.toContain('.innerHTML')
    })

    it('panel: no __html', () => {
      expect(PANEL_SOURCE).not.toContain('__html')
    })

    it('worklist: no dangerouslySetInnerHTML', () => {
      expect(WORKLIST_SOURCE).not.toContain('dangerouslySetInnerHTML')
    })
  })

  // ── H. No Unsafe Buttons/Links/Actions ────────────────────

  describe('H. No Unsafe Actions', () => {
    it('no publish handler', () => {
      expect(PANEL_SOURCE).not.toContain('handlePublish')
    })

    it('no export handler', () => {
      expect(PANEL_SOURCE).not.toContain('handleExport')
    })

    it('no approve handler', () => {
      expect(PANEL_SOURCE).not.toContain('handleApprove')
    })

    it('no regenerate handler', () => {
      expect(PANEL_SOURCE).not.toContain('handleRegenerate')
    })

    it('no generic status update', () => {
      expect(PANEL_SOURCE).not.toContain('updateStatus')
      expect(PANEL_SOURCE).not.toContain('genericStatusUpdate')
    })
  })

  // ── I. Forbidden Strings Regression ───────────────────────

  describe('I. Forbidden Strings Regression', () => {
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
      it(`panel: '${field}' yok`, () => {
        expect(PANEL_SOURCE).not.toContain(field)
      })
    })
  })

  // ── J. API Function Regression ────────────────────────────

  describe('J. API Function Regression', () => {
    it('total 21 functions', () => {
      expect(API_SOURCE).toContain('export function getWorkflowObservabilitySummary')
      expect(API_SOURCE).toContain('export function getWorkflowObservabilityDiagnostics')
    })

    it('no operational control execute function', () => {
      expect(API_SOURCE).not.toContain('executeHint')
      expect(API_SOURCE).not.toContain('executeControl')
      expect(API_SOURCE).not.toContain('runControl')
    })

    it('no publish/export API', () => {
      const lower = API_SOURCE.toLowerCase()
      expect(lower).not.toContain('publishcontent')
      expect(lower).not.toContain('exportcontent')
    })
  })
})
