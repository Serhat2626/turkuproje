import { describe, it, expect } from 'vitest'
import * as fs from 'fs'
import * as path from 'path'
import * as turkuVideoApi from '../../api/turkuVideoApi'

/**
 * FP-020 Task 06: E2E Provider Safety Regression + Final Closure — frontend.
 *
 * Kontrol noktaları:
 * - FP-020 types, API, UI tüm bileşenleri mevcut
 * - Provider mode badge + safety panel + generate actions
 * - Güvenlik taraması: yasaklı alanlar, unsafe HTML, auth headers yok
 * - FP-019 uyumluluğu korunuyor
 * - Route ve sidebar hala çalışıyor
 */

const SRC_DIR = path.resolve(__dirname, '../../')

function readFile(relativePath: string): string {
  return fs.readFileSync(path.resolve(SRC_DIR, relativePath), 'utf-8')
}

// ── A. FP-020 Types Regression ──────────────────────────

describe('FP-020 Final Closure — Types', () => {
  const types = readFile('types/index.ts')

  const REQUIRED_TYPES = [
    'TurkuProviderMode',
    'TurkuProviderFailureCode',
    'TurkuProviderTraceReference',
    'TurkuGenerationSafetyResponse',
    'TurkuProviderPlanningGenerationResponse',
    'TurkuPlanningBundleGenerationResponse',
    'TurkuImageBatchRunResponse',
    'TurkuImageBatchRunSceneStatus',
    'TurkuProviderGenerationRequest',
    'TurkuImageBatchRunRequest',
  ]

  for (const t of REQUIRED_TYPES) {
    it(`${t} tanımlı`, () => {
      expect(types).toContain(t)
    })
  }

  const REQUIRED_MODES = [
    'DETERMINISTIC_STUB',
    'PROVIDER_DRAFT',
    'PROVIDER_DISABLED',
    'PROVIDER_FAILED_SAFE',
    'HUMAN_REVIEW_REQUIRED',
  ]

  for (const m of REQUIRED_MODES) {
    it(`provider mode '${m}' tanımlı`, () => {
      expect(types).toContain(m)
    })
  }

  it('FP-019 types hala mevcut', () => {
    expect(types).toContain('TurkuVideoWorkflowStage')
    expect(types).toContain('TurkuVideoBootstrapRequest')
    expect(types).toContain('TurkuVideoProjectResponse')
    expect(types).toContain('TurkuPreparationResponse')
    expect(types).toContain('TurkuSceneImageBatch')
  })

  // Forbidden fields
  const FORBIDDEN = [
    'token:', 'jwt:', 'session:', 'credential:', 'password:', 'secret:',
    'apiKey:', 'authorizationHeader:', 'publicUrl:', 'signedUrl:', 'cdnUrl:',
    'storageKey:', 'base64:', 'exportUrl:', 'publishUrl:', 'downloadUrl:',
    'rawProviderPayload:', 'rawProviderResponse:', 'rawPrompt:',
    'imageData:', 'binaryData:', 'videoData:', 'audioData:',
  ]

  for (const field of FORBIDDEN) {
    it(`FP-020 types bloğunda '${field}' YOK`, () => {
      const fp020Block = types.substring(types.indexOf('FP-020'))
      expect(fp020Block).not.toContain(field)
    })
  }
})

// ── B. FP-020 API Client Regression ─────────────────────

