import { useMemo, useState } from 'react';
import { useApi } from '../hooks/useApi';

const DOW = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
const pad = (n) => String(n).padStart(2, '0');
const toIso = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

function buildMonthGrid(year, month) {
  const first = new Date(year, month, 1);
  const startOffset = first.getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const cells = [];
  for (let i = 0; i < startOffset; i++) {
    const d = new Date(year, month, 1 - (startOffset - i));
    cells.push({ date: d, outside: true });
  }
  for (let day = 1; day <= daysInMonth; day++) {
    cells.push({ date: new Date(year, month, day), outside: false });
  }
  while (cells.length % 7 !== 0 || cells.length < 42) {
    const last = cells[cells.length - 1].date;
    const d = new Date(last);
    d.setDate(d.getDate() + 1);
    cells.push({ date: d, outside: true });
  }
  return cells;
}

export default function CalendarPage() {
  const [viewDate, setViewDate] = useState(() => {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), 1);
  });

  const { data: holidays } = useApi('/api/holidays');
  const { data: leave } = useApi('/api/leave/team-calendar?range=120');

  const cells = useMemo(() => buildMonthGrid(viewDate.getFullYear(), viewDate.getMonth()), [viewDate]);
  const todayIso = toIso(new Date());

  const holidaysByDate = useMemo(() => {
    const map = new Map();
    (holidays || []).forEach((h) => map.set(h.date, h));
    return map;
  }, [holidays]);

  const leaveByDate = useMemo(() => {
    const map = new Map();
    (leave || []).forEach((entry) => {
      let cursor = new Date(entry.startDate + 'T00:00:00');
      const end = new Date(entry.endDate + 'T00:00:00');
      while (cursor <= end) {
        const iso = toIso(cursor);
        if (!map.has(iso)) map.set(iso, []);
        map.get(iso).push(entry);
        cursor.setDate(cursor.getDate() + 1);
      }
    });
    return map;
  }, [leave]);

  function prevMonth() {
    setViewDate((d) => new Date(d.getFullYear(), d.getMonth() - 1, 1));
  }
  function nextMonth() {
    setViewDate((d) => new Date(d.getFullYear(), d.getMonth() + 1, 1));
  }

  const monthLabel = viewDate.toLocaleDateString(undefined, { month: 'long', year: 'numeric' });

  return (
    <section>
      <div className="page-head">
        <h1>Calendar</h1>
      </div>

      <div className="panel" style={{ padding: 20 }}>
        <div className="calendar-toolbar">
          <button className="calendar-nav-btn" onClick={prevMonth} aria-label="Previous month">‹</button>
          <h2>{monthLabel}</h2>
          <button className="calendar-nav-btn" onClick={nextMonth} aria-label="Next month">›</button>
        </div>

        <div className="calendar-grid">
          {DOW.map((d) => <div className="calendar-dow" key={d}>{d}</div>)}
          {cells.map(({ date, outside }, i) => {
            const iso = toIso(date);
            const holiday = holidaysByDate.get(iso);
            const dayLeave = leaveByDate.get(iso) || [];
            return (
              <div key={i} className={'calendar-cell' + (outside ? ' outside' : '') + (iso === todayIso ? ' today' : '')}>
                <div className="day-num">{date.getDate()}</div>
                {holiday && (
                  <div className="calendar-event holiday" title={holiday.name}>{holiday.name}</div>
                )}
                {dayLeave.slice(0, 2).map((entry) => (
                  <div
                    className="calendar-event leave"
                    key={entry.userId + entry.startDate}
                    title={`${entry.userName} on leave ${entry.startDate} – ${entry.endDate}`}
                  >
                    {entry.userName.split(' ')[0]}
                  </div>
                ))}
                {dayLeave.length > 2 && (
                  <div className="calendar-event leave" title={dayLeave.slice(2).map((e) => e.userName).join(', ')}>
                    +{dayLeave.length - 2} more
                  </div>
                )}
              </div>
            );
          })}
        </div>

        <div className="calendar-legend">
          <div><span style={{ background: 'var(--red-soft)' }} />Holiday</div>
          <div><span style={{ background: 'var(--surface-sunken)' }} />Approved leave</div>
        </div>
      </div>
    </section>
  );
}
