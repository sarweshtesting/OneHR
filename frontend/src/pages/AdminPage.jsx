import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useApi } from '../hooks/useApi';
import { apiFetch } from '../api/client';
import { roleLabel } from '../utils/roles';
import BackButton from '../components/BackButton';

const HR_ADMIN_ROLES = ['EMPLOYEE', 'MANAGER'];
const SUPER_ADMIN_ROLES = ['EMPLOYEE', 'MANAGER', 'HR_ADMIN'];

export default function AdminPage() {
  const { user } = useAuth();
  const isSuperTier = user?.role === 'SUPER_ADMIN' || user?.role === 'PLATFORM_ADMIN';
  const assignableRoles = user?.role === 'HR_ADMIN' ? HR_ADMIN_ROLES : SUPER_ADMIN_ROLES;
  const manageableRoles = new Set(assignableRoles);

  const [orgName, setOrgName] = useState(user?.orgName?.trim() || '');
  const [orgSaving, setOrgSaving] = useState(false);
  const [orgSaved, setOrgSaved] = useState(false);

  const { data: departments, reload: reloadDepartments } = useApi('/api/departments');
  const [newDept, setNewDept] = useState('');
  const [deptError, setDeptError] = useState('');
  const [deptSubmitting, setDeptSubmitting] = useState(false);

  const { data: people, reload: reloadPeople } = useApi('/api/people?includeInactive=true');
  const [peopleError, setPeopleError] = useState('');
  const [savingId, setSavingId] = useState(null);

  async function saveOrgName(e) {
    e.preventDefault();
    setOrgSaving(true);
    setOrgSaved(false);
    try {
      await apiFetch('/api/organizations/current', { method: 'PATCH', body: JSON.stringify({ name: orgName }) });
      setOrgSaved(true);
    } catch (err) {
      alert(err.message);
    } finally {
      setOrgSaving(false);
    }
  }

  async function addDepartment(e) {
    e.preventDefault();
    setDeptError('');
    setDeptSubmitting(true);
    try {
      await apiFetch('/api/departments', { method: 'POST', body: JSON.stringify({ name: newDept }) });
      setNewDept('');
      await reloadDepartments();
    } catch (err) {
      setDeptError(err.message);
    } finally {
      setDeptSubmitting(false);
    }
  }

  async function changeRole(personId, role) {
    setPeopleError('');
    setSavingId(personId);
    try {
      await apiFetch(`/api/people/${personId}`, { method: 'PATCH', body: JSON.stringify({ role }) });
      await reloadPeople();
    } catch (err) {
      setPeopleError(err.message);
    } finally {
      setSavingId(null);
    }
  }

  async function toggleActive(personId, active) {
    setPeopleError('');
    setSavingId(personId);
    try {
      await apiFetch(`/api/people/${personId}`, { method: 'PATCH', body: JSON.stringify({ active }) });
      await reloadPeople();
    } catch (err) {
      setPeopleError(err.message);
    } finally {
      setSavingId(null);
    }
  }

  const manageablePeople = (people || []).filter((p) => manageableRoles.has(p.role));
  const otherPeople = (people || []).filter((p) => !manageableRoles.has(p.role) && p.email !== user?.email);

  return (
    <section>
      <div className="page-head">
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <BackButton />
          <h1>Admin</h1>
        </div>
      </div>

      {isSuperTier && (
        <div className="panel admin-panel">
          <div className="panel-head"><h2>Organization</h2></div>
          <form className="admin-inline-form" onSubmit={saveOrgName}>
            <div className="form-field">
              <label>Organization name</label>
              <input value={orgName} onChange={(e) => { setOrgName(e.target.value); setOrgSaved(false); }} />
            </div>
            <button type="submit" className="btn-mini primary" disabled={orgSaving}>{orgSaving ? 'Saving…' : 'Save'}</button>
            {orgSaved && <span className="admin-saved-tick">Saved</span>}
          </form>
        </div>
      )}

      <div className="panel admin-panel">
        <div className="panel-head"><h2>Departments</h2></div>
        <div className="admin-dept-list">
          {departments?.map((d) => <span className="pill neutral" key={d.id}>{d.name}</span>)}
          {!departments?.length && <span style={{ color: 'var(--ink-faint)', fontSize: 12.5 }}>No departments yet</span>}
        </div>
        <div className={'banner-error' + (deptError ? ' show' : '')}>{deptError}</div>
        <form className="admin-inline-form" onSubmit={addDepartment}>
          <div className="form-field">
            <label>New department</label>
            <input required placeholder="e.g. Customer Success" value={newDept} onChange={(e) => setNewDept(e.target.value)} />
          </div>
          <button type="submit" className="btn-mini primary" disabled={deptSubmitting}>{deptSubmitting ? 'Adding…' : 'Add department'}</button>
        </form>
      </div>

      <div className="panel admin-panel">
        <div className="panel-head"><h2>Manage people</h2></div>
        <div className={'banner-error' + (peopleError ? ' show' : '')}>{peopleError}</div>
        <table className="data-table">
          <thead>
            <tr><th>Name</th><th>Role</th><th>Status</th><th></th></tr>
          </thead>
          <tbody>
            {manageablePeople.map((p) => (
              <tr key={p.id}>
                <td>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <div className="avatar-circle" style={{ width: 24, height: 24, fontSize: 10 }}>{p.avatarInitials || '?'}</div>
                    {p.fullName}
                  </div>
                </td>
                <td>
                  <select
                    value={p.role}
                    disabled={savingId === p.id}
                    onChange={(e) => changeRole(p.id, e.target.value)}
                    style={{ padding: '5px 8px', fontSize: 12.5 }}
                  >
                    {assignableRoles.map((r) => <option key={r} value={r}>{roleLabel(r)}</option>)}
                  </select>
                </td>
                <td><span className={'pill ' + (p.active ? 'neutral' : 'accent-strong')}>{p.active ? 'Active' : 'Deactivated'}</span></td>
                <td>
                  <button
                    type="button"
                    className="attachments-toggle"
                    disabled={savingId === p.id}
                    onClick={() => toggleActive(p.id, !p.active)}
                  >
                    {p.active ? 'Deactivate' : 'Reactivate'}
                  </button>
                </td>
              </tr>
            ))}
            {!manageablePeople.length && (
              <tr><td colSpan={4} style={{ textAlign: 'center', color: 'var(--ink-faint)' }}>No one you can manage yet</td></tr>
            )}
          </tbody>
        </table>
        {otherPeople.length > 0 && (
          <p className="admin-scope-note">
            {otherPeople.length} other {otherPeople.length === 1 ? 'person is' : 'people are'} outside your management scope.
          </p>
        )}
      </div>
    </section>
  );
}
