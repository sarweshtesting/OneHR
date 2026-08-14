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
