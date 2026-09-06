import { FormEvent, useEffect, useState } from 'react';
import type { CSSProperties } from 'react';
import { api, ApiError } from '../api/client';
import type { CustomerDto, Page, Priority, SiteDto } from '../api/types';

export default function NewWorkOrderModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const [customers, setCustomers] = useState<CustomerDto[]>([]);
  const [sites, setSites] = useState<SiteDto[]>([]);
  const [customerId, setCustomerId] = useState<number | ''>('');
  const [siteId, setSiteId] = useState<number | ''>('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<Priority>('MEDIUM');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    api.get<Page<CustomerDto>>('/api/customers?size=100').then((p) => setCustomers(p.content)).catch(() => {});
  }, []);

  useEffect(() => {
    if (!customerId) { setSites([]); return; }
    api.get<SiteDto[]>(`/api/customers/${customerId}/sites`).then(setSites).catch(() => {});
  }, [customerId]);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!customerId || !siteId) { setError('Pick a customer and a site.'); return; }
    setError(null);
    setSubmitting(true);
    try {
      await api.post('/api/work-orders', { title, description, priority, customerId, siteId });
      onCreated();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not create the work order.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div style={overlayStyle} onClick={onClose}>
      <div style={modalStyle} onClick={(e) => e.stopPropagation()}>
        <div className="panel-header">
          <span>New Work Order</span>
          <button className="btn btn-sm" onClick={onClose}>Close</button>
        </div>
        <div className="panel-body">
          {error && <div className="form-error">{error}</div>}
          <form onSubmit={handleSubmit}>
            <div className="field">
              <label>Title</label>
              <input value={title} onChange={(e) => setTitle(e.target.value)} required />
            </div>
            <div className="field">
              <label>Description</label>
              <textarea rows={3} value={description} onChange={(e) => setDescription(e.target.value)} />
            </div>
            <div className="field">
              <label>Priority</label>
              <select value={priority} onChange={(e) => setPriority(e.target.value as Priority)}>
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="CRITICAL">Critical</option>
              </select>
            </div>
            <div className="field">
              <label>Customer</label>
              <select value={customerId} onChange={(e) => setCustomerId(e.target.value ? Number(e.target.value) : '')} required>
                <option value="">Select a customer…</option>
                {customers.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </div>
            <div className="field">
              <label>Site</label>
              <select value={siteId} onChange={(e) => setSiteId(e.target.value ? Number(e.target.value) : '')} required disabled={!customerId}>
                <option value="">Select a site…</option>
                {sites.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </div>
            <button className="btn btn-primary" type="submit" disabled={submitting} style={{ width: '100%', justifyContent: 'center' }}>
              {submitting ? 'Creating…' : 'Create Work Order'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}

const overlayStyle: CSSProperties = {
  position: 'fixed', inset: 0, background: 'rgba(27,36,48,0.5)',
  display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 50,
};
const modalStyle: CSSProperties = {
  background: 'var(--surface)', borderRadius: 8, width: 420, maxWidth: '92vw', maxHeight: '88vh', overflowY: 'auto',
};
