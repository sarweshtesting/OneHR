import { useState } from 'react';
import { Link } from 'react-router-dom';
import { forgotPassword } from '../api/client';
import LoginShowcase from '../components/LoginShowcase';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [sent, setSent] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await forgotPassword(email);
      setSent(true);
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
          <div className="login-title">Reset your password</div>

          {sent ? (
            <p style={{ fontSize: 13.5, color: 'var(--ink-soft)', lineHeight: 1.6 }}>
              If an account exists for <b>{email}</b>, we've sent a link to reset your password. It expires in 30 minutes.
            </p>
          ) : (
            <>
              <div className={'banner-error' + (error ? ' show' : '')}>{error}</div>
              <form onSubmit={handleSubmit}>
                <div className="form-row single" style={{ marginBottom: 6 }}>
                  <div className="form-field">
                    <label>Email</label>
                    <input type="email" required autoComplete="username" value={email} onChange={(e) => setEmail(e.target.value)} />
                  </div>
                </div>
                <button type="submit" className="btn-submit" style={{ width: '100%', textAlign: 'center', marginTop: 10 }} disabled={submitting}>
                  {submitting ? 'Sending…' : 'Send reset link'}
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
