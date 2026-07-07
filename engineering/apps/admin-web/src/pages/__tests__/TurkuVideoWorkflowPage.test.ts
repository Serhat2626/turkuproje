import { describe, it, expect } from 'vitest'
import * as fs from 'fs'
import * as path from 'path'

/**
 * FP-019 Task 05: Türkü Video Workflow Page + Types + Safety Tests.
 *
 * Kontrol noktaları:
 * - Types dosyası FP-019 type'larını içerir
 * - Types dosyası yasak alan içermez
 * - Page dosyası bootstrap form, stage actions, preview panellerini içerir
 * - Page dosyası unsafe HTML rendering kullanmaz
 * - Page dosyası publish/export/download kontrolleri içermez
 * - actorRole production auth olarak labellenmez
 * - API client Authorization header içermez
 */

const SRC_DIR = path.resolve(__dirname, '../../')

function readFile(relativePath: string): string {
  return fs.readFileSync(path.resolve(SRC_DIR, relativePath), 'utf-8')
}

// ── A. Types ────────────────────────────────────────────

describe('FP-019 Types', () => {
  const types = readFile('types/index.ts')

  it('TurkuVideoWorkflowStage type tanımlı', () => {
    expect(types).toContain('TurkuVideoWorkflowStage')
  })

  it('TurkuSourceConfidence type tanımlı', () => {
    expect(types).toContain('TurkuSourceConfidence')
  })

  it('TurkuImageBatchStatus type tanımlı', () => {
    expect(types).toContain('TurkuImageBatchStatus')
  })

  it('TurkuVideoBootstrapRequest interface tanımlı', () => {
    expect(types).toContain('TurkuVideoBootstrapRequest')
  })

  it('TurkuVideoBootstrapResponse interface tanımlı', () => {
    expect(types).toContain('TurkuVideoBootstrapResponse')
  })

  it('TurkuVideoProjectResponse interface tanımlı', () => {
    expect(types).toContain('TurkuVideoProjectResponse')
  })

  it('TurkuVideoProgressResponse interface tanımlı', () => {
    expect(types).toContain('TurkuVideoProgressResponse')
  })

  it('TurkuStoryPackage interface tanımlı', () => {
    expect(types).toContain('TurkuStoryPackage')
  })

  it('TurkuScenarioPlan interface tanımlı', () => {
    expect(types).toContain('TurkuScenarioPlan')
  })

  it('TurkuStoryboardPlan interface tanımlı', () => {
    expect(types).toContain('TurkuStoryboardPlan')
  })

  it('TurkuSceneAssetPlan interface tanımlı', () => {
    expect(types).toContain('TurkuSceneAssetPlan')
  })

  it('TurkuSceneImageBatch interface tanımlı', () => {
    expect(types).toContain('TurkuSceneImageBatch')
  })

  it('TurkuPreparationResponse interface tanımlı', () => {
    expect(types).toContain('TurkuPreparationResponse')
  })

  it('TurkuAssetPreparationResponse interface tanımlı', () => {
    expect(types).toContain('TurkuAssetPreparationResponse')
  })

  it('TurkuImageBatchResponse interface tanımlı', () => {
    expect(types).toContain('TurkuImageBatchResponse')
  })

  // Forbidden fields in types
  const FORBIDDEN_TYPE_FIELDS = [
    'token:', 'jwt:', 'session:', 'credential:', 'password:',
    'secret:', 'apiKey:', 'accessKey:', 'principalRaw:',
    'authorizationHeader:', 'publicUrl:', 'signedUrl:',
    'cdnUrl:', 'storageKey:', 'base64:', 'exportUrl:',
    'publishUrl:', 'downloadUrl:', 'imageData:', 'binaryData:',
  ]

  for (const field of FORBIDDEN_TYPE_FIELDS) {
    it(`types dosyasında '${field}' FP-019 bloğunda YOK`, () => {
      const fp019Block = types.substring(types.indexOf('FP-019'))
      expect(fp019Block).not.toContain(field)
    })
  }

  // Forbidden states
  it('PUBLISHED state yok', () => {
    const fp019Block = types.substring(types.indexOf('FP-019'))
    expect(fp019Block).not.toContain("'PUBLISHED'")
  })

  it('EXPORTED state yok', () => {
    const fp019Block = types.substring(types.indexOf('FP-019'))
    expect(fp019Block).not.toContain("'EXPORTED'")
  })

  it('AUTO_APPROVED state yok', () => {
    const fp019Block = types.substring(types.indexOf('FP-019'))
    expect(fp019Block).not.toContain("'AUTO_APPROVED'")
  })
})

