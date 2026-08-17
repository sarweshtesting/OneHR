const pad = (n) => String(n).padStart(2, '0');

export function fmtTime(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  return pad(d.getHours()) + ':' + pad(d.getMinutes());
}

export function fmtDateShort(isoDate) {
  return new Date(isoDate + 'T00:00:00').toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
}

export function fmtDateRange(startIso, endIso) {
  return startIso === endIso ? fmtDateShort(startIso) : `${fmtDateShort(startIso)}–${fmtDateShort(endIso)}`;
}

export function fmtDuration(totalMinutes) {
  const hrs = Math.floor(totalMinutes / 60), mins = totalMinutes % 60;
  return `${hrs}h ${mins}m`;
}

export function fmtHoursMinutes(totalMinutes) {
  if (totalMinutes == null) return '—';
  const hrs = Math.floor(totalMinutes / 60), mins = totalMinutes % 60;
  return `${hrs}:${pad(mins)}`;
}

export function currentMonthParam() {
  const d = new Date();
  return d.getFullYear() + '-' + pad(d.getMonth() + 1);
}

export function fmtRelativeTime(iso) {
  if (!iso) return '';
  const diffMs = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diffMs / 60000);
  if (mins < 1) return 'just now';
  if (mins < 60) return `${mins}m ago`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days < 7) return `${days}d ago`;
  return new Date(iso).toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
}
