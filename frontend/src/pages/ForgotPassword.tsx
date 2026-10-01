import { FormEvent, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, ApiError } from '../api/client';
import type { MessageResponse } from '../api/types';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setMessage(null);
    setLoading(true);
    try {
      // The backend always returns the same message whether or not the email exists -
      // that's deliberate, so this form can never be used to check who has an account.
      const res = await api.post<MessageResponse>('/api/auth/forgot-password', { email });
      setMessage(res.message);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Something went wrong. Check the server is running.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="login-shell">
      <div className="login-card">
        <div className="login-brand">KEYSTONE</div>
        <div className="login-sub">Reset your password</div>

        {error && <div className="form-error">{error}</div>}

        {message ? (
          <div className="demo-creds">{message}</div>
        ) : (
          <form onSubmit={handleSubmit}>
            <div className="field">
              <label htmlFor="email">Email</label>
              <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
            </div>
            <button className="btn btn-primary" type="submit" disabled={loading} style={{ width: '100%', justifyContent: 'center' }}>
              {loading ? 'Sending…' : 'Send reset link'}
            </button>
          </form>
        )}

        <div className="demo-creds" style={{ marginTop: '16px' }}>
          <Link to="/login">Back to sign in</Link>
        </div>
      </div>
    </div>
  );
}
