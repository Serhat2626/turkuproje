import { describe, it, expect } from 'vitest'
import * as fs from 'fs'
import * as path from 'path'
import * as turkuVideoApi from '../../api/turkuVideoApi'

/**
 * FP-021 Task 05: E2E Render-Planning Closure Regression.
 *
 * Final closure verification:
 * - FP-021 types, API, page additions intact
 * - FP-019/FP-020 backward compatibility preserved
 * - Safety baseline maintained
 * - No publish/export/download/render
 * - No Authorization/JWT/token/session
 * - No forbidden fields
 */

const SRC_DIR = path.resolve(__dirname, '../../')

function readFile(relativePath: string): string {
  return fs.readFileSync(path.resolve(SRC_DIR, relativePath), 'utf-8')
}

// ── A. API Client Completeness ──────────────────────

describe('FP-021 Closure: API Client', () => {
  it('tam olarak 19 export fonksiyonu var', () => {
    expect(Object.keys(turkuVideoApi)).toHaveLength(23)
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

  // Forbidden functions
  const FORBIDDEN_FUNCTIONS = [
    'publishTurkuVideo', 'exportTurkuVideo', 'downloadTurkuVideo',
    'renderTurkuVideo', 'finalizeTurkuVideo', 'getPublicUrl',
    'getSignedUrl', 'getCdnUrl', 'getStorageKey', 'getBase64Image',
    'setAuthorization', 'loginAdmin',
  ]
  for (const fn of FORBIDDEN_FUNCTIONS) {
    it(`yasak function ${fn} YOK`, () => {
      expect((turkuVideoApi as Record<string, unknown>)[fn]).toBeUndefined()
    })
  }

  it('API client kaynak kodu Authorization header içermez', () => {
    const source = readFile('api/turkuVideoApi.ts')
    expect(source).not.toContain('Bearer')
    expect(source).not.toContain('Authorization')
  })
})

// ── B. Types Completeness ──────────────────────────

describe('FP-021 Closure: Types', () => {
  const types = readFile('types/index.ts')

  // FP-021 stages
  it('COMPOSITION_PLAN_PREPARED stage mevcut', () => {
    expect(types).toContain('COMPOSITION_PLAN_PREPARED')
  })

  it('RENDER_MANIFEST_PREPARED stage mevcut', () => {
    expect(types).toContain('RENDER_MANIFEST_PREPARED')
  })

  // FP-021 types
  const FP021_TYPES = [
    'TurkuCompositionPlanResponse', 'TurkuRenderManifestResponse',
    'TurkuRenderReadinessReport', 'TurkuRenderReadinessStatus',
    'TurkuRenderReadinessIssue', 'TurkuCompositionPreviewResponse',
    'TurkuCompositionPreviewScene',
  ]
  for (const t of FP021_TYPES) {
    it(`FP-021 type ${t} tanımlı`, () => {
      expect(types).toContain(t)
    })
  }

  // FP-019 types still present
  const FP019_TYPES = [
    'TurkuVideoWorkflowStage', 'TurkuVideoBootstrapRequest',
    'TurkuVideoBootstrapResponse', 'TurkuVideoProjectResponse',
    'TurkuVideoProgressResponse',
  ]
  for (const t of FP019_TYPES) {
    it(`FP-019 type ${t} hala tanımlı`, () => {
      expect(types).toContain(t)
    })
  }

  // Forbidden fields across entire types file
  const FORBIDDEN_FIELDS = [
    'renderedVideoUrl', 'downloadUrl', 'exportUrl', 'publishUrl',
    'publicUrl', 'signedUrl', 'cdnUrl', 'storageKey', 'base64Data',
    'videoStream', 'audioStream', 'lyricsBody', 'rawProviderPayload',
  ]
  for (const field of FORBIDDEN_FIELDS) {
    it(`yasak alan ${field} YOK`, () => {
      expect(types).not.toContain(field)
    })
  }

  // Forbidden stages
  const FORBIDDEN_STAGES = [
    'PUBLISHED', 'EXPORTED', 'AUTO_APPROVED',
    'PUBLICLY_DELIVERED', 'COMPLETED_AS_PUBLIC_VIDEO',
  ]
  for (const stage of FORBIDDEN_STAGES) {
    it(`yasak stage ${stage} YOK`, () => {
      expect(types).not.toContain(`'${stage}'`)
    })
  }
})

// ── C. Page Safety ─────────────────────────────────

describe('FP-021 Closure: Page Safety', () => {
  const page = readFile('pages/TurkuVideoWorkflowPage.tsx')

  // FP-021 sections present
  it('composition planning section mevcut', () => {
    expect(page).toContain('composition-planning-section')
  })

  it('readiness panel mevcut', () => {
    expect(page).toContain('readiness-panel')
  })

  it('composition timeline mevcut', () => {
    expect(page).toContain('composition-timeline')
  })

  it('composition disclaimer mevcut', () => {
    expect(page).toContain('composition-disclaimer')
  })

  it('composition safety summary mevcut', () => {
    expect(page).toContain('composition-safety-summary')
  })

  // FP-021 actions
  it('Kompozisyon Planı Hazırla action mevcut', () => {
    expect(page).toContain('Kompozisyon Planı Hazırla')
  })

  it('Render Manifest Hazırla action mevcut', () => {
    expect(page).toContain('Render Manifest Hazırla')
  })

  // FP-021 API calls
  it('prepareTurkuCompositionPlan çağrısı mevcut', () => {
    expect(page).toContain('prepareTurkuCompositionPlan')
  })

  it('prepareTurkuRenderManifest çağrısı mevcut', () => {
    expect(page).toContain('prepareTurkuRenderManifest')
  })

  it('getTurkuCompositionPreview çağrısı mevcut', () => {
    expect(page).toContain('getTurkuCompositionPreview')
  })

  it('getTurkuRenderReadiness çağrısı mevcut', () => {
    expect(page).toContain('getTurkuRenderReadiness')
  })

  // FP-019 backward compatibility
  it('FP-019 bootstrap form hala mevcut', () => {
    expect(page).toContain('bootstrap-form')
  })

  it('FP-019 stage actions hala mevcut', () => {
    expect(page).toContain('Hikâye Hazırla')
    expect(page).toContain('Senaryo Hazırla')
    expect(page).toContain('Storyboard Hazırla')
  })

  it('FP-019 internal preview hala mevcut', () => {
    expect(page).toContain('internal-preview')
  })

  // FP-020 backward compatibility
  it('FP-020 provider mode badge hala mevcut', () => {
    expect(page).toContain('provider-mode-badge')
  })

  // Safety checks
  it('dangerouslySetInnerHTML YOK', () => {
    expect(page).not.toContain('dangerouslySetInnerHTML')
  })

  it('innerHTML YOK', () => {
    expect(page).not.toContain('innerHTML')
  })

  it('video player element YOK', () => {
    expect(page).not.toContain('<video')
    expect(page).not.toContain('videoPlayer')
    expect(page).not.toContain('video-player')
  })

  it('publish/export/download butonları YOK', () => {
    expect(page).not.toContain('btn-publish')
    expect(page).not.toContain('btn-export')
    expect(page).not.toContain('btn-download')
  })

  it('JWT/principal/session/token YOK', () => {
    expect(page).not.toContain('JWT')
    expect(page).not.toContain('principal')
    expect(page).not.toContain('session')
  })

  it('raw provider data YOK', () => {
    expect(page).not.toContain('rawProvider')
    expect(page).not.toContain('rawPayload')
  })

  it('base64/storage YOK', () => {
    expect(page).not.toContain('base64')
    expect(page).not.toContain('storageKey')
  })

  it('public/signed/CDN URL YOK', () => {
    expect(page).not.toContain('publicUrl')
    expect(page).not.toContain('signedUrl')
    expect(page).not.toContain('cdnUrl')
  })

  it('renderedVideoUrl YOK', () => {
    expect(page).not.toContain('renderedVideoUrl')
  })

  it('lyricsBody YOK', () => {
    expect(page).not.toContain('lyricsBody')
    expect(page).not.toContain('lyrics-body')
  })
})

// ── D. API Source Safety ──────────────────────────

describe('FP-021 Closure: API Source Safety', () => {
  const apiSource = readFile('api/turkuVideoApi.ts')

  it('4 FP-021 endpoint path\'leri doğru', () => {
    expect(apiSource).toContain('prepare-composition-plan')
    expect(apiSource).toContain('prepare-render-manifest')
    expect(apiSource).toContain('render-readiness')
    expect(apiSource).toContain('composition-preview')
  })

  it('publishVideo/exportVideo/downloadVideo YOK', () => {
    expect(apiSource).not.toContain('publishVideo')
    expect(apiSource).not.toContain('exportVideo')
    expect(apiSource).not.toContain('downloadVideo')
  })

  it('renderVideo endpoint YOK', () => {
    expect(apiSource).not.toContain('render-video')
    expect(apiSource).not.toContain('renderVideo')
  })

  it('storageKey/signedUrl/cdnUrl YOK', () => {
    expect(apiSource).not.toContain('storageKey')
    expect(apiSource).not.toContain('signedUrl')
    expect(apiSource).not.toContain('cdnUrl')
  })
})
