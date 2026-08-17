import { useMemo, useState } from 'react';
import { useApi } from '../hooks/useApi';
import { useAuth } from '../context/AuthContext';
import { roleLabel } from '../utils/roles';
import { IconChevronDown } from '../components/icons';
import AddPersonModal from '../components/AddPersonModal';

function groupByDepartment(people) {
  const groups = new Map();
  people.forEach((p) => {
    const key = p.departmentName || 'Unassigned';
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key).push(p);
  });
  return [...groups.entries()].sort(([a], [b]) => a.localeCompare(b));
}

export default function PeoplePage() {
  const { canManagePeople } = useAuth();
  const { data: people, reload } = useApi('/api/people');
  const [collapsed, setCollapsed] = useState(() => new Set());
  const [addOpen, setAddOpen] = useState(false);

  const groups = useMemo(() => (people ? groupByDepartment(people) : []), [people]);

  function toggle(dept) {
    setCollapsed((prev) => {
      const next = new Set(prev);
      if (next.has(dept)) next.delete(dept); else next.add(dept);
      return next;
    });
  }

  return (
    <section>
      <div className="page-head">
        <h1>People</h1>
        <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
          <div className="date">{people?.length || 0} people</div>
          {canManagePeople && <button type="button" className="btn-mini primary" onClick={() => setAddOpen(true)}>+ Add person</button>}
        </div>
      </div>

      {addOpen && <AddPersonModal onClose={() => setAddOpen(false)} onCreated={reload} />}

      {!people?.length && <div className="panel"><div className="panel-empty">No one to show yet</div></div>}

      {groups.map(([dept, members]) => {
        const isCollapsed = collapsed.has(dept);
        return (
          <div className="panel" key={dept} style={{ marginBottom: 14 }}>
            <button type="button" className="people-dept-head" onClick={() => toggle(dept)}>
              <span className={'appraisal-chevron' + (!isCollapsed ? ' open' : '')}><IconChevronDown /></span>
              <h2>{dept}</h2>
              <span className="pill neutral">{members.length}</span>
            </button>
            {!isCollapsed && (
              <div className="people-grid">
                {members.map((p) => (
                  <div className="people-card" key={p.id}>
                    <div className="avatar-circle">{p.avatarInitials || '?'}</div>
                    <div className="people-card-meta">
                      <div className="name">{p.fullName}</div>
                      <div className="title">{p.jobTitle || roleLabel(p.role)}</div>
                      <div className="contact">{p.email || 'Contact hidden'}{p.phone ? ` · ${p.phone}` : ''}</div>
                    </div>
                    <span className="pill neutral">{roleLabel(p.role)}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        );
      })}
    </section>
  );
}
