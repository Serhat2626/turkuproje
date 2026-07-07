import { describe, it, expect } from 'vitest'
import * as fs from 'fs'
import * as path from 'path'

/**
 * FP-024 Task 06: Admin UI Render Config / Artifact Status Visibility tests.
 */

const typesPath = path.resolve(__dirname, '../../types/index.ts')
const types = fs.readFileSync(typesPath, 'utf-8')

const pagePath = path.resolve(__dirname, '../TurkuVideoWorkflowPage.tsx')
const page = fs.readFileSync(pagePath, 'utf-8')

const apiPath = path.resolve(__dirname, '../../api/turkuVideoApi.ts')
const api = fs.readFileSync(apiPath, 'utf-8')

describe('FP-024 Task 06 — Admin UI Render Config / Artifact Status Visibility', () => {

  // ── A. FP-024 Types ──

  describe('A. FP-024 Types', () => {
    it('TurkuRenderConfigMode type tanımlı', () => {
      expect(types).toContain('TurkuRenderConfigMode')
    })

    it('TurkuRenderConfigFailureCode type tanımlı', () => {
      expect(types).toContain('TurkuRenderConfigFailureCode')
    })

    it('TurkuRenderConfigValidationResult interface tanımlı', () => {
      expect(types).toContain('TurkuRenderConfigValidationResult')
    })

    it('TurkuRenderConfigValidationIssue interface tanımlı', () => {
      expect(types).toContain('TurkuRenderConfigValidationIssue')
    })

    it('TurkuRenderConfigSafetyEnvelope interface tanımlı', () => {
      expect(types).toContain('TurkuRenderConfigSafetyEnvelope')
    })

    it('TurkuRenderWorkspaceRef interface tanımlı', () => {
      expect(types).toContain('TurkuRenderWorkspaceRef')
    })

    it('TurkuRenderSafeFileRef interface tanımlı', () => {
      expect(types).toContain('TurkuRenderSafeFileRef')
    })

    it('TurkuRenderFileSafetyReport interface tanımlı', () => {
      expect(types).toContain('TurkuRenderFileSafetyReport')
    })

    it('TurkuFfmpegExecutionMode type tanımlı', () => {
      expect(types).toContain('TurkuFfmpegExecutionMode')
    })

    it('TurkuFfmpegExecutionFailureCode type tanımlı', () => {
      expect(types).toContain('TurkuFfmpegExecutionFailureCode')
    })

    it('TurkuFfmpegExecutionResult interface tanımlı', () => {
      expect(types).toContain('TurkuFfmpegExecutionResult')
    })

    it('TurkuFfmpegExecutionSafetyEnvelope interface tanımlı', () => {
      expect(types).toContain('TurkuFfmpegExecutionSafetyEnvelope')
    })

    it('TurkuRenderArtifactVerificationStatus type tanımlı', () => {
      expect(types).toContain('TurkuRenderArtifactVerificationStatus')
    })

    it('TurkuRenderArtifactVerificationIssue interface tanımlı', () => {
      expect(types).toContain('TurkuRenderArtifactVerificationIssue')
    })

    it('TurkuRenderArtifactVerificationReport interface tanımlı', () => {
      expect(types).toContain('TurkuRenderArtifactVerificationReport')
    })
  })

  // ── B. Render Config Panel ──

  describe('B. Render Config Panel', () => {
    it('render config panel var', () => {
      expect(page).toContain('fp024-render-config-panel')
    })

    it('config mode gösteriliyor', () => {
      expect(page).toContain('fp024-config-mode')
    })

    it('config status gösteriliyor', () => {
      expect(page).toContain('fp024-config-status')
    })

    it('config validation gösteriliyor', () => {
      expect(page).toContain('fp024-config-validation')
    })
  })

  // ── C. Workspace / File Safety Panel ──

  describe('C. Workspace / File Safety Panel', () => {
    it('workspace safety panel var', () => {
      expect(page).toContain('fp024-workspace-safety-panel')
    })

    it('opak workspace ref gösteriliyor', () => {
      expect(page).toContain('fp024-workspace-ref')
    })

    it('extension allowlist gösteriliyor', () => {
      expect(page).toContain('fp024-extension-allowlist')
    })

    it('max size gösteriliyor', () => {
      expect(page).toContain('fp024-max-size')
    })
  })

  // ── D. FFmpeg Execution Boundary Panel ──

  describe('D. FFmpeg Execution Boundary Panel', () => {
    it('execution boundary panel var', () => {
      expect(page).toContain('fp024-execution-boundary-panel')
    })

    it('execution mode gösteriliyor', () => {
      expect(page).toContain('fp024-execution-mode')
    })

    it('no process label gösteriliyor', () => {
      expect(page).toContain('fp024-no-process')
      expect(page).toContain('NO_PROCESS_EXECUTION')
    })

    it('no filesystem label gösteriliyor', () => {
      expect(page).toContain('fp024-no-filesystem')
    })

    it('no command line label gösteriliyor', () => {
      expect(page).toContain('fp024-no-commandline')
    })
  })

  // ── E. Artifact Verification Panel ──

  describe('E. Artifact Verification Panel', () => {
    it('artifact verification panel var', () => {
      expect(page).toContain('fp024-artifact-verification-panel')
    })

    it('verification status gösteriliyor', () => {
      expect(page).toContain('fp024-verification-status')
    })

    it('format status gösteriliyor', () => {
      expect(page).toContain('fp024-format-status')
    })

    it('bounds status gösteriliyor', () => {
      expect(page).toContain('fp024-bounds-status')
    })

    it('opaque artifact ref gösteriliyor', () => {
      expect(page).toContain('fp024-opaque-artifact-ref')
    })
  })

  // ── F. Safety Labels ──

  describe('F. Internal-only Safety Labels', () => {
    it('dahili render güvenliği label var', () => {
      expect(page).toContain('Dahili render güvenliği')
    })

    it('gerçek render kapalı label var', () => {
      expect(page).toContain('Gerçek render kapalı veya güvenli yapılandırma gerektirir')
    })

    it('dosya yolu gösterilmez label var', () => {
      expect(page).toContain('Dosya yolu gösterilmez')
    })

    it('indirilebilir video yok label var', () => {
      expect(page).toContain('İndirilebilir video yok')
    })

    it('yayın/dışa aktarım yok label var', () => {
      expect(page).toContain('Yayın/Dışa Aktarım Yok')
    })

    it('notPublishReady label var', () => {
      expect(page).toContain('notPublishReady')
    })

    it('notExportReady label var', () => {
      expect(page).toContain('notExportReady')
    })
  })

  // ── G. No Video Player / Download / Export / Publish ──

  describe('G. No Video Player / Download / Export / Publish', () => {
    it('video player yok', () => {
      expect(page).not.toContain('<video')
      expect(page).not.toContain('videoPlayer')
    })

    it('download button yok', () => {
      expect(page).not.toContain('btn-download')
      expect(page).not.toContain('downloadVideo')
    })

    it('export button yok', () => {
      expect(page).not.toContain('btn-export')
      expect(page).not.toContain('exportVideo')
    })

    it('publish button yok', () => {
      expect(page).not.toContain('btn-publish')
      expect(page).not.toContain('publishVideo')
    })
  })

  // ── H. Forbidden Fields — Types ──

  describe('H. Forbidden Fields — Types', () => {
    const forbidden = [
      'filePath:', 'absolutePath:', 'storageKey:', 'publicUrl:', 'signedUrl:',
      'cdnUrl:', 'exportUrl:', 'publishUrl:', 'downloadUrl:', 'renderedVideoUrl:',
      'commandLine:', 'shellCommand:', 'executablePath:', 'workspacePath:',
      'inputPath:', 'outputPath:', 'rawRendererPayload:', 'rawProviderPayload:'
    ]

    // Check only FP-024 section
    const fp024Types = types.substring(types.indexOf('FP-024'))

    forbidden.forEach(f => {
      it(`FP-024 types '${f}' içermiyor`, () => {
        expect(fp024Types).not.toContain(f)
      })
    })
  })

  // ── I. Forbidden Fields — Page ──

  describe('I. Forbidden Fields — Page', () => {
    it('no Authorization/Bearer/JWT/token in page', () => {
      expect(page).not.toContain('Authorization')
      expect(page).not.toContain('Bearer ')
    })

    it('no dangerouslySetInnerHTML in page', () => {
      expect(page).not.toContain('dangerouslySetInnerHTML')
    })

    it('no innerHTML in page', () => {
      expect(page).not.toContain('.innerHTML')
    })

    it('no public/signed/CDN URL in page', () => {
      expect(page).not.toContain('publicUrl')
      expect(page).not.toContain('signedUrl')
      expect(page).not.toContain('cdnUrl')
    })

    it('no base64/binary in page', () => {
      expect(page).not.toContain('base64,')
      expect(page).not.toContain('binaryData')
    })
  })

  // ── J. API Client ──

  describe('J. API Client', () => {
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

    it('no Authorization header', () => {
      expect(api).not.toContain('Authorization')
      expect(api).not.toContain('Bearer ')
    })
  })

  // ── K. Backward Compatibility ──

  describe('K. Backward Compatibility', () => {
    it('FP-022 render types still present', () => {
      expect(types).toContain('TurkuRenderJobStatus')
      expect(types).toContain('TurkuRenderJobResponse')
      expect(types).toContain('TurkuRenderExecutionResponse')
    })

    it('FP-023 types still present', () => {
      expect(types).toContain('TurkuRenderAdapterMode')
      expect(types).toContain('TurkuRenderAdapterSafetyEnvelope')
      expect(types).toContain('TurkuInternalRenderArtifact')
    })

    it('FP-019 pipeline types still present', () => {
      expect(types).toContain('TurkuVideoWorkflowStage')
    })
  })
})
