import { useState, useEffect, useCallback } from 'react'
import type { ReviewTargetType, SafeAdminPreview } from '../../types'
import * as reviewApi from '../../api/reviewApi'

/**
 * FP-013 Task 04: Safe admin preview panel.
 *
 * Displays safe preview metadata/context only — no bytes, no URL,
 * no base64, no storage key, no provider URL, no raw payload.
 *
 * For IMAGE_ASSET: shows safe availability/label only.
 * No dangerouslySetInnerHTML, no innerHTML, no raw HTML rendering.
 * No publish/export wording as enabled behavior.
 * No stack trace display.
 */

/** Safe preview mode labels */
const PREVIEW_MODE_LABELS: Record<string, string> = {
  TEXT_SUMMARY: 'Metin Özeti',
  SCENARIO_SUMMARY: 'Senaryo Özeti',
  STORYBOARD_SUMMARY: 'Storyboard Özeti',
  PACKAGE_SUMMARY: 'Paket Özeti',
  IMAGE_ASSET: 'Görsel Bilgisi',
  UNAVAILABLE: 'Kullanılamıyor',
}

/** Safe availability labels */
const AVAILABILITY_LABELS: Record<string, string> = {
  AVAILABLE: 'Mevcut',
  UNAVAILABLE: 'Kullanılamıyor',
  NOT_REVIEWABLE: 'İncelemeye Uygun Değil',
  MISSING_TARGET: 'Hedef Bulunamadı',
  UNSAFE_TARGET: 'Güvenli Değil',
  STORAGE_UNAVAILABLE: 'Depolama Kullanılamıyor',
  PROVIDER_UNAVAILABLE: 'Sağlayıcı Kullanılamıyor',
}

/** Safe review state labels — no publish/export wording */
const STATE_LABELS: Record<string, string> = {
  DRAFT: 'Taslak',
  AI_GENERATED: 'AI Üretildi',
  NEEDS_REVIEW: 'İnceleme Bekliyor',
  IN_REVIEW: 'İnceleniyor',
  CHANGES_REQUESTED: 'Değişiklik İstendi',
  APPROVED_FOR_INTERNAL_USE: 'Dahili Onay (Sadece İç Kullanım)',
  REJECTED: 'Reddedildi',
}

interface ReviewPreviewPanelProps {
  targetType: ReviewTargetType
  targetId: string
}

export default function ReviewPreviewPanel({
  targetType,
  targetId,
}: ReviewPreviewPanelProps) {
  const [preview, setPreview] = useState<SafeAdminPreview | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const loadPreview = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const res = await reviewApi.getPreview(targetType, targetId)
      if (res.success && res.data) {
        setPreview(res.data)
      } else {
        setError(safeText(res.errorMessage ?? 'Önizleme yüklenemedi'))
      }
    } catch (err: unknown) {
      const msg = err && typeof err === 'object' && 'message' in err
        ? String((err as { message: string }).message)
        : 'Önizleme yüklenemedi'
      setError(safeText(msg))
    } finally {
      setLoading(false)
    }
  }, [targetType, targetId])

  useEffect(() => {
    loadPreview()
  }, [loadPreview])

  return (
    <div className="review-preview-panel" data-testid="review-preview-panel">
      <h4 className="review-preview-panel__title">Önizleme</h4>

      {/* Loading */}
      {loading && (
        <div className="review-preview-panel__loading" data-testid="review-preview-loading">
          Yükleniyor...
        </div>
      )}

      {/* Error — safe text only */}
      {error && !loading && (
        <div className="review-preview-panel__error" data-testid="review-preview-error" role="alert">
          {safeText(error)}
        </div>
      )}

      {/* Unavailable State */}
      {preview && !preview.previewAvailable && !loading && (
        <div className="review-preview-panel__unavailable" data-testid="review-preview-unavailable">
          <span className="review-preview-panel__availability-badge">
            {AVAILABILITY_LABELS[preview.availability] ?? 'Kullanılamıyor'}
          </span>
          {preview.safePreviewLabel && (
            <p className="review-preview-panel__label" data-testid="review-preview-label">
              {safeText(preview.safePreviewLabel)}
            </p>
          )}
          {preview.safeMessage && (
            <p className="review-preview-panel__message">
              {safeText(preview.safeMessage)}
            </p>
          )}
        </div>
      )}

      {/* Available Preview — safe metadata only */}
      {preview && preview.previewAvailable && !loading && (
        <div className="review-preview-panel__content" data-testid="review-preview-content">
          {/* Preview Mode Label */}
          <div className="review-preview-panel__mode" data-testid="review-preview-mode">
            <span className="review-preview-panel__mode-badge">
              {PREVIEW_MODE_LABELS[preview.previewMode] ?? preview.previewMode}
            </span>
          </div>

          {/* Safe Title */}
          {preview.safeTitle && (
            <div className="review-preview-panel__safe-title" data-testid="review-preview-safe-title">
              <strong>Başlık:</strong> {safeText(preview.safeTitle)}
            </div>
          )}

          {/* Safe Summary */}
          {preview.safeSummary && (
            <div className="review-preview-panel__safe-summary" data-testid="review-preview-safe-summary">
              <strong>Özet:</strong> {safeText(preview.safeSummary)}
            </div>
          )}

          {/* Preview Label */}
          {preview.safePreviewLabel && (
            <div className="review-preview-panel__label" data-testid="review-preview-label">
              {safeText(preview.safePreviewLabel)}
            </div>
          )}

          {/* Current Review State */}
          {preview.currentReviewState && (
            <div className="review-preview-panel__state" data-testid="review-preview-state">
              <span>Durum: </span>
              <span className="review-preview-panel__state-value">
                {STATE_LABELS[preview.currentReviewState] ?? preview.currentReviewState}
              </span>
            </div>
          )}

          {/* Thumbnail availability indicator — label only, no image */}
          {preview.previewMode === 'IMAGE_ASSET' && (
            <div className="review-preview-panel__thumbnail-info" data-testid="review-preview-thumbnail-info">
              <span>Küçük Resim: </span>
              <span data-testid="review-preview-thumbnail-status">
                {preview.safeThumbnailAvailable ? 'Mevcut' : 'Mevcut Değil'}
              </span>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

/**
 * Safe text rendering — strips dangerous content.
 * Never use unsafe HTML injection.
 */
function safeText(text: string): string {
  if (!text) return ''
  return text
    .replace(/[<>]/g, '')
    .replace(/javascript:/gi, '')
    .replace(/on\w+\s*=/gi, '')
    .slice(0, 2000)
}
