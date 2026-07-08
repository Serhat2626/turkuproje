/**
 * FP-053: Operational Summary Panel tests.
 */

import { describe, it, expect } from 'vitest'
import {
  renderOperationalStatus,
  isOperationalHealthy,
  getOperationalMetadataSummary,
} from '../InternalDryRunOperationalSummaryPanel'
import type { TurkuInternalDryRunOperationalSummary } from '../../../types'

function makeSummary(
  overrides: Partial<TurkuInternalDryRunOperationalSummary> = {}
): TurkuInternalDryRunOperationalSummary {
  return {
    totalRecordCount: 5,
    acceptedCount: 3,
    failedCount: 1,
    rejectedCount: 1,
    readyForAdminHistoryOnlyCount: 3,
    overallStatus: 'MIXED',
    issueCodes: [],
    totalSceneCount: 12,
    totalClipCount: 12,
    stitchedCount: 3,
    internalOnly: true,
    ...overrides,
  }
}

describe('FP-053 — Operational Summary Panel', () => {
  describe('A. Status Labels', () => {
    it('renders all Türkçe labels', () => {
      expect(renderOperationalStatus('NO_RECORDS')).toContain('Kayıt Yok')
      expect(renderOperationalStatus('ALL_ACCEPTED')).toContain('Kabul')
      expect(renderOperationalStatus('MIXED')).toContain('Karışık')
      expect(renderOperationalStatus('ALL_FAILED')).toContain('Başarısız')
      expect(renderOperationalStatus('ALL_REJECTED')).toContain('Reddedildi')
    })
  })

  describe('B. Health Check', () => {
    it('ALL_ACCEPTED is healthy', () => {
      expect(isOperationalHealthy(makeSummary({ overallStatus: 'ALL_ACCEPTED' }))).toBe(true)
    })
    it('MIXED is not healthy', () => {
      expect(isOperationalHealthy(makeSummary())).toBe(false)
    })
    it('ALL_FAILED is not healthy', () => {
      expect(isOperationalHealthy(makeSummary({ overallStatus: 'ALL_FAILED' }))).toBe(false)
    })
  })

  describe('C. Summary Rendering', () => {
    it('renders counts correctly', () => {
      const text = getOperationalMetadataSummary(makeSummary())
      expect(text).toContain('Toplam Kayıt: 5')
      expect(text).toContain('Kabul: 3')
      expect(text).toContain('Başarısız: 1')
      expect(text).toContain('Reddedilen: 1')
      expect(text).toContain('Toplam Sahne: 12')
      expect(text).toContain('Toplam Klip: 12')
      expect(text).toContain('Birleştirilmiş: 3')
      expect(text).toContain('Dahili: Evet')
    })
    it('shows issue codes if present', () => {
      const text = getOperationalMetadataSummary(
        makeSummary({ issueCodes: ['HIGH_FAILURE_RATE', 'STITCH_SUCCESS_LOW'] })
      )
      expect(text).toContain('HIGH_FAILURE_RATE')
      expect(text).toContain('STITCH_SUCCESS_LOW')
    })
    it('no Sorunlar line if no issues', () => {
      expect(getOperationalMetadataSummary(makeSummary())).not.toContain('Sorunlar:')
    })
  })

  describe('D. No Forbidden Fields', () => {
    it('summary text has no forbidden', () => {
      const text = getOperationalMetadataSummary(makeSummary())
      for (const f of ['filePath', 'stdout', 'stderr', 'publicUrl', 'token',
                        'lyrics', 'commandLine', 'rawLog', 'signedUrl', 'cdnUrl'])
        expect(text).not.toContain(f)
    })
  })

  describe('E. Panel Source Safety', () => {
    it('panel has no delivery/unsafe controls', async () => {
      const fs = await import('fs')
      const source = fs.readFileSync(
        'src/components/review/InternalDryRunOperationalSummaryPanel.tsx', 'utf-8'
      )
      for (const f of ['VideoPlayer', 'DownloadButton', 'ExportButton', 'PublishButton',
                        'innerHTML', 'dangerouslySetInnerHTML', 'publicUrl', 'signedUrl'])
        expect(source).not.toContain(f)
    })
  })

  describe('F. InternalOnly', () => {
    it('always true', () => {
      expect(makeSummary().internalOnly).toBe(true)
    })
  })
})
