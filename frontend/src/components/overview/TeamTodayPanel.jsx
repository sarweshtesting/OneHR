function statusDotClass(status) {
  if (status === 'In office') return 'in';
  if (status === 'Remote') return 'wfh';
  if (status === 'On break') return 'break';
  return 'out';
}

export default function TeamTodayPanel({ team, onViewAttendance }) {
  return (
    <div className="panel" style={{ marginBottom: 16 }}>
      <div className="panel-head">
        <h2>Team today</h2>
        <a className="see-all" href="#" onClick={(e) => { e.preventDefault(); onViewAttendance(); }}>View attendance →</a>
      </div>
      {!team.length && <div className="panel-empty">No direct reports</div>}
      {team.map((m) => (
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
  );
}
