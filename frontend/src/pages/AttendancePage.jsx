import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useAttendance } from '../context/AttendanceContext';
import { useApi } from '../hooks/useApi';
import { apiFetch, apiFetchBlob } from '../api/client';
import Donut from '../components/Donut';
import Heatmap from '../components/Heatmap';
import RegularizeModal from '../components/RegularizeModal';
import FlexRequestModal from '../components/FlexRequestModal';
import { IconCheck, IconClock, IconClockIn, IconClockOut, IconHome, IconRegularization, IconDocument } from '../components/icons';
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

const FLEX_LABELS = {
  WFH: 'Work From Home',
  PARTIAL_DAY_LATE_ARRIVAL: 'Partial Day (Late Arrival)',
  PARTIAL_DAY_LEAVING_EARLY: 'Partial Day (Leaving Early)',
  OVERTIME: 'Overtime',
};

const REQ_STATUS_CLASS = { PENDING: 'pending', APPROVED: 'approved', REJECTED: 'rejected' };

function TodaysTimingsCard({ attendance }) {
  const clockedIn = attendance?.clockInAt && !attendance?.clockOutAt;
  const totalWorked = attendance?.totalWorkedMinutes || 0;
  const shiftMinutes = 8 * 60;
  const progressPct = Math.min(100, (totalWorked / shiftMinutes) * 100);
  const breakMinutes = attendance?.totalBreakMinutes || 0;
  const breakAllowance = 60;
  const breakPct = Math.min(100, (breakMinutes / breakAllowance) * 100);

  return (
    <div className="panel" style={{ padding: '16px 18px' }}>
      <div style={{ fontWeight: 700, fontSize: 13.5, marginBottom: 12 }}>Today&apos;s timings</div>
      <div className="tt-row"><span>Shift</span><span className="mono">09:00 – 17:00 · grace 15m</span></div>
      <div className="tt-row"><span>Status</span><span className="mono">{clockedIn ? (attendance.onBreak ? 'On break' : 'Clocked in') : (attendance?.clockOutAt ? 'Clocked out' : 'Not clocked in')}</span></div>

      <div className="tt-label-row"><span>Progress toward shift end</span><span>{fmtHoursMinutes(totalWorked)} / 8h 0m</span></div>
      <div className="lb-bar"><div className="lb-bar-fill" style={{ width: progressPct + '%' }} /></div>

      <div className="tt-label-row" style={{ marginTop: 10 }}><span>Break used</span><span>{breakMinutes} / {breakAllowance} min</span></div>
      <div className="lb-bar"><div className="lb-bar-fill" style={{ width: breakPct + '%', background: 'var(--ink-soft)' }} /></div>
    </div>
  );
}

function ActionsCard({ onPartialDay }) {
  const navigate = useNavigate();
  const { attendance, loading, requestClockIn, clockOut, clockIn } = useAttendance();
  const clockedIn = attendance?.clockInAt && !attendance?.clockOutAt;

  async function handleClockToggle() {
    try {
      if (!clockedIn) await requestClockIn(); else await clockOut();
    } catch (err) {
      alert(err.message);
    }
  }

  async function handleWfh() {
    try {
      await clockIn('WFH', null);
    } catch (err) {
      alert(err.message);
    }
  }

  return (
    <div className="panel" style={{ padding: '16px 18px' }}>
      <div style={{ fontWeight: 700, fontSize: 13.5, marginBottom: 12 }}>Actions</div>
      <div className="action-list">
        <button type="button" className="qa-btn" onClick={handleClockToggle} disabled={loading}>
          <div className="qa-ic accent">{clockedIn ? <IconClockOut /> : <IconClockIn />}</div>
          {clockedIn ? 'Check-out' : 'Check-in'}
        </button>
        <button type="button" className="qa-btn" onClick={handleWfh} disabled={loading || clockedIn}>
          <div className="qa-ic"><IconHome /></div>Work From Home
        </button>
        <button type="button" className="qa-btn" onClick={onPartialDay}>
          <div className="qa-ic"><IconRegularization /></div>Partial Day Request
        </button>
        <button type="button" className="qa-btn" onClick={() => navigate('/help')}>
          <div className="qa-ic"><IconDocument /></div>Attendance Policy
        </button>
      </div>
    </div>
  );
}

