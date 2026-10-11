/**
 * FIN Frontend Centralized API Configuration Module
 * 
 * Single source of truth for backend API base URL resolution across all frontend services.
 * - In production (PROD): Requires VITE_API_URL to be explicitly configured. Warns if missing or pointing to localhost.
 * - In development (DEV): Falls back to http://localhost:5000 if VITE_API_URL is omitted.
 * - Normalizes trailing slashes consistently.
 */

const rawEnvUrl = typeof import.meta !== 'undefined' && import.meta.env?.VITE_API_URL;
const isProd = Boolean(typeof import.meta !== 'undefined' && import.meta.env?.PROD);
const isDev = Boolean(typeof import.meta !== 'undefined' && import.meta.env?.DEV);

function resolveApiBaseUrl() {
  if (rawEnvUrl && typeof rawEnvUrl === 'string' && rawEnvUrl.trim()) {
    let trimmed = rawEnvUrl.trim().replace(/\/+$/, '');
    // Strip trailing /api so callers appending /api/... do not produce /api/api/...
    if (trimmed.endsWith('/api')) {
      trimmed = trimmed.slice(0, -4).replace(/\/+$/, '');
    }
    if (isProd && (trimmed.includes('localhost') || trimmed.includes('127.0.0.1'))) {
      console.warn(
        '[FIN API Config] WARNING: Production build is configured with a loopback/localhost URL:',
        trimmed
      );
    }
    return trimmed;
  }

  if (isProd) {
    console.error(
      '[FIN API Config] CRITICAL: VITE_API_URL environment variable is NOT set in production build! ' +
      'API requests will fail. Please set VITE_API_URL in your hosting provider dashboard (e.g. Vercel).'
    );
    // In production without env var, default to relative root so hosting proxies can potentially handle /api
    return '';
  }

  // Explicit development fallback
  return 'http://localhost:5000';
}

export const API_BASE_URL = resolveApiBaseUrl();

/**
 * Returns a fully qualified or clean relative URL for the given API path.
 * e.g. getApiUrl('/api/schemes') -> 'https://backend.onrender.com/api/schemes' (or '/api/schemes' if base is empty)
 * @param {string} path - Endpoint path (e.g. '/api/users/login')
 * @returns {string} Fully formatted URL
 */
export function getApiUrl(path = '') {
  if (!path) return API_BASE_URL;
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path;
  }
  let cleanPath = path.startsWith('/') ? path : `/${path}`;
  if (API_BASE_URL.endsWith('/api') && cleanPath.startsWith('/api/')) {
    cleanPath = cleanPath.slice(4);
  }
  return API_BASE_URL ? `${API_BASE_URL}${cleanPath}` : cleanPath;
}

export { isProd, isDev };
export default API_BASE_URL;