describe('FP-020 Final Closure — API Client', () => {
  const api = readFile('api/turkuVideoApi.ts')

  const FP020_METHODS = [
    'generateTurkuStory',
    'generateTurkuScenario',
    'generateTurkuStoryboard',
    'generateTurkuPlanningBundle',
    'runTurkuImageBatch',
    'getTurkuGenerationSafety',
  ]

  for (const m of FP020_METHODS) {
    it(`${m} fonksiyonu mevcut`, () => {
      expect(typeof (turkuVideoApi as Record<string, unknown>)[m]).toBe('function')
    })
  }

  const FP019_METHODS = [
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

  for (const m of FP019_METHODS) {
    it(`FP-019 ${m} hala mevcut`, () => {
      expect(typeof (turkuVideoApi as Record<string, unknown>)[m]).toBe('function')
    })
  }

  it('toplam 15 export fonksiyonu var', () => {
    expect(Object.keys(turkuVideoApi)).toHaveLength(19)
  })

  it('Authorization header göndermiyor', () => {
    expect(api).not.toContain('Authorization')
    expect(api).not.toContain('Bearer')
  })

  it('publish/export/download endpoint YOK', () => {
    expect(api).not.toContain('/publish')
    expect(api).not.toContain('/export')
    expect(api).not.toContain('/download')
  })

  it('base64/storage/provider field YOK', () => {
    expect(api).not.toContain('base64')
    expect(api).not.toContain('storageKey')
    expect(api).not.toContain('signedUrl')
    expect(api).not.toContain('cdnUrl')
    expect(api).not.toContain('publicUrl')
  })

  it('generate-story endpoint doğru', () => {
    expect(api).toContain('generate-story')
  })

  it('generate-scenario endpoint doğru', () => {
    expect(api).toContain('generate-scenario')
  })

  it('generate-storyboard endpoint doğru', () => {
    expect(api).toContain('generate-storyboard')
  })

  it('generate-planning-bundle endpoint doğru', () => {
    expect(api).toContain('generate-planning-bundle')
  })

  it('run-image-batch endpoint doğru', () => {
    expect(api).toContain('run-image-batch')
  })

  it('generation-safety endpoint doğru', () => {
    expect(api).toContain('generation-safety')
  })
})

// ── C. FP-020 UI Regression ─────────────────────────────

describe('FP-020 Final Closure — UI', () => {
  const page = readFile('pages/TurkuVideoWorkflowPage.tsx')

  // Provider mode badge
  it('provider mode badge render ediliyor', () => {
    expect(page).toContain('provider-mode-badge')
  })

  it('PROVIDER_MODE_LABELS tanımlı', () => {
    expect(page).toContain('PROVIDER_MODE_LABELS')
  })

  it('PROVIDER_MODE_BADGE_CLASS tanımlı', () => {
    expect(page).toContain('PROVIDER_MODE_BADGE_CLASS')
  })

  // Safety panel
  it('safety panel mevcut', () => {
    expect(page).toContain('safety-panel')
  })

  it('safety-provider-mode gösterilir', () => {
    expect(page).toContain('safety-provider-mode')
  })

  it('safety-generation-available gösterilir', () => {
    expect(page).toContain('safety-generation-available')
  })

  it('safety-human-review gösterilir', () => {
    expect(page).toContain('safety-human-review')
  })

  it('safety-auto-approved gösterilir', () => {
    expect(page).toContain('safety-auto-approved')
  })

  it('safety-copyright-review gösterilir', () => {
    expect(page).toContain('safety-copyright-review')
  })

  it('safety-lyrics gösterilir', () => {
    expect(page).toContain('safety-lyrics')
  })

  it('safety-blocked-alert mevcut', () => {
    expect(page).toContain('safety-blocked-alert')
  })

  it('safety-failure-code gösterilir', () => {
    expect(page).toContain('safety-failure-code')
  })

  it('safety-review-flags gösterilir', () => {
    expect(page).toContain('safety-review-flags')
  })

  it('safety-warnings gösterilir', () => {
    expect(page).toContain('safety-warnings')
  })

  it('safety-trace-ref gösterilir', () => {
    expect(page).toContain('safety-trace-ref')
  })

  // Generate actions
  it('generate-actions paneli mevcut', () => {
    expect(page).toContain('generate-actions')
  })

  it('btn-generate-story mevcut', () => {
    expect(page).toContain('btn-generate-story')
  })

  it('btn-generate-scenario mevcut', () => {
    expect(page).toContain('btn-generate-scenario')
  })

  it('btn-generate-storyboard mevcut', () => {
    expect(page).toContain('btn-generate-storyboard')
  })

  it('btn-generate-planning-bundle mevcut', () => {
    expect(page).toContain('btn-generate-planning-bundle')
  })

  it('btn-run-image-batch mevcut', () => {
    expect(page).toContain('btn-run-image-batch')
  })

  it('generate-blocked-alert mevcut', () => {
    expect(page).toContain('generate-blocked-alert')
  })

  // API calls on page
  it('generateTurkuStory API çağrısı sayfada', () => {
    expect(page).toContain('generateTurkuStory')
  })

  it('generateTurkuScenario API çağrısı sayfada', () => {
    expect(page).toContain('generateTurkuScenario')
  })

  it('generateTurkuStoryboard API çağrısı sayfada', () => {
    expect(page).toContain('generateTurkuStoryboard')
  })

  it('generateTurkuPlanningBundle API çağrısı sayfada', () => {
    expect(page).toContain('generateTurkuPlanningBundle')
  })

  it('runTurkuImageBatch API çağrısı sayfada', () => {
    expect(page).toContain('runTurkuImageBatch')
  })

  it('getTurkuGenerationSafety API çağrısı sayfada', () => {
    expect(page).toContain('getTurkuGenerationSafety')
  })

  // FP-019 compatibility
  it('bootstrap form hala mevcut', () => {
    expect(page).toContain('bootstrap-form')
  })

  it('project summary hala mevcut', () => {
    expect(page).toContain('project-summary')
  })

  it('stage actions hala mevcut', () => {
    expect(page).toContain('stage-actions')
  })

  it('internal preview hala mevcut', () => {
    expect(page).toContain('internal-preview')
  })

  it('progress panel hala mevcut', () => {
    expect(page).toContain('progress-panel')
  })
})

// ── D. Full Source Safety Scan ───────────────────────────

describe('FP-020 Final Closure — Source Safety', () => {
  const page = readFile('pages/TurkuVideoWorkflowPage.tsx')
  const api = readFile('api/turkuVideoApi.ts')
  const types = readFile('types/index.ts')

  // Unsafe HTML
  it('dangerouslySetInnerHTML page YOK', () => {
    expect(page).not.toContain('dangerouslySetInnerHTML')
  })

  it('innerHTML page YOK', () => {
    expect(page).not.toContain('innerHTML')
  })

  // Auth
  it('Authorization header page YOK', () => {
    expect(page).not.toContain('Authorization')
  })

  it('Bearer page YOK', () => {
    expect(page).not.toContain('Bearer')
  })

  it('JWT/token/session/principal page YOK', () => {
    expect(page).not.toContain('JWT')
    expect(page).not.toContain('token')
    expect(page).not.toContain('session')
    expect(page).not.toContain('principal')
  })

  // Publish/export
  it('publish kontrolü YOK', () => {
    expect(page).not.toContain('btn-publish')
    expect(page).not.toContain('publishVideo')
  })

  it('export kontrolü YOK', () => {
    expect(page).not.toContain('btn-export')
    expect(page).not.toContain('exportVideo')
  })

  it('download kontrolü YOK', () => {
    expect(page).not.toContain('btn-download')
    expect(page).not.toContain('downloadVideo')
  })

  // Raw data
  it('rawProvider/rawPayload page YOK', () => {
    expect(page).not.toContain('rawProvider')
    expect(page).not.toContain('rawPayload')
  })

  it('base64 page YOK', () => {
    expect(page).not.toContain('base64')
  })

  it('publicUrl/signedUrl/cdnUrl page YOK', () => {
    expect(page).not.toContain('publicUrl')
    expect(page).not.toContain('signedUrl')
    expect(page).not.toContain('cdnUrl')
  })

  it('storageKey page YOK', () => {
    expect(page).not.toContain('storageKey')
  })

  // API forbidden
  it('API localStorage/sessionStorage/cookie YOK', () => {
    expect(api).not.toContain('localStorage')
    expect(api).not.toContain('sessionStorage')
    expect(api).not.toContain('document.cookie')
  })

  // eval
  it('eval() page YOK', () => {
    expect(page).not.toContain('eval(')
  })

  // actorRole not shown as production identity
  it('actorRole production auth olarak gösterilmiyor', () => {
    expect(page).not.toContain('Kullanıcı Kimliği')
    expect(page).not.toContain('Oturum Açan')
    expect(page).not.toContain('Giriş Yapan')
  })
})

// ── E. Route & Sidebar Regression ───────────────────────

describe('FP-020 Final Closure — Route & Sidebar', () => {
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

  it('App.tsx publish route YOK', () => {
    expect(app).not.toContain('publish')
  })

  it('Sidebar export/download nav link YOK', () => {
    expect(sidebar).not.toContain('Dışa Aktar')
    expect(sidebar).not.toContain('/download')
    expect(sidebar).not.toContain('/export')
  })
})
