import { IconCheck, IconClock, IconWarningTriangle, IconTrendUp } from '../icons';

export default function StatsRow({ stats }) {
  return (
    <section className="stats-row">
      <div className="stat-card">
        <div className="stat-top"><span className="stat-label">Present today</span><div className="stat-icon"><IconCheck /></div></div>
        <div className="stat-value">{stats.presentToday} <small>/ {stats.totalHeadcount}</small></div>
        <div className="stat-delta">&nbsp;</div>
      </div>
      <div className="stat-card">
        <div className="stat-top"><span className="stat-label">Pending approvals</span><div className="stat-icon accent"><IconClock /></div></div>
        <div className="stat-value">{stats.pendingLeaveCount + stats.pendingRegularizationCount}</div>
        <div className="stat-delta">{stats.pendingLeaveCount} leave · {stats.pendingRegularizationCount} regularizations</div>
      </div>
      <div className="stat-card">
        <div className="stat-top"><span className="stat-label">Mismatches flagged</span><div className="stat-icon accent"><IconWarningTriangle /></div></div>
        <div className="stat-value">{stats.mismatchesFlagged}</div>
        <div className="stat-delta">&nbsp;</div>
      </div>
      <div className="stat-card">
        <div className="stat-top"><span className="stat-label">Avg hours this week</span><div className="stat-icon"><IconTrendUp /></div></div>
        <div className="stat-value">{stats.avgHoursThisWeek}<small>h</small></div>
        <div className="stat-delta">&nbsp;</div>
      </div>
    </section>
  );
}
