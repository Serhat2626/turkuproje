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
}

export interface KnowledgeItemResponse {
  id: string;
  title: string;
  slug: string;
  type: string;
  status: string;
  language: string;
  createdAt: string;
}

export const KNOWLEDGE_ITEM_TYPES = [
  { value: 'FOLK_SONG', label: 'Türkü' },
  { value: 'ARTIST', label: 'Sanatçı' },
  { value: 'REGION', label: 'Bölge' },
  { value: 'TRADITION', label: 'Gelenek' },
  { value: 'HISTORICAL_EVENT', label: 'Tarihî Olay' },
  { value: 'LEGEND', label: 'Efsane' },
] as const;

export const LANGUAGES = [
  { value: 'tr', label: 'Türkçe' },
  { value: 'en', label: 'English' },
] as const;

// ── Content Domain ──────────────────────────────────────────

export interface CreateContentProjectRequest {
  knowledgeItemId: string;
  title: string;
  contentType: string;
  targetPlatform: string;
  language: string;
}

export interface ContentProjectResponse {
  id: string;
  knowledgeItemId: string;
  title: string;
  slug: string;
  contentType: string;
  targetPlatform: string;
  status: string;
  language: string;
  createdAt: string;
}

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
