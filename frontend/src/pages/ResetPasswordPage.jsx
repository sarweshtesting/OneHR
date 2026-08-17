import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { resetPassword } from '../api/client';
import LoginShowcase from '../components/LoginShowcase';

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || '';
  const navigate = useNavigate();

  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    if (password !== confirm) {
      setError('Passwords do not match');
      return;
    }
    setSubmitting(true);
    try {
      await resetPassword(token, password);
      navigate('/login', { state: { resetSuccess: true } });
    } catch (err) {
      setError(err.message || 'Something went wrong');
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
          <div className="login-title">Choose a new password</div>

          {!token ? (
            <p style={{ fontSize: 13.5, color: 'var(--ink-soft)' }}>
              This reset link is missing its token. Request a new one from the <Link to="/forgot-password">forgot password</Link> page.
            </p>
          ) : (
            <>
              <div className={'banner-error' + (error ? ' show' : '')}>{error}</div>
              <form onSubmit={handleSubmit}>
                <div className="form-row single">
                  <div className="form-field">
                    <label>New password</label>
                    <input type="password" required minLength={8} autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} />
                  </div>
                </div>
                <div className="form-row single" style={{ marginBottom: 6 }}>
                  <div className="form-field">
                    <label>Confirm password</label>
                    <input type="password" required minLength={8} autoComplete="new-password" value={confirm} onChange={(e) => setConfirm(e.target.value)} />
                  </div>
                </div>
                <button type="submit" className="btn-submit" style={{ width: '100%', textAlign: 'center', marginTop: 10 }} disabled={submitting}>
                  {submitting ? 'Saving…' : 'Save new password'}
                </button>
              </form>
            </>
          )}

          <div className="login-alt-link">
            <Link to="/login">Back to sign in</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
