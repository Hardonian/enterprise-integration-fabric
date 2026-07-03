export type ApiResult<T> = { data: T | null; error: string | null; loading: boolean };
const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080';

function authHeaders(): Record<string, string> {
  if (typeof localStorage === 'undefined') return {};
  const token = localStorage.getItem('fabric.bearerToken');
  const devTenants = localStorage.getItem('fabric.devTenantIds');
  const headers: Record<string, string> = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  if (devTenants) headers['X-Dev-Tenant-Ids'] = devTenants;
  return headers;
}

export async function apiGet<T>(path: string): Promise<ApiResult<T>> {
  try {
    const res = await fetch(`${API_BASE}${path}`, { headers: authHeaders() });
    if (!res.ok) {
      const body = await res.json().catch(() => ({}));
      return { data: null, error: body.message ?? `${res.status} ${res.statusText}`, loading: false };
    }
    return { data: await res.json() as T, error: null, loading: false };
  } catch (err) {
    return { data: null, error: err instanceof Error ? err.message : 'API unavailable', loading: false };
  }
}

export async function apiPost<T>(path: string, payload: unknown): Promise<ApiResult<T>> {
  try {
    const res = await fetch(`${API_BASE}${path}`, { method: 'POST', headers: { 'content-type': 'application/json', ...authHeaders() }, body: JSON.stringify(payload) });
    const body = await res.json().catch(() => ({}));
    if (!res.ok) return { data: null, error: body.message ?? `${res.status} ${res.statusText}`, loading: false };
    return { data: body as T, error: null, loading: false };
  } catch (err) {
    return { data: null, error: err instanceof Error ? err.message : 'API unavailable', loading: false };
  }
}
