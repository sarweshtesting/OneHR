import { Fragment, useRef, useState } from 'react';
import { useApi } from '../hooks/useApi';
import { apiFetch, apiUpload } from '../api/client';
import { fmtDateRange } from '../utils/format';
import LeaveAttachments from '../components/LeaveAttachments';
import BackButton from '../components/BackButton';
import AutoTextarea from '../components/AutoTextarea';

const STATUS_CLASS = { APPROVED: 'approved', PENDING: 'pending', REJECTED: 'rejected', CANCELLED: 'rejected' };

export default function LeavePage() {
  const { data: leaveTypes } = useApi('/api/leave-types');
  const { data: balances, reload: reloadBalances } = useApi('/api/leave/balances/me');
  const { data: history, reload: reloadHistory } = useApi('/api/me/requests/history');
  const { data: calendar } = useApi('/api/leave/team-calendar');

  const [form, setForm] = useState({ leaveTypeId: '', startDate: '', endDate: '', reason: '' });
  const [pendingFiles, setPendingFiles] = useState([]);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [expandedId, setExpandedId] = useState(null);
  const fileInputRef = useRef(null);

  function setField(key, value) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  function addFiles(e) {
    const files = Array.from(e.target.files || []);
    setPendingFiles((prev) => [...prev, ...files]);
    if (fileInputRef.current) fileInputRef.current.value = '';
  }

  function removeFile(index) {
    setPendingFiles((prev) => prev.filter((_, i) => i !== index));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const created = await apiFetch('/api/leave-requests', {
        method: 'POST',
        body: JSON.stringify({
          leaveTypeId: form.leaveTypeId || leaveTypes?.[0]?.id,
          startDate: form.startDate,
          endDate: form.endDate,
          reason: form.reason,
        }),
      });
      for (const file of pendingFiles) {
        const formData = new FormData();
        formData.append('file', file);
        await apiUpload(`/api/leave-requests/${created.id}/attachments`, formData);
      }
      setForm({ leaveTypeId: '', startDate: '', endDate: '', reason: '' });
      setPendingFiles([]);
      await reloadHistory();
      await reloadBalances();
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
          <h1>Leave</h1>
        </div>
        <div className="date">Balance as of {new Date().toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}</div>
      </div>

      <div className="leave-balance-row">
        {!balances?.length && <div className="lb-card"><div className="lb-numbers"><span>No leave balances set up yet</span></div></div>}
        {balances?.map((b) => {
          const pct = b.allocatedDays > 0 ? Math.round((b.remainingDays / b.allocatedDays) * 100) : 0;
          return (
            <div className="lb-card" key={b.leaveTypeId}>
              <div className="lb-top"><span className="lb-name">{b.leaveTypeName}</span><span className="lb-dot" /></div>
              <div className="lb-bar"><div className="lb-bar-fill" style={{ width: pct + '%' }} /></div>
              <div className="lb-numbers"><b>{b.remainingDays}</b><span>of {b.allocatedDays} days</span></div>
            </div>
          );
        })}
      </div>

      <div className="leave-apply-panel">
        <h2>Apply for leave</h2>
        <div className={'banner-error' + (error ? ' show' : '')}>{error}</div>
        <form onSubmit={handleSubmit}>
          <div className="form-row">
            <div className="form-field">
              <label>Leave type</label>
              <select value={form.leaveTypeId} onChange={(e) => setField('leaveTypeId', e.target.value)}>
                {leaveTypes?.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
              </select>
            </div>
            <div className="form-field">
              <label>From</label>
              <input type="date" required value={form.startDate} onChange={(e) => setField('startDate', e.target.value)} />
            </div>
            <div className="form-field">
              <label>To</label>
              <input type="date" required value={form.endDate} onChange={(e) => setField('endDate', e.target.value)} />
            </div>
          </div>
          <div className="form-row single">
            <div className="form-field">
              <label>Reason / justification</label>
              <AutoTextarea placeholder="Brief note for your approver…" value={form.reason} onChange={(e) => setField('reason', e.target.value)} />
            </div>
          </div>
          <div className="form-row single">
            <div className="form-field">
              <label>Attachments (optional)</label>
              <label className="file-picker-label">
                📎 Attach a file
                <input ref={fileInputRef} type="file" accept=".pdf,.png,.jpg,.jpeg,.doc,.docx" onChange={addFiles} hidden />
              </label>
              {pendingFiles.length > 0 && (
                <div className="file-picker-list">
                  {pendingFiles.map((f, i) => (
                    <span className="file-picker-chip" key={f.name + i}>
                      {f.name}
                      <button type="button" onClick={() => removeFile(i)}>×</button>
                    </span>
                  ))}
                </div>
              )}
            </div>
          </div>
          <button type="submit" className="btn-submit" disabled={submitting}>{submitting ? 'Submitting…' : 'Submit request'}</button>
        </form>
      </div>

      <section className="grid-2col">
        <div className="panel">
          <div className="panel-head"><h2>My leave history</h2></div>
          <table className="data-table">
            <thead><tr><th>Type</th><th>Dates</th><th>Days</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {!history?.length && <tr><td colSpan={5} style={{ textAlign: 'center', color: 'var(--ink-faint)' }}>No requests yet</td></tr>}
              {history?.map((r, i) => (
                <Fragment key={r.id || i}>
                  <tr>
                    <td>{r.typeLabel}</td>
                    <td className="mono">{fmtDateRange(r.startDate, r.endDate)}</td>
                    <td className="mono">{r.days}</td>
                    <td><span className={'history-status ' + (STATUS_CLASS[r.status] || '')}>{r.status.charAt(0) + r.status.slice(1).toLowerCase()}</span></td>
                    <td>
                      {r.id && (
                        <button type="button" className="attachments-toggle" onClick={() => setExpandedId(expandedId === r.id ? null : r.id)}>
                          {expandedId === r.id ? 'Hide' : 'Attachments'}
                        </button>
                      )}
                    </td>
                  </tr>
                  {expandedId === r.id && (
                    <tr>
                      <td colSpan={5}><LeaveAttachments leaveRequestId={r.id} /></td>
                    </tr>
                  )}
                </Fragment>
              ))}
            </tbody>
          </table>
        </div>
        <div className="panel">
          <div className="panel-head"><h2>Team calendar</h2></div>
          <div className="leave-strip col">
            {!calendar?.length && <div className="panel-empty">No upcoming approved leave</div>}
            {calendar?.map((e, i) => (
              <div className="leave-chip row" key={i}>
                <span><div className="avatar-circle" style={{ display: 'inline-flex', verticalAlign: 'middle' }}>{e.avatarInitials || '?'}</div> <b>{e.userName}</b></span>
                <span className="when">{fmtDateRange(e.startDate, e.endDate)}</span>
              </div>
            ))}
          </div>
        </div>
      </section>
    </section>
  );
}
