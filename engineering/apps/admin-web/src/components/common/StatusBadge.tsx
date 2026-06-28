interface StatusBadgeProps {
  status: string;
}

const statusColors: Record<string, string> = {
  DRAFT: 'badge-draft',
  RESEARCHING: 'badge-info',
  VERIFIED: 'badge-success',
  ARCHIVED: 'badge-muted',
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
