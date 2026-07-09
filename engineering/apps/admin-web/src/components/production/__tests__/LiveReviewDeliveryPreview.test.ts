/**
 * LIVE-005: Review + Controlled Delivery Preview UI Tests.
 */

import { describe, it, expect } from 'vitest'
import {
  renderDeliveryStatus,
  renderReviewDecision,
  renderChecklistItem,
  getDeliverySummary,
  isExportEligible,
} from '../LiveReviewDeliveryPreview'
import type { TurkuLiveControlledDeliveryResult, TurkuLiveReviewChecklistItem } from '../../../types'

const FORBIDDEN_TEXT = [
  'filePath', 'absolutePath', 'stdout', 'stderr', 'commandLine', 'rawLog',
  'publicUrl', 'signedUrl', 'cdnUrl', 'downloadUrl', 'storageKey', 'base64',
  'bucketName', 'objectKey', 'command', 'processBuilder',
  'token', 'JWT', 'session', 'principal', 'apiKey', 'credential',
  'providerPrompt', 'rawPrompt', 'providerPayload',
]

const COMPONENT_FORBIDDEN = [
  'PublishButton', 'AutoPublishButton', 'PublicButton',
  'innerHTML', 'dangerouslySetInnerHTML',
]

function makeItem(checked: boolean): TurkuLiveReviewChecklistItem {
  return { label: 'Gorsel kalite', checked, note: '' }
}

function makePending(): TurkuLiveControlledDeliveryResult {
  return {
    status: 'PENDING_HUMAN_REVIEW', issues: ['HUMAN_REVIEW_NOT_COMPLETED'],
    opaqueDeliveryRef: null, opaqueRenderRef: 'rrnd:abc',
    reviewDecision: 'PENDING', reviewChecklist: [makeItem(false)],
    renderArtifactLabel: 'Gercek render', formatHint: 'MP4',
    resolutionHint: '1920x1080', durationLabel: '~40 saniye', sizeLabel: '~25 MB',
    exportEligible: false, exportEligibilityNote: 'Inceleme bekleniyor',
    blockedReasons: ['Inceleme bekleniyor'], validationSummary: 'Inceleme bekleniyor',
  }
}

function makeApproved(): TurkuLiveControlledDeliveryResult {
  return { ...makePending(), status: 'ELIGIBLE_FOR_CONTROLLED_DELIVERY', issues: [], opaqueDeliveryRef: 'ldlv:abc', reviewDecision: 'APPROVED_FOR_CONTROLLED_DELIVERY', exportEligible: true, exportEligibilityNote: 'Admin erisimiyle', blockedReasons: [], validationSummary: 'Onaylandi' }
}

function makeRejected(): TurkuLiveControlledDeliveryResult {
  return { ...makePending(), status: 'BLOCKED_BY_REVIEW', issues: ['REVIEW_REJECTED'], reviewDecision: 'REJECTED' }
}

describe('LIVE-005 — Review + Controlled Delivery Preview', () => {
  describe('A. Status Rendering', () => {
    it('pending', () => expect(renderDeliveryStatus('PENDING_HUMAN_REVIEW')).toContain('Bekleniyor'))
    it('eligible', () => expect(renderDeliveryStatus('ELIGIBLE_FOR_CONTROLLED_DELIVERY')).toContain('Uygun'))
    it('blocked', () => expect(renderDeliveryStatus('BLOCKED_BY_REVIEW')).toContain('Engel'))
  })

  describe('B. Review Decision', () => {
    it('pending', () => expect(renderReviewDecision('PENDING')).toContain('Beklemede'))
    it('approved', () => expect(renderReviewDecision('APPROVED_FOR_CONTROLLED_DELIVERY')).toContain('Onaylandi'))
    it('rejected', () => expect(renderReviewDecision('REJECTED')).toContain('Reddedildi'))
    it('revision', () => expect(renderReviewDecision('NEEDS_REVISION')).toContain('Revizyon'))
  })

  describe('C. Checklist', () => {
    it('checked shows green', () => expect(renderChecklistItem(makeItem(true))).toContain('\u2705'))
    it('unchecked shows box', () => expect(renderChecklistItem(makeItem(false))).toContain('\u2B1C'))
  })

  describe('D. Export Eligibility', () => {
    it('pending = not eligible', () => expect(isExportEligible(makePending())).toBe(false))
    it('approved = eligible', () => expect(isExportEligible(makeApproved())).toBe(true))
    it('rejected = not eligible', () => expect(isExportEligible(makeRejected())).toBe(false))
  })

  describe('E. Summary', () => {
    it('includes status', () => expect(getDeliverySummary(makePending())).toContain('Bekleniyor'))
    it('includes decision', () => expect(getDeliverySummary(makeApproved())).toContain('Onaylandi'))
    it('includes render info', () => expect(getDeliverySummary(makePending())).toContain('MP4'))
  })

  describe('F. Forbidden Sweep', () => {
    it('summary no forbidden', () => {
      for (const f of FORBIDDEN_TEXT) expect(getDeliverySummary(makePending())).not.toContain(f)
    })
    it('source no unsafe controls', async () => {
      const fs = await import('fs')
      const src = fs.readFileSync('src/components/production/LiveReviewDeliveryPreview.tsx', 'utf-8')
      for (const f of COMPONENT_FORBIDDEN) expect(src).not.toContain(f)
    })
  })

  describe('G. Type Safety', () => {
    it('no forbidden keys', () => {
      for (const f of ['publicUrl', 'storageKey', 'base64', 'token', 'credential', 'command', 'filePath'])
        expect(Object.keys(makePending())).not.toContain(f)
    })
  })
})
