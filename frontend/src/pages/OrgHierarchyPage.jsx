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
  const tree = data ? buildTree(data) : [];

  return (
    <section>
      <div className="page-head">
        <h1>Organisation</h1>
      </div>

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
    </section>
  );
}
