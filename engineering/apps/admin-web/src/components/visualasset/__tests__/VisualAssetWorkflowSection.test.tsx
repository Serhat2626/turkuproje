import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import VisualAssetWorkflowSection from '../VisualAssetWorkflowSection'
import type {
  VisualAssetPlanSummaryResponse, VisualAssetPlanResponse,
} from '../../../types'

// ── Mocks ─────────────────────────────────────────────────

vi.mock('../../../api/visualAssetApi', () => ({
  generateVisualAssetPlan: vi.fn(),
  listVisualAssetPlansByContentProject: vi.fn(),
  listVisualAssetPlansByStoryboard: vi.fn(),
  getVisualAssetPlanDetail: vi.fn(),
  updateVisualAssetPlan: vi.fn(),
  updateSceneVisualPlan: vi.fn(),
  updateThumbnailPlan: vi.fn(),
  approveVisualAssetPlan: vi.fn(),
  rejectVisualAssetPlan: vi.fn(),
}))

import * as visualAssetApi from '../../../api/visualAssetApi'
const mockApi = visualAssetApi as {
  generateVisualAssetPlan: ReturnType<typeof vi.fn>;
  listVisualAssetPlansByContentProject: ReturnType<typeof vi.fn>;
  listVisualAssetPlansByStoryboard: ReturnType<typeof vi.fn>;
  getVisualAssetPlanDetail: ReturnType<typeof vi.fn>;
  updateVisualAssetPlan: ReturnType<typeof vi.fn>;
  updateSceneVisualPlan: ReturnType<typeof vi.fn>;
  updateThumbnailPlan: ReturnType<typeof vi.fn>;
  approveVisualAssetPlan: ReturnType<typeof vi.fn>;
  rejectVisualAssetPlan: ReturnType<typeof vi.fn>;
}

// ── Test Data ─────────────────────────────────────────────

const PROJECT_ID = 'proj-001'
const STORYBOARD_ID = 'sb-001'

function makeSummary(
  overrides: Partial<VisualAssetPlanSummaryResponse> = {}
): VisualAssetPlanSummaryResponse {
  return {
    id: 'vap-001',
    sourceStoryboardDraftId: STORYBOARD_ID,
    contentProjectId: PROJECT_ID,
    version: 1,
    status: 'AI_GENERATED',
    title: 'Çanakkale Görsel Planı',
    scenePlanCount: 2,
    hasThumbnailPlan: true,
    providerType: 'MOCK',
    createdAt: '2026-07-01T10:00:00',
    approvedAt: null,
    rejectedAt: null,
    editedAt: null,
    ...overrides,
  }
}

