import { useApi } from '../hooks/useApi';

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

function OrgNode({ node }) {
  return (
    <div className="org-node">
      <div className="org-node-row">
        <div className="avatar-circle">{node.avatarInitials || '?'}</div>
        <div className="org-node-meta">
          <div className="name">{node.fullName}</div>
          <div className="title">{node.jobTitle || node.role.replace('_', ' ')}</div>
        </div>
        <span className="pill neutral">{node.role.replace('_', ' ')}</span>
      </div>
      {node.children.length > 0 && (
        <div className="org-node-children">
          {node.children.map((child) => <OrgNode key={child.id} node={child} />)}
        </div>
      )}
    </div>
  );
}

export default function OrgHierarchyPage() {
  const { data } = useApi('/api/org/hierarchy');
  const tree = data ? buildTree(data) : [];

  return (
    <section>
      <div className="page-head">
        <h1>Org Hierarchy</h1>
      </div>

      <div className="panel">
        {!data?.length && <div className="panel-empty">No team members to show</div>}
        {tree.length > 0 && (
          <div className="org-tree">
            {tree.map((node) => <OrgNode key={node.id} node={node} />)}
          </div>
        )}
      </div>
    </section>
  );
}
