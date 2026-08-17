import { useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import LoginShowcase from '../components/LoginShowcase';

export default function SignupPage() {
  const { signup, status } = useAuth();
  const [orgName, setOrgName] = useState('');
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (status === 'authenticated') return <Navigate to="/overview" replace />;

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await signup(orgName, fullName, email, password);
    } catch (err) {
      setError(err.message || 'Sign up failed');
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
            <div className="brand-name" style={{ color: 'var(--ink)' }}>nForce<span>HQ</span></div>
          </div>
          <div className="login-title">Create your company workspace</div>
          <div className={'banner-error' + (error ? ' show' : '')}>{error}</div>
          <form onSubmit={handleSubmit}>
            <div className="form-row single">
              <div className="form-field">
                <label>Company name</label>
                <input type="text" required value={orgName} onChange={(e) => setOrgName(e.target.value)} placeholder="Acme Textiles" />
              </div>
            </div>
            <div className="form-row single">
              <div className="form-field">
                <label>Your full name</label>
                <input type="text" required value={fullName} onChange={(e) => setFullName(e.target.value)} placeholder="Jordan Lee" />
              </div>
            </div>
            <div className="form-row single">
              <div className="form-field">
                <label>Work email</label>
                <input type="email" required autoComplete="username" value={email} onChange={(e) => setEmail(e.target.value)} />
              </div>
            </div>
            <div className="form-row single" style={{ marginBottom: 6 }}>
              <div className="form-field">
                <label>Password</label>
                <input type="password" required minLength={8} autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} />
              </div>
            </div>
            <button type="submit" className="btn-submit" style={{ width: '100%', textAlign: 'center', marginTop: 10 }} disabled={submitting}>
              {submitting ? 'Creating workspace…' : 'Create workspace'}
            </button>
          </form>
          <div className="login-alt-link">
            Already have a workspace? <Link to="/login">Sign in</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
