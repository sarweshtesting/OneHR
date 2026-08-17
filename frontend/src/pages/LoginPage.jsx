import { useState } from 'react';
import { Link, Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import LoginShowcase from '../components/LoginShowcase';

export default function LoginPage() {
  const { login, status } = useAuth();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [resetSuccess] = useState(Boolean(location.state?.resetSuccess));

  if (status === 'authenticated') return <Navigate to="/overview" replace />;

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await login(email, password);
    } catch (err) {
      setError(err.message || 'Sign in failed');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="login-screen">
      <LoginShowcase />
      <div className="login-form-side">
        <div className="login-card">
          <div className="login-brand">
            <div className="brand-mark" />
            <div className="brand-name" style={{ color: 'var(--ink)' }}>NEX<span>ORA</span></div>
          </div>
          <div className="login-title">Sign in to your workspace</div>
          {resetSuccess && <div className="banner-success show">Your password was updated — sign in with your new password.</div>}
          <div className={'banner-error' + (error ? ' show' : '')}>{error}</div>
          <form onSubmit={handleSubmit}>
            <div className="form-row single">
              <div className="form-field">
                <label>Email</label>
                <input type="email" required autoComplete="username" value={email} onChange={(e) => setEmail(e.target.value)} />
              </div>
            </div>
            <div className="form-row single" style={{ marginBottom: 6 }}>
              <div className="form-field">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <label style={{ marginBottom: 0 }}>Password</label>
                  <Link to="/forgot-password" className="login-inline-link">Forgot password?</Link>
                </div>
                <input type="password" required autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} style={{ marginTop: 6 }} />
              </div>
            </div>
            <button type="submit" className="btn-submit" style={{ width: '100%', textAlign: 'center', marginTop: 10 }} disabled={submitting}>
              {submitting ? 'Signing in…' : 'Sign in'}
            </button>
          </form>
          <div className="login-alt-link">
            Don&apos;t have a workspace? <Link to="/signup">Sign up</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
