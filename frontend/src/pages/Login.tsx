import { FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { ApiError } from '../api/client';

const ROLE_HOME: Record<string, string> = {
  DISPATCHER: '/board',
  MANAGER: '/dashboard',
  TECHNICIAN: '/my-jobs',
  CUSTOMER: '/portal',
};

export default function Login() {
  const { login, user } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  if (user) {
    navigate(ROLE_HOME[user.role] || '/', { replace: true });
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await login(email, password);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not sign in. Check the server is running.');
    } finally {
      setLoading(false);
    }
  }

  function fillDemo(demoEmail: string) {
    setEmail(demoEmail);
    setPassword('Passw0rd!');
  }

  return (
    <div className="login-shell">
      <div className="login-card">
        <div className="login-brand">KEYSTONE</div>
        <div className="login-sub">Sign in to the field service platform</div>

        {error && <div className="form-error">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="email">Email</label>
            <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </div>
          <div className="field">
            <label htmlFor="password">Password</label>
            <input id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </div>
          <button className="btn btn-primary" type="submit" disabled={loading} style={{ width: '100%', justifyContent: 'center' }}>
            {loading ? 'Signing in…' : 'Sign in'}
          </button>
        </form>

        <div className="demo-creds">
          Seed accounts (password: <span className="mono">Passw0rd!</span>)<br />
          <a href="#" onClick={(e) => { e.preventDefault(); fillDemo('dispatcher@keystone.dev'); }}>dispatcher@keystone.dev</a> — dispatcher<br />
          <a href="#" onClick={(e) => { e.preventDefault(); fillDemo('technician@keystone.dev'); }}>technician@keystone.dev</a> — technician<br />
          <a href="#" onClick={(e) => { e.preventDefault(); fillDemo('manager@keystone.dev'); }}>manager@keystone.dev</a> — manager<br />
          <a href="#" onClick={(e) => { e.preventDefault(); fillDemo('customer@keystone.dev'); }}>customer@keystone.dev</a> — customer
        </div>
      </div>
    </div>
  );
}
