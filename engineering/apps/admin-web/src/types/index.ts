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
