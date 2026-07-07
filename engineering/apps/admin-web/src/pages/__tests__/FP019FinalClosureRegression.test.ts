import { describe, it, expect } from 'vitest'
import * as fs from 'fs'
import * as path from 'path'

/**
 * FP-019 Task 06: Final E2E Frontend Safety Regression + Closure.
 *
 * Broad final verification:
 * - Route reachable
 * - Types complete and safe
 * - API client complete and safe
 * - Page UI structure and safety
 * - Sidebar nav integration
 * - No forbidden patterns in entire FP-019 surface area
 */

const SRC_DIR = path.resolve(__dirname, '../../')

function readFile(relativePath: string): string {
  return fs.readFileSync(path.resolve(SRC_DIR, relativePath), 'utf-8')
}

function readAllTsFiles(dir: string): string[] {
  const results: string[] = []
  if (!fs.existsSync(dir)) return results
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const fullPath = path.join(dir, entry.name)
    if (entry.isDirectory() && entry.name !== 'node_modules' && entry.name !== '__tests__') {
      results.push(...readAllTsFiles(fullPath))
    } else if (entry.isFile() && (entry.name.endsWith('.ts') || entry.name.endsWith('.tsx'))) {
      results.push(fullPath)
    }
  }
  return results
}

// ── A. Route Integration ────────────────────────────────

describe('FP-019 Closure: Route Integration', () => {
  const app = readFile('App.tsx')
  const sidebar = readFile('components/layout/Sidebar.tsx')

  it('App.tsx defines turku-video route', () => {
    expect(app).toContain('turku-video')
    expect(app).toContain('TurkuVideoWorkflowPage')
  })

  it('Sidebar has Türkü Video nav item', () => {
    expect(sidebar).toContain('/turku-video')
    expect(sidebar).toContain('Türkü Video')
  })

  it('Türkü Video nav is not disabled', () => {
    const turkuLine = sidebar.split('\n').find(l => l.includes('turku-video'))
    expect(turkuLine).toBeDefined()
    expect(turkuLine).not.toContain('disabled')
  })
})

// ── B. Types Completeness ───────────────────────────────

describe('FP-019 Closure: Types Completeness', () => {
  const types = readFile('types/index.ts')

  const REQUIRED_TYPES = [
    'TurkuVideoWorkflowStage',
    'TurkuSourceConfidence',
    'TurkuImageBatchStatus',
    'TurkuVideoBootstrapRequest',
    'TurkuVideoBootstrapResponse',
    'TurkuVideoProjectResponse',
    'TurkuVideoProgressResponse',
    'TurkuStoryPackage',
    'TurkuNarrativeContext',
    'TurkuScenarioPlan',
    'TurkuStoryboardPlan',
    'TurkuStoryboardScene',
    'TurkuSceneAssetPlan',
    'TurkuSceneAssetItem',
    'TurkuSceneImageBatch',
    'TurkuSceneImageRequest',
    'TurkuSceneImageResult',
    'TurkuPreparationResponse',
    'TurkuAssetPreparationResponse',
    'TurkuImageBatchResponse',
  ]

  for (const t of REQUIRED_TYPES) {
    it(`type ${t} tanımlı`, () => {
      expect(types).toContain(t)
    })
  }
})

// ── C. Types Safety ─────────────────────────────────────

