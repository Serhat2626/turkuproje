/**
 * LIVE-002: Live Image Generation Preview UI Tests.
 */

import { describe, it, expect } from 'vitest'
import {
  renderLiveGenStatus,
  renderLiveGenMode,
  renderLiveImageResult,
  getLiveGenSummary,
  isRealProviderResult,
  areAllGatesPassed,
} from '../LiveImageGenerationPreview'
import type { TurkuLiveGenerationResult, TurkuLiveImageResultMetadata } from '../../../types'

const FORBIDDEN_TEXT = [
  'filePath', 'absolutePath', 'stdout', 'stderr', 'commandLine', 'rawLog',
  'publicUrl', 'signedUrl', 'cdnUrl', 'downloadUrl', 'storageKey', 'base64',
  'token', 'JWT', 'session', 'principal', 'apiKey', 'credential',
  'lyrics', 'scriptBody', 'providerPrompt', 'rawPrompt', 'providerPayload',
]

const COMPONENT_FORBIDDEN = [
  'DownloadButton', 'ExportButton', 'PublishButton', 'ApproveButton',
  'RenderButton', 'FfmpegButton',
  'innerHTML', 'dangerouslySetInnerHTML',
]

function makeFakeResult(): TurkuLiveImageResultMetadata {
  return { sceneNo: 1, opaqueArtifactRef: 'fimg:abc', artifactLabel: 'Sahne 1 sahte', formatHint: 'PNG', widthHint: 1920, heightHint: 1080, generationMode: 'CI_FAKE', sanitizedProviderSummary: 'CI_FAKE', humanReviewRequired: false }
}

function makeRealResult(): TurkuLiveImageResultMetadata {
  return { sceneNo: 1, opaqueArtifactRef: 'limg:abc', artifactLabel: 'Sahne 1 gercek', formatHint: 'PNG', widthHint: 1920, heightHint: 1080, generationMode: 'LOCAL_MANUAL_REAL_PROVIDER', sanitizedProviderSummary: 'Yanit alindi, icerik gizli', humanReviewRequired: true }
}

function makeFakeGen(): TurkuLiveGenerationResult {
  return {
    status: 'FAKE_RESULT_AVAILABLE', mode: 'CI_FAKE',
    opaqueGenerationRef: 'lfgen:abc', opaquePromptPackageRef: 'ipkg:def',
    killSwitchActive: false, operatorGatePassed: false, providerReady: false,
    results: [makeFakeResult()], totalScenes: 1, generatedCount: 1,
    blockedReasons: [], sanitizedProviderSummary: 'CI_FAKE', validationSummary: '1 sahte gorsel',
  }
}

function makeRealGen(): TurkuLiveGenerationResult {
  return {
    status: 'REAL_PROVIDER_RESULT_AVAILABLE', mode: 'LOCAL_MANUAL_REAL_PROVIDER',
    opaqueGenerationRef: 'lgen:abc', opaquePromptPackageRef: 'ipkg:def',
    killSwitchActive: false, operatorGatePassed: true, providerReady: true,
    results: [makeRealResult()], totalScenes: 1, generatedCount: 1,
    blockedReasons: [], sanitizedProviderSummary: 'Gercek saglayici', validationSummary: '1 gorsel',
  }
}

function makeBlocked(): TurkuLiveGenerationResult {
  return { ...makeFakeGen(), status: 'BLOCKED_BY_KILL_SWITCH', killSwitchActive: true, results: [], generatedCount: 0 }
}

describe('LIVE-002 — Live Image Generation Preview', () => {
  describe('A. Status Rendering', () => {
    it('fake', () => expect(renderLiveGenStatus('FAKE_RESULT_AVAILABLE')).toContain('Sahte'))
    it('real', () => expect(renderLiveGenStatus('REAL_PROVIDER_RESULT_AVAILABLE')).toContain('Gercek'))
    it('kill switch', () => expect(renderLiveGenStatus('BLOCKED_BY_KILL_SWITCH')).toContain('Kill'))
    it('operator gate', () => expect(renderLiveGenStatus('BLOCKED_BY_OPERATOR_GATE')).toContain('Operator'))
    it('provider', () => expect(renderLiveGenStatus('BLOCKED_BY_PROVIDER_READINESS')).toContain('Hazir'))
  })

  describe('B. Mode Rendering', () => {
    it('CI_FAKE', () => expect(renderLiveGenMode('CI_FAKE')).toContain('Sahte'))
    it('LOCAL_MANUAL', () => expect(renderLiveGenMode('LOCAL_MANUAL_REAL_PROVIDER')).toContain('Gercek'))
  })

  describe('C. Result Rendering', () => {
    it('fake result', () => expect(renderLiveImageResult(makeFakeResult())).toContain('Sahne 1'))
    it('real result needs review', () => expect(renderLiveImageResult(makeRealResult())).toContain('Inceleme'))
  })

  describe('D. Gate Checks', () => {
    it('fake = not real', () => expect(isRealProviderResult(makeFakeGen())).toBe(false))
    it('real = real', () => expect(isRealProviderResult(makeRealGen())).toBe(true))
    it('blocked = gates not passed', () => expect(areAllGatesPassed(makeBlocked())).toBe(false))
    it('real = all gates passed', () => expect(areAllGatesPassed(makeRealGen())).toBe(true))
  })

  describe('E. Summary', () => {
    it('includes status', () => expect(getLiveGenSummary(makeFakeGen())).toContain('Sahte'))
    it('includes mode', () => expect(getLiveGenSummary(makeFakeGen())).toContain('CI'))
    it('includes kill switch', () => expect(getLiveGenSummary(makeBlocked())).toContain('AKTIF'))
    it('includes counts', () => expect(getLiveGenSummary(makeFakeGen())).toContain('1/1'))
  })

  describe('F. Forbidden Sweep', () => {
    it('summary no forbidden', () => {
      for (const f of FORBIDDEN_TEXT) expect(getLiveGenSummary(makeFakeGen())).not.toContain(f)
    })
    it('source no unsafe controls', async () => {
      const fs = await import('fs')
      const src = fs.readFileSync('src/components/production/LiveImageGenerationPreview.tsx', 'utf-8')
      for (const f of COMPONENT_FORBIDDEN) expect(src).not.toContain(f)
    })
  })

  describe('G. Type Safety', () => {
    it('no forbidden keys in result', () => {
      for (const f of ['token', 'secret', 'apiKey', 'password', 'filePath', 'base64', 'storageKey'])
        expect(Object.keys(makeFakeGen())).not.toContain(f)
    })
    it('no forbidden keys in image result', () => {
      for (const f of ['url', 'path', 'key', 'base64', 'binary', 'token', 'credential'])
        expect(Object.keys(makeFakeResult())).not.toContain(f)
    })
  })
})
