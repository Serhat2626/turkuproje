/**
 * FP-051: Dry Run History Panel tests — metadata-only, no forbidden controls.
 */

import { describe, it, expect } from 'vitest'
import {
  renderRecordStatus,
  isRecordSuccessful,
  getRecordMetadataSummary,
  getHistorySummary,
} from '../InternalDryRunHistoryPanel'
import type { TurkuInternalDryRunRecordSummary } from '../../../types'

function makeRecord(
  overrides: Partial<TurkuInternalDryRunRecordSummary> = {}
): TurkuInternalDryRunRecordSummary {
  return {
    opaqueRecordRef: 'dryrec:abc123',
    opaqueDryRunRef: 'chain:xyz',
    opaqueStoryboardRef: 'sb:test',
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

describe('FP-051 — Dry Run History Panel', () => {
  describe('A. Record Status Labels', () => {
    it('renders all Türkçe status labels', () => {
      expect(renderRecordStatus('CREATED_INTERNAL_ONLY')).toContain('Oluşturuldu')
      expect(renderRecordStatus('TRIGGER_ACCEPTED')).toContain('Kabul')
      expect(renderRecordStatus('DRY_RUN_STARTED')).toContain('Başladı')
      expect(renderRecordStatus('METADATA_CAPTURED')).toContain('Yakalandı')
      expect(renderRecordStatus('READY_FOR_ADMIN_HISTORY_ONLY')).toContain('Hazır')
      expect(renderRecordStatus('REJECTED_UNSAFE')).toContain('Reddedildi')
      expect(renderRecordStatus('FAILED_SAFE')).toContain('Hata')
    })
  })

  describe('B. Record Success', () => {
    it('READY_FOR_ADMIN_HISTORY_ONLY is successful', () => {
      expect(isRecordSuccessful(makeRecord())).toBe(true)
    })
    it('REJECTED_UNSAFE is not successful', () => {
      expect(isRecordSuccessful(makeRecord({ status: 'REJECTED_UNSAFE' }))).toBe(false)
    })
    it('FAILED_SAFE is not successful', () => {
      expect(isRecordSuccessful(makeRecord({ status: 'FAILED_SAFE' }))).toBe(false)
    })
  })

  describe('C. Metadata Summary', () => {
    it('renders record metadata', () => {
      const summary = getRecordMetadataSummary(makeRecord())
      expect(summary).toContain('Sahne: 3')
      expect(summary).toContain('Üretilen Klip: 3')
      expect(summary).toContain('Girdi Klip: 3')
      expect(summary).toContain('640×360')
      expect(summary).toContain('Dahili: Evet')
      expect(summary).toContain('dryrec:abc123')
    })
    it('shows issue codes if present', () => {
      const summary = getRecordMetadataSummary(makeRecord({
        issueCodes: ['TRIGGER_REJECTED', 'DRY_RUN_CHAIN_FAILED']
      }))
      expect(summary).toContain('TRIGGER_REJECTED')
      expect(summary).toContain('DRY_RUN_CHAIN_FAILED')
    })
    it('no Sorunlar line if no issues', () => {
      expect(getRecordMetadataSummary(makeRecord())).not.toContain('Sorunlar:')
    })
  })

  describe('D. History Summary', () => {
    it('empty history → Kayıt yok', () => {
      expect(getHistorySummary([])).toBe('Kayıt yok')
    })
    it('renders numbered history', () => {
      const records = [makeRecord(), makeRecord({ status: 'FAILED_SAFE', sceneCount: 2 })]
      const summary = getHistorySummary(records)
      expect(summary).toContain('#1:')
      expect(summary).toContain('#2:')
      expect(summary).toContain('Sahne: 3')
      expect(summary).toContain('Sahne: 2')
    })
  })

  describe('E. No Forbidden Controls', () => {
    it('summary has no forbidden fields', () => {
      const summary = getRecordMetadataSummary(makeRecord())
      const forbidden = [
        'filePath', 'absolutePath', 'inputPath', 'outputPath', 'workspacePath',
        'stdout', 'stderr', 'commandLine', 'rawLog',
        'storageKey', 'publicUrl', 'signedUrl', 'cdnUrl',
        'base64', 'downloadUrl', 'publishUrl',
        'token', 'JWT', 'session', 'principal',
        'lyrics', 'scriptBody', 'promptText',
      ]
      for (const f of forbidden) expect(summary).not.toContain(f)
    })
  })

  describe('F. Panel Source Safety', () => {
    it('panel has no delivery/unsafe controls', async () => {
      const fs = await import('fs')
      const source = fs.readFileSync(
        'src/components/review/InternalDryRunHistoryPanel.tsx', 'utf-8'
      )
      const forbidden = [
        'VideoPlayer', 'AudioPlayer', 'DownloadButton', 'ExportButton',
        'PublishButton', 'PreviewPlayer', 'StreamPlayer',
        'publicUrl', 'signedUrl', 'cdnUrl', 'downloadUrl',
        'innerHTML', 'dangerouslySetInnerHTML',
      ]
      for (const f of forbidden) expect(source).not.toContain(f)
    })
  })

  describe('G. Type Safety', () => {
    it('internalOnly is always true', () => {
      expect(makeRecord().internalOnly).toBe(true)
    })
    it('record has all expected fields', () => {
      const r = makeRecord()
      expect(r.opaqueRecordRef).toBeDefined()
      expect(r.opaqueDryRunRef).toBeDefined()
      expect(r.opaqueStoryboardRef).toBeDefined()
      expect(r.status).toBeDefined()
      expect(r.issueCodes).toBeDefined()
      expect(r.sceneCount).toBeGreaterThan(0)
      expect(r.generatedClipCount).toBeGreaterThan(0)
      expect(r.inputClipCount).toBeGreaterThan(0)
      expect(r.durationSeconds).toBeGreaterThan(0)
      expect(r.fps).toBeGreaterThan(0)
      expect(r.width).toBeGreaterThan(0)
      expect(r.height).toBeGreaterThan(0)
      expect(r.sizeBytes).toBeGreaterThan(0)
    })
  })
})
