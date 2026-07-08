/**
 * RP-002: Storyboard Draft Preview UI Tests.
 */

import { describe, it, expect } from 'vitest'
import {
  renderDraftStatus,
  isDraftReady,
  renderSceneCard,
  getDraftSummary,
} from '../StoryboardDraftPreview'
import type { TurkuStoryboardDraftResult, TurkuStoryboardDraftScene } from '../../../types'

const FORBIDDEN_TEXT = [
  'filePath', 'absolutePath', 'inputPath', 'outputPath', 'workspacePath',
  'stdout', 'stderr', 'commandLine', 'rawLog', 'shellCommand', 'executablePath',
  'publicUrl', 'signedUrl', 'cdnUrl', 'downloadUrl', 'publishUrl', 'streamUrl',
  'bucketName', 'objectKey', 'base64', 'binaryData', 'rawPayload',
  'token', 'JWT', 'session', 'principal', 'authorizationHeader',
  'lyrics', 'scriptBody', 'promptText', 'copyrightedContent', 'userMedia',
  'modelInput', 'modelOutput', 'providerPayload',
]

const COMPONENT_FORBIDDEN = [
  'DownloadButton', 'ExportButton', 'PublishButton',
  'VideoPlayer', 'AudioPlayer', 'StreamPlayer',
  'innerHTML', 'dangerouslySetInnerHTML',
]

function makeScene(n: number): TurkuStoryboardDraftScene {
  return { sceneNumber: n, purpose: 'Giri\u015f', mood: 'huzurlu', visualDirection: 'Anadolu ovalar\u0131', durationMs: 12000 }
}

function makeReady(): TurkuStoryboardDraftResult {
  return {
    status: 'READY_FOR_ADMIN_PREVIEW',
    issueCodes: [],
    opaqueDraftRef: 'draft:abc',
    opaqueRequestRef: 'sb-req:xyz',
    safeTurkuTitleLabel: '\u00c7anakkale T\u00fcrk\u00fcs\u00fc',
    copyrightSafeIntentSummary: 'Telif g\u00fcvenli niyet',
    orchestrationMode: 'DETERMINISTIC_FAKE_TEST_DOUBLE',
    scenes: [makeScene(1), makeScene(2), makeScene(3)],
    totalSceneCount: 3,
    totalDurationMs: 36000,
    targetFormat: 'MP4',
    targetWidth: 1920,
    targetHeight: 1080,
    targetFps: 24,
  }
}

function makeRejected(): TurkuStoryboardDraftResult {
  return {
    ...makeReady(),
    status: 'REJECTED_UNSAFE',
    issueCodes: ['REQUEST_NOT_READY'],
    opaqueDraftRef: null,
    scenes: [],
    totalSceneCount: 0,
  }
}

describe('RP-002 \u2014 Storyboard Draft Preview', () => {
  describe('A. Status Rendering', () => {
    it('ready renders Haz\u0131r', () => {
      expect(renderDraftStatus('READY_FOR_ADMIN_PREVIEW')).toContain('Haz\u0131r')
    })
    it('rejected renders Reddedildi', () => {
      expect(renderDraftStatus('REJECTED_UNSAFE')).toContain('Reddedildi')
    })
    it('orchestration renders Ba\u015flat\u0131ld\u0131', () => {
      expect(renderDraftStatus('DRAFT_ORCHESTRATION_STARTED')).toContain('Ba\u015flat\u0131ld\u0131')
    })
  })

  describe('B. Ready Check', () => {
    it('ready returns true', () => expect(isDraftReady(makeReady())).toBe(true))
    it('rejected returns false', () => expect(isDraftReady(makeRejected())).toBe(false))
  })

  describe('C. Scene Card Rendering', () => {
    it('shows scene metadata', () => {
      const s = renderSceneCard(makeScene(1))
      expect(s).toContain('Sahne 1')
      expect(s).toContain('Giri\u015f')
      expect(s).toContain('huzurlu')
      expect(s).toContain('Anadolu')
    })
    it('no forbidden text in scene card', () => {
      const s = renderSceneCard(makeScene(1))
      for (const f of FORBIDDEN_TEXT) expect(s).not.toContain(f)
    })
  })

  describe('D. Draft Summary', () => {
    it('ready summary shows metadata', () => {
      const s = getDraftSummary(makeReady())
      expect(s).toContain('\u00c7anakkale T\u00fcrk\u00fcs\u00fc')
      expect(s).toContain('1920x1080')
      expect(s).toContain('DETERMINISTIC_FAKE_TEST_DOUBLE')
      expect(s).toContain('Telif')
    })
    it('rejected shows issue codes', () => {
      const s = getDraftSummary(makeRejected())
      expect(s).toContain('REQUEST_NOT_READY')
    })
  })

  describe('E. Forbidden Text Sweep', () => {
    it('ready summary no forbidden', () => {
      const s = getDraftSummary(makeReady())
      for (const f of FORBIDDEN_TEXT) expect(s).not.toContain(f)
    })
  })

  describe('F. Source File Safety', () => {
    it('component no unsafe controls', async () => {
      const fs = await import('fs')
      const src = fs.readFileSync('src/components/storyboard/StoryboardDraftPreview.tsx', 'utf-8')
      for (const f of COMPONENT_FORBIDDEN) expect(src).not.toContain(f)
    })
  })

  describe('G. Type Safety', () => {
    it('result no URL/path/token fields', () => {
      const keys = Object.keys(makeReady())
      for (const f of ['publicUrl', 'signedUrl', 'cdnUrl', 'filePath', 'absolutePath',
        'token', 'secret', 'lyrics', 'scriptBody', 'modelInput', 'providerPayload']) {
        expect(keys).not.toContain(f)
      }
    })
  })

  describe('H. T\u00fcrk\u00e7e Characters', () => {
    it('status labels', () => {
      expect(renderDraftStatus('SCENES_GENERATED_SAFE')).toContain('Olu\u015fturuldu')
    })
    it('scene card uses Turkish', () => {
      const s = renderSceneCard(makeScene(1))
      expect(s).toContain('G\u00f6rsel Y\u00f6n')
    })
  })
})
