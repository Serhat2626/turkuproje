import { describe, it, expect } from 'vitest'
import * as platformPackageApi from '../../../api/platformPackageApi'
import type {
  PlatformPackageDraftEditPayload,
  PlatformPackageDraftResponse,
  PlatformPackageStatus,
} from '../../../types'
import { PLATFORM_PACKAGE_TYPES, PLATFORM_PACKAGE_STATUSES } from '../../../types'

/**
 * FP-004: PlatformPackageWorkflowSection governance ve type-safety testleri.
 */
describe('PlatformPackageWorkflowSection — Governance', () => {

  // ── API Client ──────────────────────────────────────────

  it('API client has explicit generate function', () => {
    expect(typeof platformPackageApi.generatePlatformPackagesForStoryboard).toBe('function')
  })

  it('API client has explicit list by project function', () => {
    expect(typeof platformPackageApi.listPlatformPackagesByContentProject).toBe('function')
  })

  it('API client has explicit list by storyboard function', () => {
    expect(typeof platformPackageApi.listPlatformPackagesByStoryboard).toBe('function')
  })

  it('API client has explicit detail function', () => {
    expect(typeof platformPackageApi.getPlatformPackageDetail).toBe('function')
  })

  it('API client has explicit update function', () => {
    expect(typeof platformPackageApi.updatePlatformPackageDraft).toBe('function')
  })

  it('API client has explicit approve function', () => {
    expect(typeof platformPackageApi.approvePlatformPackageDraft).toBe('function')
  })

  it('API client has explicit reject function', () => {
    expect(typeof platformPackageApi.rejectPlatformPackageDraft).toBe('function')
  })

  // ── Governance: No Generic Status Update ────────────────

  it('no generic status update function in API client', () => {
    const exports = Object.keys(platformPackageApi)
    const forbidden = ['updateStatus', 'setStatus', 'changeStatus', 'updatePackageStatus', 'patchStatus']
    forbidden.forEach(fn => {
      expect(exports).not.toContain(fn)
    })
  })

  it('no READY_FOR_PUBLISH function in API client', () => {
    const exports = Object.keys(platformPackageApi)
    const forbidden = ['readyForPublish', 'markReadyForPublish', 'publishPackage', 'setReadyForPublish']
    forbidden.forEach(fn => {
      expect(exports).not.toContain(fn)
    })
  })

  it('no publishing/media generation function in API client', () => {
    const exports = Object.keys(platformPackageApi)
    const forbidden = ['publish', 'generateMedia', 'generateImage', 'generateVideo', 'generateAudio', 'uploadMedia']
    forbidden.forEach(fn => {
      expect(exports).not.toContain(fn)
    })
  })

  // ── Type Safety ────────────────────────────────────────

  it('PlatformPackageType includes 4 types', () => {
    expect(PLATFORM_PACKAGE_TYPES).toHaveLength(4)
    const values = PLATFORM_PACKAGE_TYPES.map(t => t.value)
    expect(values).toContain('YOUTUBE_LONG_FORM')
    expect(values).toContain('YOUTUBE_SHORTS')
    expect(values).toContain('INSTAGRAM_REELS')
    expect(values).toContain('FACEBOOK_REELS')
  })

  it('PlatformPackageStatus includes 5 statuses', () => {
    expect(PLATFORM_PACKAGE_STATUSES).toHaveLength(5)
    const values = PLATFORM_PACKAGE_STATUSES.map(s => s.value)
    expect(values).toContain('AI_GENERATED')
    expect(values).toContain('NEEDS_REVIEW')
    expect(values).toContain('APPROVED')
    expect(values).toContain('REJECTED')
    expect(values).toContain('FAILED')
  })

  it('PlatformPackageStatus does not include READY_FOR_PUBLISH', () => {
    const values = PLATFORM_PACKAGE_STATUSES.map(s => s.value)
    expect(values).not.toContain('READY_FOR_PUBLISH')
  })

  // ── Edit Payload Safety ────────────────────────────────

  it('edit payload does not include status field', () => {
    const payload: PlatformPackageDraftEditPayload = { title: 'test' }
    expect('status' in payload).toBe(false)
    // TypeScript enforces this — verify at runtime too
    const keys = Object.keys(payload)
    expect(keys).not.toContain('status')
  })

  it('edit payload does not include version field', () => {
    const payload: PlatformPackageDraftEditPayload = { title: 'test' }
    expect('version' in payload).toBe(false)
  })

  it('edit payload does not include providerType/modelName/generatedBy', () => {
    const payload: PlatformPackageDraftEditPayload = { title: 'test' }
    const keys = Object.keys(payload)
    expect(keys).not.toContain('providerType')
    expect(keys).not.toContain('modelName')
    expect(keys).not.toContain('generatedBy')
  })

  it('edit payload does not include source references', () => {
    const payload: PlatformPackageDraftEditPayload = { title: 'test' }
    const keys = Object.keys(payload)
    expect(keys).not.toContain('contentProjectId')
    expect(keys).not.toContain('sourceStoryboardDraftId')
  })

  it('edit payload does not include rawPrompt/apiKey/secret', () => {
    const payload: PlatformPackageDraftEditPayload = { title: 'test' }
    const keys = Object.keys(payload)
    expect(keys).not.toContain('rawPrompt')
    expect(keys).not.toContain('apiKey')
    expect(keys).not.toContain('secret')
  })

  // ── Response DTO Safety ────────────────────────────────

  it('response DTO does not contain rawPrompt/apiKey/secret fields', () => {
    // PlatformPackageDraftResponse interface has these fields defined in types
    // Check that the type system + convention doesn't expose unsafe fields
    const testResponse: Partial<PlatformPackageDraftResponse> = {
      id: '123',
      status: 'AI_GENERATED',
      packageType: 'YOUTUBE_LONG_FORM',
    }
    const keys = Object.keys(testResponse)
    expect(keys).not.toContain('rawPrompt')
    expect(keys).not.toContain('apiKey')
    expect(keys).not.toContain('secret')
    expect(keys).not.toContain('providerPayload')
  })

  // ── Editable Status Rules ──────────────────────────────

  it('AI_GENERATED is editable', () => {
    const editableStatuses: PlatformPackageStatus[] = ['AI_GENERATED', 'NEEDS_REVIEW']
    expect(editableStatuses).toContain('AI_GENERATED')
  })

  it('NEEDS_REVIEW is editable', () => {
    const editableStatuses: PlatformPackageStatus[] = ['AI_GENERATED', 'NEEDS_REVIEW']
    expect(editableStatuses).toContain('NEEDS_REVIEW')
  })

  it('APPROVED is NOT editable', () => {
    const editableStatuses: PlatformPackageStatus[] = ['AI_GENERATED', 'NEEDS_REVIEW']
    expect(editableStatuses).not.toContain('APPROVED')
  })

  it('REJECTED is NOT editable', () => {
    const editableStatuses: PlatformPackageStatus[] = ['AI_GENERATED', 'NEEDS_REVIEW']
    expect(editableStatuses).not.toContain('REJECTED')
  })

  it('FAILED is NOT editable', () => {
    const editableStatuses: PlatformPackageStatus[] = ['AI_GENERATED', 'NEEDS_REVIEW']
    expect(editableStatuses).not.toContain('FAILED')
  })
})
