import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import type { Page, WorkOrderResponse } from '../api/types';
import { PriorityBadge, StatusBadge, SlaBadge } from '../components/Badges';

export default function MyJobs() {
  const [orders, setOrders] = useState<WorkOrderResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get<Page<WorkOrderResponse>>('/api/work-orders?size=100')
      .then((p) => setOrders(p.content))
      .finally(() => setLoading(false));
  }, []);

  const open = orders.filter((o) => !['CLOSED', 'CANCELLED'].includes(o.status));
  const done = orders.filter((o) => ['CLOSED', 'CANCELLED'].includes(o.status));

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">My Jobs</h1>
          <div className="page-sub">Work orders assigned to you</div>
        </div>
      </div>

      {loading ? (
        <div className="empty-state">Loading your jobs…</div>
      ) : open.length === 0 ? (
        <div className="panel"><div className="empty-state">No jobs assigned to you right now.</div></div>
      ) : (
        <div className="panel">
          <div className="panel-header">Open</div>
          <div className="panel-body" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {open.map((wo) => (
              <Link key={wo.id} to={`/work-orders/${wo.id}`} className="wo-card">
                <div className="wo-card-code">{wo.code}</div>
                <div className="wo-card-title">{wo.title}</div>
                <div className="wo-card-meta">
                  <PriorityBadge priority={wo.priority} />
                  <StatusBadge status={wo.status} />
                  <SlaBadge dueAt={wo.slaDueAt} breached={wo.slaBreached} />
                </div>
                <div style={{ fontSize: 12, color: 'var(--ink-soft)', marginTop: 6 }}>{wo.customerName} · {wo.siteName}</div>
              </Link>
            ))}
          </div>
        </div>
      )}

      {done.length > 0 && (
        <div className="panel">
          <div className="panel-header">Completed history</div>
          <div className="panel-body">
            <table className="data-table">
              <thead><tr><th>Code</th><th>Title</th><th>Status</th></tr></thead>
              <tbody>
                {done.map((wo) => (
                  <tr key={wo.id}>
                    <td className="mono"><Link to={`/work-orders/${wo.id}`}>{wo.code}</Link></td>
                    <td>{wo.title}</td>
                    <td><StatusBadge status={wo.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