function RequestsSubTab({ isManager, myItems, pendingItems, reload, typeLabel, onNewRequest, hoursLabel, apiBase }) {
  const [busyId, setBusyId] = useState(null);

  async function act(id, action) {
    setBusyId(id);
    try {
      await apiFetch(`${apiBase}/${id}/${action}`, { method: 'POST' });
      await reload();
    } catch (err) {
      alert(err.message);
    } finally {
      setBusyId(null);
    }
  }

  return (
    <>
      {isManager && (
        <div className="panel" style={{ marginBottom: 16 }}>
          <div className="panel-head"><h2>Pending approvals — {typeLabel}</h2><span className="pill neutral">{pendingItems?.length || 0}</span></div>
          <table className="data-table">
            <thead><tr><th>Date</th><th>Type</th>{hoursLabel && <th>Hours</th>}<th>Reason</th><th>Requester</th><th>Actions</th></tr></thead>
            <tbody>
              {!pendingItems?.length && <tr><td colSpan={hoursLabel ? 6 : 5} style={{ textAlign: 'center', color: 'var(--ink-faint)' }}>Nothing pending</td></tr>}
              {pendingItems?.map((r) => (
                <tr key={r.id}>
                  <td className="mono">{r.workDate}</td>
                  <td>{FLEX_LABELS[r.type] || 'Regularization'}</td>
                  {hoursLabel && <td className="mono">{r.hours ?? '—'}</td>}
                  <td>{r.reason || '—'}</td>
                  <td>{r.userName}</td>
                  <td>
                    <button type="button" className="btn-mini primary" disabled={busyId === r.id} onClick={() => act(r.id, 'approve')} style={{ marginRight: 6 }}>Approve</button>
                    <button type="button" className="btn-mini" disabled={busyId === r.id} onClick={() => act(r.id, 'reject')}>Reject</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div className="panel">
        <div className="panel-head">
          <h2>My requests — {typeLabel}</h2>
          <button type="button" className="btn-mini primary" onClick={onNewRequest}>+ New request</button>
        </div>
        <table className="data-table">
          <thead><tr><th>Date</th><th>Type</th>{hoursLabel && <th>Hours</th>}<th>Reason</th><th>Status</th></tr></thead>
          <tbody>
            {!myItems?.length && <tr><td colSpan={hoursLabel ? 5 : 4} style={{ textAlign: 'center', color: 'var(--ink-faint)' }}>No requests yet</td></tr>}
            {myItems?.map((r) => (
              <tr key={r.id}>
                <td className="mono">{r.workDate}</td>
                <td>{FLEX_LABELS[r.type] || 'Regularization'}</td>
                {hoursLabel && <td className="mono">{r.hours ?? '—'}</td>}
                <td>{r.reason || '—'}</td>
                <td><span className={'history-status ' + (REQ_STATUS_CLASS[r.status] || '')}>{r.status}</span></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}

function RequestsTab({ isManager }) {
  const [subTab, setSubTab] = useState('Regularization');
  const [modal, setModal] = useState(null);

  const { data: myReg, reload: reloadMyReg } = useApi('/api/regularizations/me');
  const { data: pendingReg, reload: reloadPendingReg } = useApi('/api/regularizations/pending', { skip: !isManager });
  const { data: myFlex, reload: reloadMyFlex } = useApi('/api/attendance/flex-requests/me');
  const { data: pendingFlex, reload: reloadPendingFlex } = useApi('/api/attendance/flex-requests/pending', { skip: !isManager });

  const myWfhPartial = useMemo(() => (myFlex || []).filter((r) => r.type === 'WFH' || r.type.startsWith('PARTIAL_DAY')), [myFlex]);
  const pendingWfhPartial = useMemo(() => (pendingFlex || []).filter((r) => r.type === 'WFH' || r.type.startsWith('PARTIAL_DAY')), [pendingFlex]);
  const myOvertime = useMemo(() => (myFlex || []).filter((r) => r.type === 'OVERTIME'), [myFlex]);
  const pendingOvertime = useMemo(() => (pendingFlex || []).filter((r) => r.type === 'OVERTIME'), [pendingFlex]);

  async function reloadAll() {
    await Promise.all([reloadMyReg(), isManager ? reloadPendingReg() : null, reloadMyFlex(), isManager ? reloadPendingFlex() : null]);
  }

  return (
    <>
      <div className="attendance-toolbar">
        <div className="seg-control">
          <button className={'seg-btn' + (subTab === 'Regularization' ? ' active' : '')} onClick={() => setSubTab('Regularization')}>Regularization</button>
          <button className={'seg-btn' + (subTab === 'WFH & Partial Day' ? ' active' : '')} onClick={() => setSubTab('WFH & Partial Day')}>WFH &amp; Partial Day</button>
          <button className={'seg-btn' + (subTab === 'Overtime' ? ' active' : '')} onClick={() => setSubTab('Overtime')}>Overtime Requests</button>
        </div>
      </div>

      {subTab === 'Regularization' && (
        <RequestsSubTab
          isManager={isManager}
          myItems={myReg}
          pendingItems={pendingReg}
          reload={reloadAll}
          typeLabel="Regularization"
          apiBase="/api/regularizations"
          onNewRequest={() => setModal('regularize')}
        />
      )}
      {subTab === 'WFH & Partial Day' && (
        <RequestsSubTab
          isManager={isManager}
          myItems={myWfhPartial}
          pendingItems={pendingWfhPartial}
          reload={reloadAll}
          typeLabel="WFH & Partial Day"
          hoursLabel
          apiBase="/api/attendance/flex-requests"
          onNewRequest={() => setModal('wfh')}
        />
      )}
      {subTab === 'Overtime' && (
        <RequestsSubTab
          isManager={isManager}
          myItems={myOvertime}
          pendingItems={pendingOvertime}
          reload={reloadAll}
          typeLabel="Overtime"
          hoursLabel
          apiBase="/api/attendance/flex-requests"
          onNewRequest={() => setModal('overtime')}
        />
      )}

      {modal === 'regularize' && <RegularizeModal onClose={() => setModal(null)} onSubmitted={reloadAll} />}
      {modal === 'wfh' && <FlexRequestModal defaultType="WFH" onClose={() => setModal(null)} onSubmitted={reloadAll} />}
      {modal === 'overtime' && <FlexRequestModal defaultType="OVERTIME" onClose={() => setModal(null)} onSubmitted={reloadAll} />}
    </>
  );
}

export default function AttendancePage() {
  const { isManager } = useAuth();
  const [page, setPage] = useState('Overview');
  const [view, setView] = useState('my');
  const [departmentId, setDepartmentId] = useState('');
  const [partialDayOpen, setPartialDayOpen] = useState(false);
  const month = currentMonthParam();

  const { attendance } = useAttendance();
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

  const currentWeekStart = (() => {
    const today = new Date();
    const day = today.getDay();
    const monday = new Date(today);
    monday.setDate(today.getDate() + (day === 0 ? -6 : 1 - day));
    return monday.toISOString().slice(0, 10);
  })();

  function downloadMonthlyReport(format) {
    downloadReport(`/api/reports/monthly?month=${month}&format=${format}`, `monthly-report-${month}.${format}`);
  }

  function downloadWeeklyTimesheet(format) {
    downloadReport(`/api/reports/weekly-timesheet?weekStart=${currentWeekStart}&format=${format}`, `weekly-timesheet-${currentWeekStart}.${format}`);
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

      <div className="attendance-toolbar">
        <div className="seg-control">
          <button className={'seg-btn' + (page === 'Overview' ? ' active' : '')} onClick={() => setPage('Overview')}>Overview</button>
          <button className={'seg-btn' + (page === 'Requests' ? ' active' : '')} onClick={() => setPage('Requests')}>Attendance Requests</button>
        </div>
      </div>

      {page === 'Requests' && <RequestsTab isManager={isManager} />}

      {page === 'Overview' && (
        <>
          {view === 'my' && (
            <div className="grid-2col" style={{ marginBottom: 16 }}>
              <TodaysTimingsCard attendance={attendance} />
              <ActionsCard onPartialDay={() => setPartialDayOpen(true)} />
            </div>
          )}
          {partialDayOpen && <FlexRequestModal defaultType="PARTIAL_DAY_LATE_ARRIVAL" onClose={() => setPartialDayOpen(false)} />}

          <div className="panel" style={{ padding: '14px 18px', marginBottom: 16 }}>
            <div style={{ fontWeight: 700, fontSize: 13.5, marginBottom: 2 }}>My reports</div>
            <div style={{ fontSize: 12, color: 'var(--ink-faint)', marginBottom: 12 }}>Your own attendance and client hours, ready to download</div>
            <div className="my-reports-row">
              <span>Weekly timesheet</span>
              <div className="export-links">
                <span>Export:</span>
                <a className="see-all" href="#" onClick={(e) => { e.preventDefault(); downloadWeeklyTimesheet('csv'); }}>CSV</a>
                <a className="see-all" href="#" onClick={(e) => { e.preventDefault(); downloadWeeklyTimesheet('xlsx'); }}>Excel</a>
                <a className="see-all" href="#" onClick={(e) => { e.preventDefault(); downloadWeeklyTimesheet('pdf'); }}>PDF</a>
              </div>
            </div>
            <div className="my-reports-row">
              <span>Monthly report</span>
              <div className="export-links">
                <span>Export:</span>
                <a className="see-all" href="#" onClick={(e) => { e.preventDefault(); downloadMonthlyReport('csv'); }}>CSV</a>
                <a className="see-all" href="#" onClick={(e) => { e.preventDefault(); downloadMonthlyReport('xlsx'); }}>Excel</a>
                <a className="see-all" href="#" onClick={(e) => { e.preventDefault(); downloadMonthlyReport('pdf'); }}>PDF</a>
              </div>
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
        </>
      )}
    </section>
  );
}
