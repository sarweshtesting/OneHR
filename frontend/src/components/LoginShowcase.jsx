import { useMemo } from 'react';
import { useClock } from '../hooks/useClock';
import { useElapsedMinutes } from '../hooks/useElapsedMinutes';
import ShiftRail from './ShiftRail';
import { IconClock, IconCalendar, IconCheck } from './icons';
import { fmtDuration } from '../utils/format';

/**
 * The login screen's visual is a live instance of the app's own shift-rail widget —
 * driven by the visitor's real clock, not a screenshot or illustration. Every other
 * HR login page we looked at (greytHR, Zoho) shows either marketing stills or nothing
 * at all; this one is honestly labeled as a demo but is a genuinely running piece of
 * the product, ticking before you've even signed in.
 */
export default function LoginShowcase() {
  const { timeString } = useClock();

  const demoClockIn = useMemo(() => {
    const d = new Date();
    d.setHours(9, 2, 0, 0);
    return d.toISOString();
  }, []);

  const elapsedMinutes = useElapsedMinutes(demoClockIn);
  const demoAttendance = { clockInAt: demoClockIn, clockOutAt: null, mode: 'OFFICE', breaks: [] };

  return (
    <div className="login-showcase">
      <div className="login-showcase-eyebrow">Live preview — not a screenshot</div>
      <h1 className="login-showcase-title">This is nForceHQ, running right now.</h1>
      <p className="login-showcase-sub">The shift tracker below updates against your actual clock — the same widget every signed-in employee sees on their Overview page.</p>

      <div className="login-showcase-clock">
        <div className="time">{timeString}</div>
        <div className="elapsed">{fmtDuration(elapsedMinutes ?? 0)} into a sample shift</div>
      </div>

      <ShiftRail attendance={demoAttendance} />

      <div className="login-showcase-features">
        <div><div className="lf-ic"><IconClock /></div><span>Real-time attendance &amp; breaks</span></div>
        <div><div className="lf-ic"><IconCalendar /></div><span>Leave balances &amp; approvals</span></div>
        <div><div className="lf-ic"><IconCheck /></div><span>Role-aware, multi-tenant by design</span></div>
      </div>
    </div>
  );
}