// ── B. Page Structure ───────────────────────────────────

describe('FP-019 TurkuVideoWorkflowPage', () => {
  const page = readFile('pages/TurkuVideoWorkflowPage.tsx')

  // Bootstrap form
  it('türkü name input alanı mevcut', () => {
    expect(page).toContain('input-turku-name')
  })

  it('region input alanı mevcut', () => {
    expect(page).toContain('input-region')
  })

  it('mood input alanı mevcut', () => {
    expect(page).toContain('input-mood')
  })

  it('duration input alanı mevcut', () => {
    expect(page).toContain('input-duration')
  })

  it('source notes input alanı mevcut', () => {
    expect(page).toContain('input-source-notes')
  })

  it('narration style input alanı mevcut', () => {
    expect(page).toContain('input-narration-style')
  })

  it('visual style input alanı mevcut', () => {
    expect(page).toContain('input-visual-style')
  })

  it('bootstrap butonu mevcut', () => {
    expect(page).toContain('btn-bootstrap')
  })

  // Project summary
  it('project summary paneli mevcut', () => {
    expect(page).toContain('project-summary')
  })

  it('project ID gösterilir', () => {
    expect(page).toContain('project-id')
  })

  it('project name gösterilir', () => {
    expect(page).toContain('project-name')
  })

  it('project stage gösterilir', () => {
    expect(page).toContain('project-stage')
  })

  it('source confidence gösterilir', () => {
    expect(page).toContain('source-confidence')
  })

  // Review flags
  it('human review flag gösterilir', () => {
    expect(page).toContain('flag-human-review')
  })

  it('copyright flag gösterilir', () => {
    expect(page).toContain('flag-copyright')
  })

  // Progress
  it('progress panel mevcut', () => {
    expect(page).toContain('progress-panel')
  })

  it('stage timeline mevcut', () => {
    expect(page).toContain('stage-timeline')
  })

  it('progress summary gösterilir', () => {
    expect(page).toContain('progress-summary')
  })

  // Stage actions
  it('stage action butonları mevcut', () => {
    expect(page).toContain('stage-actions')
  })

  it('prepare story aksiyon tanımlı', () => {
    expect(page).toContain('Hikâye Hazırla')
    expect(page).toContain('DRAFT_CREATED')
  })

  it('prepare scenario aksiyon tanımlı', () => {
    expect(page).toContain('Senaryo Hazırla')
    expect(page).toContain('STORY_CONTEXT_PREPARED')
  })

  it('prepare storyboard aksiyon tanımlı', () => {
    expect(page).toContain('Storyboard Hazırla')
    expect(page).toContain('SCENARIO_PREPARED')
  })

  it('prepare scene assets aksiyon tanımlı', () => {
    expect(page).toContain('Varlık Planı Hazırla')
    expect(page).toContain('STORYBOARD_PREPARED')
  })

  it('prepare image batch aksiyon tanımlı', () => {
    expect(page).toContain('Görsel Batch Hazırla')
    expect(page).toContain('SCENE_ASSET_PLAN_PREPARED')
  })

  it('complete image batch aksiyon tanımlı', () => {
    expect(page).toContain('Batch Tamamla')
    expect(page).toContain('IMAGE_BATCH_PREPARED')
  })

  // Internal preview
  it('internal preview paneli mevcut', () => {
    expect(page).toContain('internal-preview')
  })

  it('preview disclaimer mevcut', () => {
    expect(page).toContain('preview-disclaimer')
  })

  it('preview story section mevcut', () => {
    expect(page).toContain('preview-story')
  })

  it('preview scenario section mevcut', () => {
    expect(page).toContain('preview-scenario')
  })

  it('preview storyboard section mevcut', () => {
    expect(page).toContain('preview-storyboard')
  })

  it('preview assets section mevcut', () => {
    expect(page).toContain('preview-assets')
  })

  it('preview image batch section mevcut', () => {
    expect(page).toContain('preview-image-batch')
  })

  // Labels
  it('dahili iş akışı labeli mevcut', () => {
    expect(page).toContain('Dahili')
  })

  it('sadece iç kullanım labeli mevcut', () => {
    expect(page).toContain('Sadece İç Kullanım')
  })
})

