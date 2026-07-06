import { describe, it, expect } from 'vitest'
import { readFileSync } from 'fs'
import { resolve } from 'path'

/**
 * FP-015 Task 04: Approved Internal Inventory Panel safety & behavior tests.
 *
 * Source-inspection tests — no DOM rendering required.
 * Verifies safe rendering, forbidden patterns, label safety,
 * loading/error/empty states, and integration.
 */

const PANEL_SOURCE = readFileSync(
  resolve(__dirname, '../ApprovedInternalInventoryPanel.tsx'),
  'utf-8'
)

describe('FP-015 Task 04 — ApprovedInternalInventoryPanel', () => {

  // ── A. Safe Rendering ────────────────────────────────────

  describe('A. Safe Rendering', () => {
    it('dangerouslySetInnerHTML kullanmıyor', () => {
      expect(PANEL_SOURCE).not.toContain('dangerouslySetInnerHTML')
    })

    it('innerHTML kullanmıyor', () => {
      expect(PANEL_SOURCE).not.toContain('.innerHTML')
    })

    it('__html kullanmıyor', () => {
      expect(PANEL_SOURCE).not.toContain('__html')
    })

    it('safeText fonksiyonu mevcut', () => {
      expect(PANEL_SOURCE).toContain('function safeText')
    })

    it('metin çıktılarında safeText kullanıyor', () => {
      const safeTextCalls = (PANEL_SOURCE.match(/safeText\(/g) || []).length
      expect(safeTextCalls).toBeGreaterThanOrEqual(3)
    })
  })

  // ── B. Internal-Only Labels ──────────────────────────────

  describe('B. Internal-Only Labels', () => {
    it('panel başlığı dahili-only vurgusu yapıyor', () => {
      expect(PANEL_SOURCE).toContain('Dahili')
      expect(PANEL_SOURCE).toContain('İç Kullanım')
    })

    it('disclaimer mevcut', () => {
      expect(PANEL_SOURCE).toContain('inventory-disclaimer')
      expect(PANEL_SOURCE).toContain('yayın/export')
    })

    it('Dahili Hazır label mevcut', () => {
      expect(PANEL_SOURCE).toContain('Dahili Hazır')
    })

    it('Onay Bekliyor label mevcut', () => {
      expect(PANEL_SOURCE).toContain('Onay Bekliyor')
    })
  })

  // ── C. Loading/Error/Empty States ────────────────────────

  describe('C. Loading/Error/Empty States', () => {
    it('loading state data-testid mevcut', () => {
      expect(PANEL_SOURCE).toContain('inventory-loading')
    })

    it('error state data-testid mevcut', () => {
      expect(PANEL_SOURCE).toContain('inventory-error')
    })

    it('empty state data-testid mevcut', () => {
      expect(PANEL_SOURCE).toContain('inventory-empty')
    })

    it('error role=alert mevcut', () => {
      expect(PANEL_SOURCE).toContain('role="alert"')
    })
  })

  // ── D. Data TestIDs ──────────────────────────────────────

  describe('D. Data TestIDs', () => {
    const expectedTestIds = [
      'inventory-panel',
      'inventory-disclaimer',
      'inventory-filters',
      'inventory-filter-target-type',
      'inventory-loading',
      'inventory-error',
      'inventory-list',
      'inventory-item',
      'inventory-empty',
    ]

    expectedTestIds.forEach(testId => {
      it(`data-testid="${testId}" mevcut`, () => {
        expect(PANEL_SOURCE).toContain(`data-testid="${testId}"`)
      })
    })
  })

  // ── E. Forbidden Values ──────────────────────────────────

  describe('E. Forbidden Values', () => {
    it('publish/export button yok', () => {
      expect(PANEL_SOURCE).not.toContain('>Yayınla<')
      expect(PANEL_SOURCE).not.toContain('>Export<')
      expect(PANEL_SOURCE).not.toContain('data-testid="publish')
      expect(PANEL_SOURCE).not.toContain('data-testid="export')
    })

    it('providerUrl alanı yok', () => {
      expect(PANEL_SOURCE).not.toContain('providerUrl')
    })

    it('storageKey alanı yok', () => {
      expect(PANEL_SOURCE).not.toContain('storageKey')
    })

    it('publicUrl alanı yok', () => {
      expect(PANEL_SOURCE).not.toContain('publicUrl')
    })

    it('signedUrl alanı yok', () => {
      expect(PANEL_SOURCE).not.toContain('signedUrl')
    })

    it('stackTrace göstermiyor', () => {
      expect(PANEL_SOURCE).not.toContain('stackTrace')
    })
  })

  // ── F. IMAGE_ASSET Safety ────────────────────────────────

  describe('F. IMAGE_ASSET Safety', () => {
    it('img src kullanmıyor', () => {
      expect(PANEL_SOURCE).not.toMatch(/<img\s/)
      expect(PANEL_SOURCE).not.toContain('img src')
    })

    it('base64 alanı yok', () => {
      expect(PANEL_SOURCE).not.toMatch(/\.base64/)
      expect(PANEL_SOURCE).not.toContain("'base64'")
    })

    it('rawPayload alanı yok', () => {
      expect(PANEL_SOURCE).not.toContain('rawPayload')
    })
  })

  // ── G. API Integration ───────────────────────────────────

  describe('G. API Integration', () => {
    it('getApprovedInternalReadiness API kullanıyor', () => {
      expect(PANEL_SOURCE).toContain('getApprovedInternalReadiness')
    })

    it('reviewApi import ediyor', () => {
      expect(PANEL_SOURCE).toContain("import * as reviewApi from '../../api/reviewApi'")
    })

    it('POST metodu kullanmıyor', () => {
      expect(PANEL_SOURCE).not.toContain("method: 'POST'")
      expect(PANEL_SOURCE).not.toContain('fetch(')
    })
  })
})
