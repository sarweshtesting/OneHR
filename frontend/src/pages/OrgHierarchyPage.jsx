import { useMemo, useState } from 'react';
import { useApi } from '../hooks/useApi';
import { roleLabel } from '../utils/roles';

function buildTree(nodes) {
  const byId = new Map(nodes.map((n) => [n.id, { ...n, children: [] }]));
  const roots = [];
  byId.forEach((node) => {
    if (node.managerId && byId.has(node.managerId)) {
      byId.get(node.managerId).children.push(node);
    } else {
      roots.push(node);
    }
  });
  return roots;
}

function groupByDepartment(nodes) {
  const groups = new Map();
  nodes.forEach((n) => {
    const key = n.departmentName || 'Unassigned';
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key).push(n);
  });
  return [...groups.entries()].sort(([a], [b]) => a.localeCompare(b));
}

function OrgChartNode({ node }) {
  const hasChildren = node.children.length > 0;
  return (
    <li>
      <div className="oc-card">
        <div className="oc-avatar">{node.avatarInitials || '?'}</div>
        <div className="oc-name">{node.fullName}</div>
        <div className="oc-title">{node.jobTitle || roleLabel(node.role)}</div>
      </div>
      {hasChildren && (
        <ul>
          {node.children.map((child) => <OrgChartNode key={child.id} node={child} />)}
        </ul>
      )}
    </li>
  );
}

export default function OrgHierarchyPage() {
  const { data } = useApi('/api/org/hierarchy');
  const [view, setView] = useState('reporting');
  const tree = useMemo(() => (data ? buildTree(data) : []), [data]);
  const departments = useMemo(() => (data ? groupByDepartment(data) : []), [data]);

  return (
    <section>
      <div className="page-head">
        <h1>Organisation</h1>
      </div>

      <div className="attendance-toolbar">
        <div className="seg-control">
          <button className={'seg-btn' + (view === 'reporting' ? ' active' : '')} onClick={() => setView('reporting')}>Reporting lines</button>
          <button className={'seg-btn' + (view === 'department' ? ' active' : '')} onClick={() => setView('department')}>Department structure</button>
        </div>
      </div>

      {view === 'reporting' ? (
        <div className="panel oc-panel">
          {!data?.length && <div className="panel-empty">No team members to show</div>}
          {tree.length > 0 && (
            <div className="oc-scroll">
              <ul className="oc-tree">
                {tree.map((node) => <OrgChartNode key={node.id} node={node} />)}
              </ul>
            </div>
          )}
        </div>
      ) : (
        <>
          {!data?.length && <div className="panel"><div className="panel-empty">No team members to show</div></div>}
          {departments.map(([dept, members]) => (
            <div className="panel" key={dept} style={{ marginBottom: 14 }}>
              <div className="panel-head"><h2>{dept}</h2><span className="pill neutral">{members.length}</span></div>
              <div className="people-grid">
                {members.map((m) => (
                  <div className="people-card" key={m.id}>
                    <div className="avatar-circle">{m.avatarInitials || '?'}</div>
                    <div className="people-card-meta">
                      <div className="name">{m.fullName}</div>
                      <div className="title">{m.jobTitle || roleLabel(m.role)}</div>
                    </div>
                    <span className="pill neutral">{roleLabel(m.role)}</span>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </>
      )}
    </section>
  );
}