// ── C. Page Safety ──────────────────────────────────────

describe('FP-019 Page Safety', () => {
  const page = readFile('pages/TurkuVideoWorkflowPage.tsx')

  it('dangerouslySetInnerHTML YOK', () => {
    expect(page).not.toContain('dangerouslySetInnerHTML')
  })

  it('innerHTML YOK', () => {
    expect(page).not.toContain('innerHTML')
  })

  it('publish kontrolleri YOK', () => {
    // Check for publish-related UI controls/labels, not comments
    expect(page).not.toContain('btn-publish')
    expect(page).not.toContain('Yayınla')
    expect(page).not.toContain('publishVideo')
  })

  it('export/dışa aktarım kontrolleri YOK', () => {
    expect(page).not.toContain('btn-export')
    expect(page).not.toContain('exportVideo')
    expect(page).not.toContain('btn-download-export')
  })

  it('download kontrolleri YOK', () => {
    expect(page).not.toContain('btn-download')
    expect(page).not.toContain('İndir')
    expect(page).not.toContain('downloadVideo')
  })

  it('Authorization header YOK', () => {
    expect(page).not.toContain('Authorization')
  })

  it('JWT/token/session/principal YOK', () => {
    expect(page).not.toContain('JWT')
    expect(page).not.toContain('token')
    expect(page).not.toContain('session')
    expect(page).not.toContain('principal')
  })

  it('base64 YOK', () => {
    expect(page).not.toContain('base64')
  })

  it('publicUrl/signedUrl/cdnUrl YOK', () => {
    expect(page).not.toContain('publicUrl')
    expect(page).not.toContain('signedUrl')
    expect(page).not.toContain('cdnUrl')
  })

  it('storageKey YOK', () => {
    expect(page).not.toContain('storageKey')
  })

  it('actorRole production auth olarak label edilmemiş', () => {
    // actorRole should not appear as auth label on the page
    expect(page).not.toContain('Kullanıcı Kimliği')
    expect(page).not.toContain('Oturum Açan')
    expect(page).not.toContain('Giriş Yapan')
  })
})

// ── D. API Client Source Safety ─────────────────────────

describe('FP-019 API Client Safety', () => {
  const api = readFile('api/turkuVideoApi.ts')

  it('Authorization header göndermiyor', () => {
    expect(api).not.toContain('Authorization')
    expect(api).not.toContain('Bearer')
  })

  it('base64/storage/provider field YOK', () => {
    expect(api).not.toContain('base64')
    expect(api).not.toContain('storageKey')
    expect(api).not.toContain('signedUrl')
    expect(api).not.toContain('cdnUrl')
    expect(api).not.toContain('publicUrl')
  })

  it('publish/export endpoint YOK', () => {
    expect(api).not.toContain('/publish')
    expect(api).not.toContain('/export')
    expect(api).not.toContain('/download')
  })
})

// ── E. Route Integration ────────────────────────────────

describe('FP-019 Route Integration', () => {
  const app = readFile('App.tsx')
  const sidebar = readFile('components/layout/Sidebar.tsx')

  it('App.tsx turku-video route tanımlı', () => {
    expect(app).toContain('turku-video')
    expect(app).toContain('TurkuVideoWorkflowPage')
  })

  it('Sidebar turku-video nav item tanımlı', () => {
    expect(sidebar).toContain('/turku-video')
    expect(sidebar).toContain('Türkü Video')
  })
})

// ── F. FP-020 Provider Mode Types ───────────────────────

