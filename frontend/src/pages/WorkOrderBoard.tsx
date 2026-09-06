import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import type { Page, WorkOrderResponse, WorkOrderStatus } from '../api/types';
import { PriorityBadge, SlaBadge } from '../components/Badges';
import NewWorkOrderModal from '../components/NewWorkOrderModal';

const COLUMNS: { status: WorkOrderStatus; label: string }[] = [
  { status: 'NEW', label: 'New' },
  { status: 'ASSIGNED', label: 'Assigned' },
  { status: 'IN_PROGRESS', label: 'In Progress' },
  { status: 'ON_HOLD', label: 'On Hold' },
  { status: 'COMPLETED', label: 'Completed' },
];

export default function WorkOrderBoard() {
  const [orders, setOrders] = useState<WorkOrderResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [showNew, setShowNew] = useState(false);

  async function load() {
    setLoading(true);
    try {
      const page = await api.get<Page<WorkOrderResponse>>(`/api/work-orders?size=100${search ? `&q=${encodeURIComponent(search)}` : ''}`);
      setOrders(page.content);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, [search]);

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Work Order Board</h1>
          <div className="page-sub">All open jobs, grouped by status</div>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <input
            placeholder="Search title or code…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{ padding: '8px 10px', border: '1px solid var(--line)', borderRadius: 5, width: 220 }}
          />
          <button className="btn btn-primary" onClick={() => setShowNew(true)}>+ New Work Order</button>
        </div>
      </div>

      {loading ? (
        <div className="empty-state">Loading work orders…</div>
      ) : (
        <div className="board">
          {COLUMNS.map((col) => {
            const items = orders.filter((o) => o.status === col.status);
            return (
              <div className="board-col" key={col.status}>
                <div className="board-col-head">
                  <span>{col.label}</span>
                  <span>{items.length}</span>
                </div>
                <div className="board-col-body">
                  {items.length === 0 && <div style={{ fontSize: 12, color: 'var(--ink-soft)', padding: '8px 4px' }}>No jobs</div>}
                  {items.map((wo) => (
                    <Link key={wo.id} to={`/work-orders/${wo.id}`} className="wo-card">
                      <div className="wo-card-code">{wo.code}</div>
                      <div className="wo-card-title">{wo.title}</div>
                      <div className="wo-card-meta">
                        <PriorityBadge priority={wo.priority} />
                        <SlaBadge dueAt={wo.slaDueAt} breached={wo.slaBreached} />
                      </div>
                      {wo.assignedToName && (
                        <div style={{ fontSize: 11.5, color: 'var(--ink-soft)', marginTop: 6 }}>{wo.assignedToName}</div>
                      )}
                    </Link>
                  ))}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {showNew && (
        <NewWorkOrderModal
          onClose={() => setShowNew(false)}
          onCreated={() => { setShowNew(false); load(); }}
        />
      )}
    </div>
  );
}
