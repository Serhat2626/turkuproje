import { describe, it, expect } from 'vitest'
import { readFileSync } from 'fs'
import { resolve } from 'path'

/**
 * FP-016 Task 04: Pipeline Overview Panel safety & behavior tests.
 *
 * Source-inspection tests — no DOM rendering required.
 * Verifies safe rendering, forbidden patterns, label safety,
 * loading/error/empty states, and integration.
 */

const PANEL_SOURCE = readFileSync(
  resolve(__dirname, '../PipelineOverviewPanel.tsx'),
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

describe('FP-016 Task 04 — PipelineOverviewPanel', () => {

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
      expect(safeTextCalls).toBeGreaterThanOrEqual(5)
    })
  })

  // ── B. Internal-Only Labels ──────────────────────────────

  describe('B. Internal-Only Labels', () => {
    it('panel başlığı Dahili vurgusu yapıyor', () => {
      expect(PANEL_SOURCE).toContain('Dahili Pipeline')
    })

    it('disclaimer mevcut', () => {
      expect(PANEL_SOURCE).toContain('yayın/export değildir')
    })

    it('İç Kullanım label mevcut', () => {
      expect(PANEL_SOURCE).toContain('İç Kullanım')
    })

    it('Dahili Hazır label mevcut', () => {
      expect(PANEL_SOURCE).toContain('Dahili Hazır')
    })

    it('Kullanılamıyor label mevcut', () => {
      expect(PANEL_SOURCE).toContain('Kullanılamıyor')
    })

    it('Engelleyici nedenler label mevcut', () => {
      expect(PANEL_SOURCE).toContain('Engelleyici nedenler')
    })

    it('Dahili sonraki adımlar label mevcut', () => {
      expect(PANEL_SOURCE).toContain('Dahili sonraki adımlar')
    })
  })

  // ── C. Stage Labels ──────────────────────────────────────

  describe('C. Stage Labels', () => {
    const EXPECTED_STAGES = [
      'GENERATED', 'REVIEW_PENDING', 'IN_REVIEW',
      'CHANGES_REQUESTED', 'INTERNALLY_APPROVED', 'INTERNAL_READY',
      'INTERNAL_REVISION_REQUESTED', 'REJECTED', 'UNAVAILABLE',
    ]

    EXPECTED_STAGES.forEach(stage => {
      it(`${stage} aşaması STAGE_LABELS'da tanımlı`, () => {
        expect(PANEL_SOURCE).toContain(`${stage}:`)
      })
    })

    it('9 aşama tanımlı', () => {
      const stageCount = EXPECTED_STAGES.filter(s => PANEL_SOURCE.includes(`${s}:`)).length
      expect(stageCount).toBe(9)
    })

    it('yasaklı aşamalar yok', () => {
      const forbidden = [
        'READY_FOR_PUBLISH', 'PUBLISHABLE', 'PUBLISHED',
        'EXPORTABLE', 'EXPORTED', 'CDN_READY',
        'PUBLIC_DELIVERY', 'SIGNED_URL_READY',
      ]
      forbidden.forEach(f => {
        expect(PANEL_SOURCE).not.toContain(f)
      })
    })
  })

  // ── D. Target Type Labels ────────────────────────────────

  describe('D. Target Type Labels', () => {
    const EXPECTED_TYPES = [
      'CONTENT_PROJECT', 'SCENARIO', 'STORYBOARD', 'PACKAGE', 'IMAGE_ASSET',
    ]

    EXPECTED_TYPES.forEach(type => {
      it(`${type} tipi tanımlı`, () => {
        expect(PANEL_SOURCE).toContain(type)
      })
    })
  })

  // ── E. Loading/Error/Empty States ────────────────────────

  describe('E. Loading/Error/Empty States', () => {
    it('loading state render eder', () => {
      expect(PANEL_SOURCE).toContain('pipeline-loading')
    })

    it('error state render eder', () => {
      expect(PANEL_SOURCE).toContain('pipeline-error')
    })

    it('empty state render eder', () => {
      expect(PANEL_SOURCE).toContain('pipeline-empty')
    })

    it('error mesajında safeText kullanıyor', () => {
      // Error rendering should use safeText
      expect(PANEL_SOURCE).toContain('safeText(error)')
    })
  })

  // ── F. Summary Bar ───────────────────────────────────────

  describe('F. Summary Bar', () => {
    it('summary bar render eder', () => {
      expect(PANEL_SOURCE).toContain('pipeline-summary-bar')
    })

    it('total count gösterir', () => {
      expect(PANEL_SOURCE).toContain('pipeline-total-count')
    })

    it('stage count gösterir', () => {
      expect(PANEL_SOURCE).toContain('pipeline-stage-count-')
    })
  })

  // ── G. Item Card ─────────────────────────────────────────

  describe('G. Item Card', () => {
    it('item card render eder', () => {
      expect(PANEL_SOURCE).toContain('pipeline-item-card')
    })

    it('item stage badge render eder', () => {
      expect(PANEL_SOURCE).toContain('pipeline-item-stage')
    })

    it('item title render eder', () => {
      expect(PANEL_SOURCE).toContain('pipeline-item-title')
    })

    it('blocking reasons render eder', () => {
      expect(PANEL_SOURCE).toContain('pipeline-item-blocking')
    })

    it('next hints render eder', () => {
      expect(PANEL_SOURCE).toContain('pipeline-item-hints')
    })
  })

  // ── H. Filters ───────────────────────────────────────────

  describe('H. Filters', () => {
    it('targetType filtresi mevcut', () => {
      expect(PANEL_SOURCE).toContain('pipeline-filter-targetType')
    })

    it('stage filtresi mevcut', () => {
      expect(PANEL_SOURCE).toContain('pipeline-filter-stage')
    })

    it('getPipelineOverview API çağrısı yapıyor', () => {
      expect(PANEL_SOURCE).toContain('getPipelineOverview')
    })
  })

  // ── I. Forbidden Fields/Strings ──────────────────────────

  describe('I. Forbidden Fields/Strings in Panel', () => {
    const FORBIDDEN = [
      'publicUrl', 'signedUrl', 'cdnUrl', 'providerUrl',
      'storageKey', 'bucket', 'objectPath', 'rawPayload',
      'base64', 'bytes', 'secret', 'apiKey', 'token',
      'stackTrace', 'exceptionClass',
      'publishStatus', 'exportStatus', 'publishedAt', 'exportedAt',
    ]

    FORBIDDEN.forEach(f => {
      it(`'${f}' panel source'da yok`, () => {
        expect(PANEL_SOURCE).not.toContain(f)
      })
    })
  })

  // ── J. No Forbidden Controls ─────────────────────────────

  describe('J. No Forbidden Controls', () => {
    it('publish button/action yok', () => {
      // Check for publish action buttons/handlers, not disclaimer text
      expect(PANEL_SOURCE).not.toContain('onPublish')
      expect(PANEL_SOURCE).not.toContain('handlePublish')
      expect(PANEL_SOURCE).not.toContain('publishReview')
    })

    it('export action yok', () => {
      // Check for export action buttons/handlers, not JS export keyword or disclaimer
      expect(PANEL_SOURCE).not.toContain('onExport')
      expect(PANEL_SOURCE).not.toContain('handleExport')
      expect(PANEL_SOURCE).not.toContain('exportReview')
    })

    it('regenerate button yok', () => {
      expect(PANEL_SOURCE).not.toMatch(/regenerate/i)
    })

    it('auto-approve button yok', () => {
      expect(PANEL_SOURCE).not.toMatch(/auto.?approv/i)
    })

    it('img src yok', () => {
      expect(PANEL_SOURCE).not.toContain('<img')
    })
  })

  // ── K. API Client ────────────────────────────────────────

  describe('K. API Client', () => {
    it('getPipelineOverview endpoint doğru', () => {
      expect(API_SOURCE).toContain('/admin/pipeline/overview')
    })

    it('getPipelineDetail endpoint doğru', () => {
      expect(API_SOURCE).toContain('/admin/pipeline/')
    })

    it('pipeline API sadece GET kullanıyor (POST yok)', () => {
      // Pipeline section should not contain POST
      const pipelineSection = API_SOURCE.split('Pipeline Aggregation')[1] || ''
      expect(pipelineSection).not.toContain("method: 'POST'")
      expect(pipelineSection).not.toContain("method: 'PUT'")
      expect(pipelineSection).not.toContain("method: 'DELETE'")
    })
  })

  // ── L. Types ─────────────────────────────────────────────

  describe('L. Types', () => {
    it('SafePipelineStage type tanımlı', () => {
      expect(TYPES_SOURCE).toContain('SafePipelineStage')
    })

    it('SafePipelineAggregateItem interface tanımlı', () => {
      expect(TYPES_SOURCE).toContain('SafePipelineAggregateItem')
    })

    it('SafePipelineOverview interface tanımlı', () => {
      expect(TYPES_SOURCE).toContain('SafePipelineOverview')
    })

    it('forbidden fields SafePipelineAggregateItem\'da yok', () => {
      const typeSection = TYPES_SOURCE.split('SafePipelineAggregateItem')[1]?.split('}')[0] || ''
      const forbidden = [
        'publicUrl', 'signedUrl', 'cdnUrl', 'providerUrl',
        'storageKey', 'bucket', 'objectPath', 'rawPayload',
        'base64', 'bytes', 'secret', 'apiKey', 'token',
        'stackTrace', 'publishStatus', 'exportStatus',
      ]
      forbidden.forEach(f => {
        expect(typeSection).not.toContain(f)
      })
    })
  })

  // ── M. ReviewWorklist Integration ────────────────────────

  describe('M. ReviewWorklist Integration', () => {
    it('ReviewWorklist PipelineOverviewPanel import ediyor', () => {
      expect(WORKLIST_SOURCE).toContain("import PipelineOverviewPanel from './PipelineOverviewPanel'")
    })

    it('ReviewWorklist PipelineOverviewPanel render ediyor', () => {
      expect(WORKLIST_SOURCE).toContain('<PipelineOverviewPanel')
    })

    it('ReviewWorklist FP-016 açıklaması mevcut', () => {
      expect(WORKLIST_SOURCE).toContain('FP-016')
    })

    it('ReviewWorklist hala ReviewConsole render ediyor', () => {
      expect(WORKLIST_SOURCE).toContain('<ReviewConsole')
    })

    it('ReviewWorklist hala ReviewPreviewPanel render ediyor', () => {
      expect(WORKLIST_SOURCE).toContain('<ReviewPreviewPanel')
    })

    it('ReviewWorklist hala InternalApprovalReadinessPanel render ediyor', () => {
      expect(WORKLIST_SOURCE).toContain('<InternalApprovalReadinessPanel')
    })

    it('ReviewWorklist hala ApprovedInternalInventoryPanel render ediyor', () => {
      expect(WORKLIST_SOURCE).toContain('<ApprovedInternalInventoryPanel')
    })

    it('ReviewWorklist hala InternalRevisionRequestPanel render ediyor', () => {
      expect(WORKLIST_SOURCE).toContain('<InternalRevisionRequestPanel')
    })
  })

  // ── N. Image Asset Safety ────────────────────────────────

  describe('N. Image Asset Safety', () => {
    it('img src yok', () => {
      expect(PANEL_SOURCE).not.toMatch(/<img\s/)
    })

    it('image URL yok', () => {
      expect(PANEL_SOURCE).not.toContain('imageUrl')
      expect(PANEL_SOURCE).not.toContain('thumbnailUrl')
    })

    it('base64 image yok', () => {
      expect(PANEL_SOURCE).not.toContain('data:image')
    })
  })
})
