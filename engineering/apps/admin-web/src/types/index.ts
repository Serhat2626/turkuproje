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

// ── Platform Package Domain (FP-004) ──────────────────────────

export type PlatformPackageType = 'YOUTUBE_LONG_FORM' | 'YOUTUBE_SHORTS' | 'INSTAGRAM_REELS' | 'FACEBOOK_REELS'

export type PlatformPackageStatus = 'AI_GENERATED' | 'NEEDS_REVIEW' | 'APPROVED' | 'REJECTED' | 'FAILED'

export const PLATFORM_PACKAGE_TYPES: { value: PlatformPackageType; label: string }[] = [
  { value: 'YOUTUBE_LONG_FORM', label: 'YouTube Long-Form' },
  { value: 'YOUTUBE_SHORTS', label: 'YouTube Shorts' },
  { value: 'INSTAGRAM_REELS', label: 'Instagram Reels' },
  { value: 'FACEBOOK_REELS', label: 'Facebook Reels' },
]

export const PLATFORM_PACKAGE_STATUSES: { value: PlatformPackageStatus; label: string }[] = [
  { value: 'AI_GENERATED', label: 'AI Generated' },
  { value: 'NEEDS_REVIEW', label: 'Needs Review' },
  { value: 'APPROVED', label: 'Onaylandı' },
  { value: 'REJECTED', label: 'Reddedildi' },
  { value: 'FAILED', label: 'Başarısız' },
]

export interface PlatformPackageSceneUsageResponse {
  id: string
  sceneNumber: number
  sourceStoryboardSceneId: string | null
  usageType: string
  included: boolean
  adaptationNote: string | null
  createdAt: string
  updatedAt: string | null
}

export interface PlatformPackageDraftSummaryResponse {
  id: string
  contentProjectId: string
  sourceStoryboardDraftId: string
  packageType: PlatformPackageType
  status: PlatformPackageStatus
  version: number
  title: string | null
  captionOrDescription: string | null
  generatedBy: string | null
  providerType: string | null
  targetDurationSeconds: number | null
  aspectRatio: string | null
  createdAt: string
  editedAt: string | null
  approvedAt: string | null
  rejectedAt: string | null
}

export interface PlatformPackageDraftResponse {
  id: string
  contentProjectId: string
  sourceStoryboardDraftId: string
  packageType: PlatformPackageType
  status: PlatformPackageStatus
  version: number
  // Common
  title: string | null
  captionOrDescription: string | null
  hashtagsJson: string | null
  callToAction: string | null
  targetDurationSeconds: number | null
  aspectRatio: string | null
  contentStructureNotes: string | null
  sceneUsageNotes: string | null
  reviewNotes: string | null
  // YouTube Long-Form
  videoTitle: string | null
  longDescription: string | null
  chapterOutlineJson: string | null
  pinnedCommentSuggestion: string | null
  thumbnailTextSuggestion: string | null
  tagsJson: string | null
  // Short-Form / Reels
  shortHook: string | null
  shortCaption: string | null
  overlayTextSuggestionsJson: string | null
  shortHashtagSetJson: string | null
  shortCta: string | null
  durationGuidance: string | null
  // AI Metadata (safe)
  providerType: string | null
  modelName: string | null
  generatedBy: string | null
  // Review Lifecycle
  editedAt: string | null
  editedBy: string | null
  approvedAt: string | null
  approvedBy: string | null
  rejectedAt: string | null
  rejectedBy: string | null
  rejectionReason: string | null
  // Timestamps
  createdAt: string
  updatedAt: string | null
  // Scene Usages
  sceneUsages: PlatformPackageSceneUsageResponse[]
}

export interface PlatformPackageSceneUsageEditPayload {
  sceneNumber: number
  usageType?: string
  included?: boolean
  adaptationNote?: string
}

export interface PlatformPackageDraftEditPayload {
  // Common
  title?: string
  captionOrDescription?: string
  hashtagsJson?: string
  callToAction?: string
  targetDurationSeconds?: number | null
  aspectRatio?: string
  contentStructureNotes?: string
  sceneUsageNotes?: string
  reviewNotes?: string
  // YouTube Long-Form
  videoTitle?: string
  longDescription?: string
  chapterOutlineJson?: string
  pinnedCommentSuggestion?: string
  thumbnailTextSuggestion?: string
  // Short-Form / Reels
  shortHook?: string
  shortCaption?: string
  overlayTextSuggestionsJson?: string
  shortHashtagSetJson?: string
  shortCta?: string
  durationGuidance?: string
  // Scene Usages
  sceneUsages?: PlatformPackageSceneUsageEditPayload[]
}

export interface PlatformPackageGenerateResponse {
  success: boolean
  contentProjectId: string
  sourceStoryboardDraftId: string | null
  generatedPackageIds: string[] | null
  packageCount: number | null
  message: string | null
  errorCode: string | null
}

export interface PlatformPackageRejectRequest {
  reason: string
}

// ── Platform Package Export Domain (FP-005) ───────────────────

export type PlatformPackageExportType = 'MARKDOWN_BRIEF' | 'JSON_PACKAGE' | 'COPY_TEXT' | 'PRODUCTION_BRIEF'

export const PLATFORM_PACKAGE_EXPORT_TYPES: { value: PlatformPackageExportType; label: string; icon: string; ext: string }[] = [
  { value: 'MARKDOWN_BRIEF', label: 'Markdown Brief', icon: '📝', ext: '.md' },
  { value: 'JSON_PACKAGE', label: 'JSON Package', icon: '📦', ext: '.json' },
  { value: 'COPY_TEXT', label: 'Copy Text', icon: '📋', ext: '.txt' },
  { value: 'PRODUCTION_BRIEF', label: 'Production Brief', icon: '🎬', ext: '.md' },
]

export interface PlatformPackageExportSceneUsageDto {
  sourceStoryboardSceneId: string | null
  sceneNumber: number
  usageType: string
  included: boolean
  adaptationNote: string | null
}

export interface PlatformPackageExportMetadata {
  contentProjectId: string
  sourceStoryboardDraftId: string
  version: number
  packageStatus: string
  packageTitle: string
  approvedAt: string | null
  approvedBy: string | null
  providerType: string | null
  modelName: string | null
  generatedBy: string | null
  sceneUsages: PlatformPackageExportSceneUsageDto[]
}

export interface PlatformPackageExportResponse {
  packageId: string
  packageType: string
  exportType: PlatformPackageExportType
  filename: string
  contentType: string
  content: string
  exportedAt: string
  metadata: PlatformPackageExportMetadata
}

// ── Visual Asset Planning Domain (FP-006) ─────────────────────

export type VisualAssetPlanStatus = 'AI_GENERATED' | 'NEEDS_REVIEW' | 'APPROVED' | 'REJECTED' | 'FAILED'

export const VISUAL_ASSET_PLAN_STATUSES: { value: VisualAssetPlanStatus; label: string }[] = [
  { value: 'AI_GENERATED', label: 'AI Generated' },
  { value: 'NEEDS_REVIEW', label: 'Needs Review' },
  { value: 'APPROVED', label: 'Onaylandı' },
  { value: 'REJECTED', label: 'Reddedildi' },
  { value: 'FAILED', label: 'Başarısız' },
]

export interface SceneVisualPlanResponse {
  id: string
  sourceStoryboardSceneId: string | null
  sceneNumber: number
  sourceBeatType: string | null
  sceneTitle: string | null
  turkishVisualContext: string | null
  englishImagePrompt: string | null
  negativePrompt: string | null
  styleDirective: string | null
  aspectRatio: string | null
  compositionNotes: string | null
  cameraFraming: string | null
  lightingNotes: string | null
  colorPalette: string | null
  mood: string | null
  location: string | null
  characters: string | null
  culturalDetails: string | null
  textOverlayGuidance: string | null
  imageGenerationToolNotes: string | null
  safetyNotes: string | null
  createdAt: string
  updatedAt: string | null
}

export interface ThumbnailPlanResponse {
  id: string
  sourceStoryboardSceneId: string | null
  sourceSceneNumber: number | null
  thumbnailPrompt: string | null
  negativePrompt: string | null
  textOverlayGuidance: string | null
  compositionNotes: string | null
  emotionalHook: string | null
  aspectRatio: string | null
  styleDirective: string | null
  colorPalette: string | null
  createdAt: string
  updatedAt: string | null
}

export interface VisualAssetPlanResponse {
  id: string
  sourceStoryboardDraftId: string
  contentProjectId: string
  version: number
  status: VisualAssetPlanStatus
  title: string | null
  summary: string | null
  overallVisualStyle: string | null
  aspectRatioStrategy: string | null
  culturalAuthenticityGuidance: string | null
  characterContinuityGuidance: string | null
  locationContinuityGuidance: string | null
  colorPaletteGuidance: string | null
  reviewNotes: string | null
  generatedBy: string | null
  providerType: string | null
  modelName: string | null
  approvedAt: string | null
  approvedBy: string | null
  rejectedAt: string | null
  rejectedBy: string | null
  rejectionReason: string | null
  editedAt: string | null
  editedBy: string | null
  createdAt: string
  updatedAt: string | null
  scenePlans: SceneVisualPlanResponse[]
  thumbnailPlan: ThumbnailPlanResponse | null
}

export interface VisualAssetPlanSummaryResponse {
  id: string
  sourceStoryboardDraftId: string
  contentProjectId: string
  version: number
  status: VisualAssetPlanStatus
  title: string | null
  scenePlanCount: number
  hasThumbnailPlan: boolean
  providerType: string | null
  createdAt: string
  approvedAt: string | null
  rejectedAt: string | null
  editedAt: string | null
}

export interface VisualAssetPlanEditRequest {
  title?: string
  summary?: string
  overallVisualStyle?: string
  aspectRatioStrategy?: string
  culturalAuthenticityGuidance?: string
  characterContinuityGuidance?: string
  locationContinuityGuidance?: string
  colorPaletteGuidance?: string
  reviewNotes?: string
}

export interface SceneVisualPlanEditRequest {
  englishImagePrompt?: string
  turkishVisualContext?: string
  negativePrompt?: string
  styleDirective?: string
  aspectRatio?: string
  compositionNotes?: string
  cameraFraming?: string
  lightingNotes?: string
  colorPalette?: string
  mood?: string
  location?: string
  characters?: string
  culturalDetails?: string
  textOverlayGuidance?: string
  imageGenerationToolNotes?: string
  safetyNotes?: string
}

export interface ThumbnailPlanEditRequest {
  thumbnailPrompt?: string
  negativePrompt?: string
  textOverlayGuidance?: string
  compositionNotes?: string
  emotionalHook?: string
  aspectRatio?: string
  styleDirective?: string
  colorPalette?: string
}

export interface VisualAssetPlanGenerateResponse {
  success: boolean
  contentProjectId: string
  storyboardDraftId: string | null
  visualAssetPlanId: string | null
  version: number | null
  status: string | null
  planTitle: string | null
  scenePlanCount: number | null
  hasThumbnailPlan: boolean | null
  message: string | null
  errorCode: string | null
}

export interface VisualAssetPlanRejectRequest {
  reason: string
}

// ── FP-012 Admin Review Workflow ────────────────────────────

/**
 * Safe review target types.
 * No publish/export/CDN/public media targets.
 */
export type ReviewTargetType =
  | 'CONTENT_PROJECT'
  | 'SCENARIO'
  | 'STORYBOARD'
  | 'PACKAGE'
  | 'IMAGE_ASSET'

/**
 * Safe review states.
 * No READY_FOR_PUBLISH / COMPLETED / PUBLISHED / EXPORTED.
 */
export type ReviewState =
  | 'DRAFT'
  | 'NEEDS_REVIEW'
  | 'IN_REVIEW'
  | 'CHANGES_REQUESTED'
  | 'APPROVED_FOR_INTERNAL_USE'
  | 'REJECTED'

/**
 * Explicit review action types — no generic status update.
 * No PUBLISH / EXPORT / MARK_COMPLETED / AUTO_APPROVE.
 */
export type ReviewActionType =
  | 'SUBMIT_FOR_REVIEW'
  | 'START_REVIEW'
  | 'REQUEST_CHANGES'
  | 'APPROVE_FOR_INTERNAL_USE'
  | 'REJECT'
  | 'RETURN_TO_DRAFT'
  | 'ADD_REVIEW_NOTE'

export type ReviewActorRole =
  | 'CREATOR'
  | 'REVIEWER'
  | 'ADMIN'
  | 'SYSTEM'

export type ReviewAuditEventType =
  | 'REVIEW_SUBMITTED'
  | 'REVIEW_STARTED'
  | 'CHANGES_REQUESTED'
  | 'INTERNAL_APPROVAL_GRANTED'
  | 'REVIEW_REJECTED'
  | 'RETURNED_TO_DRAFT'
  | 'REVIEW_NOTE_ADDED'

/**
 * Safe admin review action request.
 * No provider URL, storage key, raw payload, base64, secret, public/signed URL.
 */
export interface AdminReviewActionRequest {
  targetType: ReviewTargetType
  targetId: string
  actionType: ReviewActionType
  actorRole: ReviewActorRole
  actorReference: string
  reasonCode?: string
  note?: string
}

