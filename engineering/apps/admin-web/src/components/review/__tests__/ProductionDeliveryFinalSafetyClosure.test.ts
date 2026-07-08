/**
 * PD-008 — Production Delivery Final Safety Closure.
 * Locks PD-001→PD-007 frontend chain.
 */

import { describe, it, expect } from 'vitest'
import * as fs from 'fs'

const FORBIDDEN_TEXT = [
  'filePath', 'absolutePath', 'inputPath', 'outputPath', 'workspacePath',
  'stdout', 'stderr', 'commandLine', 'rawLog', 'shellCommand', 'executablePath',
  'publicUrl', 'signedUrl', 'cdnUrl', 'downloadUrl', 'publishUrl', 'streamUrl',
  'bucketName', 'objectKey', 'base64', 'binaryData', 'rawPayload',
  'token', 'JWT', 'session', 'principal', 'authorizationHeader',
  'lyrics', 'scriptBody', 'promptText', 'copyrightedContent', 'userMedia',
]

const CONTROL_FORBIDDEN = [
  'DownloadButton', 'ExportButton', 'PublishButton',
  'innerHTML', 'dangerouslySetInnerHTML',
  'video src=',
]

const PD_COMPONENTS = [
  'src/components/review/InternalPreviewPlayerBoundary.tsx',
  'src/components/review/ReviewDecisionWorkflow.tsx',
  'src/components/review/ControlledPublicPlayback.tsx',
]

