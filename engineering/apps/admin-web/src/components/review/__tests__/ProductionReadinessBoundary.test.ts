/**
 * FP-054: Final Production Readiness Boundary — Admin UI Safety.
 * Dense cross-panel sweep across FP-049→FP-053 panels.
 */

import { describe, it, expect } from 'vitest'
import { renderDryRunStatus, isDryRunSuccessful, getDryRunMetadataSummary } from '../InternalDryRunPanel'
import { renderRecordStatus, isRecordSuccessful, getRecordMetadataSummary, getHistorySummary } from '../InternalDryRunHistoryPanel'
import { renderOperationalStatus, isOperationalHealthy, getOperationalMetadataSummary } from '../InternalDryRunOperationalSummaryPanel'
import type {
  TurkuInternalDryRunTriggerResponse,
  TurkuInternalDryRunRecordSummary,
  TurkuInternalDryRunOperationalSummary,
} from '../../../types'

const FORBIDDEN_TEXT = [
  'filePath', 'absolutePath', 'inputPath', 'outputPath', 'workspacePath',
  'stdout', 'stderr', 'commandLine', 'rawLog', 'shellCommand', 'executablePath',
  'storageKey', 'publicUrl', 'signedUrl', 'cdnUrl', 'downloadUrl', 'publishUrl',
  'base64', 'binaryData', 'rawPayload',
  'token', 'JWT', 'session', 'principal', 'authorizationHeader',
  'lyrics', 'scriptBody', 'promptText', 'copyrightedContent', 'userMedia',
]

const PANEL_FILES = [
  'src/components/review/InternalDryRunPanel.tsx',
  'src/components/review/InternalDryRunHistoryPanel.tsx',
  'src/components/review/InternalDryRunOperationalSummaryPanel.tsx',
]

const PANEL_FORBIDDEN = [
  'VideoPlayer', 'AudioPlayer', 'DownloadButton', 'ExportButton',
  'PublishButton', 'PreviewPlayer', 'StreamPlayer',
  'innerHTML', 'dangerouslySetInnerHTML',
  'publicUrl', 'signedUrl', 'cdnUrl', 'downloadUrl',
]

function makeResponse(): TurkuInternalDryRunTriggerResponse {
  return {
    opaqueChainRef: 'chain:054', opaqueStoryboardRef: 'sb:054',
    status: 'ADMIN_METADATA_VISIBLE', issueCodes: [],
    sceneCount: 3, generatedClipCount: 3, stitchedArtifactPresent: true,
    reviewPackageStatus: 'READY_FOR_INTERNAL_REVIEW_ONLY',
    adminMetadataVisible: true, totalDurationSeconds: 9,
    maxWidth: 640, maxHeight: 360, maxFps: 24,
    totalSizeBytes: 3145728, internalOnly: true,
  }
}

function makeRecord(): TurkuInternalDryRunRecordSummary {
  return {
    opaqueRecordRef: 'dryrec:054', opaqueDryRunRef: 'chain:054',
    opaqueStoryboardRef: 'sb:054',
    status: 'READY_FOR_ADMIN_HISTORY_ONLY', issueCodes: [],
    sceneCount: 3, generatedClipCount: 3, inputClipCount: 3,
    stitchedArtifactPresent: true, durationSeconds: 9,
    fps: 24, width: 640, height: 360, sizeBytes: 3145728, internalOnly: true,
  }
}

function makeOpSummary(): TurkuInternalDryRunOperationalSummary {
  return {
    totalRecordCount: 5, acceptedCount: 4, failedCount: 0,
    rejectedCount: 1, readyForAdminHistoryOnlyCount: 4,
    overallStatus: 'MIXED', issueCodes: [],
    totalSceneCount: 15, totalClipCount: 15, stitchedCount: 4, internalOnly: true,
  }
}

