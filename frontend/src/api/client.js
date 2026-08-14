export const API_BASE = import.meta.env.VITE_API_BASE || 'https://onehr-jcvs.onrender.com';

const TOKEN_KEY = 'nforcehq_token';
const ORG_KEY = 'nforcehq_selected_org';

let unauthorizedHandler = null;
export function onUnauthorized(handler) {
  unauthorizedHandler = handler;
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}
export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token);
}
export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

export function getSelectedOrg() {
  return localStorage.getItem(ORG_KEY);
}
export function setSelectedOrg(orgId) {
  localStorage.setItem(ORG_KEY, orgId);
}
export function clearSelectedOrg() {
  localStorage.removeItem(ORG_KEY);
}

export async function apiFetch(path, opts = {}) {
  const headers = { ...(opts.headers || {}), Authorization: `Bearer ${getToken()}` };
  if (opts.body) headers['Content-Type'] = 'application/json';
  const selectedOrg = getSelectedOrg();
  if (selectedOrg) headers['X-Organization-Id'] = selectedOrg;

  const res = await fetch(API_BASE + path, { ...opts, headers });

  if (res.status === 401) {
    clearToken();
    clearSelectedOrg();
    if (unauthorizedHandler) unauthorizedHandler();
    throw new Error('Session expired');
  }
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.message || body.error || `Request failed: ${res.status}`);
  }
  if (res.status === 204) return null;
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

export async function login(email, password) {
  const res = await fetch(API_BASE + '/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });
  const body = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(body.message || 'Invalid email or password');
  return body;
}

export async function apiFetchBlob(path) {
  const res = await fetch(API_BASE + path, { headers: { Authorization: `Bearer ${getToken()}` } });
  if (!res.ok) throw new Error('Request failed: ' + res.status);
  return res.blob();
}
