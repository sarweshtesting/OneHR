import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useApi } from '../hooks/useApi';
import { apiFetch } from '../api/client';
import BackButton from '../components/BackButton';

const NEW_CLIENT = '__new__';

function fmtDate(dateStr) {
  return new Date(dateStr).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
}

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

export default function ClientTrackingPage() {
  const { isManager } = useAuth();
  const [view, setView] = useState('my');
  const { data: mine, reload: reloadMine } = useApi('/api/client-logs/me', { skip: view !== 'my' });
  const { data: org } = useApi('/api/client-logs', { skip: view !== 'org' });
  const { data: clients, reload: reloadClients } = useApi('/api/clients');
  const rows = view === 'my' ? mine : org;

  const [form, setForm] = useState({
    workDate: todayIso(), clientId: '', newClientName: '', newClientContact: '', newClientNotes: '', loggedHours: '',
  });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const loggingNewClient = form.clientId === NEW_CLIENT;

  function setField(key, value) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await apiFetch('/api/client-logs', {
        method: 'POST',
        body: JSON.stringify({
          workDate: form.workDate,
          loggedHours: Number(form.loggedHours),
          clientId: loggingNewClient ? null : form.clientId || null,
          newClientName: loggingNewClient ? form.newClientName : null,
          newClientContact: loggingNewClient ? form.newClientContact : null,
          newClientNotes: loggingNewClient ? form.newClientNotes : null,
        }),
      });
      setForm({ workDate: todayIso(), clientId: '', newClientName: '', newClientContact: '', newClientNotes: '', loggedHours: '' });
      await Promise.all([reloadMine(), reloadClients()]);
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section>
      <div className="page-head">
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <BackButton />
          <h1>Client Tracking</h1>
        </div>
      </div>

      <div className="leave-apply-panel">
        <h2>Log hours against a client</h2>
        <div className={'banner-error' + (error ? ' show' : '')}>{error}</div>
        <form onSubmit={handleSubmit}>
          <div className="form-row">
            <div className="form-field">
              <label>Date</label>
              <input type="date" required max={todayIso()} value={form.workDate} onChange={(e) => setField('workDate', e.target.value)} />
            </div>
            <div className="form-field">
              <label>Client</label>
              <select required value={form.clientId} onChange={(e) => setField('clientId', e.target.value)}>
                <option value="" disabled>Select a client…</option>
                {clients?.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                <option value={NEW_CLIENT}>+ Add new client…</option>
              </select>
            </div>
            <div className="form-field">
              <label>Hours</label>
              <input type="number" step="0.25" min="0" max="24" required value={form.loggedHours} onChange={(e) => setField('loggedHours', e.target.value)} />
            </div>
          </div>

          {loggingNewClient && (
            <div className="form-row new-client-row">
              <div className="form-field">
                <label>New client name</label>
                <input required placeholder="Client name" value={form.newClientName} onChange={(e) => setField('newClientName', e.target.value)} />
              </div>
              <div className="form-field">
                <label>Contact person (optional)</label>
                <input placeholder="Who to reach at this client" value={form.newClientContact} onChange={(e) => setField('newClientContact', e.target.value)} />
              </div>
              <div className="form-field">
                <label>Notes (optional)</label>
                <input placeholder="Engagement, scope, anything useful" value={form.newClientNotes} onChange={(e) => setField('newClientNotes', e.target.value)} />
              </div>
            </div>
          )}

          <button type="submit" className="btn-submit" disabled={submitting}>{submitting ? 'Logging…' : 'Log hours'}</button>
        </form>
      </div>

      {isManager && (
        <div className="attendance-toolbar">
          <div className="seg-control">
            <button className={'seg-btn' + (view === 'my' ? ' active' : '')} onClick={() => setView('my')}>My logs</button>
            <button className={'seg-btn' + (view === 'org' ? ' active' : '')} onClick={() => setView('org')}>All employees</button>
          </div>
        </div>
      )}

      <div className="panel">
        <div className="panel-head"><h2>{view === 'my' ? 'My client hours' : 'Organization client hours'}</h2></div>
        <table className="data-table">
          <thead>
            <tr>
              <th>Date</th>
              {view === 'org' && <th>Employee</th>}
              <th>Client</th><th>Hours</th>
            </tr>
          </thead>
          <tbody>
            {!rows?.length && (
              <tr><td colSpan={view === 'org' ? 4 : 3} style={{ textAlign: 'center', color: 'var(--ink-faint)' }}>No client hours logged yet</td></tr>
            )}
            {rows?.map((r) => (
              <tr key={r.id}>
                <td>{fmtDate(r.workDate)}</td>
                {view === 'org' && (
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <div className="avatar-circle" style={{ width: 22, height: 22, fontSize: 9.5 }}>{r.avatarInitials || '?'}</div>
                      {r.employeeName}
                    </div>
                  </td>
                )}
                <td>{r.clientName}</td>
                <td className="mono">{Number(r.loggedHours).toFixed(2)}h</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
