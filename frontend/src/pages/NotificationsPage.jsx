import { useEffect, useState } from 'react';
import { apiFetch } from '../api/client';
import { useApi } from '../hooks/useApi';
import { fmtRelativeTime } from '../utils/format';
import { IconCheck, IconLeaveType, IconRegularization, IconWarningTriangle } from '../components/icons';

function iconFor(type) {
  if (type.endsWith('APPROVED')) return <IconCheck />;
  if (type.endsWith('REJECTED')) return <IconWarningTriangle />;
  if (type.startsWith('REGULARIZATION')) return <IconRegularization />;
  return <IconLeaveType />;
}

export default function NotificationsPage() {
  const { data, reload } = useApi('/api/notifications');
  const [items, setItems] = useState(null);

  useEffect(() => {
    if (data) setItems(data);
  }, [data]);

  const unreadCount = (items || []).filter((n) => !n.read).length;

  async function markRead(id) {
    setItems((prev) => prev.map((n) => (n.id === id ? { ...n, read: true } : n)));
    try {
      await apiFetch(`/api/notifications/${id}/read`, { method: 'POST' });
    } catch {
      reload();
    }
  }

  async function markAllRead() {
    setItems((prev) => prev.map((n) => ({ ...n, read: true })));
    try {
      await apiFetch('/api/notifications/read-all', { method: 'POST' });
    } catch {
      reload();
    }
  }

  return (
    <section>
      <div className="page-head">
        <h1>Notifications</h1>
        {unreadCount > 0 && <button className="btn-mini primary" onClick={markAllRead}>Mark all as read</button>}
      </div>

      <div className="panel">
        {!items?.length && <div className="panel-empty">You have no notifications yet</div>}
        {items?.map((n) => (
          <div
            key={n.id}
            className={'notif-page-item' + (!n.read ? ' unread' : '')}
            onClick={() => !n.read && markRead(n.id)}
            style={{ cursor: n.read ? 'default' : 'pointer' }}
          >
            <div className="notif-page-icon">{iconFor(n.type)}</div>
            <div className="notif-page-body">
              <div className="notif-page-top">
                <div className="notif-page-title">{n.title}</div>
                <div className="notif-page-time">{fmtRelativeTime(n.createdAt)}</div>
              </div>
              {n.body && <div className="notif-page-text">{n.body}</div>}
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
