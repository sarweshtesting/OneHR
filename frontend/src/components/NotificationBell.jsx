import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiFetch } from '../api/client';
import { useApi } from '../hooks/useApi';
import { fmtRelativeTime } from '../utils/format';
import { IconBell } from './icons';

const VISIBLE_LIMIT = 4;
const AUTO_ADVANCE_MS = 1800;

export default function NotificationBell() {
  const { data: items, reload, setLocal } = useNotificationsList();
  const [open, setOpen] = useState(false);
  const [expandedId, setExpandedId] = useState(null);
  const closeTimer = useRef(null);
  const advanceTimer = useRef(null);
  const navigate = useNavigate();

  const list = items || [];
  const visible = list.slice(0, VISIBLE_LIMIT);
  const unreadCount = list.filter((n) => !n.read).length;
  const hasMore = list.length > VISIBLE_LIMIT;

  function openDropdown() {
    clearTimeout(closeTimer.current);
    setOpen(true);
    reload();
  }

  function scheduleClose() {
    closeTimer.current = setTimeout(() => {
      setOpen(false);
      setExpandedId(null);
    }, 150);
  }

  async function markRead(id) {
    setLocal((prev) => prev.map((n) => (n.id === id ? { ...n, read: true } : n)));
    try {
      await apiFetch(`/api/notifications/${id}/read`, { method: 'POST' });
    } catch {
      // best-effort — the list will reconcile next time the dropdown opens
    }
  }

  function handleItemClick(item) {
    if (!item.read) markRead(item.id);
    setExpandedId(item.id);
  }

  useEffect(() => {
    clearTimeout(advanceTimer.current);
    if (!expandedId) return undefined;
    const index = visible.findIndex((n) => n.id === expandedId);
    const next = index >= 0 ? visible[index + 1] : null;
    advanceTimer.current = setTimeout(() => {
      if (next) {
        if (!next.read) markRead(next.id);
        setExpandedId(next.id);
      } else {
        setExpandedId(null);
      }
    }, AUTO_ADVANCE_MS);
    return () => clearTimeout(advanceTimer.current);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [expandedId]);

  function goToAll() {
    setOpen(false);
    setExpandedId(null);
    navigate('/notifications');
  }

  return (
    <div className="header-dropdown-anchor" onMouseEnter={openDropdown} onMouseLeave={scheduleClose}>
      <button className="icon-btn" title="Notifications">
        <IconBell />
        {unreadCount > 0 && <span className="ping" />}
      </button>
      {open && (
        <div className="header-dropdown notif-dropdown">
          <div className="notif-dropdown-head">
            <span>Notifications</span>
            {unreadCount > 0 && <span className="pill accent">{unreadCount} new</span>}
          </div>
          <div className="notif-list">
            {!visible.length && <div className="notif-dropdown-empty">You're all caught up</div>}
            {visible.map((n) => (
              <button key={n.id} className={'notif-item' + (!n.read ? ' unread' : '')} onClick={() => handleItemClick(n)}>
                <div className="notif-item-top">
                  {!n.read && <span className="notif-dot" />}
                  <span className="notif-title">{n.title}</span>
                  <span className="notif-time">{fmtRelativeTime(n.createdAt)}</span>
                </div>
                {expandedId === n.id && n.body && <div className="notif-body">{n.body}</div>}
              </button>
            ))}
          </div>
          {hasMore && (
            <div className="notif-dropdown-footer">
              <button onClick={goToAll}>and {list.length - VISIBLE_LIMIT} more</button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

/** Thin wrapper around useApi that also allows optimistic local mutation (read state). */
function useNotificationsList() {
  const { data, reload } = useApi('/api/notifications');
  const [local, setLocalState] = useState(null);

  useEffect(() => {
    if (data) setLocalState(data);
  }, [data]);

  function setLocal(updater) {
    setLocalState((prev) => updater(prev || []));
  }

  return { data: local, reload, setLocal };
}
