import { useMemo, useState } from 'react';
import { useApi } from '../hooks/useApi';
import AttentionPanel from '../components/overview/AttentionPanel';
import { IconChevronDown } from '../components/icons';

const CAL_STATUS_CLASS = {
  HOLIDAY: 'holiday', WEEKLY_OFF: 'weekly-off', ON_LEAVE: 'on-leave', WFH_ON_DUTY: 'wfh', MISSING_ATTENDANCE: 'missing', PRESENT: '',
};
const WEEKDAY_LETTERS = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];

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

function TeamMiniList({ items, empty }) {
  if (!items.length) {
    return <div className="panel-empty">{empty}</div>;
  }
  return items.map((m) => (
    <div className="team-row" key={m.id}>
      <div className="avatar-circle">{m.avatarInitials || '?'}</div>
      <div className="team-meta">
        <div className="name">{m.name}</div>
        <div className="role">{m.jobTitle || ''}</div>
      </div>
    </div>
  ));
}

function TeamCalendar() {
  const [cursor, setCursor] = useState(() => new Date());
  const { data: calendar, loading } = useApi('/api/team/calendar?month=' + monthKey(cursor));

  const today = new Date();
  const isCurrentMonth = today.getFullYear() === cursor.getFullYear() && today.getMonth() === cursor.getMonth();
  const todayDate = isCurrentMonth ? today.getDate() : -1;
  const monthLabel = cursor.toLocaleDateString(undefined, { month: 'long', year: 'numeric' });

  function shiftMonth(delta) {
    setCursor((d) => new Date(d.getFullYear(), d.getMonth() + delta, 1));
  }

  return (
    <div className="panel" style={{ marginBottom: 16 }}>
      <div className="panel-head">
        <h2>Team calendar</h2>
        <div className="team-cal-nav">
          <button type="button" className="panel-toggle" onClick={() => shiftMonth(-1)} aria-label="Previous month">
            <span style={{ display: 'inline-flex', transform: 'rotate(90deg)' }}><IconChevronDown /></span>
          </button>
          <span className="team-cal-month">{monthLabel}</span>
          <button type="button" className="panel-toggle" onClick={() => shiftMonth(1)} aria-label="Next month">
            <span style={{ display: 'inline-flex', transform: 'rotate(-90deg)' }}><IconChevronDown /></span>
          </button>
          <span className="pill neutral">{calendar?.length || 0} people</span>
        </div>
      </div>

      {loading && <div className="panel-empty">Loading…</div>}
      {!loading && !calendar?.length && <div className="panel-empty">No direct reports to show</div>}
      {!loading && calendar?.length > 0 && (
        <>
          <div className="team-cal-scroll">
            <table className="team-cal-table">
              <thead>
                <tr>
                  <th className="team-cal-name"></th>
                  {calendar[0].days.map((d) => {
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
                {calendar.map((row) => (
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
                          <span className={'tc-day' + (cls ? ' ' + cls : '') + (dayNum === todayDate ? ' today' : '')}>{dayNum}</span>
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
      )}
    </div>
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
          <div className="stat-card"><div className="stat-label">Team size</div><div className="stat-value">{stats.teamSize}</div></div>
          <div className="stat-card"><div className="stat-label">Employees on time</div><div className="stat-value">{stats.employeesOnTime}</div></div>
          <div className="stat-card"><div className="stat-label">Late arrivals</div><div className="stat-value">{stats.lateArrivals}</div></div>
          <div className="stat-card"><div className="stat-label">WFH / on duty</div><div className="stat-value">{stats.wfhOnDuty}</div></div>
          <div className="stat-card"><div className="stat-label">Remote clock-ins</div><div className="stat-value">{stats.remoteClockIns}</div></div>
          <div className="stat-card"><div className="stat-label">Needs your attention</div><div className="stat-value">{stats.needsAttentionCount}</div></div>
        </div>
      )}

      <TeamCalendar />

      <section className="grid-2col">
        <div className="panel">
          <div className="panel-head"><h2>Team roster</h2><span className="pill neutral">{team?.length || 0} people</span></div>
          <div className="attendance-toolbar" style={{ padding: '0 18px 14px 18px', marginBottom: 0 }}>
            <input
              type="text" className="team-search-input" placeholder="Search by name…"
              value={search} onChange={(e) => setSearch(e.target.value)}
            />
            <div className="seg-control">
              {STATUS_FILTERS.map((f) => (
                <button key={f} className={'seg-btn' + (filter === f ? ' active' : '')} onClick={() => setFilter(f)}>{f}</button>
              ))}
            </div>
          </div>
          {!roster.length && <div className="panel-empty">No team members match this filter</div>}
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
        {data?.leaderboard?.map((p) => (
          <div className="team-lb-row" key={p.userId}>
            <div className="avatar-circle">{p.avatarInitials || '?'}</div>
            <div className="team-lb-meta">
              <div className="name">{p.userName}</div>
              <div className="title">{p.jobTitle || ''}</div>
            </div>
            <div className="team-lb-bar-wrap">
              <div className="lb-bar"><div className="lb-bar-fill" style={{ width: p.onTimePercent + '%' }} /></div>
            </div>
            <div className="team-lb-stat">{p.onTimePercent}% on time · {p.onTimeDays}/{p.totalDays} days</div>
          </div>
        ))}

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
          <div className="team-lb-stat" style={{ width: 'auto', textAlign: 'left' }}>employees, this range</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Min. on time / day</div>
          <div className="stat-value">{data?.minOnTimePerDay ?? 0}</div>
          <div className="team-lb-stat" style={{ width: 'auto', textAlign: 'left' }}>lowest single day</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Max. on time / day</div>
          <div className="stat-value">{data?.maxOnTimePerDay ?? 0}</div>
          <div className="team-lb-stat" style={{ width: 'auto', textAlign: 'left' }}>highest single day</div>
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
            Attendance, leave, and open requests for your direct reports — one place, so you don&apos;t have to check
            four separate pages to know how your team is doing today.
          </div>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button type="button" className="btn-mini primary">Direct Reports</button>
          <button type="button" className="btn-mini" disabled title="Coming soon">Project Team</button>
        </div>
      </div>

      <div className="attendance-toolbar">
        <div className="seg-control">
          {TABS.map((t) => (
            <button key={t} className={'seg-btn' + (tab === t ? ' active' : '')} onClick={() => setTab(t)}>{t}</button>
          ))}
        </div>
      </div>

      {tab === 'Overview' && <OverviewTab />}
      {tab === 'Efforts / Punctuality' && <PunctualityTab />}
      {stubbed.includes(tab) && (
        <div className="panel"><div className="panel-empty">{tab} is coming in a future update.</div></div>
      )}
    </section>
  );
}
