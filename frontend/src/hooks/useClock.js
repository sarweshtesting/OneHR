import { useEffect, useState } from 'react';

const pad = (n) => String(n).padStart(2, '0');

export function useClock() {
  const [now, setNow] = useState(new Date());
  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(id);
  }, []);
  const h = pad(now.getHours()), m = pad(now.getMinutes()), s = pad(now.getSeconds());
  return { now, timeString: `${h}:${m}:${s}` };
}
