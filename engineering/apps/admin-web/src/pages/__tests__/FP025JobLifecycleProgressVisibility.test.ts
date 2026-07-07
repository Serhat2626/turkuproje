import { describe, it, expect } from 'vitest'
import * as fs from 'fs'
import * as path from 'path'

/**
 * FP-025 Task 04: Admin UI Internal Render Job Lifecycle / Progress Visibility.
 */

const typesPath = path.resolve(__dirname, '../../types/index.ts')
const types = fs.readFileSync(typesPath, 'utf-8')

const pagePath = path.resolve(__dirname, '../TurkuVideoWorkflowPage.tsx')
const page = fs.readFileSync(pagePath, 'utf-8')

const apiPath = path.resolve(__dirname, '../../api/turkuVideoApi.ts')
const api = fs.readFileSync(apiPath, 'utf-8')

describe('FP-025 Task 04 — Admin UI Internal Render Job Lifecycle / Progress', () => {

  // ── A. Type Inventory ──

  describe('A. FP-025 Types', () => {
    it('TurkuInternalRenderJobStatus type tanımlı', () => {
      expect(types).toContain('TurkuInternalRenderJobStatus')
    })

    it('TurkuInternalRenderJobAction type tanımlı', () => {
      expect(types).toContain('TurkuInternalRenderJobAction')
    })

    it('TurkuInternalRenderJobReasonCode type tanımlı', () => {
      expect(types).toContain('TurkuInternalRenderJobReasonCode')
    })

    it('TurkuInternalRenderJobProgress interface tanımlı', () => {
      expect(types).toContain('TurkuInternalRenderJobProgress')
    })

    it('TurkuInternalRenderJobSummary interface tanımlı', () => {
      expect(types).toContain('TurkuInternalRenderJobSummary')
    })

    it('TurkuInternalRenderReadinessStatus type tanımlı', () => {
      expect(types).toContain('TurkuInternalRenderReadinessStatus')
    })

    it('TurkuInternalRenderReadinessIssue type tanımlı', () => {
      expect(types).toContain('TurkuInternalRenderReadinessIssue')
    })

    it('TurkuInternalRenderReadinessSnapshot interface tanımlı', () => {
      expect(types).toContain('TurkuInternalRenderReadinessSnapshot')
    })

    it('TurkuInternalRenderSafetyChainSnapshot interface tanımlı', () => {
      expect(types).toContain('TurkuInternalRenderSafetyChainSnapshot')
    })

    it('TurkuInternalRenderJobActionResult interface tanımlı', () => {
      expect(types).toContain('TurkuInternalRenderJobActionResult')
    })

    it('8 job statuses exist', () => {
      const statuses = ['DRAFT', 'READY_FOR_INTERNAL_RENDER', 'QUEUED_INTERNAL',
        'RUNNING_INTERNAL', 'SUCCEEDED_INTERNAL', 'FAILED_INTERNAL',
        'CANCELLED_INTERNAL', 'EXPIRED_INTERNAL']
      statuses.forEach(s => expect(types).toContain(`'${s}'`))
    })

    it('7 job actions exist', () => {
      const actions = ['VALIDATE_FOR_INTERNAL_RENDER', 'ENQUEUE_INTERNAL_RENDER',
        'START_INTERNAL_RENDER', 'COMPLETE_INTERNAL_RENDER',
        'FAIL_INTERNAL_RENDER', 'CANCEL_INTERNAL_RENDER', 'EXPIRE_INTERNAL_RENDER']
      actions.forEach(a => expect(types).toContain(`'${a}'`))
    })

    it('14 reason codes exist', () => {
      const codes = ['TRANSITION_ALLOWED', 'UNKNOWN_CURRENT_STATE', 'UNKNOWN_ACTION',
        'TRANSITION_NOT_ALLOWED', 'TERMINAL_STATE', 'PUBLIC_DELIVERY_FORBIDDEN',
        'GENERIC_STATUS_UPDATE_FORBIDDEN', 'JOB_REF_INVALID', 'RENDER_EXECUTION_DISABLED',
        'UNSAFE_INPUT_REJECTED', 'ARTIFACT_REF_INVALID', 'PROGRESS_OUT_OF_BOUNDS',
        'PROVIDER_PAYLOAD_FORBIDDEN', 'FILESYSTEM_EXPOSURE_FORBIDDEN']
      codes.forEach(c => expect(types).toContain(`'${c}'`))
    })
  })

  // ── B. Lifecycle Panel ──

  describe('B. Lifecycle Panel', () => {
    it('lifecycle panel var', () => {
      expect(page).toContain('fp025-lifecycle-panel')
    })

    it('job ref gösteriliyor', () => {
      expect(page).toContain('fp025-job-ref')
    })

    it('job status gösteriliyor', () => {
      expect(page).toContain('fp025-job-status')
    })

    it('human review gerekli label var', () => {
      expect(page).toContain('fp025-human-review')
    })

    it('auto approval kapalı label var', () => {
      expect(page).toContain('fp025-auto-approval')
    })

    it('public delivery kullanılamaz label var', () => {
      expect(page).toContain('fp025-public-delivery')
    })

    it('terminal indicator var', () => {
      expect(page).toContain('fp025-terminal-indicator')
    })
  })

  // ── C. Readiness Panel ──

  describe('C. Readiness Panel', () => {
    it('readiness panel var', () => {
      expect(page).toContain('fp025-readiness-panel')
    })

    it('readiness status gösteriliyor', () => {
      expect(page).toContain('fp025-readiness-status')
    })

    it('readiness issues gösteriliyor', () => {
      expect(page).toContain('fp025-readiness-issues')
    })

    it('readiness flags var', () => {
      expect(page).toContain('fp025-readiness-flags')
    })

    it('public delivery forbidden flag var', () => {
      expect(page).toContain('Yayın/Dışa aktarım yok')
    })

    it('auto approval forbidden flag var', () => {
      expect(page).toContain('Otomatik onay kapalı')
    })
  })

  // ── D. Progress Panel ──

  describe('D. Progress Panel', () => {
    it('progress panel var', () => {
      expect(page).toContain('fp025-progress-panel')
    })

    it('progress percent gösteriliyor', () => {
      expect(page).toContain('fp025-progress-percent')
    })

    it('stage label gösteriliyor', () => {
      expect(page).toContain('fp025-stage-label')
    })

    it('progress bar var', () => {
      expect(page).toContain('fp025-progress-bar')
    })

    it('güvenli ilerleme açıklaması var', () => {
      expect(page).toContain('log/stdout/stderr/komut/yol gösterilmez')
    })
  })

  // ── E. Safety Chain Panel ──

  describe('E. FP-024 Safety Chain Panel', () => {
    it('safety chain panel var', () => {
      expect(page).toContain('fp025-safety-chain-panel')
    })

    it('config status gösteriliyor', () => {
      expect(page).toContain('fp025-chain-config')
    })

    it('workspace status gösteriliyor', () => {
      expect(page).toContain('fp025-chain-workspace')
    })

    it('execution boundary gösteriliyor', () => {
      expect(page).toContain('fp025-chain-execution')
    })

    it('verification status gösteriliyor', () => {
      expect(page).toContain('fp025-chain-verification')
    })

    it('human review gösteriliyor', () => {
      expect(page).toContain('fp025-chain-human-review')
    })

    it('delivery status gösteriliyor', () => {
      expect(page).toContain('fp025-chain-delivery')
    })
  })

  // ── F. Forbidden Exposure Regression ──

  describe('F. Forbidden Exposure — Types', () => {
    const forbidden = [
      'filePath:', 'absolutePath:', 'storageKey:', 'publicUrl:', 'signedUrl:',
      'cdnUrl:', 'exportUrl:', 'publishUrl:', 'downloadUrl:', 'renderedVideoUrl:',
      'commandLine:', 'shellCommand:', 'executablePath:', 'workspacePath:',
      'inputPath:', 'outputPath:', 'rawRendererPayload:', 'rawProviderPayload:'
    ]

    const fp025Types = types.substring(types.indexOf('FP-025'))

    forbidden.forEach(f => {
      it(`FP-025 types '${f}' içermiyor`, () => {
        expect(fp025Types).not.toContain(f)
      })
    })
  })

  describe('F2. Forbidden Exposure — Page', () => {
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

    it('no publish button', () => {
      expect(page).not.toContain('btn-publish')
      expect(page).not.toContain('publishVideo')
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
  })

  // ── G. Safety Labels ──

  describe('G. Turkish Safety Labels', () => {
    it('dahili render iş akışı label', () => {
      expect(page).toContain('Dahili render iş akışı')
    })

    it('sadece dahili durum görünürlüğü label', () => {
      expect(page).toContain('Sadece dahili durum görünürlüğü')
    })

    it('insan onayı gerekli label', () => {
      expect(page).toContain('İnsan onayı gerekli')
    })

    it('otomatik onay kapalı label', () => {
      expect(page).toContain('Otomatik onay kapalı')
    })

    it('indirilebilir video yok label', () => {
      expect(page).toContain('İndirilebilir video yok')
    })

    it('video oynatıcı yok label', () => {
      expect(page).toContain('Video oynatıcı yok')
    })

    it('public/CDN URL yok label', () => {
      expect(page).toContain('Public/CDN URL yok')
    })

    it('dosya yolu gösterilmez label', () => {
      expect(page).toContain('Dosya yolu gösterilmez')
    })

    it('komut satırı gösterilmez label', () => {
      expect(page).toContain('Komut satırı gösterilmez')
    })

    it('güvenli ilerleme özeti label', () => {
      expect(page).toContain('Güvenli ilerleme özeti')
    })
  })

  // ── H. API Client ──

  describe('H. API Client', () => {
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

    it('no generic setStatus method', () => {
      expect(api).not.toContain('setStatus')
      expect(api).not.toContain('updateStatus')
      expect(api).not.toContain('patchStatus')
    })
  })

  // ── I. Backward Compatibility ──

  describe('I. Backward Compatibility', () => {
    it('FP-022 render types still present', () => {
      expect(types).toContain('TurkuRenderJobStatus')
      expect(types).toContain('TurkuRenderJobResponse')
    })

    it('FP-023 types still present', () => {
      expect(types).toContain('TurkuRenderAdapterMode')
      expect(types).toContain('TurkuRenderAdapterSafetyEnvelope')
    })

    it('FP-024 types still present', () => {
      expect(types).toContain('TurkuRenderConfigMode')
      expect(types).toContain('TurkuFfmpegExecutionMode')
      expect(types).toContain('TurkuRenderArtifactVerificationStatus')
    })

    it('FP-019 pipeline types still present', () => {
      expect(types).toContain('TurkuVideoWorkflowStage')
    })
  })
})
