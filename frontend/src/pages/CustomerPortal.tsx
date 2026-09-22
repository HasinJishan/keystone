import { FormEvent, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, ApiError } from '../api/client';
import type { Page, Priority, SiteDto, WorkOrderResponse } from '../api/types';
import { PriorityBadge, StatusBadge } from '../components/Badges';
import { useAuth } from '../auth/AuthContext';

export default function CustomerPortal() {
  const { user } = useAuth();
  const [orders, setOrders] = useState<WorkOrderResponse[]>([]);
  const [sites, setSites] = useState<SiteDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<Priority>('MEDIUM');
  const [siteId, setSiteId] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function load() {
    setLoading(true);
    setLoadError(null);
    try {
      const page = await api.get<Page<WorkOrderResponse>>('/api/work-orders?size=100');
      setOrders(page.content);
    } catch (err) {
      setLoadError(err instanceof ApiError ? err.message : 'Could not load your requests. The server may be waking up - try refreshing in a moment.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    if (user?.customerId) {
      api.get<SiteDto[]>(`/api/customers/${user.customerId}/sites`).then(setSites).catch(() => {});
    }
  }, [user?.customerId]);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!siteId) { setError('Pick a site.'); return; }
    setError(null);
    setSubmitting(true);
    try {
      await api.post('/api/work-orders', { title, description, priority, customerId: user!.customerId, siteId: Number(siteId) });
      setTitle(''); setDescription(''); setPriority('MEDIUM'); setSiteId('');
      setShowForm(false);
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not submit your request.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">My Requests</h1>
          <div className="page-sub">Raise a service request and track its progress</div>
        </div>
        <button className="btn btn-primary" onClick={() => setShowForm((v) => !v)}>
          {showForm ? 'Cancel' : '+ New Request'}
        </button>
      </div>

      {showForm && (
        <div className="panel">
          <div className="panel-header">New Service Request</div>
          <div className="panel-body">
            {error && <div className="form-error">{error}</div>}
            <form onSubmit={handleSubmit}>
              <div className="field">
                <label>What's the issue?</label>
                <input value={title} onChange={(e) => setTitle(e.target.value)} required placeholder="e.g. AC not cooling in Suite 4B" />
              </div>
              <div className="field">
                <label>More detail (optional)</label>
                <textarea rows={3} value={description} onChange={(e) => setDescription(e.target.value)} />
              </div>
              <div className="field">
                <label>Site</label>
                <select value={siteId} onChange={(e) => setSiteId(e.target.value)} required>
                  <option value="">Select a site…</option>
                  {sites.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                </select>
              </div>
              <div className="field">
                <label>How urgent is this?</label>
                <select value={priority} onChange={(e) => setPriority(e.target.value as Priority)}>
                  <option value="LOW">Low — whenever convenient</option>
                  <option value="MEDIUM">Medium — this week</option>
                  <option value="HIGH">High — today</option>
                  <option value="CRITICAL">Critical — right now</option>
                </select>
              </div>
              <button className="btn btn-primary" type="submit" disabled={submitting}>
                {submitting ? 'Submitting…' : 'Submit Request'}
              </button>
            </form>
          </div>
        </div>
      )}

      <div className="panel">
        <div className="panel-header">
          <span>Your Requests</span>
          {!loading && <button className="btn btn-sm" onClick={load}>Refresh</button>}
        </div>
        {loading ? (
          <div className="empty-state">Loading…</div>
        ) : loadError ? (
          <div className="form-error" style={{ margin: 16 }}>
            {loadError}
            <div style={{ marginTop: 8 }}>
              <button className="btn btn-sm" onClick={load}>Try again</button>
            </div>
          </div>
        ) : orders.length === 0 ? (
          <div className="empty-state">You haven't raised any requests yet.</div>
        ) : (
          <table className="data-table">
            <thead><tr><th>Code</th><th>Title</th><th>Priority</th><th>Status</th><th>Site</th></tr></thead>
            <tbody>
              {orders.map((wo) => (
                <tr key={wo.id}>
                  <td className="mono"><Link to={`/work-orders/${wo.id}`}>{wo.code}</Link></td>
                  <td>{wo.title}</td>
                  <td><PriorityBadge priority={wo.priority} /></td>
                  <td><StatusBadge status={wo.status} /></td>
                  <td>{wo.siteName}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
