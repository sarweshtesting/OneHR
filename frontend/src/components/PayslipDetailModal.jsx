import Modal from './Modal';

function fmtMoney(n) {
  return Number(n).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
function fmtMonth(dateStr) {
  return new Date(dateStr).toLocaleDateString(undefined, { month: 'long', year: 'numeric' });
}

/** Splits the stored gross/deductions totals into a conventional Indian payslip's
 * line items for display — the backend only stores totals, not itemized figures. */
function breakdown(payslip) {
  const gross = Number(payslip.grossPay);
  const deductions = Number(payslip.deductions);
  const basic = gross * 0.5;
  const hra = gross * 0.25;
  const specialAllowance = gross - basic - hra;
  const pf = Math.min(deductions, basic * 0.12);
  const professionalTax = Math.min(Math.max(deductions - pf, 0), 200);
  const incomeTax = Math.max(deductions - pf - professionalTax, 0);
  return {
    earnings: [
      { label: 'Basic pay', value: basic },
      { label: 'House rent allowance', value: hra },
      { label: 'Special allowance', value: specialAllowance },
    ],
    deductionItems: [
      { label: 'Provident fund', value: pf },
      { label: 'Professional tax', value: professionalTax },
      { label: 'Income tax (TDS)', value: incomeTax },
    ],
  };
}

export default function PayslipDetailModal({ payslip, onClose }) {
  const { earnings, deductionItems } = breakdown(payslip);

  return (
    <Modal title="Payslip" onClose={onClose} wide>
      <div className="payslip-doc">
        <div className="payslip-doc-head">
          <div>
            <div className="org">{payslip.organizationName || 'Nexora'}</div>
            <div className="period">Payslip for {fmtMonth(payslip.periodMonth)}</div>
          </div>
          <span className={'pill ' + (payslip.status === 'PAID' ? 'neutral' : 'dark')}>{payslip.status === 'PAID' ? 'Paid' : 'Generated'}</span>
        </div>

        <div className="payslip-doc-employee">
          <div><span className="k">Employee</span><span className="v">{payslip.employeeName}</span></div>
          <div><span className="k">Designation</span><span className="v">{payslip.jobTitle || '—'}</span></div>
          {payslip.employeeCode && <div><span className="k">Employee code</span><span className="v mono">{payslip.employeeCode}</span></div>}
        </div>

        <div className="payslip-doc-cols">
          <div>
            <div className="payslip-doc-col-head">Earnings</div>
            {earnings.map((e) => (
              <div className="payslip-doc-row" key={e.label}><span>{e.label}</span><span className="mono">₹{fmtMoney(e.value)}</span></div>
            ))}
            <div className="payslip-doc-row total"><span>Gross pay</span><span className="mono">₹{fmtMoney(payslip.grossPay)}</span></div>
          </div>
          <div>
            <div className="payslip-doc-col-head">Deductions</div>
            {deductionItems.map((d) => (
              <div className="payslip-doc-row" key={d.label}><span>{d.label}</span><span className="mono">₹{fmtMoney(d.value)}</span></div>
            ))}
            <div className="payslip-doc-row total"><span>Total deductions</span><span className="mono">₹{fmtMoney(payslip.deductions)}</span></div>
          </div>
        </div>

        <div className="payslip-doc-net">
          <span>Net pay</span>
          <span className="mono">₹{fmtMoney(payslip.netPay)}</span>
        </div>

        <button type="button" className="btn-submit payslip-doc-print" onClick={() => window.print()}>Print / Save as PDF</button>
      </div>
    </Modal>
  );
}
