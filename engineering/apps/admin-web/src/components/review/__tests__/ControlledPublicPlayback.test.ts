/**
 * PD-007: Controlled Public Playback MVP UI Tests.
 */

import { describe, it, expect } from 'vitest'
import {
  renderPlaybackStatus,
  isControlledPlaybackReady,
  getPlaybackMetadataSummary,
  getPlaybackPlaceholder,
} from '../ControlledPublicPlayback'
import type { ProductionMediaPublicPlaybackResult } from '../../../types'

const FORBIDDEN_TEXT = [
  'filePath', 'absolutePath', 'inputPath', 'outputPath', 'workspacePath',
  'stdout', 'stderr', 'commandLine', 'rawLog', 'shellCommand', 'executablePath',
  'storageKey', 'publicUrl', 'signedUrl', 'cdnUrl', 'downloadUrl', 'publishUrl', 'streamUrl',
  'bucketName', 'objectKey', 'base64', 'binaryData', 'rawPayload',
  'token', 'JWT', 'session', 'principal', 'authorizationHeader',
  'lyrics', 'scriptBody', 'promptText', 'copyrightedContent', 'userMedia',
]

const PANEL_FORBIDDEN = [
  'DownloadButton', 'ExportButton', 'PublishButton',
  'innerHTML', 'dangerouslySetInnerHTML',
  'video src=', 'publicUrl', 'signedUrl', 'cdnUrl',
]

function makeReady(): ProductionMediaPublicPlaybackResult {
  return {
    status: 'READY_FOR_CONTROLLED_PUBLIC_PLAYBACK',
    issueCodes: [],
    opaquePublicPlaybackRef: 'pub-play:abc',
    opaqueDeliveryGateRef: 'gate:abc',
    opaqueArtifactRef: 'art:mp4',
    mediaFormat: 'MP4',
    durationSeconds: 10,
    fps: 24,
    width: 640,
    height: 360,
    sizeBytes: 1024,
    controlledPlaybackEligible: true,
  }
}

function makeRejected(): ProductionMediaPublicPlaybackResult {
  return {
    ...makeReady(),
    status: 'REJECTED_UNSAFE',
    issueCodes: ['DELIVERY_GATE_NOT_APPROVED'],
    opaquePublicPlaybackRef: null,
    controlledPlaybackEligible: false,
  }
}

function makeFailed(): ProductionMediaPublicPlaybackResult {
  return {
    ...makeReady(),
    status: 'FAILED_SAFE',
    issueCodes: ['INTERNAL_ERROR'],
    opaquePublicPlaybackRef: null,
    controlledPlaybackEligible: false,
  }
}

describe('PD-007 \u2014 Controlled Public Playback MVP', () => {
  describe('A. Status Rendering', () => {
    it('ready renders Haz\u0131r', () => {
      expect(renderPlaybackStatus('READY_FOR_CONTROLLED_PUBLIC_PLAYBACK')).toContain('Haz\u0131r')
    })
    it('rejected renders Reddedildi', () => {
      expect(renderPlaybackStatus('REJECTED_UNSAFE')).toContain('Reddedildi')
    })
    it('failed renders Ba\u015far\u0131s\u0131z', () => {
      expect(renderPlaybackStatus('FAILED_SAFE')).toContain('Ba\u015far\u0131s\u0131z')
    })
    it('handle created renders Olu\u015fturuldu', () => {
      expect(renderPlaybackStatus('CONTROLLED_PLAYBACK_HANDLE_CREATED')).toContain('Olu\u015fturuldu')
    })
  })

  describe('B. Controlled Playback Ready Check', () => {
    it('ready returns true', () => expect(isControlledPlaybackReady(makeReady())).toBe(true))
    it('rejected returns false', () => expect(isControlledPlaybackReady(makeRejected())).toBe(false))
    it('failed returns false', () => expect(isControlledPlaybackReady(makeFailed())).toBe(false))
  })

  describe('C. Metadata Summary', () => {
    it('ready summary shows metadata', () => {
      const s = getPlaybackMetadataSummary(makeReady())
      expect(s).toContain('Format: MP4')
      expect(s).toContain('640x360')
      expect(s).toContain('pub-play:abc')
      expect(s).toContain('Evet')
    })
    it('rejected summary shows issue codes', () => {
      const s = getPlaybackMetadataSummary(makeRejected())
      expect(s).toContain('DELIVERY_GATE_NOT_APPROVED')
      expect(s).toContain('Hay\u0131r')
    })
  })

  describe('D. Playback Placeholder', () => {
    it('ready shows controlled playback', () => {
      const p = getPlaybackPlaceholder(makeReady())
      expect(p).toContain('Kontroll\u00fc Oynatma Haz\u0131r')
      expect(p).toContain('pub-play:abc')
    })
    it('rejected shows unavailable', () => {
      const p = getPlaybackPlaceholder(makeRejected())
      expect(p).toContain('Oynatma Kullan\u0131lam\u0131yor')
    })
  })

  describe('E. Forbidden Text Sweep', () => {
    it('ready summary no forbidden', () => {
      const s = getPlaybackMetadataSummary(makeReady())
      for (const f of FORBIDDEN_TEXT) expect(s).not.toContain(f)
    })
    it('rejected summary no forbidden', () => {
      const s = getPlaybackMetadataSummary(makeRejected())
      for (const f of FORBIDDEN_TEXT) expect(s).not.toContain(f)
    })
    it('placeholder no forbidden', () => {
      const p = getPlaybackPlaceholder(makeReady())
      for (const f of FORBIDDEN_TEXT) expect(p).not.toContain(f)
    })
  })

  describe('F. Source File Safety', () => {
    it('component has no delivery/unsafe controls', async () => {
      const fs = await import('fs')
      const src = fs.readFileSync('src/components/review/ControlledPublicPlayback.tsx', 'utf-8')
      for (const f of PANEL_FORBIDDEN) expect(src).not.toContain(f)
    })
  })

  describe('G. Type Safety', () => {
    it('ready result has all required fields', () => {
      const r = makeReady()
      expect(r.opaquePublicPlaybackRef).toBeDefined()
      expect(r.opaqueDeliveryGateRef).toBeDefined()
      expect(r.controlledPlaybackEligible).toBe(true)
    })
    it('result has no URL/path/token fields', () => {
      const keys = Object.keys(makeReady())
      for (const f of ['publicUrl', 'signedUrl', 'cdnUrl', 'streamUrl', 'downloadUrl',
        'filePath', 'absolutePath', 'token', 'secret', 'bucketName', 'objectKey']) {
        expect(keys).not.toContain(f)
      }
    })
  })

  describe('H. T\u00fcrk\u00e7e Characters', () => {
    it('status labels', () => {
      expect(renderPlaybackStatus('DELIVERY_GATE_ACCEPTED')).toContain('Kap\u0131s\u0131')
    })
    it('metadata summary uses correct chars', () => {
      const s = getPlaybackMetadataSummary(makeReady())
      expect(s).toContain('\u00c7\u00f6z\u00fcn\u00fcrl\u00fck')
    })
  })
})
