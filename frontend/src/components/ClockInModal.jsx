import { useState } from 'react';
import { useApi } from '../hooks/useApi';
import Modal from './Modal';

export default function ClockInModal({ onClose, onConfirm }) {
  const { data: clients } = useApi('/api/clients');
  const [mode, setMode] = useState('OFFICE');
  const [workingForClient, setWorkingForClient] = useState(null);
  const [clientId, setClientId] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const canSubmit = workingForClient === false || (workingForClient === true && clientId);

  async function handleSubmit(e) {
    e.preventDefault();
    if (!canSubmit) return;
    setError('');
    setSubmitting(true);
    try {
      await onConfirm(mode, workingForClient ? clientId : null);
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal title="Before you clock in" onClose={onClose}>
      <div className={'banner-error' + (error ? ' show' : '')}>{error}</div>
      <form onSubmit={handleSubmit}>
        <div className="form-row single">
          <div className="form-field">
            <label>Where are you working from today?</label>
            <div className="seg-control" style={{ width: '100%' }}>
              <button type="button" className={'seg-btn' + (mode === 'OFFICE' ? ' active' : '')} onClick={() => setMode('OFFICE')} style={{ flex: 1 }}>Office</button>
              <button type="button" className={'seg-btn' + (mode === 'WFH' ? ' active' : '')} onClick={() => setMode('WFH')} style={{ flex: 1 }}>Work from home</button>
            </div>
          </div>
        </div>

        <div className="form-row single">
          <div className="form-field">
            <label>Are you working for a client today?</label>
            <div className="seg-control" style={{ width: '100%' }}>
              <button type="button" className={'seg-btn' + (workingForClient === true ? ' active' : '')} onClick={() => setWorkingForClient(true)} style={{ flex: 1 }}>Yes</button>
              <button type="button" className={'seg-btn' + (workingForClient === false ? ' active' : '')} onClick={() => { setWorkingForClient(false); setClientId(''); }} style={{ flex: 1 }}>No</button>
            </div>
          </div>
        </div>

        {workingForClient === true && (
          <div className="form-row single">
            <div className="form-field">
              <label>Client</label>
              <select required value={clientId} onChange={(e) => setClientId(e.target.value)}>
                <option value="" disabled>Select a client…</option>
                {clients?.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
              {clients && clients.length === 0 && (
                <p style={{ fontSize: 11.5, color: 'var(--ink-faint)', marginTop: 6 }}>
                  No clients yet — add one from Client Tracking after clocking in.
                </p>
              )}
            </div>
          </div>
        )}

        <button type="submit" className="btn-submit" disabled={!canSubmit || submitting} style={{ width: '100%' }}>
          {submitting ? 'Clocking in…' : 'Clock in'}
        </button>
      </form>
    </Modal>
  );
}