/**
 * Safe admin review action response.
 * No provider URL, storage key, raw payload, base64, secret, public/signed URL,
 * publish/export semantics.
 */
export interface AdminReviewActionResponse {
  targetType: ReviewTargetType
  targetId: string
  previousState: ReviewState
  currentState: ReviewState
  actionType: ReviewActionType
  actorRole: ReviewActorRole
  auditEventType: ReviewAuditEventType
  changed: boolean
  safeMessage: string | null
  safeReasonCode: string | null
  occurredAt: string
}

/**
 * Safe admin review state summary.
 * No raw notes, no provider/storage/publish/export fields.
 */
export interface AdminReviewStateSummary {
  targetType: ReviewTargetType
  targetId: string
  currentState: ReviewState
  currentReviewerActorReference: string | null
  lastActionType: ReviewActionType | null
  lastActionAt: string | null
  lastActionByActorReference: string | null
  safeMessage: string | null
  safeReasonCode: string | null
  createdAt: string | null
  updatedAt: string | null
}

/**
 * Safe admin review audit timeline item.
 * Sanitized note only. No raw payload/URL/key/secret.
 */
export interface AdminReviewAuditTimelineItem {
  eventType: ReviewAuditEventType
  fromState: ReviewState
  toState: ReviewState
  actionType: ReviewActionType
  actorRole: ReviewActorRole
  actorReference: string
  occurredAt: string
  safeReasonCode: string | null
  safeMessage: string | null
  sanitizedNote: string | null
}

/**
 * FP-012 Task 10: Safe admin review worklist item.
 * No provider URL/storage key/raw payload/secret/public URL.
 */
export interface AdminReviewWorklistItem {
  targetType: ReviewTargetType
  targetId: string
  currentState: ReviewState
  currentReviewerActorReference: string | null
  lastActionType: ReviewActionType | null
  lastActionAt: string | null
  lastActionByActorReference: string | null
  safeReasonCode: string | null
  safeMessage: string | null
  displayTitle: string | null
  createdAt: string | null
  updatedAt: string | null
}

/**
 * FP-012 Task 10: Safe admin review target context.
 * No provider URL/storage key/raw payload/secret/public URL.
 */
export interface AdminReviewTargetContext {
  targetType: ReviewTargetType
  targetId: string
  currentState: ReviewState
  displayTitle: string | null
  displaySubtitle: string | null
  safePreviewAvailable: boolean
  safePreviewLabel: string | null
  createdAt: string | null
  updatedAt: string | null
}

/**
 * FP-012 Task 11: Safe single action availability.
 * No publish/export/provider/storage fields.
 */
export interface AdminReviewAvailableAction {
  actionType: ReviewActionType
  enabled: boolean
  disabledReasonCode: string | null
  safeLabel: string | null
  safeDescription: string | null
}

/**
 * FP-012 Task 11: Safe action availability container.
 * No publish/export/provider/storage fields.
 */
export interface AdminReviewActionAvailability {
  targetType: ReviewTargetType
  targetId: string
  currentState: ReviewState
  actorRole: ReviewActorRole
  availableActions: AdminReviewAvailableAction[]
  safeMessage: string | null
}

// ── FP-013 Task 04: Handoff & Preview Types ─────────────────────

/**
 * Safe review handoff sources.
 * No PUBLISH/EXPORT/PUBLIC_DELIVERY/CDN/SIGNED_URL values.
 */
export type ReviewHandoffSource =
  | 'CONTENT_PROJECT_GENERATION'
  | 'SCENARIO_GENERATION'
  | 'STORYBOARD_GENERATION'
  | 'PACKAGE_GENERATION'
  | 'IMAGE_GENERATION'
  | 'STORAGE_HANDOFF'

/**
 * Safe admin review handoff request.
 * No provider URL, storage key, raw payload, base64, secret, public/signed URL.
 */
export interface AdminReviewHandoffRequest {
  targetType: ReviewTargetType
  targetId: string
  source: ReviewHandoffSource
  actorRole: ReviewActorRole
  actorReference: string
  safeTitle?: string
  safeSummary?: string
  safeReasonCode?: string
  safeMessage?: string
}

/**
 * Safe admin review handoff response.
 * No provider/storage/publish/export fields.
 */
export interface AdminReviewHandoffResponse {
  targetType: ReviewTargetType
  targetId: string
  previousState: ReviewState | null
  currentState: ReviewState
  created: boolean
  updated: boolean
  preserved: boolean
  auditEventType: ReviewAuditEventType | null
  safeReasonCode: string | null
  safeMessage: string | null
}

/**
 * Safe admin preview modes.
 * No PUBLIC_URL, SIGNED_URL, CDN, RAW_PAYLOAD, BASE64 values.
 */
export type AdminPreviewMode =
  | 'TEXT_SUMMARY'
  | 'IMAGE_ASSET'
  | 'PACKAGE_SUMMARY'
  | 'STORYBOARD_SUMMARY'
  | 'SCENARIO_SUMMARY'
  | 'UNAVAILABLE'

/**
 * Safe admin preview availability states.
 * No provider/storage internal detail.
 */
export type AdminPreviewAvailability =
  | 'AVAILABLE'
  | 'UNAVAILABLE'
  | 'NOT_REVIEWABLE'
  | 'MISSING_TARGET'
  | 'UNSAFE_TARGET'
  | 'STORAGE_UNAVAILABLE'
  | 'PROVIDER_UNAVAILABLE'

/**
 * Safe admin preview DTO.
 * No provider URL, storage key, raw payload, base64, secret,
 * public/signed URL, rawImageBytes, rawContent.
 */
export interface SafeAdminPreview {
  targetType: ReviewTargetType
  targetId: string
  previewMode: AdminPreviewMode
  availability: AdminPreviewAvailability
  previewAvailable: boolean
  safeTitle: string | null
  safeSummary: string | null
  safeMessage: string | null
  safeReasonCode: string | null
  safePreviewLabel: string | null
  safeThumbnailAvailable: boolean
  currentReviewState: ReviewState | null
  updatedAt: string | null
}

// ── FP-014 Task 04: Internal Approval Readiness ─────────────

/**
 * Safe internal-only next action hints for approved review targets.
 * No PUBLISH, EXPORT, PUBLIC_DELIVERY, CREATE_PUBLIC_URL, CREATE_SIGNED_URL,
 * CDN_DELIVERY, USER_MEDIA_LIBRARY, READY_FOR_PUBLISH, PUBLISHED, EXPORTED, COMPLETED.
 */
export type SafeInternalAction =
  | 'VIEW_SAFE_PREVIEW'
  | 'REVIEW_APPROVAL_DETAILS'
  | 'REQUEST_INTERNAL_REVISION'
  | 'PREPARE_FOR_FUTURE_EXPORT'
  | 'MARK_AS_INTERNAL_READY_CANDIDATE'

/**
 * Safe availability states for internal approval readiness.
 * No PUBLISHABLE, EXPORTABLE, PUBLICLY_AVAILABLE, CDN_READY, SIGNED_URL_READY.
 */
export type InternalApprovalReadinessAvailability =
  | 'READY'
  | 'NOT_APPROVED'
  | 'REJECTED'
  | 'MISSING_TARGET'
  | 'UNSAFE_TARGET'
  | 'PREVIEW_UNAVAILABLE'
  | 'UNAVAILABLE'

/**
 * Safe immutable DTO for internal approval readiness.
 * No provider URL, storage key, bucket, raw payload, base64, secret,
 * public/signed URL, publish/export, bytes, or stack trace fields.
 */
export interface SafeInternalApprovalReadiness {
  targetType: ReviewTargetType
  targetId: string
  reviewState: ReviewState | null
  availability: InternalApprovalReadinessAvailability
  safeTitle: string | null
  safeSummary: string | null
  safeReasonCode: string | null
  safeMessage: string | null
  safePreviewAvailable: boolean
  approvedAt: string | null
  approvedByRole: string | null
  safeNextInternalActions: SafeInternalAction[]
  safeWarnings: string[]
  updatedAt: string | null
}

// ── FP-015 Task 04: Internal Revision Request Types ────────────

/**
 * Request DTO for POST /{targetType}/{targetId}/request-internal-revision.
 * targetType and targetId provided via path — NOT in the body.
 * Safe fields only — no forbidden exposure.
 */
export interface AdminInternalRevisionRequest {
  actorRole: ReviewActorRole
  actorReference: string
  safeReasonCode?: string
  safeMessage?: string
}

/**
 * Response DTO for internal revision request.
 * Safe fields only — no forbidden exposure.
 */
export interface AdminInternalRevisionResponse {
  targetType: ReviewTargetType
  targetId: string
  previousState: ReviewState
  currentState: ReviewState
  actionApplied: string
  safeReasonCode: string | null
  safeMessage: string | null
  appendedNote: boolean
  auditRecorded: boolean
  updatedAt: string | null
}

// ── FP-016 Task 04: Pipeline Aggregation Types ─────────────────

/**
 * Safe internal-only pipeline stages.
 * No forbidden exposure.
 */
export type SafePipelineStage =
  | 'GENERATED'
  | 'REVIEW_PENDING'
  | 'IN_REVIEW'
  | 'CHANGES_REQUESTED'
  | 'INTERNALLY_APPROVED'
  | 'INTERNAL_READY'
  | 'INTERNAL_REVISION_REQUESTED'
  | 'REJECTED'
  | 'UNAVAILABLE'

/**
 * Safe pipeline aggregate item DTO.
 * Read-only derived view per target.
 * No forbidden exposure.
 */
export interface SafePipelineAggregateItem {
  targetType: ReviewTargetType
  targetId: string
  pipelineStage: SafePipelineStage
  reviewState: ReviewState | null
  previewAvailable: boolean
  readinessAvailability: InternalApprovalReadinessAvailability | null
  revisionRequested: boolean
  safeTitle: string | null
  safeSummary: string | null
  safeBlockingReasons: string[]
  safeNextInternalHints: string[]
  updatedAt: string | null
}

/**
 * Safe pipeline overview wrapper DTO.
 * Aggregate counts and item list.
 * No forbidden exposure.
 */
export interface SafePipelineOverview {
  items: SafePipelineAggregateItem[]
  totalCount: number
  countsByStage: Record<string, number>
  countsByTargetType: Record<string, number>
  safeWarnings: string[]
  updatedAt: string | null
}

// ── FP-017 Task 04: Workflow Observability ──────────────────────

/**
 * Safe internal-only workflow diagnostic types.
 * No forbidden exposure.
 */
export type SafeWorkflowDiagnosticType =
  | 'STALE_REVIEW'
  | 'PREVIEW_UNAVAILABLE'
  | 'READINESS_UNAVAILABLE'
  | 'PIPELINE_UNAVAILABLE'
  | 'REVISION_REQUESTED'
  | 'MISSING_TARGET'
  | 'BLOCKED_INTERNAL_WORKFLOW'
  | 'DEGRADED_VISIBILITY'

/**
 * Safe non-mutating operational control hints.
 * These are informational labels — no execution.
 * No forbidden exposure.
 */
export type SafeOperationalControlHint =
  | 'REFRESH_OVERVIEW'
  | 'RECHECK_READINESS'
  | 'VIEW_SAFE_PREVIEW'
  | 'REVIEW_PIPELINE_DETAIL'
  | 'REQUEST_MANUAL_INVESTIGATION'

/**
 * Safe workflow diagnostic item DTO.
 * Read-only derived view.
 * No forbidden exposure.
 */
export interface SafeWorkflowDiagnosticItem {
  diagnosticType: SafeWorkflowDiagnosticType
  targetType: ReviewTargetType
  targetId: string
  pipelineStage: SafePipelineStage | null
  reviewState: ReviewState | null
  safeReasonCode: string
  safeMessage: string
  safeRecommendedHints: SafeOperationalControlHint[]
  updatedAt: string | null
}

/**
 * Safe workflow observability summary DTO.
 * Aggregate counts and diagnostics.
 * No forbidden exposure.
 */
export interface SafeWorkflowObservabilitySummary {
  countsByReviewState: Record<string, number>
  countsByPipelineStage: Record<string, number>
  countsByReadinessAvailability: Record<string, number>
  previewAvailableCount: number
  previewUnavailableCount: number
  revisionRequestedCount: number
  unavailableCount: number
  diagnostics: SafeWorkflowDiagnosticItem[]
  safeWarnings: string[]
  updatedAt: string | null
}

// ── FP-019: Türkü Video Project Types ───────────────────────

/**
 * FP-019 Task 05: Türkü video project workflow stages.
 * No forbidden states (PUBLISHED, EXPORTED, etc.)
 */
export type TurkuVideoWorkflowStage =
  | 'DRAFT_CREATED'
  | 'STORY_CONTEXT_PREPARED'
  | 'SCENARIO_PREPARED'
  | 'STORYBOARD_PREPARED'
  | 'SCENE_ASSET_PLAN_PREPARED'
  | 'IMAGE_BATCH_PREPARED'
  | 'IMAGE_BATCH_COMPLETED'
  | 'IMAGE_BATCH_PARTIAL'
  | 'COMPOSITION_PLAN_PREPARED'
  | 'RENDER_MANIFEST_PREPARED'
  | 'INTERNAL_PREVIEW_READY'
  | 'NEEDS_REVISION'
  | 'BLOCKED'
  | 'ARCHIVED'

