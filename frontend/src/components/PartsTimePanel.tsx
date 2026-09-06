import { FormEvent, useEffect, useState } from 'react';
import { api, ApiError } from '../api/client';
import type { PartDto } from '../api/types';

export default function PartsTimePanel({ workOrderId, onLogged }: { workOrderId: number; onLogged: () => void }) {
  const [parts, setParts] = useState<PartDto[]>([]);
  const [partId, setPartId] = useState('');
  const [qty, setQty] = useState('1');
  const [minutes, setMinutes] = useState('');
  const [note, setNote] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api.get<PartDto[]>('/api/parts').then(setParts).catch(() => {});
  }, []);

  async function logPart(e: FormEvent) {
    e.preventDefault();
    if (!partId) return;
    setBusy(true);
    setError(null);
    try {
      await api.post(`/api/work-orders/${workOrderId}/parts`, { partId: Number(partId), qtyUsed: Number(qty) });
      setPartId(''); setQty('1');
      onLogged();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not log parts.');
    } finally {
      setBusy(false);
    }
  }

  async function logTime(e: FormEvent) {
    e.preventDefault();
    if (!minutes) return;
    setBusy(true);
    setError(null);
    try {
      await api.post(`/api/work-orders/${workOrderId}/time`, { minutes: Number(minutes), note });
      setMinutes(''); setNote('');
      onLogged();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not log time.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="panel">
      <div className="panel-header">Log Parts &amp; Time</div>
      <div className="panel-body">
        {error && <div className="form-error">{error}</div>}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>
          <form onSubmit={logPart}>
            <div className="field">
              <label>Part used</label>
              <select value={partId} onChange={(e) => setPartId(e.target.value)}>
                <option value="">Select a part…</option>
                {parts.map((p) => (
                  <option key={p.id} value={p.id}>{p.name} ({p.stockQty} in stock)</option>
                ))}
              </select>
            </div>
            <div className="field">
              <label>Quantity</label>
              <input type="number" min={1} value={qty} onChange={(e) => setQty(e.target.value)} />
            </div>
            <button className="btn" disabled={busy || !partId} type="submit">Log Part</button>
          </form>

          <form onSubmit={logTime}>
            <div className="field">
              <label>Time spent (minutes)</label>
              <input type="number" min={1} value={minutes} onChange={(e) => setMinutes(e.target.value)} />
            </div>
            <div className="field">
              <label>Note (optional)</label>
              <input value={note} onChange={(e) => setNote(e.target.value)} />
            </div>
            <button className="btn" disabled={busy || !minutes} type="submit">Log Time</button>
          </form>
        </div>
      </div>
    </div>
  );
}
