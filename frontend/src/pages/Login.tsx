import { FormEvent, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { ApiError } from '../api/client';

const ROLE_HOME: Record<string, string> = {
  ADMIN: '/dashboard',
  DISPATCHER: '/board',
  MANAGER: '/dashboard',
  TECHNICIAN: '/my-jobs',
  CUSTOMER: '/portal',
};

export default function Login() {
  const { login, user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const flash = (location.state as { flash?: string } | null)?.flash;
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

  return (
    <div className="login-shell">
      <div className="login-card">
        <div className="login-brand">KEYSTONE</div>
        <div className="login-sub">Sign in to the field service platform</div>

        {flash && !error && <div className="demo-creds" style={{ marginBottom: '14px' }}>{flash}</div>}
        {error && <div className="form-error">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="email">Email</label>
            <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </div>
          <div className="field">
            <label htmlFor="password">Password</label>
            <input id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
            <div style={{ marginTop: '6px', fontSize: '13px' }}>
              <Link to="/forgot-password">Forgot password?</Link>
            </div>
          </div>
          <button className="btn btn-primary" type="submit" disabled={loading} style={{ width: '100%', justifyContent: 'center' }}>
            {loading ? 'Signing in…' : 'Sign in'}
          </button>
        </form>

        <div className="demo-creds" style={{ marginBottom: '16px' }}>
          New customer? <Link to="/register">Create an account</Link>
        </div>

      </div>
    </div>
  );
}
