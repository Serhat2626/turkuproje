/**
 * RP-004: Animatic Timeline Preview UI Tests.
 */

import { describe, it, expect } from 'vitest'
import {
  renderTimelineStatus,
  isTimelineReady,
  renderTimelineItem,
  getTimelineSummary,
} from '../AnimaticTimelinePreview'
import type { TurkuAnimaticTimelineItem, TurkuAnimaticTimelinePlanResult } from '../../../types'

const FORBIDDEN_TEXT = [
  'filePath', 'absolutePath', 'stdout', 'stderr', 'commandLine', 'rawLog',
  'publicUrl', 'signedUrl', 'cdnUrl', 'downloadUrl', 'publishUrl', 'streamUrl',
  'bucketName', 'objectKey', 'storageKey', 'base64', 'binaryData', 'rawPayload',
  'token', 'JWT', 'session', 'principal',
  'lyrics', 'scriptBody', 'promptText', 'providerPrompt', 'rawPrompt',
]

const COMPONENT_FORBIDDEN = [
  'DownloadButton', 'ExportButton', 'PublishButton',
  'innerHTML', 'dangerouslySetInnerHTML',
  'FFmpeg', 'ProcessBuilder',
]

function makeItem(n: number): TurkuAnimaticTimelineItem {
  return {
    sceneNumber: n, startMs: (n - 1) * 10000, durationMs: 10000,
    transitionType: 'FADE_IN', cameraMovement: 'SLOW_PAN_RIGHT',
    visualPacingSummary: 'Yava\u015f ba\u015flang\u0131\u00e7', narrativeBeatSummary: 'Tan\u0131t\u0131m',
  }
}

function makeResult(): TurkuAnimaticTimelinePlanResult {
  return {
    status: 'READY_FOR_ASSEMBLY_BOUNDARY', issueCodes: [],
    opaqueTimelineRef: 'timeline:abc', opaquePlanRef: 'plan:xyz', opaqueDraftRef: 'draft:def',
    safeTurkuTitleLabel: '\u00c7anakkale T\u00fcrk\u00fcs\u00fc', plannerMode: 'DETERMINISTIC_FAKE_TEST_DOUBLE',
    items: [makeItem(1), makeItem(2), makeItem(3)], totalItems: 3, totalDurationMs: 30000,
  }
}

describe('RP-004 \u2014 Animatic Timeline Preview', () => {
  describe('A. Status Rendering', () => {
    it('ready renders Haz\u0131r', () => {
      expect(renderTimelineStatus('READY_FOR_ASSEMBLY_BOUNDARY')).toContain('Haz\u0131r')
    })
    it('rejected renders Reddedildi', () => {
      expect(renderTimelineStatus('REJECTED_UNSAFE')).toContain('Reddedildi')
    })
    it('timeline renders Olu\u015fturuldu', () => {
      expect(renderTimelineStatus('TIMELINE_ITEMS_GENERATED')).toContain('Olu\u015fturuldu')
    })
  })

  describe('B. Ready Check', () => {
    it('ready returns true', () => expect(isTimelineReady(makeResult())).toBe(true))
    it('rejected returns false', () => {
      expect(isTimelineReady({ ...makeResult(), status: 'REJECTED_UNSAFE' })).toBe(false)
    })
  })

  describe('C. Item Rendering', () => {
    it('shows scene metadata', () => {
      const s = renderTimelineItem(makeItem(1))
      expect(s).toContain('Sahne 1')
      expect(s).toContain('FADE_IN')
      expect(s).toContain('SLOW_PAN_RIGHT')
      expect(s).toContain('Yava\u015f')
    })
  })

  describe('D. Timeline Summary', () => {
    it('summary shows metadata', () => {
      const s = getTimelineSummary(makeResult())
      expect(s).toContain('\u00c7anakkale')
      expect(s).toContain('DETERMINISTIC_FAKE_TEST_DOUBLE')
      expect(s).toContain('30000ms')
    })
  })

  describe('E. Forbidden Text Sweep', () => {
    it('item no forbidden', () => {
      const s = renderTimelineItem(makeItem(1))
      for (const f of FORBIDDEN_TEXT) expect(s).not.toContain(f)
    })
    it('summary no forbidden', () => {
      const s = getTimelineSummary(makeResult())
      for (const f of FORBIDDEN_TEXT) expect(s).not.toContain(f)
    })
  })

  describe('F. Source File Safety', () => {
    it('component no unsafe controls', async () => {
      const fs = await import('fs')
      const src = fs.readFileSync('src/components/storyboard/AnimaticTimelinePreview.tsx', 'utf-8')
      for (const f of COMPONENT_FORBIDDEN) expect(src).not.toContain(f)
    })
  })

  describe('G. Type Safety', () => {
    it('result no forbidden fields', () => {
      const keys = Object.keys(makeResult())
      for (const f of ['publicUrl', 'signedUrl', 'filePath', 'token', 'lyrics', 'providerPrompt']) {
        expect(keys).not.toContain(f)
      }
    })
  })

  describe('H. T\u00fcrk\u00e7e Characters', () => {
    it('status labels', () => {
      expect(renderTimelineStatus('ASSET_PLAN_ACCEPTED')).toContain('Kabul')
    })
    it('item labels', () => {
      const s = renderTimelineItem(makeItem(1))
      expect(s).toContain('Ge\u00e7i\u015f')
      expect(s).toContain('Kamera')
    })
  })
})
