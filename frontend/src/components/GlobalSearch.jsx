import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { roleLabel } from '../utils/roles';
import { IconSearch } from './icons';

export default function GlobalSearch() {
  const { data: people } = useApi('/api/people');
  const [query, setQuery] = useState('');
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();
  const boxRef = useRef(null);

  const matches = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q || !people) return [];
    return people
      .filter((p) => p.fullName.toLowerCase().includes(q) || p.jobTitle?.toLowerCase().includes(q) || p.email?.toLowerCase().includes(q))
      .slice(0, 6);
  }, [query, people]);

  useEffect(() => {
    function onClickOutside(e) {
      if (boxRef.current && !boxRef.current.contains(e.target)) setOpen(false);
    }
    document.addEventListener('mousedown', onClickOutside);
    return () => document.removeEventListener('mousedown', onClickOutside);
  }, []);

  function goToPeople() {
    setOpen(false);
    navigate('/people');
  }

  return (
    <div className="search-box" ref={boxRef}>
      <IconSearch />
      <input
        type="text"
        placeholder="Search people, requests, records…"
        value={query}
        onChange={(e) => { setQuery(e.target.value); setOpen(true); }}
        onFocus={() => setOpen(true)}
        onKeyDown={(e) => { if (e.key === 'Enter') goToPeople(); }}
      />
      {open && query.trim() && (
        <div className="search-dropdown">
          {!matches.length && <div className="search-empty">No people match "{query.trim()}"</div>}
          {matches.map((p) => (
            <button type="button" className="search-result" key={p.id} onClick={goToPeople}>
              <div className="avatar-circle">{p.avatarInitials || '?'}</div>
              <div className="search-result-meta">
                <div className="name">{p.fullName}</div>
                <div className="sub">{p.jobTitle || roleLabel(p.role)}</div>
              </div>
            </button>
          ))}
          {matches.length > 0 && (
            <button type="button" className="search-viewall" onClick={goToPeople}>View in People directory →</button>
          )}
        </div>
      )}
    </div>
  );
}
