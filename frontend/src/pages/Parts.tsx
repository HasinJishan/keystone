import { FormEvent, useEffect, useState } from 'react';
import { api, ApiError } from '../api/client';
import type { PartDto } from '../api/types';
import { useAuth } from '../auth/AuthContext';

export default function Parts() {
  const { user } = useAuth();
  const [parts, setParts] = useState<PartDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [name, setName] = useState('');
  const [sku, setSku] = useState('');
  const [unitCost, setUnitCost] = useState('');
  const [stockQty, setStockQty] = useState('');
  const [error, setError] = useState<string | null>(null);

  async function load() {
    setLoading(true);
    setParts(await api.get<PartDto[]>('/api/parts'));
    setLoading(false);
  }

  useEffect(() => { load(); }, []);

  async function handleAdd(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await api.post('/api/parts', { name, sku, unitCost: Number(unitCost), stockQty: Number(stockQty) });
      setName(''); setSku(''); setUnitCost(''); setStockQty('');
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not add this part.');
    }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Parts Inventory</h1>
          <div className="page-sub">Stock levels for parts used on jobs</div>
        </div>
      </div>

      {error && <div className="form-error">{error}</div>}

      <div className="panel">
        <div className="panel-header">Current Stock</div>
        {loading ? <div className="empty-state">Loading…</div> : (
          <table className="data-table">
            <thead><tr><th>Name</th><th>SKU</th><th>Unit Cost</th><th>In Stock</th></tr></thead>
            <tbody>
              {parts.map((p) => (
                <tr key={p.id}>
                  <td>{p.name}</td>
                  <td className="mono">{p.sku}</td>
                  <td className="mono">${p.unitCost.toFixed(2)}</td>
                  <td className="mono">{p.stockQty}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {user?.role === 'MANAGER' && (
        <div className="panel">
          <div className="panel-header">Add a Part</div>
          <div className="panel-body">
            <form onSubmit={handleAdd} style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1fr 1fr auto', gap: 10, alignItems: 'end' }}>
              <div className="field" style={{ marginBottom: 0 }}><label>Name</label><input value={name} onChange={(e) => setName(e.target.value)} required /></div>
              <div className="field" style={{ marginBottom: 0 }}><label>SKU</label><input value={sku} onChange={(e) => setSku(e.target.value)} required /></div>
              <div className="field" style={{ marginBottom: 0 }}><label>Unit Cost</label><input type="number" step="0.01" value={unitCost} onChange={(e) => setUnitCost(e.target.value)} required /></div>
              <div className="field" style={{ marginBottom: 0 }}><label>Stock Qty</label><input type="number" value={stockQty} onChange={(e) => setStockQty(e.target.value)} required /></div>
              <button className="btn btn-primary" type="submit">Add</button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
