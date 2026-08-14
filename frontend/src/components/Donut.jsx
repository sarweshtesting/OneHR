const COLORS = {
  office: 'var(--black)',
  wfh: 'var(--ink-faint)',
  partial: 'var(--red)',
  leave: 'var(--border-strong)',
};

export default function Donut({ officePct, wfhPct, partialPct, leavePct }) {
  const wfhEnd = officePct + wfhPct;
  const partialEnd = wfhEnd + partialPct;
  const background = `conic-gradient(${COLORS.office} 0% ${officePct}%, ${COLORS.wfh} ${officePct}% ${wfhEnd}%, ${COLORS.partial} ${wfhEnd}% ${partialEnd}%, ${COLORS.leave} ${partialEnd}% 100%)`;

  return (
    <div className="donut-card">
      <div className="donut" style={{ background }} />
      <div className="donut-legend">
        <div><span style={{ background: COLORS.office }} />Office <b>{officePct}%</b></div>
        <div><span style={{ background: COLORS.wfh }} />WFH <b>{wfhPct}%</b></div>
        <div><span style={{ background: COLORS.partial }} />Partial day <b>{partialPct}%</b></div>
        <div><span style={{ background: COLORS.leave }} />Leave/off <b>{leavePct}%</b></div>
      </div>
    </div>
  );
}
