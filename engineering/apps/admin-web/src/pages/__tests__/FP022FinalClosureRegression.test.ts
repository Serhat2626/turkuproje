import { describe, it, expect } from 'vitest'
import * as fs from 'fs'
import * as path from 'path'
import * as turkuVideoApi from '../../api/turkuVideoApi'

/**
 * FP-022 Task 05: E2E Render Execution Closure Regression.
 *
 * Final closure verification:
 * - FP-022 types, API, page additions intact
 * - FP-019/020/021 backward compatibility preserved
 * - Safety baseline maintained
 * - No publish/export/download/video player
 * - No Authorization/JWT/token/session
 * - No forbidden fields
 */

const SRC_DIR = path.resolve(__dirname, '../../')

function readFile(relativePath: string): string {
  return fs.readFileSync(path.resolve(SRC_DIR, relativePath), 'utf-8')
}

// ── A. API Client Completeness ──────────────────────

describe('FP-022 Closure: API Client', () => {
  it('tam olarak 25 export fonksiyonu var', () => {
    expect(Object.keys(turkuVideoApi)).toHaveLength(25)
  })

  // FP-019 functions
  const FP019_FUNCTIONS = [
    'bootstrapTurkuVideoProject', 'getTurkuVideoProject', 'getTurkuVideoProjectProgress',
    'prepareTurkuStory', 'prepareTurkuScenario', 'prepareTurkuStoryboard',
    'prepareTurkuSceneAssets', 'prepareTurkuImageBatch', 'completeTurkuImageBatch',
  ]
  for (const fn of FP019_FUNCTIONS) {
    it(`FP-019 function ${fn} mevcut`, () => {
      expect(typeof (turkuVideoApi as Record<string, unknown>)[fn]).toBe('function')
    })
  }

  // FP-020 functions
  const FP020_FUNCTIONS = [
    'generateTurkuStory', 'generateTurkuScenario', 'generateTurkuStoryboard',
    'generateTurkuPlanningBundle', 'runTurkuImageBatch', 'getTurkuGenerationSafety',
  ]
  for (const fn of FP020_FUNCTIONS) {
    it(`FP-020 function ${fn} mevcut`, () => {
      expect(typeof (turkuVideoApi as Record<string, unknown>)[fn]).toBe('function')
    })
  }

  // FP-021 functions
  const FP021_FUNCTIONS = [
    'prepareTurkuCompositionPlan', 'prepareTurkuRenderManifest',
    'getTurkuRenderReadiness', 'getTurkuCompositionPreview',
  ]
  for (const fn of FP021_FUNCTIONS) {
    it(`FP-021 function ${fn} mevcut`, () => {
      expect(typeof (turkuVideoApi as Record<string, unknown>)[fn]).toBe('function')
    })
  }

  // FP-022 functions
  const FP022_FUNCTIONS = [
    'prepareTurkuRenderJob', 'getTurkuRenderJob',
    'runTurkuRenderJob', 'getTurkuRenderExecutionSafety',
  ]
  for (const fn of FP022_FUNCTIONS) {
    it(`FP-022 function ${fn} mevcut`, () => {
      expect(typeof (turkuVideoApi as Record<string, unknown>)[fn]).toBe('function')
    })
  }

  // Forbidden functions
  const FORBIDDEN = [
    'publishTurkuVideo', 'exportTurkuVideo', 'downloadTurkuVideo',
    'getPublicUrl', 'getSignedUrl', 'getCdnUrl', 'getStorageKey',
    'getBase64Image', 'setAuthorization', 'loginAdmin',
    'renderVideo', 'downloadRenderedVideo', 'getRenderedVideoUrl',
  ]
  for (const fn of FORBIDDEN) {
    it(`forbidden function ${fn} YOK`, () => {
      expect((turkuVideoApi as Record<string, unknown>)[fn]).toBeUndefined()
    })
  }
})

// ── B. FP-022 Types ──────────────────────────────────

describe('FP-022 Closure: Types', () => {
  const types = readFile('types/index.ts')

  const FP022_TYPES = [
    'TurkuRenderJobStatus', 'TurkuRenderFailureCode',
    'TurkuRenderAttempt', 'TurkuRenderArtifact',
    'TurkuRenderJobResponse', 'TurkuRenderExecutionResponse',
    'TurkuRenderExecutionSafetyEnvelope', 'TurkuRenderExecutionSafetyResponse',
  ]
  for (const t of FP022_TYPES) {
    it(`FP-022 type ${t} tanımlı`, () => {
      expect(types).toContain(t)
    })
  }

  it('humanReviewRequired field mevcut', () => {
    expect(types).toContain('humanReviewRequired: boolean')
  })

  it('autoApproved field mevcut', () => {
    expect(types).toContain('autoApproved: boolean')
  })

  it('notPublishReady field mevcut', () => {
    expect(types).toContain('notPublishReady: boolean')
  })

  it('notExportReady field mevcut', () => {
    expect(types).toContain('notExportReady: boolean')
  })
})

