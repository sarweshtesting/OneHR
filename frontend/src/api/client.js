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

/** Unauthenticated POST — for login/signup/forgot/reset, none of which have a token yet. */
async function publicPost(path, payload) {
  const res = await fetch(API_BASE + path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  const body = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(body.message || 'Request failed');
  return body;
}

export async function login(email, password) {
  return publicPost('/api/auth/login', { email, password });
}

export async function signup(orgName, adminFullName, adminEmail, password) {
  return publicPost('/api/auth/signup', { orgName, adminFullName, adminEmail, password });
}

export async function forgotPassword(email) {
  return publicPost('/api/auth/forgot-password', { email });
}

export async function resetPassword(token, newPassword) {
  return publicPost('/api/auth/reset-password', { token, newPassword });
}

/** Multipart upload — omits the JSON Content-Type so the browser can set its own multipart boundary. */
export async function apiUpload(path, formData) {
  const headers = { Authorization: `Bearer ${getToken()}` };
  const selectedOrg = getSelectedOrg();
  if (selectedOrg) headers['X-Organization-Id'] = selectedOrg;

  const res = await fetch(API_BASE + path, { method: 'POST', headers, body: formData });

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
  return res.json();
}

export async function apiFetchBlob(path) {
  const res = await fetch(API_BASE + path, { headers: { Authorization: `Bearer ${getToken()}` } });
  if (!res.ok) throw new Error('Request failed: ' + res.status);
  return res.blob();
}
