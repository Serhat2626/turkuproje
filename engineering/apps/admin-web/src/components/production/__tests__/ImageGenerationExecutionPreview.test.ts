/**
 * RPE-006: Image Generation Execution Preview UI Tests.
 */

import { describe, it, expect } from 'vitest'
import {
  renderGenerationStatus,
  renderExecutionMode,
  renderFakeResult,
  getGenerationSummary,
  isRealProviderEnabled,
} from '../ImageGenerationExecutionPreview'
import type { TurkuImageGenerationExecutionResult, TurkuFakeImageResultMetadata } from '../../../types'

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

function makeFakeResult(): TurkuFakeImageResultMetadata {
  return { sceneNo: 1, opaqueArtifactRef: 'fimg:abc', artifactLabel: 'Sahne 1 sahte', formatHint: 'PNG', widthHint: 1920, heightHint: 1080, generationNote: 'CI_FAKE' }
}

function makeCiFake(): TurkuImageGenerationExecutionResult {
  return {
    status: 'FAKE_RESULT_AVAILABLE',
    issues: ['PROVIDER_EXECUTION_DISABLED'],
    opaqueGenerationRef: 'igen:abc', opaquePromptPackageRef: 'ipkg:def',
    executionMode: 'CI_FAKE',
    providerDisabledReason: 'Gercek saglayici devre disi',
    manualGatePassed: false,
    fakeResults: [makeFakeResult(), makeFakeResult()],
    totalScenes: 2, fakeResultCount: 2,
    validationSummary: '2 sahte gorsel uretildi',
  }
}

function makeDisabled(): TurkuImageGenerationExecutionResult {
  return { ...makeCiFake(), status: 'BLOCKED_BY_SAFETY', executionMode: 'DISABLED', fakeResults: [], fakeResultCount: 0 }
}

describe('RPE-006 — Image Generation Execution Preview', () => {
  describe('A. Status Rendering', () => {
    it('fake result', () => expect(renderGenerationStatus('FAKE_RESULT_AVAILABLE')).toContain('Sahte'))
    it('blocked', () => expect(renderGenerationStatus('BLOCKED_BY_SAFETY')).toContain('Engel'))
    it('waiting', () => expect(renderGenerationStatus('WAITING_FOR_MANUAL_GATE')).toContain('Bekleniyor'))
    it('not started', () => expect(renderGenerationStatus('NOT_STARTED')).toContain('Baslamadi'))
  })

  describe('B. Execution Mode', () => {
    it('CI_FAKE', () => expect(renderExecutionMode('CI_FAKE')).toContain('Sahte'))
    it('DISABLED', () => expect(renderExecutionMode('DISABLED')).toContain('Devre'))
    it('LOCAL_MANUAL', () => expect(renderExecutionMode('LOCAL_MANUAL_ONLY')).toContain('Manuel'))
  })

  describe('C. Real Provider Check', () => {
    it('CI_FAKE = not enabled', () => expect(isRealProviderEnabled(makeCiFake())).toBe(false))
    it('DISABLED = not enabled', () => expect(isRealProviderEnabled(makeDisabled())).toBe(false))
  })

  describe('D. Fake Result', () => {
    it('renders scene', () => expect(renderFakeResult(makeFakeResult())).toContain('Sahne 1'))
    it('renders format', () => expect(renderFakeResult(makeFakeResult())).toContain('PNG'))
    it('renders resolution', () => expect(renderFakeResult(makeFakeResult())).toContain('1920x1080'))
  })

  describe('E. Summary', () => {
    it('includes status', () => expect(getGenerationSummary(makeCiFake())).toContain('Sahte'))
    it('includes mode', () => expect(getGenerationSummary(makeCiFake())).toContain('CI'))
    it('includes counts', () => expect(getGenerationSummary(makeCiFake())).toContain('2/2'))
  })

  describe('F. Forbidden Sweep', () => {
    it('summary no forbidden', () => {
      for (const f of FORBIDDEN_TEXT) expect(getGenerationSummary(makeCiFake())).not.toContain(f)
    })
    it('source no unsafe controls', async () => {
      const fs = await import('fs')
      const src = fs.readFileSync('src/components/production/ImageGenerationExecutionPreview.tsx', 'utf-8')
      for (const f of COMPONENT_FORBIDDEN) expect(src).not.toContain(f)
    })
  })

  describe('G. Type Safety', () => {
    it('no forbidden keys in result', () => {
      for (const f of ['token', 'secret', 'filePath', 'publicUrl', 'storageKey', 'base64', 'lyrics'])
        expect(Object.keys(makeCiFake())).not.toContain(f)
    })
    it('no forbidden keys in fake result', () => {
      for (const f of ['url', 'path', 'key', 'base64', 'binary', 'token'])
        expect(Object.keys(makeFakeResult())).not.toContain(f)
    })
  })
})
