// ── API Response ────────────────────────────────────────────

export interface FieldError {
  field: string;
  message: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message: string | null;
  errors: FieldError[] | null;
  timestamp: string;
}

// ── Health ──────────────────────────────────────────────────

export interface HealthResponse {
  status: string;
  service: string;
}

// ── Knowledge Domain ────────────────────────────────────────

export interface CreateKnowledgeItemRequest {
  title: string;
  type: string;
  language: string;
  summary?: string;
  description?: string;
  category?: string;
  city?: string;
  district?: string;
  locationText?: string;
  notes?: string;
}

export interface UpdateKnowledgeItemRequest {
  title: string;
  type?: string;
  language?: string;
  summary?: string;
  description?: string;
  category?: string;
  city?: string;
  district?: string;
  locationText?: string;
  notes?: string;
}

export interface KnowledgeItemResponse {
  id: string;
  title: string;
  slug: string;
  type: string;
  summary: string | null;
  description: string | null;
  category: string | null;
  city: string | null;
  district: string | null;
  locationText: string | null;
  notes: string | null;
  status: string;
  language: string;
  createdAt: string;
  updatedAt: string | null;
}

export interface KnowledgeItemSummaryResponse {
  id: string;
  title: string;
  slug: string;
  type: string;
  category: string | null;
  city: string | null;
  status: string;
  language: string;
  createdAt: string;
}

export interface ActionStatusResponse {
  id: string;
  status: string;
  message: string;
  timestamp: string;
}

export const KNOWLEDGE_ITEM_TYPES = [
  { value: 'FOLK_SONG', label: 'Türkü' },
  { value: 'ARTIST', label: 'Sanatçı' },
  { value: 'REGION', label: 'Bölge' },
  { value: 'TRADITION', label: 'Gelenek' },
  { value: 'HISTORICAL_EVENT', label: 'Tarihî Olay' },
  { value: 'LEGEND', label: 'Efsane' },
] as const;

export const KNOWLEDGE_ITEM_STATUSES = [
  { value: 'DRAFT', label: 'Taslak' },
  { value: 'READY', label: 'Hazır' },
  { value: 'ARCHIVED', label: 'Arşivlenmiş' },
] as const;

export const LANGUAGES = [
  { value: 'tr', label: 'Türkçe' },
  { value: 'en', label: 'English' },
] as const;

// ── Content Domain (FP-001) ─────────────────────────────────

export interface CreateContentProjectRequest {
  knowledgeItemId: string;
  contentPurpose?: string;
  targetAudience?: string;
  tone?: string;
  language: string;
  formatHint?: string;
  lengthHint?: string;
}

export interface ContentProjectResponse {
  id: string;
  knowledgeItemId: string;
  knowledgeItemTitle: string;
  contentPurpose: string | null;
  targetAudience: string | null;
  tone: string | null;
  language: string;
  formatHint: string | null;
  lengthHint: string | null;
  status: string;
  retryCount: number;
  lastErrorType: string | null;
  lastErrorMessage: string | null;
  latestDraftId: string | null;
  createdAt: string;
  updatedAt: string | null;
}

export interface ContentProjectSummaryResponse {
  id: string;
  knowledgeItemId: string;
  knowledgeItemTitle: string;
  contentPurpose: string | null;
  status: string;
  language: string;
  createdAt: string;
}

export interface AiGenerationSummaryResponse {
  id: string | null;
  contentProjectId: string;
  success: boolean;
  contentDraftId: string | null;
  projectStatus: string;
  draftTitle: string | null;
  message: string | null;
  errorType: string | null;
  retryable: boolean | null;
  retryCount: number | null;
  maxRetryCount: number | null;
}

export interface ContentDraftResponse {
  id: string;
  contentProjectId: string;
  title: string;
  body: string;
  summary: string | null;
  status: string;
  generatedBy: string;
  createdAt: string;
  updatedAt: string | null;
}

export interface GenerateAiDraftRequest {
  humanFeedbackNote?: string;
  feedbackType?: string;
}

export interface ReviewNoteRequest {
  reviewNote: string;
}

export interface ApproveRequest {
  reviewNote?: string;
}

export const CONTENT_PURPOSES = [
  { value: 'INFORMATIONAL_TEXT', label: 'Bilgilendirici Metin' },
  { value: 'CHILD_FRIENDLY_TEXT', label: 'Çocuklara Uygun Metin' },
  { value: 'TOURISTIC_GUIDE_TEXT', label: 'Turistik Rehber' },
  { value: 'SOCIAL_MEDIA_POST', label: 'Sosyal Medya' },
  { value: 'MOBILE_APP_CARD', label: 'Mobil Kart' },
  { value: 'AUDIO_GUIDE_SCRIPT', label: 'Sesli Rehber' },
] as const;

export const TARGET_AUDIENCES = [
  { value: 'GENERAL_USER', label: 'Genel Kullanıcı' },
  { value: 'CHILD', label: 'Çocuk' },
  { value: 'TOURIST', label: 'Turist' },
  { value: 'STUDENT', label: 'Öğrenci' },
  { value: 'RESEARCHER', label: 'Araştırmacı' },
  { value: 'MOBILE_APP_USER', label: 'Mobil Kullanıcı' },
] as const;

