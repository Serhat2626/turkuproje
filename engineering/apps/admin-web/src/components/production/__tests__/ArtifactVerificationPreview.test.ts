/**
 * RPE-007: Artifact Verification + Render Package Preview UI Tests.
 */

import { describe, it, expect } from 'vitest'
import {
  renderVerificationStatus,
  renderRenderPackageStatus,
  renderSceneReadiness,
  getVerificationSummary,
  getRenderPackageSummary,
  isLocalRenderEnabled,
} from '../ArtifactVerificationPreview'
import type { TurkuArtifactVerificationReport, TurkuRenderPackageResult, TurkuSceneArtifactReadiness } from '../../../types'

const FORBIDDEN_TEXT = [
  'filePath', 'absolutePath', 'stdout', 'stderr', 'commandLine', 'rawLog',
  'publicUrl', 'signedUrl', 'cdnUrl', 'downloadUrl', 'storageKey', 'base64',
  'token', 'JWT', 'session', 'principal', 'apiKey', 'credential',
  'lyrics', 'scriptBody', 'providerPrompt', 'rawPrompt', 'providerPayload',
]

const COMPONENT_FORBIDDEN = [
  'DownloadButton', 'ExportButton', 'PublishButton', 'ApproveButton',
  'RenderButton', 'GenerateButton', 'ExecuteButton', 'FfmpegButton',
  'innerHTML', 'dangerouslySetInnerHTML',
]

function makeScene(verified: boolean): TurkuSceneArtifactReadiness {
  return { sceneNo: 1, opaqueArtifactRef: 'fimg:abc', artifactLabel: 'Sahne 1 sahte', verified, verificationNote: verified ? 'Basarili' : 'Bekleniyor' }
}

function makeVerified(): TurkuArtifactVerificationReport {
  return {
    status: 'FAKE_VERIFIED', opaqueVerificationRef: 'avrf:abc', opaqueGenerationRef: 'igen:def',
    sceneReadiness: [makeScene(true), makeScene(true)],
    totalScenes: 2, verifiedScenes: 2, validationSummary: '2/2 dogrulandi',
  }
}

function makeRenderPkg(): TurkuRenderPackageResult {
  return {
    status: 'WAITING_FOR_MANUAL_GATE', opaqueRenderPackageRef: 'rpkg:abc', opaqueVerificationRef: 'avrf:def',
    renderInputManifestSummary: '2 sahne gorseli',
    renderPrerequisiteLabels: ['FFmpeg ortami', 'Telif kontrolu'],
    localManualRenderEnabled: false, localManualRenderNote: 'RPE-007: devre disi',
    blockedReasons: ['FFmpeg ortami yapilandirilmadi'], validationSummary: 'Taslak',
  }
}

describe('RPE-007 — Artifact Verification + Render Package Preview', () => {
  describe('A. Verification Status', () => {
    it('fake verified', () => expect(renderVerificationStatus('FAKE_VERIFIED')).toContain('Sahte'))
    it('blocked', () => expect(renderVerificationStatus('BLOCKED_BY_SAFETY')).toContain('Engel'))
    it('not verified', () => expect(renderVerificationStatus('NOT_VERIFIED')).toContain('Dogrulanmadi'))
  })

  describe('B. Render Package Status', () => {
    it('waiting', () => expect(renderRenderPackageStatus('WAITING_FOR_MANUAL_GATE')).toContain('Bekleniyor'))
    it('not ready', () => expect(renderRenderPackageStatus('NOT_READY')).toContain('Degil'))
  })

  describe('C. Scene Readiness', () => {
    it('verified shows green', () => expect(renderSceneReadiness(makeScene(true))).toContain('\u2705'))
    it('not verified shows red', () => expect(renderSceneReadiness(makeScene(false))).toContain('\u274c'))
  })

  describe('D. Verification Summary', () => {
    it('includes status', () => expect(getVerificationSummary(makeVerified())).toContain('Sahte'))
    it('includes counts', () => expect(getVerificationSummary(makeVerified())).toContain('2/2'))
  })

  describe('E. Render Package Summary', () => {
    it('includes status', () => expect(getRenderPackageSummary(makeRenderPkg())).toContain('Bekleniyor'))
    it('local render disabled', () => expect(getRenderPackageSummary(makeRenderPkg())).toContain('Devre disi'))
    it('isLocalRenderEnabled false', () => expect(isLocalRenderEnabled(makeRenderPkg())).toBe(false))
  })

  describe('F. Forbidden Sweep', () => {
    it('verification no forbidden', () => {
      for (const f of FORBIDDEN_TEXT) expect(getVerificationSummary(makeVerified())).not.toContain(f)
    })
    it('render pkg no forbidden', () => {
      for (const f of FORBIDDEN_TEXT) expect(getRenderPackageSummary(makeRenderPkg())).not.toContain(f)
    })
    it('source no unsafe controls', async () => {
      const fs = await import('fs')
      const src = fs.readFileSync('src/components/production/ArtifactVerificationPreview.tsx', 'utf-8')
      for (const f of COMPONENT_FORBIDDEN) expect(src).not.toContain(f)
    })
  })

  describe('G. Type Safety', () => {
    it('no forbidden keys in verification', () => {
      for (const f of ['token', 'secret', 'filePath', 'publicUrl', 'storageKey', 'base64'])
        expect(Object.keys(makeVerified())).not.toContain(f)
    })
    it('no forbidden keys in render pkg', () => {
      for (const f of ['url', 'path', 'key', 'command', 'stdout', 'stderr'])
        expect(Object.keys(makeRenderPkg())).not.toContain(f)
    })
  })
})
