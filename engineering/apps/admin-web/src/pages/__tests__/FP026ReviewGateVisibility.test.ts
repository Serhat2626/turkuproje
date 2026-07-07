import { describe, it, expect } from 'vitest'
import * as fs from 'fs'
import * as path from 'path'

/**
 * FP-026 Task 04: Admin UI Human Review Gate Visibility / Actions.
 */

const typesPath = path.resolve(__dirname, '../../types/index.ts')
const types = fs.readFileSync(typesPath, 'utf-8')

const pagePath = path.resolve(__dirname, '../TurkuVideoWorkflowPage.tsx')
const page = fs.readFileSync(pagePath, 'utf-8')

const apiPath = path.resolve(__dirname, '../../api/turkuVideoApi.ts')
const api = fs.readFileSync(apiPath, 'utf-8')

describe('FP-026 Task 04 — Admin UI Human Review Gate Visibility', () => {

  // ── A. FP-026 Types ──

  describe('A. FP-026 Types', () => {
    it('TurkuInternalRenderReviewState type tanımlı', () => {
      expect(types).toContain('TurkuInternalRenderReviewState')
    })

    it('TurkuInternalRenderReviewAction type tanımlı', () => {
      expect(types).toContain('TurkuInternalRenderReviewAction')
    })

    it('TurkuInternalRenderReviewReasonCode type tanımlı', () => {
      expect(types).toContain('TurkuInternalRenderReviewReasonCode')
    })

    it('TurkuInternalRenderReviewSummary interface tanımlı', () => {
      expect(types).toContain('TurkuInternalRenderReviewSummary')
    })

    it('TurkuInternalRenderReviewReadinessSnapshot interface tanımlı', () => {
      expect(types).toContain('TurkuInternalRenderReviewReadinessSnapshot')
    })

    it('TurkuInternalRenderReviewReadinessStatus type tanımlı', () => {
      expect(types).toContain('TurkuInternalRenderReviewReadinessStatus')
    })

    it('TurkuInternalRenderReviewReadinessIssue type tanımlı', () => {
      expect(types).toContain('TurkuInternalRenderReviewReadinessIssue')
    })

    it('TurkuInternalRenderReviewTransitionResult interface tanımlı', () => {
      expect(types).toContain('TurkuInternalRenderReviewTransitionResult')
    })

    it('TurkuInternalRenderReviewActionResult interface tanımlı', () => {
      expect(types).toContain('TurkuInternalRenderReviewActionResult')
    })

    it('8 review states exist', () => {
      const states = ['REVIEW_NOT_REQUIRED', 'REVIEW_REQUIRED', 'READY_FOR_REVIEW',
        'IN_REVIEW', 'APPROVED_FOR_INTERNAL_NEXT_STEP', 'REJECTED_BY_HUMAN_REVIEW',
        'BLOCKED_BY_SAFETY', 'EXPIRED_REVIEW']
      states.forEach(s => expect(types).toContain(`'${s}'`))
    })

    it('7 review actions exist', () => {
      const actions = ['REQUIRE_HUMAN_REVIEW', 'MARK_READY_FOR_REVIEW',
        'START_HUMAN_REVIEW', 'APPROVE_FOR_INTERNAL_NEXT_STEP',
        'REJECT_HUMAN_REVIEW', 'BLOCK_FOR_SAFETY', 'EXPIRE_HUMAN_REVIEW']
      actions.forEach(a => expect(types).toContain(`'${a}'`))
    })

    it('18 review reason codes exist', () => {
      const codes = ['REVIEW_NOT_REQUIRED', 'REVIEW_REQUIRED', 'READY_FOR_REVIEW',
        'REVIEW_STARTED', 'APPROVED_INTERNAL_ONLY', 'REJECTED_BY_REVIEWER',
        'BLOCKED_BY_SAFETY', 'REVIEW_EXPIRED', 'JOB_NOT_READY', 'READINESS_BLOCKED',
        'ARTIFACT_VERIFICATION_FAILED', 'PUBLIC_DELIVERY_FORBIDDEN',
        'AUTO_APPROVAL_FORBIDDEN', 'GENERIC_STATUS_UPDATE_FORBIDDEN',
        'UNSAFE_REVIEW_INPUT_REJECTED', 'TRANSITION_NOT_ALLOWED',
        'TERMINAL_STATE', 'REVIEW_REF_INVALID']
      codes.forEach(c => expect(types).toContain(`'${c}'`))
    })
  })

  // ── B. Review Gate Panel ──

  describe('B. Review Gate Panel', () => {
    it('review gate section var', () => {
      expect(page).toContain('fp026-review-gate-section')
    })

    it('review gate panel var', () => {
      expect(page).toContain('fp026-review-gate-panel')
    })

    it('review ref gösteriliyor', () => {
      expect(page).toContain('fp026-review-ref')
    })

    it('review state gösteriliyor', () => {
      expect(page).toContain('fp026-review-state')
    })

    it('internal-only badge var', () => {
      expect(page).toContain('fp026-internal-only')
    })

    it('human review gerekli', () => {
      expect(page).toContain('fp026-human-review')
    })

    it('auto approval kapalı', () => {
      expect(page).toContain('fp026-auto-approval')
    })

    it('public delivery kullanılamaz', () => {
      expect(page).toContain('fp026-public-delivery')
    })
  })

  // ── C. Readiness Panel ──

  describe('C. Review Readiness Panel', () => {
    it('readiness panel var', () => {
      expect(page).toContain('fp026-review-readiness-panel')
    })

    it('readiness status gösteriliyor', () => {
      expect(page).toContain('fp026-review-readiness-status')
    })

    it('readiness issues gösteriliyor', () => {
      expect(page).toContain('fp026-review-readiness-issues')
    })

    it('readiness flags var', () => {
      expect(page).toContain('fp026-review-readiness-flags')
    })
  })

  // ── D. Action Panel ──

  describe('D. Review Actions Panel', () => {
    it('actions panel var', () => {
      expect(page).toContain('fp026-review-actions-panel')
    })

    it('approve label is internal-only', () => {
      expect(page).toContain('Dahili Sonraki Adım İçin Onayla')
    })

    it('reject label var', () => {
      expect(page).toContain('fp026-reject-label')
    })

    it('block label var', () => {
      expect(page).toContain('fp026-block-label')
    })

    it('no actual publish button', () => {
      expect(page).not.toContain('btn-publish')
      expect(page).not.toContain('publishVideo')
    })
  })

  // ── E. Decision Safety Panel ──

  describe('E. Decision Safety Panel', () => {
    it('decision safety panel var', () => {
      expect(page).toContain('fp026-decision-safety-panel')
    })

    it('approval meaning is internal-only', () => {
      expect(page).toContain('fp026-approval-meaning')
    })

    it('no publish', () => {
      expect(page).toContain('fp026-no-publish')
    })

    it('no download', () => {
      expect(page).toContain('fp026-no-download')
    })

    it('no player', () => {
      expect(page).toContain('fp026-no-player')
    })
  })

  // ── F. Forbidden Exposure — Types ──

  describe('F. Forbidden Exposure — Types', () => {
    const forbidden = [
      'filePath:', 'absolutePath:', 'storageKey:', 'publicUrl:', 'signedUrl:',
      'cdnUrl:', 'exportUrl:', 'publishUrl:', 'downloadUrl:', 'renderedVideoUrl:',
      'commandLine:', 'shellCommand:', 'executablePath:', 'workspacePath:',
      'inputPath:', 'outputPath:', 'rawRendererPayload:', 'rawProviderPayload:'
    ]

    const fp026Types = types.substring(types.indexOf('FP-026'))

    forbidden.forEach(f => {
      it(`FP-026 types '${f}' içermiyor`, () => {
        expect(fp026Types).not.toContain(f)
      })
    })
  })

  // ── G. Forbidden Controls ──

  describe('G. Forbidden Controls', () => {
    it('no video player', () => {
      expect(page).not.toContain('<video')
      expect(page).not.toContain('videoPlayer')
    })

    it('no download button', () => {
      expect(page).not.toContain('btn-download')
      expect(page).not.toContain('downloadVideo')
    })

    it('no export button', () => {
      expect(page).not.toContain('btn-export')
      expect(page).not.toContain('exportVideo')
    })

    it('no dangerouslySetInnerHTML', () => {
      expect(page).not.toContain('dangerouslySetInnerHTML')
    })

    it('no innerHTML', () => {
      expect(page).not.toContain('.innerHTML')
    })

    it('no Authorization/Bearer', () => {
      expect(page).not.toContain('Authorization')
      expect(page).not.toContain('Bearer ')
    })

    it('no publicUrl/signedUrl/cdnUrl', () => {
      expect(page).not.toContain('publicUrl')
      expect(page).not.toContain('signedUrl')
      expect(page).not.toContain('cdnUrl')
    })

    it('no base64/binary', () => {
      expect(page).not.toContain('base64,')
      expect(page).not.toContain('binaryData')
    })

    it('no generic status update form', () => {
      expect(page).not.toContain('setStatus')
      expect(page).not.toContain('updateStatus')
      expect(page).not.toContain('patchStatus')
    })
  })

  // ── H. Turkish Safety Labels ──

  describe('H. Turkish Safety Labels', () => {
    it('insan incelemesi gerekli', () => {
      expect(page).toContain('İnsan incelemesi gerekli')
    })

    it('otomatik onay kapalı', () => {
      expect(page).toContain('Otomatik onay kapalı')
    })

    it('sadece dahili sonraki adım', () => {
      expect(page).toContain('Sadece dahili sonraki adım')
    })

    it('yayın/dışa aktarım yok', () => {
      expect(page).toContain('Yayın/Dışa aktarım yok')
    })

    it('indirilebilir video yok', () => {
      expect(page).toContain('İndirilebilir video yok')
    })

    it('video oynatıcı yok', () => {
      expect(page).toContain('Video oynatıcı yok')
    })

    it('public/CDN URL yok', () => {
      expect(page).toContain('Public/CDN URL yok')
    })

    it('dosya yolu gösterilmez', () => {
      expect(page).toContain('Dosya yolu gösterilmez')
    })

    it('komut satırı gösterilmez', () => {
      expect(page).toContain('Komut satırı gösterilmez')
    })

    it('güvenli inceleme kapısı', () => {
      expect(page).toContain('Güvenli inceleme kapısı')
    })

    it('genel durum güncellemesi yok', () => {
      expect(page).toContain('Genel durum güncellemesi yok')
    })
  })

  // ── I. API Client ──

  describe('I. API Client', () => {
    it('no publish method', () => {
      expect(api).not.toContain('publishVideo')
      expect(api).not.toContain('publishProject')
    })

    it('no export method', () => {
      expect(api).not.toContain('exportVideo')
    })

    it('no download method', () => {
      expect(api).not.toContain('downloadVideo')
      expect(api).not.toContain('downloadFile')
    })

    it('no generic setStatus', () => {
      expect(api).not.toContain('setStatus')
      expect(api).not.toContain('updateStatus')
      expect(api).not.toContain('patchStatus')
    })

    it('no autoApprove', () => {
      expect(api).not.toContain('autoApprove')
    })
  })

  // ── J. Backward Compatibility ──

  describe('J. Backward Compatibility', () => {
    it('FP-022 render types still present', () => {
      expect(types).toContain('TurkuRenderJobStatus')
    })

    it('FP-024 types still present', () => {
      expect(types).toContain('TurkuRenderConfigMode')
      expect(types).toContain('TurkuFfmpegExecutionMode')
    })

    it('FP-025 lifecycle types still present', () => {
      expect(types).toContain('TurkuInternalRenderJobStatus')
      expect(types).toContain('TurkuInternalRenderReadinessStatus')
    })

    it('FP-019 pipeline types still present', () => {
      expect(types).toContain('TurkuVideoWorkflowStage')
    })
  })
})
