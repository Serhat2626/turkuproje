/**
 * FP-049: Internal Dry Run Panel tests.
 * No player/preview/download/export/publish controls.
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
    opaqueChainRef: 'chain:test',
    opaqueStoryboardRef: 'sb:test',
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

describe('FP-049 — Internal Dry Run Panel', () => {
  describe('A. Status Labels', () => {
    it('renders ADMIN_METADATA_VISIBLE', () => {
      expect(renderDryRunStatus('ADMIN_METADATA_VISIBLE')).toBe('Admin Metadata Görünür')
    })
    it('renders REJECTED_UNSAFE', () => {
      expect(renderDryRunStatus('REJECTED_UNSAFE')).toBe('Güvensiz — Reddedildi')
    })
    it('renders FAILED_SAFE', () => {
      expect(renderDryRunStatus('FAILED_SAFE')).toBe('Güvenli Hata')
    })
  })

  describe('B. Success Check', () => {
    it('successful when ADMIN_METADATA_VISIBLE + internalOnly', () => {
      expect(isDryRunSuccessful(makeResponse())).toBe(true)
    })
    it('not successful when REJECTED_UNSAFE', () => {
      expect(isDryRunSuccessful(makeResponse({ status: 'REJECTED_UNSAFE' }))).toBe(false)
    })
    it('not successful when FAILED_SAFE', () => {
      expect(isDryRunSuccessful(makeResponse({ status: 'FAILED_SAFE' }))).toBe(false)
    })
  })

  describe('C. Metadata Summary', () => {
    it('contains safe metadata', () => {
      const summary = getDryRunMetadataSummary(makeResponse())
      expect(summary).toContain('Sahne: 3')
      expect(summary).toContain('Klip: 3')
      expect(summary).toContain('640×360')
      expect(summary).toContain('Dahili: Evet')
    })
    it('shows issue codes when present', () => {
      const summary = getDryRunMetadataSummary(
        makeResponse({ issueCodes: ['ADAPTER_FAILED'] })
      )
      expect(summary).toContain('ADAPTER_FAILED')
    })
  })

  describe('D. No Forbidden Controls', () => {
    it('summary has no forbidden fields', () => {
      const summary = getDryRunMetadataSummary(makeResponse())
      const forbidden = [
        'filePath', 'absolutePath', 'stdout', 'stderr', 'commandLine',
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

  describe('E. No Player/Download/Export/Publish', () => {
    it('component source has no player/download/export/publish controls', async () => {
      const fs = await import('fs')
      const source = fs.readFileSync(
        'src/components/review/InternalDryRunPanel.tsx',
        'utf-8'
      )
      expect(source).not.toContain('VideoPlayer')
      expect(source).not.toContain('DownloadButton')
      expect(source).not.toContain('ExportButton')
      expect(source).not.toContain('PublishButton')
      expect(source).not.toContain('PreviewPlayer')
      expect(source).not.toContain('publicUrl')
      expect(source).not.toContain('signedUrl')
      expect(source).not.toContain('cdnUrl')
    })
  })

  describe('F. Response Type Safety', () => {
    it('response has internalOnly=true', () => {
      expect(makeResponse().internalOnly).toBe(true)
    })
    it('10 chain statuses defined', () => {
      const statuses: string[] = [
        'CREATED_INTERNAL_ONLY', 'STORYBOARD_ACCEPTED', 'SCENE_PLANS_CREATED',
        'MULTI_SCENE_JOB_READY', 'SYNTHETIC_CLIPS_GENERATED',
        'STITCHED_ARTIFACT_VERIFIED', 'REVIEW_PACKAGE_READY_INTERNAL_ONLY',
        'ADMIN_METADATA_VISIBLE', 'REJECTED_UNSAFE', 'FAILED_SAFE',
      ]
      expect(statuses).toHaveLength(10)
      for (const s of statuses) {
        expect(renderDryRunStatus(s as any)).toBeTruthy()
      }
    })
  })
})
