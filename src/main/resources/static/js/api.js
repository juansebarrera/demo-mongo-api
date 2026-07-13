const API = (() => {
  const BASE = '/api';

  function getTokens() {
    return {
      access: localStorage.getItem('accessToken'),
      refresh: localStorage.getItem('refreshToken'),
    };
  }

  function saveTokens(access, refresh) {
    localStorage.setItem('accessToken', access);
    localStorage.setItem('refreshToken', refresh);
  }

  function clearTokens() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('username');
  }

  function getUser() {
    return localStorage.getItem('username');
  }

  function setUser(u) {
    localStorage.setItem('username', u);
  }

  function isAdmin() {
    const token = getTokens().access;
    if (!token) return false;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return (payload.roles || []).some(r => r === 'ROLE_ADMIN' || r === 'admin');
    } catch {
      return false;
    }
  }

  async function request(method, path, body, retry = false) {
    const { access } = getTokens();
    const headers = { 'Content-Type': 'application/json' };
    if (access) headers['Authorization'] = `Bearer ${access}`;

    const res = await fetch(`${BASE}${path}`, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
    });

    if (res.status === 401 && !retry && getTokens().refresh) {
      const refreshed = await tryRefresh();
      if (refreshed) return request(method, path, body, true);
      clearTokens();
      window.location.href = '/index.html';
      throw new Error('Sesion expirada');
    }

    if (res.status === 204) return null;

    const text = await res.text();
    let data;
    try { data = JSON.parse(text); } catch { data = text; }

    if (!res.ok) {
      let msg;
      if (typeof data === 'string' && data.length > 0) {
        msg = data;
      } else if (data && data.errores) {
        msg = Object.values(data.errores).join(', ');
      } else if (data && (data.mensaje || data.message || data.error)) {
        msg = data.mensaje || data.message || data.error;
      } else if (res.status === 401 || res.status === 403) {
        clearTokens();
        window.location.href = '/index.html';
        throw new Error('No autorizado - redirigiendo al login');
      } else {
        msg = `Error HTTP ${res.status}`;
      }
      throw new Error(msg);
    }
    return data;
  }

  async function tryRefresh() {
    const { refresh } = getTokens();
    if (!refresh) return false;
    try {
      const res = await fetch(`${BASE}/auth/refresh-token`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: refresh }),
      });
      if (!res.ok) return false;
      const data = await res.json();
      saveTokens(data.accessToken, data.refreshToken);
      return true;
    } catch {
      return false;
    }
  }

  async function download(path, filename) {
    const { access } = getTokens();
    const headers = {};
    if (access) headers['Authorization'] = `Bearer ${access}`;

    const res = await fetch(`${BASE}${path}`, { headers });
    if (!res.ok) {
      const text = await res.text();
      let msg;
      try { const d = JSON.parse(text); msg = d.mensaje || d.message || d.error || text; } catch { msg = text; }
      throw new Error(msg || `Error HTTP ${res.status}`);
    }
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  }

  return {
    getTokens,
    saveTokens,
    clearTokens,
    getUser,
    setUser,
    isAdmin,
    get: (path) => request('GET', path),
    post: (path, body) => request('POST', path, body),
    put: (path, body) => request('PUT', path, body),
    del: (path) => request('DELETE', path),
    download,
  };
})();
