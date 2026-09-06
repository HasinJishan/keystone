import type { Priority, WorkOrderStatus } from '../api/types';

export function PriorityBadge({ priority }: { priority: Priority }) {
  const cls = priority === 'CRITICAL' || priority === 'HIGH' ? 'badge-danger'
    : priority === 'MEDIUM' ? 'badge-amber' : 'badge-slate';
  return <span className={`badge ${cls}`}>{priority}</span>;
}

export function StatusBadge({ status }: { status: WorkOrderStatus }) {
  const cls = status === 'CLOSED' || status === 'COMPLETED' ? 'badge-success'
    : status === 'CANCELLED' ? 'badge-slate'
    : status === 'ON_HOLD' ? 'badge-amber'
    : 'badge-slate';
  return <span className={`badge ${cls}`}>{status.replace('_', ' ')}</span>;
}

export function SlaBadge({ dueAt, breached }: { dueAt: string; breached: boolean }) {
  if (breached) return <span className="badge badge-danger">SLA BREACHED</span>;
  const due = new Date(dueAt);
  const hoursLeft = (due.getTime() - Date.now()) / 3_600_000;
  if (hoursLeft < 0) return <span className="badge badge-danger">OVERDUE</span>;
  if (hoursLeft < 4) return <span className="badge badge-amber">DUE SOON</span>;
  return <span className="badge badge-slate">{due.toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}</span>;
}
