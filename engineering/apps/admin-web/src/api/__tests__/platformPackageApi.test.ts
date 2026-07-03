import { describe, it, expect } from 'vitest'
import * as platformPackageApi from '../platformPackageApi'

/**
 * FP-004: Platform Package API client testleri.
 *
 * Kontrol noktaları:
 * - Sadece explicit action function'lar mevcut
 * - Generic status update function yok
 * - READY_FOR_PUBLISH function yok
 * - Publishing/media generation function yok
 */

describe('platformPackageApi', () => {
  // ── Exported Functions ──────────────────────────────────

  it('generatePlatformPackagesForStoryboard fonksiyonu mevcut', () => {
    expect(typeof platformPackageApi.generatePlatformPackagesForStoryboard).toBe('function')
  })

  it('listPlatformPackagesByContentProject fonksiyonu mevcut', () => {
    expect(typeof platformPackageApi.listPlatformPackagesByContentProject).toBe('function')
  })

  it('listPlatformPackagesByStoryboard fonksiyonu mevcut', () => {
    expect(typeof platformPackageApi.listPlatformPackagesByStoryboard).toBe('function')
  })

  it('getPlatformPackageDetail fonksiyonu mevcut', () => {
    expect(typeof platformPackageApi.getPlatformPackageDetail).toBe('function')
  })

  it('updatePlatformPackageDraft fonksiyonu mevcut', () => {
    expect(typeof platformPackageApi.updatePlatformPackageDraft).toBe('function')
  })

  it('approvePlatformPackageDraft fonksiyonu mevcut', () => {
    expect(typeof platformPackageApi.approvePlatformPackageDraft).toBe('function')
  })

  it('rejectPlatformPackageDraft fonksiyonu mevcut', () => {
    expect(typeof platformPackageApi.rejectPlatformPackageDraft).toBe('function')
  })

  // ── Governance ──────────────────────────────────────────

  it('generic status update function yok', () => {
    const exports = Object.keys(platformPackageApi)
    const forbidden = ['updateStatus', 'setStatus', 'changeStatus', 'updatePackageStatus', 'patchStatus']
    forbidden.forEach(fn => {
      expect(exports).not.toContain(fn)
    })
  })

  it('READY_FOR_PUBLISH function yok', () => {
    const exports = Object.keys(platformPackageApi)
    const forbidden = ['readyForPublish', 'markReadyForPublish', 'publishPackage', 'setReadyForPublish']
    forbidden.forEach(fn => {
      expect(exports).not.toContain(fn)
    })
  })

  it('publishing/media generation function yok', () => {
    const exports = Object.keys(platformPackageApi)
    const forbidden = ['publish', 'generateMedia', 'generateImage', 'generateVideo', 'generateAudio', 'uploadMedia']
    forbidden.forEach(fn => {
      expect(exports).not.toContain(fn)
    })
  })

  it('sadece 7 explicit function export ediyor', () => {
    const exportedFunctions = Object.keys(platformPackageApi).filter(
      k => typeof (platformPackageApi as Record<string, unknown>)[k] === 'function'
    )
    expect(exportedFunctions).toHaveLength(8)
    expect(exportedFunctions).toEqual(expect.arrayContaining([
      'generatePlatformPackagesForStoryboard',
      'listPlatformPackagesByContentProject',
      'listPlatformPackagesByStoryboard',
      'getPlatformPackageDetail',
      'updatePlatformPackageDraft',
      'approvePlatformPackageDraft',
      'rejectPlatformPackageDraft',
      'exportPlatformPackage',
    ]))
  })
})
