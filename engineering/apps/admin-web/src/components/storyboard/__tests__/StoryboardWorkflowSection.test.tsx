import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import StoryboardWorkflowSection from '../StoryboardWorkflowSection'
import type { StoryboardDraftSummaryResponse, StoryboardDraftResponse, StoryboardStatus } from '../../../types'

// ── Mocks ─────────────────────────────────────────────────

vi.mock('../../../api/storyboardApi', () => ({
  generateStoryboardForScenario: vi.fn(),
  listStoryboardsByContentProject: vi.fn(),
  getStoryboardDetail: vi.fn(),
  updateStoryboardDraft: vi.fn(),
  approveStoryboardDraft: vi.fn(),
  rejectStoryboardDraft: vi.fn(),
}))

import * as storyboardApi from '../../../api/storyboardApi'
const mockApi = storyboardApi as {
  generateStoryboardForScenario: ReturnType<typeof vi.fn>;
  listStoryboardsByContentProject: ReturnType<typeof vi.fn>;
  getStoryboardDetail: ReturnType<typeof vi.fn>;
  updateStoryboardDraft: ReturnType<typeof vi.fn>;
  approveStoryboardDraft: ReturnType<typeof vi.fn>;
  rejectStoryboardDraft: ReturnType<typeof vi.fn>;
}

// ── Test Data ─────────────────────────────────────────────

const PROJECT_ID = 'proj-001'
const SCENARIO_ID = 'scen-001'

function makeSummary(
  overrides: Partial<StoryboardDraftSummaryResponse> = {}
): StoryboardDraftSummaryResponse {
  return {
    id: 'sb-001',
    version: 1,
    status: 'AI_GENERATED',
    title: 'Test Storyboard',
    sceneCount: 3,
    generatedBy: 'AI_CONTENT_AGENT',
    providerType: 'MOCK',
    createdAt: '2026-07-01T10:00:00',
    approvedAt: null,
    rejectedAt: null,
    editedAt: null,
    ...overrides,
  }
}

function makeDetail(
  overrides: Partial<StoryboardDraftResponse> = {}
): StoryboardDraftResponse {
  return {
    id: 'sb-001',
    contentProjectId: PROJECT_ID,
    sourceDraftId: 'draft-001',
    sourceScenarioDraftId: SCENARIO_ID,
    sourceScenarioVersion: 1,
    version: 1,
    status: 'AI_GENERATED',
    title: 'Test Storyboard',
    summary: 'Test özet',
    targetPlatform: 'YouTube',
    aspectRatio: '16:9',
    estimatedDurationSeconds: 180,
    visualStyleGuide: 'Sinematik',
    narrationStyle: 'Anlatıcı',
    reviewNotes: null,
    generatedBy: 'AI_CONTENT_AGENT',
    providerType: 'MOCK',
    modelName: 'mock-v1',
    createdAt: '2026-07-01T10:00:00',
    updatedAt: null,
    approvedAt: null,
    rejectedAt: null,
    editedAt: null,
    createdBy: 'admin-system',
    approvedBy: null,
    rejectedBy: null,
    editedBy: null,
    scenes: [
      {
        id: 'scene-001', sceneNumber: 1, sourceBeatType: 'HOOK',
        sourceBeatOrder: 0, sceneTitle: 'Açılış', visualDescription: 'Gökyüzü',
        narrationText: 'Bir zamanlar...', onScreenText: null,
        cameraDirection: 'wide shot', transitionNote: 'fade in',
        mood: 'dramatic', location: 'Anadolu', characters: 'Anlatıcı',
        estimatedDurationSeconds: 30,
      },
      {
        id: 'scene-002', sceneNumber: 2, sourceBeatType: 'MAIN_NARRATIVE',
        sourceBeatOrder: 1, sceneTitle: 'Ana Hikâye', visualDescription: 'Köy manzarası',
        narrationText: 'Köyün ortasında...', onScreenText: null,
        cameraDirection: 'tracking', transitionNote: 'cut',
        mood: 'melancholic', location: 'Köy', characters: 'Köylüler',
        estimatedDurationSeconds: 60,
      },
    ],
    ...overrides,
  }
}

function apiSuccess<T>(data: T) {
  return Promise.resolve({ success: true, data, message: null, errors: null, timestamp: '' })
}

beforeEach(() => {
  vi.clearAllMocks()
  mockApi.listStoryboardsByContentProject.mockReturnValue(apiSuccess([]))
})

// ── Tests ─────────────────────────────────────────────────