export type TurkuSourceConfidence = 'HIGH' | 'MEDIUM' | 'LOW' | 'UNKNOWN'

export type TurkuImageBatchStatus = 'PREPARED' | 'PARTIAL' | 'COMPLETED' | 'BLOCKED' | 'FAILED'

/** Bootstrap request */
export interface TurkuVideoBootstrapRequest {
  turkuName: string
  region?: string
  sourceNotes?: string
  mood?: string
  targetDurationMinutes?: number
  narrationStyle?: string
  visualStyle?: string
}

/** Bootstrap response */
export interface TurkuVideoBootstrapResponse {
  id: string
  turkuName: string
  workflowStage: TurkuVideoWorkflowStage
  sourceConfidence: TurkuSourceConfidence
  needsHumanReview: boolean
  copyrightReviewRequired: boolean
  safeMessage: string
}

/** Full project response */
export interface TurkuVideoProjectResponse {
  id: string
  turkuName: string
  region: string | null
  sourceNotes: string | null
  mood: string | null
  targetDurationMinutes: number | null
  narrationStyle: string | null
  visualStyle: string | null
  workflowStage: TurkuVideoWorkflowStage
  sourceConfidence: TurkuSourceConfidence
  needsHumanReview: boolean
  copyrightReviewRequired: boolean
  createdByActorReference: string | null
  createdAt: string
  updatedAt: string
}

/** Progress response */
export interface TurkuVideoProgressResponse {
  id: string
  currentStage: TurkuVideoWorkflowStage
  completedStages: TurkuVideoWorkflowStage[]
  nextAvailableAction: string
  safeProgressSummary: string
}

/** Narrative context */
export interface TurkuNarrativeContext {
  turkuName: string
  region: string | null
  sourceNotes: string | null
  mood: string | null
}

/** Story package */
export interface TurkuStoryPackage {
  turkuName: string
  regionContext: string
  originSummary: string
  emotionalTheme: string
  culturalSignificance: string
  narrativeAngle: string | null
  storySummary: string
  sourceConfidence: TurkuSourceConfidence
  needsHumanReview: boolean
  copyrightReviewRequired: boolean
  reviewFlags: string[]
}

/** Scenario plan */
export interface TurkuScenarioPlan {
  title: string
  logline: string
  narrationTone: string
  targetDurationHintSeconds: number
  sceneCountHint: number
  scenarioBeats: string[]
  reviewFlags: string[]
}

/** Storyboard scene */
export interface TurkuStoryboardScene {
  sceneNo: number
  sceneTitle: string
  visualDescription: string
  narrationSummary: string
  mood: string
  estimatedDurationSeconds: number
  cameraDirection: string
  transitionHint: string | null
  musicNote: string | null
}

/** Storyboard plan */
export interface TurkuStoryboardPlan {
  scenes: TurkuStoryboardScene[]
  totalEstimatedDurationSeconds: number
  reviewFlags: string[]
}

/** Scene asset item */
export interface TurkuSceneAssetItem {
  sceneNo: number
  assetPurpose: string
  visualPromptSummary: string
  negativePromptSafetyNotes: string
  styleHint: string
  mood: string
  aspectRatioHint: string
  needsHumanReview: boolean
  copyrightReviewRequired: boolean
}

/** Scene asset plan */
export interface TurkuSceneAssetPlan {
  planId: string
  projectId: string
  items: TurkuSceneAssetItem[]
  reviewFlags: string[]
}

/** Image request */
export interface TurkuSceneImageRequest {
  sceneNo: number
  safePromptSummary: string
  safeNegativeNotes: string
  styleHint: string
  aspectRatioHint: string
}

/** Image result — safe opaque internal ref only */
export interface TurkuSceneImageResult {
  sceneNo: number
  safeInternalRef: string
  success: boolean
  safeErrorCode: string | null
}

/** Image batch */
export interface TurkuSceneImageBatch {
  batchId: string
  projectId: string
  status: TurkuImageBatchStatus
  imageRequests: TurkuSceneImageRequest[]
  imageResults: TurkuSceneImageResult[]
  reviewFlags: string[]
}

/** Preparation response (story/scenario/storyboard) */
export interface TurkuPreparationResponse {
  projectId: string
  newStage: TurkuVideoWorkflowStage
  safeMessage: string
  sourceConfidence: TurkuSourceConfidence
  needsHumanReview: boolean
  copyrightReviewRequired: boolean
  reviewFlags: string[]
}

/** Asset preparation response */
export interface TurkuAssetPreparationResponse {
  projectId: string
  newStage: TurkuVideoWorkflowStage
  safeMessage: string
  assetCount: number
  needsHumanReview: boolean
  copyrightReviewRequired: boolean
  reviewFlags: string[]
}

/** Image batch response */
export interface TurkuImageBatchResponse {
  projectId: string
  batchId: string
  batchStatus: TurkuImageBatchStatus
  newStage: TurkuVideoWorkflowStage
  safeMessage: string
  requestCount: number
  resultCount: number
  needsHumanReview: boolean
  copyrightReviewRequired: boolean
  reviewFlags: string[]
}

// ── FP-020: Provider Mode & Generation Safety Types ─────────

export type TurkuProviderMode =
  | 'DETERMINISTIC_STUB'
  | 'PROVIDER_DRAFT'
  | 'PROVIDER_DISABLED'
  | 'PROVIDER_FAILED_SAFE'
  | 'HUMAN_REVIEW_REQUIRED'

export type TurkuProviderFailureCode =
  | 'PROVIDER_DISABLED'
  | 'PROVIDER_FAILED_SAFE'
  | 'HUMAN_REVIEW_PENDING'
  | 'PROMPT_INJECTION_DETECTED'
  | 'UNSAFE_INPUT'
  | 'POLICY_VIOLATION'
  | 'SANITIZATION_FAILED'

export interface TurkuProviderTraceReference {
  traceId: string
  providerAlias: string
  generationStage: string
  safeTimestamp: string
}

export interface TurkuGenerationSafetyResponse {
  projectId: string
  providerMode: TurkuProviderMode
  currentStage: TurkuVideoWorkflowStage
  generationAvailable: boolean
  blockedReasonSummary: string | null
  humanReviewRequired: boolean
  autoApproved: boolean
  copyrightReviewRequired: boolean
  lyricsIncluded: boolean
  canGenerateStory: boolean
  canGenerateScenario: boolean
  canGenerateStoryboard: boolean
  canGeneratePlanningBundle: boolean
  canRunImageBatch: boolean
  safeFailureCode: string | null
  lastTraceRef: TurkuProviderTraceReference | null
  reviewFlags: string[]
  safeWarnings: string[]
}

export interface TurkuProviderGenerationRequest {
  narrativeAngleHint?: string
  additionalContextHint?: string
}

export interface TurkuProviderPlanningGenerationResponse {
  projectId: string
  providerMode: TurkuProviderMode
  currentStage: TurkuVideoWorkflowStage
  safeOutputSummary: string
  safeFailureCode: string | null
  traceRef: TurkuProviderTraceReference | null
  humanReviewRequired: boolean
  autoApproved: boolean
  copyrightReviewRequired: boolean
  reviewFlags: string[]
  safeWarnings: string[]
}

export interface TurkuPlanningBundleGenerationResponse {
  projectId: string
  providerMode: TurkuProviderMode
  currentStage: TurkuVideoWorkflowStage
  safeOutputSummary: string
  safeFailureCode: string | null
  storyGenerated: boolean
  scenarioGenerated: boolean
  storyboardGenerated: boolean
  humanReviewRequired: boolean
  autoApproved: boolean
  copyrightReviewRequired: boolean
  reviewFlags: string[]
  safeWarnings: string[]
}

export interface TurkuImageBatchRunRequest {
  styleOverrideHint?: string
  aspectRatioHint?: string
}

export interface TurkuImageBatchRunSceneStatus {
  sceneNo: number
  success: boolean
  safeInternalRef: string
  safeErrorCode: string | null
  needsHumanReview: boolean
}

export interface TurkuImageBatchRunResponse {
  projectId: string
  batchId: string
  providerMode: TurkuProviderMode
  batchStatus: TurkuImageBatchStatus
  currentStage: TurkuVideoWorkflowStage
  safeOutputSummary: string
  safeFailureCode: string | null
  traceRef: TurkuProviderTraceReference | null
  requestCount: number
  successCount: number
  failureCount: number
  humanReviewRequired: boolean
  autoApproved: boolean
  copyrightReviewRequired: boolean
  sceneStatuses: TurkuImageBatchRunSceneStatus[]
  reviewFlags: string[]
  safeWarnings: string[]
}

// ── FP-021: Composition / Render Planning Types ──

export interface TurkuCompositionPlanResponse {
  projectId: string
  compositionId: string
  currentStage: TurkuVideoWorkflowStage
  sceneCount: number
  totalDurationSeconds: number
  humanReviewRequired: boolean
  copyrightReviewRequired: boolean
  autoApproved: boolean
  reviewFlags: string[]
  summary: string
}

export interface TurkuRenderManifestResponse {
  projectId: string
  manifestId: string
  currentStage: TurkuVideoWorkflowStage
  sceneCount: number
  totalDurationSeconds: number
  targetResolution: string
  targetFps: number
  targetFormat: string
  humanReviewRequired: boolean
  copyrightReviewRequired: boolean
  autoApproved: boolean
  reviewFlags: string[]
  summary: string
}

export type TurkuRenderReadinessStatus = 'READY' | 'NEEDS_REVIEW' | 'BLOCKED'

export interface TurkuRenderReadinessIssue {
  code: string
  description: string
  severity: string
}

export interface TurkuRenderReadinessReport {
  projectId: string
  status: TurkuRenderReadinessStatus
  compositionReady: boolean
  manifestReady: boolean
  allScenesHaveImages: boolean
  allNarrationPresent: boolean
  timingPlanComplete: boolean
  copyrightReviewDone: boolean
  humanReviewRequired: boolean
  autoApproved: boolean
  readinessScore: number
  issues: TurkuRenderReadinessIssue[]
  reviewFlags: string[]
}

export interface TurkuCompositionPreviewScene {
  sceneNo: number
  sceneTitle: string
  narrationSummary: string
  imageRefLabel: string
  durationSeconds: number
  mood: string
  transitionIn: string
  transitionOut: string
  cameraDirection: string
}

export interface TurkuCompositionPreviewResponse {
  projectId: string
  currentStage: TurkuVideoWorkflowStage
  compositionReady: boolean
  manifestReady: boolean
  timelineScenes: TurkuCompositionPreviewScene[]
  totalDurationSeconds: number
  sceneCount: number
  readinessStatus: string
  readinessIssues: string[]
  humanReviewRequired: boolean
  copyrightReviewRequired: boolean
  autoApproved: boolean
  reviewFlags: string[]
  summary: string
}

// ── FP-022: Render Job + Execution Safety Domain ─────────────

export type TurkuRenderJobStatus = 'PREPARED' | 'RUNNING' | 'COMPLETED_INTERNAL' | 'FAILED_SAFE' | 'CANCELLED'

export type TurkuRenderFailureCode =
  | 'MANIFEST_INCOMPLETE'
  | 'READINESS_CHECK_FAILED'
  | 'RENDERER_UNAVAILABLE'
  | 'RENDERER_TIMEOUT'
  | 'RENDERER_INTERNAL_ERROR'
  | 'SAFETY_VIOLATION'
  | 'COPYRIGHT_REVIEW_PENDING'
  | 'HUMAN_REVIEW_REQUIRED'

export interface TurkuRenderAttempt {
  attemptId: string
  attemptNo: number
  startedAt: string
  completedAt: string | null
  status: TurkuRenderJobStatus
  failureCode: TurkuRenderFailureCode | null
  durationMs: number
  rendererAlias: string
}

export interface TurkuRenderArtifact {
  artifactId: string
  artifactRef: string
  format: string
  durationSeconds: number
  resolution: string
  sizeLabel: string
  humanReviewRequired: boolean
  autoApproved: boolean
}

export interface TurkuRenderJobResponse {
  projectId: string
  jobId: string | null
  status: TurkuRenderJobStatus | null
  currentStage: TurkuVideoWorkflowStage
  manifestRef: string | null
  attemptCount: number
  humanReviewRequired: boolean
  copyrightReviewRequired: boolean
  autoApproved: boolean
  notPublishReady: boolean
  notExportReady: boolean
  reviewFlags: string[]
  summary: string
}

export interface TurkuRenderExecutionResponse {
  projectId: string
  jobId: string | null
  attemptId: string | null
  status: TurkuRenderJobStatus | null
  currentStage: TurkuVideoWorkflowStage
  safeTraceRef: string | null
  artifactRef: string | null
  humanReviewRequired: boolean
  copyrightReviewRequired: boolean
  autoApproved: boolean
  notPublishReady: boolean
  notExportReady: boolean
  reviewFlags: string[]
  summary: string
  failureCode: TurkuRenderFailureCode | null
}