describe('FP-020 Types', () => {
  const types = readFile('types/index.ts')

  it('TurkuProviderMode type tanımlı', () => {
    expect(types).toContain('TurkuProviderMode')
  })

  it('TurkuProviderFailureCode type tanımlı', () => {
    expect(types).toContain('TurkuProviderFailureCode')
  })

  it('TurkuProviderTraceReference interface tanımlı', () => {
    expect(types).toContain('TurkuProviderTraceReference')
  })

  it('TurkuGenerationSafetyResponse interface tanımlı', () => {
    expect(types).toContain('TurkuGenerationSafetyResponse')
  })

  it('TurkuProviderPlanningGenerationResponse interface tanımlı', () => {
    expect(types).toContain('TurkuProviderPlanningGenerationResponse')
  })

  it('TurkuPlanningBundleGenerationResponse interface tanımlı', () => {
    expect(types).toContain('TurkuPlanningBundleGenerationResponse')
  })

  it('TurkuImageBatchRunResponse interface tanımlı', () => {
    expect(types).toContain('TurkuImageBatchRunResponse')
  })

  it('TurkuImageBatchRunSceneStatus interface tanımlı', () => {
    expect(types).toContain('TurkuImageBatchRunSceneStatus')
  })

  it('DETERMINISTIC_STUB provider mode tanımlı', () => {
    expect(types).toContain('DETERMINISTIC_STUB')
  })

  it('PROVIDER_DRAFT provider mode tanımlı', () => {
    expect(types).toContain('PROVIDER_DRAFT')
  })

  it('PROVIDER_DISABLED provider mode tanımlı', () => {
    expect(types).toContain('PROVIDER_DISABLED')
  })

  // Forbidden fields in FP-020 block
  const FP020_FORBIDDEN = [
    'token:', 'jwt:', 'session:', 'credential:', 'password:',
    'secret:', 'apiKey:', 'authorizationHeader:', 'publicUrl:',
    'signedUrl:', 'cdnUrl:', 'storageKey:', 'base64:',
    'exportUrl:', 'publishUrl:', 'downloadUrl:',
    'rawProviderPayload:', 'rawProviderResponse:',
  ]

  for (const field of FP020_FORBIDDEN) {
    it(`FP-020 types bloğunda '${field}' YOK`, () => {
      const fp020Block = types.substring(types.indexOf('FP-020'))
      expect(fp020Block).not.toContain(field)
    })
  }
})

// ── G. FP-020 Safety Panel ──────────────────────────────

describe('FP-020 Safety Panel', () => {
  const page = readFile('pages/TurkuVideoWorkflowPage.tsx')

  it('safety panel mevcut', () => {
    expect(page).toContain('safety-panel')
  })

  it('provider mode badge gösterilir', () => {
    expect(page).toContain('provider-mode-badge')
  })

  it('safety provider mode gösterilir', () => {
    expect(page).toContain('safety-provider-mode')
  })

  it('safety generation available gösterilir', () => {
    expect(page).toContain('safety-generation-available')
  })

  it('safety human review gösterilir', () => {
    expect(page).toContain('safety-human-review')
  })

  it('safety auto approved gösterilir', () => {
    expect(page).toContain('safety-auto-approved')
  })

  it('safety copyright review gösterilir', () => {
    expect(page).toContain('safety-copyright-review')
  })

  it('safety lyrics gösterilir', () => {
    expect(page).toContain('safety-lyrics')
  })

  it('safety blocked alert mevcut', () => {
    expect(page).toContain('safety-blocked-alert')
  })

  it('safety failure code gösterilir', () => {
    expect(page).toContain('safety-failure-code')
  })

  it('safety review flags gösterilir', () => {
    expect(page).toContain('safety-review-flags')
  })

  it('safety warnings gösterilir', () => {
    expect(page).toContain('safety-warnings')
  })

  it('safety trace ref gösterilir', () => {
    expect(page).toContain('safety-trace-ref')
  })
})

// ── H. FP-020 Generate Actions ──────────────────────────