describe('FP-019 Closure: Types Safety', () => {
  const types = readFile('types/index.ts')
  const fp019 = types.substring(types.indexOf('FP-019'))

  const FORBIDDEN_FIELDS = [
    'token:', 'jwt:', 'session:', 'credential:', 'password:',
    'secret:', 'apiKey:', 'accessKey:', 'principalRaw:',
    'authorizationHeader:', 'publicUrl:', 'signedUrl:',
    'cdnUrl:', 'storageKey:', 'base64:', 'exportUrl:',
    'publishUrl:', 'downloadUrl:', 'imageData:', 'binaryData:',
    'lyrics:', 'copyrightedAudio:', 'copyrightedVideo:',
  ]

  for (const f of FORBIDDEN_FIELDS) {
    it(`FP-019 types: yasak alan '${f}' yok`, () => {
      expect(fp019).not.toContain(f)
    })
  }

  const FORBIDDEN_STATES = [
    "'PUBLISHED'", "'EXPORTED'", "'AUTO_APPROVED'",
    "'PUBLICLY_DELIVERED'", "'COMPLETED_AS_PUBLIC_VIDEO'",
  ]

  for (const s of FORBIDDEN_STATES) {
    it(`FP-019 types: yasak state ${s} yok`, () => {
      expect(fp019).not.toContain(s)
    })
  }
})

// ── D. API Client Completeness ──────────────────────────

describe('FP-019 Closure: API Client Completeness', () => {
  const apiSource = readFile('api/turkuVideoApi.ts')

  const REQUIRED_METHODS = [
    'bootstrapTurkuVideoProject',
    'getTurkuVideoProject',
    'getTurkuVideoProjectProgress',
    'prepareTurkuStory',
    'prepareTurkuScenario',
    'prepareTurkuStoryboard',
    'prepareTurkuSceneAssets',
    'prepareTurkuImageBatch',
    'completeTurkuImageBatch',
  ]

  for (const m of REQUIRED_METHODS) {
    it(`${m} fonksiyonu tanımlı`, () => {
      expect(apiSource).toContain(`export function ${m}`)
    })
  }

  it('tam olarak 19 export function var', () => {
    const matches = apiSource.match(/export function \w+/g) || []
    expect(matches).toHaveLength(19)
  })

  const FORBIDDEN_METHODS = [
    'publishVideo', 'exportVideo', 'downloadVideo',
    'getPublicUrl', 'getSignedUrl', 'getCdnUrl',
    'getStorageKey', 'getBase64Image', 'setAuthorization',
    'loginAdmin', 'getProviderPayload',
  ]

  for (const m of FORBIDDEN_METHODS) {
    it(`yasak fonksiyon '${m}' yok`, () => {
      expect(apiSource).not.toContain(`function ${m}`)
    })
  }
})

// ── E. API Client Source Safety ──────────────────────────

describe('FP-019 Closure: API Client Source Safety', () => {
  const api = readFile('api/turkuVideoApi.ts')

  it('Bearer header yok', () => {
    expect(api).not.toContain('Bearer')
  })

  it('providerKey/storageKey/signedUrl yok', () => {
    expect(api).not.toContain('storageKey')
    expect(api).not.toContain('signedUrl')
    expect(api).not.toContain('cdnUrl')
    expect(api).not.toContain('publicUrl')
  })

  it('/publish veya /export endpoint yok', () => {
    expect(api).not.toContain('/publish')
    expect(api).not.toContain('/export')
    expect(api).not.toContain('/download')
  })
})

// ── F. Page UI Structure ────────────────────────────────