export interface TurkuRenderExecutionSafetyEnvelope {
  humanReviewRequired: boolean
  copyrightReviewRequired: boolean
  autoApproved: boolean
  lyricsIncluded: boolean
  copyrightClearanceVerified: boolean
  renderMode: string
  safetyFlags: string[]
  reviewFlags: string[]
  failureCodes: TurkuRenderFailureCode[]
  blockedReasons: string[]
}

export interface TurkuRenderExecutionSafetyResponse {
  projectId: string
  jobId: string | null
  jobStatus: TurkuRenderJobStatus | null
  currentStage: TurkuVideoWorkflowStage
  safetyEnvelope: TurkuRenderExecutionSafetyEnvelope
  summary: string
}

// ── FP-023: Render Adapter + Artifact Metadata Domain ────────

export type TurkuRenderAdapterMode = 'STUB' | 'FFMPEG_DISABLED' | 'FFMPEG_CONFIG_MISSING' | 'FFMPEG_READY' | 'FFMPEG_FAILED_SAFE'

export type TurkuRenderAdapterFailureCode =
  | 'RENDER_PROVIDER_DISABLED'
  | 'RENDER_CONFIG_MISSING'
  | 'RENDER_INPUT_MISSING'
  | 'RENDER_TIMEOUT'
  | 'RENDER_PROCESS_FAILED'
  | 'RENDER_OUTPUT_REJECTED'
  | 'RENDER_SECURITY_VIOLATION'
  | 'RENDER_FAILED_SAFE'

export interface TurkuRenderAdapterTraceReference {
  traceRef: string
  adapterMode: TurkuRenderAdapterMode
  timestampMs: number
}

export interface TurkuRenderAdapterSafetyEnvelope {
  humanReviewRequired: boolean
  copyrightReviewRequired: boolean
  autoApproved: boolean
  notPublishReady: boolean
  notExportReady: boolean
  adapterMode: TurkuRenderAdapterMode
  safetyFlags: string[]
  reviewFlags: string[]
}

export interface TurkuFfmpegCommandPlanSummary {
  executableToken: string
  argCount: number
  validated: boolean
  validationError: string | null
  format: string | null
  resolution: string | null
  fps: number | null
}

export type TurkuRenderArtifactStatus =
  | 'CREATED'
  | 'VALIDATED'
  | 'RETENTION_ACTIVE'
  | 'CLEANUP_ELIGIBLE'
  | 'CLEANUP_PLANNED'
  | 'CLEANED'
  | 'REJECTED'

export interface TurkuRenderArtifactReference {
  artifactId: string
  opaqueRef: string
  rendererMode: TurkuRenderAdapterMode
  format: string
  durationSeconds: number
  resolution: string
  sizeLabel: string
  createdAt: string
}

export interface TurkuInternalRenderArtifact {
  reference: TurkuRenderArtifactReference
  safetyReport: TurkuRenderArtifactSafetyReport
  retentionPolicy: TurkuRenderArtifactRetentionPolicy
  status: TurkuRenderArtifactStatus
}

export interface TurkuRenderArtifactRetentionPolicy {
  retentionHours: number
  autoCleanupEnabled: boolean
  policyLabel: string
}

export interface TurkuRenderArtifactCleanupPlan {
  planId: string
  artifactId: string
  opaqueRef: string
  retentionPolicy: TurkuRenderArtifactRetentionPolicy
  eligible: boolean
  reason: string
}

export interface TurkuRenderArtifactSafetyReport {
  artifactId: string
  humanReviewRequired: boolean
  copyrightReviewRequired: boolean
  autoApproved: boolean
  notPublishReady: boolean
  notExportReady: boolean
  status: TurkuRenderArtifactStatus
  rendererMode: TurkuRenderAdapterMode
  safetyFlags: string[]
  reviewFlags: string[]
}

// ── FP-024: Secure Render Config / Workspace / Execution / Verification Domain ────────

export type TurkuRenderConfigMode =
  | 'STUB_ONLY'
  | 'FFMPEG_DISABLED'
  | 'FFMPEG_CONFIG_MISSING'
  | 'FFMPEG_CONFIG_INVALID'
  | 'FFMPEG_CONFIG_VALID'
  | 'FFMPEG_FAILED_SAFE'

export type TurkuRenderConfigFailureCode =
  | 'CONFIG_MISSING'
  | 'CONFIG_DISABLED'
  | 'EXECUTABLE_EMPTY'
  | 'EXECUTABLE_UNSAFE'
  | 'WORKSPACE_EMPTY'
  | 'WORKSPACE_UNSAFE'
  | 'TIMEOUT_OUT_OF_RANGE'
  | 'MAX_DURATION_OUT_OF_RANGE'
  | 'MAX_SIZE_OUT_OF_RANGE'
  | 'RESOLUTION_INVALID'
  | 'FPS_OUT_OF_RANGE'
  | 'FORMAT_EMPTY'
  | 'CODEC_EMPTY'
  | 'TRAVERSAL_DETECTED'
  | 'SHELL_DETECTED'
  | 'URL_DETECTED'
  | 'SECRET_DETECTED'
  | 'STORAGE_KEY_DETECTED'
  | 'GENERAL_FAILURE'

export interface TurkuRenderConfigValidationIssue {
  code: TurkuRenderConfigFailureCode
  field: string
  safeMessage: string
}

export interface TurkuRenderConfigValidationResult {
  valid: boolean
  resolvedMode: TurkuRenderConfigMode
  issues: TurkuRenderConfigValidationIssue[]
}

export interface TurkuRenderConfigSafetyEnvelope {
  configMode: TurkuRenderConfigMode
  humanReviewRequired: boolean
  copyrightReviewRequired: boolean
  autoApproved: boolean
  notPublishReady: boolean
  notExportReady: boolean
  safetyFlags: string[]
}

export interface TurkuRenderWorkspaceRef {
  workspaceId: string
  opaqueRef: string
}

export interface TurkuRenderSafeFileRef {
  fileId: string
  opaqueRef: string
  extension: string
  purpose: string
}

export interface TurkuRenderFileSafetyReport {
  fileId: string
  safe: boolean
  humanReviewRequired: boolean
  copyrightReviewRequired: boolean
  autoApproved: boolean
  notPublishReady: boolean
  notExportReady: boolean
  safetyFlags: string[]
}

export type TurkuFfmpegExecutionMode =
  | 'DISABLED'
  | 'CONFIG_MISSING'
  | 'CONFIG_INVALID'
  | 'DRY_RUN'
  | 'SIMULATED_SUCCESS'
  | 'FAILED_SAFE'

export type TurkuFfmpegExecutionFailureCode =
  | 'EXECUTION_DISABLED'
  | 'CONFIG_MISSING'
  | 'CONFIG_INVALID'
  | 'WORKSPACE_INVALID'
  | 'FILE_REF_INVALID'
  | 'COMMAND_PLAN_INVALID'
  | 'COMMAND_PLAN_UNSAFE'
  | 'TIMEOUT_EXCEEDED'
  | 'LIMITS_EXCEEDED'
  | 'DRY_RUN_FAILED'
  | 'SIMULATED_FAILURE'
  | 'SAFETY_VIOLATION'
  | 'GENERAL_FAILURE'

export interface TurkuFfmpegExecutionResult {
  success: boolean
  executionMode: TurkuFfmpegExecutionMode
  failureCode: TurkuFfmpegExecutionFailureCode | null
  safeTraceRef: string
  opaqueArtifactRef: string | null
  safeMessage: string
  durationMs: number
  safetyEnvelope: TurkuFfmpegExecutionSafetyEnvelope
}

export interface TurkuFfmpegExecutionSafetyEnvelope {
  executionMode: TurkuFfmpegExecutionMode
  humanReviewRequired: boolean
  copyrightReviewRequired: boolean
  autoApproved: boolean
  notPublishReady: boolean
  notExportReady: boolean
  safetyFlags: string[]
  validationNotes: string[]
}

export type TurkuRenderArtifactVerificationStatus =
  | 'PENDING'
  | 'VERIFIED'
  | 'FORMAT_REJECTED'
  | 'BOUNDS_EXCEEDED'
  | 'REF_UNSAFE'
  | 'METADATA_INCOMPLETE'
  | 'FAILED_SAFE'

export interface TurkuRenderArtifactVerificationIssue {
  code: string
  field: string
  safeMessage: string
}

export interface TurkuRenderArtifactVerificationReport {
  artifactRef: string | null
  status: TurkuRenderArtifactVerificationStatus
  rendererMode: string
  expectedFormat: string | null
  safeTraceRef: string
  issues: TurkuRenderArtifactVerificationIssue[]
  humanReviewRequired: boolean
  copyrightReviewRequired: boolean
  autoApproved: boolean
  notPublishReady: boolean
  notExportReady: boolean
  safetyFlags: string[]
}

// ── FP-025: Internal Render Job Lifecycle & Progress Domain ────────

export type TurkuInternalRenderJobStatus =
  | 'DRAFT'
  | 'READY_FOR_INTERNAL_RENDER'
  | 'QUEUED_INTERNAL'
  | 'RUNNING_INTERNAL'
  | 'SUCCEEDED_INTERNAL'
  | 'FAILED_INTERNAL'
  | 'CANCELLED_INTERNAL'
  | 'EXPIRED_INTERNAL'

export type TurkuInternalRenderJobAction =
  | 'VALIDATE_FOR_INTERNAL_RENDER'
  | 'ENQUEUE_INTERNAL_RENDER'
  | 'START_INTERNAL_RENDER'
  | 'COMPLETE_INTERNAL_RENDER'
  | 'FAIL_INTERNAL_RENDER'
  | 'CANCEL_INTERNAL_RENDER'
  | 'EXPIRE_INTERNAL_RENDER'

export type TurkuInternalRenderJobReasonCode =
  | 'TRANSITION_ALLOWED'
  | 'UNKNOWN_CURRENT_STATE'
  | 'UNKNOWN_ACTION'
  | 'TRANSITION_NOT_ALLOWED'
  | 'TERMINAL_STATE'
  | 'PUBLIC_DELIVERY_FORBIDDEN'
  | 'GENERIC_STATUS_UPDATE_FORBIDDEN'
  | 'JOB_REF_INVALID'
  | 'RENDER_EXECUTION_DISABLED'
  | 'UNSAFE_INPUT_REJECTED'
  | 'ARTIFACT_REF_INVALID'
  | 'PROGRESS_OUT_OF_BOUNDS'
  | 'PROVIDER_PAYLOAD_FORBIDDEN'
  | 'FILESYSTEM_EXPOSURE_FORBIDDEN'

export interface TurkuInternalRenderJobProgress {
  jobRef: string
  status: TurkuInternalRenderJobStatus
  progressPercent: number
  currentStageLabel: string
  humanReviewRequired: boolean
  autoApproved: boolean
  safetyFlags: string[]
}

export interface TurkuInternalRenderJobSummary {
  jobRef: string
  status: TurkuInternalRenderJobStatus
  progressPercent: number
  currentStageLabel: string
  safeTraceRef: string
  humanReviewRequired: boolean
  copyrightReviewRequired: boolean
  autoApproved: boolean
  notPublishReady: boolean
  notExportReady: boolean
  safetyFlags: string[]
}

export type TurkuInternalRenderReadinessStatus =
  | 'NOT_READY'
  | 'READY_INTERNAL_ONLY'
  | 'BLOCKED_BY_CONFIG'
  | 'BLOCKED_BY_WORKSPACE'
  | 'BLOCKED_BY_EXECUTION_BOUNDARY'
  | 'BLOCKED_BY_ARTIFACT_VERIFICATION'
  | 'BLOCKED_BY_HUMAN_REVIEW'

export type TurkuInternalRenderReadinessIssue =
  | 'CONFIG_DISABLED'
  | 'CONFIG_INVALID'
  | 'WORKSPACE_REF_MISSING'
  | 'WORKSPACE_REF_INVALID'
  | 'EXECUTION_BOUNDARY_DISABLED'
  | 'EXECUTION_PLAN_UNSAFE'
  | 'ARTIFACT_REF_MISSING'
  | 'ARTIFACT_REF_INVALID'
  | 'ARTIFACT_VERIFICATION_FAILED'
  | 'HUMAN_REVIEW_REQUIRED'
  | 'PUBLIC_DELIVERY_FORBIDDEN'
  | 'AUTO_APPROVAL_FORBIDDEN'
  | 'UNSAFE_INPUT_REJECTED'

export interface TurkuInternalRenderReadinessSnapshot {
  status: TurkuInternalRenderReadinessStatus
  issues: TurkuInternalRenderReadinessIssue[]
  humanReviewRequired: boolean
  autoApproved: boolean
  publicDeliveryAvailable: boolean
  safetyFlags: string[]
}

export interface TurkuInternalRenderSafetyChainSnapshot {
  configValid: boolean
  workspaceSafe: boolean
  executionBoundarySafe: boolean
  artifactVerified: boolean
  configModeLabel: string
  executionModeLabel: string
  verificationStatusLabel: string
  humanReviewRequired: boolean
  copyrightReviewRequired: boolean
  autoApproved: boolean
  publicDeliveryAvailable: boolean
  safetyFlags: string[]
}