// ── C. FP-022 Types Safety ───────────────────────────

describe('FP-022 Closure: Type Safety', () => {
  const types = readFile('types/index.ts')

  const FORBIDDEN_FIELDS = [
    'rawPayload', 'rawRendererPayload', 'token', 'jwt', 'session',
    'credential', 'password', 'secret', 'apiKey', 'authorizationHeader',
    'publicUrl', 'signedUrl', 'cdnUrl', 'exportUrl', 'publishUrl',
    'downloadUrl', 'storageKey', 'base64', 'binaryData',
    'renderedVideo', 'videoFile', 'downloadFile', 'filePath',
    'renderedVideoUrl', 'ffmpegCommand',
  ]

  for (const field of FORBIDDEN_FIELDS) {
    it(`yasak alan YOK: ${field}`, () => {
      const renderSection = types.substring(types.indexOf('FP-022'))
      expect(renderSection).not.toContain(`${field}:`)
    })
  }
})

// ── D. Page Safety ───────────────────────────────────

describe('FP-022 Closure: Page Safety', () => {
  const page = readFile('pages/TurkuVideoWorkflowPage.tsx')

  it('render-execution-section mevcut', () => {
    expect(page).toContain('render-execution-section')
  })

  it('render-job-status-panel mevcut', () => {
    expect(page).toContain('render-job-status-panel')
  })

  it('render-execution-safety-panel mevcut', () => {
    expect(page).toContain('render-execution-safety-panel')
  })

  it('Internal Stub Render label mevcut', () => {
    expect(page).toContain('Internal Stub Render')
  })

  it('Yayın/Dışa Aktarım Yok label mevcut', () => {
    expect(page).toContain('Yayın/Dışa Aktarım Yok')
  })

  it('Human Review Required label mevcut', () => {
    expect(page).toContain('Human Review Required')
  })

  it('Auto Approved label mevcut', () => {
    expect(page).toContain('Auto Approved')
  })

  // Forbidden controls
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

// ── E. API Source Safety ─────────────────────────────

describe('FP-022 Closure: API Source Safety', () => {
  const api = readFile('api/turkuVideoApi.ts')

  it('Authorization header yok', () => {
    expect(api).not.toContain('Authorization')
    expect(api).not.toContain('Bearer')
  })

  it('publish/export/download yok', () => {
    expect(api).not.toContain('publishTurku')
    expect(api).not.toContain('exportTurku')
    expect(api).not.toContain('downloadTurku')
  })

  it('storageKey/signedUrl/cdnUrl/publicUrl yok', () => {
    expect(api).not.toContain('storageKey')
    expect(api).not.toContain('signedUrl')
    expect(api).not.toContain('cdnUrl')
    expect(api).not.toContain('publicUrl')
  })

  it('prepare-render-job endpoint doğru', () => {
    expect(api).toContain('prepare-render-job')
  })

  it('run-render-job endpoint doğru', () => {
    expect(api).toContain('run-render-job')
  })

  it('render-execution-safety endpoint doğru', () => {
    expect(api).toContain('render-execution-safety')
  })

  it('render-job GET endpoint doğru', () => {
    expect(api).toContain('/render-job')
  })
})

// ── F. Backward Compatibility ────────────────────────

describe('FP-022 Closure: Backward Compatibility', () => {
  const page = readFile('pages/TurkuVideoWorkflowPage.tsx')

  it('bootstrap form mevcut', () => {
    expect(page).toContain('bootstrap-form')
  })

  it('stage-actions mevcut', () => {
    expect(page).toContain('stage-actions')
  })

  it('FP-020 generate-actions mevcut', () => {
    expect(page).toContain('generate-actions')
  })

  it('FP-020 safety-panel mevcut', () => {
    expect(page).toContain('safety-panel')
  })

  it('FP-021 composition-planning-section mevcut', () => {
    expect(page).toContain('composition-planning-section')
  })

  it('FP-021 readiness-panel mevcut', () => {
    expect(page).toContain('readiness-panel')
  })

  it('internal-preview mevcut', () => {
    expect(page).toContain('internal-preview')
  })
})

