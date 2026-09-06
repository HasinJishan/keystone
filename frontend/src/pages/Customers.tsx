import { FormEvent, useEffect, useState } from 'react';
import { api, ApiError } from '../api/client';
import type { CustomerDto, Page, SiteDto } from '../api/types';

export default function Customers() {
  const [customers, setCustomers] = useState<CustomerDto[]>([]);
  const [selected, setSelected] = useState<CustomerDto | null>(null);
  const [sites, setSites] = useState<SiteDto[]>([]);
  const [loading, setLoading] = useState(true);

  const [name, setName] = useState('');
  const [contactEmail, setContactEmail] = useState('');
  const [error, setError] = useState<string | null>(null);

  const [siteName, setSiteName] = useState('');
  const [siteAddress, setSiteAddress] = useState('');

  async function loadCustomers() {
    setLoading(true);
    const page = await api.get<Page<CustomerDto>>('/api/customers?size=100');
    setCustomers(page.content);
    setLoading(false);
  }

  useEffect(() => { loadCustomers(); }, []);

  async function loadSites(customer: CustomerDto) {
    setSelected(customer);
    const s = await api.get<SiteDto[]>(`/api/customers/${customer.id}/sites`);
    setSites(s);
  }

  async function handleAddCustomer(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await api.post('/api/customers', { name, contactEmail });
      setName(''); setContactEmail('');
      await loadCustomers();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not create customer.');
    }
  }

  async function handleAddSite(e: FormEvent) {
    e.preventDefault();
    if (!selected) return;
    setError(null);
    try {
      await api.post(`/api/customers/${selected.id}/sites`, { customerId: selected.id, name: siteName, address: siteAddress });
      setSiteName(''); setSiteAddress('');
      await loadSites(selected);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not create site.');
    }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Customers &amp; Sites</h1>
          <div className="page-sub">Manage the organisations Meridian serves and their locations</div>
        </div>
      </div>

      {error && <div className="form-error">{error}</div>}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
        <div className="panel">
          <div className="panel-header">Customers</div>
          {loading ? <div className="empty-state">Loading…</div> : (
            <table className="data-table">
              <thead><tr><th>Name</th><th>Contact</th></tr></thead>
              <tbody>
                {customers.map((c) => (
                  <tr key={c.id} onClick={() => loadSites(c)} style={{ cursor: 'pointer', background: selected?.id === c.id ? 'var(--paper)' : 'transparent' }}>
                    <td>{c.name}</td><td>{c.contactEmail || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          <div className="panel-body">
            <form onSubmit={handleAddCustomer}>
              <div className="field"><label>New customer name</label><input value={name} onChange={(e) => setName(e.target.value)} required /></div>
              <div className="field"><label>Contact email</label><input type="email" value={contactEmail} onChange={(e) => setContactEmail(e.target.value)} /></div>
              <button className="btn btn-primary" type="submit">Add Customer</button>
            </form>
          </div>
        </div>

        <div className="panel">
          <div className="panel-header">{selected ? `Sites for ${selected.name}` : 'Select a customer'}</div>
          {selected && (
            <>
              <table className="data-table">
                <thead><tr><th>Name</th><th>Address</th></tr></thead>
                <tbody>
                  {sites.map((s) => <tr key={s.id}><td>{s.name}</td><td>{s.address || '—'}</td></tr>)}
                  {sites.length === 0 && <tr><td colSpan={2} style={{ color: 'var(--ink-soft)' }}>No sites yet.</td></tr>}
                </tbody>
              </table>
              <div className="panel-body">
                <form onSubmit={handleAddSite}>
                  <div className="field"><label>New site name</label><input value={siteName} onChange={(e) => setSiteName(e.target.value)} required /></div>
                  <div className="field"><label>Address</label><input value={siteAddress} onChange={(e) => setSiteAddress(e.target.value)} /></div>
                  <button className="btn btn-primary" type="submit">Add Site</button>
                </form>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
