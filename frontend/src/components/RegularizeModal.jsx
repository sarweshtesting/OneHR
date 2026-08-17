import { useState } from 'react';
import { apiFetch } from '../api/client';
import Modal from './Modal';

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

export default function RegularizeModal({ onClose, onSubmitted }) {
  const [form, setForm] = useState({ workDate: todayIso(), clockIn: '', clockOut: '', reason: '' });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  function setField(key, value) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  function toInstant(time) {
    if (!time) return null;
    return new Date(`${form.workDate}T${time}:00`).toISOString();
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await apiFetch('/api/attendance/regularizations', {
        method: 'POST',
        body: JSON.stringify({
          workDate: form.workDate,
          requestedClockIn: toInstant(form.clockIn),
          requestedClockOut: toInstant(form.clockOut),
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
    <Modal title="Regularize attendance" onClose={onClose}>
      <div className={'banner-error' + (error ? ' show' : '')}>{error}</div>
      <form onSubmit={handleSubmit}>
        <div className="form-row single">
          <div className="form-field">
            <label>Date</label>
            <input type="date" required max={todayIso()} value={form.workDate} onChange={(e) => setField('workDate', e.target.value)} />
          </div>
        </div>
        <div className="form-row">
          <div className="form-field">
            <label>Correct clock in</label>
            <input type="time" value={form.clockIn} onChange={(e) => setField('clockIn', e.target.value)} />
          </div>
          <div className="form-field">
            <label>Correct clock out</label>
            <input type="time" value={form.clockOut} onChange={(e) => setField('clockOut', e.target.value)} />
          </div>
        </div>
        <div className="form-row single">
          <div className="form-field">
            <label>Reason</label>
            <textarea required placeholder="Why does this day need correcting?" value={form.reason} onChange={(e) => setField('reason', e.target.value)} />
          </div>
        </div>
        <button type="submit" className="btn-submit" disabled={submitting}>{submitting ? 'Submitting…' : 'Submit request'}</button>
      </form>
    </Modal>
  );
}
