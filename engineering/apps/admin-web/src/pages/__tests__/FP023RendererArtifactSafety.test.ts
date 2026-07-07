import { describe, it, expect } from 'vitest'
import * as fs from 'fs'
import * as path from 'path'

/**
 * FP-023 Task 05: Admin UI Renderer Mode / Artifact Safety Visibility tests.
 * Verifies:
 * - FP-023 types present and safe
 * - Renderer adapter panel renders
 * - Artifact metadata panel renders
 * - Retention/cleanup panel renders
 * - No video player / download / export / publish
 * - No forbidden fields in types
 * - No unsafe HTML
 * - Backward compatibility with FP-019/020/021/022
 */

const typesPath = path.resolve(__dirname, '../../types/index.ts')
const types = fs.readFileSync(typesPath, 'utf-8')

const pagePath = path.resolve(__dirname, '../TurkuVideoWorkflowPage.tsx')
const page = fs.readFileSync(pagePath, 'utf-8')

const apiPath = path.resolve(__dirname, '../../api/turkuVideoApi.ts')
const api = fs.readFileSync(apiPath, 'utf-8')

describe('FP-023 Task 05 — Admin UI Renderer Mode / Artifact Safety', () => {

  // ── A. FP-023 Types ──

  describe('A. FP-023 Types', () => {
    it('TurkuRenderAdapterMode type tanımlı', () => {
      expect(types).toContain('TurkuRenderAdapterMode')
    })

    it('TurkuRenderAdapterFailureCode type tanımlı', () => {
      expect(types).toContain('TurkuRenderAdapterFailureCode')
    })

    it('TurkuRenderAdapterTraceReference interface tanımlı', () => {
      expect(types).toContain('TurkuRenderAdapterTraceReference')
    })

    it('TurkuRenderAdapterSafetyEnvelope interface tanımlı', () => {
      expect(types).toContain('TurkuRenderAdapterSafetyEnvelope')
    })

    it('TurkuFfmpegCommandPlanSummary interface tanımlı', () => {
      expect(types).toContain('TurkuFfmpegCommandPlanSummary')
    })

    it('TurkuRenderArtifactStatus type tanımlı', () => {
      expect(types).toContain('TurkuRenderArtifactStatus')
    })

    it('TurkuRenderArtifactReference interface tanımlı', () => {
      expect(types).toContain('TurkuRenderArtifactReference')
    })

    it('TurkuInternalRenderArtifact interface tanımlı', () => {
      expect(types).toContain('TurkuInternalRenderArtifact')
    })

    it('TurkuRenderArtifactRetentionPolicy interface tanımlı', () => {
      expect(types).toContain('TurkuRenderArtifactRetentionPolicy')
    })

    it('TurkuRenderArtifactCleanupPlan interface tanımlı', () => {
      expect(types).toContain('TurkuRenderArtifactCleanupPlan')
    })

    it('TurkuRenderArtifactSafetyReport interface tanımlı', () => {
      expect(types).toContain('TurkuRenderArtifactSafetyReport')
    })

    it('adapter mode STUB tanımlı', () => {
      expect(types).toContain("'STUB'")
    })

    it('adapter mode FFMPEG_DISABLED tanımlı', () => {
      expect(types).toContain("'FFMPEG_DISABLED'")
    })

    it('adapter mode FFMPEG_READY tanımlı', () => {
      expect(types).toContain("'FFMPEG_READY'")
    })

    it('adapter failure RENDER_PROVIDER_DISABLED tanımlı', () => {
      expect(types).toContain("'RENDER_PROVIDER_DISABLED'")
    })

    it('adapter failure RENDER_SECURITY_VIOLATION tanımlı', () => {
      expect(types).toContain("'RENDER_SECURITY_VIOLATION'")
    })

    it('artifact status VALIDATED tanımlı', () => {
      expect(types).toContain("'VALIDATED'")
    })

    it('artifact status CLEANUP_ELIGIBLE tanımlı', () => {
      expect(types).toContain("'CLEANUP_ELIGIBLE'")
    })

    it('opaqueRef field tanımlı', () => {
      expect(types).toContain('opaqueRef:')
    })

    it('retentionHours field tanımlı', () => {
      expect(types).toContain('retentionHours:')
    })
  })

  // ── B. FP-023 Type Safety ──

  describe('B. FP-023 Type Safety — Forbidden Fields', () => {
    const FORBIDDEN = [
      'token:', 'jwt:', 'session:', 'principal:', 'authorizationHeader:',
      'password:', 'secret:', 'rawRendererPayload:', 'rawProviderPayload:',
      'base64:', 'binary:', 'storageKey:', 'filePath:', 'publicUrl:',
      'signedUrl:', 'cdnUrl:', 'exportUrl:', 'publishUrl:', 'downloadUrl:',
      'renderedVideoUrl:', 'commandLine:', 'shellCommand:', 'processBuilder:',
      'ffmpegCommand:', 'absolutePath:'
    ]

    const fp023Section = types.substring(types.indexOf('FP-023'))

    FORBIDDEN.forEach(field => {
      it(`forbidden field '${field.replace(':', '')}' FP-023 types'ta YOK`, () => {
        expect(fp023Section).not.toContain(field)
      })
    })
  })

  // ── C. Renderer Adapter Panel ──

  describe('C. Renderer Adapter Panel', () => {
    it('renderer-adapter-panel data-testid mevcut', () => {
      expect(page).toContain('renderer-adapter-panel')
    })

    it('renderer-mode-badge data-testid mevcut', () => {
      expect(page).toContain('renderer-mode-badge')
    })

    it('ffmpeg-sandbox-label data-testid mevcut', () => {
      expect(page).toContain('ffmpeg-sandbox-label')
    })

    it('adapter-safety-flags data-testid mevcut', () => {
      expect(page).toContain('adapter-safety-flags')
    })

    it('STUB mode badge render ediyor', () => {
      expect(page).toContain('>STUB<')
    })

    it('NO_REAL_RENDER flag mevcut', () => {
      expect(page).toContain('NO_REAL_RENDER')
    })

    it('NO_SHELL_EXECUTION flag mevcut', () => {
      expect(page).toContain('NO_SHELL_EXECUTION')
    })

    it('Kapalı (Varsayılan) label mevcut', () => {
      expect(page).toContain('Kapalı (Varsayılan)')
    })

    it('Gerçek render kapalı label mevcut', () => {
      expect(page).toContain('Gerçek render kapalı')
    })
  })

  // ── D. Artifact Metadata Panel ──

  describe('D. Artifact Metadata Panel', () => {
    it('artifact-metadata-panel data-testid mevcut', () => {
      expect(page).toContain('artifact-metadata-panel')
    })

    it('artifact-opaque-ref data-testid mevcut', () => {
      expect(page).toContain('artifact-opaque-ref')
    })

    it('artifact-status-label data-testid mevcut', () => {
      expect(page).toContain('artifact-status-label')
    })

    it('Opaque Ref Only label mevcut', () => {
      expect(page).toContain('Opaque Ref Only')
    })

    it('İndirilebilir dosya yok label mevcut', () => {
      expect(page).toContain('İndirilebilir dosya')
    })

    it('Public URL yok label mevcut', () => {
      expect(page).toContain('Public URL')
    })
  })

  // ── E. Retention/Cleanup Panel ──

  describe('E. Retention / Cleanup Panel', () => {
    it('retention-cleanup-panel data-testid mevcut', () => {
      expect(page).toContain('retention-cleanup-panel')
    })

    it('retention-policy-label data-testid mevcut', () => {
      expect(page).toContain('retention-policy-label')
    })

    it('default-24h policy label mevcut', () => {
      expect(page).toContain('default-24h')
    })

    it('24 saat retention mevcut', () => {
      expect(page).toContain('24 saat')
    })

    it('Metadata-only cleanup label mevcut', () => {
      expect(page).toContain('Metadata-only')
    })
  })

  // ── F. Render Adapter Safety Summary ──

  describe('F. Render Adapter Safety Summary', () => {
    it('render-adapter-safety-summary data-testid mevcut', () => {
      expect(page).toContain('render-adapter-safety-summary')
    })

    it('Dahili render güvenliği label mevcut', () => {
      expect(page).toContain('Dahili Render Güvenliği')
    })

    it('Stub/Sandbox render label mevcut', () => {
      expect(page).toContain('Stub/Sandbox render')
    })

    it('Yayın/Dışa Aktarım Yok label mevcut', () => {
      expect(page).toContain('Yayın/Dışa Aktarım Yok')
    })

    it('İndirilebilir dosya yok label mevcut', () => {
      expect(page).toContain('İndirilebilir dosya yok')
    })

    it('Public URL yok label mevcut', () => {
      expect(page).toContain('Public URL yok')
    })
  })

  // ── G. Page Safety — Forbidden Controls ──

  describe('G. Page Safety — Forbidden Controls', () => {
    it('video player YOK', () => {
      expect(page).not.toContain('<video')
      expect(page).not.toContain('<Video')
    })

    it('download button YOK', () => {
      expect(page).not.toContain('btn-download')
      expect(page).not.toContain('download-btn')
      expect(page).not.toContain('İndir</button')
    })

    it('export button YOK', () => {
      expect(page).not.toContain('btn-export')
      expect(page).not.toContain('export-btn')
    })

    it('publish button YOK', () => {
      expect(page).not.toContain('btn-publish')
      expect(page).not.toContain('publish-btn')
      expect(page).not.toContain('Yayınla</button')
    })

    it('dangerouslySetInnerHTML YOK', () => {
      expect(page).not.toContain('dangerouslySetInnerHTML')
    })

    it('innerHTML YOK', () => {
      expect(page).not.toContain('.innerHTML')
    })

    it('publicUrl YOK', () => {
      expect(page).not.toContain('publicUrl')
    })

    it('signedUrl YOK', () => {
      expect(page).not.toContain('signedUrl')
    })

    it('cdnUrl YOK', () => {
      expect(page).not.toContain('cdnUrl')
    })

    it('storageKey YOK', () => {
      expect(page).not.toContain('storageKey')
    })

    it('renderedVideoUrl YOK', () => {
      expect(page).not.toContain('renderedVideoUrl')
    })

    it('commandLine YOK', () => {
      expect(page).not.toContain('commandLine')
    })

    it('shellCommand YOK', () => {
      expect(page).not.toContain('shellCommand')
    })

    it('base64 YOK', () => {
      expect(page).not.toContain('base64')
    })

    it('filePath YOK', () => {
      expect(page).not.toContain('filePath')
    })
  })

  // ── H. API Client Safety ──

  describe('H. API Client Safety', () => {
    it('Authorization header YOK', () => {
      expect(api).not.toContain('Authorization')
    })

    it('Bearer token YOK', () => {
      expect(api).not.toContain('Bearer')
    })

    it('JWT YOK', () => {
      expect(api).not.toContain('jwt')
    })

    it('publish endpoint YOK', () => {
      expect(api).not.toContain('/publish')
    })

    it('download endpoint YOK', () => {
      expect(api).not.toContain('/download')
    })

    it('storageKey YOK', () => {
      expect(api).not.toContain('storageKey')
    })
  })

  // ── I. Backward Compatibility ──

  describe('I. Backward Compatibility', () => {
    // FP-019 elements
    it('progress-panel data-testid mevcut', () => {
      expect(page).toContain('progress-panel')
    })

    it('progress-summary data-testid mevcut', () => {
      expect(page).toContain('progress-summary')
    })

    // FP-021 elements
    it('composition-plan data-testid mevcut', () => {
      expect(page).toContain('composition-plan')
    })

    it('readiness-panel data-testid mevcut', () => {
      expect(page).toContain('readiness-panel')
    })

    // FP-022 elements
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
  })
})