export interface TurkuInternalRenderJobActionResult {
  allowed: boolean
  summary: TurkuInternalRenderJobSummary | null
  action: TurkuInternalRenderJobAction
  reasonCode: TurkuInternalRenderJobReasonCode
}

// ── FP-026: Human Review Gate Domain ──────────────────────────────

export type TurkuInternalRenderReviewState =
  | 'REVIEW_NOT_REQUIRED'
  | 'REVIEW_REQUIRED'
  | 'READY_FOR_REVIEW'
  | 'IN_REVIEW'
  | 'APPROVED_FOR_INTERNAL_NEXT_STEP'
  | 'REJECTED_BY_HUMAN_REVIEW'
  | 'BLOCKED_BY_SAFETY'
  | 'EXPIRED_REVIEW'

export type TurkuInternalRenderReviewAction =
  | 'REQUIRE_HUMAN_REVIEW'
  | 'MARK_READY_FOR_REVIEW'
  | 'START_HUMAN_REVIEW'
  | 'APPROVE_FOR_INTERNAL_NEXT_STEP'
  | 'REJECT_HUMAN_REVIEW'
  | 'BLOCK_FOR_SAFETY'
  | 'EXPIRE_HUMAN_REVIEW'

export type TurkuInternalRenderReviewReasonCode =
  | 'REVIEW_NOT_REQUIRED'
  | 'REVIEW_REQUIRED'
  | 'READY_FOR_REVIEW'
  | 'REVIEW_STARTED'
  | 'APPROVED_INTERNAL_ONLY'
  | 'REJECTED_BY_REVIEWER'
  | 'BLOCKED_BY_SAFETY'
  | 'REVIEW_EXPIRED'
  | 'JOB_NOT_READY'
  | 'READINESS_BLOCKED'
  | 'ARTIFACT_VERIFICATION_FAILED'
  | 'PUBLIC_DELIVERY_FORBIDDEN'
  | 'AUTO_APPROVAL_FORBIDDEN'
  | 'GENERIC_STATUS_UPDATE_FORBIDDEN'
  | 'UNSAFE_REVIEW_INPUT_REJECTED'
  | 'TRANSITION_NOT_ALLOWED'
  | 'TERMINAL_STATE'
  | 'REVIEW_REF_INVALID'

export type TurkuInternalRenderReviewReadinessStatus =
  | 'NOT_READY_FOR_REVIEW'
  | 'READY_FOR_HUMAN_REVIEW'
  | 'IN_REVIEW'
  | 'APPROVED_INTERNAL_ONLY'
  | 'REJECTED'
  | 'BLOCKED'
  | 'EXPIRED'

export type TurkuInternalRenderReviewReadinessIssue =
  | 'JOB_NOT_READY'
  | 'READINESS_BLOCKED'
  | 'ARTIFACT_VERIFICATION_FAILED'
  | 'HUMAN_REVIEW_REQUIRED'
  | 'PUBLIC_DELIVERY_FORBIDDEN'
  | 'AUTO_APPROVAL_FORBIDDEN'
  | 'UNSAFE_INPUT_REJECTED'

export interface TurkuInternalRenderReviewSummary {
  reviewRef: string
  jobRef: string
  reviewState: TurkuInternalRenderReviewState
  reasonCode: TurkuInternalRenderReviewReasonCode
  humanReviewRequired: boolean
  autoApproved: boolean
  publicDeliveryAvailable: boolean
  internalOnly: boolean
  notPublishReady: boolean
  notExportReady: boolean
  safetyFlags: string[]
}

export interface TurkuInternalRenderReviewReadinessSnapshot {
  status: TurkuInternalRenderReviewReadinessStatus
  issues: TurkuInternalRenderReviewReadinessIssue[]
  humanReviewRequired: boolean
  autoApproved: boolean
  publicDeliveryAvailable: boolean
  internalOnly: boolean
  safetyFlags: string[]
}

export interface TurkuInternalRenderReviewTransitionResult {
  allowed: boolean
  fromState: TurkuInternalRenderReviewState | null
  action: TurkuInternalRenderReviewAction
  toState: TurkuInternalRenderReviewState | null
  reasonCode: TurkuInternalRenderReviewReasonCode
}

export interface TurkuInternalRenderReviewActionResult {
  allowed: boolean
  summary: TurkuInternalRenderReviewSummary | null
  action: TurkuInternalRenderReviewAction
  reasonCode: TurkuInternalRenderReviewReasonCode
}

// ── FP-027: Internal Render Queue / Retry / Expiration ──────────

export type TurkuInternalRenderQueueState =
  | 'NOT_QUEUED'
  | 'QUEUE_READY'
  | 'QUEUED_INTERNAL'
  | 'DISPATCHING_INTERNAL'
  | 'IN_PROGRESS_INTERNAL'
  | 'RETRY_WAIT_INTERNAL'
  | 'SUCCEEDED_INTERNAL'
  | 'FAILED_INTERNAL'
  | 'CANCELLED_INTERNAL'
  | 'EXPIRED_INTERNAL'

export type TurkuInternalRenderQueueAction =
  | 'MARK_QUEUE_READY'
  | 'ENQUEUE_INTERNAL'
  | 'DISPATCH_INTERNAL'
  | 'MARK_IN_PROGRESS'
  | 'MARK_RETRY_WAIT'
  | 'MARK_SUCCEEDED'
  | 'MARK_FAILED'
  | 'CANCEL_INTERNAL'
  | 'EXPIRE_INTERNAL'

export type TurkuInternalRenderQueueReasonCode =
  | 'NOT_QUEUED'
  | 'QUEUE_READY'
  | 'ENQUEUED'
  | 'DISPATCHING'
  | 'IN_PROGRESS'
  | 'RETRY_WAIT'
  | 'SUCCEEDED'
  | 'FAILED_PERMANENT'
  | 'CANCELLED'
  | 'EXPIRED'
  | 'TRANSITION_NOT_ALLOWED'
  | 'TERMINAL_STATE'
  | 'QUEUE_REF_INVALID'
  | 'READINESS_NOT_MET'
  | 'REVIEW_NOT_APPROVED'
  | 'MAX_RETRIES_EXHAUSTED'
  | 'RETRY_DELAY_ACTIVE'
  | 'PUBLIC_DELIVERY_FORBIDDEN'
  | 'AUTO_APPROVAL_FORBIDDEN'
  | 'GENERIC_STATUS_UPDATE_FORBIDDEN'

export type TurkuInternalRenderRetryReasonCode =
  | 'EXECUTION_FAILED_RETRIABLE'
  | 'TIMEOUT_RETRIABLE'
  | 'TRANSIENT_ERROR_RETRIABLE'
  | 'MAX_RETRIES_EXHAUSTED'
  | 'NOT_RETRIABLE_PERMANENT'
  | 'CANCELLED_NO_RETRY'
  | 'EXPIRED_NO_RETRY'

export type TurkuInternalRenderExpirationReasonCode =
  | 'QUEUE_DURATION_EXCEEDED'
  | 'RETRY_WAIT_DURATION_EXCEEDED'
  | 'STALE_INTERNAL_STATE'
  | 'ADMIN_FORCED_EXPIRATION'
  | 'NOT_EXPIRED'

export interface TurkuInternalRenderQueueSummary {
  queueItemRef: string
  jobRef: string
  reviewRef: string | null
  queueState: TurkuInternalRenderQueueState
  reasonCode: TurkuInternalRenderQueueReasonCode
  attemptNumber: number
  maxAttempts: number
  retriable: boolean
  expired: boolean
  humanReviewRequired: boolean
  autoApproved: boolean
  publicDeliveryAvailable: boolean
  internalOnly: boolean
  notPublishReady: boolean
  notExportReady: boolean
  safetyFlags: string[]
}

export interface TurkuInternalRenderQueueActionResult {
  allowed: boolean
  summary: TurkuInternalRenderQueueSummary | null
  action: TurkuInternalRenderQueueAction
  reasonCode: TurkuInternalRenderQueueReasonCode
}

export interface TurkuInternalRenderRetryPolicy {
  maxAttempts: number
  retryDelaySeconds: number
}

export interface TurkuInternalRenderRetrySnapshot {
  currentAttempt: number
  maxAttempts: number
  retryDelaySeconds: number
  retriable: boolean
  reasonCode: TurkuInternalRenderRetryReasonCode
}

export interface TurkuInternalRenderExpirationPolicy {
  maxQueueDurationSeconds: number
  maxRetryWaitDurationSeconds: number
}

export interface TurkuInternalRenderExpirationSnapshot {
  expired: boolean
  reasonCode: TurkuInternalRenderExpirationReasonCode
  maxQueueDurationSeconds: number
  maxRetryWaitDurationSeconds: number
}

// ── FP-028: Real FFmpeg Internal Dev/Test Enablement ────────────

export type TurkuFfmpegRealExecutionMode =
  | 'DISABLED'
  | 'DRY_RUN'
  | 'SIMULATED'
  | 'REAL_DEV_TEST_ONLY'

export type TurkuFfmpegExecutionEnvironment =
  | 'LOCAL'
  | 'DEV'
  | 'TEST'
  | 'STAGING'
  | 'PROD'
  | 'UNKNOWN'

export type TurkuFfmpegBinaryPolicy =
  | 'SYSTEM_FFMPEG_LOCAL'
  | 'TEST_FIXTURE_FFMPEG'
  | 'DISABLED_BINARY'

export type TurkuFfmpegBoundaryResultStatus =
  | 'NOT_EXECUTED'
  | 'DRY_RUN_COMPLETE'
  | 'SIMULATED_COMPLETE'
  | 'EXECUTED_INTERNAL'
  | 'FAILED_SAFE'
  | 'DENIED'

export type TurkuFfmpegBoundaryReasonCode =
  | 'DISABLED_BY_DEFAULT'
  | 'DRY_RUN_SAFE'
  | 'SIMULATED_SAFE'
  | 'EXECUTED_INTERNAL_SAFE'
  | 'ENVIRONMENT_DENIED'
  | 'MODE_DISABLED'
  | 'BINARY_UNAVAILABLE'
  | 'INTERNAL_FLAG_NOT_SET'
  | 'REVIEW_NOT_APPROVED'
  | 'DISPATCH_NOT_READY'
  | 'BOUNDS_EXCEEDED'
  | 'TIMEOUT_EXCEEDED'
  | 'PUBLIC_DELIVERY_FORBIDDEN'
  | 'AUTO_APPROVAL_FORBIDDEN'

export interface TurkuFfmpegExecutionBoundaryResult {
  status: TurkuFfmpegBoundaryResultStatus
  reasonCode: TurkuFfmpegBoundaryReasonCode
  mode: TurkuFfmpegRealExecutionMode
  environment: TurkuFfmpegExecutionEnvironment
  jobRef: string | null
  queueItemRef: string | null
  reviewRef: string | null
  internalOnly: boolean
  publicDeliveryAvailable: boolean
  autoApproved: boolean
  safetyFlags: string[]
}

export interface TurkuFfmpegDevTestAdapterResult {
  allowed: boolean
  gateResult: TurkuFfmpegExecutionBoundaryResult
  outputRef: string | null
  internalOnly: boolean
  publicDeliveryAvailable: boolean
  autoApproved: boolean
  safetyFlags: string[]
}

export interface TurkuFfmpegVerificationCandidate {
  outputRef: string
  jobRef: string
  format: string
  durationSeconds: number
  fps: number
  width: number
  height: number
  estimatedSizeMB: number
  boundsValid: boolean
  internalOnly: boolean
  publicDeliveryAvailable: boolean
  autoApproved: boolean
  safetyFlags: string[]
}

// ── FP-029: Internal E2E Video Smoke Path ───────────────────────

export type TurkuInternalSmokePathStatus =
  | 'NOT_STARTED'
  | 'READINESS_CHECK_PASSED'
  | 'REVIEW_CHECK_PASSED'
  | 'DISPATCH_CHECK_PASSED'
  | 'GATE_CHECK_PASSED'
  | 'PLAN_CREATED'
  | 'CANDIDATE_CREATED'
  | 'SMOKE_COMPLETE_INTERNAL'
  | 'DENIED'
  | 'FAILED_SAFE'

export type TurkuInternalSmokePathReasonCode =
  | 'NOT_STARTED'
  | 'READINESS_NOT_MET'
  | 'REVIEW_NOT_APPROVED'
  | 'DISPATCH_NOT_READY'
  | 'GATE_DENIED'
  | 'BINARY_UNAVAILABLE'
  | 'ENVIRONMENT_DENIED'
  | 'BOUNDS_INVALID'
  | 'FORMAT_NOT_ALLOWED'
  | 'PLAN_CREATED_SAFE'
  | 'CANDIDATE_CREATED_SAFE'
  | 'SMOKE_COMPLETE_SAFE'
  | 'PUBLIC_DELIVERY_FORBIDDEN'
  | 'AUTO_APPROVAL_FORBIDDEN'

export interface TurkuInternalSmokeRenderPlan {
  format: string
  durationSeconds: number
  fps: number
  width: number
  height: number
  estimatedSizeMB: number
  sceneCount: number
  assetCount: number
  boundsValid: boolean
  internalOnly: boolean
  publicDeliveryAvailable: boolean
  safetyFlags: string[]
}

