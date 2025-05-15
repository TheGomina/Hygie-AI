export const TOKEN_KEY = 'hygie_jwt';

export function getToken(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string) {
  if (typeof window === 'undefined') return;
  localStorage.setItem(TOKEN_KEY, token);
}

export function logout() {
  if (typeof window === 'undefined') return;
  localStorage.removeItem(TOKEN_KEY);
}
