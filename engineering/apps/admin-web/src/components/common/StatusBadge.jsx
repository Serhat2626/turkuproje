const STATUS_MAP = {
  DRAFT: { label: 'Taslak', className: 'draft' },
  RESEARCHED: { label: 'Araştırıldı', className: 'complete' },
  SCRIPT_GENERATED: { label: 'Senaryo Hazır', className: 'complete' },
  STORYBOARD_GENERATED: { label: 'Storyboard Hazır', className: 'complete' },
  MEDIA_PROMPTS_GENERATED: { label: 'Promptlar Hazır', className: 'complete' },
  VOICE_READY: { label: 'Ses Hazır', className: 'complete' },
  VIDEO_READY: { label: 'Video Hazır', className: 'complete' },
  PUBLISHED: { label: 'Yayında', className: 'complete' },
}

export default function StatusBadge({ status }) {
  const config = STATUS_MAP[status] || { label: status, className: 'draft' }
  return <span className={`status-badge ${config.className}`}>{config.label}</span>
}
