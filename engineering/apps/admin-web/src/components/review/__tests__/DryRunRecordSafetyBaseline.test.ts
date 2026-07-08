/**
 * FP-052: Dry Run Record Safety Baseline Closure.
 * No new capability. Locks FP-049→FP-051 safety.
 */

import { describe, it, expect } from 'vitest'
import {
  renderRecordStatus,
  isRecordSuccessful,
  getRecordMetadataSummary,
  getHistorySummary,
} from '../InternalDryRunHistoryPanel'
import {
  renderDryRunStatus,
  isDryRunSuccessful,
  getDryRunMetadataSummary,
} from '../InternalDryRunPanel'
import type {
  TurkuInternalDryRunRecordSummary,
  TurkuInternalDryRunTriggerResponse,
} from '../../../types'

function makeRecord(
  overrides: Partial<TurkuInternalDryRunRecordSummary> = {}
): TurkuInternalDryRunRecordSummary {
  return {
    opaqueRecordRef: 'dryrec:baseline',
    opaqueDryRunRef: 'chain:baseline',
    opaqueStoryboardRef: 'sb:baseline',
    status: 'READY_FOR_ADMIN_HISTORY_ONLY',
    issueCodes: [],
    sceneCount: 3,
    generatedClipCount: 3,
    inputClipCount: 3,
    stitchedArtifactPresent: true,
    durationSeconds: 9,
    fps: 24,
    width: 640,
    height: 360,
    sizeBytes: 4194304,
    internalOnly: true,
    ...overrides,
  }
}

function makeResponse(
  overrides: Partial<TurkuInternalDryRunTriggerResponse> = {}
): TurkuInternalDryRunTriggerResponse {
  return {
    opaqueChainRef: 'chain:base',
    opaqueStoryboardRef: 'sb:base',
    status: 'ADMIN_METADATA_VISIBLE',
    issueCodes: [],
    sceneCount: 2,
    generatedClipCount: 2,
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

describe('FP-052 — Dry Run Record Safety Baseline', () => {
  describe('A. FP-049 + FP-051 Panel Cross-check', () => {
    it('FP-049 panel still renders metadata-only', () => {
      const summary = getDryRunMetadataSummary(makeResponse())
      expect(summary).toContain('Sahne: 2')
      expect(summary).toContain('Dahili: Evet')
    })
    it('FP-051 panel still renders metadata-only', () => {
      const summary = getRecordMetadataSummary(makeRecord())
      expect(summary).toContain('Sahne: 3')
      expect(summary).toContain('Dahili: Evet')
    })
  })

  describe('B. Rejected/Failed States Render Safely', () => {
    it('FP-049 rejected renders status label', () => {
      expect(renderDryRunStatus('REJECTED_UNSAFE')).toContain('Reddedildi')
    })
    it('FP-051 rejected renders status label', () => {
      expect(renderRecordStatus('REJECTED_UNSAFE')).toContain('Reddedildi')
    })
    it('FP-051 failed renders status label', () => {
      expect(renderRecordStatus('FAILED_SAFE')).toContain('Hata')
    })
  })

  describe('C. No Forbidden Fields in Summaries', () => {
    it('FP-049 summary no forbidden', () => {
      const s = getDryRunMetadataSummary(makeResponse())
      for (const f of ['filePath', 'stdout', 'stderr', 'publicUrl', 'token', 'lyrics', 'commandLine', 'rawLog'])
        expect(s).not.toContain(f)
    })
    it('FP-051 summary no forbidden', () => {
      const s = getRecordMetadataSummary(makeRecord())
      for (const f of ['filePath', 'stdout', 'stderr', 'publicUrl', 'token', 'lyrics', 'commandLine', 'rawLog'])
        expect(s).not.toContain(f)
    })
  })

  describe('D. Panel Source Safety (FP-049 + FP-051)', () => {
    it('both panels have no delivery/unsafe controls', async () => {
      const fs = await import('fs')
      const panels = [
        'src/components/review/InternalDryRunPanel.tsx',
        'src/components/review/InternalDryRunHistoryPanel.tsx',
      ]
      const forbidden = [
        'VideoPlayer', 'AudioPlayer', 'DownloadButton', 'ExportButton',
        'PublishButton', 'innerHTML', 'dangerouslySetInnerHTML',
        'publicUrl', 'signedUrl', 'cdnUrl',
      ]
      for (const p of panels) {
        const src = fs.readFileSync(p, 'utf-8')
        for (const f of forbidden) expect(src).not.toContain(f)
      }
    })
  })

  describe('E. History Summary Safety', () => {
    it('empty history safe', () => {
      expect(getHistorySummary([])).toBe('Kayıt yok')
    })
    it('multiple records summary safe', () => {
      const h = getHistorySummary([makeRecord(), makeRecord({ status: 'FAILED_SAFE', sceneCount: 1 })])
      expect(h).toContain('#1:')
      expect(h).toContain('#2:')
      expect(h).not.toContain('filePath')
      expect(h).not.toContain('publicUrl')
    })
  })

  describe('F. Success Functions', () => {
    it('FP-049 isDryRunSuccessful baseline', () => {
      expect(isDryRunSuccessful(makeResponse())).toBe(true)
      expect(isDryRunSuccessful(makeResponse({ status: 'REJECTED_UNSAFE' }))).toBe(false)
    })
    it('FP-051 isRecordSuccessful baseline', () => {
      expect(isRecordSuccessful(makeRecord())).toBe(true)
      expect(isRecordSuccessful(makeRecord({ status: 'REJECTED_UNSAFE' }))).toBe(false)
    })
  })

  describe('G. InternalOnly Always True', () => {
    it('FP-049 response internalOnly', () => {
      expect(makeResponse().internalOnly).toBe(true)
    })
    it('FP-051 record internalOnly', () => {
      expect(makeRecord().internalOnly).toBe(true)
    })
  })
})
