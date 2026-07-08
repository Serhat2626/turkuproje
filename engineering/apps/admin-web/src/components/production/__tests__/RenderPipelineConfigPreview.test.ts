/**
 * RPE-003: Render Pipeline Configuration Preview UI Tests.
 */

import { describe, it, expect } from 'vitest'
import {
  renderRenderConfigStatus,
  isRenderExecutionEnabled,
  renderEnvironment,
  renderFfmpegCapability,
  renderOutputPolicy,
  getRenderConfigSummary,
  getSafetyPrerequisites,
} from '../RenderPipelineConfigPreview'
import type {
  TurkuRenderPipelineConfigResult,
  TurkuRenderEnvironmentBoundary,
  TurkuFfmpegCapabilityMetadata,
  TurkuOutputFormatPolicy,
} from '../../../types'

const FORBIDDEN_TEXT = [
  'filePath', 'absolutePath', 'stdout', 'stderr', 'commandLine', 'rawLog',
  'publicUrl', 'signedUrl', 'cdnUrl', 'downloadUrl', 'storageKey', 'base64',
  'token', 'JWT', 'session', 'principal', 'apiKey', 'credential',
  'lyrics', 'scriptBody', 'promptText', 'providerPrompt', 'rawPrompt',
]

const COMPONENT_FORBIDDEN = [
  'DownloadButton', 'ExportButton', 'PublishButton', 'ApproveButton',
  'RenderButton', 'GenerateButton', 'ExecuteButton', 'FfmpegButton',
  'innerHTML', 'dangerouslySetInnerHTML',
]

function makeEnv(configured: boolean): TurkuRenderEnvironmentBoundary {
  return { environmentLabel: 'Yerel ortam', environmentConfigured: configured, configurationStatusLabel: configured ? 'Hazir' : 'Yapilandirilmadi' }
}

function makeFfmpegCap(available: boolean): TurkuFfmpegCapabilityMetadata {
  return { capabilityLabel: 'Video birlestirme', available, availabilityNote: available ? '' : 'RPE-003: devre disi' }
}

function makeOutput(): TurkuOutputFormatPolicy {
  return { formatLabel: 'MP4', width: 1920, height: 1080, frameRate: 24, policyNote: 'Varsayilan' }
}

function makeNotConfigured(): TurkuRenderPipelineConfigResult {
  return {
    status: 'NOT_CONFIGURED',
    issues: ['RENDER_ENVIRONMENT_MISSING', 'FFMPEG_NOT_AVAILABLE'],
    opaqueRenderConfigRef: 'rcfg:abc',
    environmentBoundary: makeEnv(false),
    ffmpegCapabilities: [makeFfmpegCap(false), makeFfmpegCap(false)],
    outputFormatPolicy: makeOutput(),
    safetyPrerequisiteLabels: ['Saglayici yapilandirilmis olmali', 'Telif kontrolu tamamlanmis olmali'],
    renderExecutionEnabled: false,
    validationSummary: 'Render devre disi',
  }
}

describe('RPE-003 — Render Pipeline Config Preview', () => {
  describe('A. Status Rendering', () => {
    it('not configured', () => expect(renderRenderConfigStatus('NOT_CONFIGURED')).toContain('Yapilandirilmadi'))
    it('blocked', () => expect(renderRenderConfigStatus('BLOCKED_BY_SAFETY')).toContain('Engel'))
    it('waiting', () => expect(renderRenderConfigStatus('WAITING_FOR_OPERATOR_REVIEW')).toContain('Bekleniyor'))
    it('failed', () => expect(renderRenderConfigStatus('FAILED_SAFE')).toContain('Guvenli'))
  })

  describe('B. Execution Disabled', () => {
    it('not configured = disabled', () => expect(isRenderExecutionEnabled(makeNotConfigured())).toBe(false))
  })

  describe('C. Environment Boundary', () => {
    it('not configured shows red', () => expect(renderEnvironment(makeEnv(false))).toContain('\u274c'))
    it('configured shows green', () => expect(renderEnvironment(makeEnv(true))).toContain('\u2705'))
    it('null env', () => expect(renderEnvironment(null)).toContain('yok'))
  })

  describe('D. FFmpeg Capability', () => {
    it('unavailable shows red', () => expect(renderFfmpegCapability(makeFfmpegCap(false))).toContain('\u274c'))
    it('available shows green', () => expect(renderFfmpegCapability(makeFfmpegCap(true))).toContain('\u2705'))
    it('shows RPE-003', () => expect(renderFfmpegCapability(makeFfmpegCap(false))).toContain('RPE-003'))
  })

  describe('E. Output Policy', () => {
    it('shows resolution', () => expect(renderOutputPolicy(makeOutput())).toContain('1920x1080'))
    it('shows fps', () => expect(renderOutputPolicy(makeOutput())).toContain('24fps'))
    it('null policy', () => expect(renderOutputPolicy(null)).toContain('belirlenmedi'))
  })

  describe('F. Summary', () => {
    it('includes status', () => expect(getRenderConfigSummary(makeNotConfigured())).toContain('Yapilandirilmadi'))
    it('includes validation', () => expect(getRenderConfigSummary(makeNotConfigured())).toContain('devre disi'))
  })

  describe('G. Safety Prerequisites', () => {
    it('listed', () => expect(getSafetyPrerequisites(makeNotConfigured())).toContain('Saglayici'))
  })

  describe('H. Forbidden Sweep', () => {
    it('summary no forbidden', () => {
      for (const f of FORBIDDEN_TEXT) expect(getRenderConfigSummary(makeNotConfigured())).not.toContain(f)
    })
    it('source no unsafe controls', async () => {
      const fs = await import('fs')
      const src = fs.readFileSync('src/components/production/RenderPipelineConfigPreview.tsx', 'utf-8')
      for (const f of COMPONENT_FORBIDDEN) expect(src).not.toContain(f)
    })
  })

  describe('I. Type Safety', () => {
    it('no forbidden keys', () => {
      for (const f of ['token', 'secret', 'filePath', 'commandLine', 'stdout', 'stderr'])
        expect(Object.keys(makeNotConfigured())).not.toContain(f)
    })
  })
})