function makeDetail(
  overrides: Partial<VisualAssetPlanResponse> = {}
): VisualAssetPlanResponse {
  return {
    id: 'vap-001',
    sourceStoryboardDraftId: STORYBOARD_ID,
    contentProjectId: PROJECT_ID,
    version: 1,
    status: 'AI_GENERATED',
    title: 'Çanakkale Görsel Planı',
    summary: 'Çanakkale görsel plan özeti',
    overallVisualStyle: 'Sinematik belgesel',
    aspectRatioStrategy: '16:9 primary',
    culturalAuthenticityGuidance: 'Kültürel rehber',
    characterContinuityGuidance: 'Karakter rehberi',
    locationContinuityGuidance: 'Mekân rehberi',
    colorPaletteGuidance: 'Renk rehberi',
    reviewNotes: null,
    generatedBy: 'AI_CONTENT_AGENT',
    providerType: 'MOCK',
    modelName: 'mock-visual-asset-planner',
    approvedAt: null,
    approvedBy: null,
    rejectedAt: null,
    rejectedBy: null,
    rejectionReason: null,
    editedAt: null,
    editedBy: null,
    createdAt: '2026-07-01T10:00:00',
    updatedAt: null,
    scenePlans: [
      {
        id: 'svp-001',
        sourceStoryboardSceneId: 'scene-001',
        sceneNumber: 1,
        sourceBeatType: 'HOOK',
        sceneTitle: 'Çanakkale Şafağı',
        turkishVisualContext: 'Şafak vakti Gelibolu',
        englishImagePrompt: 'Cinematic wide shot of Gallipoli at dawn',
        negativePrompt: 'no modern buildings',
        styleDirective: 'Cinematic',
        aspectRatio: '16:9',
        compositionNotes: 'Rule of thirds',
        cameraFraming: 'Wide establishing',
        lightingNotes: 'Golden hour',
        colorPalette: 'Earth tones',
        mood: 'Dramatic',
        location: 'Gelibolu',
        characters: 'Askerler',
        culturalDetails: '1915 dönemi',
        textOverlayGuidance: 'Alt 1/5 bırak',
        imageGenerationToolNotes: 'DALL-E uyumlu',
        safetyNotes: 'Saygılı işlenmeli',
        createdAt: '2026-07-01T10:00:00',
        updatedAt: null,
      },
      {
        id: 'svp-002',
        sourceStoryboardSceneId: 'scene-002',
        sceneNumber: 2,
        sourceBeatType: 'STORY_DEVELOPMENT',
        sceneTitle: 'Mektup Sahnesi',
        turkishVisualContext: 'Mumışığında mektup',
        englishImagePrompt: 'Close-up of hands writing a letter by candlelight',
        negativePrompt: 'no modern elements',
        styleDirective: 'Intimate',
        aspectRatio: '16:9',
        compositionNotes: 'Tight framing',
        cameraFraming: 'Close-up',
        lightingNotes: 'Candlelight',
        colorPalette: 'Warm amber',
        mood: 'Melancholic',
        location: 'Askeri çadır',
        characters: 'Genç asker',
        culturalDetails: 'Osmanlı mektup geleneği',
        textOverlayGuidance: null,
        imageGenerationToolNotes: null,
        safetyNotes: 'Hassas işlenmeli',
        createdAt: '2026-07-01T10:00:00',
        updatedAt: null,
      },
    ],
    thumbnailPlan: {
      id: 'tp-001',
      sourceStoryboardSceneId: 'scene-001',
      sourceSceneNumber: 1,
      thumbnailPrompt: 'Dramatic portrait of elderly storyteller',
      negativePrompt: 'no text, no watermark',
      textOverlayGuidance: 'Sol 2/3 yüz',
      compositionNotes: 'Rule of thirds',
      emotionalHook: 'Doğrudan göz teması',
      aspectRatio: '16:9',
      styleDirective: 'Cinematic portrait',
      colorPalette: 'Warm saturated',
      createdAt: '2026-07-01T10:00:00',
      updatedAt: null,
    },
    ...overrides,
  }
}

function apiSuccess<T>(data: T) {
  return Promise.resolve({ success: true, data, message: null, errors: null, timestamp: '' })
}

beforeEach(() => {
  vi.clearAllMocks()
  mockApi.listVisualAssetPlansByContentProject.mockReturnValue(apiSuccess([]))
})

// ── Tests ─────────────────────────────────────────────────