export interface TurkuInternalSmokePathResult {
  status: TurkuInternalSmokePathStatus
  reasonCode: TurkuInternalSmokePathReasonCode
  outputRef: string | null
  plan: TurkuInternalSmokeRenderPlan | null
  internalOnly: boolean
  publicDeliveryAvailable: boolean
  autoApproved: boolean
  safetyFlags: string[]
}

// ── FP-029 Task 02: Internal Generation Trial ──────────────────

export type TurkuInternalTrialStatus =
  | 'NOT_ATTEMPTED'
  | 'GATE_DENIED'
  | 'PLAN_INVALID'
  | 'TRIAL_READY'
  | 'EXECUTION_SKIPPED'
  | 'EXECUTION_SIMULATED'
  | 'EXECUTION_ATTEMPTED_DEV_TEST'
  | 'VERIFICATION_CANDIDATE_CREATED'
  | 'TRIAL_FAILED_SAFE'

export type TurkuInternalTrialReasonCode =
  | 'NOT_ATTEMPTED'
  | 'READINESS_NOT_MET'
  | 'REVIEW_NOT_APPROVED'
  | 'DISPATCH_NOT_READY'
  | 'GATE_DENIED_MODE'
  | 'GATE_DENIED_ENVIRONMENT'
  | 'GATE_DENIED_FLAG'
  | 'BINARY_UNAVAILABLE'
  | 'PLAN_BOUNDS_INVALID'
  | 'FORMAT_NOT_ALLOWED'
  | 'EXECUTION_SKIPPED_SAFE'
  | 'EXECUTION_SIMULATED_SAFE'
  | 'EXECUTION_ATTEMPTED_SAFE'
  | 'CANDIDATE_CREATED_SAFE'
  | 'PUBLIC_DELIVERY_FORBIDDEN'
  | 'AUTO_APPROVAL_FORBIDDEN'

export interface TurkuInternalTrialResult {
  status: TurkuInternalTrialStatus
  reasonCode: TurkuInternalTrialReasonCode
  outputRef: string | null
  verificationCandidate: TurkuFfmpegVerificationCandidate | null
  internalOnly: boolean
  publicDeliveryAvailable: boolean
  autoApproved: boolean
  safetyFlags: string[]
}

// ── FP-030: Real FFmpeg Smoke Run ───────────────────────────────

export type TurkuFfmpegSmokeRunStatus =
  | 'NOT_ATTEMPTED'
  | 'READINESS_DENIED'
  | 'REVIEW_DENIED'
  | 'GATE_DENIED'
  | 'PROCESS_DENIED'
  | 'EXECUTION_SKIPPED_DISABLED'
  | 'EXECUTION_SKIPPED_UNAVAILABLE'
  | 'EXECUTION_FAKE_TESTED'
  | 'EXECUTION_ATTEMPTED_LOCAL'
  | 'EXECUTION_COMPLETED_SAFE'
  | 'EXECUTION_FAILED_SAFE'
  | 'VERIFICATION_CANDIDATE_CREATED'

export interface TurkuFfmpegSmokeRunResult {
  status: TurkuFfmpegSmokeRunStatus
  outputRef: string | null
  verificationCandidate: TurkuFfmpegVerificationCandidate | null
  durationMs: number
  internalOnly: boolean
  publicDeliveryAvailable: boolean
  autoApproved: boolean
  safetyFlags: string[]
}

// ── FP-042/043: Internal Render Review Package ──

export type TurkuRenderReviewPackageStatus =
  | 'CREATED_INTERNAL_ONLY'
  | 'ARTIFACT_METADATA_ATTACHED'
  | 'READY_FOR_INTERNAL_REVIEW_ONLY'
  | 'REJECTED_UNSAFE'
  | 'FAILED_SAFE'

export type TurkuRenderReviewPackageIssueCode =
  | 'NONE'
  | 'NO_PRIMARY_ARTIFACT'
  | 'ARTIFACT_NOT_VERIFIED'
  | 'ARTIFACT_REJECTED'
  | 'ARTIFACT_FAILED'
  | 'ARTIFACT_ROLE_UNKNOWN'
  | 'TOO_MANY_ARTIFACTS'
  | 'DUPLICATE_ARTIFACT_REF'
  | 'MISSING_ARTIFACT_REF'
  | 'LIFECYCLE_STATUS_INVALID'
  | 'BUNDLE_REF_MISSING'
  | 'METADATA_INCOMPLETE'

export type TurkuRenderReviewArtifactRole =
  | 'PRIMARY_VIDEO_CLIP'
  | 'POSTER_FRAME'
  | 'AUDIO_STUB'
  | 'VERIFICATION_REPORT'
  | 'INTERNAL_METADATA'
  | 'UNKNOWN'

export type TurkuRenderReviewVerificationStatus =
  | 'PASSED'
  | 'REJECTED_DURATION'
  | 'REJECTED_SIZE'
  | 'REJECTED_MULTIPLE'

export interface TurkuRenderReviewArtifactSummary {
  opaqueArtifactRef: string
  role: TurkuRenderReviewArtifactRole
  verificationStatus: TurkuRenderReviewVerificationStatus
  format: string
  durationSeconds: number
  fps: number
  width: number
  height: number
  sizeBytes: number
  verified: boolean
}

export interface TurkuRenderReviewPackage {
  opaqueReviewPackageRef: string
  opaqueJobRef: string
  opaqueBundleRef: string
  status: TurkuRenderReviewPackageStatus
  issueCodes: TurkuRenderReviewPackageIssueCode[]
  artifactSummaries: TurkuRenderReviewArtifactSummary[]
  sceneCount: number
  inputClipCount: number
  totalDurationSeconds: number
  maxWidth: number
  maxHeight: number
  maxFps: number
  totalSizeBytes: number
  internalOnly: true
}

// FP-049: Internal dry-run trigger types

export type TurkuStoryboardInternalRenderChainStatus =
  | 'CREATED_INTERNAL_ONLY'
  | 'STORYBOARD_ACCEPTED'
  | 'SCENE_PLANS_CREATED'
  | 'MULTI_SCENE_JOB_READY'
  | 'SYNTHETIC_CLIPS_GENERATED'
  | 'STITCHED_ARTIFACT_VERIFIED'
  | 'REVIEW_PACKAGE_READY_INTERNAL_ONLY'
  | 'ADMIN_METADATA_VISIBLE'
  | 'REJECTED_UNSAFE'
  | 'FAILED_SAFE'

export type TurkuStoryboardInternalRenderChainIssueCode =
  | 'NONE'
  | 'ADAPTER_FAILED'
  | 'SCENE_PLAN_FAILED'
  | 'MULTI_SCENE_JOB_FAILED'
  | 'CLIP_GENERATION_FAILED'
  | 'STITCHING_FAILED'
  | 'LIFECYCLE_VERIFICATION_FAILED'
  | 'REVIEW_PACKAGE_FAILED'
  | 'ADMIN_VISIBILITY_UNAVAILABLE'
  | 'SAFETY_VIOLATION'

export interface TurkuInternalDryRunSceneInput {
  opaqueSceneRef: string
  sceneOrder: number
  targetDurationSeconds: number
  targetFps: number
  targetWidth: number
  targetHeight: number
  targetFormat: string
}

export interface TurkuInternalDryRunTriggerRequest {
  opaqueStoryboardRef: string
  opaqueStoryRef: string
  sceneInputs: TurkuInternalDryRunSceneInput[]
}

export interface TurkuInternalDryRunTriggerResponse {
  opaqueChainRef: string
  opaqueStoryboardRef: string
  status: TurkuStoryboardInternalRenderChainStatus
  issueCodes: TurkuStoryboardInternalRenderChainIssueCode[]
  sceneCount: number
  generatedClipCount: number
  stitchedArtifactPresent: boolean
  reviewPackageStatus: string | null
  adminMetadataVisible: boolean
  totalDurationSeconds: number
  maxWidth: number
  maxHeight: number
  maxFps: number
  totalSizeBytes: number
  internalOnly: true
}

// ── FP-051: Dry Run Record & Audit-Safe History ──

export type TurkuInternalDryRunRecordStatus =
  | 'CREATED_INTERNAL_ONLY'
  | 'TRIGGER_ACCEPTED'
  | 'DRY_RUN_STARTED'
  | 'METADATA_CAPTURED'
  | 'READY_FOR_ADMIN_HISTORY_ONLY'
  | 'REJECTED_UNSAFE'
  | 'FAILED_SAFE'

export type TurkuInternalDryRunRecordIssueCode =
  | 'TRIGGER_REJECTED'
  | 'DRY_RUN_CHAIN_FAILED'
  | 'METADATA_CAPTURE_FAILED'
  | 'INVALID_FIXTURE_INPUT'
  | 'SCENE_COUNT_MISMATCH'
  | 'CLIP_COUNT_MISMATCH'
  | 'STITCH_NOT_PRESENT'
  | 'REVIEW_PACKAGE_MISSING'
  | 'ADMIN_VISIBILITY_BLOCKED'
  | 'INTERNAL_ERROR'

export interface TurkuInternalDryRunRecordSummary {
  opaqueRecordRef: string
  opaqueDryRunRef: string
  opaqueStoryboardRef: string
  status: TurkuInternalDryRunRecordStatus
  issueCodes: TurkuInternalDryRunRecordIssueCode[]
  sceneCount: number
  generatedClipCount: number
  inputClipCount: number
  stitchedArtifactPresent: boolean
  durationSeconds: number
  fps: number
  width: number
  height: number
  sizeBytes: number
  internalOnly: true
}

// ── FP-053: Dry Run Operational Summary ──

export type TurkuInternalDryRunOperationalSummaryStatus =
  | 'NO_RECORDS'
  | 'ALL_ACCEPTED'
  | 'MIXED'
  | 'ALL_FAILED'
  | 'ALL_REJECTED'

export type TurkuInternalDryRunOperationalSummaryIssueCode =
  | 'NO_RECORDS_AVAILABLE'
  | 'HIGH_FAILURE_RATE'
  | 'HIGH_REJECTION_RATE'
  | 'STITCH_SUCCESS_LOW'
  | 'AGGREGATE_LIMIT_EXCEEDED'

export interface TurkuInternalDryRunOperationalSummary {
  totalRecordCount: number
  acceptedCount: number
  failedCount: number
  rejectedCount: number
  readyForAdminHistoryOnlyCount: number
  overallStatus: TurkuInternalDryRunOperationalSummaryStatus
  issueCodes: TurkuInternalDryRunOperationalSummaryIssueCode[]
  totalSceneCount: number
  totalClipCount: number
  stitchedCount: number
  internalOnly: true
}

// ── PD-004: Internal Preview Player Boundary ────────────────

export type ProductionMediaPlaybackPreparationStatus =
  | 'CREATED_INTERNAL_ONLY'
  | 'PRIVATE_STORAGE_REF_ACCEPTED'
  | 'PLAYBACK_METADATA_PREPARED_INTERNAL_ONLY'
  | 'READY_FOR_INTERNAL_PREVIEW_ONLY'
  | 'REJECTED_UNSAFE'
  | 'FAILED_SAFE'

export type ProductionMediaPlaybackPreparationIssueCode =
  | 'STORAGE_REF_MISSING'
  | 'STORAGE_REF_NOT_VERIFIED'
  | 'STORAGE_REF_REJECTED'
  | 'STORAGE_REF_FAILED'
  | 'FORMAT_NOT_SUPPORTED'
  | 'SIZE_EXCEEDS_PLAYBACK_LIMIT'
  | 'DURATION_EXCEEDS_LIMIT'
  | 'RESOLUTION_NOT_SUPPORTED'
  | 'INTERNAL_ERROR'

export interface ProductionMediaPlaybackPreparationResult {
  opaquePlaybackPreparationRef: string
  opaqueStorageObjectRef: string
  opaqueArtifactRef: string
  mediaFormat: string
  durationSeconds: number
  fps: number
  width: number
  height: number
  sizeBytes: number
  status: ProductionMediaPlaybackPreparationStatus
  issueCodes: ProductionMediaPlaybackPreparationIssueCode[]
  internalPreviewEligible: boolean
}

// ── PD-005: Human Review Decision Workflow ──────────────────

export type ProductionMediaReviewDecisionType =
  | 'APPROVE_FOR_INTERNAL_DELIVERY_PREP'
  | 'REJECT_FOR_INTERNAL_REVISION'
  | 'REQUEST_INTERNAL_FIX'
  | 'MARK_UNSAFE_INTERNAL_ONLY'

export type ProductionMediaReviewDecisionStatus =
  | 'CREATED_INTERNAL_ONLY'
  | 'REVIEW_METADATA_ACCEPTED'
  | 'DECISION_RECORDED_INTERNAL_ONLY'
  | 'APPROVED_FOR_INTERNAL_DELIVERY_PREP'
  | 'REJECTED_FOR_INTERNAL_REVISION'
  | 'INTERNAL_FIX_REQUESTED'
  | 'REJECTED_UNSAFE'
  | 'FAILED_SAFE'