describe('FP-020 Generate Actions', () => {
  const page = readFile('pages/TurkuVideoWorkflowPage.tsx')

  it('generate actions paneli mevcut', () => {
    expect(page).toContain('generate-actions')
  })

  it('generate story butonu mevcut', () => {
    expect(page).toContain('btn-generate-story')
  })

  it('generate scenario butonu mevcut', () => {
    expect(page).toContain('btn-generate-scenario')
  })

  it('generate storyboard butonu mevcut', () => {
    expect(page).toContain('btn-generate-storyboard')
  })

  it('generate planning bundle butonu mevcut', () => {
    expect(page).toContain('btn-generate-planning-bundle')
  })

  it('run image batch butonu mevcut', () => {
    expect(page).toContain('btn-run-image-batch')
  })

  it('generate blocked alert mevcut', () => {
    expect(page).toContain('generate-blocked-alert')
  })

  it('generate story API çağrısı mevcut', () => {
    expect(page).toContain('generateTurkuStory')
  })

  it('generate scenario API çağrısı mevcut', () => {
    expect(page).toContain('generateTurkuScenario')
  })

  it('generate storyboard API çağrısı mevcut', () => {
    expect(page).toContain('generateTurkuStoryboard')
  })

  it('generate planning bundle API çağrısı mevcut', () => {
    expect(page).toContain('generateTurkuPlanningBundle')
  })

  it('run image batch API çağrısı mevcut', () => {
    expect(page).toContain('runTurkuImageBatch')
  })

  it('generation safety API çağrısı mevcut', () => {
    expect(page).toContain('getTurkuGenerationSafety')
  })
})

// ── I. FP-020 Page Safety ───────────────────────────────

describe('FP-020 Page Safety', () => {
  const page = readFile('pages/TurkuVideoWorkflowPage.tsx')

  it('dangerouslySetInnerHTML YOK', () => {
    expect(page).not.toContain('dangerouslySetInnerHTML')
  })

  it('innerHTML YOK', () => {
    expect(page).not.toContain('innerHTML')
  })

  it('publish kontrolleri YOK', () => {
    expect(page).not.toContain('btn-publish')
    expect(page).not.toContain('publishVideo')
  })

  it('export kontrolleri YOK', () => {
    expect(page).not.toContain('btn-export')
    expect(page).not.toContain('exportVideo')
  })

  it('download kontrolleri YOK', () => {
    expect(page).not.toContain('btn-download')
    expect(page).not.toContain('downloadVideo')
  })

  it('rawProvider/rawPayload YOK', () => {
    expect(page).not.toContain('rawProvider')
    expect(page).not.toContain('rawPayload')
  })
})

// ── FP-021: Composition / Render Planning ────────────────

describe('FP-021 Types', () => {
  const types = readFile('types/index.ts')

  it('COMPOSITION_PLAN_PREPARED stage tanımlı', () => {
    expect(types).toContain('COMPOSITION_PLAN_PREPARED')
  })

  it('RENDER_MANIFEST_PREPARED stage tanımlı', () => {
    expect(types).toContain('RENDER_MANIFEST_PREPARED')
  })

  it('TurkuCompositionPlanResponse interface tanımlı', () => {
    expect(types).toContain('TurkuCompositionPlanResponse')
  })

  it('TurkuRenderManifestResponse interface tanımlı', () => {
    expect(types).toContain('TurkuRenderManifestResponse')
  })

  it('TurkuRenderReadinessReport interface tanımlı', () => {
    expect(types).toContain('TurkuRenderReadinessReport')
  })

  it('TurkuRenderReadinessStatus type tanımlı', () => {
    expect(types).toContain('TurkuRenderReadinessStatus')
  })

  it('TurkuCompositionPreviewResponse interface tanımlı', () => {
    expect(types).toContain('TurkuCompositionPreviewResponse')
  })

  it('TurkuCompositionPreviewScene interface tanımlı', () => {
    expect(types).toContain('TurkuCompositionPreviewScene')
  })

  it('TurkuRenderReadinessIssue interface tanımlı', () => {
    expect(types).toContain('TurkuRenderReadinessIssue')
  })

  // Safety
  it('types dosyasında renderedVideoUrl YOK', () => {
    expect(types).not.toContain('renderedVideoUrl')
  })

  it('types dosyasında downloadUrl YOK', () => {
    expect(types).not.toContain('downloadUrl')
  })

  it('types dosyasında exportUrl YOK', () => {
    expect(types).not.toContain('exportUrl')
  })
})

