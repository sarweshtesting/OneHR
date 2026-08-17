import { useState } from 'react';
import { useApi } from '../hooks/useApi';
import { IconChevronDown } from '../components/icons';

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

function OrgNode({ node, collapsed, toggle }) {
  const isCollapsed = collapsed.has(node.id);
  const hasChildren = node.children.length > 0;

  return (
    <div className="org-node">
      <div className="org-node-row">
        {hasChildren ? (
          <button
            type="button"
            className={'org-node-toggle' + (isCollapsed ? ' collapsed' : '')}
            onClick={() => toggle(node.id)}
            aria-label={isCollapsed ? 'Expand' : 'Collapse'}
          >
            <IconChevronDown />
          </button>
        ) : (
          <span className="org-node-toggle-spacer" />
        )}
        <div className="avatar-circle" title={node.jobTitle || node.role.replace('_', ' ')}>{node.avatarInitials || '?'}</div>
        <div className="org-node-meta" title={node.jobTitle || node.role.replace('_', ' ')}>
          <div className="name">{node.fullName}</div>
          <div className="title">{node.jobTitle || node.role.replace('_', ' ')}</div>
        </div>
        <span className="pill neutral">{node.role.replace('_', ' ')}</span>
      </div>
      {hasChildren && !isCollapsed && (
        <div className="org-node-children">
          {node.children.map((child) => <OrgNode key={child.id} node={child} collapsed={collapsed} toggle={toggle} />)}
        </div>
      )}
    </div>
  );
}

export default function OrgHierarchyPage() {
  const { data } = useApi('/api/org/hierarchy');
  const [collapsed, setCollapsed] = useState(() => new Set());
  const tree = data ? buildTree(data) : [];

  function toggle(id) {
    setCollapsed((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  }

  return (
    <section>
      <div className="page-head">
        <h1>Organisation</h1>
      </div>

      <div className="panel">
        {!data?.length && <div className="panel-empty">No team members to show</div>}
        {tree.length > 0 && (
          <div className="org-tree">
            {tree.map((node) => <OrgNode key={node.id} node={node} collapsed={collapsed} toggle={toggle} />)}
          </div>
        )}
      </div>
    </section>
  );
}
