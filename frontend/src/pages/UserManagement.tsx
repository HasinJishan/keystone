import { FormEvent, useEffect, useState } from 'react';
import { api, ApiError } from '../api/client';
import type { Role, UserDto } from '../api/types';

export default function UserManagement() {
  const [users, setUsers] = useState<UserDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<Role>('TECHNICIAN');
  const [submitting, setSubmitting] = useState(false);

  async function load() {
    setLoading(true);
    try {
      setUsers(await api.get<UserDto[]>('/api/users'));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  async function handleCreate(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await api.post('/api/users', { name, email, password, role });
      setName(''); setEmail(''); setPassword(''); setRole('TECHNICIAN');
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not create user.');
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(id: number, userName: string) {
    if (!confirm(`Delete ${userName}? This cannot be undone.`)) return;
    setError(null);
    try {
      await api.delete(`/api/users/${id}`);
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not delete this user.');
    }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">User Management</h1>
          <div className="page-sub">Create and manage login credentials for employees — Admin only</div>
        </div>
      </div>

      {error && <div className="form-error">{error}</div>}

      <div className="panel">
        <div className="panel-header">All Users</div>
        {loading ? <div className="empty-state">Loading…</div> : (
          <table className="data-table">
            <thead><tr><th>Name</th><th>Email</th><th>Role</th><th></th></tr></thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td>{u.name}</td>
                  <td className="mono">{u.email}</td>
                  <td><span className="badge badge-slate">{u.role}</span></td>
                  <td>
                    <button className="btn btn-danger btn-sm" onClick={() => handleDelete(u.id, u.name)}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="panel">
        <div className="panel-header">Create User</div>
        <div className="panel-body">
          <form onSubmit={handleCreate}>
            <div className="field"><label>Name</label><input value={name} onChange={(e) => setName(e.target.value)} required /></div>
            <div className="field"><label>Email</label><input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required /></div>
            <div className="field"><label>Temporary password</label><input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={4} /></div>
            <div className="field">
              <label>Role</label>
              <select value={role} onChange={(e) => setRole(e.target.value as Role)}>
                <option value="DISPATCHER">Dispatcher</option>
                <option value="TECHNICIAN">Technician</option>
                <option value="MANAGER">Manager</option>
                <option value="ADMIN">Admin</option>
              </select>
            </div>
            <button className="btn btn-primary" type="submit" disabled={submitting}>
              {submitting ? 'Creating…' : 'Create User'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
