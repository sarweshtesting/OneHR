import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useAttendance } from '../context/AttendanceContext';
import { useElapsedMinutes } from '../hooks/useElapsedMinutes';
import { useApi } from '../hooks/useApi';
import { useClock } from '../hooks/useClock';
import ShiftRail from '../components/ShiftRail';
import StatsRow from '../components/overview/StatsRow';
import TeamTodayPanel from '../components/overview/TeamTodayPanel';
import AttentionPanel from '../components/overview/AttentionPanel';
import { IconClockIn, IconClockOut, IconLeaveType, IconRegularization, IconPayroll, IconTeamDirectory, IconChevronDown } from '../components/icons';
import { fmtTime, fmtDuration, fmtDateRange } from '../utils/format';
import RegularizeModal from '../components/RegularizeModal';

const ANNOUNCEMENTS = [
  { tag: 'Policy update', title: 'New WFH tagging rule effective Sept 1', sub: 'HR · 2 days ago' },
  { tag: 'Holiday', title: 'Office closed Aug 15 — Independence Day', sub: 'Admin · 4 days ago' },
  { tag: 'Payroll', title: 'August payslips release on the 30th', sub: 'Finance · 5 days ago' },
  { tag: 'IT', title: 'VPN maintenance window this Saturday, 10pm–2am', sub: 'IT · 6 days ago' },
  { tag: 'Facilities', title: 'Cafeteria menu refreshed for the new quarter', sub: 'Admin · 1 week ago' },
  { tag: 'Benefits', title: 'Open enrollment for health cover closes Sept 10', sub: 'HR · 1 week ago' },
];