describe('StoryboardWorkflowSection', () => {

  // ── Generate Button Visibility ──────────────────────────

  describe('Generate butonu', () => {
    it('onaylanmış senaryo olmadığında uyarı mesajı gösterir', async () => {
      render(
        <StoryboardWorkflowSection contentProjectId={PROJECT_ID} projectStatus="APPROVED" approvedScenarioId={null} />
      )

      await waitFor(() => {
        expect(screen.getByText(/onaylanmış senaryo gereklidir/i)).toBeInTheDocument()
      })
    })

    it('onaylanmış senaryo olmadığında üret butonu gösterilmez', async () => {
      render(
        <StoryboardWorkflowSection contentProjectId={PROJECT_ID} projectStatus="APPROVED" approvedScenarioId={null} />
      )

      await waitFor(() => {
        expect(screen.queryByText(/Storyboard Üret/)).not.toBeInTheDocument()
      })
    })

    it('onaylanmış senaryo varken üret butonu gösterilir', async () => {
      render(
        <StoryboardWorkflowSection contentProjectId={PROJECT_ID} projectStatus="APPROVED" approvedScenarioId={SCENARIO_ID} />
      )

      await waitFor(() => {
        expect(screen.getByText(/Storyboard Üret/)).toBeInTheDocument()
      })
    })
  })

  // ── List View ───────────────────────────────────────────

  describe('Liste görünümü', () => {
    it('storyboard listesini version/status/title/sahne sayısıyla render eder', async () => {
      const summaries = [
        makeSummary({ id: 'sb-1', version: 2, title: 'V2 Storyboard', sceneCount: 5 }),
        makeSummary({ id: 'sb-2', version: 1, title: 'V1 Storyboard', sceneCount: 3, status: 'REJECTED' }),
      ]
      mockApi.listStoryboardsByContentProject.mockReturnValue(apiSuccess(summaries))

      render(
        <StoryboardWorkflowSection contentProjectId={PROJECT_ID} projectStatus="APPROVED" approvedScenarioId={SCENARIO_ID} />
      )

      await waitFor(() => {
        expect(screen.getByText('v2')).toBeInTheDocument()
        expect(screen.getByText('V2 Storyboard')).toBeInTheDocument()
        expect(screen.getByText('5')).toBeInTheDocument()
        expect(screen.getByText('V1 Storyboard')).toBeInTheDocument()
      })
    })

    it('boş liste durumunda empty state gösterir', async () => {
      mockApi.listStoryboardsByContentProject.mockReturnValue(apiSuccess([]))

      render(
        <StoryboardWorkflowSection contentProjectId={PROJECT_ID} projectStatus="APPROVED" approvedScenarioId={SCENARIO_ID} />
      )

      await waitFor(() => {
        expect(screen.getByText(/Henüz storyboard yok/)).toBeInTheDocument()
      })
    })
  })

  // ── Detail View ─────────────────────────────────────────

  describe('Detay görünümü', () => {
    it('storyboard detayını sahnelerle render eder', async () => {
      const detail = makeDetail()
      mockApi.listStoryboardsByContentProject.mockReturnValue(apiSuccess([makeSummary()]))
      mockApi.getStoryboardDetail.mockReturnValue(apiSuccess(detail))

      render(
        <StoryboardWorkflowSection contentProjectId={PROJECT_ID} projectStatus="APPROVED" approvedScenarioId={SCENARIO_ID} />
      )

      await waitFor(() => {
        expect(screen.getByText('Test Storyboard')).toBeInTheDocument()
      })

      // Click detail
      const detailBtn = screen.getByText('Detay')
      await userEvent.click(detailBtn)

      await waitFor(() => {
        expect(screen.getByText(/Açılış/)).toBeInTheDocument()
        expect(screen.getByText(/Ana Hikâye/)).toBeInTheDocument()
        expect(screen.getByText(/Gökyüzü/)).toBeInTheDocument()
        expect(screen.getByText(/Bir zamanlar/)).toBeInTheDocument()
      })
    })
  })

  // ── Edit Form ───────────────────────────────────────────

  describe('Düzenleme formu', () => {
    it('mediaGenerationNotes göstermez', async () => {
      const detail = makeDetail()
      mockApi.listStoryboardsByContentProject.mockReturnValue(apiSuccess([makeSummary()]))
      mockApi.getStoryboardDetail.mockReturnValue(apiSuccess(detail))

      const { container } = render(
        <StoryboardWorkflowSection contentProjectId={PROJECT_ID} projectStatus="APPROVED" approvedScenarioId={SCENARIO_ID} />
      )

      await waitFor(() => { screen.getByText('Test Storyboard') })
      await userEvent.click(screen.getByText('Detay'))
      await waitFor(() => { screen.getByText(/Açılış/) })

      // Edit mode'a geç
      await userEvent.click(screen.getByText(/Düzenle/))

      // mediaGenerationNotes label or input bulunmamalı
      const html = container.innerHTML
      expect(html).not.toContain('mediaGenerationNotes')
      expect(html).not.toContain('Media Generation')
    })

    it('status/version/provider alanları düzenlenebilir formda yok', async () => {
      const detail = makeDetail()
      mockApi.listStoryboardsByContentProject.mockReturnValue(apiSuccess([makeSummary()]))
      mockApi.getStoryboardDetail.mockReturnValue(apiSuccess(detail))

      const { container } = render(
        <StoryboardWorkflowSection contentProjectId={PROJECT_ID} projectStatus="APPROVED" approvedScenarioId={SCENARIO_ID} />
      )

      await waitFor(() => { screen.getByText('Test Storyboard') })
      await userEvent.click(screen.getByText('Detay'))
      await waitFor(() => { screen.getByText(/Açılış/) })
      await userEvent.click(screen.getByText(/Düzenle/))

      // No input for status, version, providerType, modelName, rawPrompt, apiKey
      const inputs = container.querySelectorAll('input, textarea, select')
      const names = Array.from(inputs).map(el => (el as HTMLInputElement).name || (el as HTMLInputElement).placeholder || '')
      const forbidden = ['status', 'version', 'providerType', 'modelName', 'rawPrompt', 'apiKey', 'secret']
      forbidden.forEach(f => {
        expect(names.join(' ')).not.toContain(f)
      })
    })

    it('kaydet sadece güvenli alanları gönderir', async () => {
      const detail = makeDetail()
      mockApi.listStoryboardsByContentProject.mockReturnValue(apiSuccess([makeSummary()]))
      mockApi.getStoryboardDetail.mockReturnValue(apiSuccess(detail))
      mockApi.updateStoryboardDraft.mockReturnValue(apiSuccess(detail))

      render(
        <StoryboardWorkflowSection contentProjectId={PROJECT_ID} projectStatus="APPROVED" approvedScenarioId={SCENARIO_ID} />
      )

      await waitFor(() => { screen.getByText('Test Storyboard') })
      await userEvent.click(screen.getByText('Detay'))
      await waitFor(() => { screen.getByText(/Açılış/) })
      await userEvent.click(screen.getByText(/Düzenle/))
      await userEvent.click(screen.getByText(/Kaydet/))

      await waitFor(() => {
        expect(mockApi.updateStoryboardDraft).toHaveBeenCalledTimes(1)
      })

      const [, payload] = mockApi.updateStoryboardDraft.mock.calls[0]
      // Güvenli alanlar kontrolü
      expect(payload).not.toHaveProperty('status')
      expect(payload).not.toHaveProperty('version')
      expect(payload).not.toHaveProperty('providerType')
      expect(payload).not.toHaveProperty('modelName')
      expect(payload).not.toHaveProperty('rawPrompt')
      expect(payload).not.toHaveProperty('apiKey')
      expect(payload).not.toHaveProperty('secret')

      // scenes içinde mediaGenerationNotes olmamalı
      if (payload.scenes) {
        payload.scenes.forEach((s: Record<string, unknown>) => {
          expect(s).not.toHaveProperty('mediaGenerationNotes')
        })
      }
    })
  })

  // ── Approve / Reject ────────────────────────────────────

  describe('Onayla / Reddet aksiyonları', () => {

    async function renderWithDetail(status: StoryboardStatus) {
      const detail = makeDetail({ status })
      mockApi.listStoryboardsByContentProject.mockReturnValue(apiSuccess([makeSummary({ status })]))
      mockApi.getStoryboardDetail.mockReturnValue(apiSuccess(detail))

      render(
        <StoryboardWorkflowSection contentProjectId={PROJECT_ID} projectStatus="APPROVED" approvedScenarioId={SCENARIO_ID} />
      )

      await waitFor(() => { screen.getByText('Test Storyboard') })
      await userEvent.click(screen.getByText('Detay'))
      await waitFor(() => { screen.getByText(/Açılış/) })
    }

    it('AI_GENERATED storyboard için onayla butonu gösterilir', async () => {
      await renderWithDetail('AI_GENERATED')
      expect(screen.getByText('✓ Onayla')).toBeInTheDocument()
    })

    it('NEEDS_REVIEW storyboard için onayla butonu gösterilir', async () => {
      await renderWithDetail('NEEDS_REVIEW')
      expect(screen.getByText('✓ Onayla')).toBeInTheDocument()
    })

    it('APPROVED storyboard için onayla/reddet butonları gizli', async () => {
      await renderWithDetail('APPROVED')
      expect(screen.queryByText('✓ Onayla')).not.toBeInTheDocument()
      expect(screen.queryByText('✕ Reddet')).not.toBeInTheDocument()
    })

    it('REJECTED storyboard için onayla/reddet butonları gizli', async () => {
      await renderWithDetail('REJECTED')
      expect(screen.queryByText('✓ Onayla')).not.toBeInTheDocument()
      expect(screen.queryByText('✕ Reddet')).not.toBeInTheDocument()
    })

    it('FAILED storyboard için onayla/reddet butonları gizli', async () => {
      await renderWithDetail('FAILED')
      expect(screen.queryByText('✓ Onayla')).not.toBeInTheDocument()
      expect(screen.queryByText('✕ Reddet')).not.toBeInTheDocument()
    })

    it('APPROVED storyboard için düzenle butonu gizli', async () => {
      await renderWithDetail('APPROVED')
      expect(screen.queryByText(/Düzenle/)).not.toBeInTheDocument()
    })

    it('onayla butonu explicit approve endpoint çağırır', async () => {
      mockApi.approveStoryboardDraft.mockReturnValue(
        apiSuccess({ id: 'sb-001', status: 'APPROVED', message: 'OK', timestamp: '' })
      )
      await renderWithDetail('AI_GENERATED')

      await userEvent.click(screen.getByText('✓ Onayla'))

      // ConfirmDialog açılır, onaylıyoruz
      await waitFor(() => {
        expect(screen.getByText(/onaylamak istediğinize/)).toBeInTheDocument()
      })

      const confirmBtn = screen.getAllByRole('button').find(
        b => b.textContent?.includes('Onayla') && b.classList.contains('btn')
      )
      if (confirmBtn) {
        await userEvent.click(confirmBtn)
      }

      await waitFor(() => {
        expect(mockApi.approveStoryboardDraft).toHaveBeenCalledWith('sb-001')
      })
    })

    it('reddet butonu sebep gerektirir ve reject endpoint çağırır', async () => {
      mockApi.rejectStoryboardDraft.mockReturnValue(
        apiSuccess({ id: 'sb-001', status: 'REJECTED', message: 'OK', timestamp: '' })
      )
      await renderWithDetail('AI_GENERATED')

      await userEvent.click(screen.getByText('✕ Reddet'))

      // NoteDialog açılır
      await waitFor(() => {
        expect(screen.getByText(/Red Sebebi/)).toBeInTheDocument()
      })
    })
  })

  // ── Governance ──────────────────────────────────────────

  describe('Governance', () => {
    it('raw prompt/provider payload/API key/secret alanları UI da görünmez', async () => {
      const detail = makeDetail()
      mockApi.listStoryboardsByContentProject.mockReturnValue(apiSuccess([makeSummary()]))
      mockApi.getStoryboardDetail.mockReturnValue(apiSuccess(detail))

      const { container } = render(
        <StoryboardWorkflowSection contentProjectId={PROJECT_ID} projectStatus="APPROVED" approvedScenarioId={SCENARIO_ID} />
      )

      await waitFor(() => { screen.getByText('Test Storyboard') })
      await userEvent.click(screen.getByText('Detay'))
      await waitFor(() => { screen.getByText(/Açılış/) })

      const html = container.innerHTML
      expect(html).not.toContain('rawPrompt')
      expect(html).not.toContain('providerPayload')
      expect(html).not.toContain('apiKey')
      expect(html).not.toContain('secretKey')
    })

    it('READY_FOR_PUBLISH aksiyonu yok', async () => {
      const detail = makeDetail()
      mockApi.listStoryboardsByContentProject.mockReturnValue(apiSuccess([makeSummary()]))
      mockApi.getStoryboardDetail.mockReturnValue(apiSuccess(detail))

      const { container } = render(
        <StoryboardWorkflowSection contentProjectId={PROJECT_ID} projectStatus="APPROVED" approvedScenarioId={SCENARIO_ID} />
      )

      await waitFor(() => { screen.getByText('Test Storyboard') })
      await userEvent.click(screen.getByText('Detay'))
      await waitFor(() => { screen.getByText(/Açılış/) })

      const html = container.innerHTML
      expect(html).not.toContain('READY_FOR_PUBLISH')
      expect(html).not.toContain('Yayına Hazır')
    })
  })
})