export const CONTENT_TONES = [
  { value: 'INFORMATIVE', label: 'Bilgilendirici' },
  { value: 'SIMPLE', label: 'Sade' },
  { value: 'STORYTELLING', label: 'Hikâye Anlatıcı' },
  { value: 'NON_ACADEMIC', label: 'Gündelik' },
  { value: 'CHILD_FRIENDLY', label: 'Çocuklara Uygun' },
  { value: 'TOURISTIC', label: 'Turistik' },
  { value: 'SHORT_AND_CLEAR', label: 'Kısa ve Net' },
] as const;

export const CONTENT_PROJECT_STATUSES = [
  { value: 'CREATED', label: 'Oluşturuldu' },
  { value: 'AI_REQUESTED', label: 'AI Bekleniyor' },
  { value: 'AI_PROCESSING', label: 'AI İşliyor' },
  { value: 'DRAFT_READY', label: 'Taslak Hazır' },
  { value: 'IN_REVIEW', label: 'İncelemede' },
  { value: 'REVISION_REQUESTED', label: 'Revizyon Gerekli' },
  { value: 'APPROVED', label: 'Onaylandı' },
  { value: 'READY_FOR_PUBLISH', label: 'Yayına Hazır' },
  { value: 'FAILED', label: 'Başarısız' },
  { value: 'REJECTED', label: 'Reddedildi' },
  { value: 'CANCELLED', label: 'İptal Edildi' },
] as const;

// Legacy constants — kept for backward compatibility with existing pages
export const CONTENT_TYPES = [
  { value: 'LONG_VIDEO', label: 'Uzun Video' },
  { value: 'SHORT_VIDEO', label: 'Kısa Video' },
  { value: 'REELS', label: 'Reels' },
  { value: 'BLOG', label: 'Blog' },
  { value: 'PODCAST', label: 'Podcast' },
  { value: 'CAROUSEL', label: 'Carousel' },
] as const;

export const TARGET_PLATFORMS = [
  { value: 'YOUTUBE', label: 'YouTube' },
  { value: 'YOUTUBE_SHORTS', label: 'YouTube Shorts' },
  { value: 'INSTAGRAM', label: 'Instagram' },
  { value: 'FACEBOOK', label: 'Facebook' },
  { value: 'TIKTOK', label: 'TikTok' },
  { value: 'WEBSITE', label: 'Website' },
  { value: 'SPOTIFY', label: 'Spotify' },
] as const;

// ── Media Domain ────────────────────────────────────────────

export interface RegisterMediaAssetRequest {
  assetType: string;
  storageProvider: string;
  bucket: string;
  objectKey: string;
  fileName?: string;
  mimeType?: string;
  sizeBytes?: number | null;
  durationSeconds?: number | null;
}

export interface MediaAssetResponse {
  id: string;
  assetType: string;
  storageProvider: string;
  bucket: string;
  objectKey: string;
  fileName: string | null;
  mimeType: string | null;
  sizeBytes: number | null;
  createdAt: string;
}

export const ASSET_TYPES = [
  { value: 'IMAGE', label: 'Görsel' },
  { value: 'VIDEO', label: 'Video' },
  { value: 'AUDIO', label: 'Ses' },
  { value: 'MUSIC', label: 'Müzik' },
  { value: 'VOICEOVER', label: 'Seslendirme' },
  { value: 'THUMBNAIL', label: 'Thumbnail' },
  { value: 'SUBTITLE', label: 'Altyazı' },
  { value: 'DOCUMENT', label: 'Doküman' },
] as const;

export const STORAGE_PROVIDERS = [
  { value: 'MINIO', label: 'MinIO' },
  { value: 'S3', label: 'Amazon S3' },
  { value: 'LOCAL', label: 'Yerel' },
] as const;

// ── Scenario Domain (FP-002) ───────────────────────────────────

export type ScenarioStatus = 'AI_GENERATED' | 'NEEDS_REVIEW' | 'APPROVED' | 'REJECTED' | 'FAILED'

export const SCENARIO_STATUSES: { value: ScenarioStatus; label: string }[] = [
  { value: 'AI_GENERATED', label: 'AI Generated' },
  { value: 'NEEDS_REVIEW', label: 'Needs Review' },
  { value: 'APPROVED', label: 'Onaylandı' },
  { value: 'REJECTED', label: 'Reddedildi' },
  { value: 'FAILED', label: 'Başarısız' },
]

export const SCENARIO_BEAT_TYPES = [
  { value: 'HOOK', label: 'Hook', order: 1 },
  { value: 'INTRODUCTION', label: 'Giriş', order: 2 },
  { value: 'MAIN_NARRATIVE', label: 'Hikâye', order: 3 },
  { value: 'EMOTIONAL_CLIMAX', label: 'Duygusal Zirve', order: 4 },
  { value: 'CONCLUSION', label: 'Kapanış', order: 5 },
  { value: 'CALL_TO_ACTION', label: 'CTA', order: 6 },
] as const

