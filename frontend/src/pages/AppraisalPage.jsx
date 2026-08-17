import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useApi } from '../hooks/useApi';
import { apiFetch } from '../api/client';
import { IconChevronDown } from '../components/icons';

const STATUS_PILL = { DRAFT: 'neutral', SUBMITTED: 'accent', ACKNOWLEDGED: 'dark' };

function AppraisalCard({ appraisal, isOwn, onAcknowledge }) {
  const [open, setOpen] = useState(false);

  return (
    <div className="appraisal-card">
      <button type="button" className="appraisal-card-head" onClick={() => setOpen((o) => !o)}>
        <div className="appraisal-card-head-main">
          <span className={'appraisal-chevron' + (open ? ' open' : '')}><IconChevronDown /></span>
          <div>
            <div className="appraisal-cycle">{appraisal.cycleName}</div>
            {!isOwn && <div className="appraisal-employee">{appraisal.userName}</div>}
          </div>
        </div>
        <div className="appraisal-card-head-meta">
          {appraisal.overallRating && <span className="pill accent">{appraisal.overallRating}</span>}
          <span className={'pill ' + (STATUS_PILL[appraisal.status] || 'neutral')}>{appraisal.status}</span>
        </div>
      </button>
      {open && (
        <div className="appraisal-card-body">
          <div className="appraisal-section">
            <h4>Strengths</h4>
            <p>{appraisal.strengths || '—'}</p>
          </div>
          <div className="appraisal-section">
            <h4>Areas for improvement</h4>
            <p>{appraisal.areasForImprovement || '—'}</p>
          </div>
          <div className="appraisal-section">
            <h4>Goals for next cycle</h4>
            <p>{appraisal.goalsNextCycle || '—'}</p>
          </div>
          <div className="appraisal-section-footer">
            {appraisal.reviewerName && <span>Reviewed by {appraisal.reviewerName}</span>}
            {isOwn && appraisal.status === 'SUBMITTED' && (
              <button className="btn-mini primary" onClick={() => onAcknowledge(appraisal.id)}>Acknowledge</button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

export default function AppraisalPage() {
  const { isManager } = useAuth();
  const { data: mine, reload: reloadMine } = useApi('/api/appraisals/me');
  const { data: team, reload: reloadTeam } = useApi('/api/appraisals/team', { skip: !isManager });
  const { data: people } = useApi('/api/people', { skip: !isManager });

  const [form, setForm] = useState({ userId: '', cycleName: '', overallRating: '', strengths: '', areasForImprovement: '', goalsNextCycle: '' });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  function setField(key, value) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  async function acknowledge(id) {
    try {
      await apiFetch(`/api/appraisals/${id}/acknowledge`, { method: 'POST' });
      await reloadMine();
    } catch (err) {
      alert(err.message);
    }
  }

  async function handleCreate(e) {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await apiFetch('/api/appraisals', { method: 'POST', body: JSON.stringify(form) });
      setForm({ userId: '', cycleName: '', overallRating: '', strengths: '', areasForImprovement: '', goalsNextCycle: '' });
      await reloadTeam();
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section>
      <div className="page-head">
        <h1>Appraisal</h1>
      </div>

      {isManager && (
        <div className="panel" style={{ marginBottom: 18 }}>
          <div className="panel-head"><h2>New appraisal</h2></div>
          <div className="profile-panel-body">
            <div className={'banner-error' + (error ? ' show' : '')}>{error}</div>
            <form onSubmit={handleCreate}>
              <div className="form-row">
                <div className="form-field">
                  <label>Employee</label>
                  <select required value={form.userId} onChange={(e) => setField('userId', e.target.value)}>
                    <option value="">Select…</option>
                    {people?.map((p) => <option key={p.id} value={p.id}>{p.fullName}</option>)}
                  </select>
                </div>
                <div className="form-field">
                  <label>Cycle</label>
                  <input required placeholder="e.g. H1 2026" value={form.cycleName} onChange={(e) => setField('cycleName', e.target.value)} />
                </div>
                <div className="form-field">
                  <label>Overall rating</label>
                  <input placeholder="e.g. Exceeds Expectations" value={form.overallRating} onChange={(e) => setField('overallRating', e.target.value)} />
                </div>
              </div>
              <div className="form-row single">
                <div className="form-field">
                  <label>Strengths</label>
                  <textarea value={form.strengths} onChange={(e) => setField('strengths', e.target.value)} />
                </div>
              </div>
              <div className="form-row single">
                <div className="form-field">
                  <label>Areas for improvement</label>
                  <textarea value={form.areasForImprovement} onChange={(e) => setField('areasForImprovement', e.target.value)} />
                </div>
              </div>
              <div className="form-row single">
                <div className="form-field">
                  <label>Goals for next cycle</label>
                  <textarea value={form.goalsNextCycle} onChange={(e) => setField('goalsNextCycle', e.target.value)} />
                </div>
              </div>
              <button type="submit" className="btn-submit" disabled={submitting}>{submitting ? 'Saving…' : 'Submit appraisal'}</button>
            </form>
          </div>
        </div>
      )}

      <div className="panel" style={{ marginBottom: 18 }}>
        <div className="panel-head"><h2>My appraisals</h2></div>
        <div className="appraisal-list">
          {!mine?.length && <div className="panel-empty">No appraisal records yet</div>}
          {mine?.map((a) => <AppraisalCard key={a.id} appraisal={a} isOwn onAcknowledge={acknowledge} />)}
        </div>
      </div>

      {isManager && (
        <div className="panel">
          <div className="panel-head"><h2>Team appraisals</h2></div>
          <div className="appraisal-list">
            {!team?.length && <div className="panel-empty">No team appraisal records yet</div>}
            {team?.map((a) => <AppraisalCard key={a.id} appraisal={a} />)}
          </div>
        </div>
      )}
    </section>
  );
}
