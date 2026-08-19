import { useState } from 'react';
import { apiFetch } from '../api/client';
import Modal from './Modal';

const TYPE_OPTIONS = [
  { value: 'WFH', label: 'Work From Home', needsHours: false },
  { value: 'PARTIAL_DAY_LATE_ARRIVAL', label: 'Partial Day — Late Arrival', needsHours: true },
  { value: 'PARTIAL_DAY_LEAVING_EARLY', label: 'Partial Day — Leaving Early', needsHours: true },
  { value: 'OVERTIME', label: 'Overtime', needsHours: true },
];

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

export default function FlexRequestModal({ defaultType, onClose, onSubmitted }) {
  const [form, setForm] = useState({ type: defaultType || 'WFH', workDate: todayIso(), hours: '', reason: '' });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const typeMeta = TYPE_OPTIONS.find((t) => t.value === form.type);

  function setField(key, value) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await apiFetch('/api/attendance/flex-requests', {
        method: 'POST',
        body: JSON.stringify({
          type: form.type,
          workDate: form.workDate,
          hours: typeMeta.needsHours && form.hours ? Number(form.hours) : null,
          reason: form.reason,
        }),
      });
      onSubmitted?.();
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal title="New attendance request" onClose={onClose}>
      <div className={'banner-error' + (error ? ' show' : '')}>{error}</div>
      <form onSubmit={handleSubmit}>
        <div className="form-row single">
          <div className="form-field">
            <label>Request type</label>
            <select value={form.type} onChange={(e) => setField('type', e.target.value)}>
              {TYPE_OPTIONS.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
            </select>
          </div>
        </div>
        <div className="form-row">
          <div className="form-field">
            <label>Date</label>
            <input type="date" required value={form.workDate} onChange={(e) => setField('workDate', e.target.value)} />
          </div>
          {typeMeta.needsHours && (
            <div className="form-field">
              <label>Hours</label>
              <input type="number" min="0.5" max="12" step="0.5" required value={form.hours} onChange={(e) => setField('hours', e.target.value)} />
            </div>
          )}
        </div>
        <div className="form-row single">
          <div className="form-field">
            <label>Reason</label>
            <textarea required placeholder="Why are you requesting this?" value={form.reason} onChange={(e) => setField('reason', e.target.value)} />
          </div>
        </div>
        <button type="submit" className="btn-submit" disabled={submitting}>{submitting ? 'Submitting…' : 'Submit request'}</button>
      </form>
    </Modal>
  );
}