describe('FP-054 — Production Readiness Boundary', () => {
  describe('A. Cross-Panel Metadata Rendering', () => {
    it('FP-049 panel renders metadata-only', () => {
      const s = getDryRunMetadataSummary(makeResponse())
      expect(s).toContain('Sahne: 3')
      expect(s).toContain('Dahili: Evet')
    })
    it('FP-051 panel renders metadata-only', () => {
      const s = getRecordMetadataSummary(makeRecord())
      expect(s).toContain('Sahne: 3')
      expect(s).toContain('Dahili: Evet')
    })
    it('FP-053 panel renders metadata-only', () => {
      const s = getOperationalMetadataSummary(makeOpSummary())
      expect(s).toContain('Toplam Kayıt: 5')
      expect(s).toContain('Dahili: Evet')
    })
  })

  describe('B. Cross-Panel Forbidden Text Sweep', () => {
    it('FP-049 summary no forbidden text', () => {
      const s = getDryRunMetadataSummary(makeResponse())
      for (const f of FORBIDDEN_TEXT) expect(s).not.toContain(f)
    })
    it('FP-051 summary no forbidden text', () => {
      const s = getRecordMetadataSummary(makeRecord())
      for (const f of FORBIDDEN_TEXT) expect(s).not.toContain(f)
    })
    it('FP-053 summary no forbidden text', () => {
      const s = getOperationalMetadataSummary(makeOpSummary())
      for (const f of FORBIDDEN_TEXT) expect(s).not.toContain(f)
    })
  })

  describe('C. All Panel Source Files Safety', () => {
    it('all 3 panels have no delivery/unsafe controls', async () => {
      const fs = await import('fs')
      for (const p of PANEL_FILES) {
        const src = fs.readFileSync(p, 'utf-8')
        for (const f of PANEL_FORBIDDEN) expect(src).not.toContain(f)
      }
    })
  })

  describe('D. Rejected/Failed States Safety', () => {
    it('FP-049 rejected', () => expect(renderDryRunStatus('REJECTED_UNSAFE')).toContain('Reddedildi'))
    it('FP-049 failed', () => expect(renderDryRunStatus('FAILED_SAFE')).toContain('Hata'))
    it('FP-051 rejected', () => expect(renderRecordStatus('REJECTED_UNSAFE')).toContain('Reddedildi'))
    it('FP-051 failed', () => expect(renderRecordStatus('FAILED_SAFE')).toContain('Hata'))
    it('FP-053 all_failed', () => expect(renderOperationalStatus('ALL_FAILED')).toContain('Başarısız'))
    it('FP-053 all_rejected', () => expect(renderOperationalStatus('ALL_REJECTED')).toContain('Reddedildi'))
  })

  describe('E. Success/Health Checks', () => {
    it('FP-049 success baseline', () => {
      expect(isDryRunSuccessful(makeResponse())).toBe(true)
      expect(isDryRunSuccessful({ ...makeResponse(), status: 'REJECTED_UNSAFE' })).toBe(false)
    })
    it('FP-051 success baseline', () => {
      expect(isRecordSuccessful(makeRecord())).toBe(true)
      expect(isRecordSuccessful({ ...makeRecord(), status: 'FAILED_SAFE' })).toBe(false)
    })
    it('FP-053 health baseline', () => {
      expect(isOperationalHealthy({ ...makeOpSummary(), overallStatus: 'ALL_ACCEPTED' })).toBe(true)
      expect(isOperationalHealthy(makeOpSummary())).toBe(false)
    })
  })

  describe('F. InternalOnly Always True', () => {
    it('all three models', () => {
      expect(makeResponse().internalOnly).toBe(true)
      expect(makeRecord().internalOnly).toBe(true)
      expect(makeOpSummary().internalOnly).toBe(true)
    })
  })

  describe('G. Türkçe Character Preservation', () => {
    it('FP-049 labels', () => {
      expect(renderDryRunStatus('ADMIN_METADATA_VISIBLE')).toContain('Görünür')
      expect(renderDryRunStatus('STORYBOARD_ACCEPTED')).toContain('Kabul')
    })
    it('FP-051 labels', () => {
      expect(renderRecordStatus('METADATA_CAPTURED')).toContain('Yakalandı')
      expect(renderRecordStatus('TRIGGER_ACCEPTED')).toContain('Kabul')
    })
    it('FP-053 labels', () => {
      expect(renderOperationalStatus('ALL_ACCEPTED')).toContain('Kabul')
      expect(renderOperationalStatus('NO_RECORDS')).toContain('Kayıt')
    })
  })

  describe('H. History Edge Cases', () => {
    it('empty history safe', () => expect(getHistorySummary([])).toBe('Kayıt yok'))
    it('single record history', () => {
      const h = getHistorySummary([makeRecord()])
      expect(h).toContain('#1:')
      expect(h).not.toContain('filePath')
    })
  })
})
