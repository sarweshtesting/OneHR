import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useApi } from '../hooks/useApi';
import BackButton from '../components/BackButton';

const STATUS_PILL = { PAID: ['neutral', 'Paid'], GENERATED: ['dark', 'Generated'] };

function fmtMonth(dateStr) {
  return new Date(dateStr).toLocaleDateString(undefined, { month: 'long', year: 'numeric' });
}
function fmtMoney(n) {
  return Number(n).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

export default function PayslipsPage() {
  const { isManager } = useAuth();
  const [view, setView] = useState('my');
  const { data: mine } = useApi('/api/payslips/me', { skip: view !== 'my' });
  const { data: org } = useApi('/api/payslips', { skip: view !== 'org' });
  const rows = view === 'my' ? mine : org;

  return (
    <section>
      <div className="page-head">
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <BackButton />
          <h1>Payslips</h1>
        </div>
      </div>

      {isManager && (
        <div className="attendance-toolbar">
          <div className="seg-control">
            <button className={'seg-btn' + (view === 'my' ? ' active' : '')} onClick={() => setView('my')}>My payslips</button>
            <button className={'seg-btn' + (view === 'org' ? ' active' : '')} onClick={() => setView('org')}>All employees</button>
          </div>
        </div>
      )}

      <div className="panel">
        <div className="panel-head"><h2>{view === 'my' ? 'My payslips' : 'Organization payslips'}</h2></div>
        <table className="data-table">
          <thead>
            <tr>
              <th>Period</th>
              {view === 'org' && <th>Employee</th>}
              <th>Gross pay</th><th>Deductions</th><th>Net pay</th><th>Status</th>
            </tr>
          </thead>
          <tbody>
            {!rows?.length && (
              <tr><td colSpan={view === 'org' ? 6 : 5} style={{ textAlign: 'center', color: 'var(--ink-faint)' }}>No payslips yet</td></tr>
            )}
            {rows?.map((r) => {
              const [pillCls, pillLabel] = STATUS_PILL[r.status] || ['neutral', r.status];
              return (
                <tr key={r.id}>
                  <td>{fmtMonth(r.periodMonth)}</td>
                  {view === 'org' && (
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <div className="avatar-circle" style={{ width: 22, height: 22, fontSize: 9.5 }}>{r.avatarInitials || '?'}</div>
                        {r.employeeName}
                      </div>
                    </td>
                  )}
                  <td className="mono">₹{fmtMoney(r.grossPay)}</td>
                  <td className="mono">₹{fmtMoney(r.deductions)}</td>
                  <td className="mono"><b>₹{fmtMoney(r.netPay)}</b></td>
                  <td><span className={'pill ' + pillCls}>{pillLabel}</span></td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </section>
  );
}