describe('FP-021 API Client', () => {
  const apiSource = readFile('api/turkuVideoApi.ts')

  it('prepareTurkuCompositionPlan fonksiyonu mevcut', () => {
    expect(apiSource).toContain('prepareTurkuCompositionPlan')
  })

  it('prepareTurkuRenderManifest fonksiyonu mevcut', () => {
    expect(apiSource).toContain('prepareTurkuRenderManifest')
  })

  it('getTurkuRenderReadiness fonksiyonu mevcut', () => {
    expect(apiSource).toContain('getTurkuRenderReadiness')
  })

  it('getTurkuCompositionPreview fonksiyonu mevcut', () => {
    expect(apiSource).toContain('getTurkuCompositionPreview')
  })

  it('doğru endpoint path: prepare-composition-plan', () => {
    expect(apiSource).toContain('prepare-composition-plan')
  })

  it('doğru endpoint path: prepare-render-manifest', () => {
    expect(apiSource).toContain('prepare-render-manifest')
  })

  it('doğru endpoint path: render-readiness', () => {
    expect(apiSource).toContain('render-readiness')
  })

  it('doğru endpoint path: composition-preview', () => {
    expect(apiSource).toContain('composition-preview')
  })

  it('Bearer header YOK', () => {
    expect(apiSource).not.toContain('Bearer')
  })

  it('Authorization header YOK', () => {
    expect(apiSource).not.toContain('Authorization')
  })
})

describe('FP-021 Composition / Render Planning Page', () => {
  const page = readFile('pages/TurkuVideoWorkflowPage.tsx')

  it('composition planning section mevcut', () => {
    expect(page).toContain('composition-planning-section')
  })

  it('readiness panel mevcut', () => {
    expect(page).toContain('readiness-panel')
  })

  it('composition timeline mevcut', () => {
    expect(page).toContain('composition-timeline')
  })

  it('readiness status gösteriliyor', () => {
    expect(page).toContain('readiness-status')
  })

  it('readiness issues gösteriliyor', () => {
    expect(page).toContain('readiness-issues')
  })

  it('review flags gösteriliyor', () => {
    expect(page).toContain('review-flags')
  })

  it('composition disclaimer gösteriliyor', () => {
    expect(page).toContain('composition-disclaimer')
  })

  it('composition safety summary gösteriliyor', () => {
    expect(page).toContain('composition-safety-summary')
  })

  it('Kompozisyon Planı Hazırla action mevcut', () => {
    expect(page).toContain('Kompozisyon Planı Hazırla')
  })

  it('Render Manifest Hazırla action mevcut', () => {
    expect(page).toContain('Render Manifest Hazırla')
  })

  it('prepareTurkuCompositionPlan API çağrısı mevcut', () => {
    expect(page).toContain('prepareTurkuCompositionPlan')
  })

  it('prepareTurkuRenderManifest API çağrısı mevcut', () => {
    expect(page).toContain('prepareTurkuRenderManifest')
  })

  it('getTurkuCompositionPreview API çağrısı mevcut', () => {
    expect(page).toContain('getTurkuCompositionPreview')
  })

  it('getTurkuRenderReadiness API çağrısı mevcut', () => {
    expect(page).toContain('getTurkuRenderReadiness')
  })

  it('yayın hazır değil bilgisi gösteriliyor', () => {
    expect(page).toContain('sadece dahili planlama')
  })

  it('dışa aktarım hazır değil bilgisi gösteriliyor', () => {
    expect(page).toContain('aktarım hazır')
  })

  it('timeline scene detayları gösteriliyor', () => {
    expect(page).toContain('timeline-scene-')
    expect(page).toContain('narrationSummary')
    expect(page).toContain('imageRefLabel')
    expect(page).toContain('durationSeconds')
  })

  // Safety
  it('video player YOK', () => {
    expect(page).not.toContain('<video')
    expect(page).not.toContain('videoPlayer')
  })

  it('download/export/publish butonları YOK', () => {
    expect(page).not.toContain('btn-download')
    expect(page).not.toContain('btn-export')
    expect(page).not.toContain('btn-publish')
  })

  it('lyrics body YOK', () => {
    expect(page).not.toContain('lyricsBody')
    expect(page).not.toContain('lyrics-body')
  })

  it('dangerouslySetInnerHTML YOK', () => {
    expect(page).not.toContain('dangerouslySetInnerHTML')
  })

  it('renderedVideoUrl YOK', () => {
    expect(page).not.toContain('renderedVideoUrl')
  })

  it('publicUrl/signedUrl/cdnUrl YOK', () => {
    expect(page).not.toContain('publicUrl')
    expect(page).not.toContain('signedUrl')
    expect(page).not.toContain('cdnUrl')
  })
})
