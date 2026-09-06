import { useEffect, useState } from 'react';
import { api, ApiError } from '../api/client';
import type { UserDto } from '../api/types';

export default function AssignPanel({ workOrderId, currentAssignee, onAssigned }: {
  workOrderId: number; currentAssignee: string | null; onAssigned: () => void;
}) {
  const [technicians, setTechnicians] = useState<UserDto[]>([]);
  const [technicianId, setTechnicianId] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api.get<UserDto[]>('/api/users?role=TECHNICIAN').then(setTechnicians).catch(() => {});
  }, []);

  async function handleAssign() {
    if (!technicianId) return;
    setBusy(true);
    setError(null);
    try {
      await api.post(`/api/work-orders/${workOrderId}/assign`, { technicianId: Number(technicianId) });
      setTechnicianId('');
      onAssigned();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not assign this job.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="panel">
      <div className="panel-header">Assignment</div>
      <div className="panel-body">
        {error && <div className="form-error">{error}</div>}
        <div style={{ fontSize: 13, marginBottom: 10 }}>
          Currently: <strong>{currentAssignee || 'Unassigned'}</strong>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <select
            value={technicianId}
            onChange={(e) => setTechnicianId(e.target.value)}
            style={{ flex: 1, padding: '8px 10px', border: '1px solid var(--line)', borderRadius: 5 }}
          >
            <option value="">Select a technician…</option>
            {technicians.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
          </select>
          <button className="btn btn-primary" disabled={busy || !technicianId} onClick={handleAssign}>
            {currentAssignee ? 'Reassign' : 'Assign'}
          </button>
        </div>
      </div>
    </div>
  );
}