export type ProductionMediaReviewDecisionIssueCode =
  | 'PREVIEW_METADATA_MISSING'
  | 'PREVIEW_NOT_ELIGIBLE'
  | 'STORAGE_REF_MISSING'
  | 'REVIEW_PACKAGE_MISSING'
  | 'DECISION_TYPE_INVALID'
  | 'ARTIFACT_NOT_VERIFIED'
  | 'INTERNAL_ERROR'

export interface ProductionMediaReviewDecisionResult {
  status: ProductionMediaReviewDecisionStatus
  decisionType: ProductionMediaReviewDecisionType | null
  issueCodes: ProductionMediaReviewDecisionIssueCode[]
  opaqueDecisionRef: string | null
  internalOnly: true
}

// ── PD-007: Controlled Public Playback MVP ──────────────────

export type ProductionMediaPublicPlaybackStatus =
  | 'CREATED_INTERNAL_ONLY'
  | 'DELIVERY_GATE_ACCEPTED'
  | 'CONTROLLED_PLAYBACK_HANDLE_CREATED'
  | 'READY_FOR_CONTROLLED_PUBLIC_PLAYBACK'
  | 'REJECTED_UNSAFE'
  | 'FAILED_SAFE'

export type ProductionMediaPublicPlaybackIssueCode =
  | 'DELIVERY_GATE_MISSING'
  | 'DELIVERY_GATE_NOT_APPROVED'
  | 'DELIVERY_GATE_REJECTED'
  | 'DELIVERY_GATE_FAILED'
  | 'STORAGE_REF_MISSING'
  | 'STORAGE_REF_NOT_VERIFIED'
  | 'PLAYBACK_PREP_MISSING'
  | 'FORMAT_NOT_SUPPORTED'
  | 'SIZE_EXCEEDS_LIMIT'
  | 'INTERNAL_ERROR'

export interface ProductionMediaPublicPlaybackResult {
  status: ProductionMediaPublicPlaybackStatus
  issueCodes: ProductionMediaPublicPlaybackIssueCode[]
  opaquePublicPlaybackRef: string | null
  opaqueDeliveryGateRef: string | null
  opaqueArtifactRef: string | null
  mediaFormat: string
  durationSeconds: number
  fps: number
  width: number
  height: number
  sizeBytes: number
  controlledPlaybackEligible: boolean
}

// ── RP-001: Türkü Name Storyboard Request ───────────────────

export type TurkuNameStoryboardRequestStatus =
  | 'CREATED_INTERNAL_ONLY'
  | 'TURKU_NAME_ACCEPTED'
  | 'COPYRIGHT_SAFE_INTENT_CREATED'
  | 'READY_FOR_STORYBOARD_GENERATION_BOUNDARY'
  | 'REJECTED_UNSAFE'
  | 'FAILED_SAFE'

export type TurkuNameStoryboardRequestIssueCode =
  | 'TITLE_EMPTY'
  | 'TITLE_TOO_LONG'
  | 'TITLE_CONTAINS_LYRICS'
  | 'TITLE_CONTAINS_MULTILINE'
  | 'TITLE_CONTAINS_URL'
  | 'SCENE_COUNT_OUT_OF_RANGE'
  | 'DURATION_OUT_OF_RANGE'
  | 'FPS_OUT_OF_RANGE'
  | 'RESOLUTION_OUT_OF_RANGE'
  | 'MEDIA_INPUT_DETECTED'
  | 'INTERNAL_ERROR'

export interface TurkuNameStoryboardRequestResult {
  status: TurkuNameStoryboardRequestStatus
  issueCodes: TurkuNameStoryboardRequestIssueCode[]
  opaqueStoryboardRequestRef: string | null
  safeTurkuTitleLabel: string | null
  inspirationMode: string | null
  targetSceneCount: number
  targetDurationMs: number
  targetFormat: string | null
  targetWidth: number
  targetHeight: number
  targetFps: number
  copyrightSafeIntentSummary: string | null
}

// ── RP-002: Safe Storyboard Draft Orchestration ─────────────

export type TurkuStoryboardDraftStatus =
  | 'CREATED_INTERNAL_ONLY'
  | 'REQUEST_ACCEPTED'
  | 'DRAFT_ORCHESTRATION_STARTED'
  | 'SCENES_GENERATED_SAFE'
  | 'READY_FOR_ADMIN_PREVIEW'
  | 'REJECTED_UNSAFE'
  | 'FAILED_SAFE'

export type TurkuStoryboardDraftIssueCode =
  | 'REQUEST_MISSING'
  | 'REQUEST_NOT_READY'
  | 'TITLE_MISSING'
  | 'SCENE_COUNT_INVALID'
  | 'DURATION_INVALID'
  | 'COPYRIGHT_INTENT_MISSING'
  | 'ORCHESTRATION_FAILED'
  | 'INTERNAL_ERROR'

export interface TurkuStoryboardDraftScene {
  sceneNumber: number
  purpose: string
  mood: string
  visualDirection: string
  durationMs: number
}

export interface TurkuStoryboardDraftResult {
  status: TurkuStoryboardDraftStatus
  issueCodes: TurkuStoryboardDraftIssueCode[]
  opaqueDraftRef: string | null
  opaqueRequestRef: string | null
  safeTurkuTitleLabel: string | null
  copyrightSafeIntentSummary: string | null
  orchestrationMode: string | null
  scenes: TurkuStoryboardDraftScene[]
  totalSceneCount: number
  totalDurationMs: number
  targetFormat: string | null
  targetWidth: number
  targetHeight: number
  targetFps: number
}

// ── RP-003: Scene Asset Plan + Visual Brief ─────────────────

export type TurkuSceneAssetPlanStatus =
  | 'CREATED_INTERNAL_ONLY'
  | 'DRAFT_ACCEPTED'
  | 'VISUAL_BRIEF_GENERATED'
  | 'READY_FOR_ASSET_GENERATION_BOUNDARY'
  | 'BLOCKED_CONTENT_UNSAFE'
  | 'REJECTED_UNSAFE'
  | 'FAILED_SAFE'

export type TurkuSceneAssetPlanIssueCode =
  | 'DRAFT_MISSING'
  | 'DRAFT_NOT_READY'
  | 'SCENE_MISSING'
  | 'SCENE_PURPOSE_EMPTY'
  | 'VISUAL_DIRECTION_EMPTY'
  | 'CONTENT_BLOCKED_UNSAFE'
  | 'INTERNAL_ERROR'

export interface TurkuSceneAssetPlanEntry {
  sceneNumber: number
  status: TurkuSceneAssetPlanStatus
  issueCodes: TurkuSceneAssetPlanIssueCode[]
  visualBriefSummary: string | null
  shotType: string | null
  framing: string | null
  atmosphere: string | null
  historicalCulturalStyle: string | null
  safetyClassification: string | null
  blockedReason: string | null
}

export interface TurkuSceneAssetPlanResult {
  overallStatus: TurkuSceneAssetPlanStatus
  overallIssueCodes: TurkuSceneAssetPlanIssueCode[]
  opaquePlanRef: string | null
  opaqueDraftRef: string | null
  safeTurkuTitleLabel: string | null
  plannerMode: string | null
  entries: TurkuSceneAssetPlanEntry[]
  totalPlannedScenes: number
  blockedSceneCount: number
}

// ── RP-004: Animatic Timeline Plan ──────────────────────────

export type TurkuAnimaticTimelinePlanStatus =
  | 'CREATED_INTERNAL_ONLY'
  | 'ASSET_PLAN_ACCEPTED'
  | 'TIMELINE_ITEMS_GENERATED'
  | 'READY_FOR_ASSEMBLY_BOUNDARY'
  | 'REJECTED_UNSAFE'
  | 'FAILED_SAFE'

export type TurkuAnimaticTimelinePlanIssueCode =
  | 'ASSET_PLAN_MISSING'
  | 'ASSET_PLAN_NOT_READY'
  | 'DRAFT_REF_MISSING'
  | 'NO_ENTRIES'
  | 'DURATION_INVALID'
  | 'INTERNAL_ERROR'

export interface TurkuAnimaticTimelineItem {
  sceneNumber: number
  startMs: number
  durationMs: number
  transitionType: string
  cameraMovement: string
  visualPacingSummary: string
  narrativeBeatSummary: string
}

export interface TurkuAnimaticTimelinePlanResult {
  status: TurkuAnimaticTimelinePlanStatus
  issueCodes: TurkuAnimaticTimelinePlanIssueCode[]
  opaqueTimelineRef: string | null
  opaquePlanRef: string | null
  opaqueDraftRef: string | null
  safeTurkuTitleLabel: string | null
  plannerMode: string | null
  items: TurkuAnimaticTimelineItem[]
  totalItems: number
  totalDurationMs: number
}

// ── RP-005: MVP Readiness Package ───────────────────────────

export type TurkuMvpReadinessStatus =
  | 'NOT_READY'
  | 'READY_FOR_MANUAL_REVIEW'
  | 'BLOCKED_BY_SAFETY'
  | 'PARTIALLY_READY'
  | 'FAILED_SAFE'

export type TurkuMvpReadinessIssueCode =
  | 'REQUEST_MISSING'
  | 'REQUEST_NOT_READY'
  | 'STORYBOARD_MISSING'
  | 'STORYBOARD_NOT_READY'
  | 'ASSET_PLAN_MISSING'
  | 'ASSET_PLAN_NOT_READY'
  | 'ASSET_PLAN_HAS_BLOCKED_SCENES'
  | 'TIMELINE_MISSING'
  | 'TIMELINE_NOT_READY'
  | 'INTERNAL_ERROR'

export interface TurkuMvpReadinessResult {
  status: TurkuMvpReadinessStatus
  blockingIssues: TurkuMvpReadinessIssueCode[]
  opaqueRequestRef: string | null
  safeTurkuTitleLabel: string | null
  storyboardReadinessSummary: string
  assetPlanReadinessSummary: string
  timelineReadinessSummary: string
  totalScenes: number
  blockedScenes: number
  timelineItems: number
  totalDurationMs: number
  copyrightSafeIntentPresent: boolean
  allScenesPlanned: boolean
  timelineComplete: boolean
  noBlockedScenes: boolean
  nextAllowedManualStepLabels: string[]
}

// ── MP-001: Manual Production Enablement ────────────────────

export type TurkuManualEnablementStatus =
  | 'NOT_ELIGIBLE'
  | 'ELIGIBLE_FOR_MANUAL_PREP'
  | 'BLOCKED_BY_SAFETY'
  | 'MANUAL_REVIEW_REQUIRED'
  | 'FAILED_SAFE'

export type TurkuManualEnablementIssueCode =
  | 'READINESS_NOT_READY'
  | 'READINESS_MISSING'
  | 'STORYBOARD_INCOMPLETE'
  | 'ASSET_PLAN_INCOMPLETE'
  | 'TIMELINE_INCOMPLETE'
  | 'BLOCKED_SCENES_PRESENT'
  | 'COPYRIGHT_INTENT_MISSING'
  | 'INTERNAL_ERROR'

export interface TurkuManualEnablementResult {
  status: TurkuManualEnablementStatus
  blockedReasons: TurkuManualEnablementIssueCode[]
  opaqueReadinessRef: string | null
  safeTurkuTitleLabel: string | null
  operatorSummary: string
  safetyPrerequisites: string[]
  manualStepLabels: string[]
  copyrightSafeIntentPresent: boolean
  allScenesPlanned: boolean
  timelineComplete: boolean
  noBlockedScenes: boolean
}

// ── MP-002: Manual Production Checklist + Work Order ────────

export type TurkuWorkOrderPreviewStatus =
  | 'DRAFT_PREVIEW'
  | 'BLOCKED_BY_SAFETY'
  | 'WAITING_FOR_MANUAL_REVIEW'
  | 'ELIGIBLE_FOR_MANUAL_PREPARATION'
  | 'FAILED_SAFE'

export type TurkuWorkOrderPreviewIssueCode =
  | 'ENABLEMENT_NOT_ELIGIBLE'
  | 'ENABLEMENT_MISSING'
  | 'CHECKLIST_INCOMPLETE'
  | 'SAFETY_PREREQUISITE_FAILED'
  | 'BLOCKED_SCENES_UNRESOLVED'
  | 'INTERNAL_ERROR'

export interface TurkuProductionChecklistItem {
  order: number
  label: string
  instruction: string
  prerequisiteMet: boolean
  blockedReason: string | null
}

export interface TurkuWorkOrderPreviewResult {
  status: TurkuWorkOrderPreviewStatus
  blockedReasons: TurkuWorkOrderPreviewIssueCode[]
  opaqueWorkOrderRef: string | null
  opaqueEnablementRef: string | null
  safeTurkuTitleLabel: string | null
  operatorSummary: string
  checklistItems: TurkuProductionChecklistItem[]
  totalItems: number
  completedItems: number
  blockedItems: number
  workOrderStepLabels: string[]
}

// ── RPE-001: Execution Safety Gate ──────────────────────────

export type TurkuExecutionGateStatus =
  | 'EXECUTION_NOT_ENABLED'
  | 'BLOCKED_BY_SAFETY'
  | 'WAITING_FOR_OPERATOR_GATE'
  | 'ELIGIBLE_FOR_FUTURE_EXECUTION_REVIEW'
  | 'FAILED_SAFE'

