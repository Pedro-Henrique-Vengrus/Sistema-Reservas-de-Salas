// Cliente HTTP com interceptor de token e tratamento padrao de erros
const BASE = '/api';

function getToken() {
  return localStorage.getItem('cf_token');
}

async function request(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
  const token = getToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;

  let res;
  try {
    res = await fetch(`${BASE}${path}`, { ...options, headers });
  } catch (netErr) {
    throw new Error('Sem conexao com o servidor (backend rodando?)');
  }

  if (res.status === 401) {
    localStorage.removeItem('cf_token');
    localStorage.removeItem('cf_user');
    if (!path.includes('/auth/login')) window.location.href = '/login';
    throw new Error('Sessao expirada');
  }

  if (!res.ok) {
    let msg = `Erro ${res.status}`;
    try {
      const body = await res.json();
      if (body.message) msg = body.message;
    } catch (_) {}
    throw new Error(msg);
  }

  if (res.status === 204) return null;
  return res.json();
}

export const api = {
  get: (p) => request(p),
  post: (p, body) => request(p, { method: 'POST', body: JSON.stringify(body) }),
  put: (p, body) => request(p, { method: 'PUT', body: JSON.stringify(body) }),
  del: (p) => request(p, { method: 'DELETE' }),
};