describe('VisualAssetWorkflowSection', () => {

  // ── Generate Button Visibility ──────────────────────────

  describe('Generate butonu', () => {
    it('onaylanmış storyboard olmadığında uyarı mesajı gösterir', async () => {
      render(
        <VisualAssetWorkflowSection contentProjectId={PROJECT_ID} approvedStoryboardId={null} />
      )

      await waitFor(() => {
        expect(screen.getByText(/storyboard onaylanmalıdır/i)).toBeInTheDocument()
      })
    })

    it('onaylanmış storyboard olmadığında üret butonu gösterilmez', async () => {
      render(
        <VisualAssetWorkflowSection contentProjectId={PROJECT_ID} approvedStoryboardId={null} />
      )

      await waitFor(() => {
        expect(screen.queryByText(/Görsel Plan Üret/)).not.toBeInTheDocument()
      })
    })

    it('onaylanmış storyboard varken üret butonu gösterilir', async () => {
      render(
        <VisualAssetWorkflowSection contentProjectId={PROJECT_ID} approvedStoryboardId={STORYBOARD_ID} />
      )

      await waitFor(() => {
        expect(screen.getByText(/Görsel Plan Üret/)).toBeInTheDocument()
      })
    })
  })

  // ── List View ──────────────────────────────────────────

  describe('Liste görünümü', () => {
    it('plan listesini render eder', async () => {
      const summaries = [
        makeSummary({ id: 'vap-1', version: 2, title: 'V2 Plan' }),
        makeSummary({ id: 'vap-2', version: 1, title: 'V1 Plan', status: 'REJECTED' }),
      ]
      mockApi.listVisualAssetPlansByContentProject.mockReturnValue(apiSuccess(summaries))

      render(
        <VisualAssetWorkflowSection contentProjectId={PROJECT_ID} approvedStoryboardId={STORYBOARD_ID} />
      )

      await waitFor(() => {
        expect(screen.getByText('V2 Plan')).toBeInTheDocument()
        expect(screen.getByText('V1 Plan')).toBeInTheDocument()
      })
    })

    it('boş liste empty state gösterir', async () => {
      render(
        <VisualAssetWorkflowSection contentProjectId={PROJECT_ID} approvedStoryboardId={STORYBOARD_ID} />
      )

      await waitFor(() => {
        expect(screen.getByText(/Henüz görsel plan üretilmedi/)).toBeInTheDocument()
      })
    })
  })

  // ── Detail View ────────────────────────────────────────

  describe('Detay görünümü', () => {
    it('plan detayını sahnelerle render eder', async () => {
      const detail = makeDetail()
      mockApi.listVisualAssetPlansByContentProject.mockReturnValue(apiSuccess([makeSummary()]))
      mockApi.getVisualAssetPlanDetail.mockReturnValue(apiSuccess(detail))

      render(
        <VisualAssetWorkflowSection contentProjectId={PROJECT_ID} approvedStoryboardId={STORYBOARD_ID} />
      )

      // Click detail button
      await waitFor(() => {
        expect(screen.getByText('Detay')).toBeInTheDocument()
      })
      await userEvent.click(screen.getByText('Detay'))

      await waitFor(() => {
        const titles = screen.getAllByText('Çanakkale Görsel Planı')
        expect(titles.length).toBeGreaterThanOrEqual(1)
      })
    })

    it('sahne englishImagePrompt ve turkishVisualContext gösterir', async () => {
      const detail = makeDetail()
      mockApi.listVisualAssetPlansByContentProject.mockReturnValue(apiSuccess([makeSummary()]))
      mockApi.getVisualAssetPlanDetail.mockReturnValue(apiSuccess(detail))

      render(
        <VisualAssetWorkflowSection contentProjectId={PROJECT_ID} approvedStoryboardId={STORYBOARD_ID} />
      )

      await waitFor(() => { screen.getByText('Detay') })
      await userEvent.click(screen.getByText('Detay'))

      await waitFor(() => {
        expect(screen.getByText(/Gallipoli at dawn/)).toBeInTheDocument()
        expect(screen.getByText(/Şafak vakti Gelibolu/)).toBeInTheDocument()
      })
    })

    it('thumbnail planı render eder', async () => {
      const detail = makeDetail()
      mockApi.listVisualAssetPlansByContentProject.mockReturnValue(apiSuccess([makeSummary()]))
      mockApi.getVisualAssetPlanDetail.mockReturnValue(apiSuccess(detail))

      render(
        <VisualAssetWorkflowSection contentProjectId={PROJECT_ID} approvedStoryboardId={STORYBOARD_ID} />
      )

      await waitFor(() => { screen.getByText('Detay') })
      await userEvent.click(screen.getByText('Detay'))

      await waitFor(() => {
        expect(screen.getByText(/elderly storyteller/)).toBeInTheDocument()
      })
    })

    it('plan-level alanları gösterir', async () => {
      const detail = makeDetail()
      mockApi.listVisualAssetPlansByContentProject.mockReturnValue(apiSuccess([makeSummary()]))
      mockApi.getVisualAssetPlanDetail.mockReturnValue(apiSuccess(detail))

      render(
        <VisualAssetWorkflowSection contentProjectId={PROJECT_ID} approvedStoryboardId={STORYBOARD_ID} />
      )

      await waitFor(() => { screen.getByText('Detay') })
      await userEvent.click(screen.getByText('Detay'))

      await waitFor(() => {
        expect(screen.getByText(/Sinematik belgesel/)).toBeInTheDocument()
        expect(screen.getByText(/16:9 primary/)).toBeInTheDocument()
      })
    })
  })

  // ── Approve / Reject ───────────────────────────────────

  describe('Approve/Reject butonları', () => {
    it('AI_GENERATED statusunda onayla/reddet butonları gösterilir', async () => {
      const detail = makeDetail({ status: 'AI_GENERATED' })
      mockApi.listVisualAssetPlansByContentProject.mockReturnValue(apiSuccess([makeSummary()]))
      mockApi.getVisualAssetPlanDetail.mockReturnValue(apiSuccess(detail))

      render(
        <VisualAssetWorkflowSection contentProjectId={PROJECT_ID} approvedStoryboardId={STORYBOARD_ID} />
      )

      await waitFor(() => { screen.getByText('Detay') })
      await userEvent.click(screen.getByText('Detay'))

      await waitFor(() => {
        expect(screen.getByText('✓ Onayla')).toBeInTheDocument()
        expect(screen.getByText('✕ Reddet')).toBeInTheDocument()
      })
    })

    it('APPROVED statusunda onayla/reddet butonları gizlidir', async () => {
      const detail = makeDetail({ status: 'APPROVED' })
      mockApi.listVisualAssetPlansByContentProject.mockReturnValue(apiSuccess([makeSummary({ status: 'APPROVED' })]))
      mockApi.getVisualAssetPlanDetail.mockReturnValue(apiSuccess(detail))

      render(
        <VisualAssetWorkflowSection contentProjectId={PROJECT_ID} approvedStoryboardId={STORYBOARD_ID} />
      )

      await waitFor(() => { screen.getByText('Detay') })
      await userEvent.click(screen.getByText('Detay'))

      await waitFor(() => {
        const titles = screen.getAllByText('Çanakkale Görsel Planı')
        expect(titles.length).toBeGreaterThanOrEqual(1)
        expect(screen.queryByText('✓ Onayla')).not.toBeInTheDocument()
        expect(screen.queryByText('✕ Reddet')).not.toBeInTheDocument()
      })
    })

    it('FAILED statusunda onayla/reddet butonları gizlidir', async () => {
      const detail = makeDetail({ status: 'FAILED' })
      mockApi.listVisualAssetPlansByContentProject.mockReturnValue(apiSuccess([makeSummary({ status: 'FAILED' })]))
      mockApi.getVisualAssetPlanDetail.mockReturnValue(apiSuccess(detail))

      render(
        <VisualAssetWorkflowSection contentProjectId={PROJECT_ID} approvedStoryboardId={STORYBOARD_ID} />
      )

      await waitFor(() => { screen.getByText('Detay') })
      await userEvent.click(screen.getByText('Detay'))

      await waitFor(() => {
        expect(screen.queryByText('✓ Onayla')).not.toBeInTheDocument()
        expect(screen.queryByText('✕ Reddet')).not.toBeInTheDocument()
      })
    })
  })

  // ── Edit Visibility ────────────────────────────────────

  describe('Düzenleme butonları', () => {
    it('AI_GENERATED statusunda düzenle butonları gösterilir', async () => {
      const detail = makeDetail({ status: 'AI_GENERATED' })
      mockApi.listVisualAssetPlansByContentProject.mockReturnValue(apiSuccess([makeSummary()]))
      mockApi.getVisualAssetPlanDetail.mockReturnValue(apiSuccess(detail))

      render(
        <VisualAssetWorkflowSection contentProjectId={PROJECT_ID} approvedStoryboardId={STORYBOARD_ID} />
      )

      await waitFor(() => { screen.getByText('Detay') })
      await userEvent.click(screen.getByText('Detay'))

      await waitFor(() => {
        const editButtons = screen.getAllByText(/Düzenle/)
        expect(editButtons.length).toBeGreaterThan(0)
      })
    })

    it('FAILED statusunda düzenle butonları gizlidir', async () => {
      const detail = makeDetail({ status: 'FAILED' })
      mockApi.listVisualAssetPlansByContentProject.mockReturnValue(apiSuccess([makeSummary({ status: 'FAILED' })]))
      mockApi.getVisualAssetPlanDetail.mockReturnValue(apiSuccess(detail))

      render(
        <VisualAssetWorkflowSection contentProjectId={PROJECT_ID} approvedStoryboardId={STORYBOARD_ID} />
      )

      await waitFor(() => { screen.getByText('Detay') })
      await userEvent.click(screen.getByText('Detay'))

      await waitFor(() => {
        expect(screen.queryByText('✏️ Düzenle')).not.toBeInTheDocument()
      })
    })
  })

  // ── Governance ─────────────────────────────────────────

  describe('Governance', () => {
    it('generic status dropdown yok', async () => {
      const detail = makeDetail()
      mockApi.listVisualAssetPlansByContentProject.mockReturnValue(apiSuccess([makeSummary()]))
      mockApi.getVisualAssetPlanDetail.mockReturnValue(apiSuccess(detail))

      render(
        <VisualAssetWorkflowSection contentProjectId={PROJECT_ID} approvedStoryboardId={STORYBOARD_ID} />
      )

      await waitFor(() => { screen.getByText('Detay') })
      await userEvent.click(screen.getByText('Detay'))

      await waitFor(() => {
        expect(screen.queryByRole('combobox')).not.toBeInTheDocument()
      })
    })

    it('READY_FOR_PUBLISH metni yok', async () => {
      const detail = makeDetail()
      mockApi.listVisualAssetPlansByContentProject.mockReturnValue(apiSuccess([makeSummary()]))
      mockApi.getVisualAssetPlanDetail.mockReturnValue(apiSuccess(detail))

      const { container } = render(
        <VisualAssetWorkflowSection contentProjectId={PROJECT_ID} approvedStoryboardId={STORYBOARD_ID} />
      )

      await waitFor(() => { screen.getByText('Detay') })
      await userEvent.click(screen.getByText('Detay'))

      await waitFor(() => {
        expect(container.textContent).not.toContain('READY_FOR_PUBLISH')
        expect(container.textContent).not.toContain('Yayına Hazır')
      })
    })

    it('image generation/upload/publish butonları yok', async () => {
      const detail = makeDetail()
      mockApi.listVisualAssetPlansByContentProject.mockReturnValue(apiSuccess([makeSummary()]))
      mockApi.getVisualAssetPlanDetail.mockReturnValue(apiSuccess(detail))

      render(
        <VisualAssetWorkflowSection contentProjectId={PROJECT_ID} approvedStoryboardId={STORYBOARD_ID} />
      )

      await waitFor(() => { screen.getByText('Detay') })
      await userEvent.click(screen.getByText('Detay'))

      await waitFor(() => {
        expect(screen.queryByText(/Generate Image/i)).not.toBeInTheDocument()
        expect(screen.queryByText(/Create Image/i)).not.toBeInTheDocument()
        expect(screen.queryByText(/Upload Image/i)).not.toBeInTheDocument()
        expect(screen.queryByText(/Publish/i)).not.toBeInTheDocument()
        expect(screen.queryByText(/Schedule/i)).not.toBeInTheDocument()
        expect(screen.queryByText(/Media Library/i)).not.toBeInTheDocument()
      })
    })

    it('forbidden fields render edilmez', async () => {
      const detail = makeDetail()
      mockApi.listVisualAssetPlansByContentProject.mockReturnValue(apiSuccess([makeSummary()]))
      mockApi.getVisualAssetPlanDetail.mockReturnValue(apiSuccess(detail))

      const { container } = render(
        <VisualAssetWorkflowSection contentProjectId={PROJECT_ID} approvedStoryboardId={STORYBOARD_ID} />
      )

      await waitFor(() => { screen.getByText('Detay') })
      await userEvent.click(screen.getByText('Detay'))

      await waitFor(() => {
        const html = container.innerHTML
        const forbidden = [
          'rawPrompt', 'internalPrompt', 'providerRequest', 'providerResponse',
          'providerPayload', 'apiKey', 'secret', 'imageUrl', 'objectKey',
          'bucket', 'mediaAssetId', 'generatedAssetId',
        ]
        forbidden.forEach(field => {
          expect(html).not.toContain(field)
        })
      })
    })
  })
})
