import { describe, it, expect } from 'vitest'
import * as turkuVideoApi from '../turkuVideoApi'

/**
 * FP-019 Task 05 + FP-020 Task 05 + FP-021 Task 04: Türkü Video API client tests.
 *
 * Kontrol noktaları:
 * - Tüm 19 endpoint fonksiyonu mevcut (9 FP-019 + 6 FP-020 + 4 FP-021)
 * - Publish/export/download fonksiyonu yok
 * - Authorization header fonksiyonu yok
 * - Provider/storage/base64 fonksiyonu yok
 */

describe('turkuVideoApi', () => {
  // ── Exported Functions ────────────────────────

  it('bootstrapTurkuVideoProject fonksiyonu mevcut', () => {
    expect(typeof turkuVideoApi.bootstrapTurkuVideoProject).toBe('function')
  })

  it('getTurkuVideoProject fonksiyonu mevcut', () => {
    expect(typeof turkuVideoApi.getTurkuVideoProject).toBe('function')
  })

  it('getTurkuVideoProjectProgress fonksiyonu mevcut', () => {
    expect(typeof turkuVideoApi.getTurkuVideoProjectProgress).toBe('function')
  })

  it('prepareTurkuStory fonksiyonu mevcut', () => {
    expect(typeof turkuVideoApi.prepareTurkuStory).toBe('function')
  })

  it('prepareTurkuScenario fonksiyonu mevcut', () => {
    expect(typeof turkuVideoApi.prepareTurkuScenario).toBe('function')
  })

  it('prepareTurkuStoryboard fonksiyonu mevcut', () => {
    expect(typeof turkuVideoApi.prepareTurkuStoryboard).toBe('function')
  })

  it('prepareTurkuSceneAssets fonksiyonu mevcut', () => {
    expect(typeof turkuVideoApi.prepareTurkuSceneAssets).toBe('function')
  })

  it('prepareTurkuImageBatch fonksiyonu mevcut', () => {
    expect(typeof turkuVideoApi.prepareTurkuImageBatch).toBe('function')
  })

  it('completeTurkuImageBatch fonksiyonu mevcut', () => {
    expect(typeof turkuVideoApi.completeTurkuImageBatch).toBe('function')
  })

  // ── FP-020 Functions ──────────────────────────

  it('generateTurkuStory fonksiyonu mevcut', () => {
    expect(typeof turkuVideoApi.generateTurkuStory).toBe('function')
  })

  it('generateTurkuScenario fonksiyonu mevcut', () => {
    expect(typeof turkuVideoApi.generateTurkuScenario).toBe('function')
  })

  it('generateTurkuStoryboard fonksiyonu mevcut', () => {
    expect(typeof turkuVideoApi.generateTurkuStoryboard).toBe('function')
  })

  it('generateTurkuPlanningBundle fonksiyonu mevcut', () => {
    expect(typeof turkuVideoApi.generateTurkuPlanningBundle).toBe('function')
  })

  it('runTurkuImageBatch fonksiyonu mevcut', () => {
    expect(typeof turkuVideoApi.runTurkuImageBatch).toBe('function')
  })

  it('getTurkuGenerationSafety fonksiyonu mevcut', () => {
    expect(typeof turkuVideoApi.getTurkuGenerationSafety).toBe('function')
  })

  // ── FP-021 Functions ───────────────────────────────

  it('prepareTurkuCompositionPlan fonksiyonu mevcut', () => {
    expect(typeof turkuVideoApi.prepareTurkuCompositionPlan).toBe('function')
  })

  it('prepareTurkuRenderManifest fonksiyonu mevcut', () => {
    expect(typeof turkuVideoApi.prepareTurkuRenderManifest).toBe('function')
  })

  it('getTurkuRenderReadiness fonksiyonu mevcut', () => {
    expect(typeof turkuVideoApi.getTurkuRenderReadiness).toBe('function')
  })

  it('getTurkuCompositionPreview fonksiyonu mevcut', () => {
    expect(typeof turkuVideoApi.getTurkuCompositionPreview).toBe('function')
  })

  // ── Forbidden Functions ───────────────────────

  it('publishTurkuVideo fonksiyonu YOK', () => {
    expect((turkuVideoApi as Record<string, unknown>).publishTurkuVideo).toBeUndefined()
  })

  it('exportTurkuVideo fonksiyonu YOK', () => {
    expect((turkuVideoApi as Record<string, unknown>).exportTurkuVideo).toBeUndefined()
  })

  it('downloadTurkuVideo fonksiyonu YOK', () => {
    expect((turkuVideoApi as Record<string, unknown>).downloadTurkuVideo).toBeUndefined()
  })

  it('getPublicUrl fonksiyonu YOK', () => {
    expect((turkuVideoApi as Record<string, unknown>).getPublicUrl).toBeUndefined()
  })

  it('getSignedUrl fonksiyonu YOK', () => {
    expect((turkuVideoApi as Record<string, unknown>).getSignedUrl).toBeUndefined()
  })

  it('getCdnUrl fonksiyonu YOK', () => {
    expect((turkuVideoApi as Record<string, unknown>).getCdnUrl).toBeUndefined()
  })

  it('getStorageKey fonksiyonu YOK', () => {
    expect((turkuVideoApi as Record<string, unknown>).getStorageKey).toBeUndefined()
  })

  it('getBase64Image fonksiyonu YOK', () => {
    expect((turkuVideoApi as Record<string, unknown>).getBase64Image).toBeUndefined()
  })

  it('setAuthorization fonksiyonu YOK', () => {
    expect((turkuVideoApi as Record<string, unknown>).setAuthorization).toBeUndefined()
  })

  it('loginAdmin fonksiyonu YOK', () => {
    expect((turkuVideoApi as Record<string, unknown>).loginAdmin).toBeUndefined()
  })

  // ── Source code safety ────────────────────────

  it('API client kaynak kodu Auth header içermez', async () => {
    const fs = await import('fs')
    const path = await import('path')
    const source = fs.readFileSync(
      path.resolve(__dirname, '../turkuVideoApi.ts'), 'utf-8'
    )
    expect(source).not.toContain('Bearer')
  })

  it('API client providerKey/storageKey/signedUrl içermez', async () => {
    const fs = await import('fs')
    const path = await import('path')
    const source = fs.readFileSync(
      path.resolve(__dirname, '../turkuVideoApi.ts'), 'utf-8'
    )
    expect(source).not.toContain('storageKey')
    expect(source).not.toContain('signedUrl')
    expect(source).not.toContain('cdnUrl')
    expect(source).not.toContain('publicUrl')
  })

  it('tam olarak 19 export fonksiyonu var', () => {
    const exports = Object.keys(turkuVideoApi)
    expect(exports).toHaveLength(19)
  })
})
