import { useEffect, useMemo, useState } from 'react';
import { useApi } from '../hooks/useApi';
import AttentionPanel from '../components/overview/AttentionPanel';
import {
  IconChevronDown, IconCheck, IconClock, IconHome, IconClockIn,
  IconWarningTriangle, IconTeamDirectory, IconMaximize, IconMinimize,
} from '../components/icons';

const CAL_STATUS_CLASS = {
  HOLIDAY: 'holiday', WEEKLY_OFF: 'weekly-off', ON_LEAVE: 'on-leave', WFH_ON_DUTY: 'wfh', MISSING_ATTENDANCE: 'missing', PRESENT: '',
};
const WEEKDAY_LETTERS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

const TABS = ['Overview', 'Efforts / Punctuality', 'Negligence', 'Regularize & Cancel Penalties', 'Employee Assignments', 'Reports'];
const STATUS_FILTERS = ['All', 'In', 'Out', 'Not in yet', 'On leave'];

function statusDotClass(status) {
  if (status === 'In office') return 'in';
  if (status === 'Remote') return 'wfh';
  if (status === 'On break') return 'break';
  return 'out';
}

function matchesFilter(status, filter) {
  if (filter === 'All') return true;
  if (filter === 'In') return status === 'In office' || status === 'Remote' || status === 'On break';
  if (filter === 'Out') return status === 'Clocked out';
  if (filter === 'Not in yet') return status === 'Not clocked in';
  if (filter === 'On leave') return status === 'On leave';
  return true;
}

function monthKey(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
}

