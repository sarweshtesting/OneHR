import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import {
  apiFetch, login as apiLogin, signup as apiSignup, getToken, setToken, clearToken,
  getSelectedOrg, setSelectedOrg, clearSelectedOrg, onUnauthorized,
} from '../api/client';
import { isManagerUp, canAccessFinance as roleCanAccessFinance } from '../utils/roles';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [organizations, setOrganizations] = useState([]);
  const [selectedOrgId, setSelectedOrgIdState] = useState(getSelectedOrg());
  const [status, setStatus] = useState('loading'); // loading | authenticated | anonymous

  const logout = useCallback(() => {
    clearToken();
    clearSelectedOrg();
    setUser(null);
    setOrganizations([]);
    setSelectedOrgIdState(null);
    setStatus('anonymous');
  }, []);

  useEffect(() => {
    onUnauthorized(logout);
  }, [logout]);

  const loadOrganizations = useCallback(async () => {
    try {
      const orgs = await apiFetch('/api/organizations');
      setOrganizations(orgs);
      const stored = getSelectedOrg();
      if (stored && orgs.some((o) => o.id === stored)) {
        setSelectedOrgIdState(stored);
      } else if (orgs.length) {
        setSelectedOrg(orgs[0].id);
        setSelectedOrgIdState(orgs[0].id);
      }
    } catch (err) {
      console.error('Failed to load organizations', err);
    }
  }, []);

  const afterAuth = useCallback(async (userData) => {
    setUser(userData);
    // Must resolve the org list (and default selectedOrgId) before flipping to
    // 'authenticated' — that status change is what lets protected routes render,
    // and their data hooks fire immediately on mount. If a PLATFORM_ADMIN's first
    // requests go out before an org is selected, the backend has no tenant to
    // scope them to and correctly rejects them with 400.
    if (userData.role === 'PLATFORM_ADMIN') {
      await loadOrganizations();
    }
    setStatus('authenticated');
  }, [loadOrganizations]);

  useEffect(() => {
    (async () => {
      if (!getToken()) {
        setStatus('anonymous');
        return;
      }
      try {
        const me = await apiFetch('/api/auth/me');
        await afterAuth(me);
      } catch {
        setStatus('anonymous');
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const login = useCallback(async (email, password) => {
    const { token, user: userData } = await apiLogin(email, password);
    setToken(token);
    await afterAuth(userData);
  }, [afterAuth]);

  const signup = useCallback(async (orgName, adminFullName, adminEmail, password) => {
    const { token, user: userData } = await apiSignup(orgName, adminFullName, adminEmail, password);
    setToken(token);
    await afterAuth(userData);
  }, [afterAuth]);

  const selectOrg = useCallback((orgId) => {
    setSelectedOrg(orgId);
    setSelectedOrgIdState(orgId);
  }, []);

  /** Re-fetches /api/auth/me — used after profile/avatar edits so the header updates without a re-login. */
  const refreshUser = useCallback(async () => {
    try {
      const me = await apiFetch('/api/auth/me');
      setUser(me);
    } catch (err) {
      console.error('Failed to refresh user', err);
    }
  }, []);

  const isManager = useMemo(() => user && isManagerUp(user.role), [user]);
  const canAccessFinance = useMemo(() => user && roleCanAccessFinance(user.role), [user]);

  const value = useMemo(() => ({
    user, status, organizations, selectedOrgId, isManager, canAccessFinance,
    login, signup, logout, selectOrg, refreshUser,
  }), [user, status, organizations, selectedOrgId, isManager, canAccessFinance, login, signup, logout, selectOrg, refreshUser]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
