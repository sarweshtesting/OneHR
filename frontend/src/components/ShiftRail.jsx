function buildSegments(attendance) {
  const today = new Date();
  const shiftStart = new Date(today); shiftStart.setHours(9, 0, 0, 0);
  const shiftEnd = new Date(today); shiftEnd.setHours(21, 0, 0, 0);
  const spanMs = shiftEnd - shiftStart;
  const clamp = (d) => Math.min(Math.max(d.getTime(), shiftStart.getTime()), shiftEnd.getTime());
  const pct = (ms) => (ms / spanMs) * 100;

  const segments = [];
  if (!attendance || !attendance.clockInAt) {
    segments.push({ cls: 'pending', flex: true });
  } else {
    const clockIn = clamp(new Date(attendance.clockInAt));
    const end = clamp(attendance.clockOutAt ? new Date(attendance.clockOutAt) : new Date());
    const leadingPct = pct(clockIn - shiftStart.getTime());
    if (leadingPct > 0) segments.push({ cls: 'pending', width: leadingPct });

    const modeCls = (attendance.mode || 'OFFICE').toLowerCase();
    let cursor = clockIn;
    (attendance.breaks || []).forEach((b) => {
      const bStart = clamp(new Date(b.startAt));
      const bEnd = clamp(b.endAt ? new Date(b.endAt) : new Date());
      if (bStart > cursor) segments.push({ cls: modeCls, width: pct(bStart - cursor) });
      if (bEnd > bStart) segments.push({ cls: 'break', width: pct(bEnd - bStart) });
      cursor = Math.max(cursor, bEnd);
    });
    if (end > cursor) segments.push({ cls: modeCls, width: pct(end - cursor) });

    if (shiftEnd.getTime() > end) segments.push({ cls: 'pending', flex: true });
  }

  let nowMarkerPct = null;
  if (!attendance || !attendance.clockOutAt) {
    const now = Date.now();
    if (now >= shiftStart.getTime() && now <= shiftEnd.getTime()) {
      nowMarkerPct = pct(now - shiftStart.getTime());
    }
  }

  return { segments, nowMarkerPct };
}

export default function ShiftRail({ attendance }) {
  const { segments, nowMarkerPct } = buildSegments(attendance);

  return (
    <div className="shift-rail-wrap">
      <div className="shift-rail-labels"><span>09:00</span><span>12:00</span><span>15:00</span><span>18:00</span><span>21:00</span></div>
      <div className="shift-rail">
        {segments.map((seg, i) => (
          <div
            key={i}
            className={'rail-seg ' + seg.cls}
            style={seg.flex ? { flex: 1 } : { width: seg.width.toFixed(2) + '%' }}
          />
        ))}
        {nowMarkerPct !== null && <div className="rail-now" style={{ left: nowMarkerPct.toFixed(2) + '%' }} />}
      </div>
      <div className="rail-legend">
        <div><span style={{ background: 'var(--red)' }} />Office</div>
        <div><span style={{ background: 'rgba(255,255,255,0.55)' }} />Work from home</div>
        <div><span style={{ background: 'rgba(255,255,255,0.28)' }} />Break</div>
        <div><span style={{ background: 'rgba(255,255,255,0.15)' }} />Remaining shift</div>
      </div>
    </div>
  );
}
