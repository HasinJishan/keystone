import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { DashboardSummary } from '../api/types';

const STATUS_ORDER = ['NEW', 'ASSIGNED', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETED', 'CLOSED', 'CANCELLED'];

export default function Dashboard() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get<DashboardSummary>('/api/reports/summary').then(setSummary).finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="empty-state">Loading dashboard…</div>;
  if (!summary) return <div className="empty-state">Could not load the dashboard.</div>;

  const maxCount = Math.max(...Object.values(summary.countsByStatus), 1);

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Dashboard</h1>
          <div className="page-sub">Live view of every work order in the system</div>
        </div>
      </div>

      <div className="stat-row">
        <div className="stat-box">
          <div className="stat-value">{summary.overdueCount}</div>
          <div className="stat-label">Overdue right now</div>
        </div>
        <div className="stat-box">
          <div className="stat-value">{summary.slaCompliancePercent}%</div>
          <div className="stat-label">SLA compliance (closed jobs)</div>
        </div>
        <div className="stat-box">
          <div className="stat-value">{Object.values(summary.countsByStatus).reduce((a, b) => a + b, 0)}</div>
          <div className="stat-label">Total work orders</div>
        </div>
      </div>

      <div className="panel">
        <div className="panel-header">Work Orders by Status</div>
        <div className="panel-body">
          {STATUS_ORDER.map((status) => {
            const count = summary.countsByStatus[status] || 0;
            return (
              <div key={status} style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 8 }}>
                <div style={{ width: 100, fontSize: 12, color: 'var(--ink-soft)' }}>{status.replace('_', ' ')}</div>
                <div style={{ flex: 1, background: 'var(--paper)', borderRadius: 4, height: 18, overflow: 'hidden' }}>
                  <div style={{ width: `${(count / maxCount) * 100}%`, background: 'var(--amber)', height: '100%' }} />
                </div>
                <div className="mono" style={{ width: 24, textAlign: 'right', fontSize: 12 }}>{count}</div>
              </div>
            );
          })}
        </div>
      </div>

      <div className="panel">
        <div className="panel-header">Load by Technician</div>
        <div className="panel-body">
          {Object.keys(summary.countByTechnician).length === 0 ? (
            <div style={{ color: 'var(--ink-soft)', fontSize: 13 }}>No jobs assigned yet.</div>
          ) : (
            <table className="data-table">
              <thead><tr><th>Technician</th><th>Assigned jobs</th></tr></thead>
              <tbody>
                {Object.entries(summary.countByTechnician).map(([name, count]) => (
                  <tr key={name}><td>{name}</td><td className="mono">{count}</td></tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}
