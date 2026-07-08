/**
 * RPE-005: Image Prompt Package Preview UI Tests.
 */

import { describe, it, expect } from 'vitest'
import {
  renderPromptPackageStatus,
  isManualApprovalGranted,
  renderScenePrompt,
  getPromptPackageSummary,
  getSafetyConstraints,
} from '../ImagePromptPackagePreview'
import type { TurkuImagePromptPackageResult, TurkuSceneImagePrompt } from '../../../types'

const FORBIDDEN_TEXT = [
  'filePath', 'absolutePath', 'stdout', 'stderr', 'commandLine', 'rawLog',
  'publicUrl', 'signedUrl', 'cdnUrl', 'downloadUrl', 'storageKey', 'base64',
  'token', 'JWT', 'session', 'principal', 'apiKey', 'credential',
  'lyrics', 'scriptBody', 'providerPrompt', 'rawPrompt', 'providerPayload',
]

const COMPONENT_FORBIDDEN = [
  'DownloadButton', 'ExportButton', 'PublishButton', 'ApproveButton',
  'RenderButton', 'GenerateButton', 'ExecuteButton',
  'innerHTML', 'dangerouslySetInnerHTML',
]

function makePrompt(blocked: boolean): TurkuSceneImagePrompt {
  return {
    sceneNo: 1, safePromptSummary: 'Sahne 1: guvenli ozet', safeNegativeConstraints: 'siddet yok',
    safetyConstraintLabels: ['Telif yok', 'Siddet yok', 'Hassasiyet kontrol edildi'],
    aspectRatio: '16:9', styleHint: 'CINEMATIC', blocked, blockedReason: blocked ? 'Icerik engellendi' : null,
  }
}

function makePending(): TurkuImagePromptPackageResult {
  return {
    status: 'PENDING_MANUAL_APPROVAL',
    issues: ['OPERATOR_REVIEW_PENDING'],
    opaquePromptPackageRef: 'ipkg:abc', opaqueExecutionPackageRef: 'epkg:def',
    safeTurkuTitleLabel: 'Canakkale Turkusu',
    scenePrompts: [makePrompt(false), makePrompt(false)],
    totalScenes: 2, blockedScenes: 0, manualApprovalGranted: false,
    approvalGateLabel: 'Operator incelemesi bekleniyor',
    validationSummary: '2 sahne hazir',
  }
}

function makeBlocked(): TurkuImagePromptPackageResult {
  return { ...makePending(), status: 'BLOCKED_BY_SAFETY', issues: ['EXECUTION_PACKAGE_NOT_READY'] }
}

describe('RPE-005 — Image Prompt Package Preview', () => {
  describe('A. Status Rendering', () => {
    it('pending', () => expect(renderPromptPackageStatus('PENDING_MANUAL_APPROVAL')).toContain('Onay'))
    it('blocked', () => expect(renderPromptPackageStatus('BLOCKED_BY_SAFETY')).toContain('Engel'))
    it('draft', () => expect(renderPromptPackageStatus('DRAFT')).toContain('Taslak'))
  })

  describe('B. Manual Approval Gate', () => {
    it('pending = not granted', () => expect(isManualApprovalGranted(makePending())).toBe(false))
    it('blocked = not granted', () => expect(isManualApprovalGranted(makeBlocked())).toBe(false))
  })

  describe('C. Scene Prompt Rendering', () => {
    it('not blocked shows green', () => expect(renderScenePrompt(makePrompt(false))).toContain('\u2705'))
    it('blocked shows red', () => expect(renderScenePrompt(makePrompt(true))).toContain('\u274c'))
    it('shows aspect ratio', () => expect(renderScenePrompt(makePrompt(false))).toContain('16:9'))
  })

  describe('D. Summary', () => {
    it('includes status', () => expect(getPromptPackageSummary(makePending())).toContain('Onay'))
    it('includes title', () => expect(getPromptPackageSummary(makePending())).toContain('Canakkale'))
    it('includes scene counts', () => expect(getPromptPackageSummary(makePending())).toContain('2 toplam'))
  })

  describe('E. Safety Constraints', () => {
    it('listed', () => expect(getSafetyConstraints(makePrompt(false))).toContain('Telif'))
  })

  describe('F. Forbidden Sweep', () => {
    it('summary no forbidden', () => {
      for (const f of FORBIDDEN_TEXT) expect(getPromptPackageSummary(makePending())).not.toContain(f)
    })
    it('scene no forbidden', () => {
      for (const f of FORBIDDEN_TEXT) expect(renderScenePrompt(makePrompt(false))).not.toContain(f)
    })
    it('source no unsafe controls', async () => {
      const fs = await import('fs')
      const src = fs.readFileSync('src/components/production/ImagePromptPackagePreview.tsx', 'utf-8')
      for (const f of COMPONENT_FORBIDDEN) expect(src).not.toContain(f)
    })
  })

  describe('G. Type Safety', () => {
    it('no forbidden keys', () => {
      for (const f of ['token', 'secret', 'lyrics', 'scriptBody', 'providerPayload', 'base64'])
        expect(Object.keys(makePending())).not.toContain(f)
    })
  })
})
