import { useApi } from '../hooks/useApi';
import { fmtRelativeTime } from '../utils/format';
import BackButton from '../components/BackButton';

const ACTION_LABELS = {
  PERSON_ADDED: 'Person added',
  PERSON_UPDATED: 'Person updated',
  PERSON_DEACTIVATED: 'Person deactivated',
  DEPARTMENT_CREATED: 'Department created',
  ORG_RENAMED: 'Organization renamed',
  LEAVE_APPROVED: 'Leave approved',
  LEAVE_REJECTED: 'Leave rejected',
  REGULARIZATION_APPROVED: 'Regularization approved',
  REGULARIZATION_REJECTED: 'Regularization rejected',
};

export default function AuditLogPage() {
  const { data: logs } = useApi('/api/audit-logs');

  return (
    <section>
      <div className="page-head">
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <BackButton />
          <h1>Audit Log</h1>
        </div>
      </div>

      <div className="panel">
        {!logs?.length && <div className="panel-empty">No admin activity recorded yet</div>}
        {logs?.map((log) => (
          <div className="audit-row" key={log.id}>
            <span className="pill dark">{ACTION_LABELS[log.action] || log.action}</span>
            <div className="audit-row-body">
              <div className="desc">{log.description}</div>
              <div className="meta">{log.actorName} · {fmtRelativeTime(log.createdAt)}</div>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