export type TurkuExecutionGateIssueCode =
  | 'WORK_ORDER_MISSING'
  | 'WORK_ORDER_NOT_ELIGIBLE'
  | 'CHECKLIST_INCOMPLETE'
  | 'IMAGE_PROVIDER_NOT_CONFIGURED'
  | 'RENDER_ENVIRONMENT_NOT_CONFIGURED'
  | 'OPERATOR_GATE_NOT_PASSED'
  | 'INTERNAL_ERROR'

export interface TurkuExecutionCapability {
  capabilityLabel: string
  enabled: boolean
  disabledReason: string
}

export interface TurkuExecutionGateResult {
  status: TurkuExecutionGateStatus
  blockedReasons: TurkuExecutionGateIssueCode[]
  opaqueExecutionRef: string | null
  opaqueWorkOrderRef: string | null
  safeTurkuTitleLabel: string | null
  auditDecisionSummary: string
  capabilities: TurkuExecutionCapability[]
  requiredOperatorGates: string[]
  futureExecutionStepLabels: string[]
}

// ── RPE-002: Image Provider Configuration ───────────────────

export type TurkuImageProviderType =
  | 'PLACEHOLDER_LOCAL'
  | 'DETERMINISTIC_FAKE'
  | 'FUTURE_EXTERNAL_API'

export type TurkuProviderConfigStatus =
  | 'NOT_CONFIGURED'
  | 'CONFIGURATION_DRAFT'
  | 'BLOCKED_BY_SAFETY'
  | 'WAITING_FOR_OPERATOR_REVIEW'
  | 'FAILED_SAFE'

export type TurkuProviderConfigIssueCode =
  | 'CREDENTIAL_NOT_PRESENT'
  | 'PROVIDER_TYPE_NOT_SELECTED'
  | 'FALLBACK_NOT_CONFIGURED'
  | 'SAFETY_VALIDATION_FAILED'
  | 'OPERATOR_REVIEW_PENDING'
  | 'INTERNAL_ERROR'

export interface TurkuCredentialVaultBoundary {
  vaultLabel: string
  credentialPresent: boolean
  presenceStatusLabel: string
}

export interface TurkuProviderConfigResult {
  status: TurkuProviderConfigStatus
  issues: TurkuProviderConfigIssueCode[]
  opaqueConfigRef: string | null
  providerLabel: string | null
  providerType: TurkuImageProviderType | null
  capabilityLabels: string[]
  vaultBoundary: TurkuCredentialVaultBoundary | null
  selectionPolicySummary: string | null
  fallbackPolicySummary: string | null
  executionEnabled: boolean
  safetyValidationSummary: string
}

// ── RPE-003: Render Pipeline Configuration ──────────────────

export type TurkuRenderConfigStatus =
  | 'NOT_CONFIGURED'
  | 'CONFIGURATION_DRAFT'
  | 'BLOCKED_BY_SAFETY'
  | 'WAITING_FOR_OPERATOR_REVIEW'
  | 'FAILED_SAFE'

export type TurkuRenderConfigIssueCode =
  | 'RENDER_ENVIRONMENT_MISSING'
  | 'FFMPEG_NOT_AVAILABLE'
  | 'OUTPUT_FORMAT_NOT_SELECTED'
  | 'RESOLUTION_NOT_SELECTED'
  | 'SAFETY_PREREQUISITE_NOT_MET'
  | 'OPERATOR_REVIEW_PENDING'
  | 'INTERNAL_ERROR'

export interface TurkuRenderEnvironmentBoundary {
  environmentLabel: string
  environmentConfigured: boolean
  configurationStatusLabel: string
}

export interface TurkuFfmpegCapabilityMetadata {
  capabilityLabel: string
  available: boolean
  availabilityNote: string
}

export interface TurkuOutputFormatPolicy {
  formatLabel: string
  width: number
  height: number
  frameRate: number
  policyNote: string
}

export interface TurkuRenderPipelineConfigResult {
  status: TurkuRenderConfigStatus
  issues: TurkuRenderConfigIssueCode[]
  opaqueRenderConfigRef: string | null
  environmentBoundary: TurkuRenderEnvironmentBoundary | null
  ffmpegCapabilities: TurkuFfmpegCapabilityMetadata[]
  outputFormatPolicy: TurkuOutputFormatPolicy | null
  safetyPrerequisiteLabels: string[]
  renderExecutionEnabled: boolean
  validationSummary: string
}

// ── RPE-004: Execution Package + Manual Gate ────────────────

export type TurkuExecutionPackageStatus =
  | 'PACKAGE_DRAFT'
  | 'NOT_READY'
  | 'BLOCKED_BY_SAFETY'
  | 'WAITING_FOR_MANUAL_GATE'
  | 'FAILED_SAFE'

export type TurkuExecutionPackageIssueCode =
  | 'WORK_ORDER_NOT_ELIGIBLE'
  | 'IMAGE_PROVIDER_NOT_CONFIGURED'
  | 'RENDER_PIPELINE_NOT_CONFIGURED'
  | 'MANUAL_GATE_NOT_PASSED'
  | 'SAFETY_PREREQUISITE_NOT_MET'
  | 'INTERNAL_ERROR'

export interface TurkuManualGate {
  order: number
  gateLabel: string
  instruction: string
  passed: boolean
  blockedReason: string | null
}

export interface TurkuExecutionPackageResult {
  status: TurkuExecutionPackageStatus
  blockedReasons: TurkuExecutionPackageIssueCode[]
  opaquePackageRef: string | null
  opaqueWorkOrderRef: string | null
  safeTurkuTitleLabel: string | null
  providerConfigSummary: string
  renderConfigSummary: string
  manualGates: TurkuManualGate[]
  executionCapabilities: TurkuExecutionCapability[]
  operatorExecutionSummary: string
  futureExecutionStepLabels: string[]
  validationSummary: string
}

// ── RPE-005: Image Prompt Package + Manual Approval ─────────

export type TurkuImagePromptPackageStatus =
  | 'DRAFT'
  | 'PENDING_MANUAL_APPROVAL'
  | 'BLOCKED_BY_SAFETY'
  | 'WAITING_FOR_OPERATOR_REVIEW'
  | 'FAILED_SAFE'

export type TurkuImagePromptPackageIssueCode =
  | 'EXECUTION_PACKAGE_NOT_READY'
  | 'SCENE_VISUAL_BRIEF_MISSING'
  | 'SAFETY_CONSTRAINT_VIOLATED'
  | 'PROMPT_CONTENT_BLOCKED'
  | 'OPERATOR_REVIEW_PENDING'
  | 'INTERNAL_ERROR'

export interface TurkuSceneImagePrompt {
  sceneNo: number
  safePromptSummary: string
  safeNegativeConstraints: string
  safetyConstraintLabels: string[]
  aspectRatio: string
  styleHint: string
  blocked: boolean
  blockedReason: string | null
}

export interface TurkuImagePromptPackageResult {
  status: TurkuImagePromptPackageStatus
  issues: TurkuImagePromptPackageIssueCode[]
  opaquePromptPackageRef: string | null
  opaqueExecutionPackageRef: string | null
  safeTurkuTitleLabel: string | null
  scenePrompts: TurkuSceneImagePrompt[]
  totalScenes: number
  blockedScenes: number
  manualApprovalGranted: boolean
  approvalGateLabel: string
  validationSummary: string
}

// ── RPE-006: Image Generation Execution ─────────────────────

export type TurkuImageGenerationExecutionMode =
  | 'DISABLED'
  | 'CI_FAKE'
  | 'LOCAL_MANUAL_ONLY'

export type TurkuImageGenerationStatus =
  | 'NOT_STARTED'
  | 'FAKE_RESULT_AVAILABLE'
  | 'BLOCKED_BY_SAFETY'
  | 'WAITING_FOR_MANUAL_GATE'
  | 'FAILED_SAFE'

export type TurkuImageGenerationIssueCode =
  | 'PROMPT_PACKAGE_NOT_APPROVED'
  | 'MANUAL_GATE_NOT_PASSED'
  | 'PROVIDER_EXECUTION_DISABLED'
  | 'SAFETY_CONSTRAINT_VIOLATED'
  | 'OPERATOR_REVIEW_PENDING'
  | 'INTERNAL_ERROR'

export interface TurkuFakeImageResultMetadata {
  sceneNo: number
  opaqueArtifactRef: string
  artifactLabel: string
  formatHint: string
  widthHint: number
  heightHint: number
  generationNote: string
}

export interface TurkuImageGenerationExecutionResult {
  status: TurkuImageGenerationStatus
  issues: TurkuImageGenerationIssueCode[]
  opaqueGenerationRef: string | null
  opaquePromptPackageRef: string | null
  executionMode: TurkuImageGenerationExecutionMode
  providerDisabledReason: string
  manualGatePassed: boolean
  fakeResults: TurkuFakeImageResultMetadata[]
  totalScenes: number
  fakeResultCount: number
  validationSummary: string
}

// ── RPE-007: Artifact Verification + Render Package ─────────

export type TurkuArtifactVerificationStatus =
  | 'NOT_VERIFIED'
  | 'FAKE_VERIFIED'
  | 'BLOCKED_BY_SAFETY'
  | 'WAITING_FOR_OPERATOR_REVIEW'
  | 'FAILED_SAFE'

export type TurkuRenderPackageStatus =
  | 'NOT_READY'
  | 'DRAFT'
  | 'BLOCKED_BY_SAFETY'
  | 'WAITING_FOR_MANUAL_GATE'
  | 'FAILED_SAFE'

export interface TurkuSceneArtifactReadiness {
  sceneNo: number
  opaqueArtifactRef: string
  artifactLabel: string
  verified: boolean
  verificationNote: string
}

export interface TurkuArtifactVerificationReport {
  status: TurkuArtifactVerificationStatus
  opaqueVerificationRef: string | null
  opaqueGenerationRef: string | null
  sceneReadiness: TurkuSceneArtifactReadiness[]
  totalScenes: number
  verifiedScenes: number
  validationSummary: string
}

export interface TurkuRenderPackageResult {
  status: TurkuRenderPackageStatus
  opaqueRenderPackageRef: string | null
  opaqueVerificationRef: string | null
  renderInputManifestSummary: string
  renderPrerequisiteLabels: string[]
  localManualRenderEnabled: boolean
  localManualRenderNote: string
  blockedReasons: string[]
  validationSummary: string
}

// ── LIVE-001: Provider Credential Enablement + Kill Switch ──

export type TurkuLiveProviderStatus =
  | 'DISABLED_BY_DEFAULT'
  | 'CREDENTIAL_PRESENT'
  | 'BLOCKED_BY_KILL_SWITCH'
  | 'WAITING_FOR_OPERATOR_ENABLEMENT'
  | 'FAILED_SAFE'

export type TurkuLiveProviderIssueCode =
  | 'CREDENTIAL_NOT_PRESENT'
  | 'KILL_SWITCH_ACTIVE'
  | 'OPERATOR_ENABLEMENT_REQUIRED'
  | 'PREREQUISITE_NOT_MET'
  | 'INTERNAL_ERROR'

export interface TurkuKillSwitch {
  active: boolean
  reason: string
  activatedBy: string
}

export interface TurkuLiveProviderReadinessReport {
  status: TurkuLiveProviderStatus
  issues: TurkuLiveProviderIssueCode[]
  credentialPresent: boolean
  credentialSourceLabel: string
  operatorEnabled: boolean
  killSwitch: TurkuKillSwitch
  prerequisiteLabels: string[]
  blockedReasons: string[]
  providerTypeLabel: string
  validationSummary: string
}

// ── LIVE-002: Manual-Gated Real Image Generation ────────────

export type TurkuLiveGenerationMode =
  | 'CI_FAKE'
  | 'LOCAL_MANUAL_REAL_PROVIDER'

export type TurkuLiveGenerationStatus =
  | 'NOT_STARTED'
  | 'FAKE_RESULT_AVAILABLE'
  | 'REAL_PROVIDER_RESULT_AVAILABLE'
  | 'BLOCKED_BY_KILL_SWITCH'
  | 'BLOCKED_BY_OPERATOR_GATE'
  | 'BLOCKED_BY_PROVIDER_READINESS'
  | 'FAILED_SAFE'

export interface TurkuLiveImageResultMetadata {
  sceneNo: number
  opaqueArtifactRef: string
  artifactLabel: string
  formatHint: string
  widthHint: number
  heightHint: number
  generationMode: TurkuLiveGenerationMode
  sanitizedProviderSummary: string
  humanReviewRequired: boolean
}

export interface TurkuLiveGenerationResult {
  status: TurkuLiveGenerationStatus
  mode: TurkuLiveGenerationMode
  opaqueGenerationRef: string | null
  opaquePromptPackageRef: string | null
  killSwitchActive: boolean
  operatorGatePassed: boolean
  providerReady: boolean
  results: TurkuLiveImageResultMetadata[]
  totalScenes: number
  generatedCount: number
  blockedReasons: string[]
  sanitizedProviderSummary: string
  validationSummary: string
}
