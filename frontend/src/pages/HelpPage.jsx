const TOPICS = [
  {
    title: 'Clocking in and out',
    body: 'Use the Clock in / Clock out button on your Overview page to record attendance. Breaks can be started and stopped from the same panel.',
  },
  {
    title: 'Applying for leave',
    body: 'Go to the Leave tab, choose a leave type, pick your dates and add a short reason. Your request goes to your manager for approval.',
  },
  {
    title: 'Regularizing attendance',
    body: 'If you forgot to clock in or out, submit a regularization request from the Attendance tab with the correct times and a reason.',
  },
  {
    title: 'Notifications',
    body: 'The bell icon in the header shows your recent approvals and rejections. Use the Notifications tab to see the full history.',
  },
  {
    title: 'Updating your profile',
    body: 'Open My Profile from the header avatar menu to update your phone number, blood group and emergency contact details, or change your password.',
  },
];

export default function HelpPage() {
  return (
    <section>
      <div className="page-head">
        <h1>Help &amp; Guidance</h1>
      </div>

      <div className="panel">
        {TOPICS.map((topic) => (
          <div className="notif-page-item" key={topic.title}>
            <div className="notif-page-body">
              <div className="notif-page-title">{topic.title}</div>
              <div className="notif-page-text">{topic.body}</div>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
