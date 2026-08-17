import { useState } from 'react';
import { apiFetch } from '../api/client';
import { useApi } from '../hooks/useApi';
import { useAuth } from '../context/AuthContext';
import { roleLabel } from '../utils/roles';
import Modal from './Modal';

const HR_ADMIN_ROLES = ['EMPLOYEE', 'MANAGER'];
const SUPER_ADMIN_ROLES = ['EMPLOYEE', 'MANAGER', 'HR_ADMIN'];

export default function AddPersonModal({ onClose, onCreated }) {
  const { user } = useAuth();
  const { data: departments } = useApi('/api/departments');
  const assignableRoles = user?.role === 'HR_ADMIN' ? HR_ADMIN_ROLES : SUPER_ADMIN_ROLES;

  const [form, setForm] = useState({ fullName: '', email: '', role: assignableRoles[0], jobTitle: '', departmentId: '' });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [created, setCreated] = useState(null);

  function setField(key, value) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const result = await apiFetch('/api/people', {
        method: 'POST',
        body: JSON.stringify({
          fullName: form.fullName,
          email: form.email,
          role: form.role,
          jobTitle: form.jobTitle || null,
          departmentId: form.departmentId || null,
        }),
      });
      setCreated(result);
      onCreated?.();
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  if (created) {
    return (
      <Modal title="Person added" onClose={onClose}>
        <p style={{ marginBottom: 14 }}>
          <b>{created.fullName}</b> was added as {roleLabel(created.role)}. Share these sign-in details —
          the temporary password won't be shown again.
        </p>
        <div className="form-row single">
          <div className="form-field"><label>Email</label><input readOnly value={created.email} /></div>
        </div>
        <div className="form-row single">
          <div className="form-field"><label>Temporary password</label><input readOnly value={created.temporaryPassword} className="mono" /></div>
        </div>
        <button type="button" className="btn-submit" onClick={onClose}>Done</button>
      </Modal>
    );
  }

  return (
    <Modal title="Add person" onClose={onClose}>
      <div className={'banner-error' + (error ? ' show' : '')}>{error}</div>
      <form onSubmit={handleSubmit}>
        <div className="form-row single">
          <div className="form-field">
            <label>Full name</label>
            <input required value={form.fullName} onChange={(e) => setField('fullName', e.target.value)} />
          </div>
        </div>
        <div className="form-row single">
          <div className="form-field">
            <label>Email</label>
            <input type="email" required value={form.email} onChange={(e) => setField('email', e.target.value)} />
          </div>
        </div>
        <div className="form-row">
          <div className="form-field">
            <label>Role</label>
            <select value={form.role} onChange={(e) => setField('role', e.target.value)}>
              {assignableRoles.map((r) => <option key={r} value={r}>{roleLabel(r)}</option>)}
            </select>
          </div>
          <div className="form-field">
            <label>Department</label>
            <select value={form.departmentId} onChange={(e) => setField('departmentId', e.target.value)}>
              <option value="">Unassigned</option>
              {departments?.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
            </select>
          </div>
        </div>
        <div className="form-row single">
          <div className="form-field">
            <label>Job title (optional)</label>
            <input value={form.jobTitle} onChange={(e) => setField('jobTitle', e.target.value)} />
          </div>
        </div>
        <button type="submit" className="btn-submit" disabled={submitting}>{submitting ? 'Adding…' : 'Add person'}</button>
      </form>
    </Modal>
  );
}