function toIsoDate(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function clamp(value, min, max) {
  return Math.max(min, Math.min(value, max));
}

function TeamMiniList({ items, empty }) {
  if (!items.length) {
    return <div className="panel-empty">{empty}</div>;
  }
  return (
    <div className="team-mini-scroll scroll-polished">
      {items.map((m) => (
        <div className="team-row" key={m.id}>
          <div className="avatar-circle">{m.avatarInitials || '?'}</div>
          <div className="team-meta">
            <div className="name">{m.name}</div>
            <div className="role">{m.jobTitle || ''}</div>
          </div>
        </div>
      ))}
    </div>
  );
}

function CalendarGrid({ days, rows, todayNum, tall, mode }) {
  return (
    <>
      <div className={'team-cal-scroll scroll-polished' + (tall ? ' tall' : '')}>
        <table className={'team-cal-table ' + (mode === 'month' ? 'month-mode' : 'week-mode')}>
          <thead>
            <tr>
              <th className="team-cal-name"></th>
              {days.map((d) => {
                const dow = new Date(d.date).getDay();
                return (
                  <th key={d.date} className={dow === 0 || dow === 6 ? 'tc-weekend' : ''}>
                    <div className="team-cal-dow">{WEEKDAY_LETTERS[dow]}</div>
                    {new Date(d.date).getDate()}
                  </th>
                );
              })}
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.userId}>
                <td className="team-cal-name">
                  <div className="team-cal-name-inner">
                    <div className="avatar-circle">{row.avatarInitials || '?'}</div>
                    {row.userName}
                  </div>
                </td>
                {row.days.map((d) => {
                  const dayNum = new Date(d.date).getDate();
                  const dow = new Date(d.date).getDay();
                  const cls = CAL_STATUS_CLASS[d.status] || '';
                  return (
                    <td key={d.date} className={dow === 0 || dow === 6 ? 'tc-weekend' : ''}>
                      <span className={'tc-day' + (cls ? ' ' + cls : '') + (dayNum === todayNum ? ' today' : '')}>{dayNum}</span>
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="team-cal-legend">
        <span className="team-cal-legend-item"><span className="team-cal-legend-dot holiday" />Holiday</span>
        <span className="team-cal-legend-item"><span className="team-cal-legend-dot weekly-off" />Weekly off</span>
        <span className="team-cal-legend-item"><span className="team-cal-legend-dot on-leave" />On leave</span>
        <span className="team-cal-legend-item"><span className="team-cal-legend-dot wfh" />WFH / on duty</span>
        <span className="team-cal-legend-item"><span className="team-cal-legend-dot missing" />Missing attendance</span>
      </div>
    </>
  );
}

function TeamCalendar() {
  const [selectedDate, setSelectedDate] = useState(() => new Date());
  const [viewMode, setViewMode] = useState('week');
  const [maximized, setMaximized] = useState(false);
  const { data: calendar, loading } = useApi('/api/team/calendar?month=' + monthKey(selectedDate));

  useEffect(() => {
    if (!maximized) return;
    document.body.style.overflow = 'hidden';
    function onKeyDown(e) {
      if (e.key === 'Escape') setMaximized(false);
    }
    window.addEventListener('keydown', onKeyDown);
    return () => {
      document.body.style.overflow = '';
      window.removeEventListener('keydown', onKeyDown);
    };
  }, [maximized]);

  const today = new Date();
  const isCurrentMonth = today.getFullYear() === selectedDate.getFullYear() && today.getMonth() === selectedDate.getMonth();
  const todayNum = isCurrentMonth ? today.getDate() : -1;
  const monthLabel = selectedDate.toLocaleDateString(undefined, { month: 'long', year: 'numeric' });

  const rawDays = calendar?.[0]?.days || [];
  const showFullMonth = maximized || viewMode === 'month';
  const weekOffset = clamp(Math.floor((selectedDate.getDate() - 1) / 7) * 7, 0, Math.max(0, rawDays.length - 7));
  const visibleDays = showFullMonth ? rawDays : rawDays.slice(weekOffset, weekOffset + 7);
  const visibleRows = useMemo(() => {
    if (!calendar) return [];
    return calendar.map((row) => ({ ...row, days: showFullMonth ? row.days : row.days.slice(weekOffset, weekOffset + 7) }));
  }, [calendar, showFullMonth, weekOffset]);

  function shiftMonth(delta) {
    setSelectedDate((d) => new Date(d.getFullYear(), d.getMonth() + delta, 1));
  }

  function shiftWeek(delta) {
    setSelectedDate((d) => new Date(d.getFullYear(), d.getMonth(), d.getDate() + delta * 7));
  }

  function handleDatePick(e) {
    if (!e.target.value) return;
    const [y, m, d] = e.target.value.split('-').map(Number);
    setSelectedDate(new Date(y, m - 1, d));
  }

  const toolbar = (
    <div className="team-cal-toolbar">
      <input type="date" className="team-cal-datepick" value={toIsoDate(selectedDate)} onChange={handleDatePick} aria-label="Jump to date" />
      <div className="seg-control">
        <button type="button" className={'seg-btn' + (viewMode === 'week' && !maximized ? ' active' : '')} onClick={() => setViewMode('week')} disabled={maximized}>Week</button>
        <button type="button" className={'seg-btn' + (viewMode === 'month' || maximized ? ' active' : '')} onClick={() => setViewMode('month')}>Month</button>
      </div>
      {viewMode === 'week' && !maximized && (
        <div className="team-cal-nav">
          <button type="button" className="panel-toggle" onClick={() => shiftWeek(-1)} aria-label="Previous week">
            <span style={{ display: 'inline-flex', transform: 'rotate(90deg)' }}><IconChevronDown /></span>
          </button>
          <button type="button" className="panel-toggle" onClick={() => shiftWeek(1)} aria-label="Next week">
            <span style={{ display: 'inline-flex', transform: 'rotate(-90deg)' }}><IconChevronDown /></span>
          </button>
        </div>
      )}
      <div className="team-cal-nav" style={{ marginLeft: 'auto' }}>
        <button type="button" className="panel-toggle" onClick={() => shiftMonth(-1)} aria-label="Previous month">
          <span style={{ display: 'inline-flex', transform: 'rotate(90deg)' }}><IconChevronDown /></span>
        </button>
        <span className="team-cal-month">{monthLabel}</span>
        <button type="button" className="panel-toggle" onClick={() => shiftMonth(1)} aria-label="Next month">
          <span style={{ display: 'inline-flex', transform: 'rotate(-90deg)' }}><IconChevronDown /></span>
        </button>
      </div>
    </div>
  );

  const body = (
    <>
      {loading && <div className="panel-empty">Loading…</div>}
      {!loading && !calendar?.length && <div className="panel-empty">No direct reports to show</div>}
      {!loading && calendar?.length > 0 && (
        <CalendarGrid days={visibleDays} rows={visibleRows} todayNum={todayNum} tall={maximized} mode={showFullMonth ? 'month' : 'week'} />
      )}
    </>
  );

  return (
    <>
      <div className="panel" style={{ marginBottom: 16 }}>
        <div className="panel-head">
          <h2>Team calendar</h2>
          <div className="team-cal-head-actions">
            <span className="pill neutral">{calendar?.length || 0} people</span>
            <button type="button" className="panel-toggle" onClick={() => setMaximized(true)} aria-label="Maximize calendar" title="Expand to full month">
              <IconMaximize />
            </button>
          </div>
        </div>
        {toolbar}
        {body}
      </div>

      {maximized && (
        <div className="team-cal-overlay" onClick={() => setMaximized(false)}>
          <div className="team-cal-overlay-panel" onClick={(e) => e.stopPropagation()}>
            <div className="panel-head">
              <h2>Team calendar</h2>
              <div className="team-cal-head-actions">
                <span className="pill neutral">{calendar?.length || 0} people</span>
                <button type="button" className="panel-toggle" onClick={() => setMaximized(false)} aria-label="Minimize calendar" title="Collapse">
                  <IconMinimize />
                </button>
              </div>
            </div>
            {toolbar}
            {body}
          </div>
        </div>
      )}
    </>
  );
}

function OverviewTab() {
  const { data: team } = useApi('/api/team/today-status');
  const { data: stats } = useApi('/api/team/stats');
  const { data: attention, reload: reloadAttention } = useApi('/api/team/attention');

  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState('All');

  const onLeaveToday = useMemo(() => (team || []).filter((m) => m.status === 'On leave'), [team]);
  const notInYetToday = useMemo(() => (team || []).filter((m) => m.status === 'Not clocked in'), [team]);

  const roster = useMemo(() => {
    return (team || [])
      .filter((m) => matchesFilter(m.status, filter))
      .filter((m) => !search.trim() || m.name.toLowerCase().includes(search.trim().toLowerCase()));
  }, [team, filter, search]);

  return (
    <>
      <section className="grid-2col" style={{ marginBottom: 16 }}>
        <div className="panel">
          <div className="panel-head"><h2>Who&apos;s on leave today</h2><span className="pill neutral">{onLeaveToday.length} people</span></div>
          <TeamMiniList items={onLeaveToday} empty="No one on your team is on leave today." />
        </div>
        <div className="panel">
          <div className="panel-head"><h2>Not in yet today</h2><span className="pill neutral">{notInYetToday.length} people</span></div>
          <TeamMiniList items={notInYetToday} empty="Everyone on your team has clocked in." />
        </div>
      </section>

      {stats && (
        <div className="stats-row cols-6">
          <div className="stat-card">
            <div className="stat-top"><span className="stat-label">Team size</span><div className="stat-icon"><IconTeamDirectory /></div></div>
            <div className="stat-value">{stats.teamSize}</div>
          </div>
          <div className="stat-card">
            <div className="stat-top"><span className="stat-label">Employees on time</span><div className="stat-icon"><IconCheck /></div></div>
            <div className="stat-value">{stats.employeesOnTime}</div>
          </div>
          <div className="stat-card">
            <div className="stat-top"><span className="stat-label">Late arrivals</span><div className="stat-icon accent"><IconClock /></div></div>
            <div className="stat-value">{stats.lateArrivals}</div>
          </div>
          <div className="stat-card">
            <div className="stat-top"><span className="stat-label">WFH / on duty</span><div className="stat-icon"><IconHome /></div></div>
            <div className="stat-value">{stats.wfhOnDuty}</div>
          </div>
          <div className="stat-card">
            <div className="stat-top"><span className="stat-label">Remote clock-ins</span><div className="stat-icon"><IconClockIn /></div></div>
            <div className="stat-value">{stats.remoteClockIns}</div>
          </div>
          <div className="stat-card">
            <div className="stat-top"><span className="stat-label">Needs your attention</span><div className="stat-icon accent"><IconWarningTriangle /></div></div>
            <div className="stat-value">{stats.needsAttentionCount}</div>
          </div>
        </div>
      )}

      <TeamCalendar />

      <section className="grid-2col">
        <div className="panel">
          <div className="panel-head"><h2>Team roster</h2><span className="pill neutral">{team?.length || 0} people</span></div>
          <div className="team-roster-toolbar">
            <input
              type="text" className="team-search-input" placeholder="Search by name…"
              value={search} onChange={(e) => setSearch(e.target.value)}
            />
            <div className="seg-control gapped">
              {STATUS_FILTERS.map((f) => (
                <button key={f} className={'seg-btn' + (filter === f ? ' active' : '')} onClick={() => setFilter(f)}>{f}</button>
              ))}
            </div>
          </div>
          {!roster.length && <div className="panel-empty">No team members match this filter</div>}
          {roster.length > 0 && (
            <div className="team-roster-scroll scroll-polished">
              {roster.map((m) => (
                <div className="team-row" key={m.id}>
                  <div className="avatar-circle">{m.avatarInitials || '?'}</div>
                  <div className="team-meta">
                    <div className="name">{m.name}</div>
                    <div className="role">{m.jobTitle || ''}</div>
                  </div>
                  <div className="status-text"><span className={'status-dot ' + statusDotClass(m.status)} />{m.status}</div>
                  <div className="team-time">{m.clockInTime || '—'}</div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div>
          <AttentionPanel items={attention || []} onChanged={reloadAttention} />
        </div>
      </section>
    </>
  );
}

function PunctualityTab() {
  const today = new Date();
  const defaultEnd = today.toISOString().slice(0, 10);
  const defaultStart = new Date(today.getTime() - 6 * 86400000).toISOString().slice(0, 10);
  const [start, setStart] = useState(defaultStart);
  const [end, setEnd] = useState(defaultEnd);
  const { data } = useApi(`/api/team/punctuality?start=${start}&end=${end}`);

  const maxDaily = Math.max(1, ...(data?.dailyCounts || []).map((d) => d.onTimeCount));

  return (
    <>
      <div className="panel" style={{ marginBottom: 16 }}>
        <div className="panel-head">
          <h2>On-Time Leaderboard</h2>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <input type="date" value={start} onChange={(e) => setStart(e.target.value)} />
            <span style={{ color: 'var(--ink-faint)' }}>–</span>
            <input type="date" value={end} onChange={(e) => setEnd(e.target.value)} />
          </div>
        </div>

        {!data?.leaderboard?.length && <div className="panel-empty">No attendance data for this range.</div>}
        {data?.leaderboard?.length > 0 && (
          <>
            <div className="team-lb-head-row">
              <span>Employee</span>
              <span></span>
              <span className="lb-col-pct">%</span>
              <span className="lb-col-num">On-time</span>
              <span className="lb-col-num">Days</span>
            </div>
            <div className="team-lb-scroll scroll-polished">
              {data.leaderboard.map((p) => (
                <div className="team-lb-row" key={p.userId}>
                  <div className="team-lb-meta">
                    <div className="avatar-circle">{p.avatarInitials || '?'}</div>
                    <div className="team-lb-meta-text">
                      <div className="name">{p.userName}</div>
                      <div className="title">{p.jobTitle || ''}</div>
                    </div>
                  </div>
                  <div className="team-lb-bar-wrap">
                    <div className="lb-bar"><div className="lb-bar-fill" style={{ width: p.onTimePercent + '%' }} /></div>
                  </div>
                  <div className="lb-col-pct">{p.onTimePercent}%</div>
                  <div className="lb-col-num">{p.onTimeDays}</div>
                  <div className="lb-col-num">{p.totalDays}</div>
                </div>
              ))}
            </div>
          </>
        )}

        {data?.dailyCounts?.length > 0 && (
          <div className="team-daily-chart">
            {data.dailyCounts.map((d) => (
              <div className="team-daily-bar-col" key={d.date}>
                <div className="team-daily-bar" style={{ height: `${(d.onTimeCount / maxDaily) * 100}%` }} title={`${d.onTimeCount} on time`} />
                <div className="team-daily-bar-label">{new Date(d.date).toLocaleDateString(undefined, { day: 'numeric', month: 'short' })}</div>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="stats-row cols-3">
        <div className="stat-card">
          <div className="stat-label">Avg. on time / day</div>
          <div className="stat-value">{data ? data.avgOnTimePerDay.toFixed(1) : '0.0'}</div>
          <div className="team-lb-caption">employees, this range</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Min. on time / day</div>
          <div className="stat-value">{data?.minOnTimePerDay ?? 0}</div>
          <div className="team-lb-caption">lowest single day</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Max. on time / day</div>
          <div className="stat-value">{data?.maxOnTimePerDay ?? 0}</div>
          <div className="team-lb-caption">highest single day</div>
        </div>
      </div>
    </>
  );
}

export default function MyTeamPage() {
  const [tab, setTab] = useState('Overview');
  const stubbed = ['Negligence', 'Regularize & Cancel Penalties', 'Employee Assignments', 'Reports'];

  return (
    <section>
      <div className="page-head">
        <div className="page-head-text">
          <h1>My Team</h1>
          <div className="page-desc">
            Attendance, leave, and open requests for your direct reports, all in one place.
          </div>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button type="button" className="btn-mini primary">Direct Reports</button>
          <button type="button" className="btn-mini" disabled title="Coming soon">Project Team</button>
        </div>
      </div>

      <div className="attendance-toolbar">
        <div className="seg-control full-width">
          {TABS.map((t) => (
            <button key={t} className={'seg-btn' + (tab === t ? ' active' : '')} onClick={() => setTab(t)}>{t}</button>
          ))}
        </div>
      </div>

      {tab === 'Overview' && <OverviewTab />}
      {tab === 'Efforts / Punctuality' && <PunctualityTab />}
      {stubbed.includes(tab) && (
        <div className="panel stub-panel">
          <div className="stub-panel-icon"><IconClock /></div>
          <h2>{tab}</h2>
          <p>This section is coming in a future update.</p>
        </div>
      )}
    </section>
  );
}
