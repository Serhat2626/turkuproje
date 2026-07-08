/**
 * FP-053: Internal Dry Run Operational Summary Panel — counts/status only.
 * No individual record detail, no paths, no logs, no URLs, no media.
 */

import type {
  TurkuInternalDryRunOperationalSummary,
  TurkuInternalDryRunOperationalSummaryStatus,
} from '../../types'

export function renderOperationalStatus(status: TurkuInternalDryRunOperationalSummaryStatus): string {
  const labels: Record<TurkuInternalDryRunOperationalSummaryStatus, string> = {
    NO_RECORDS: 'Kayıt Yok',
    ALL_ACCEPTED: 'Tümü Kabul Edildi',
    MIXED: 'Karışık Durum',
    ALL_FAILED: 'Tümü Başarısız',
    ALL_REJECTED: 'Tümü Reddedildi',
  }
  return labels[status] ?? status
}

export function isOperationalHealthy(summary: TurkuInternalDryRunOperationalSummary): boolean {
  return summary.overallStatus === 'ALL_ACCEPTED'
}

export function getOperationalMetadataSummary(summary: TurkuInternalDryRunOperationalSummary): string {
  const lines: string[] = [
    `Durum: ${renderOperationalStatus(summary.overallStatus)}`,
    `Toplam Kayıt: ${summary.totalRecordCount}`,
    `Kabul: ${summary.acceptedCount}`,
    `Başarısız: ${summary.failedCount}`,
    `Reddedilen: ${summary.rejectedCount}`,
    `Admin Geçmişi: ${summary.readyForAdminHistoryOnlyCount}`,
    `Toplam Sahne: ${summary.totalSceneCount}`,
    `Toplam Klip: ${summary.totalClipCount}`,
    `Birleştirilmiş: ${summary.stitchedCount}`,
    `Dahili: ${summary.internalOnly ? 'Evet' : 'Hayır'}`,
  ]
  if (summary.issueCodes.length > 0) {
    lines.push(`Sorunlar: ${summary.issueCodes.join(', ')}`)
  }
  return lines.join('\n')
}
