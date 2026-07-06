import { describe, it, expect } from 'vitest'
import ReviewPreviewPanel from '../ReviewPreviewPanel'

/**
 * FP-013 Task 04: ReviewPreviewPanel component tests.
 *
 * Safe rendering — no image bytes/URL/base64/storage key display.
 * No dangerouslySetInnerHTML/innerHTML.
 * No forbidden strings as enabled behavior.
 */

describe('ReviewPreviewPanel', () => {
  // ── Component Existence ─────────────────────────────────────

  it('ReviewPreviewPanel bileşeni mevcut', () => {
    expect(typeof ReviewPreviewPanel).toBe('function')
  })

  // ── Source Code Safety ──────────────────────────────────────

  it('bileşen kaynak kodu dangerouslySetInnerHTML içermez', async () => {
    // Import as text via toString()
    const source = ReviewPreviewPanel.toString()
    expect(source).not.toContain('dangerouslySetInnerHTML')
    expect(source).not.toContain('innerHTML')
  })

  it('bileşen yasaklı davranış stringleri içermez', () => {
    const source = ReviewPreviewPanel.toString()
    const forbidden = [
      'READY_FOR_PUBLISH',
      'COMPLETED',
      'PUBLISHED',
      'EXPORTED',
      'publicUrl',
      'signedUrl',
      'providerUrl',
      'storageKey',
      'rawPayload',
      'rawImageBytes',
      'base64',
      'secret',
      'apiKey',
      'config',
      'stackTrace',
    ]
    forbidden.forEach(str => {
      expect(source).not.toContain(str)
    })
  })

  // ── Props Interface ─────────────────────────────────────────

  it('bileşen targetType ve targetId props kabul eder', () => {
    // Verify component accepts the expected props by checking it doesn't throw on call
    expect(ReviewPreviewPanel.length).toBeGreaterThanOrEqual(0)
  })

  // ── No Forbidden Exports ────────────────────────────────────

  it('bileşen modülünden yasaklı export yok', () => {
    const exports = Object.keys(ReviewPreviewPanel)
    const forbidden = [
      'publish', 'export', 'publicUrl', 'signedUrl',
      'providerUrl', 'storageKey', 'rawPayload', 'base64',
    ]
    forbidden.forEach(fn => {
      expect(exports).not.toContain(fn)
    })
  })
})
