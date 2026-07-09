/**
 * LIVE-003: Artifact Storage + Verification Preview UI Tests.
 */

import { describe, it, expect } from 'vitest'
import {
  renderStorageStatus,
  renderStoredArtifact,
  getStorageSummary,
  isAllVerified,
} from '../LiveArtifactStoragePreview'
import type { TurkuLiveStorageResult, TurkuLiveStoredArtifact } from '../../../types'

const FORBIDDEN_TEXT = [
  'filePath', 'absolutePath', 'stdout', 'stderr', 'commandLine', 'rawLog',
  'publicUrl', 'signedUrl', 'cdnUrl', 'downloadUrl', 'storageKey', 'base64',
  'bucketName', 'objectKey',
  'token', 'JWT', 'session', 'principal', 'apiKey', 'credential',
  'providerPrompt', 'rawPrompt', 'providerPayload',
]

const COMPONENT_FORBIDDEN = [
  'DownloadButton', 'ExportButton', 'PublishButton', 'ApproveButton',
  'RenderButton', 'FfmpegButton', 'OpenButton',
  'innerHTML', 'dangerouslySetInnerHTML',
]

function makeArtifact(verified: boolean): TurkuLiveStoredArtifact {
  return { sceneNo: 1, opaqueInternalRef: 'sart:abc', artifactLabel: 'Sahne 1 depo', formatHint: 'PNG', widthHint: 1920, heightHint: 1080, verified, verificationNote: verified ? 'OK' : 'Bekleniyor' }
}

function makeFakeStored(): TurkuLiveStorageResult {
  return {
    status: 'FAKE_STORED', issues: [],
    opaqueStorageRef: 'lstr:abc', opaqueGenerationRef: 'lfgen:def',
    storedArtifacts: [makeArtifact(true), makeArtifact(true)],
    totalScenes: 2, storedCount: 2, verifiedCount: 2,
    blockedReasons: [], validationSummary: '2/2 sahte depolandi',
  }
}

function makeBlocked(): TurkuLiveStorageResult {
  return { ...makeFakeStored(), status: 'BLOCKED_BY_SAFETY', storedArtifacts: [], storedCount: 0, verifiedCount: 0 }
}

describe('LIVE-003 — Artifact Storage + Verification Preview', () => {
  describe('A. Status Rendering', () => {
    it('fake stored', () => expect(renderStorageStatus('FAKE_STORED')).toContain('Sahte'))
    it('real stored', () => expect(renderStorageStatus('REAL_STORED_INTERNAL')).toContain('Gercek'))
    it('blocked', () => expect(renderStorageStatus('BLOCKED_BY_SAFETY')).toContain('Engel'))
    it('waiting', () => expect(renderStorageStatus('WAITING_FOR_MANUAL_GATE')).toContain('Bekleniyor'))
  })

  describe('B. Artifact Rendering', () => {
    it('verified shows green', () => expect(renderStoredArtifact(makeArtifact(true))).toContain('\u2705'))
    it('not verified shows red', () => expect(renderStoredArtifact(makeArtifact(false))).toContain('\u274c'))
    it('shows scene', () => expect(renderStoredArtifact(makeArtifact(true))).toContain('Sahne 1'))
  })

  describe('C. Verification Check', () => {
    it('all verified', () => expect(isAllVerified(makeFakeStored())).toBe(true))
    it('blocked = not verified', () => expect(isAllVerified(makeBlocked())).toBe(false))
  })

  describe('D. Summary', () => {
    it('includes status', () => expect(getStorageSummary(makeFakeStored())).toContain('Sahte'))
    it('includes counts', () => expect(getStorageSummary(makeFakeStored())).toContain('2/2'))
  })

  describe('E. Forbidden Sweep', () => {
    it('summary no forbidden', () => {
      for (const f of FORBIDDEN_TEXT) expect(getStorageSummary(makeFakeStored())).not.toContain(f)
    })
    it('source no unsafe controls', async () => {
      const fs = await import('fs')
      const src = fs.readFileSync('src/components/production/LiveArtifactStoragePreview.tsx', 'utf-8')
      for (const f of COMPONENT_FORBIDDEN) expect(src).not.toContain(f)
    })
  })

  describe('F. Type Safety', () => {
    it('no forbidden keys in result', () => {
      for (const f of ['token', 'secret', 'filePath', 'publicUrl', 'storageKey', 'base64', 'bucketName'])
        expect(Object.keys(makeFakeStored())).not.toContain(f)
    })
    it('no forbidden keys in artifact', () => {
      for (const f of ['url', 'path', 'key', 'base64', 'binary', 'bucket'])
        expect(Object.keys(makeArtifact(true))).not.toContain(f)
    })
  })
})