export default function OverviewPage() {
  const { user, isManager } = useAuth();
  const navigate = useNavigate();
  const { timeString } = useClock();
  const { attendance, clockIn, clockOut } = useAttendance();
  const elapsedMinutes = useElapsedMinutes(attendance && !attendance.clockOutAt ? attendance.clockInAt : null);
  const [announceOpen, setAnnounceOpen] = useState(true);
  const [regularizeOpen, setRegularizeOpen] = useState(false);

  const { data: team } = useApi('/api/team/today-status');
  const { data: upcomingLeave } = useApi('/api/leave/team-calendar');
  const { data: stats, reload: reloadStats } = useApi('/api/dashboard/stats', { skip: !isManager });
  const { data: approvals, reload: reloadApprovals } = useApi('/api/approvals/pending', { skip: !isManager });

  async function handleClockToggle() {
    try {
      if (!attendance || !attendance.clockInAt || attendance.clockOutAt) {
        await clockIn('OFFICE');
      } else {
        await clockOut();
      }
    } catch (err) {
      alert(err.message);
    }
  }

  function heroSub() {
    const jobTitle = user?.jobTitle ? ` · ${user.jobTitle}` : '';
    if (!attendance || !attendance.clockInAt) return `Not clocked in yet${jobTitle}`;
    if (!attendance.clockOutAt) return `Clocked in at ${fmtTime(attendance.clockInAt)} · ${attendance.mode}${jobTitle}`;
    return `Clocked out at ${fmtTime(attendance.clockOutAt)} · ${attendance.mode}${jobTitle}`;
  }

  function elapsedText() {
    if (!attendance || !attendance.clockInAt) return 'Not on the clock';
    if (!attendance.clockOutAt) return `${fmtDuration(elapsedMinutes ?? 0)} on the clock`;
    return `${fmtDuration(attendance.totalWorkedMinutes || 0)} worked today`;
  }

  // Colors follow the shift-rail legend's own naming (Office = red, WFH = white,
  // Break = dimmed red since you're paused mid-shift, Remaining/idle = faint).
  function timeColor() {
    if (!attendance || !attendance.clockInAt) return 'var(--on-black-faint)';
    if (attendance.clockOutAt) return 'var(--on-black)';
    if (attendance.onBreak) return 'rgba(216,30,39,0.6)';
    return attendance.mode === 'WFH' ? 'var(--on-black)' : 'var(--red)';
  }

  const notClockedInOrDone = !attendance || !attendance.clockInAt || attendance.clockOutAt;
  const shiftComplete = attendance && attendance.clockInAt && attendance.clockOutAt;
  const clockLabel = !attendance || !attendance.clockInAt
    ? 'Clock in'
    : !attendance.clockOutAt
      ? (attendance.onBreak ? 'On break — clock out' : 'Clock out')
      : 'Shift complete';

  async function handleApprovalsChanged() {
    await Promise.all([reloadApprovals(), reloadStats()]);
  }

  return (
    <section>
      <div className="page-head">
        <h1>Overview</h1>
        <div className="date">{new Date().toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'short', year: 'numeric' })}</div>
      </div>

      <div className="hero-row">
        <section className="hero">
          <div className="hero-top">
            <div>
              <div className="hero-eyebrow">Your shift</div>
              <div className="hero-name">{user?.name}</div>
              <div className="hero-sub">{heroSub()}</div>
            </div>
            <div className="hero-clock">
              <div className="time" style={{ color: timeColor() }}>{timeString}</div>
              <div className="elapsed" style={{ color: timeColor() }}>{elapsedText()}</div>
              <button className={'clock-btn' + (notClockedInOrDone ? ' out' : '')} disabled={shiftComplete} onClick={handleClockToggle}>
                {notClockedInOrDone ? <IconClockIn /> : <IconClockOut />}
                <span>{clockLabel}</span>
              </button>
            </div>
          </div>
          <ShiftRail attendance={attendance} />
        </section>

        <div className="panel todo-panel-hero">
          <div className="panel-head"><h2>Your to-do</h2><span className="stat-delta" style={{ margin: 0 }}>5 open</span></div>
          <div className="todo-scroll">
            <div className="todo-row"><div className="todo-check" /><div className="todo-text">Upload Q3 <b>skill certification</b> to profile</div><div className="todo-tag">Profile</div></div>
            <div className="todo-row"><div className="todo-check" /><div className="todo-text">Acknowledge <b>WFH policy</b> update</div><div className="todo-tag">Policy</div></div>
            <div className="todo-row"><div className="todo-check" /><div className="todo-text">Review <b>1:1 notes</b> before Friday sync</div><div className="todo-tag">Manager</div></div>
            <div className="todo-row"><div className="todo-check" /><div className="todo-text">Submit <b>timesheet</b> for client engagement</div><div className="todo-tag">Client</div></div>
            <div className="todo-row"><div className="todo-check" /><div className="todo-text">Confirm <b>emergency contact</b> details</div><div className="todo-tag">HR</div></div>
          </div>
        </div>
      </div>

      <div className="quick-actions">
        <button className="qa-btn" onClick={() => navigate('/leave')}>
          <div className="qa-ic accent"><IconLeaveType /></div>Apply for leave
        </button>
        <button className="qa-btn" onClick={() => setRegularizeOpen(true)}>
          <div className="qa-ic"><IconRegularization /></div>Regularize attendance
        </button>
        <button className="qa-btn" onClick={() => navigate('/payslips')}>
          <div className="qa-ic"><IconPayroll /></div>View payslip
        </button>
        <button className="qa-btn" onClick={() => navigate('/people')}>
          <div className="qa-ic"><IconTeamDirectory /></div>Team directory
        </button>
      </div>

      {regularizeOpen && <RegularizeModal onClose={() => setRegularizeOpen(false)} />}

      {isManager && stats && <StatsRow stats={stats} />}

      <section className="grid-2col">
        <div>
          <TeamTodayPanel team={team || []} onViewAttendance={() => navigate('/attendance')} />

          <div className="panel">
            <div className="panel-head"><h2>Upcoming leave</h2><a className="see-all" href="#" onClick={(e) => { e.preventDefault(); navigate('/leave'); }}>Calendar →</a></div>
            <div className="leave-strip">
              {!upcomingLeave?.length && <div className="panel-empty">No upcoming leave</div>}
              {upcomingLeave?.slice(0, 6).map((e) => (
                <div className="leave-chip" key={e.userId + e.startDate}>
                  <div className="avatar-circle">{e.avatarInitials || '?'}</div>
                  <b>{e.userName.split(' ')[0]}</b>
                  <span className="when">{fmtDateRange(e.startDate, e.endDate)}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div>
          {isManager && approvals && <AttentionPanel items={approvals} onChanged={handleApprovalsChanged} />}

          <div className="panel" style={{ marginBottom: 16 }}>
            <div className="panel-head">
              <h2>Announcements</h2>
              <button
                type="button"
                className={'panel-toggle' + (announceOpen ? '' : ' collapsed')}
                onClick={() => setAnnounceOpen((v) => !v)}
                aria-expanded={announceOpen}
                aria-label={announceOpen ? 'Collapse announcements' : 'Expand announcements'}
              >
                <IconChevronDown />
              </button>
            </div>
            {announceOpen && (
              <div className="announce-list">
                {ANNOUNCEMENTS.map((a, i) => (
                  <div className="announce-row" key={i}>
                    <span className="dot" />
                    <div className="announce-row-body">
                      <div className="tag">{a.tag}</div>
                      <div className="title">{a.title}</div>
                      <div className="sub">{a.sub}</div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </section>
    </section>
  );
}