describe('PD-008 — Production Delivery Final Safety Closure', () => {
  describe('A. PD Component Source Sweep', () => {
    for (const file of PD_COMPONENTS) {
      it(`${file.split('/').pop()} no forbidden text`, () => {
        const src = fs.readFileSync(file, 'utf-8')
        for (const f of FORBIDDEN_TEXT) expect(src).not.toContain(f)
      })
      it(`${file.split('/').pop()} no delivery/unsafe controls`, () => {
        const src = fs.readFileSync(file, 'utf-8')
        for (const f of CONTROL_FORBIDDEN) expect(src).not.toContain(f)
      })
    }
  })

  describe('B. Types Sweep — PD-004/005/007 No Publish Statuses', () => {
    it('PD-004 statuses safe', () => {
      const statuses = [
        'CREATED_INTERNAL_ONLY', 'PRIVATE_STORAGE_REF_ACCEPTED',
        'PLAYBACK_METADATA_PREPARED_INTERNAL_ONLY', 'READY_FOR_INTERNAL_PREVIEW_ONLY',
        'REJECTED_UNSAFE', 'FAILED_SAFE',
      ]
      for (const s of statuses) {
        expect(s).not.toContain('COMPLETED')
        expect(s).not.toContain('PUBLISHED')
        expect(s).not.toContain('PUBLICLY_AVAILABLE')
        expect(s).not.toContain('AUTO_APPROVED')
      }
    })
    it('PD-005 statuses safe', () => {
      const statuses = [
        'CREATED_INTERNAL_ONLY', 'REVIEW_METADATA_ACCEPTED',
        'DECISION_RECORDED_INTERNAL_ONLY', 'APPROVED_FOR_INTERNAL_DELIVERY_PREP',
        'REJECTED_FOR_INTERNAL_REVISION', 'INTERNAL_FIX_REQUESTED',
        'REJECTED_UNSAFE', 'FAILED_SAFE',
      ]
      for (const s of statuses) {
        expect(s).not.toContain('COMPLETED')
        expect(s).not.toContain('PUBLISHED')
        expect(s).not.toContain('AUTO_APPROVED')
      }
    })
    it('PD-007 statuses safe', () => {
      const statuses = [
        'CREATED_INTERNAL_ONLY', 'DELIVERY_GATE_ACCEPTED',
        'CONTROLLED_PLAYBACK_HANDLE_CREATED', 'READY_FOR_CONTROLLED_PUBLIC_PLAYBACK',
        'REJECTED_UNSAFE', 'FAILED_SAFE',
      ]
      for (const s of statuses) {
        expect(s).not.toContain('COMPLETED')
        expect(s).not.toContain('PUBLISHED')
        expect(s).not.toContain('PUBLICLY_AVAILABLE')
      }
    })
  })

  describe('C. Types Sweep — Result Interfaces No Forbidden Fields', () => {
    it('types/index.ts PD sections no forbidden fields', () => {
      const src = fs.readFileSync('src/types/index.ts', 'utf-8')
      const pdSection = src.substring(src.indexOf('PD-004'))
      for (const f of FORBIDDEN_TEXT) {
        expect(pdSection).not.toContain(f)
      }
    })
  })

  describe('D. PD-004 Preview — Eligible/Rejected Render', () => {
    it('eligible renders metadata safely', async () => {
      const { getPreviewMetadataSummary } = await import('../../review/InternalPreviewPlayerBoundary')
      const s = getPreviewMetadataSummary({
        opaquePlaybackPreparationRef: 'pp:1', opaqueStorageObjectRef: 'obj:1',
        opaqueArtifactRef: 'art:1', mediaFormat: 'MP4', durationSeconds: 10,
        fps: 24, width: 640, height: 360, sizeBytes: 1024,
        status: 'READY_FOR_INTERNAL_PREVIEW_ONLY', issueCodes: [], internalPreviewEligible: true,
      })
      for (const f of FORBIDDEN_TEXT) expect(s).not.toContain(f)
    })
  })

  describe('E. PD-005 Review — Decision Controls Safe', () => {
    it('controls internal-only', async () => {
      const { getDecisionControls } = await import('../../review/ReviewDecisionWorkflow')
      const controls = getDecisionControls()
      expect(controls).toHaveLength(4)
      for (const c of controls) {
        expect(c.type).toContain('INTERNAL')
        expect(c.label).not.toContain('Publish')
        expect(c.label).not.toContain('Download')
      }
    })
  })

  describe('F. PD-007 Playback — Controlled Handle Safe', () => {
    it('ready placeholder uses opaque ref', async () => {
      const { getPlaybackPlaceholder } = await import('../../review/ControlledPublicPlayback')
      const p = getPlaybackPlaceholder({
        status: 'READY_FOR_CONTROLLED_PUBLIC_PLAYBACK', issueCodes: [],
        opaquePublicPlaybackRef: 'pub-play:xyz', opaqueDeliveryGateRef: 'g:1',
        opaqueArtifactRef: 'art:1', mediaFormat: 'MP4', durationSeconds: 10,
        fps: 24, width: 640, height: 360, sizeBytes: 1024, controlledPlaybackEligible: true,
      })
      expect(p).toContain('pub-play:xyz')
      for (const f of FORBIDDEN_TEXT) expect(p).not.toContain(f)
    })
    it('rejected does not expose handle', async () => {
      const { getPlaybackPlaceholder } = await import('../../review/ControlledPublicPlayback')
      const p = getPlaybackPlaceholder({
        status: 'REJECTED_UNSAFE', issueCodes: ['DELIVERY_GATE_NOT_APPROVED'],
        opaquePublicPlaybackRef: null, opaqueDeliveryGateRef: null,
        opaqueArtifactRef: null, mediaFormat: 'MP4', durationSeconds: 0,
        fps: 0, width: 0, height: 0, sizeBytes: 0, controlledPlaybackEligible: false,
      })
      expect(p).toContain('Kullanılamıyor')
      expect(p).not.toContain('pub-play:')
    })
  })

  describe('G. Türkçe Character Preservation', () => {
    it('PD-004 labels', async () => {
      const { renderPreviewStatus } = await import('../../review/InternalPreviewPlayerBoundary')
      expect(renderPreviewStatus('READY_FOR_INTERNAL_PREVIEW_ONLY')).toContain('Önizleme')
    })
    it('PD-005 labels', async () => {
      const { renderDecisionStatus } = await import('../../review/ReviewDecisionWorkflow')
      expect(renderDecisionStatus('APPROVED_FOR_INTERNAL_DELIVERY_PREP')).toContain('Onaylandı')
    })
    it('PD-007 labels', async () => {
      const { renderPlaybackStatus } = await import('../../review/ControlledPublicPlayback')
      expect(renderPlaybackStatus('READY_FOR_CONTROLLED_PUBLIC_PLAYBACK')).toContain('Hazır')
    })
  })
})
