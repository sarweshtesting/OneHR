import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useApi } from '../hooks/useApi';
import { apiFetchBlob } from '../api/client';
import Donut from '../components/Donut';
import Heatmap from '../components/Heatmap';
import { IconCheck, IconClock } from '../components/icons';
import BackButton from '../components/BackButton';
import { currentMonthParam, fmtTime, fmtHoursMinutes } from '../utils/format';

const MONTH_NAMES = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];

const STATUS_PILL = {
  ON_TIME: ['neutral', 'On time'],
  LATE: ['accent', 'Late'],
  IN_PROGRESS: ['dark', 'In progress'],
  ABSENT: ['accent-strong', 'Absent'],
  ON_LEAVE: ['accent-strong', 'On leave'],
};

export default function AttendancePage() {
  const { isManager } = useAuth();
  const [view, setView] = useState('my');
  const [departmentId, setDepartmentId] = useState('');
  const month = currentMonthParam();

  const { data: departments } = useApi('/api/departments', { skip: view !== 'org' });

  const qs = (extra = '') => `view=${view}&month=${month}${departmentId ? `&departmentId=${departmentId}` : ''}${extra}`;
  const { data: summary } = useApi(`/api/attendance/summary?${qs()}`);
  const { data: heatmap } = useApi(`/api/attendance/heatmap?month=${month}`);
  const { data: logs } = useApi(`/api/attendance/logs?${qs('&size=20')}`);

  const EXPORT_FORMATS = { csv: 'attendance-logs.csv', xlsx: 'attendance-logs.xlsx', pdf: 'attendance-logs.pdf' };

  async function exportAs(format) {
    try {
      const blob = await apiFetchBlob(`/api/attendance/logs/export?${qs()}&format=${format}`);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = EXPORT_FORMATS[format];
      document.body.appendChild(a); a.click(); a.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      alert(err.message);
    }
  }

  async function downloadReport(path, filename) {
    try {
      const blob = await apiFetchBlob(path);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = filename;
      document.body.appendChild(a); a.click(); a.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      alert(err.message);
    }
  }

  function downloadMonthlyReport() {
    downloadReport(`/api/reports/monthly?month=${month}`, `monthly-report-${month}.csv`);
  }

  function downloadWeeklyTimesheet() {
    const today = new Date();
    const day = today.getDay();
    const mondayOffset = day === 0 ? -6 : 1 - day;
    const monday = new Date(today);
    monday.setDate(today.getDate() + mondayOffset);
    const weekStart = monday.toISOString().slice(0, 10);
    downloadReport(`/api/reports/weekly-timesheet?weekStart=${weekStart}`, `weekly-timesheet-${weekStart}.csv`);
  }

  return (
    <section>
      <div className="page-head">
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <BackButton />
          <h1>Attendance</h1>
        </div>
        <div className="date">{MONTH_NAMES[new Date().getMonth()]} {new Date().getFullYear()}</div>
      </div>

      <div className="panel" style={{ padding: '14px 18px', marginBottom: 16, display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 10 }}>
        <div>
          <div style={{ fontWeight: 700, fontSize: 13.5 }}>My reports</div>
          <div style={{ fontSize: 12, color: 'var(--ink-faint)' }}>Your own attendance and client hours, ready to download</div>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button type="button" className="btn-mini" onClick={downloadWeeklyTimesheet}>Weekly timesheet (CSV)</button>
          <button type="button" className="btn-mini" onClick={downloadMonthlyReport}>Monthly report (CSV)</button>
        </div>
      </div>

      <div className="attendance-toolbar">
        <div className="seg-control">
          <button className={'seg-btn' + (view === 'my' ? ' active' : '')} onClick={() => setView('my')}>My view</button>
          {isManager && <button className={'seg-btn' + (view === 'team' ? ' active' : '')} onClick={() => setView('team')}>Team</button>}
          {isManager && <button className={'seg-btn' + (view === 'org' ? ' active' : '')} onClick={() => setView('org')}>Organization</button>}
        </div>
        {view === 'org' && (
          <select className="filter-select" value={departmentId} onChange={(e) => setDepartmentId(e.target.value)}>
            <option value="">All departments</option>
            {departments?.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
          </select>
        )}
      </div>

      {summary && (
        <div className="attn-summary-grid">
          <Donut officePct={summary.officePct} wfhPct={summary.wfhPct} partialPct={summary.partialPct} leavePct={summary.leavePct} />
          <div className="stat-card">
            <div className="stat-top"><span className="stat-label">On-time rate</span><div className="stat-icon"><IconCheck /></div></div>
            <div className="stat-value">{summary.onTimeRatePct}<small>%</small></div>
            <div className="stat-delta">This month</div>
          </div>
          <div className="stat-card">
            <div className="stat-top"><span className="stat-label">Overtime logged</span><div className="stat-icon accent"><IconClock /></div></div>
            <div className="stat-value">{summary.overtimeHours}<small>h</small></div>
            <div className="stat-delta">Across {summary.overtimeSessions} session{summary.overtimeSessions === 1 ? '' : 's'}</div>
          </div>
        </div>
      )}

      <div className="heatmap-panel">
        <div className="heatmap-title">Daily attendance — {MONTH_NAMES[new Date().getMonth()]}</div>
        {heatmap && <Heatmap days={heatmap} />}
        <div className="heatmap-legend">
          <div><span style={{ background: 'var(--black)' }} />Full day</div>
          <div><span style={{ background: 'var(--ink-faint)' }} />WFH</div>
          <div><span style={{ background: 'var(--red-soft-strong)' }} />Partial</div>
          <div><span style={{ background: 'var(--surface-sunken)' }} />No record</div>
        </div>
      </div>

      <div className="panel">
        <div className="panel-head">
          <h2>Recent daily logs</h2>
          <div className="export-links">
            <span>Export:</span>
            <a className="see-all" href="#" onClick={(e) => { e.preventDefault(); exportAs('csv'); }}>CSV</a>
            <a className="see-all" href="#" onClick={(e) => { e.preventDefault(); exportAs('xlsx'); }}>Excel</a>
            <a className="see-all" href="#" onClick={(e) => { e.preventDefault(); exportAs('pdf'); }}>PDF</a>
          </div>
        </div>
        <table className="data-table">
          <thead>
            <tr>
              <th>Date</th>
              {view !== 'my' && <th>Employee</th>}
              <th>Clock in</th><th>Clock out</th><th>Break</th><th>Hours</th><th>Mode</th><th>Status</th>
            </tr>
          </thead>
          <tbody>
            {!logs?.length && (
              <tr><td colSpan={view !== 'my' ? 8 : 7} style={{ textAlign: 'center', color: 'var(--ink-faint)' }}>No attendance records this month</td></tr>
            )}
            {logs?.map((r, i) => {
              const [pillCls, pillLabel] = STATUS_PILL[r.status] || ['neutral', r.status];
              return (
                <tr key={i}>
                  <td>{new Date(r.workDate).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}</td>
                  {view !== 'my' && <td>{r.employeeName}</td>}
                  <td className="mono">{r.clockInAt ? fmtTime(r.clockInAt) : '—'}</td>
                  <td className="mono">{r.clockOutAt ? fmtTime(r.clockOutAt) : '—'}</td>
                  <td className="mono">{r.totalBreakMinutes ? fmtHoursMinutes(r.totalBreakMinutes) : '—'}</td>
                  <td className="mono">{fmtHoursMinutes(r.totalWorkedMinutes)}</td>
                  <td>{r.mode === 'WFH' ? 'WFH' : 'Office'}</td>
                  <td><span className={'pill ' + pillCls}>{pillLabel}</span></td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </section>
  );
}
