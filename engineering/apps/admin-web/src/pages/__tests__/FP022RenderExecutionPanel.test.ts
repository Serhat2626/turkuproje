import { describe, it, expect } from 'vitest'
import * as fs from 'fs'
import * as path from 'path'

/**
 * FP-022 Task 04: Render Job + Execution Safety Admin UI tests.
 *
 * Kontrol noktaları:
 * - Types dosyası FP-022 type'larını içerir
 * - Types dosyası yasak alan içermez
 * - API client FP-022 fonksiyonlarını içerir
 * - API client publish/export/download fonksiyonu içermez
 * - Page dosyası render execution section içerir
 * - Page dosyası video player, download/export/publish kontrolleri içermez
 * - Page dosyası unsafe HTML rendering kullanmaz
 * - Authorization/Bearer/JWT/token/session/principal yok
 */

const SRC_DIR = path.resolve(__dirname, '../../')

function readFile(relativePath: string): string {
  return fs.readFileSync(path.resolve(SRC_DIR, relativePath), 'utf-8')
}

// ── A. FP-022 Types ────────────────────────────────

describe('FP-022 Types', () => {
  const types = readFile('types/index.ts')

  it('TurkuRenderJobStatus type tanımlı', () => {
    expect(types).toContain('TurkuRenderJobStatus')
  })

  it('TurkuRenderFailureCode type tanımlı', () => {
    expect(types).toContain('TurkuRenderFailureCode')
  })

  it('TurkuRenderAttempt interface tanımlı', () => {
    expect(types).toContain('TurkuRenderAttempt')
  })

  it('TurkuRenderArtifact interface tanımlı', () => {
    expect(types).toContain('TurkuRenderArtifact')
  })

  it('TurkuRenderJobResponse interface tanımlı', () => {
    expect(types).toContain('TurkuRenderJobResponse')
  })

  it('TurkuRenderExecutionResponse interface tanımlı', () => {
    expect(types).toContain('TurkuRenderExecutionResponse')
  })

  it('TurkuRenderExecutionSafetyEnvelope interface tanımlı', () => {
    expect(types).toContain('TurkuRenderExecutionSafetyEnvelope')
  })

  it('TurkuRenderExecutionSafetyResponse interface tanımlı', () => {
    expect(types).toContain('TurkuRenderExecutionSafetyResponse')
  })

  it('humanReviewRequired alanı var', () => {
    expect(types).toContain('humanReviewRequired: boolean')
  })

  it('copyrightReviewRequired alanı var', () => {
    expect(types).toContain('copyrightReviewRequired: boolean')
  })

  it('autoApproved alanı var', () => {
    expect(types).toContain('autoApproved: boolean')
  })

  it('notPublishReady alanı var', () => {
    expect(types).toContain('notPublishReady: boolean')
  })

  it('notExportReady alanı var', () => {
    expect(types).toContain('notExportReady: boolean')
  })
})

// ── B. FP-022 Type Safety — Forbidden Fields ────────

describe('FP-022 Type Safety — Forbidden Fields', () => {
  const types = readFile('types/index.ts')

  const FORBIDDEN_FIELDS = [
    'rawPayload', 'rawRendererPayload', 'rawProviderResponse',
    'token', 'jwt', 'session', 'credential', 'password', 'secret',
    'apiKey', 'authorizationHeader', 'principalRaw',
    'publicUrl', 'signedUrl', 'cdnUrl', 'exportUrl', 'publishUrl',
    'downloadUrl', 'storageKey', 'base64', 'imageData', 'binaryData',
    'lyrics', 'videoData', 'audioData',
    'renderedVideo', 'videoFile', 'downloadFile', 'videoStream',
    'filePath', 'renderedVideoUrl', 'ffmpegCommand',
  ]

  for (const field of FORBIDDEN_FIELDS) {
    it(`yasak alan YOK: ${field}`, () => {
      // Check within TurkuRender* type blocks only
      const renderSection = types.substring(types.indexOf('FP-022'))
      expect(renderSection).not.toContain(`${field}:`)
    })
  }
})

// ── C. FP-022 API Client ────────────────────────────

describe('FP-022 API Client', () => {
  const api = readFile('api/turkuVideoApi.ts')

  it('prepareTurkuRenderJob fonksiyonu mevcut', () => {
    expect(api).toContain('prepareTurkuRenderJob')
  })

  it('getTurkuRenderJob fonksiyonu mevcut', () => {
    expect(api).toContain('getTurkuRenderJob')
  })

  it('runTurkuRenderJob fonksiyonu mevcut', () => {
    expect(api).toContain('runTurkuRenderJob')
  })

  it('getTurkuRenderExecutionSafety fonksiyonu mevcut', () => {
    expect(api).toContain('getTurkuRenderExecutionSafety')
  })

  it('prepare-render-job endpoint URL doğru', () => {
    expect(api).toContain('prepare-render-job')
  })

  it('render-job GET endpoint URL doğru', () => {
    expect(api).toContain('/render-job')
  })

  it('run-render-job endpoint URL doğru', () => {
    expect(api).toContain('run-render-job')
  })

  it('render-execution-safety endpoint URL doğru', () => {
    expect(api).toContain('render-execution-safety')
  })

  it('Authorization header içermez', () => {
    expect(api).not.toContain('Authorization')
    expect(api).not.toContain('Bearer')
  })

  it('publish/export/download fonksiyonu YOK', () => {
    expect(api).not.toContain('publishTurku')
    expect(api).not.toContain('exportTurku')
    expect(api).not.toContain('downloadTurku')
  })

  it('storageKey/signedUrl/cdnUrl/publicUrl YOK', () => {
    expect(api).not.toContain('storageKey')
    expect(api).not.toContain('signedUrl')
    expect(api).not.toContain('cdnUrl')
    expect(api).not.toContain('publicUrl')
  })
})