describe('FP-019 Closure: Page UI Structure', () => {
  const page = readFile('pages/TurkuVideoWorkflowPage.tsx')

  // Bootstrap form
  it('bootstrap form mevcut', () => {
    expect(page).toContain('bootstrap-form')
  })

  it('türkü name required input mevcut', () => {
    expect(page).toContain('turkuName')
    expect(page).toContain('required')
  })

  it('optional fields mevcut (region, mood, duration, sourceNotes, narrationStyle, visualStyle)', () => {
    expect(page).toContain('region')
    expect(page).toContain('mood')
    expect(page).toContain('targetDurationMinutes')
    expect(page).toContain('sourceNotes')
    expect(page).toContain('narrationStyle')
    expect(page).toContain('visualStyle')
  })

  // Project summary
  it('project summary panel mevcut', () => {
    expect(page).toContain('project-summary')
  })

  // Progress panel
  it('progress panel mevcut', () => {
    expect(page).toContain('progress-panel')
    expect(page).toContain('stage-timeline')
  })

  // Stage actions
  it('6 workflow stage actions tanımlı', () => {
    expect(page).toContain('Hikâye Hazırla')
    expect(page).toContain('Senaryo Hazırla')
    expect(page).toContain('Storyboard Hazırla')
    expect(page).toContain('Varlık Planı Hazırla')
    expect(page).toContain('Görsel Batch Hazırla')
    expect(page).toContain('Batch Tamamla')
  })

  // Internal preview
  it('internal preview panel mevcut', () => {
    expect(page).toContain('internal-preview')
    expect(page).toContain('preview-disclaimer')
  })

  it('preview sections: story, scenario, storyboard, assets, batch', () => {
    expect(page).toContain('preview-story')
    expect(page).toContain('preview-scenario')
    expect(page).toContain('preview-storyboard')
    expect(page).toContain('preview-assets')
    expect(page).toContain('preview-image-batch')
  })

  // Labels
  it('dahili iş akışı labelleri', () => {
    expect(page).toContain('Dahili')
    expect(page).toContain('Sadece İç Kullanım')
  })

  it('review flags labelleri', () => {
    expect(page).toContain('İnsan İncelemesi Gerekli')
    expect(page).toContain('Telif İncelemesi Gerekli')
  })
})

// ── G. Page Safety ──────────────────────────────────────

describe('FP-019 Closure: Page Safety', () => {
  const page = readFile('pages/TurkuVideoWorkflowPage.tsx')

  it('dangerouslySetInnerHTML yok', () => {
    expect(page).not.toContain('dangerouslySetInnerHTML')
  })

  it('innerHTML yok', () => {
    expect(page).not.toContain('innerHTML')
  })

  it('btn-publish/btn-export/btn-download yok', () => {
    expect(page).not.toContain('btn-publish')
    expect(page).not.toContain('btn-export')
    expect(page).not.toContain('btn-download')
  })

  it('Yayınla/Dışa Aktar/İndir butonları yok', () => {
    expect(page).not.toContain('Yayınla')
    expect(page).not.toContain('btn-export')
    expect(page).not.toContain('İndir')
  })

  it('JWT/principal/session yok', () => {
    expect(page).not.toContain('JWT')
    expect(page).not.toContain('principal')
    expect(page).not.toContain('session')
  })

  it('base64/storage/provider yok', () => {
    expect(page).not.toContain('base64')
    expect(page).not.toContain('storageKey')
    expect(page).not.toContain('publicUrl')
    expect(page).not.toContain('signedUrl')
    expect(page).not.toContain('cdnUrl')
  })

  it('actorRole üretim kimliği olarak gösterilmiyor', () => {
    expect(page).not.toContain('Kullanıcı Kimliği')
    expect(page).not.toContain('Oturum Açan')
    expect(page).not.toContain('Giriş Yapan')
  })
})

// ── H. Broad Source Safety Scan ─────────────────────────

describe('FP-019 Closure: Broad FP-019 Source Safety', () => {
  const fp019Files = [
    'api/turkuVideoApi.ts',
    'pages/TurkuVideoWorkflowPage.tsx',
  ]

  const allContent = fp019Files.map(f => readFile(f)).join('\n')

  it('dangerouslySetInnerHTML yok (tüm FP-019)', () => {
    expect(allContent).not.toContain('dangerouslySetInnerHTML')
  })

  it('innerHTML yok (tüm FP-019)', () => {
    expect(allContent).not.toContain('innerHTML')
  })

  it('localStorage/sessionStorage yok (tüm FP-019)', () => {
    expect(allContent).not.toContain('localStorage')
    expect(allContent).not.toContain('sessionStorage')
  })

  it('document.cookie yok (tüm FP-019)', () => {
    expect(allContent).not.toContain('document.cookie')
  })

  it('eval() yok (tüm FP-019)', () => {
    expect(allContent).not.toMatch(/\beval\s*\(/)
  })
})
