import { useMemo, useState } from 'react';
import { useApi } from '../hooks/useApi';
import { apiFetch } from '../api/client';
import { IconWarningTriangle, IconClock, IconRegularization } from '../components/icons';

const TYPE_META = {
  LATE_ARRIVAL: { label: 'Late arrival', icon: <IconClock /> },
  MISSED_CLOCKOUT: { label: 'Missed clock-out', icon: <IconRegularization /> },
  HOURS_MISMATCH: { label: 'Hours mismatch', icon: <IconWarningTriangle /> },
};

const FILTERS = [
  { value: 'ALL', label: 'All' },
  { value: 'LATE_ARRIVAL', label: 'Late arrivals' },
  { value: 'MISSED_CLOCKOUT', label: 'Missed clock-outs' },
  { value: 'HOURS_MISMATCH', label: 'Hours mismatches' },
];

export default function ExceptionDashboardPage() {
  const { data, loading, reload } = useApi('/api/attendance/exceptions');
  const [filter, setFilter] = useState('ALL');
  const [busyId, setBusyId] = useState(null);

  const items = useMemo(() => {
    if (!data) return [];
    return filter === 'ALL' ? data : data.filter((item) => item.type === filter);
  }, [data, filter]);

  const counts = useMemo(() => {
    const tally = { LATE_ARRIVAL: 0, MISSED_CLOCKOUT: 0, HOURS_MISMATCH: 0 };
    (data || []).forEach((item) => { tally[item.type] = (tally[item.type] || 0) + 1; });
    return tally;
  }, [data]);

  async function resolve(item) {
    setBusyId(item.pseudoId);
    try {
      await apiFetch(`/api/attendance/mismatches/${encodeURIComponent(item.pseudoId)}/resolve`, { method: 'POST' });
      await reload();
    } catch (err) {
      alert(err.message);
    } finally {
      setBusyId(null);
    }
  }

  return (
    <section>
      <div className="page-head">
        <h1>Exception Dashboard</h1>
      </div>

      <div className="stats-row" style={{ marginBottom: 16 }}>
        {FILTERS.slice(1).map((f) => (
          <div className="stat-card" key={f.value}>
            <div className="stat-label">{f.label}</div>
            <div className="stat-value">{counts[f.value] || 0}</div>
          </div>
        ))}
      </div>

      <div className="attendance-toolbar">
        <div className="seg-control">
          {FILTERS.map((f) => (
            <button
              key={f.value}
              className={'seg-btn' + (filter === f.value ? ' active' : '')}
              onClick={() => setFilter(f.value)}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>

      <div className="panel">
        {loading && <div className="panel-empty">Loading…</div>}
        {!loading && !items.length && <div className="panel-empty">No exceptions — everything looks clean</div>}
        {items.map((item) => (
          <div className="attn-item" key={item.type + item.userId + item.workDate}>
            <div className={'attn-badge' + (item.type === 'HOURS_MISMATCH' ? ' accent' : '')}>
              {TYPE_META[item.type]?.icon}
            </div>
            <div className="attn-body">
              <div className="attn-title">{TYPE_META[item.type]?.label || item.type}</div>
              <div className="attn-sub">{item.description}</div>
              <div className="attn-actions">
                {item.type === 'HOURS_MISMATCH' && (
                  <button className="btn-mini primary" disabled={busyId === item.pseudoId} onClick={() => resolve(item)}>
                    Resolve
                  </button>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
