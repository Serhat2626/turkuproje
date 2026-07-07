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
    expect(page).not.toContain('Dışa Aktar')
    expect(page).not.toContain('exportVideo')
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