// ── D. FP-022 Workflow Page ─────────────────────────

describe('FP-022 Workflow Page', () => {
  const page = readFile('pages/TurkuVideoWorkflowPage.tsx')

  it('render-execution-section data-testid mevcut', () => {
    expect(page).toContain('render-execution-section')
  })

  it('render-job-status-panel data-testid mevcut', () => {
    expect(page).toContain('render-job-status-panel')
  })

  it('render-execution-safety-panel data-testid mevcut', () => {
    expect(page).toContain('render-execution-safety-panel')
  })

  it('render-safety-summary data-testid mevcut', () => {
    expect(page).toContain('render-safety-summary')
  })

  it('render-no-publish-label data-testid mevcut', () => {
    expect(page).toContain('render-no-publish-label')
  })

  it('Internal Stub Render label mevcut', () => {
    expect(page).toContain('Internal Stub Render')
  })

  it('Dahili render hazırlığı label mevcut', () => {
    expect(page).toContain('Dahili render hazırlığı')
  })

  it('Yayın/Dışa Aktarım Yok label mevcut', () => {
    expect(page).toContain('Yayın/Dışa Aktarım Yok')
  })

  it('İndirilebilir video yok label mevcut', () => {
    expect(page).toContain('İndirilebilir video yok')
  })

  it('Human Review Required label mevcut', () => {
    expect(page).toContain('Human Review Required')
  })

  it('Auto Approved: false label mevcut', () => {
    expect(page).toContain('Auto Approved')
  })

  it('prepareTurkuRenderJob API çağrısı mevcut', () => {
    expect(page).toContain('prepareTurkuRenderJob')
  })

  it('runTurkuRenderJob API çağrısı mevcut', () => {
    expect(page).toContain('runTurkuRenderJob')
  })

  it('getTurkuRenderExecutionSafety API çağrısı mevcut', () => {
    expect(page).toContain('getTurkuRenderExecutionSafety')
  })

  it('RENDER_JOB_PREPARED stage mevcut', () => {
    expect(page).toContain('RENDER_JOB_PREPARED')
  })

  it('RENDER_JOB_COMPLETED_INTERNAL stage mevcut', () => {
    expect(page).toContain('RENDER_JOB_COMPLETED_INTERNAL')
  })
})

// ── E. Forbidden Controls ───────────────────────────

describe('FP-022 Forbidden Controls', () => {
  const page = readFile('pages/TurkuVideoWorkflowPage.tsx')

  it('video player YOK', () => {
    expect(page).not.toContain('<video')
    expect(page).not.toContain('VideoPlayer')
    expect(page).not.toContain('ReactPlayer')
  })

  it('download/export/publish button YOK', () => {
    expect(page).not.toContain('download-btn')
    expect(page).not.toContain('export-btn')
    expect(page).not.toContain('publish-btn')
    expect(page).not.toContain('>İndir<')
    expect(page).not.toContain('Yayınla')
  })

  it('dangerouslySetInnerHTML YOK', () => {
    expect(page).not.toContain('dangerouslySetInnerHTML')
    expect(page).not.toContain('innerHTML')
  })

  it('publicUrl/signedUrl/cdnUrl YOK', () => {
    expect(page).not.toContain('publicUrl')
    expect(page).not.toContain('signedUrl')
    expect(page).not.toContain('cdnUrl')
  })

  it('filePath/storageKey/base64/binary YOK', () => {
    expect(page).not.toContain('filePath')
    expect(page).not.toContain('storageKey')
    expect(page).not.toContain('base64')
    expect(page).not.toContain('binaryData')
  })

  it('Authorization/Bearer/JWT YOK', () => {
    expect(page).not.toContain('Authorization')
    expect(page).not.toContain('Bearer')
    expect(page).not.toContain('JWT')
  })

  it('renderedVideoUrl YOK', () => {
    expect(page).not.toContain('renderedVideoUrl')
  })
})

// ── F. API Export Count ─────────────────────────────

describe('FP-022 API Export Count', () => {
  it('tam olarak 25 export fonksiyonu var (19 + 4 FP-022)', async () => {
    const turkuVideoApi = await import('../../api/turkuVideoApi')
    const exports = Object.keys(turkuVideoApi)
    expect(exports).toHaveLength(25)
  })
})
