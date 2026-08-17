import { useApi } from '../hooks/useApi';

export default function PeoplePage() {
  const { data: people } = useApi('/api/people');

  return (
    <section>
      <div className="page-head">
        <h1>People</h1>
        <div className="date">{people?.length || 0} people</div>
      </div>

      <div className="people-grid">
        {!people?.length && <div className="panel-empty">No one to show yet</div>}
        {people?.map((p) => (
          <div className="people-card" key={p.id}>
            <div className="avatar-circle">{p.avatarInitials || '?'}</div>
            <div className="people-card-meta">
              <div className="name">{p.fullName}</div>
              <div className="title">{p.jobTitle || p.role.replace('_', ' ')}</div>
              <div className="dept">{p.departmentName || '—'}</div>
              <div className="contact">{p.email}{p.phone ? ` · ${p.phone}` : ''}</div>
            </div>
            <span className="pill neutral">{p.role.replace('_', ' ')}</span>
          </div>
        ))}
      </div>
    </section>
  );
}
