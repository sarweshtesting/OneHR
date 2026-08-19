import { Fragment, useState } from 'react';
import { useApi } from '../hooks/useApi';
import { apiFetch } from '../api/client';
import { useAuth } from '../context/AuthContext';
import AutoTextarea from '../components/AutoTextarea';

const TYPE_LABELS = {
  HR_QUERY: 'HR query',
  DOCUMENT_REQUEST: 'Document request',
  IT_SUPPORT: 'IT support',
  PAYROLL_QUERY: 'Payroll query',
  OTHER: 'Other',
};

const STATUS_OPTIONS = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
const STATUS_CLASS = { OPEN: 'pending', IN_PROGRESS: 'pending', RESOLVED: 'approved', CLOSED: 'rejected' };

function fmtDate(iso) {
  return new Date(iso).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
}

export default function ServiceRequestsPage() {
  const { canManagePeople } = useAuth();
  const { data: myRequests, reload: reloadMine } = useApi('/api/service-requests/me');
  const { data: inbox, reload: reloadInbox } = useApi('/api/service-requests', { skip: !canManagePeople });

  const [form, setForm] = useState({ type: 'HR_QUERY', subject: '', description: '' });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [expandedId, setExpandedId] = useState(null);
  const [notesDraft, setNotesDraft] = useState('');
  const [savingId, setSavingId] = useState(null);

  function setField(key, value) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await apiFetch('/api/service-requests', { method: 'POST', body: JSON.stringify(form) });
      setForm({ type: 'HR_QUERY', subject: '', description: '' });
      await reloadMine();
      if (canManagePeople) await reloadInbox();
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  async function changeStatus(item, status) {
    setSavingId(item.id);
    try {
      await apiFetch(`/api/service-requests/${item.id}`, { method: 'PATCH', body: JSON.stringify({ status }) });
      await reloadInbox();
    } catch (err) {
      alert(err.message);
    } finally {
      setSavingId(null);
    }
  }

  async function saveNotes(item) {
    setSavingId(item.id);
    try {
      await apiFetch(`/api/service-requests/${item.id}`, { method: 'PATCH', body: JSON.stringify({ resolutionNotes: notesDraft }) });
      setExpandedId(null);
      await reloadInbox();
    } catch (err) {
      alert(err.message);
    } finally {
      setSavingId(null);
    }
  }

  function toggleExpand(item) {
    if (expandedId === item.id) {
      setExpandedId(null);
    } else {
      setExpandedId(item.id);
      setNotesDraft(item.resolutionNotes || '');
    }
  }

  return (
    <section>
      <div className="page-head">
        <h1>Service Requests</h1>
      </div>

      <div className="leave-apply-panel">
        <h2>Raise a request</h2>
        <div className={'banner-error' + (error ? ' show' : '')}>{error}</div>
        <form onSubmit={handleSubmit}>
          <div className="form-row">
            <div className="form-field">
              <label>Type</label>
              <select value={form.type} onChange={(e) => setField('type', e.target.value)}>
                {Object.entries(TYPE_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
              </select>
            </div>
            <div className="form-field" style={{ flex: 2 }}>
              <label>Subject</label>
              <input type="text" required placeholder="Short summary" value={form.subject} onChange={(e) => setField('subject', e.target.value)} />
            </div>
          </div>
          <div className="form-row single">
            <div className="form-field">
              <label>Description</label>
              <AutoTextarea placeholder="Give HR the details they'll need…" required value={form.description} onChange={(e) => setField('description', e.target.value)} />
            </div>
          </div>
          <button type="submit" className="btn-submit" disabled={submitting}>{submitting ? 'Submitting…' : 'Submit request'}</button>
        </form>
      </div>

      <div className="panel" style={{ marginBottom: 16 }}>
        <div className="panel-head"><h2>My requests</h2></div>
        <table className="data-table">
          <thead><tr><th>Subject</th><th>Type</th><th>Status</th><th>Raised</th></tr></thead>
          <tbody>
            {!myRequests?.length && <tr><td colSpan={4} style={{ textAlign: 'center', color: 'var(--ink-faint)' }}>No requests yet</td></tr>}
            {myRequests?.map((r) => (
              <tr key={r.id}>
                <td>{r.subject}</td>
                <td>{TYPE_LABELS[r.type] || r.type}</td>
                <td><span className={'history-status ' + (STATUS_CLASS[r.status] || '')}>{r.status.replace('_', ' ')}</span></td>
                <td className="mono">{fmtDate(r.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {canManagePeople && (
        <div className="panel">
          <div className="panel-head"><h2>HR inbox</h2><span className="pill neutral">{inbox?.length || 0}</span></div>
          <table className="data-table">
            <thead><tr><th>Subject</th><th>Requester</th><th>Type</th><th>Status</th><th>Raised</th><th></th></tr></thead>
            <tbody>
              {!inbox?.length && <tr><td colSpan={6} style={{ textAlign: 'center', color: 'var(--ink-faint)' }}>Nothing in the inbox</td></tr>}
              {inbox?.map((r) => (
                <Fragment key={r.id}>
                  <tr>
                    <td>{r.subject}</td>
                    <td>{r.requesterName}</td>
                    <td>{TYPE_LABELS[r.type] || r.type}</td>
                    <td>
                      <select value={r.status} disabled={savingId === r.id} onChange={(e) => changeStatus(r, e.target.value)}>
                        {STATUS_OPTIONS.map((s) => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
                      </select>
                    </td>
                    <td className="mono">{fmtDate(r.createdAt)}</td>
                    <td>
                      <button type="button" className="attachments-toggle" onClick={() => toggleExpand(r)}>
                        {expandedId === r.id ? 'Hide' : 'Details'}
                      </button>
                    </td>
                  </tr>
                  {expandedId === r.id && (
                    <tr>
                      <td colSpan={6}>
                        <div className="form-field" style={{ margin: '8px 0' }}>
                          <label>Description</label>
                          <p style={{ margin: '4px 0' }}>{r.description}</p>
                        </div>
                        <div className="form-field" style={{ margin: '8px 0' }}>
                          <label>Resolution notes</label>
                          <AutoTextarea value={notesDraft} onChange={(e) => setNotesDraft(e.target.value)} />
                        </div>
                        <button type="button" className="btn-mini primary" disabled={savingId === r.id} onClick={() => saveNotes(r)}>Save notes</button>
                      </td>
                    </tr>
                  )}
                </Fragment>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
