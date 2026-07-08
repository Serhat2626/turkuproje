/**
 * FP-050: Admin Internal Dry Run Endpoint & UI Safety Baseline Closure.
 * No new capability. Regression hardening only.
 */

import { describe, it, expect } from 'vitest'
import {
  renderDryRunStatus,
  isDryRunSuccessful,
  getDryRunMetadataSummary,
} from '../InternalDryRunPanel'
import type { TurkuInternalDryRunTriggerResponse } from '../../../types'

function makeResponse(
  overrides: Partial<TurkuInternalDryRunTriggerResponse> = {}
): TurkuInternalDryRunTriggerResponse {
  return {
    opaqueChainRef: 'chain:baseline',
    opaqueStoryboardRef: 'sb:baseline',
    status: 'ADMIN_METADATA_VISIBLE',
    issueCodes: [],
    sceneCount: 3,
    generatedClipCount: 3,
    stitchedArtifactPresent: true,
    reviewPackageStatus: 'READY_FOR_INTERNAL_REVIEW_ONLY',
    adminMetadataVisible: true,
    totalDurationSeconds: 6,
    maxWidth: 640,
    maxHeight: 360,
    maxFps: 24,
    totalSizeBytes: 3145728,
    internalOnly: true,
    ...overrides,
  }
}

describe('FP-050 — Dry Run Safety Baseline', () => {
  describe('A. Metadata-only Rendering', () => {
    it('successful response renders metadata summary', () => {
      const summary = getDryRunMetadataSummary(makeResponse())
      expect(summary).toContain('Sahne: 3')
      expect(summary).toContain('Klip: 3')
      expect(summary).toContain('640×360')
      expect(summary).toContain('Dahili: Evet')
    })
    it('rejected response renders REJECTED_UNSAFE status', () => {
      const label = renderDryRunStatus('REJECTED_UNSAFE')
      expect(label).toBe('Güvensiz — Reddedildi')
    })
    it('failed response renders FAILED_SAFE status', () => {
      const label = renderDryRunStatus('FAILED_SAFE')
      expect(label).toBe('Güvenli Hata')
    })
  })

  describe('B. Issue Codes Rendering', () => {
    it('issue codes shown in summary', () => {
      const summary = getDryRunMetadataSummary(
        makeResponse({ issueCodes: ['CLIP_GENERATION_FAILED', 'STITCHING_FAILED'] })
      )
      expect(summary).toContain('CLIP_GENERATION_FAILED')
      expect(summary).toContain('STITCHING_FAILED')
    })
    it('no issue codes → no Sorunlar line', () => {
      const summary = getDryRunMetadataSummary(makeResponse({ issueCodes: [] }))
      expect(summary).not.toContain('Sorunlar:')
    })
  })

  describe('C. No Forbidden Controls', () => {
    it('summary has no forbidden fields', () => {
      const summary = getDryRunMetadataSummary(makeResponse())
      const forbidden = [
        'filePath', 'absolutePath', 'inputPath', 'outputPath', 'workspacePath',
        'stdout', 'stderr', 'commandLine', 'rawLog',
        'storageKey', 'publicUrl', 'signedUrl', 'cdnUrl',
        'base64', 'downloadUrl', 'publishUrl',
        'token', 'JWT', 'session', 'principal',
        'lyrics', 'scriptBody', 'promptText',
      ]
      for (const f of forbidden) {
        expect(summary).not.toContain(f)
      }
    })
  })

  describe('D. No Player/Download/Export/Publish in Panel', () => {
    it('panel source has no delivery controls', async () => {
      const fs = await import('fs')
      const source = fs.readFileSync(
        'src/components/review/InternalDryRunPanel.tsx',
        'utf-8'
      )
      const forbidden = [
        'VideoPlayer', 'AudioPlayer', 'DownloadButton', 'ExportButton',
        'PublishButton', 'PreviewPlayer', 'StreamPlayer',
        'publicUrl', 'signedUrl', 'cdnUrl', 'downloadUrl',
        'innerHTML', 'dangerouslySetInnerHTML',
      ]
      for (const f of forbidden) {
        expect(source).not.toContain(f)
      }
    })
  })

  describe('E. Türkçe Character Preservation', () => {
    it('status labels contain correct Türkçe', () => {
      expect(renderDryRunStatus('CREATED_INTERNAL_ONLY')).toContain('Oluşturuldu')
      expect(renderDryRunStatus('STORYBOARD_ACCEPTED')).toContain('Kabul')
      expect(renderDryRunStatus('ADMIN_METADATA_VISIBLE')).toContain('Görünür')
    })
    it('summary uses Türkçe labels', () => {
      const summary = getDryRunMetadataSummary(makeResponse())
      expect(summary).toContain('Süre:')
      expect(summary).toContain('Çözünürlük:')
      expect(summary).toContain('Boyut:')
    })
  })

  describe('F. API Export Safety', () => {
    it('turkuVideoApi has exactly 25 exports', async () => {
      const api = await import('../../../api/turkuVideoApi')
      expect(Object.keys(api)).toHaveLength(25)
    })
    it('no publish/export/download function in API', async () => {
      const api = await import('../../../api/turkuVideoApi')
      const names = Object.keys(api)
      expect(names).not.toContain('publishVideo')
      expect(names).not.toContain('exportVideo')
      expect(names).not.toContain('downloadVideo')
    })
  })

  describe('G. Response Type Baseline', () => {
    it('internalOnly is always true', () => {
      expect(makeResponse().internalOnly).toBe(true)
    })
    it('isDryRunSuccessful checks ADMIN_METADATA_VISIBLE', () => {
      expect(isDryRunSuccessful(makeResponse())).toBe(true)
      expect(isDryRunSuccessful(makeResponse({ status: 'FAILED_SAFE' }))).toBe(false)
      expect(isDryRunSuccessful(makeResponse({ status: 'REJECTED_UNSAFE' }))).toBe(false)
    })
  })
})
