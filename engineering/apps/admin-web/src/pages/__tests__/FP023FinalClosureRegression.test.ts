import { describe, it, expect } from 'vitest'
import * as fs from 'fs'
import * as path from 'path'

/**
 * FP-023 Task 06: E2E Frontend Regression — Final Closure.
 */

const typesPath = path.resolve(__dirname, '../../types/index.ts')
const types = fs.readFileSync(typesPath, 'utf-8')

const pagePath = path.resolve(__dirname, '../TurkuVideoWorkflowPage.tsx')
const page = fs.readFileSync(pagePath, 'utf-8')

const apiPath = path.resolve(__dirname, '../../api/turkuVideoApi.ts')
const api = fs.readFileSync(apiPath, 'utf-8')

describe('FP-023 Task 06 — E2E Frontend Regression', () => {

  // ── A. Complete Type Inventory ──

  describe('A. FP-023 Types Complete', () => {
    const FP023_TYPES = [
      'TurkuRenderAdapterMode', 'TurkuRenderAdapterFailureCode',
      'TurkuRenderAdapterTraceReference', 'TurkuRenderAdapterSafetyEnvelope',
      'TurkuFfmpegCommandPlanSummary', 'TurkuRenderArtifactStatus',
      'TurkuRenderArtifactReference', 'TurkuInternalRenderArtifact',
      'TurkuRenderArtifactRetentionPolicy', 'TurkuRenderArtifactCleanupPlan',
      'TurkuRenderArtifactSafetyReport'
    ]

    FP023_TYPES.forEach(typeName => {
      it(`${typeName} tanımlı`, () => {
        expect(types).toContain(typeName)
      })
    })
  })

  // ── B. Adapter Mode Values ──

  describe('B. Adapter Mode Values', () => {
    const MODES = ['STUB', 'FFMPEG_DISABLED', 'FFMPEG_CONFIG_MISSING', 'FFMPEG_READY', 'FFMPEG_FAILED_SAFE']
    MODES.forEach(mode => {
      it(`adapter mode '${mode}' tanımlı`, () => {
        expect(types).toContain(`'${mode}'`)
      })
    })
  })

  // ── C. Adapter Failure Codes ──

  describe('C. Adapter Failure Codes', () => {
    const CODES = [
      'RENDER_PROVIDER_DISABLED', 'RENDER_CONFIG_MISSING', 'RENDER_INPUT_MISSING',
      'RENDER_TIMEOUT', 'RENDER_PROCESS_FAILED', 'RENDER_OUTPUT_REJECTED',
      'RENDER_SECURITY_VIOLATION', 'RENDER_FAILED_SAFE'
    ]
    CODES.forEach(code => {
      it(`failure code '${code}' tanımlı`, () => {
        expect(types).toContain(`'${code}'`)
      })
    })
  })

  // ── D. Artifact Status Values ──

  describe('D. Artifact Status Values', () => {
    const STATUSES = ['CREATED', 'VALIDATED', 'RETENTION_ACTIVE', 'CLEANUP_ELIGIBLE',
      'CLEANUP_PLANNED', 'CLEANED', 'REJECTED']
    STATUSES.forEach(status => {
      it(`artifact status '${status}' tanımlı`, () => {
        expect(types).toContain(`'${status}'`)
      })
    })
  })

  // ── E. Type Safety — Forbidden Fields ──

  describe('E. Type Safety — FP-023 Forbidden Fields', () => {
    const FORBIDDEN = [
      'token:', 'jwt:', 'session:', 'principal:', 'authorizationHeader:',
      'password:', 'secret:', 'rawRendererPayload:', 'rawProviderPayload:',
      'base64:', 'binary:', 'storageKey:', 'filePath:', 'publicUrl:',
      'signedUrl:', 'cdnUrl:', 'exportUrl:', 'publishUrl:', 'downloadUrl:',
      'renderedVideoUrl:', 'commandLine:', 'shellCommand:'
    ]

    const fp023Section = types.substring(types.indexOf('FP-023'))

    FORBIDDEN.forEach(field => {
      it(`forbidden '${field.replace(':', '')}' YOK`, () => {
        expect(fp023Section).not.toContain(field)
      })
    })
  })

  // ── F. UI Panels Render ──

  describe('F. UI Panels Render', () => {
    it('renderer-adapter-panel', () => expect(page).toContain('renderer-adapter-panel'))
    it('renderer-mode-badge', () => expect(page).toContain('renderer-mode-badge'))
    it('ffmpeg-sandbox-label', () => expect(page).toContain('ffmpeg-sandbox-label'))
    it('adapter-safety-flags', () => expect(page).toContain('adapter-safety-flags'))
    it('artifact-metadata-panel', () => expect(page).toContain('artifact-metadata-panel'))
    it('artifact-opaque-ref', () => expect(page).toContain('artifact-opaque-ref'))
    it('artifact-status-label', () => expect(page).toContain('artifact-status-label'))
    it('retention-cleanup-panel', () => expect(page).toContain('retention-cleanup-panel'))
    it('retention-policy-label', () => expect(page).toContain('retention-policy-label'))
    it('render-adapter-safety-summary', () => expect(page).toContain('render-adapter-safety-summary'))
  })

  // ── G. Turkish Safety Labels ──

  describe('G. Turkish Safety Labels', () => {
    it('Dahili Render Güvenliği', () => expect(page).toContain('Dahili Render Güvenliği'))
    it('Stub/Sandbox render', () => expect(page).toContain('Stub/Sandbox render'))
    it('Yayın/Dışa Aktarım Yok', () => expect(page).toContain('Yayın/Dışa Aktarım Yok'))
    it('İndirilebilir dosya yok', () => expect(page).toContain('İndirilebilir dosya yok'))
    it('Public URL yok', () => expect(page).toContain('Public URL yok'))
    it('Gerçek render kapalı', () => expect(page).toContain('Gerçek render kapalı'))
    it('Kapalı (Varsayılan)', () => expect(page).toContain('Kapalı (Varsayılan)'))
    it('default-24h', () => expect(page).toContain('default-24h'))
    it('Metadata-only', () => expect(page).toContain('Metadata-only'))
  })

  // ── H. Forbidden Page Elements ──

  describe('H. Forbidden Page Elements', () => {
    it('no <video>', () => {
      expect(page).not.toContain('<video')
      expect(page).not.toContain('<Video')
    })
    it('no download button', () => {
      expect(page).not.toContain('btn-download')
      expect(page).not.toContain('download-btn')
    })
    it('no export button', () => {
      expect(page).not.toContain('btn-export')
      expect(page).not.toContain('export-btn')
    })
    it('no publish button', () => {
      expect(page).not.toContain('btn-publish')
      expect(page).not.toContain('publish-btn')
    })
    it('no dangerouslySetInnerHTML', () => expect(page).not.toContain('dangerouslySetInnerHTML'))
    it('no innerHTML', () => expect(page).not.toContain('.innerHTML'))
    it('no publicUrl', () => expect(page).not.toContain('publicUrl'))
    it('no signedUrl', () => expect(page).not.toContain('signedUrl'))
    it('no cdnUrl', () => expect(page).not.toContain('cdnUrl'))
    it('no storageKey', () => expect(page).not.toContain('storageKey'))
    it('no renderedVideoUrl', () => expect(page).not.toContain('renderedVideoUrl'))
    it('no commandLine', () => expect(page).not.toContain('commandLine'))
    it('no shellCommand', () => expect(page).not.toContain('shellCommand'))
    it('no base64', () => expect(page).not.toContain('base64'))
    it('no filePath', () => expect(page).not.toContain('filePath'))
  })

  // ── I. API Client Safety ──

  describe('I. API Client Safety', () => {
    it('no Authorization header', () => expect(api).not.toContain('Authorization'))
    it('no Bearer token', () => expect(api).not.toContain('Bearer'))
    it('no JWT', () => expect(api).not.toContain('jwt'))
    it('no /publish', () => expect(api).not.toContain('/publish'))
    it('no /download', () => expect(api).not.toContain('/download'))
    it('no /export', () => expect(api).not.toContain('/export'))

    it('API export count', () => {
      const exportMatches = api.match(/export\s+(async\s+)?function/g) || []
      expect(exportMatches.length).toBeGreaterThanOrEqual(20)
      expect(exportMatches.length).toBeLessThanOrEqual(25)
    })
  })

  // ── J. FP-019/020/021/022 Backward Compatibility ──

  describe('J. Backward Compatibility', () => {
    // FP-019
    it('progress-panel', () => expect(page).toContain('progress-panel'))
    it('btn-bootstrap', () => expect(page).toContain('btn-bootstrap'))

    // FP-020
    it('provider-mode-badge', () => expect(page).toContain('provider-mode-badge'))
    it('image-batch', () => expect(page).toContain('image-batch'))

    // FP-021
    it('composition-plan', () => expect(page).toContain('composition-plan'))
    it('readiness-panel', () => expect(page).toContain('readiness-panel'))

    // FP-022
    it('render-execution-section', () => expect(page).toContain('render-execution-section'))
    it('render-job-status-panel', () => expect(page).toContain('render-job-status-panel'))
    it('render-execution-safety-panel', () => expect(page).toContain('render-execution-safety-panel'))
    it('render-safety-summary', () => expect(page).toContain('render-safety-summary'))
  })
})
