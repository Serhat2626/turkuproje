/**
 * RP-001: Türkü Name Storyboard Request UI Tests.
 */

import { describe, it, expect } from 'vitest'
import {
  renderRequestStatus,
  isRequestReady,
  getRequestSummary,
  getInputWarning,
} from '../TurkuNameStoryboardForm'
import type { TurkuNameStoryboardRequestResult } from '../../../types'

const FORBIDDEN_TEXT = [
  'filePath', 'absolutePath', 'inputPath', 'outputPath', 'workspacePath',
  'stdout', 'stderr', 'commandLine', 'rawLog', 'shellCommand', 'executablePath',
  'publicUrl', 'signedUrl', 'cdnUrl', 'downloadUrl', 'publishUrl', 'streamUrl',
  'bucketName', 'objectKey', 'base64', 'binaryData', 'rawPayload',
  'token', 'JWT', 'session', 'principal', 'authorizationHeader',
  'lyrics', 'scriptBody', 'promptText', 'copyrightedContent', 'userMedia',
]

const COMPONENT_FORBIDDEN = [
  'textarea', 'DownloadButton', 'ExportButton', 'PublishButton',
  'VideoPlayer', 'AudioPlayer', 'StreamPlayer',
  'innerHTML', 'dangerouslySetInnerHTML',
  'type="file"', 'input type="url"',
]

function makeReady(): TurkuNameStoryboardRequestResult {
  return {
    status: 'READY_FOR_STORYBOARD_GENERATION_BOUNDARY',
    issueCodes: [],
    opaqueStoryboardRequestRef: 'sb-req:abc',
    safeTurkuTitleLabel: '\u00c7anakkale T\u00fcrk\u00fcs\u00fc',
    inspirationMode: 'TURKU_TITLE_INSPIRATION',
    targetSceneCount: 8,
    targetDurationMs: 60000,
    targetFormat: 'MP4',
    targetWidth: 1920,
    targetHeight: 1080,
    targetFps: 24,
    copyrightSafeIntentSummary: 'Telif g\u00fcvenli niyet',
  }
}

function makeRejected(): TurkuNameStoryboardRequestResult {
  return {
    ...makeReady(),
    status: 'REJECTED_UNSAFE',
    issueCodes: ['TITLE_CONTAINS_LYRICS'],
    opaqueStoryboardRequestRef: null,
    safeTurkuTitleLabel: null,
    copyrightSafeIntentSummary: null,
  }
}

describe('RP-001 \u2014 T\u00fcrk\u00fc Name Storyboard Request', () => {
  describe('A. Status Rendering', () => {
    it('ready renders Haz\u0131r', () => {
      expect(renderRequestStatus('READY_FOR_STORYBOARD_GENERATION_BOUNDARY')).toContain('Haz\u0131r')
    })
    it('rejected renders Reddedildi', () => {
      expect(renderRequestStatus('REJECTED_UNSAFE')).toContain('Reddedildi')
    })
    it('accepted renders Kabul', () => {
      expect(renderRequestStatus('TURKU_NAME_ACCEPTED')).toContain('Kabul')
    })
    it('copyright safe renders Telif', () => {
      expect(renderRequestStatus('COPYRIGHT_SAFE_INTENT_CREATED')).toContain('Telif')
    })
  })

  describe('B. Ready Check', () => {
    it('ready returns true', () => expect(isRequestReady(makeReady())).toBe(true))
    it('rejected returns false', () => expect(isRequestReady(makeRejected())).toBe(false))
  })

  describe('C. Request Summary', () => {
    it('ready shows metadata', () => {
      const s = getRequestSummary(makeReady())
      expect(s).toContain('\u00c7anakkale T\u00fcrk\u00fcs\u00fc')
      expect(s).toContain('1920x1080')
      expect(s).toContain('FPS: 24')
      expect(s).toContain('Telif')
    })
    it('rejected shows issue codes', () => {
      const s = getRequestSummary(makeRejected())
      expect(s).toContain('TITLE_CONTAINS_LYRICS')
    })
  })

  describe('D. Input Warning', () => {
    it('warning mentions no lyrics/media', () => {
      const w = getInputWarning()
      expect(w).toContain('S\u00f6z')
      expect(w).toContain('medya')
    })
  })

  describe('E. Forbidden Text Sweep', () => {
    it('ready summary no forbidden', () => {
      const s = getRequestSummary(makeReady())
      for (const f of FORBIDDEN_TEXT) expect(s).not.toContain(f)
    })
    it('rejected summary no forbidden', () => {
      const s = getRequestSummary(makeRejected())
      for (const f of FORBIDDEN_TEXT) expect(s).not.toContain(f)
    })
  })

  describe('F. Source File Safety', () => {
    it('component has no unsafe controls', async () => {
      const fs = await import('fs')
      const src = fs.readFileSync('src/components/storyboard/TurkuNameStoryboardForm.tsx', 'utf-8')
      for (const f of COMPONENT_FORBIDDEN) expect(src).not.toContain(f)
    })
  })

  describe('G. Type Safety', () => {
    it('result has all required fields', () => {
      const r = makeReady()
      expect(r.opaqueStoryboardRequestRef).toBeDefined()
      expect(r.safeTurkuTitleLabel).toBeDefined()
      expect(r.copyrightSafeIntentSummary).toBeDefined()
    })
    it('result has no URL/path/token fields', () => {
      const keys = Object.keys(makeReady())
      for (const f of ['publicUrl', 'signedUrl', 'cdnUrl', 'streamUrl', 'downloadUrl',
        'filePath', 'absolutePath', 'token', 'secret', 'lyrics', 'scriptBody']) {
        expect(keys).not.toContain(f)
      }
    })
  })

  describe('H. T\u00fcrk\u00e7e Characters', () => {
    it('status labels use correct chars', () => {
      expect(renderRequestStatus('CREATED_INTERNAL_ONLY')).toContain('Olu\u015fturuldu')
    })
    it('warning uses Turkish', () => {
      expect(getInputWarning()).toContain('giriniz')
    })
  })
})
