export default function Heatmap({ days }) {
  return (
    <div className="heatmap-grid">
      {days.map((day) => (
        <div key={day.date} className={'hm-cell ' + (day.type === 'none' ? '' : day.type)} title={day.date} />
      ))}
    </div>
  );
}
