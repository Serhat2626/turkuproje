/**
 * PD-004: Admin Internal Preview Player UI Boundary Tests.
 */

import { describe, it, expect } from 'vitest'
import {
  renderPreviewStatus,
  isInternalPreviewEligible,
  getPreviewMetadataSummary,
  getPreviewPlaceholder,
} from '../InternalPreviewPlayerBoundary'
import type { ProductionMediaPlaybackPreparationResult } from '../../../types'

const FORBIDDEN_TEXT = [
  'filePath', 'absolutePath', 'inputPath', 'outputPath', 'workspacePath',
  'stdout', 'stderr', 'commandLine', 'rawLog', 'shellCommand', 'executablePath',
  'storageKey', 'publicUrl', 'signedUrl', 'cdnUrl', 'downloadUrl', 'publishUrl', 'streamUrl',
  'bucketName', 'objectKey',
  'base64', 'binaryData', 'rawPayload',
  'token', 'JWT', 'session', 'principal', 'authorizationHeader',
  'lyrics', 'scriptBody', 'promptText', 'copyrightedContent', 'userMedia',
]

const PANEL_FORBIDDEN = [
  'VideoPlayer', 'AudioPlayer', 'DownloadButton', 'ExportButton',
  'PublishButton', 'StreamPlayer', 'video src=',
  'innerHTML', 'dangerouslySetInnerHTML',
]

function makeEligible(): ProductionMediaPlaybackPreparationResult {
  return {
    opaquePlaybackPreparationRef: 'playback-prep:abc',
    opaqueStorageObjectRef: 'fake-obj:123',
    opaqueArtifactRef: 'art:mp4',
    mediaFormat: 'MP4',
    durationSeconds: 10,
    fps: 24,
    width: 640,
    height: 360,
    sizeBytes: 1024,
    status: 'READY_FOR_INTERNAL_PREVIEW_ONLY',
    issueCodes: [],
    internalPreviewEligible: true,
  }
}

function makeRejected(): ProductionMediaPlaybackPreparationResult {
  return {
    ...makeEligible(),
    status: 'REJECTED_UNSAFE',
    issueCodes: ['FORMAT_NOT_SUPPORTED'],
    internalPreviewEligible: false,
  }
}

function makeFailed(): ProductionMediaPlaybackPreparationResult {
  return {
    ...makeEligible(),
    status: 'FAILED_SAFE',
    issueCodes: ['INTERNAL_ERROR'],
    internalPreviewEligible: false,
  }
}

describe('PD-004 — Admin Internal Preview Player UI Boundary', () => {
  describe('A. Status Rendering', () => {
    it('eligible renders Hazır', () => {
      expect(renderPreviewStatus('READY_FOR_INTERNAL_PREVIEW_ONLY')).toContain('Hazır')
    })
    it('rejected renders Reddedildi', () => {
      expect(renderPreviewStatus('REJECTED_UNSAFE')).toContain('Reddedildi')
    })
    it('failed renders Başarısız', () => {
      expect(renderPreviewStatus('FAILED_SAFE')).toContain('Başarısız')
    })
    it('created renders Oluşturuldu', () => {
      expect(renderPreviewStatus('CREATED_INTERNAL_ONLY')).toContain('Oluşturuldu')
    })
  })

  describe('B. Eligibility Check', () => {
    it('eligible result returns true', () => {
      expect(isInternalPreviewEligible(makeEligible())).toBe(true)
    })
    it('rejected result returns false', () => {
      expect(isInternalPreviewEligible(makeRejected())).toBe(false)
    })
    it('failed result returns false', () => {
      expect(isInternalPreviewEligible(makeFailed())).toBe(false)
    })
    it('wrong status + eligible flag returns false', () => {
      expect(isInternalPreviewEligible({ ...makeEligible(), status: 'FAILED_SAFE' })).toBe(false)
    })
  })

  describe('C. Metadata Summary', () => {
    it('eligible shows metadata', () => {
      const s = getPreviewMetadataSummary(makeEligible())
      expect(s).toContain('Format: MP4')
      expect(s).toContain('640x360')
      expect(s).toContain('FPS: 24')
      expect(s).toContain('Evet')
    })
    it('rejected shows issue codes', () => {
      const s = getPreviewMetadataSummary(makeRejected())
      expect(s).toContain('FORMAT_NOT_SUPPORTED')
      expect(s).toContain('Hayır')
    })
  })

  describe('D. Preview Placeholder', () => {
    it('eligible shows internal preview placeholder', () => {
      const p = getPreviewPlaceholder(makeEligible())
      expect(p).toContain('Dahili Önizleme Hazır')
      expect(p).toContain('dahili kullanım')
    })
    it('rejected shows unavailable', () => {
      const p = getPreviewPlaceholder(makeRejected())
      expect(p).toContain('Önizleme Kullanılamıyor')
    })
  })

  describe('E. Forbidden Text Sweep', () => {
    it('eligible summary no forbidden text', () => {
      const s = getPreviewMetadataSummary(makeEligible())
      for (const f of FORBIDDEN_TEXT) expect(s).not.toContain(f)
    })
    it('rejected summary no forbidden text', () => {
      const s = getPreviewMetadataSummary(makeRejected())
      for (const f of FORBIDDEN_TEXT) expect(s).not.toContain(f)
    })
    it('placeholder no forbidden text', () => {
      const p = getPreviewPlaceholder(makeEligible())
      for (const f of FORBIDDEN_TEXT) expect(p).not.toContain(f)
    })
  })

  describe('F. Source File Safety', () => {
    it('component has no delivery/unsafe controls', async () => {
      const fs = await import('fs')
      const src = fs.readFileSync('src/components/review/InternalPreviewPlayerBoundary.tsx', 'utf-8')
      for (const f of PANEL_FORBIDDEN) expect(src).not.toContain(f)
    })
  })

  describe('G. Türkçe Character Preservation', () => {
    it('eligible label has İç and Önizleme', () => {
      const s = getPreviewMetadataSummary(makeEligible())
      expect(s).toContain('Önizleme')
    })
    it('status labels use correct chars', () => {
      expect(renderPreviewStatus('PLAYBACK_METADATA_PREPARED_INTERNAL_ONLY')).toContain('Hazırlandı')
    })
  })

  describe('H. Type Safety', () => {
    it('eligible result has all required fields', () => {
      const r = makeEligible()
      expect(r.opaquePlaybackPreparationRef).toBeDefined()
      expect(r.opaqueStorageObjectRef).toBeDefined()
      expect(r.opaqueArtifactRef).toBeDefined()
      expect(r.mediaFormat).toBeDefined()
      expect(r.durationSeconds).toBeGreaterThan(0)
      expect(r.fps).toBeGreaterThan(0)
      expect(r.width).toBeGreaterThan(0)
      expect(r.height).toBeGreaterThan(0)
      expect(r.sizeBytes).toBeGreaterThan(0)
      expect(r.internalPreviewEligible).toBe(true)
    })
    it('result has no URL/path/token fields', () => {
      const keys = Object.keys(makeEligible())
      for (const f of ['publicUrl', 'signedUrl', 'cdnUrl', 'streamUrl', 'downloadUrl',
        'filePath', 'absolutePath', 'token', 'secret', 'bucketName', 'objectKey']) {
        expect(keys).not.toContain(f)
      }
    })
  })
})
