import { describe, it, expect } from 'vitest'
import { readFileSync } from 'fs'
import { resolve } from 'path'

/**
 * FP-043: Internal Render Review Panel safety & behavior tests.
 *
 * Source-inspection tests — no DOM rendering required.
 * Verifies safe rendering, forbidden patterns, internal-only wording,
 * no player/download/export/publish controls, no public URL exposure.
 */

const PANEL_SOURCE = readFileSync(
  resolve(__dirname, '../InternalRenderReviewPanel.tsx'),
  'utf-8'
)

const TYPES_SOURCE = readFileSync(
  resolve(__dirname, '../../..', 'types/index.ts'),
  'utf-8'
)

describe('FP-043 — InternalRenderReviewPanel', () => {

  // ── A. Safe Rendering ──
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
  })

  // ── B. Internal-Only Wording ──
  describe('B. Internal-Only Wording', () => {
    it('dahili inceleme vurgusu yapıyor', () => {
      expect(PANEL_SOURCE).toContain('Dahili')
      expect(PANEL_SOURCE).toContain('dahili inceleme')
    })
    it('internal-only notice data-testid mevcut', () => {
      expect(PANEL_SOURCE).toContain('internal-only-notice')
    })
    it('READY_FOR_INTERNAL_REVIEW_ONLY label dahili', () => {
      expect(PANEL_SOURCE).toContain('Dahili İnceleme İçin Hazır')
    })
  })

  // ── C. No Player/Preview ──
  describe('C. No Player/Preview', () => {
    it('<video> tag yok', () => {
      expect(PANEL_SOURCE).not.toContain('<video')
    })
    it('<audio> tag yok', () => {
      expect(PANEL_SOURCE).not.toContain('<audio')
    })
    it('<iframe> tag yok', () => {
      expect(PANEL_SOURCE).not.toContain('<iframe')
    })
    it('VideoPlayer referansı yok', () => {
      expect(PANEL_SOURCE).not.toContain('VideoPlayer')
    })
    it('AudioPlayer referansı yok', () => {
      expect(PANEL_SOURCE).not.toContain('AudioPlayer')
    })
  })

  // ── D. No Download/Export/Publish Controls ──
  describe('D. No Download/Export/Publish Controls', () => {
    it('download butonu yok', () => {
      // Check for English "download" as a standalone UI action word
      expect(PANEL_SOURCE).not.toMatch(/\bdownload\b/i)
    })
    it('export butonu yok', () => {
      // Exclude JS 'export default' — check for export as UI action
      const withoutJsExport = PANEL_SOURCE.replace(/export\s+default/g, '')
        .replace(/export\s+type/g, '').replace(/export\s+{/g, '')
        .replace(/export\s+function/g, '').replace(/export\s+const/g, '')
        .replace(/export\s+interface/g, '')
      expect(withoutJsExport).not.toMatch(/\bexport\b/i)
    })
    it('publish butonu yok', () => {
      expect(PANEL_SOURCE).not.toMatch(/\bpublish\b/i)
    })
  })

  // ── E. No Public/Storage URL ──
  describe('E. No Public/Storage URL', () => {
    it('publicUrl yok', () => {
      expect(PANEL_SOURCE).not.toContain('publicUrl')
    })
    it('signedUrl yok', () => {
      expect(PANEL_SOURCE).not.toContain('signedUrl')
    })
    it('cdnUrl yok', () => {
      expect(PANEL_SOURCE).not.toContain('cdnUrl')
    })
    it('storageKey yok', () => {
      expect(PANEL_SOURCE).not.toContain('storageKey')
    })
    it('<a href yok', () => {
      expect(PANEL_SOURCE).not.toContain('<a href')
    })
  })

  // ── F. No Forbidden Fields in Source ──
  describe('F. No Forbidden Fields', () => {
    const FORBIDDEN = [
      'stdout', 'stderr', 'rawLog', 'commandLine', 'shellCommand',
      'executablePath', 'filePath', 'absolutePath', 'inputPath', 'outputPath',
      'base64', 'binaryData', 'rawPayload',
      'token', 'jwt', 'session', 'principal', 'authorizationHeader',
      'lyrics', 'scriptBody', 'promptText', 'copyrightedContent',
    ]
    for (const field of FORBIDDEN) {
      it(`${field} field yok`, () => {
        expect(PANEL_SOURCE).not.toContain(field)
      })
    }
  })

  // ── G. Review Package Status Labels ──
  describe('G. Status Labels', () => {
    it('5 status label mevcut', () => {
      expect(PANEL_SOURCE).toContain('CREATED_INTERNAL_ONLY')
      expect(PANEL_SOURCE).toContain('ARTIFACT_METADATA_ATTACHED')
      expect(PANEL_SOURCE).toContain('READY_FOR_INTERNAL_REVIEW_ONLY')
      expect(PANEL_SOURCE).toContain('REJECTED_UNSAFE')
      expect(PANEL_SOURCE).toContain('FAILED_SAFE')
    })
  })

  // ── H. Issue Code Labels ──
  describe('H. Issue Code Labels', () => {
    it('NO_PRIMARY_ARTIFACT issue label mevcut', () => {
      expect(PANEL_SOURCE).toContain('NO_PRIMARY_ARTIFACT')
    })
    it('ARTIFACT_NOT_VERIFIED issue label mevcut', () => {
      expect(PANEL_SOURCE).toContain('ARTIFACT_NOT_VERIFIED')
    })
    it('TOO_MANY_ARTIFACTS issue label mevcut', () => {
      expect(PANEL_SOURCE).toContain('TOO_MANY_ARTIFACTS')
    })
  })

  // ── I. Types Safety ──
  describe('I. Types Safety', () => {
    it('TurkuRenderReviewPackage type tanımlı', () => {
      expect(TYPES_SOURCE).toContain('TurkuRenderReviewPackage')
    })
    it('TurkuRenderReviewArtifactSummary type tanımlı', () => {
      expect(TYPES_SOURCE).toContain('TurkuRenderReviewArtifactSummary')
    })
    it('TurkuRenderReviewPackageStatus type tanımlı', () => {
      expect(TYPES_SOURCE).toContain('TurkuRenderReviewPackageStatus')
    })
    it('internalOnly: true zorunlu', () => {
      expect(TYPES_SOURCE).toContain('internalOnly: true')
    })
  })

  // ── J. No Auto-Approval ──
  describe('J. No Auto-Approval', () => {
    it('autoApprove yok', () => {
      expect(PANEL_SOURCE).not.toContain('autoApprove')
    })
    it('READY_FOR_PUBLISH yok', () => {
      expect(PANEL_SOURCE).not.toContain('READY_FOR_PUBLISH')
    })
    it('COMPLETED status yok', () => {
      expect(PANEL_SOURCE).not.toMatch(/['"]COMPLETED['"]/)
    })
  })

  // ── K. Metadata Display ──
  describe('K. Metadata Display', () => {
    it('review-metadata data-testid mevcut', () => {
      expect(PANEL_SOURCE).toContain('review-metadata')
    })
    it('review-artifacts data-testid mevcut', () => {
      expect(PANEL_SOURCE).toContain('review-artifacts')
    })
    it('review-status data-testid mevcut', () => {
      expect(PANEL_SOURCE).toContain('review-status')
    })
  })
})
