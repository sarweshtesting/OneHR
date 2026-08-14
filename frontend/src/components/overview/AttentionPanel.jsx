import { useState } from 'react';
import { apiFetch } from '../../api/client';
import { IconLeaveType, IconRegularization, IconWarningTriangle } from '../icons';

const ICONS = { LEAVE: <IconLeaveType />, REGULARIZATION: <IconRegularization />, MISMATCH: <IconWarningTriangle /> };

export default function AttentionPanel({ items, onChanged }) {
  const [busyId, setBusyId] = useState(null);

  async function act(item, action) {
    setBusyId(item.id);
    try {
      if (item.type === 'LEAVE') {
        await apiFetch(`/api/leave-requests/${item.id}/${action}`, { method: 'POST' });
      } else if (item.type === 'REGULARIZATION') {
        await apiFetch(`/api/regularizations/${item.id}/${action}`, { method: 'POST' });
      } else if (item.type === 'MISMATCH') {
        await apiFetch(`/api/attendance/mismatches/${encodeURIComponent(item.id)}/resolve`, { method: 'POST' });
      }
      onChanged();
    } catch (err) {
      alert(err.message);
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="panel" style={{ marginBottom: 16 }}>
      <div className="panel-head"><h2>Needs your attention</h2></div>
      {!items.length && <div className="panel-empty">Nothing pending — you&apos;re all caught up</div>}
      {items.map((item) => (
        <div className="attn-item" key={item.type + item.id}>
          <div className={'attn-badge' + (item.type === 'MISMATCH' ? ' accent' : '')}>{ICONS[item.type]}</div>
          <div className="attn-body">
            <div className="attn-title">{item.title}</div>
            <div className="attn-sub">{item.subtitle}</div>
            <div className="attn-actions">
              {item.type === 'MISMATCH' ? (
                <button className="btn-mini primary" disabled={busyId === item.id} onClick={() => act(item, 'resolve')}>Resolve</button>
              ) : (
                <>
                  <button className="btn-mini primary" disabled={busyId === item.id} onClick={() => act(item, 'approve')}>Approve</button>
                  <button className="btn-mini" disabled={busyId === item.id} onClick={() => act(item, 'reject')}>Decline</button>
                </>
              )}
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
