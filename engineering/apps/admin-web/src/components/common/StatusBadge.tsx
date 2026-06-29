interface StatusBadgeProps {
  status: string;
}

const statusColors: Record<string, string> = {
  DRAFT: 'badge-draft',
  READY: 'badge-success',
  CREATED: 'badge-draft',
  AI_REQUESTED: 'badge-info',
  AI_PROCESSING: 'badge-info',
  DRAFT_READY: 'badge-draft',
  IN_REVIEW: 'badge-info',
  REVISION_REQUESTED: 'badge-info',
  APPROVED: 'badge-success',
  READY_FOR_PUBLISH: 'badge-success',
  GENERATED: 'badge-info',
  EDITED: 'badge-draft',
  REVISION_NEEDED: 'badge-info',
  RESEARCHING: 'badge-info',
  VERIFIED: 'badge-success',
  ARCHIVED: 'badge-muted',
  CANCELLED: 'badge-muted',
  REJECTED: 'badge-error',
  SCRIPTING: 'badge-info',
  PUBLISHED: 'badge-success',
  REQUESTED: 'badge-info',
  SUCCESS: 'badge-success',
  FAILED: 'badge-error',
};

function StatusBadge({ status }: StatusBadgeProps) {
  const cls = statusColors[status] || 'badge-draft';
  return <span className={`status-badge ${cls}`}>{status}</span>;
}

export default StatusBadge;
