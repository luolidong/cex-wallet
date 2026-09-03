import type { AdminUser } from '../api/auth';

const TOKEN_KEY = 'cex_wallet_access_token';
const USER_KEY = 'cex_wallet_admin_user';

export function getAccessToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setSession(token: string, user: AdminUser) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function updateStoredUser(user: AdminUser) {
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export function getStoredUser(): AdminUser | null {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as AdminUser;
  } catch {
    clearSession();
    return null;
  }
}
