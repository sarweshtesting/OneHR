import { useCallback, useEffect, useRef, useState } from 'react';
import { apiFetch } from '../api/client';

/**
 * Fetches `path` on mount (and whenever `path` changes), skipping entirely
 * when `path` is null/false — the pattern used to gate manager-only fetches.
 */
export function useApi(path, { skip = false } = {}) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(!skip);
  const [error, setError] = useState(null);
  const requestId = useRef(0);

  const reload = useCallback(async () => {
    if (skip || !path) return;
    const id = ++requestId.current;
    setLoading(true);
    setError(null);
    try {
      const result = await apiFetch(path);
      if (id === requestId.current) setData(result);
    } catch (err) {
      if (id === requestId.current) setError(err);
    } finally {
      if (id === requestId.current) setLoading(false);
    }
  }, [path, skip]);

  useEffect(() => {
    if (skip || !path) {
      setLoading(false);
      return;
    }
    reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [path, skip]);

  return { data, loading, error, reload };
}
