import { describe, it, expect } from 'vitest'
import * as storyboardApi from '../storyboardApi'

/**
 * FP-003: Storyboard API client testleri.
 *
 * Kontrol noktaları:
 * - Sadece explicit action function'lar mevcut
 * - Generic status update function yok
 * - READY_FOR_PUBLISH function yok
 * - mediaGenerationNotes gönderilmez
 */

describe('storyboardApi', () => {
  // ── Exported Functions ──────────────────────────────────

  it('generateStoryboardForScenario fonksiyonu mevcut', () => {
    expect(typeof storyboardApi.generateStoryboardForScenario).toBe('function')
  })

  it('listStoryboardsByContentProject fonksiyonu mevcut', () => {
    expect(typeof storyboardApi.listStoryboardsByContentProject).toBe('function')
  })

  it('getStoryboardDetail fonksiyonu mevcut', () => {
    expect(typeof storyboardApi.getStoryboardDetail).toBe('function')
  })

  it('updateStoryboardDraft fonksiyonu mevcut', () => {
    expect(typeof storyboardApi.updateStoryboardDraft).toBe('function')
  })

  it('approveStoryboardDraft fonksiyonu mevcut', () => {
    expect(typeof storyboardApi.approveStoryboardDraft).toBe('function')
  })

  it('rejectStoryboardDraft fonksiyonu mevcut', () => {
    expect(typeof storyboardApi.rejectStoryboardDraft).toBe('function')
  })

  // ── Governance ──────────────────────────────────────────

  it('generic status update function yok', () => {
    const exports = Object.keys(storyboardApi)
    const forbidden = ['updateStatus', 'setStatus', 'changeStatus', 'updateStoryboardStatus', 'patchStatus']
    forbidden.forEach(fn => {
      expect(exports).not.toContain(fn)
    })
  })

  it('READY_FOR_PUBLISH function yok', () => {
    const exports = Object.keys(storyboardApi)
    const forbidden = ['readyForPublish', 'markReadyForPublish', 'publishStoryboard', 'setReadyForPublish']
    forbidden.forEach(fn => {
      expect(exports).not.toContain(fn)
    })
  })

  it('sadece 6 explicit function export ediyor', () => {
    const exportedFunctions = Object.keys(storyboardApi).filter(
      k => typeof (storyboardApi as Record<string, unknown>)[k] === 'function'
    )
    expect(exportedFunctions).toHaveLength(6)
    expect(exportedFunctions).toEqual(expect.arrayContaining([
      'generateStoryboardForScenario',
      'listStoryboardsByContentProject',
      'getStoryboardDetail',
      'updateStoryboardDraft',
      'approveStoryboardDraft',
      'rejectStoryboardDraft',
    ]))
  })
})
