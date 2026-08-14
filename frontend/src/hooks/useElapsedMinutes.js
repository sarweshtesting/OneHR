import { useEffect, useState } from 'react';

/** Live-updating elapsed minutes since `startIso`, ticking every 30s. Returns null when startIso is falsy. */
export function useElapsedMinutes(startIso) {
  const [minutes, setMinutes] = useState(() => (startIso ? computeMinutes(startIso) : null));

  useEffect(() => {
    if (!startIso) { setMinutes(null); return; }
    setMinutes(computeMinutes(startIso));
    const id = setInterval(() => setMinutes(computeMinutes(startIso)), 30000);
    return () => clearInterval(id);
  }, [startIso]);

  return minutes;
}

function computeMinutes(startIso) {
  return Math.max(0, Math.floor((Date.now() - new Date(startIso).getTime()) / 60000));
}