export interface ScenarioBeat {
  type: string
  label: string
  body: string
  orderIndex: number
  estimatedDurationSeconds: number | null
}

export interface ScenarioDraftSummaryResponse {
  id: string
  version: number
  status: ScenarioStatus
  title: string | null
  generatedBy: string | null
  providerType: string | null
  createdAt: string
  approvedAt: string | null
  rejectedAt: string | null
  editedAt: string | null
}

export interface ScenarioDraftResponse {
  id: string
  contentProjectId: string
  sourceDraftId: string
  version: number
  status: ScenarioStatus
  title: string | null
  summary: string | null
  targetAudience: string | null
  culturalObjective: string | null
  tone: string | null
  estimatedDurationSeconds: number | null
  setting: string | null
  characters: string | null
  reviewNotes: string | null
  generatedBy: string | null
  providerType: string | null
  modelName: string | null
  createdAt: string
  updatedAt: string | null
  approvedAt: string | null
  rejectedAt: string | null
  editedAt: string | null
  createdBy: string | null
  approvedBy: string | null
  rejectedBy: string | null
  editedBy: string | null
  beats: ScenarioBeat[]
}

export interface ScenarioBeatEditPayload {
  type: string
  label: string
  body: string
  orderIndex: number
  estimatedDurationSeconds: number | null
}

export interface ScenarioDraftEditPayload {
  title?: string
  summary?: string
  targetAudience?: string
  culturalObjective?: string
  tone?: string
  estimatedDurationSeconds?: number | null
  setting?: string
  characters?: string
  reviewNotes?: string
  beats?: ScenarioBeatEditPayload[]
}

export interface ScenarioGenerateResponse {
  success: boolean
  contentProjectId: string
  scenarioDraftId: string | null
  version: number | null
  status: string | null
  scenarioTitle: string | null
  message: string | null
  errorCode: string | null
}

export interface ScenarioRejectRequest {
  reason: string
}

// ── Storyboard Domain (FP-003) ────────────────────────────────

export type StoryboardStatus = 'AI_GENERATED' | 'NEEDS_REVIEW' | 'APPROVED' | 'REJECTED' | 'FAILED'

export const STORYBOARD_STATUSES: { value: StoryboardStatus; label: string }[] = [
  { value: 'AI_GENERATED', label: 'AI Generated' },
  { value: 'NEEDS_REVIEW', label: 'Needs Review' },
  { value: 'APPROVED', label: 'Onaylandı' },
  { value: 'REJECTED', label: 'Reddedildi' },
  { value: 'FAILED', label: 'Başarısız' },
]

export interface StoryboardScene {
  id: string
  sceneNumber: number
  sourceBeatType: string | null
  sourceBeatOrder: number | null
  sceneTitle: string | null
  visualDescription: string | null
  narrationText: string | null
  onScreenText: string | null
  cameraDirection: string | null
  transitionNote: string | null
  mood: string | null
  location: string | null
  characters: string | null
  estimatedDurationSeconds: number | null
}

export interface StoryboardDraftSummaryResponse {
  id: string
  version: number
  status: StoryboardStatus
  title: string | null
  sceneCount: number
  generatedBy: string | null
  providerType: string | null
  createdAt: string
  approvedAt: string | null
  rejectedAt: string | null
  editedAt: string | null
}

export interface StoryboardDraftResponse {
  id: string
  contentProjectId: string
  sourceDraftId: string
  sourceScenarioDraftId: string
  sourceScenarioVersion: number | null
  version: number
  status: StoryboardStatus
  title: string | null
  summary: string | null
  targetPlatform: string | null
  aspectRatio: string | null
  estimatedDurationSeconds: number | null
  visualStyleGuide: string | null
  narrationStyle: string | null
  reviewNotes: string | null
  generatedBy: string | null
  providerType: string | null
  modelName: string | null
  createdAt: string
  updatedAt: string | null
  approvedAt: string | null
  rejectedAt: string | null
  editedAt: string | null
  createdBy: string | null
  approvedBy: string | null
  rejectedBy: string | null
  editedBy: string | null
  scenes: StoryboardScene[]
}

export interface StoryboardSceneEditPayload {
  sceneNumber: number
  sceneTitle?: string
  visualDescription?: string
  narrationText?: string
  onScreenText?: string
  cameraDirection?: string
  transitionNote?: string
  mood?: string
  location?: string
  characters?: string
  estimatedDurationSeconds?: number | null
}

export interface StoryboardDraftEditPayload {
  title?: string
  summary?: string
  targetPlatform?: string
  aspectRatio?: string
  estimatedDurationSeconds?: number | null
  visualStyleGuide?: string
  narrationStyle?: string
  reviewNotes?: string
  scenes?: StoryboardSceneEditPayload[]
}

export interface StoryboardGenerateResponse {
  success: boolean
  contentProjectId: string
  storyboardDraftId: string | null
  version: number | null
  status: string | null
  storyboardTitle: string | null
  sceneCount: number | null
  message: string | null
  errorCode: string | null
}

export interface StoryboardRejectRequest {
  reason: string
}
