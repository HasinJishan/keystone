import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { api, ApiError } from '../api/client';
import type { WorkOrderResponse, WorkOrderStatus } from '../api/types';
import { PriorityBadge, StatusBadge, SlaBadge } from '../components/Badges';
import { useAuth } from '../auth/AuthContext';
import AssignPanel from '../components/AssignPanel';
import PartsTimePanel from '../components/PartsTimePanel';

const NEXT_STATUSES: Record<WorkOrderStatus, WorkOrderStatus[]> = {
  NEW: [],
  ASSIGNED: ['IN_PROGRESS'],
  IN_PROGRESS: ['ON_HOLD', 'COMPLETED'],
  ON_HOLD: ['IN_PROGRESS'],
  COMPLETED: ['CLOSED', 'IN_PROGRESS'],
  CLOSED: [],
  CANCELLED: [],
};

const STATUS_LABEL: Record<WorkOrderStatus, string> = {
  NEW: 'New', ASSIGNED: 'Assigned', IN_PROGRESS: 'Start Work', ON_HOLD: 'Put On Hold',
  COMPLETED: 'Mark Complete', CLOSED: 'Close Job', CANCELLED: 'Cancel',
};

export default function WorkOrderDetail() {
  const { id } = useParams();
  const { user } = useAuth();
  const [wo, setWo] = useState<WorkOrderResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function load() {
    setError(null);
    try {
      const res = await api.get<WorkOrderResponse>(`/api/work-orders/${id}`);
      setWo(res);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not load this work order.');
    }
  }

  useEffect(() => { load(); }, [id]);

  async function changeStatus(toStatus: WorkOrderStatus) {
    setBusy(true);
    setError(null);
    try {
      await api.post(`/api/work-orders/${id}/status`, { toStatus });
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'That transition was rejected.');
    } finally {
      setBusy(false);
    }
  }

  if (error && !wo) return <div className="form-error">{error}</div>;
  if (!wo) return <div className="empty-state">Loading…</div>;

  const canManageLifecycle = user?.role === 'MANAGER' || user?.role === 'DISPATCHER' ||
    (user?.role === 'TECHNICIAN' && wo.assignedTo === user.userId);

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title mono">{wo.code} — {wo.title}</h1>
          <div className="page-sub">{wo.customerName} · {wo.siteName}</div>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <PriorityBadge priority={wo.priority} />
          <StatusBadge status={wo.status} />
          <SlaBadge dueAt={wo.slaDueAt} breached={wo.slaBreached} />
        </div>
      </div>

      {error && <div className="form-error">{error}</div>}

      <div className="panel">
        <div className="panel-header">Details</div>
        <div className="panel-body">
          <p style={{ marginTop: 0 }}>{wo.description || <em>No description provided.</em>}</p>
          <div style={{ fontSize: 12.5, color: 'var(--ink-soft)' }}>
            Assigned to: <strong style={{ color: 'var(--ink)' }}>{wo.assignedToName || 'Unassigned'}</strong>
          </div>
        </div>
      </div>

      {(user?.role === 'DISPATCHER' || user?.role === 'MANAGER') && !wo.status.match(/CLOSED|CANCELLED/) && (
        <AssignPanel workOrderId={wo.id} currentAssignee={wo.assignedToName} onAssigned={load} />
      )}

      {canManageLifecycle && NEXT_STATUSES[wo.status].length > 0 && (
        <div className="panel">
          <div className="panel-header">Update Status</div>
          <div className="panel-body" style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            {NEXT_STATUSES[wo.status].map((s) => (
              <button key={s} className="btn btn-primary" disabled={busy} onClick={() => changeStatus(s)}>
                {STATUS_LABEL[s]}
              </button>
            ))}
          </div>
        </div>
      )}

      {user?.role === 'MANAGER' && wo.status === 'NEW' && (
        <div className="panel">
          <div className="panel-header">Cancel</div>
          <div className="panel-body">
            <button className="btn btn-danger" disabled={busy} onClick={() => changeStatus('CANCELLED')}>Cancel Work Order</button>
          </div>
        </div>
      )}

      {(user?.role === 'TECHNICIAN' || user?.role === 'MANAGER') && !wo.status.match(/CLOSED|CANCELLED|NEW/) && (
        <PartsTimePanel workOrderId={wo.id} onLogged={load} />
      )}

      <div className="panel">
        <div className="panel-header">Parts Used</div>
        <div className="panel-body">
          {wo.partsUsed.length === 0 ? <div style={{ color: 'var(--ink-soft)', fontSize: 13 }}>No parts logged yet.</div> : (
            <table className="data-table">
              <thead><tr><th>Part</th><th>Qty</th><th>Logged</th></tr></thead>
              <tbody>
                {wo.partsUsed.map((p, i) => (
                  <tr key={i}><td>{p.partName}</td><td>{p.qtyUsed}</td><td className="mono">{new Date(p.loggedAt).toLocaleString()}</td></tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      <div className="panel">
        <div className="panel-header">Time Logged</div>
        <div className="panel-body">
          {wo.timeLogs.length === 0 ? <div style={{ color: 'var(--ink-soft)', fontSize: 13 }}>No time logged yet.</div> : (
            <table className="data-table">
              <thead><tr><th>Technician</th><th>Minutes</th><th>Note</th><th>Logged</th></tr></thead>
              <tbody>
                {wo.timeLogs.map((t, i) => (
                  <tr key={i}><td>{t.technicianName}</td><td>{t.minutes}</td><td>{t.note || '—'}</td><td className="mono">{new Date(t.loggedAt).toLocaleString()}</td></tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      <div className="panel">
        <div className="panel-header">Status History</div>
        <div className="panel-body">
          {wo.history.map((h, i) => (
            <div className="history-item" key={i}>
              <span className="history-time">{new Date(h.changedAt).toLocaleString()}</span>
              {' — '}{h.fromStatus ? `${h.fromStatus} → ${h.toStatus}` : `Raised as ${h.toStatus}`}
              {h.changedByName ? ` by ${h.changedByName}` : ''}
              {h.note ? <div style={{ color: 'var(--ink-soft)' }}>{h.note}</div> : null}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
