import { useEffect, useRef, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useApi } from '../hooks/useApi';
import { apiFetch } from '../api/client';
import { fmtRelativeTime } from '../utils/format';

const POLL_MS = 8000;

export default function FinanceChatPage() {
  const { user } = useAuth();
  const { data: messages, reload } = useApi('/api/finance/chat/messages');
  const [draft, setDraft] = useState('');
  const [sending, setSending] = useState(false);
  const listRef = useRef(null);

  useEffect(() => {
    const interval = setInterval(reload, POLL_MS);
    return () => clearInterval(interval);
  }, [reload]);

  useEffect(() => {
    if (listRef.current) listRef.current.scrollTop = listRef.current.scrollHeight;
  }, [messages]);

  async function handleSend(e) {
    e.preventDefault();
    if (!draft.trim()) return;
    setSending(true);
    try {
      await apiFetch('/api/finance/chat/messages', { method: 'POST', body: JSON.stringify({ message: draft.trim() }) });
      setDraft('');
      await reload();
    } catch (err) {
      alert(err.message);
    } finally {
      setSending(false);
    }
  }

  return (
    <section>
      <div className="page-head">
        <h1>Finance</h1>
        <div className="date">Ask finance questions — payroll, reimbursements, tax</div>
      </div>

      <div className="panel finance-chat-panel">
        <div className="panel-head"><h2>Finance chat</h2></div>
        <div className="finance-chat-messages" ref={listRef}>
          {!messages?.length && <div className="panel-empty">No messages yet — start the conversation</div>}
          {messages?.map((m) => (
            <div key={m.id} className={'chat-bubble-row' + (m.userId === user?.id ? ' own' : '')}>
              <div className="chat-bubble">
                {m.userId !== user?.id && <div className="chat-bubble-sender">{m.userName}</div>}
                <div className="chat-bubble-text">{m.message}</div>
                <div className="chat-bubble-time">{fmtRelativeTime(m.createdAt)}</div>
              </div>
            </div>
          ))}
        </div>
        <form className="finance-chat-input" onSubmit={handleSend}>
          <input
            placeholder="Type a message to Finance…"
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
          />
          <button type="submit" className="btn-mini primary" disabled={sending || !draft.trim()}>Send</button>
        </form>
      </div>
    </section>
  );
}
