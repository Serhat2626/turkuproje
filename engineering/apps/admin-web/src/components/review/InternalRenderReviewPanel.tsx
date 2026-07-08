import type {
  TurkuRenderReviewPackage,
  TurkuRenderReviewPackageStatus,
  TurkuRenderReviewPackageIssueCode,
  TurkuRenderReviewArtifactSummary,
} from '../../types'

/**
 * FP-043: Internal Render Review Summary Panel.
 *
 * Shows metadata-only review package state.
 * No player/preview. No indirme/dışa aktarım/yayın.
 * No public URL/link. No raw HTML.
 * "READY" means internal review only, NOT yayın/dışa aktarım.
 */

const STATUS_LABELS: Record<TurkuRenderReviewPackageStatus, string> = {
  CREATED_INTERNAL_ONLY: 'Oluşturuldu (Dahili)',
  ARTIFACT_METADATA_ATTACHED: 'Meta Veriler Eklendi',
  READY_FOR_INTERNAL_REVIEW_ONLY: 'Dahili İnceleme İçin Hazır',
  REJECTED_UNSAFE: 'Reddedildi (Güvenli Değil)',
  FAILED_SAFE: 'Başarısız (Güvenli)',
}

const STATUS_CLASSES: Record<TurkuRenderReviewPackageStatus, string> = {
  CREATED_INTERNAL_ONLY: 'review-status--created',
  ARTIFACT_METADATA_ATTACHED: 'review-status--attached',
  READY_FOR_INTERNAL_REVIEW_ONLY: 'review-status--ready',
  REJECTED_UNSAFE: 'review-status--rejected',
  FAILED_SAFE: 'review-status--failed',
}

const ISSUE_LABELS: Record<TurkuRenderReviewPackageIssueCode, string> = {
  NONE: 'Yok',
  NO_PRIMARY_ARTIFACT: 'Birincil eser eksik',
  ARTIFACT_NOT_VERIFIED: 'Eser doğrulanmadı',
  ARTIFACT_REJECTED: 'Eser reddedildi',
  ARTIFACT_FAILED: 'Eser başarısız',
  ARTIFACT_ROLE_UNKNOWN: 'Bilinmeyen rol',
  TOO_MANY_ARTIFACTS: 'Çok fazla eser',
  DUPLICATE_ARTIFACT_REF: 'Yinelenen referans',
  MISSING_ARTIFACT_REF: 'Eksik referans',
  LIFECYCLE_STATUS_INVALID: 'Geçersiz yaşam döngüsü',
  BUNDLE_REF_MISSING: 'Paket referansı eksik',
  METADATA_INCOMPLETE: 'Meta veriler eksik',
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function ArtifactSummaryRow({ summary }: { summary: TurkuRenderReviewArtifactSummary }) {
  return (
    <tr className="review-artifact-row" data-testid="review-artifact-row">
      <td>{summary.opaqueArtifactRef?.substring(0, 12) ?? '—'}…</td>
      <td>{summary.role}</td>
      <td>
        <span className={summary.verified ? 'badge--verified' : 'badge--rejected'}>
          {summary.verificationStatus}
        </span>
      </td>
      <td>{summary.format}</td>
      <td>{summary.durationSeconds}s</td>
      <td>{summary.width}×{summary.height}</td>
      <td>{summary.fps}fps</td>
      <td>{formatBytes(summary.sizeBytes)}</td>
    </tr>
  )
}

export default function InternalRenderReviewPanel({
  reviewPackage,
}: {
  reviewPackage: TurkuRenderReviewPackage | null
}) {
  if (!reviewPackage) {
    return (
      <section className="review-panel" data-testid="internal-render-review-panel">
        <h3 className="review-panel__title">Dahili Render İnceleme Paketi</h3>
        <p className="review-panel__empty">Henüz inceleme paketi oluşturulmadı.</p>
      </section>
    )
  }

  const statusLabel = STATUS_LABELS[reviewPackage.status] ?? reviewPackage.status
  const statusClass = STATUS_CLASSES[reviewPackage.status] ?? 'review-status--unknown'

  return (
    <section className="review-panel" data-testid="internal-render-review-panel">
      <h3 className="review-panel__title">Dahili Render İnceleme Paketi</h3>
      <p className="review-panel__internal-notice" data-testid="internal-only-notice">
        ⚠ Yalnızca dahili inceleme — yayın/dışa aktarım/indirme/oynatıcı yok
      </p>

      <div className="review-panel__header">
        <span className={`review-status-badge ${statusClass}`} data-testid="review-status">
          {statusLabel}
        </span>
        <span className="review-panel__ref" data-testid="review-ref">
          {reviewPackage.opaqueReviewPackageRef?.substring(0, 16) ?? '—'}
        </span>
      </div>

      {reviewPackage.issueCodes.length > 0 && reviewPackage.issueCodes[0] !== 'NONE' && (
        <div className="review-panel__issues" data-testid="review-issues">
          <h4>Sorunlar</h4>
          <ul>
            {reviewPackage.issueCodes.map((code, i) => (
              <li key={i} className="review-issue-item">
                {ISSUE_LABELS[code] ?? code}
              </li>
            ))}
          </ul>
        </div>
      )}

      <div className="review-panel__metadata" data-testid="review-metadata">
        <table className="review-metadata-table">
          <tbody>
            <tr><td>Sahne Sayısı</td><td>{reviewPackage.sceneCount}</td></tr>
            <tr><td>Klip Sayısı</td><td>{reviewPackage.inputClipCount}</td></tr>
            <tr><td>Toplam Süre</td><td>{reviewPackage.totalDurationSeconds}s</td></tr>
            <tr><td>Maks Çözünürlük</td><td>{reviewPackage.maxWidth}×{reviewPackage.maxHeight}</td></tr>
            <tr><td>Maks FPS</td><td>{reviewPackage.maxFps}</td></tr>
            <tr><td>Toplam Boyut</td><td>{formatBytes(reviewPackage.totalSizeBytes)}</td></tr>
          </tbody>
        </table>
      </div>

      {reviewPackage.artifactSummaries.length > 0 && (
        <div className="review-panel__artifacts" data-testid="review-artifacts">
          <h4>Eser Özetleri ({reviewPackage.artifactSummaries.length})</h4>
          <table className="review-artifacts-table">
            <thead>
              <tr>
                <th>Ref</th><th>Rol</th><th>Doğrulama</th><th>Format</th>
                <th>Süre</th><th>Çözünürlük</th><th>FPS</th><th>Boyut</th>
              </tr>
            </thead>
            <tbody>
              {reviewPackage.artifactSummaries.map((s, i) => (
                <ArtifactSummaryRow key={i} summary={s} />
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
