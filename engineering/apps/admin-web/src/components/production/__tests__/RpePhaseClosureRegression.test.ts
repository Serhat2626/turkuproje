/**
 * RPE-008: Real Production Execution MVP Final Closure — Admin UI Regression.
 */

import { describe, it, expect } from 'vitest'
import { renderGateStatus } from '../ExecutionGatePreview'
import { renderConfigStatus as renderProviderConfigStatus } from '../ProviderConfigPreview'
import { renderRenderConfigStatus } from '../RenderPipelineConfigPreview'
import { renderPackageStatus, isAnyCapabilityEnabled } from '../ExecutionPackagePreview'
import { renderPromptPackageStatus, isManualApprovalGranted } from '../ImagePromptPackagePreview'
import { renderGenerationStatus, renderExecutionMode, isRealProviderEnabled } from '../ImageGenerationExecutionPreview'
import { renderVerificationStatus, renderRenderPackageStatus, isLocalRenderEnabled } from '../ArtifactVerificationPreview'

const ALL_COMPONENT_FILES = [
  'src/components/production/ExecutionGatePreview.tsx',
  'src/components/production/ProviderConfigPreview.tsx',
  'src/components/production/RenderPipelineConfigPreview.tsx',
  'src/components/production/ExecutionPackagePreview.tsx',
  'src/components/production/ImagePromptPackagePreview.tsx',
  'src/components/production/ImageGenerationExecutionPreview.tsx',
  'src/components/production/ArtifactVerificationPreview.tsx',
]

const FORBIDDEN_CONTROLS = [
  'DownloadButton', 'ExportButton', 'PublishButton', 'ApproveButton',
  'RenderButton', 'GenerateButton', 'ExecuteButton', 'FfmpegButton',
  'innerHTML', 'dangerouslySetInnerHTML',
]

const FORBIDDEN_TEXT = [
  'filePath', 'absolutePath', 'stdout', 'stderr', 'commandLine', 'rawLog',
  'publicUrl', 'signedUrl', 'cdnUrl', 'downloadUrl', 'storageKey', 'base64',
  'token', 'JWT', 'session', 'principal', 'apiKey', 'credential',
  'lyrics', 'scriptBody', 'providerPrompt', 'rawPrompt', 'providerPayload',
]

describe('RPE-008 — Final Closure Admin UI Regression', () => {
  describe('A. All Previews Render Safely', () => {
    it('RPE-001 gate', () => expect(renderGateStatus('NOT_ELIGIBLE')).toBeTruthy())
    it('RPE-002 provider', () => expect(renderProviderConfigStatus('NOT_CONFIGURED')).toBeTruthy())
    it('RPE-003 render config', () => expect(renderRenderConfigStatus('NOT_CONFIGURED')).toBeTruthy())
    it('RPE-004 exec package', () => expect(renderPackageStatus('WAITING_FOR_MANUAL_GATE')).toBeTruthy())
    it('RPE-005 prompt package', () => expect(renderPromptPackageStatus('PENDING_MANUAL_APPROVAL')).toBeTruthy())
    it('RPE-006 generation', () => expect(renderGenerationStatus('FAKE_RESULT_AVAILABLE')).toBeTruthy())
    it('RPE-006 mode', () => expect(renderExecutionMode('CI_FAKE')).toBeTruthy())
    it('RPE-007 verification', () => expect(renderVerificationStatus('FAKE_VERIFIED')).toBeTruthy())
    it('RPE-007 render pkg', () => expect(renderRenderPackageStatus('WAITING_FOR_MANUAL_GATE')).toBeTruthy())
  })

  describe('B. Disabled/Manual States Visible', () => {
    it('exec pkg capabilities disabled', () => {
      const pkg = {
        status: 'WAITING_FOR_MANUAL_GATE' as const,
        blockedReasons: [], opaquePackageRef: 'epkg:x', opaqueWorkOrderRef: 'wo:x',
        safeTurkuTitleLabel: 'Test', providerConfigSummary: 'x', renderConfigSummary: 'x',
        manualGates: [], executionCapabilities: [{ capabilityLabel: 'Test', enabled: false, disabledReason: 'RPE' }],
        operatorExecutionSummary: 'x', futureExecutionStepLabels: [], validationSummary: 'x',
      }
      expect(isAnyCapabilityEnabled(pkg)).toBe(false)
    })
    it('prompt approval not granted', () => {
      const pkg = {
        status: 'PENDING_MANUAL_APPROVAL' as const,
        issues: [], opaquePromptPackageRef: 'ipkg:x', opaqueExecutionPackageRef: 'epkg:x',
        safeTurkuTitleLabel: 'Test', scenePrompts: [], totalScenes: 0, blockedScenes: 0,
        manualApprovalGranted: false, approvalGateLabel: 'x', validationSummary: 'x',
      }
      expect(isManualApprovalGranted(pkg)).toBe(false)
    })
    it('real provider not enabled', () => {
      const gen = {
        status: 'FAKE_RESULT_AVAILABLE' as const,
        issues: [], opaqueGenerationRef: 'igen:x', opaquePromptPackageRef: 'ipkg:x',
        executionMode: 'CI_FAKE' as const, providerDisabledReason: 'x',
        manualGatePassed: false, fakeResults: [], totalScenes: 0, fakeResultCount: 0,
        validationSummary: 'x',
      }
      expect(isRealProviderEnabled(gen)).toBe(false)
    })
    it('local render not enabled', () => {
      const rpkg = {
        status: 'WAITING_FOR_MANUAL_GATE' as const,
        opaqueRenderPackageRef: 'rpkg:x', opaqueVerificationRef: 'avrf:x',
        renderInputManifestSummary: 'x', renderPrerequisiteLabels: [],
        localManualRenderEnabled: false, localManualRenderNote: 'x',
        blockedReasons: [], validationSummary: 'x',
      }
      expect(isLocalRenderEnabled(rpkg)).toBe(false)
    })
  })

  describe('C. Source File Safety Sweep', () => {
    it('no unsafe controls in any RPE component', async () => {
      const fs = await import('fs')
      for (const file of ALL_COMPONENT_FILES) {
        const src = fs.readFileSync(file, 'utf-8')
        for (const f of FORBIDDEN_CONTROLS)
          expect(src).not.toContain(f)
      }
    })
    it('no forbidden text in status labels', () => {
      const allLabels = [
        renderGateStatus('NOT_ELIGIBLE'), renderGateStatus('BLOCKED_BY_SAFETY'),
        renderProviderConfigStatus('NOT_CONFIGURED'), renderProviderConfigStatus('BLOCKED_BY_SAFETY'),
        renderRenderConfigStatus('NOT_CONFIGURED'), renderRenderConfigStatus('BLOCKED_BY_SAFETY'),
        renderPackageStatus('WAITING_FOR_MANUAL_GATE'), renderPackageStatus('BLOCKED_BY_SAFETY'),
        renderPromptPackageStatus('PENDING_MANUAL_APPROVAL'), renderPromptPackageStatus('BLOCKED_BY_SAFETY'),
        renderGenerationStatus('FAKE_RESULT_AVAILABLE'), renderGenerationStatus('BLOCKED_BY_SAFETY'),
        renderVerificationStatus('FAKE_VERIFIED'), renderVerificationStatus('BLOCKED_BY_SAFETY'),
        renderRenderPackageStatus('WAITING_FOR_MANUAL_GATE'), renderRenderPackageStatus('BLOCKED_BY_SAFETY'),
      ]
      for (const label of allLabels)
        for (const f of FORBIDDEN_TEXT)
          expect(label).not.toContain(f)
    })
  })
})
