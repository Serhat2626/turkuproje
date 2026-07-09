/**
 * LIVE-004: Render Execution Preview UI Tests.
 */

import { describe, it, expect } from 'vitest'
import {
  renderRenderStatus,
  renderRenderMode,
  renderRenderArtifact,
  getRenderSummary,
  isRealRender,
  areRenderGatesPassed,
} from '../LiveRenderExecutionPreview'
import type { TurkuLiveRenderResult, TurkuLiveRenderArtifact } from '../../../types'

const FORBIDDEN_TEXT = [
  'filePath', 'absolutePath', 'stdout', 'stderr', 'commandLine', 'rawLog',
  'publicUrl', 'signedUrl', 'cdnUrl', 'downloadUrl', 'storageKey', 'base64',
  'bucketName', 'objectKey', 'command', 'args', 'processBuilder',
  'token', 'JWT', 'session', 'principal', 'apiKey', 'credential',
  'providerPrompt', 'rawPrompt', 'providerPayload',
]

const COMPONENT_FORBIDDEN = [
  'DownloadButton', 'ExportButton', 'PublishButton', 'ApproveButton',
  'OpenButton', 'PlayButton',
  'innerHTML', 'dangerouslySetInnerHTML',
]

function makeArtifact(real: boolean): TurkuLiveRenderArtifact {
  return { opaqueRenderRef: real ? 'rrnd:abc' : 'frnd:abc', artifactLabel: real ? 'Gercek render' : 'Sahte render', formatHint: 'MP4', durationSecondsHint: 40, resolutionHint: '1920x1080', sizeLabel: '~25 MB', verified: true, verificationNote: 'OK', humanReviewRequired: real }
}

function makeFake(): TurkuLiveRenderResult {
  return {
    status: 'FAKE_RENDER_AVAILABLE', mode: 'CI_FAKE', issues: [],
    opaqueRenderJobRef: 'lrjb:abc', opaqueStorageRef: 'lstr:def',
    killSwitchActive: false, operatorGatePassed: false, artifactsReady: true,
    renderArtifact: makeArtifact(false), inputSceneCount: 4,
    blockedReasons: [], sanitizedRenderSummary: 'Sahte render',
    validationSummary: 'Sahte render basarili',
  }
}

function makeReal(): TurkuLiveRenderResult {
  return { ...makeFake(), status: 'REAL_RENDER_AVAILABLE_INTERNAL', mode: 'LOCAL_MANUAL_REAL_FFMPEG', operatorGatePassed: true, renderArtifact: makeArtifact(true) }
}

function makeBlocked(): TurkuLiveRenderResult {
  return { ...makeFake(), status: 'BLOCKED_BY_KILL_SWITCH', killSwitchActive: true, renderArtifact: null }
}

describe('LIVE-004 — Render Execution Preview', () => {
  describe('A. Status Rendering', () => {
    it('fake', () => expect(renderRenderStatus('FAKE_RENDER_AVAILABLE')).toContain('Sahte'))
    it('real', () => expect(renderRenderStatus('REAL_RENDER_AVAILABLE_INTERNAL')).toContain('Gercek'))
    it('kill switch', () => expect(renderRenderStatus('BLOCKED_BY_KILL_SWITCH')).toContain('Kill'))
    it('operator gate', () => expect(renderRenderStatus('BLOCKED_BY_OPERATOR_GATE')).toContain('Operator'))
    it('artifact readiness', () => expect(renderRenderStatus('BLOCKED_BY_ARTIFACT_READINESS')).toContain('Dogrulama'))
  })

  describe('B. Mode Rendering', () => {
    it('CI_FAKE', () => expect(renderRenderMode('CI_FAKE')).toContain('Sahte'))
    it('LOCAL_MANUAL', () => expect(renderRenderMode('LOCAL_MANUAL_REAL_FFMPEG')).toContain('FFmpeg'))
  })

  describe('C. Artifact Rendering', () => {
    it('verified shows green', () => expect(renderRenderArtifact(makeArtifact(false))).toContain('\u2705'))
    it('real needs review', () => expect(renderRenderArtifact(makeArtifact(true))).toContain('Inceleme'))
    it('includes format', () => expect(renderRenderArtifact(makeArtifact(false))).toContain('MP4'))
  })

  describe('D. Gate Checks', () => {
    it('fake = not real', () => expect(isRealRender(makeFake())).toBe(false))
    it('real = real', () => expect(isRealRender(makeReal())).toBe(true))
    it('blocked = gates not passed', () => expect(areRenderGatesPassed(makeBlocked())).toBe(false))
    it('real = all gates passed', () => expect(areRenderGatesPassed(makeReal())).toBe(true))
  })

  describe('E. Summary', () => {
    it('includes status', () => expect(getRenderSummary(makeFake())).toContain('Sahte'))
    it('includes mode', () => expect(getRenderSummary(makeFake())).toContain('CI'))
    it('includes scene count', () => expect(getRenderSummary(makeFake())).toContain('4'))
  })

  describe('F. Forbidden Sweep', () => {
    it('summary no forbidden', () => {
      for (const f of FORBIDDEN_TEXT) expect(getRenderSummary(makeFake())).not.toContain(f)
    })
    it('source no unsafe controls', async () => {
      const fs = await import('fs')
      const src = fs.readFileSync('src/components/production/LiveRenderExecutionPreview.tsx', 'utf-8')
      for (const f of COMPONENT_FORBIDDEN) expect(src).not.toContain(f)
    })
  })

  describe('G. Type Safety', () => {
    it('no forbidden keys in result', () => {
      for (const f of ['command', 'args', 'stdout', 'stderr', 'filePath', 'storageKey', 'base64', 'processBuilder'])
        expect(Object.keys(makeFake())).not.toContain(f)
    })
    it('no forbidden keys in artifact', () => {
      for (const f of ['url', 'path', 'key', 'base64', 'binary', 'command', 'stdout', 'stderr'])
        expect(Object.keys(makeArtifact(false))).not.toContain(f)
    })
  })
})
